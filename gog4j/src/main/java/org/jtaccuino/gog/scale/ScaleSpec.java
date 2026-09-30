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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javafx.scene.paint.Color;
import org.jtaccuino.gog.MinMax;
import org.jtaccuino.gog.layer.PointShape;

/**
 * Specifications for axis scale breaks, allowing custom tick positions
 * for the X and Y axes, optional tick labels paired with those positions,
 * and an explicit categorical colour palette.
 */
public class ScaleSpec {
    /**
     * Creates an empty scale specification: no custom breaks or labels, the
     * linear identity transform, and the default expansion on both axes.
     */
    public ScaleSpec() {
    }

    /**
     * Copies every configured field of {@code other} into this specification,
     * deep-copying the collection-valued state. Used by
     * {@code PlotDescriptor.copy()} to give a derived descriptor its own
     * isolated scale configuration.
     *
     * @param other the specification to copy from
     */
    public void copyFrom(ScaleSpec other) {
        this.xBreaks = other.xBreaks == null ? null : List.copyOf(other.xBreaks);
        this.yBreaks = other.yBreaks == null ? null : List.copyOf(other.yBreaks);
        this.zBreaks = other.zBreaks == null ? null : List.copyOf(other.zBreaks);
        this.xLabels = other.xLabels == null ? null : List.copyOf(other.xLabels);
        this.yLabels = other.yLabels == null ? null : List.copyOf(other.yLabels);
        this.zLabels = other.zLabels == null ? null : List.copyOf(other.zLabels);
        this.manualColors = other.manualColors == null ? null : new LinkedHashMap<>(other.manualColors);
        this.continuousColorMap = other.continuousColorMap;
        this.continuousGradient = other.continuousGradient == null ? null : List.copyOf(other.continuousGradient);
        this.discreteColorRamp = other.discreteColorRamp;
        this.colorSteps = other.colorSteps;
        this.manualShapes = other.manualShapes == null ? null : new LinkedHashMap<>(other.manualShapes);
        if (other.manualLinetypes == null) {
            this.manualLinetypes = null;
        } else {
            var copy = new LinkedHashMap<String, List<Double>>();
            for (var e : other.manualLinetypes.entrySet()) {
                copy.put(e.getKey(), e.getValue() == null ? null : List.copyOf(e.getValue()));
            }
            this.manualLinetypes = copy;
        }
        this.manualSizes = other.manualSizes == null ? null : new LinkedHashMap<>(other.manualSizes);
        this.xTransform = other.xTransform;
        this.yTransform = other.yTransform;
        this.xExpand = other.xExpand;
        this.yExpand = other.yExpand;
        this.xReverse = other.xReverse;
        this.yReverse = other.yReverse;
        this.xLimits = other.xLimits;
        this.yLimits = other.yLimits;
        this.zLimits = other.zLimits;
        this.xAxisPosition = other.xAxisPosition;
        this.yAxisPosition = other.yAxisPosition;
    }

    private List<Double> xBreaks = null;
    private List<Double> yBreaks = null;
    private List<Double> zBreaks = null;
    private List<String> xLabels = null;
    private List<String> yLabels = null;
    private List<String> zLabels = null;
    private Map<String, Color> manualColors = null;
    private String continuousColorMap = null;
    private List<Color> continuousGradient = null;
    private boolean discreteColorRamp = false;
    private Integer colorSteps = null;
    private Map<String, PointShape> manualShapes = null;
    private Map<String, List<Double>> manualLinetypes = null;
    private Map<String, Double> manualSizes = null;
    private ScaleTransform xTransform = ScaleTransform.IDENTITY;
    private ScaleTransform yTransform = ScaleTransform.IDENTITY;
    private Expansion xExpand = Expansion.DEFAULT;
    private Expansion yExpand = Expansion.DEFAULT;
    private Expansion zExpand = Expansion.DEFAULT;
    private boolean xReverse = false;
    private boolean yReverse = false;
    private MinMax xLimits = null;
    private MinMax yLimits = null;
    private MinMax zLimits = null;
    private AxisPosition xAxisPosition = AxisPosition.BOTTOM;
    private AxisPosition yAxisPosition = AxisPosition.LEFT;

