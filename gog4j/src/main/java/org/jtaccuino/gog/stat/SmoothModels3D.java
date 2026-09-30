/*
 * Copyright 2026 JTaccuino project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jtaccuino.gog.stat;

import java.util.Arrays;

/**
 * Two-dimensional local-estimation helpers for the {@code Stats.smooth3d()}
 * and {@code Geoms.smooth3d()} statistics and geometries.
 * <p>
 * Supports a basic LOESS surface (local weighted least-squares plane) and a
 * global linear-model plane {@code z ~ x + y}, producing fitted values and
 * approximate standard errors.
 */
public final class SmoothModels3D {

    private SmoothModels3D() {
    }

    /**
     * Result of a 2-D smooth fit over a set of evaluation points.
     *
     * @param fitted the fitted value at each evaluation point
     * @param se the approximate standard error at each evaluation point
     */
    @SuppressWarnings("ArrayRecordComponent") // read-only carrier
    public record Fit(double[] fitted, double[] se) {
    }

    /**
     * Fits a global linear plane {@code z ~ x + y} and predicts at the
     * evaluation points.
     *
     * @param x   data x
     * @param y   data y
     * @param z   data z
     * @param ex  evaluation x
     * @param ey  evaluation y
     * @return fitted values and per-point standard errors
     */
    public static Fit fitPlane(double[] x, double[] y, double[] z, double[] ex, double[] ey) {
        int n = x.length;
        double sx = 0, sy = 0, sz = 0;
        double sxx = 0, syy = 0, sxy = 0, sxz = 0, syz = 0;
        for (int i = 0; i < n; i++) {
            sx += x[i];
            sy += y[i];
            sz += z[i];
            sxx += x[i] * x[i];
            syy += y[i] * y[i];
            sxy += x[i] * y[i];
            sxz += x[i] * z[i];
            syz += y[i] * z[i];
        }
        // Normal equations for z = b0 + b1*x + b2*y
        double[][] a = {
                {n, sx, sy},
                {sx, sxx, sxy},
                {sy, sxy, syy}
        };
        double[] rhs = {sz, sxz, syz};
        double[] coef = solve3(a, rhs);
        if (coef == null) {
            // Degenerate; return all z means.
            double mean = sz / n;
            double[] f = new double[ex.length];
            Arrays.fill(f, mean);
            return new Fit(f, new double[ex.length]);
        }
        double[] f = new double[ex.length];
        double[] se = new double[ex.length];
        double mse = 0;
        for (int i = 0; i < n; i++) {
            double pred = coef[0] + coef[1] * x[i] + coef[2] * y[i];
            double r = z[i] - pred;
            mse += r * r;
        }
        mse = n > 3 ? mse / (n - 3) : 0;
        double[] cxx = invert3(a);
        for (int i = 0; i < ex.length; i++) {
            f[i] = coef[0] + coef[1] * ex[i] + coef[2] * ey[i];
            // Standard error of the predicted mean.
            double v = cxx[0] + cxx[4] + cxx[8]
                    + ex[i] * (cxx[1] + cxx[3]) + ey[i] * (cxx[2] + cxx[6])
                    + ex[i] * ex[i] * cxx[4] + 2 * ex[i] * ey[i] * cxx[5]
                    + ey[i] * ey[i] * cxx[8];
            se[i] = Math.sqrt(Math.max(0, mse) * Math.max(0, v));
        }
        return new Fit(f, se);
    }

