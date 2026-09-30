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
package org.jtaccuino.gog.scale;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javafx.scene.paint.Color;
import org.jtaccuino.gog.MinMax;

/**
 * The public DSL for configuring axis scales, following the
 * {@code scale_*()} functions: break positions, labels, expansions, and
 * transforms for the continuous x and y axes, plus manual colour palettes.
 */
public class Scales {
    /** Utility class; not meant to be instantiated. */
    private Scales() {
    }

    /**
     * Explicitly sets the axis tick positions (breaks) for the X-axis.
     *
     * @param breaks the list of tick positions
     * @return a {@link ScaleConfigurator} for the given breaks
     */
    public static ScaleConfigurator scaleXContinuous(List<Double> breaks) {
        return plot -> plot.getScaleSpec().setXBreaks(breaks);
    }

    /**
     * Sets the axis domain expansion for the continuous X-axis, e.g.
     * {@code scaleXContinuous(expand = Expansion.none())} to make the data
     * reach the panel edges without padding.
     *
     * @param expand the expansion to apply to the X-axis domain
     * @return a {@link ScaleConfigurator} applying the expansion
     */
    public static ScaleConfigurator scaleXContinuous(Expansion expand) {
        return plot -> plot.getScaleSpec().setXExpand(expand);
    }

    /**
     * Sets the axis tick positions (breaks) for the X-axis together with the
     * labels drawn at them. Labels are paired positionally with the breaks, so
     * arbitrary text can sit at arbitrary continuous positions — as needed for
     * chromosome names on a genomic axis.
     *
     * @param breaks the list of tick positions
     * @param labels the label for each break, in the same order
     * @return a {@link ScaleConfigurator} for the given breaks and labels
     * @throws IllegalArgumentException if the two lists differ in length
     */
    public static ScaleConfigurator scaleXContinuous(List<Double> breaks, List<String> labels) {
        requireSameSize(breaks, labels);
        return plot -> {
            plot.getScaleSpec().setXBreaks(breaks);
            plot.getScaleSpec().setXLabels(labels);
        };
    }

    /**
     * Explicitly sets the axis tick positions (breaks) for the Y-axis.
     *
     * @param breaks the list of tick positions
     * @return a {@link ScaleConfigurator} for the given breaks
     */
    public static ScaleConfigurator scaleYContinuous(List<Double> breaks) {
        return plot -> plot.getScaleSpec().setYBreaks(breaks);
    }

    /**
     * Sets the axis domain expansion for the continuous Y-axis, e.g.
     * {@code scaleYContinuous(expand = Expansion.none())} to make the data
     * reach the panel edges without padding.
     *
     * @param expand the expansion to apply to the Y-axis domain
     * @return a {@link ScaleConfigurator} applying the expansion
     */
    public static ScaleConfigurator scaleYContinuous(Expansion expand) {
        return plot -> plot.getScaleSpec().setYExpand(expand);
    }

    /**
     * Sets the axis tick positions (breaks) for the Y-axis together with the
     * labels drawn at them.
     *
     * @param breaks the list of tick positions
     * @param labels the label for each break, in the same order
     * @return a {@link ScaleConfigurator} for the given breaks and labels
     * @throws IllegalArgumentException if the two lists differ in length
     */
    public static ScaleConfigurator scaleYContinuous(List<Double> breaks, List<String> labels) {
        requireSameSize(breaks, labels);
        return plot -> {
            plot.getScaleSpec().setYBreaks(breaks);
            plot.getScaleSpec().setYLabels(labels);
        };
    }

    /**
     * Renders the Y axis on a base-10 logarithmic scale. Every observation keeps
     * its value; only the spacing changes, so no data is clamped to fit the panel.
     * Requires strictly positive values.
     *
     * @return a {@link ScaleConfigurator} applying the transform
     */
    public static ScaleConfigurator scaleYLog10() {
        return plot -> plot.getScaleSpec().setYTransform(ScaleTransform.LOG10);
    }

    /**
     * Renders the Y axis on a square-root scale, compressing a long upper tail
     * while keeping the range near zero legible. Suits quantities that span
     * orders of magnitude but must still show their small values — where a
     * logarithm would push them towards negative infinity.
     *
     * @return a {@link ScaleConfigurator} applying the transform
     */
    public static ScaleConfigurator scaleYSqrt() {
        return plot -> plot.getScaleSpec().setYTransform(ScaleTransform.SQRT);
    }