    /**
     * Returns the custom X-axis break positions.
     *
     * @return list of break values, or {@code null} if not set
     */
    public List<Double> getXBreaks() { return xBreaks; }

    /**
     * Sets custom break positions for the X-axis.
     *
     * @param xBreaks list of break values
     */
    public void setXBreaks(List<Double> xBreaks) { this.xBreaks = xBreaks; }

    /**
     * Returns the custom Y-axis break positions.
     *
     * @return list of break values, or {@code null} if not set
     */
    public List<Double> getYBreaks() { return yBreaks; }

    /**
     * Sets custom break positions for the Y-axis.
     *
     * @param yBreaks list of break values
     */
    public void setYBreaks(List<Double> yBreaks) { this.yBreaks = yBreaks; }

    /**
     * Returns the custom Z-axis break positions.
     *
     * @return list of break values, or {@code null} if not set
     */
    public List<Double> getZBreaks() { return zBreaks; }

    /**
     * Sets custom break positions for the Z-axis.
     *
     * @param zBreaks list of break values
     */
    public void setZBreaks(List<Double> zBreaks) { this.zBreaks = zBreaks; }

    /**
     * Returns the tick labels paired positionally with {@link #getXBreaks()}.
     *
     * @return list of labels, or {@code null} if the breaks are unlabelled
     */
    public List<String> getXLabels() { return xLabels; }

    /**
     * Sets the tick labels paired positionally with the X-axis breaks.
     *
     * @param xLabels list of labels, one per break
     */
    public void setXLabels(List<String> xLabels) { this.xLabels = xLabels; }

    /**
     * Returns the tick labels paired positionally with {@link #getYBreaks()}.
     *
     * @return list of labels, or {@code null} if the breaks are unlabelled
     */
    public List<String> getYLabels() { return yLabels; }

    /**
     * Sets the tick labels paired positionally with the Y-axis breaks.
     *
     * @param yLabels list of labels, one per break
     */
    public void setYLabels(List<String> yLabels) { this.yLabels = yLabels; }

    /**
     * Returns the tick labels paired positionally with {@link #getZBreaks()}.
     *
     * @return list of labels, or {@code null} if the breaks are unlabelled
     */
    public List<String> getZLabels() { return zLabels; }

    /**
     * Sets the tick labels paired positionally with the Z-axis breaks.
     *
     * @param zLabels list of labels, one per break
     */
    public void setZLabels(List<String> zLabels) { this.zLabels = zLabels; }

    /**
     * Checks whether custom X-breaks have been configured.
     *
     * @return {@code true} if X-breaks are present and non-empty
     */
    public boolean hasXBreaks() { return xBreaks != null && !xBreaks.isEmpty(); }

    /**
     * Checks whether custom Y-breaks have been configured.
     *
     * @return {@code true} if Y-breaks are present and non-empty
     */
    public boolean hasYBreaks() { return yBreaks != null && !yBreaks.isEmpty(); }

    /**
     * Checks whether custom Z-breaks have been configured.
     *
     * @return {@code true} if Z-breaks are present and non-empty
     */
    public boolean hasZBreaks() { return zBreaks != null && !zBreaks.isEmpty(); }

    /**
     * Returns the label configured for the X-axis break at the given index.
     *
     * @param breakIndex the position of the break within {@link #getXBreaks()}
     * @return the label, or {@code null} to fall back to numeric formatting
     */
    public String xLabelAt(int breakIndex) { return labelAt(xLabels, breakIndex); }

    /**
     * Returns the label configured for the Y-axis break at the given index.
     *
     * @param breakIndex the position of the break within {@link #getYBreaks()}
     * @return the label, or {@code null} to fall back to numeric formatting
     */
    public String yLabelAt(int breakIndex) { return labelAt(yLabels, breakIndex); }

