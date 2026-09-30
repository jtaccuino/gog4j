# Human height GWAS — example data

Everything here derives from the GIANT consortium's genome-wide association
study of human height, published open access under **CC BY 4.0**:

> Yengo, L., Vedantam, S., Marouli, E., Sidorenko, J., Bartell, E., Sakaue, S.,
> *et al.* (2022). **A saturated map of common genetic variants associated with
> human height.** *Nature* **610**, 704–712.
> <https://doi.org/10.1038/s41586-022-05275-y>

Coordinates are **GRCh37/hg19**, matching the published release.

## Layout

| Path                         | Committed | Contents                                          |
|------------------------------|-----------|---------------------------------------------------|
| `height_gwas_thinned.csv.gz` | yes, 4.0 MB | the demo summary statistics                     |
| `height_lead_snps.csv`       | yes, 459 KB | the demo lead SNP table                         |
| `derive-demo-dataset.sh`     | yes       | derives both of the above from `upstream/`        |
| `DeriveGwas.java`            | yes       | the jbang derivation script (Apache POI)          |
| `upstream/`                  | **no**    | the unmodified published datasets, ~50 MB         |

`upstream/` is git-ignored: the files are large and freely re-downloadable, and
the script fetches them automatically when they are absent. They are kept here
rather than elsewhere so that the demo data and its source sit together.

The script and `DeriveGwas.java` are excluded from the Gradle build
(`gog4j-data/build.gradle`, `processResources`), so neither the 50 MB of source
data nor the derivation tooling is packaged into the module jar.

## Reproducing the demo data

```bash
cd gog4j-data/src/main/resources/examples/gwas
./derive-demo-dataset.sh            # download sources if needed, then derive
./derive-demo-dataset.sh --verify   # derive to a temp dir and diff against the committed files
```

Requires `bash`, `curl`, `gunzip`, and `jbang`. The derivation is a jbang script
(`DeriveGwas.java`) that reads the `.xlsx` with Apache POI, so the tooling is
JVM-only; jbang resolves its dependencies on first run. The verified output is:

```
ST10    12111 lead SNPs extracted
thin    1377294 usable SNPs (11 dropped for a missing p-value)
thin    467159 rows kept, of which 459098 form the uniform 1-in-3 sample
lead    12111 lead SNPs retained
verify  height_gwas_thinned.csv.gz matches the committed file
verify  height_lead_snps.csv matches the committed file
```

## Sources

**Summary statistics** — `GIANT_HEIGHT_YENGO_2022_GWAS_SUMMARY_STATS_ALL.gz`
(40,630,479 bytes, 1,377,305 SNPs) from the
[GIANT consortium data page](https://portals.broadinstitute.org/collaboration/giant/index.php/GIANT_consortium_data_files).
That URL now redirects; the script uses the current host directly. The release
excludes 23andMe participants, whose data requires a separate agreement, and
covers the ~1.39 M HapMap3 variants. Tab-separated, with columns
`SNPID RSID CHR POS EFFECT_ALLELE OTHER_ALLELE EFFECT_ALLELE_FREQ BETA SE P N`.

**Supplementary Table 10** — `41586_2022_5275_MOESM3_ESM.xlsx` (9,368,771 bytes)
from the article's supplementary information, sheet `ST10 - COJO - METAFE`:
the 12,111 independent genome-wide significant lead SNPs.

## Derivation, step by step

1. **Drop unusable rows.** 11 SNPs carry `NA` as their p-value, leaving
   **1,377,294**.
2. **Take a uniform sample.** Every third SNP is kept — **459,098** rows.
   The sample is deliberately *uniform*: thinning by p-value (keeping all strong
   signals plus a fraction of the rest) leaves a visible density step in the
   Manhattan plot exactly where the retention rule changes, which reads as a
   property of the data rather than of the preprocessing.
3. **Add back the lead SNPs.** All 12,111 published lead SNPs are retained
   regardless of the sampling, so the highlight layer is complete. This brings
   the file to **467,159** rows. The `SAMPLED` column records which rows belong
   to the uniform sample.
4. **Restrict the lead SNP table** to entries present in the thinned file.

### Columns of `height_gwas_thinned.csv.gz`

| Column    | Meaning                                                            |
|-----------|--------------------------------------------------------------------|
| `CHR`     | chromosome, 1–22                                                   |
| `BP`      | base-pair position (hg19)                                          |
| `P`       | association p-value, exactly as published                          |
| `SAMPLED` | `1` if the row belongs to the uniform one-in-three sample, else `0` |

`SAMPLED` matters for the quantile-quantile plot, which must run on an unbiased
sample; the extra lead SNPs would otherwise over-weight the extreme tail.

### Columns of `height_lead_snps.csv`

`CHR`, `BP` (hg19), `SNP` (rsID), `P`, `GENE` (nearest gene).

Note that `P` here comes from the **full** meta-analysis including 23andMe, so
it is generally stronger than the same variant's p-value in the summary
statistics. In the public data only 6,218 of these SNPs still clear
`P < 5e-8`; the examples highlight those, and `org.jtaccuino.gog.dflib.data.GwasDatasets`
documents why.

## No values are altered

The demo files carry the published p-values verbatim — the derivation only
selects rows, it never rewrites a value. 522 p-values are reported as `0` in the
source because the associations are stronger than IEEE-754 double precision can
represent; the plots place them at `-log10(4.94e-324) ≈ 323.3`, the
representational floor, and every other SNP at its exact `-log10(P)`. The
Manhattan plots fit that range with a **square-root axis**, not by clamping.