    /**
     * Renders the Y axis on a square-root scale with explicit tick positions.
     *
     * @param breaks the tick positions, in data units
     * @return a {@link ScaleConfigurator} applying the transform and breaks
     */
    public static ScaleConfigurator scaleYSqrt(List<Double> breaks) {
        return plot -> {
            plot.getScaleSpec().setYTransform(ScaleTransform.SQRT);
            plot.getScaleSpec().setYBreaks(breaks);
        };
    }

    /**
     * Renders the X axis running in the reverse direction, so the largest value
     * sits at the left, mirroring {@code scaleXReverse()}.
     *
     * @return a {@link ScaleConfigurator} inverting the X axis direction
     */
    public static ScaleConfigurator scaleXReverse() {
        return plot -> plot.getScaleSpec().setXReverse(true);
    }

    /**
     * Renders the Y axis running in the reverse direction, so the largest value
     * sits at the bottom, mirroring {@code scaleYReverse()}.
     *
     * @return a {@link ScaleConfigurator} inverting the Y axis direction
     */
    public static ScaleConfigurator scaleYReverse() {
        return plot -> plot.getScaleSpec().setYReverse(true);
    }

    /**
     * Renders the X axis along the top edge of the panel instead of the
     * bottom, mirroring {@code scaleXContinuous(position = "top")}.
     *
     * @param position the axis position ({@link AxisPosition#TOP} or
     *        {@link AxisPosition#BOTTOM})
     * @return a {@link ScaleConfigurator} repositioning the X axis
     */
    public static ScaleConfigurator scaleXPosition(AxisPosition position) {
        return plot -> plot.getScaleSpec().setXAxisPosition(position);
    }

    /**
     * Renders the Y axis along the right edge of the panel instead of the
     * left, mirroring {@code scaleYContinuous(position = "right")}.
     *
     * @param position the axis position ({@link AxisPosition#RIGHT} or
     *        {@link AxisPosition#LEFT})
     * @return a {@link ScaleConfigurator} repositioning the Y axis
     */
    public static ScaleConfigurator scaleYPosition(AxisPosition position) {
        return plot -> plot.getScaleSpec().setYAxisPosition(position);
    }

    /**
     * Sets explicit X-axis limits, restricting the plotted data range to
     * {@code [min, max]}, mirroring {@code scaleXContinuous(limits = ...)}.
     *
     * @param min the lower X limit
     * @param max the upper X limit
     * @return a {@link ScaleConfigurator} applying the X limits
     */
    public static ScaleConfigurator scaleXLimits(double min, double max) {
        return plot -> plot.getScaleSpec().setXLimits(new MinMax(min, max));
    }

    /**
     * Sets explicit Y-axis limits, restricting the plotted data range to
     * {@code [min, max]}, mirroring {@code scaleYContinuous(limits = ...)}.
     *
     * @param min the lower Y limit
     * @param max the upper Y limit
     * @return a {@link ScaleConfigurator} applying the Y limits
     */
    public static ScaleConfigurator scaleYLimits(double min, double max) {
        return plot -> plot.getScaleSpec().setYLimits(new MinMax(min, max));
    }

    /**
     * Explicitly sets the axis tick positions (breaks) for the Z-axis of a 3D
     * coordinate system, mirroring {@code scaleZContinuous()}.
     *
     * @param breaks the list of tick positions
     * @return a {@link ScaleConfigurator} for the given breaks
     */
    public static ScaleConfigurator scaleZContinuous(List<Double> breaks) {
        return plot -> plot.getScaleSpec().setZBreaks(breaks);
    }

    /**
     * Sets the axis domain expansion for the continuous Z-axis, e.g.
     * {@code scaleZContinuous(expand = Expansion.none())} to make the data
     * reach the cube faces without padding.
     *
     * @param expand the expansion to apply to the Z-axis domain
     * @return a {@link ScaleConfigurator} applying the expansion
     */
    public static ScaleConfigurator scaleZContinuous(Expansion expand) {
        return plot -> plot.getScaleSpec().setZExpand(expand);
    }

