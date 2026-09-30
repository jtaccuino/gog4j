//DEPS org.apache.poi:poi-ooxml:5.3.0
//DEPS org.apache.logging.log4j:log4j-core:2.24.3

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 * Derives the committed GWAS demo files from the published GIANT sources:
 *
 * <ul>
 *   <li>{@code height_lead_snps.csv} from Supplementary Table 10 (.xlsx)</li>
 *   <li>{@code height_gwas_thinned.csv.gz} from the summary-statistics .gz</li>
 * </ul>
 *
 * Invoked by {@code derive-demo-dataset.sh}. Implemented as a jbang script so the
 * project's tooling stays JVM-only (no Python/openpyxl). Apache POI reads the
 * workbook; the {@code pyRepr} helper reproduces Python's {@code repr(float)}
 * so the regenerated CSV is byte-for-byte identical to the committed one.
 */
public class DeriveGwas {

    private static final String SHEET = "ST10 - COJO - METAFE";
    private static final String[] WANTED = {"Chr", "BP (HG19)", "SNP", "b P-value", "Closest gene"};
    // openpyxl converts date-formatted cells to datetime; Python's str() renders
    // it in this exact shape, a couple of the source gene cells being dates.
    private static final DateTimeFormatter PY_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) throws IOException {
        Path sumstats = null;
        Path suppl = null;
        Path out = null;
        int every = 3;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--sumstats" -> sumstats = Path.of(args[++i]);
                case "--suppl" -> suppl = Path.of(args[++i]);
                case "--out" -> out = Path.of(args[++i]);
                case "--every" -> every = Integer.parseInt(args[++i]);
                default -> throw new IllegalArgumentException("unknown argument: " + args[i]);
            }
        }
        if (sumstats == null || suppl == null || out == null) {
            throw new IllegalArgumentException(
                    "usage: DeriveGwas --sumstats <gz> --suppl <xlsx> --out <dir> [--every N]");
        }
        Files.createDirectories(out);

        List<Lead> leads = extractLeads(suppl);
        Path thinned = out.resolve("height_gwas_thinned.csv.gz");
        Set<String> thinnedKeys = thin(sumstats, thinned, leads, every);
        writeLeadSnps(out.resolve("height_lead_snps.csv"), leads, thinnedKeys);
    }

    /** A lead SNP row from Supplementary Table 10. */
    private record Lead(long chr, long bp, String snp, String p, String gene) {
        String key() {
            return chr + ":" + bp;
        }
    }

    // --- 1. Supplementary Table 10 -------------------------------------------

    private static List<Lead> extractLeads(Path suppl) throws IOException {
        try (Workbook wb = WorkbookFactory.create(suppl.toFile(), null, true)) {
            Sheet sheet = wb.getSheet(SHEET);
            if (sheet == null) {
                throw new IllegalStateException("workbook has no sheet " + SHEET);
            }
            Row headerRow = sheet.getRow(1);
            Map<String, Integer> header = new LinkedHashMap<>();
            for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                Cell cell = headerRow.getCell(c);
                if (cell != null && cell.getCellType() == CellType.STRING) {
                    header.putIfAbsent(cell.getStringCellValue(), c);
                }
            }
            for (String column : WANTED) {
                if (!header.containsKey(column)) {
                    throw new IllegalStateException(
                            "Supplementary Table 10 is missing column: " + column);
                }
            }
            int chrCol = header.get("Chr");
            int bpCol = header.get("BP (HG19)");
            int snpCol = header.get("SNP");
            int pCol = header.get("b P-value");
            int geneCol = header.get("Closest gene");

            List<Lead> leads = new ArrayList<>();
            for (int r = 2; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || blank(row.getCell(chrCol))) {
                    continue;
                }
                leads.add(new Lead(
                        longOf(row.getCell(chrCol)),
                        longOf(row.getCell(bpCol)),
                        csvCell(row.getCell(snpCol)),
                        floatOf(row.getCell(pCol)),
                        csvCell(row.getCell(geneCol))));
            }
            System.err.printf("ST10    %d lead SNPs extracted%n", leads.size());
            return leads;
        }
    }

    // --- 2. Thin the summary statistics --------------------------------------

    private static Set<String> thin(Path sumstats, Path thinned, List<Lead> leads, int every)
            throws IOException {
        Set<String> leadKeys = new HashSet<>();
        for (Lead lead : leads) {
            leadKeys.add(lead.key());
        }

        Set<String> keptKeys = new HashSet<>();
        long usable = 0;
        long dropped = 0;
        long kept = 0;
        long uniform = 0;

        try (InputStream raw = new BufferedInputStream(Files.newInputStream(sumstats));
                InputStream in = new GZIPInputStream(raw, 1 << 16);
                var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8), 1 << 16);
                OutputStream fileOut = Files.newOutputStream(thinned);
                OutputStream gz = new GZIPOutputStream(fileOut, 1 << 16);
                Writer out = new BufferedWriter(new OutputStreamWriter(gz, StandardCharsets.UTF_8), 1 << 16)) {
            out.write("CHR,BP,P,SAMPLED\n");
            reader.readLine(); // header
            String line;
            while ((line = reader.readLine()) != null) {
                String[] f = line.split("\t", -1);
                if (f.length < 10) {
                    continue;
                }
                String chr = f[2];
                String bp = f[3];
                String p = f[9];
                if (p.equals("NA") || p.isEmpty()) {
                    dropped++;
                    continue;
                }
                usable++;
                boolean sampled = usable % every == 0;
                String key = chr + ":" + bp;
                if (sampled || leadKeys.contains(key)) {
                    out.write(chr);
                    out.write(',');
                    out.write(bp);
                    out.write(',');
                    out.write(p);
                    out.write(',');
                    out.write(sampled ? '1' : '0');
                    out.write('\n');
                    kept++;
                    keptKeys.add(key);
                    if (sampled) {
                        uniform++;
                    }
                }
            }
        }
        System.err.printf("thin    %d usable SNPs (%d dropped for a missing p-value)%n", usable, dropped);
        System.err.printf("thin    %d rows kept, of which %d form the uniform 1-in-%d sample%n",
                kept, uniform, every);
        return keptKeys;
    }

    // --- 3. Restrict the lead SNPs to those present in the thinned file ------

    private static void writeLeadSnps(Path target, List<Lead> leads, Set<String> thinnedKeys)
            throws IOException {
        long kept = 0;
        try (Writer out = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            out.write("CHR,BP,SNP,P,GENE\r\n");
            for (Lead lead : leads) {
                if (!thinnedKeys.contains(lead.key())) {
                    continue;
                }
                out.write(lead.chr() + "," + lead.bp() + "," + lead.snp() + ","
                        + lead.p() + "," + lead.gene() + "\r\n");
                kept++;
            }
        }
        System.err.printf("lead    %d lead SNPs retained%n", kept);
    }

    // --- Cell helpers --------------------------------------------------------

    private static boolean blank(Cell cell) {
        return cell == null || cell.getCellType() == CellType.BLANK;
    }

    /** Reads a whole-number cell as a {@code long}, accepting a numeric string. */
    private static long longOf(Cell cell) {
        if (cell.getCellType() == CellType.NUMERIC) {
            return (long) cell.getNumericCellValue();
        }
        return Long.parseLong(cell.getStringCellValue().trim());
    }

    /**
     * Formats a cell the way Python's {@code csv.writer} renders an openpyxl
     * value: empty for a missing cell, the literal for a string, and an integer
     * or {@link #pyRepr(float)} for a number.
     */
    private static String csvCell(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().format(PY_DATETIME)
                    : floatOf(cell);
            case BOOLEAN -> cell.getBooleanCellValue() ? "True" : "False";
            default -> "";
        };
    }

    /**
     * Formats a numeric cell the way Python's {@code str(openpyxl-value)} does:
     * an integral value reads as an integer, anything else through
     * {@link #pyRepr(double)}. Supplementary Table 10 stores the lead p-values
     * that underflow double precision as the integer {@code 0}.
     */
    private static String floatOf(Cell cell) {
        double value = cell.getNumericCellValue();
        if (value == Math.rint(value) && !Double.isInfinite(value)) {
            return Long.toString((long) value);
        }
        return pyRepr(value);
    }

    /**
     * Reproduces Python's {@code repr(float)} for a finite non-zero value:
     * fixed-point notation for decimal exponents in {@code [-4, 16]},
     * scientific notation otherwise, lower-case {@code e}, an exponent of at
     * least two digits, and a trailing {@code .0} only in fixed notation.
     */
    static String pyRepr(double value) {
        boolean negative = value < 0;
        String java = Double.toString(Math.abs(value));

        String digits;
        int decpt;
        int e = java.indexOf('E');
        if (e >= 0) {
            String mantissa = java.substring(0, e);
            int exp = Integer.parseInt(java.substring(e + 1));
            int dot = mantissa.indexOf('.');
            digits = mantissa.substring(0, dot) + mantissa.substring(dot + 1);
            decpt = dot + exp;
        } else {
            int dot = java.indexOf('.');
            digits = java.substring(0, dot) + java.substring(dot + 1);
            decpt = dot;
            int lead = 0;
            while (lead < digits.length() - 1 && digits.charAt(lead) == '0') {
                lead++;
                decpt--;
            }
            digits = digits.substring(lead);
        }
        int end = digits.length();
        while (end > 1 && digits.charAt(end - 1) == '0') {
            end--;
        }
        digits = digits.substring(0, end);

        String sign = negative ? "-" : "";
        if (decpt <= -4 || decpt > 16) {
            String mantissa = digits.substring(0, 1)
                    + (digits.length() > 1 ? "." + digits.substring(1) : "");
            int exp = decpt - 1;
            String exponent = Integer.toString(Math.abs(exp));
            if (exponent.length() < 2) {
                exponent = "0" + exponent;
            }
            return sign + mantissa + "e" + (exp < 0 ? "-" : "+") + exponent;
        }
        if (decpt <= 0) {
            return sign + "0." + "0".repeat(-decpt) + digits;
        }
        if (decpt >= digits.length()) {
            return sign + digits + "0".repeat(decpt - digits.length()) + ".0";
        }
        return sign + digits.substring(0, decpt) + "." + digits.substring(decpt);
    }
}