    /**
     * Returns the label configured for the Z-axis break at the given index.
     *
     * @param breakIndex the position of the break within {@link #getZBreaks()}
     * @return the label, or {@code null} to fall back to numeric formatting
     */
    public String zLabelAt(int breakIndex) { return labelAt(zLabels, breakIndex); }

    private static String labelAt(List<String> labels, int breakIndex) {
        if (labels == null || breakIndex < 0 || breakIndex >= labels.size()) return null;
        return labels.get(breakIndex);
    }

    /**
     * Returns the transformation applied to the X aesthetic.
     *
     * @return the transform; {@link ScaleTransform#IDENTITY} for a linear axis
     */
    public ScaleTransform getXTransform() { return xTransform; }

    /**
     * Sets the transformation applied to the X aesthetic.
     *
     * @param xTransform the transform, or {@code null} for a linear axis
     */
    public void setXTransform(ScaleTransform xTransform) {
        this.xTransform = xTransform == null ? ScaleTransform.IDENTITY : xTransform;
    }

    /**
     * Returns the transformation applied to the Y aesthetic.
     *
     * @return the transform; {@link ScaleTransform#IDENTITY} for a linear axis
     */
    public ScaleTransform getYTransform() { return yTransform; }

    /**
     * Sets the transformation applied to the Y aesthetic.
     *
     * @param yTransform the transform, or {@code null} for a linear axis
     */
    public void setYTransform(ScaleTransform yTransform) {
        this.yTransform = yTransform == null ? ScaleTransform.IDENTITY : yTransform;
    }

    /**
     * Returns whether the Y-axis applies a base-10 logarithmic transform.
     * <p>
     * A convenience view onto {@link #getYTransform()}, kept because a log axis
     * is by far the most common transform and reads better as a flag.
     *
     * @return {@code true} for a log10 y-axis
     */
    public boolean isYLog() { return Objects.equals(yTransform, ScaleTransform.LOG10); }

    /**
     * Sets whether the Y-axis applies a base-10 logarithmic transform
     * (mirrors the {@code scaleYLog10()}).
     *
     * @param yLog {@code true} to enable the log10 y-axis
     */
    public void setYLog(boolean yLog) {
        this.yTransform = yLog ? ScaleTransform.LOG10 : ScaleTransform.IDENTITY;
    }

    /**
     * Returns the expansion applied to the X-axis domain of a continuous scale.
     *
     * @return the X expansion; {@link Expansion#mult(double)} of 5% by default
     */
    public Expansion getXExpand() { return xExpand; }

    /**
     * Sets the expansion applied to the X-axis domain of a continuous scale.
     *
     * @param xExpand the expansion, or {@code null} for no expansion
     */
    public void setXExpand(Expansion xExpand) {
        this.xExpand = Objects. requireNonNullElseGet(xExpand, Expansion::none);
    }

    /**
     * Returns the expansion applied to the Y-axis domain of a continuous scale.
     *
     * @return the Y expansion; {@link Expansion#mult(double)} of 5% by default
     */
    public Expansion getYExpand() { return yExpand; }

    /**
     * Sets the expansion applied to the Y-axis domain of a continuous scale.
     *
     * @param yExpand the expansion, or {@code null} for no expansion
     */
    public void setYExpand(Expansion yExpand) {
        this.yExpand = yExpand == null ? Expansion.none() : yExpand;
    }

    /**
     * Returns the expansion applied to the Z-axis domain of a continuous scale.
     * <p>
     * The z-axis expansion is decoupled from the Y axis, so that future
     * {@code z scales} / {@code zlim()} calls can control the Z domain
     * independently. Defaults to {@link Expansion#DEFAULT} — the same 5%
     * headroom that X and Y use.
     *
     * @return the Z expansion; {@link Expansion#mult(double)} of 5% by default
     */
    public Expansion getZExpand() { return zExpand; }