    /**
     * Sets the axis tick positions (breaks) for the Z-axis together with the
     * labels drawn at them, mirroring {@code scaleZContinuous(breaks, labels)}.
     *
     * @param breaks the list of tick positions
     * @param labels the label for each break, in the same order
     * @return a {@link ScaleConfigurator} for the given breaks and labels
     * @throws IllegalArgumentException if the two lists differ in length
     */
    public static ScaleConfigurator scaleZContinuous(List<Double> breaks, List<String> labels) {
        requireSameSize(breaks, labels);
        return plot -> {
            plot.getScaleSpec().setZBreaks(breaks);
            plot.getScaleSpec().setZLabels(labels);
        };
    }

    /**
     * Explicitly sets the tick positions (breaks) for a discrete Z-axis, whose
     * categories are labelled by the values themselves, mirroring
     * {@code scaleZDiscrete()}.
     *
     * @param breaks the list of category positions
     * @return a {@link ScaleConfigurator} for the given breaks
     */
    public static ScaleConfigurator scaleZDiscrete(List<Double> breaks) {
        return plot -> plot.getScaleSpec().setZBreaks(breaks);
    }

    /**
     * Sets explicit Z-axis limits, restricting the plotted data range to
     * {@code [min, max]}, mirroring {@code zlim()}.
     *
     * @param min the lower Z limit
     * @param max the upper Z limit
     * @return a {@link ScaleConfigurator} applying the Z limits
     */
    public static ScaleConfigurator zlim(double min, double max) {
        return plot -> plot.getScaleSpec().setZLimits(new MinMax(min, max));
    }

    /**
     * Renders the X axis on a base-10 logarithmic scale.
     *
     * @return a {@link ScaleConfigurator} applying the transform
     */
    public static ScaleConfigurator scaleXLog10() {
        return plot -> plot.getScaleSpec().setXTransform(ScaleTransform.LOG10);
    }

    /**
     * Renders the X axis on a square-root scale.
     *
     * @return a {@link ScaleConfigurator} applying the transform
     */
    public static ScaleConfigurator scaleXSqrt() {
        return plot -> plot.getScaleSpec().setXTransform(ScaleTransform.SQRT);
    }

    /**
     * Assigns explicit colours to the categories of the {@code color}/{@code fill}
     * aesthetic, overriding the theme's automatic categorical palette. Each {@link ColorManualScale#color
     * color(category, colour)} call adds one category colour, mirroring
     * {@code scaleColorManual(values = c(a = "grey", b = "skyblue"))}:
     * <pre>{@code
     * scaleColorManual()
     *     .color("a", Color.GREY)
     *     .color("b", Color.SKYBLUE)
     * }</pre>
     *
     * @return a {@link ColorManualScale} applying the palette
     */
    public static ColorManualScale scaleColorManual() {
        return new ColorManualScale();
    }

    /**
     * Assigns explicit point shapes to the categories of the {@code shape}
     * aesthetic, overriding the automatic shape cycle — the Java equivalent of
     * {@code scaleShapeManual(values = ...)}:
     * <pre>{@code
     * scaleShapeManual()
     *     .shape("4", PointShape.CIRCLE)
     *     .shape("8", PointShape.DIAMOND)
     * }</pre>
     *
     * @return a {@link ShapeManualScale} applying the shape map
     */
    public static ShapeManualScale scaleShapeManual() {
        return new ShapeManualScale();
    }

    /**
     * Returns a fluent builder that assigns explicit radii to the categories of
     * the {@code size} aesthetic, overriding the continuous size scaling for
     * those categories — the Java equivalent of
     * {@code scaleSizeManual(values = ...)}:
     * <pre>{@code
     * scaleSizeManual()
     *     .size("4", 2.5)
     *     .size("8", 6.0)
     * }</pre>
     *
     * @return a {@link SizeManualScale} applying the size map
     */
    public static SizeManualScale scaleSizeManual() {
        return new SizeManualScale();
    }

