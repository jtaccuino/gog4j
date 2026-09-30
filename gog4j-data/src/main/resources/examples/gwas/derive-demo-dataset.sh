#!/usr/bin/env bash
#
# Derives the committed demo datasets from the published GIANT height GWAS.
#
#   height_gwas_thinned.csv.gz   <- GIANT multi-ancestry summary statistics
#   height_lead_snps.csv         <- Supplementary Table 10 of the paper
#
# Source publication (open access, CC BY 4.0):
#   Yengo L, Vedantam S, Marouli E, Sidorenko J, Bartell E, Sakaue S, et al.
#   A saturated map of common genetic variants associated with human height.
#   Nature 610, 704-712 (2022).  https://doi.org/10.1038/s41586-022-05275-y
#
# Usage:
#   ./derive-demo-dataset.sh            # download sources if absent, then derive
#   ./derive-demo-dataset.sh --verify   # derive into a temp dir and diff against
#                                       # the committed files
#
# Requirements: bash, curl, gunzip, and jbang. The derivation itself is a jbang
# script (DeriveGwas.java, Apache POI for the .xlsx), so the tooling is JVM-only;
# jbang resolves its dependencies on first run.
#
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
UPSTREAM="$HERE/upstream"

# --- Source addresses -------------------------------------------------------
# The URL printed in the paper (portals.broadinstitute.org) now 301-redirects
# to the host below; it is hardcoded so the script does not depend on the
# redirect surviving.
SUMSTATS_URL="https://giant-consortium.web.broadinstitute.org/images/4/4e/GIANT_HEIGHT_YENGO_2022_GWAS_SUMMARY_STATS_ALL.gz"
SUPPL_URL="https://static-content.springer.com/esm/art%3A10.1038%2Fs41586-022-05275-y/MediaObjects/41586_2022_5275_MOESM3_ESM.xlsx"

SUMSTATS="$UPSTREAM/GIANT_HEIGHT_YENGO_2022_GWAS_SUMMARY_STATS_ALL.gz"
SUPPL="$UPSTREAM/41586_2022_5275_MOESM3_ESM.xlsx"

# --- Derivation parameters --------------------------------------------------
# Keep every Nth SNP. The sample must be uniform: thinning by p-value instead
# would leave a visible density step in the Manhattan plot exactly where the
# retention rule changes, which reads as a feature of the data.
SAMPLE_EVERY=3

verify=0
[[ "${1:-}" == "--verify" ]] && verify=1

outdir="$HERE"
if [[ $verify -eq 1 ]]; then
  outdir="$(mktemp -d)"
  trap 'rm -rf "$outdir"' EXIT
fi

# --- 1. Fetch the published sources ----------------------------------------
mkdir -p "$UPSTREAM"
fetch() {
  local url="$1" dest="$2"
  if [[ -s "$dest" ]]; then
    echo "have    $(basename "$dest") ($(wc -c < "$dest" | tr -d ' ') bytes)"
  else
    echo "fetch   $(basename "$dest")"
    curl -fsSL --retry 3 -o "$dest" "$url"
  fi
}
fetch "$SUMSTATS_URL" "$SUMSTATS"
fetch "$SUPPL_URL" "$SUPPL"

# --- 2. Derive the demo files ----------------------------------------------
# DeriveGwas.java extracts Supplementary Table 10, thins the summary statistics
# (uniform 1-in-N sample plus the published lead SNPs), and writes both committed
# files. Kept as a jbang script so the derivation needs no Python.
jbang --quiet "$HERE/DeriveGwas.java" \
  --sumstats "$SUMSTATS" \
  --suppl "$SUPPL" \
  --out "$outdir" \
  --every "$SAMPLE_EVERY"

# --- 3. Report or verify ----------------------------------------------------
if [[ $verify -eq 1 ]]; then
  status=0
  for f in height_gwas_thinned.csv.gz height_lead_snps.csv; do
    # compare content, not the gzip container, whose header carries a timestamp
    if [[ "$f" == *.gz ]]; then
      if gunzip -c "$outdir/$f" | diff -q - <(gunzip -c "$HERE/$f") >/dev/null; then
        echo "verify  $f matches the committed file"
      else
        echo "verify  $f DIFFERS from the committed file"; status=1
      fi
    elif diff -q "$outdir/$f" "$HERE/$f" >/dev/null; then
      echo "verify  $f matches the committed file"
    else
      echo "verify  $f DIFFERS from the committed file"; status=1
    fi
  done
  exit $status
fi

echo "wrote   $outdir/height_gwas_thinned.csv.gz"
echo "wrote   $outdir/height_lead_snps.csv"