    /**
     * Sets the expansion applied to the Z-axis domain of a continuous scale.
     *
     * @param zExpand the expansion, or {@code null} for no expansion
     */
    public void setZExpand(Expansion zExpand) {
        this.zExpand = zExpand == null ? Expansion.none() : zExpand;
    }

    /**
     * Returns whether the X-axis runs in the reverse direction.
     *
     * @return {@code true} to invert the X-axis direction
     */
    public boolean isXReverse() { return xReverse; }

    /**
     * Sets whether the X-axis runs in the reverse direction, mirroring
     * {@code scaleXReverse()}.
     *
     * @param xReverse {@code true} to invert the X-axis direction
     */
    public void setXReverse(boolean xReverse) { this.xReverse = xReverse; }

    /**
     * Returns whether the Y-axis runs in the reverse direction.
     *
     * @return {@code true} to invert the Y-axis direction
     */
    public boolean isYReverse() { return yReverse; }

    /**
     * Sets whether the Y-axis runs in the reverse direction, mirroring
     * {@code scaleYReverse()}.
     *
     * @param yReverse {@code true} to invert the Y-axis direction
     */
    public void setYReverse(boolean yReverse) { this.yReverse = yReverse; }

    /**
     * Returns the explicit X-axis limits, if configured.
     *
     * @return the {@code [min, max]} X limits, or {@code null} to fit the data
     */
    public MinMax getXLimits() { return xLimits; }

    /**
     * Sets explicit X-axis limits, overriding the data-driven domain
     * (mirrors {@code scaleXContinuous(limits = c(min, max))}).
     *
     * @param xLimits the {@code [min, max]} limits, or {@code null} to fit the data
     */
    public void setXLimits(MinMax xLimits) { this.xLimits = xLimits; }

    /**
     * Returns the explicit Y-axis limits, if configured.
     *
     * @return the {@code [min, max]} Y limits, or {@code null} to fit the data
     */
    public MinMax getYLimits() { return yLimits; }

    /**
     * Sets explicit Y-axis limits, overriding the data-driven domain
     * (mirrors {@code scaleYContinuous(limits = c(min, max))}).
     *
     * @param yLimits the {@code [min, max]} limits, or {@code null} to fit the data
     */
    public void setYLimits(MinMax yLimits) { this.yLimits = yLimits; }

    /**
     * Returns the explicit Z-axis limits, if configured.
     *
     * @return the {@code [min, max]} Z limits, or {@code null} to fit the data
     */
    public MinMax getZLimits() { return zLimits; }

    /**
     * Sets explicit Z-axis limits, overriding the data-driven domain
     * (mirrors {@code zlim(min, max)}).
     *
     * @param zLimits the {@code [min, max]} limits, or {@code null} to fit the data
     */
    public void setZLimits(MinMax zLimits) { this.zLimits = zLimits; }

    /**
     * Returns where the X axis is drawn: the bottom (default) or top edge.
     *
     * @return the X axis position
     */
    public AxisPosition getXAxisPosition() { return xAxisPosition; }

    /**
     * Sets where the X axis is drawn, mirroring
     * {@code scaleXContinuous(position = "top"|"bottom")}.
     *
     * @param xAxisPosition the X axis position, or {@code null} for the bottom
     */
    public void setXAxisPosition(AxisPosition xAxisPosition) {
        this.xAxisPosition = xAxisPosition == null ? AxisPosition.BOTTOM : xAxisPosition;
    }

    /**
     * Returns where the Y axis is drawn: the left (default) or right edge.
     *
     * @return the Y axis position
     */
    public AxisPosition getYAxisPosition() { return yAxisPosition; }

    /**
     * Sets where the Y axis is drawn, mirroring
     * {@code scaleYContinuous(position = "left"|"right")}.
     *
     * @param yAxisPosition the Y axis position, or {@code null} for the left
     */
    public void setYAxisPosition(AxisPosition yAxisPosition) {
        this.yAxisPosition = yAxisPosition == null ? AxisPosition.LEFT : yAxisPosition;
    }