    /**
     * LOESS surface at evaluation points using a Gaussian bandwidth.
     *
     * @param x   data x
     * @param y   data y
     * @param z   data z
     * @param ex  evaluation x
     * @param ey  evaluation y
     * @param span  loess span (0–1)
     * @return fitted values and approximate per-point standard errors
     */
    public static Fit fitLoess(double[] x, double[] y, double[] z, double[] ex, double[] ey, double span) {
        int nd = x.length;
        int ne = ex.length;
        // Compute pairwise distances and bandwidth.
        double maxD = 0;
        double[] dist2 = new double[nd * ne];
        for (int i = 0; i < ne; i++) {
            for (int j = 0; j < nd; j++) {
                double dx = x[j] - ex[i];
                double dy = y[j] - ey[i];
                dist2[i * nd + j] = dx * dx + dy * dy;
                maxD = Math.max(maxD, dist2[i * nd + j]);
            }
        }
        double bw = maxD * Math.pow(span, 2); // square-bw for Gaussian

        double[] fitted = new double[ne];
        double[] se = new double[ne];
        for (int i = 0; i < ne; i++) {
            // Local least-squares plane z = a0 + a1*x + a2*y with Gaussian weights.
            double wsum = 0, xs = 0, ys = 0, zs = 0;
            double wxx = 0, wyy = 0, wxy = 0, wxz = 0, wyz = 0;
            for (int j = 0; j < nd; j++) {
                double w = bw == 0 ? 1 : Math.exp(-dist2[i * nd + j] / bw);
                wsum += w;
                xs += w * x[j];
                ys += w * y[j];
                zs += w * z[j];
                wxx += w * x[j] * x[j];
                wyy += w * y[j] * y[j];
                wxy += w * x[j] * y[j];
                wxz += w * x[j] * z[j];
                wyz += w * y[j] * z[j];
            }
            if (wsum == 0) {
                fitted[i] = 0;
                continue;
            }
            double[][] a = {
                    {wsum, xs, ys},
                    {xs, wxx, wxy},
                    {ys, wxy, wyy}
            };
            double[] rhs = {zs, wxz, wyz};
            double[] coef = solve3(a, rhs);
            if (coef == null) {
                fitted[i] = zs / wsum;
            } else {
                fitted[i] = coef[0] + coef[1] * ex[i] + coef[2] * ey[i];
            }
            // Local residual variance.
            double mse = 0;
            if (coef != null) {
                for (int j = 0; j < nd; j++) {
                    double pred = coef[0] + coef[1] * x[j] + coef[2] * y[j];
                    double r = z[j] - pred;
                    mse += r * r;
                }
                mse = nd > 3 ? mse / (nd - 3) : 0;
            }
            double[] cxx = invert3(a);
            if (cxx != null) {
                double v = cxx[0] + cxx[4] + cxx[8]
                        + ex[i] * (cxx[1] + cxx[3]) + ey[i] * (cxx[2] + cxx[6])
                        + ex[i] * ex[i] * cxx[4] + 2 * ex[i] * ey[i] * cxx[5]
                        + ey[i] * ey[i] * cxx[8];
                se[i] = Math.sqrt(Math.max(0, mse) * Math.max(0, v));
            }
        }
        return new Fit(fitted, se);
    }

    /** Solves a 3×3 linear system a*x=rhs. */
    private static double[] solve3(double[][] a, double[] rhs) {
        double det = a[0][0] * (a[1][1] * a[2][2] - a[1][2] * a[2][1])
                - a[0][1] * (a[1][0] * a[2][2] - a[1][2] * a[2][0])
                + a[0][2] * (a[1][0] * a[2][1] - a[1][1] * a[2][0]);
        if (Math.abs(det) < 1e-12) {
            return null;
        }
        double inv = 1.0 / det;
        double[] x = new double[3];
        x[0] = inv * (rhs[0] * (a[1][1] * a[2][2] - a[1][2] * a[2][1])
                - a[0][1] * (rhs[1] * a[2][2] - a[1][2] * rhs[2])
                + a[0][2] * (rhs[1] * a[2][1] - a[1][1] * rhs[2]));
        x[1] = inv * (a[0][0] * (rhs[1] * a[2][2] - a[1][2] * rhs[2])
                - rhs[0] * (a[1][0] * a[2][2] - a[1][2] * a[2][0])
                + a[0][2] * (a[1][0] * rhs[2] - rhs[1] * a[2][0]));
        x[2] = inv * (a[0][0] * (a[1][1] * rhs[2] - rhs[1] * a[2][1])
                - a[0][1] * (a[1][0] * rhs[2] - rhs[1] * a[2][0])
                + rhs[0] * (a[1][0] * a[2][1] - a[1][1] * a[2][0]));
        return x;
    }

    /** Inverse of a 3×3 matrix, flattened row-major (9 elements). */
    private static double[] invert3(double[][] a) {
        double det = a[0][0] * (a[1][1] * a[2][2] - a[1][2] * a[2][1])
                - a[0][1] * (a[1][0] * a[2][2] - a[1][2] * a[2][0])
                + a[0][2] * (a[1][0] * a[2][1] - a[1][1] * a[2][0]);
        if (Math.abs(det) < 1e-12) {
            return null;
        }
        double inv = 1.0 / det;
        return new double[]{
                (a[1][1] * a[2][2] - a[1][2] * a[2][1]) * inv,
                (a[0][2] * a[2][1] - a[0][1] * a[2][2]) * inv,
                (a[0][1] * a[1][2] - a[0][2] * a[1][1]) * inv,
                (a[1][2] * a[2][0] - a[1][0] * a[2][2]) * inv,
                (a[0][0] * a[2][2] - a[0][2] * a[2][0]) * inv,
                (a[0][2] * a[1][0] - a[0][0] * a[1][2]) * inv,
                (a[1][0] * a[2][1] - a[1][1] * a[2][0]) * inv,
                (a[0][1] * a[2][0] - a[0][0] * a[2][1]) * inv,
                (a[0][0] * a[1][1] - a[0][1] * a[1][0]) * inv
        };
    }
}