    /**
     * Returns a fluent builder that assigns explicit dash patterns to the
     * categories of the {@code linetype} aesthetic, overriding the automatic
     * line-type cycle — the Java equivalent of
     * {@code Scales.scaleLinetypeManual(values = ...)}:
     * <pre>{@code
     * scaleLinetypeManual()
     *     .linetype("4", DASHED)
     *     .linetype("6", 2.0, 2.0)
     *     .linetype("8", LineType.of(6.0, 2.0, 1.0, 2.0))
     * }</pre>
     *
     * @return a {@link LinetypeManualScale} applying the line-type map
     */
    public static LinetypeManualScale scaleLinetypeManual() {
        return new LinetypeManualScale();
    }

    /**
     * Assigns explicit dash patterns to the categories of the {@code linetype}
     * aesthetic, overriding the automatic line-type cycle — the Java equivalent
     * of {@code Scales.scaleLinetypeManual(values = ...)}.
     * <p>
     * The map is keyed by the category's string form; each value is the
     * alternating on/off dash lengths (empty means solid).
     *
     * @param values category name to dash patterns (as lists)
     * @return a {@link ScaleConfigurator} applying the line-type map
     */
    public static ScaleConfigurator scaleLinetypeManual(Map<String, List<Double>> values) {
        return plot -> plot.getScaleSpec().setManualLinetypes(values);
    }

    private static void requireSameSize(List<Double> breaks, List<String> labels) {
        if (breaks == null || labels == null || breaks.size() != labels.size()) {
            throw new IllegalArgumentException(
                    "breaks and labels must be non-null and of equal length, got "
                    + (breaks == null ? "null" : String.valueOf(breaks.size())) + " breaks and "
                    + (labels == null ? "null" : String.valueOf(labels.size())) + " labels");
        }
    }

    /**
     * Selects the viridis colour ramp for a continuous {@code color} scale,
     * mirroring {@code scaleColorViridisC()}.
     *
     * @return a {@link ScaleConfigurator} selecting the viridis ramp
     */
    public static ScaleConfigurator scaleColorViridisC() {
        return plot -> {
            plot.getScaleSpec().setContinuousColorMap("viridis");
            plot.getScaleSpec().setDiscreteColorRamp(false);
            plot.getScaleSpec().setContinuousGradient(null);
        };
    }

    /**
     * Selects the viridis palette for a discrete {@code color} scale, mirroring
     * {@code scaleColorViridisD()}.
     *
     * @return a {@link ScaleConfigurator} selecting the viridis palette
     */
    public static ScaleConfigurator scaleColorViridisD() {
        return plot -> {
            plot.getScaleSpec().setContinuousColorMap("viridis");
            plot.getScaleSpec().setDiscreteColorRamp(true);
        };
    }

    /**
     * Selects a named ColorBrewer palette for a discrete {@code color} scale,
     * mirroring {@code scaleColorBrewer(palette = ...)}.
     *
     * @param palette the palette name, e.g. {@code "Set1"}, {@code "Dark2"},
     *                {@code "RdYlBu"}
     * @return a {@link ScaleConfigurator} selecting the brewer palette
     * @throws IllegalArgumentException if the palette name is unknown
     */
    public static ScaleConfigurator scaleColorBrewer(String palette) {
        requirePalette(palette);
        return plot -> {
            plot.getScaleSpec().setContinuousColorMap(palette);
            plot.getScaleSpec().setDiscreteColorRamp(true);
        };
    }

    /**
     * Selects the viridis colour ramp for a continuous {@code fill} scale,
     * mirroring {@code scaleFillViridisC()}.
     *
     * @return a {@link ScaleConfigurator} selecting the viridis ramp
     */
    public static ScaleConfigurator scaleFillViridisC() {
        return scaleColorViridisC();
    }

    /**
     * Selects the viridis palette for a discrete {@code fill} scale, mirroring
     * {@code scaleFillViridisD()}.
     *
     * @return a {@link ScaleConfigurator} selecting the viridis palette
     */
    public static ScaleConfigurator scaleFillViridisD() {
        return scaleColorViridisD();
    }

    /**
     * Bins a continuous {@code color} scale into discrete steps, mirroring
     * {@code scaleColorBinned(n = ...)}; the colour range becomes
     * {@code n} evenly-spaced steps and the guide renders as a colorsteps bar.
     *
     * @param n    the number of discrete colour steps
     * @return a {@link ScaleConfigurator} binning the colour scale
     */
    public static ScaleConfigurator scaleColorBinned(int n) {
        return plot -> {
            plot.getScaleSpec().setColorSteps(n);
            plot.getScaleSpec().setDiscreteColorRamp(false);
        };
    }