    /**
     * Returns whether the X axis draws its labels and title along the top edge.
     *
     * @return {@code true} when {@link #getXAxisPosition()} is {@link AxisPosition#TOP}
     */
    public boolean isXAxisTop() { return xAxisPosition == AxisPosition.TOP; }

    /**
     * Returns whether the Y axis draws its labels and title along the right edge.
     *
     * @return {@code true} when {@link #getYAxisPosition()} is {@link AxisPosition#RIGHT}
     */
    public boolean isYAxisRight() { return yAxisPosition == AxisPosition.RIGHT; }

    /**
     * Returns the explicit category-to-colour mapping.
     *
     * @return the palette map, or {@code null} if the default palette applies
     */
    public Map<String, Color> getManualColors() { return manualColors; }

    /**
     * Sets an explicit category-to-colour mapping, keyed by the category's string form.
     *
     * @param manualColors the palette map
     */
    public void setManualColors(Map<String, Color> manualColors) {
        this.manualColors = manualColors == null ? null : new LinkedHashMap<>(manualColors);
    }

    /**
     * Resolves the manually configured colour for a category value.
     *
     * @param category the raw category value
     * @return the configured {@link Color}, or {@code null} if none applies
     */
    public Color manualColorFor(Object category) {
        return lookup(manualColors, category);
    }

    /**
     * Returns the named colour ramp selected for continuous scales override
     * the per-layer default.
     *
     * @return the colour-ramp name, or {@code null} to defer to the layer
     */
    public String getContinuousColorMap() { return continuousColorMap; }

    /**
     * Sets the named colour ramp used by continuous colour scales, overriding
     * the per-layer default (e.g. {@code scaleColorViridisC()}).
     *
     * @param continuousColorMap the colour-ramp name, or {@code null} for the default
     */
    public void setContinuousColorMap(String continuousColorMap) {
        this.continuousColorMap = continuousColorMap;
    }

    /**
     * Returns whether the configured ramp should be applied per category
     * (discrete) rather than interpolated by value.
     *
     * @return {@code true} for {@code scaleColorViridisD} and
     *         {@code scaleColorBrewer}
     */
    public boolean isDiscreteColorRamp() { return discreteColorRamp; }

    /**
     * Marks the configured ramp as discrete ({@code scaleColorViridisD},
     * {@code scaleColorBrewer}) or clears it for continuous ramps
     * ({@code scaleColorViridisC}, gradients, binned scales).
     *
     * @param discreteColorRamp {@code true} when the ramp maps per category
     */
    public void setDiscreteColorRamp(boolean discreteColorRamp) {
        this.discreteColorRamp = discreteColorRamp;
    }

    /**
     * Returns the explicit gradient colours for continuous colour scales.
     *
     * @return the gradient stop colours in low-to-high order, or {@code null}
     *         to use a named ramp
     */
    public List<Color> getContinuousGradient() { return continuousGradient; }

    /**
     * Sets an explicit gradient for continuous colour scales, overriding any
     * named ramp (e.g. {@code scaleColorGradient(low, high)}).
     *
     * @param continuousGradient the gradient colours, or {@code null} to use a named ramp
     */
    public void setContinuousGradient(List<Color> continuousGradient) {
        this.continuousGradient = continuousGradient == null
                ? null : new ArrayList<>(continuousGradient);
    }

    /**
     * Returns the number of discrete steps for a binned continuous scale.
     *
     * @return the step count, or {@code null} for an unbinned continuous scale
     */
    public Integer getColorSteps() { return colorSteps; }

    /**
     * Sets the number of discrete steps for a binned continuous colour scale
     * (e.g. {@code scaleColorBinned(n = 6)}).
     *
     * @param colorSteps the step count, or {@code null} for an unbinned scale
     */
    public void setColorSteps(Integer colorSteps) {
        this.colorSteps = colorSteps != null && colorSteps <= 0 ? null : colorSteps;
    }

    /**
     * Returns whether a binned continuous colour scale is configured.
     *
     * @return {@code true} when {@link #getColorSteps()} is set
     */
    public boolean isBinned() { return colorSteps != null; }

    /**
     * Returns the explicit category-to-shape mapping for the {@code shape}
     * aesthetic.
     *
     * @return the manual shape map, or {@code null} for the default cycle
     */
    public Map<String, PointShape> getManualShapes() { return manualShapes; }

    /**
     * Sets an explicit category-to-shape mapping for the {@code shape}
     * aesthetic, keyed by the category's string form.
     *
     * @param manualShapes the manual shape map, or {@code null} for the default
     */
    public void setManualShapes(Map<String, PointShape> manualShapes) {
        this.manualShapes = manualShapes == null ? null : new LinkedHashMap<>(manualShapes);
    }

    /**
     * Resolves the manually configured shape for a category value.
     *
     * @param category the raw category value
     * @return the configured {@link PointShape}, or {@code null} if none applies
     */
    public PointShape manualShapeFor(Object category) {
        return lookup(manualShapes, category);
    }

    /**
     * Returns the explicit category-to-size mapping for the {@code size}
     * aesthetic.
     *
     * @return the manual size map, or {@code null} for the default scaling
     */
    public Map<String, Double> getManualSizes() { return manualSizes; }

    /**
     * Sets an explicit category-to-size mapping for the {@code size} aesthetic,
     * keyed by the category's string form.
     *
     * @param manualSizes the manual size map, or {@code null} for the default
     */
    public void setManualSizes(Map<String, Double> manualSizes) {
        this.manualSizes = manualSizes == null ? null : new LinkedHashMap<>(manualSizes);
    }

    /**
     * Resolves the manually configured size for a category value.
     *
     * @param category the raw category value
     * @return the configured size, or {@code null} if none applies
     */
    public Double manualSizeFor(Object category) {
        return lookup(manualSizes, category);
    }

    /**
     * Returns the explicit category-to-dash mapping for the {@code linetype}
     * aesthetic, keyed by the category's string form.
     *
     * @return the manual linetype map (dash arrays as lists), or {@code null}
     *         for the default cycle
     */
    public Map<String, List<Double>> getManualLinetypes() { return manualLinetypes; }

    /**
     * Sets an explicit category-to-dash mapping for the {@code linetype}
     * aesthetic, overriding the automatic line-type cycle for those categories.
     *
     * @param manualLinetypes the manual linetype map, or {@code null} for the
     *                        default line-type cycle
     */
    public void setManualLinetypes(Map<String, List<Double>> manualLinetypes) {
        if (manualLinetypes == null) {
            this.manualLinetypes = null;
            return;
        }
        var copy = new LinkedHashMap<String, List<Double>>();
        for (var e : manualLinetypes.entrySet()) {
            copy.put(e.getKey(), e.getValue() == null ? null : List.copyOf(e.getValue()));
        }
        this.manualLinetypes = copy;
    }

    /**
     * Resolves the manually configured dash pattern for a category value.
     *
     * @param category the raw category value
     * @return the configured dash list, or {@code null} if none applies
     */
    public List<Double> manualLinetypeFor(Object category) {
        return lookup(manualLinetypes, category);
    }

    private static <T> T lookup(Map<String, T> map, Object category) {
        if (map == null || category == null) {
            return null;
        }
        var hit = map.get(category.toString());
        if (hit != null) {
            return hit;
        }
        return map.get(normalizedKey(category));
    }

    private static String normalizedKey(Object category) {
        if (category instanceof Number n) {
            var d = n.doubleValue();
            if (!Double.isInfinite(d) && d == Math.rint(d)) {
                return String.valueOf((long) d);
            }
        }
        return category.toString();
    }
}