    /**
     * Bins a continuous {@code fill} scale into discrete steps, mirroring
     * {@code scaleFillBinned(n = ...)}.
     *
     * @param n    the number of discrete colour steps
     * @return a {@link ScaleConfigurator} binning the fill scale
     */
    public static ScaleConfigurator scaleFillBinned(int n) {
        return scaleColorBinned(n);
    }

    /**
     * Selects a named ColorBrewer palette for a discrete {@code fill} scale.
     *
     * @param palette the palette name, e.g. {@code "Set1"}
     * @return a {@link ScaleConfigurator} selecting the brewer palette
     * @throws IllegalArgumentException if the palette name is unknown
     */
    public static ScaleConfigurator scaleFillBrewer(String palette) {
        return scaleColorBrewer(palette);
    }

    /**
     * Interpolates a two-stop {@code color} gradient between low and high
     * colours, mirroring {@code scaleColorGradient(low, high)}.
     *
     * @param low  the colour at the low end of the range
     * @param high the colour at the high end of the range
     * @return a {@link ScaleConfigurator} applying the gradient
     */
    public static ScaleConfigurator scaleColorGradient(Color low, Color high) {
        requireGradientColors(low, high);
        return plot -> {
            plot.getScaleSpec().setContinuousGradient(List.of(low, high));
        };
    }

    /**
     * Interpolates a three-stop {@code color} gradient through an explicit mid
     * colour, mirroring {@code scaleColorGradient2(low, mid, high)}.
     *
     * @param low  the colour at the low end of the range
     * @param mid  the colour at the midpoint of the range
     * @param high the colour at the high end of the range
     * @return a {@link ScaleConfigurator} applying the gradient
     */
    public static ScaleConfigurator scaleColorGradient2(Color low, Color mid, Color high) {
        requireGradientColors(low, mid, high);
        return plot -> {
            plot.getScaleSpec().setContinuousGradient(List.of(low, mid, high));
        };
    }

    /**
     * Interpolates a {@code colors} gradient through an arbitrary list of
     * colours, mirroring {@code scaleColorGradientN(colours = ...)}.
     *
     * @param colors the ramp colours in low-to-high order
     * @return a {@link ScaleConfigurator} applying the gradient
     */
    public static ScaleConfigurator scaleColorGradientN(List<Color> colors) {
        if (colors == null || colors.isEmpty()) {
            throw new IllegalArgumentException("gradient colours must be non-null and non-empty");
        }
        return plot -> {
            plot.getScaleSpec().setContinuousGradient(new ArrayList<>(colors));
        };
    }

    /**
     * Interpolates a two-stop {@code fill} gradient between low and high
     * colours.
     *
     * @param low  the colour at the low end of the range
     * @param high the colour at the high end of the range
     * @return a {@link ScaleConfigurator} applying the gradient
     */
    public static ScaleConfigurator scaleFillGradient(Color low, Color high) {
        return scaleColorGradient(low, high);
    }

    /**
     * Interpolates a three-stop {@code fill} gradient through an explicit mid
     * colour.
     *
     * @param low  the colour at the low end of the range
     * @param mid  the colour at the midpoint of the range
     * @param high the colour at the high end of the range
     * @return a {@link ScaleConfigurator} applying the gradient
     */
    public static ScaleConfigurator scaleFillGradient2(Color low, Color mid, Color high) {
        return scaleColorGradient2(low, mid, high);
    }

    /**
     * Interpolates a {@code fill} gradient through an arbitrary list of colours.
     *
     * @param colors the ramp colours in low-to-high order
     * @return a {@link ScaleConfigurator} applying the gradient
     */
    public static ScaleConfigurator scaleFillGradientN(List<Color> colors) {
        return scaleColorGradientN(colors);
    }

    private static void requirePalette(String palette) {
        if (palette == null || !Palettes.exists(palette)) {
            throw new IllegalArgumentException("unknown colour palette: "
                    + (palette == null ? "null" : palette));
        }
    }

    private static void requireGradientColors(Color... colors) {
        for (var c : colors) {
            if (c == null) {
                throw new IllegalArgumentException("gradient colours must be non-null");
            }
        }
    }
}
