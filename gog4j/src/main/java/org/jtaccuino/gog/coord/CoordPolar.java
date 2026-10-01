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
package org.jtaccuino.gog.coord;

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.VPos;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import org.jtaccuino.gog.MinMax;
import org.jtaccuino.gog.data.Temporals;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.scale.Scale;
import org.jtaccuino.gog.theme.Theme;

/**
 * A polar coordinate system, the counterpart of the {@code Coords.coordPolar()}.
 * <p>
 * The plot's data point pair is reinterpreted as an angle (the <em>theta</em>
 * aesthetic) and a radius (the other aesthetic), which turns bar or column
 * rectangles into wedges: pies, coxcomb/rose plots, and bulls'-eyes.
 * By convention, theta zero sits at twelve o'clock and increasing theta
 * runs clockwise on screen (y grows downwards), so the {@link DrawSurface}
 * arc primitives line up with the point geometry by construction.
 * <p>
 * By convention, the default {@code Coords.coordPolar()} maps the x
 * aesthetic to the angle (a "rose", one wedge per category). Passing
 * {@link #theta(String) theta("y")} maps the y aesthetic to the angle and uses
 * x as the radius, the pie form where each stacked segment at a shared
 * position becomes one angular slice.
 * <p>
 * Rendering falls back to plain Cartesian grid behaviour replaced by a
 * circular rim, concentric radius guides, and angular spokes.
 */
public class CoordPolar implements Coord {

    private static final double TWO_PI = 2.0 * Math.PI;
    private static final double HALF_PI = Math.PI / 2.0;

    /** The number of breaks the theta axis aims for. */
    private static final int THETA_BREAKS = 12;

    /**
     * Fraction of the data range added to each end of a continuous scale
     * inside a partial fan, so the minimum and maximum observations keep a
     * margin from the inner border, the rim, and the angular edges — the
     * default 5% continuous-scale expansion for {@code coordRadial()}.
     */
    private static final double EXPANSION = 0.05;

    /**
     * The radial counterpart of {@link #EXPANSION}: the fraction of the radial
     * data range kept as a margin at the inner border and the rim of a partial
     * fan. Twice the theta padding, so the extreme radial gridline and the
     * minimum and maximum observations sit clearly off the fan's edges instead
     * of hugging the ring at the border.
     */
    private static final double RADIAL_EXPANSION = 0.10;

    /**
     * The disc occupies this fraction of the panel's half-extent, so the plot
     * keeps clear of the plot background on every side. The default draws its
     * outermost {@code coordPolar} circle at 0.45 of the panel's 0.5
     * half-extent, i.e. 90%.
     */
    private static final double RADIUS_FACTOR = 0.9;

    /**
     * The angular overlap between abutting wedges, in radians. Adjacent filled
     * shapes each antialias their shared edge, so a wedge is widened on both
     * sides by this hair and neighbouring fills overlap invisibly instead of
     * leaving a background line between segments.
     */
    private static final double OVERLAP = Math.toRadians(0.25);

    private String theta = "x";
    private double start = 0.0;
    private Double end = null;
    private Direction direction = Direction.CLOCKWISE;
    private double innerRadius = 0.0;

    /** Memoized fan bounding box, valid for the panel bounds it was computed at. */
    private double[] fanBoundsCache;
    private double fanBoundsKeyX = Double.NaN;
    private double fanBoundsKeyY = Double.NaN;
    private double fanBoundsKeyW = Double.NaN;
    private double fanBoundsKeyH = Double.NaN;

    /**
     * Whether the radial axis ticks and labels sit inside the panel on the
     * inner radius instead of beside the plot. {@link Mode#AUTO} follows
     * the {@code Coords.coordRadial()} default: the axis stays outside for a
     * full turn and moves inside the panel for a partial fan.
     */
    private Mode rAxisInside = Mode.AUTO;

    /**
     * Whether the continuous scales expand their limits by the {@link
     * #EXPANSION} padding. {@link ScaleExpansion#AUTO} keeps the historic
     * behaviour — expansion only inside a partial fan, so full-turn plots hug
     * their rim — while {@link ScaleExpansion#ON} matches the default, which
     * expands every scale of a radial plot (the pie charts of the 3.5.0
     * release post need {@code expand = FALSE} to fill the disc).
     */
    private ScaleExpansion expand = ScaleExpansion.AUTO;

    /** Whether the theta (angular) axis — spokes, rim ticks, and labels — is drawn. */
    private boolean showThetaAxis = true;

    /** Whether the radial axis — value ticks, labels, and title — is drawn. */
    private boolean showRadialAxis = true;

    /**
     * Whether a text geometry's {@code angle} is realigned with the angular
     * position of each label, the {@code rotateAngle} switch of the
     * {@code coordRadial()}.
     */
    private boolean rotateAngle = false;

    /**
     * The {@code angle} of the {@code axisGuide()} axis: how much the
     * theta value labels are turned relative to the tangent of the circle at
     * their position. {@code null} leaves the labels horizontal (the default),
     * {@code 0} lays them tangential to the ring, {@code 90} along the radius.
     */
    private Double thetaLabelAngle = null;

    private double panelX, panelY, panelW, panelH;
    private Scale scaleX, scaleY;
    private Theme theme;
    private String radialAxisTitle;

    /** Creates the default polar coordinate system, theta mapping to the x aesthetic. */
    public CoordPolar() {
    }

    /**
     * Chooses which aesthetic carries the angle.
     *
     * @param theta {@code "x"} (rose/coxcomb) or {@code "y"} (pie)
     * @return this coordinate system for fluid chaining
     */
    public CoordPolar theta(String theta) {
        this.theta = theta;
        return this;
    }

    /**
     * Sets the angular offset of the first position, applied after the
     * direction flip. The value follows the default and is measured in radians
     * clockwise from twelve o'clock.
     *
     * @param start the angular offset in radians
     * @return this coordinate system for fluid chaining
     */
    public CoordPolar start(double start) {
        this.start = start;
        return this;
    }

    /**
     * Limits the angular span: the theta range runs from {@link #start(double)}
     * to this offset (twelve o'clock, clockwise radians) instead of a full
     * turn, the {code Coords.coordRadial(start, end)} fan. Leaving {@code
     * end} unset (the default) keeps the full {@code start + 2π} circle.
     *
     * @param end the angular offset of the end of the data range in radians
     * @return this coordinate system for fluid chaining
     */
    public CoordPolar end(double end) {
        this.end = end;
        return this;
    }

    /**
     * Sets the sweep direction of increasing theta: clockwise on screen
     * (the default, following the {@code direction}), or the reverse.
     *
     * @param direction the {@link Direction} of increasing theta
     * @return this coordinate system for fluid chaining
     */
    public CoordPolar direction(Direction direction) {
        this.direction = direction;
        return this;
    }

    /**
     * Leaves a hollow centre: the radius starts at this fraction of the outer
     * radius (0 for a full disc, up to just under 1 for a thin ring). A donut
     * chart is a pie with {@code innerRadius} around 0.5.
     *
     * @param innerRadius the fraction between 0 and 1
     * @return this coordinate system for fluid chaining
     */
    public CoordPolar innerRadius(double innerRadius) {
        this.innerRadius = Math.max(0.0, Math.min(1.0, innerRadius));
        return this;
    }

    /**
     * Places the radial axis inside the panel at the inner radius rather than
     * beside it, the {@code radialAxisInside = TRUE} of the
     * {@code coordRadial()}. A donut chart or a hollow-centred scatter can
     * then keep its value labels on the hole's boundary instead of outside the
     * rim. Left {@link Mode#AUTO}, fans default to an inside axis and full
     * turns to an outside one, by default.
     *
     * @param inside {@link Mode#YES} to draw the radial axis on the inner radius
     * @return this coordinate system for fluid chaining
     */
    public CoordPolar rAxisInside(Mode inside) {
        this.rAxisInside = inside;
        return this;
    }

    /**
     * Controls the margin the continuous scales keep at their extremes, the
     * {@code expand} argument of the {@code Coords.coordRadial()}. With the
     * default on, the theta and radial ranges are padded by {@link #EXPANSION}
     * so observations never touch the rim or the angular edges — but a pie's
     * single category then leaves a gap at twelve o'clock. Passing
     * {@link ScaleExpansion#OFF} takes the limits straight from the scale, the
     * full-disc pie of the release post.
     *
     * @param expand the {@link ScaleExpansion} of the continuous scale limits
     * @return this coordinate system for fluid chaining
     */
    public CoordPolar expand(ScaleExpansion expand) {
        this.expand = expand;
        return this;
    }

    /**
     * Hides or shows the theta (angular) axis: the spokes, their rim tick
     * marks, and the value labels around the outside, the polar counterpart of
     * {@code guides(theta = Guides.none())} in the reference implementation.
     *
     * @param show {@code false} to suppress the theta axis
     * @return this coordinate system for fluid chaining
     */
    public CoordPolar showThetaAxis(boolean show) {
        this.showThetaAxis = show;
        return this;
    }

    /**
     * Hides or shows the radial axis: the value ticks, their labels, and the
     * rotated axis title, the polar counterpart of
     * {@code guides(r = Guides.none())} in the reference implementation.
     *
     * @param show {@code false} to suppress the radial axis
     * @return this coordinate system for fluid chaining
     */
    public CoordPolar showRadialAxis(boolean show) {
        this.showRadialAxis = show;
        return this;
    }

    /**
     * Turns the {@code rotateAngle} switch on: text geometries then place
     * their labels by adding the user's {@code angle} to the angular position
     * of each data point, so an angle of 0 reads tangent to the ring and 90
     * along the radius — the wind-rose trick of the 3.5.0 reference release
     * post, where car names skirt the rim without hand-computed angles.
     *
     * @param rotate {@code true} to realign text angles with the coordinate
     * @return this coordinate system for fluid chaining
     */
    public CoordPolar rotateAngle(boolean rotate) {
        this.rotateAngle = rotate;
        return this;
    }

    /** {@return whether {@link #rotateAngle(boolean)} realigns text with the theta coordinate} */
    public boolean isRotateAngle() {
        return rotateAngle;
    }

    /**
     * Rotates the theta value labels by this angle relative to the tangent of
     * the circle at their angular position, the {@code angle} argument of
     * the {@code axisGuide()}. Left unset the labels stay
     * horizontal (the default); {@code 0} places them tangential to the
     * ring, {@code 90} along the radius, and any angle beyond keeps them right
     * side up by turning upside-down labels over.
     *
     * @param angle the label angle in degrees relative to the tangent
     * @return this coordinate system for fluid chaining
     */
    public CoordPolar thetaLabelAngle(double angle) {
        this.thetaLabelAngle = angle;
        return this;
    }

    /** {@return whether the x aesthetic maps to the angle} */
    public boolean isThetaX() {
        return "x".equals(theta);
    }

    @Override
    public boolean isFlipped() {
        return false;
    }

    @Override
    public MinMax adjustXBounds(MinMax computedBounds) {
        return computedBounds;
    }

    @Override
    public MinMax adjustYBounds(MinMax computedBounds) {
        return computedBounds;
    }

    /** {@return the scale of the theta (angular) coordinate} */
    public Scale thetaScale() {
        return isThetaX() ? scaleX : scaleY;
    }

    /** {@return the scale of the radial coordinate} */
    public Scale radialScale() {
        return isThetaX() ? scaleY : scaleX;
    }

    /**
     * The angular span of the data range: the full turn with the offset and
     * end unset, or the {@code end - start} fan otherwise.
     */
    private double angularSpan() {
        return end == null ? TWO_PI : (end - start);
    }

    /**
     * The bounding box, in unit-disc coordinates, of everything the fan draws:
     * the apex plus the sampled arc. A full turn yields the unit square.
     * Memoized on the panel bounds, which are the only inputs that change.
     */
    private double[] fanBounds() {
        if (fanBoundsCache != null
                && fanBoundsKeyX == panelX && fanBoundsKeyY == panelY
                && fanBoundsKeyW == panelW && fanBoundsKeyH == panelH) {
            return fanBoundsCache;
        }
        double[] b = computeFanBounds();
        fanBoundsCache = b;
        fanBoundsKeyX = panelX;
        fanBoundsKeyY = panelY;
        fanBoundsKeyW = panelW;
        fanBoundsKeyH = panelH;
        return b;
    }

    private double[] computeFanBounds() {
        if (Math.abs(angularSpan()) >= TWO_PI - 1e-9) {
            return new double[]{-1.0, -1.0, 1.0, 1.0};
        }
        double a0 = thetaAngle(thetaLow());
        double a1 = a0 + angularSpan();
        double minX = 0.0, minY = 0.0, maxX = 0.0, maxY = 0.0;
        int steps = 72;
        for (int i = 0; i <= steps; i++) {
            double a = a0 + (a1 - a0) * i / steps;
            minX = Math.min(minX, Math.cos(a));
            maxX = Math.max(maxX, Math.cos(a));
            minY = Math.min(minY, Math.sin(a));
            maxY = Math.max(maxY, Math.sin(a));
        }
        return new double[]{minX, minY, maxX, maxY};
    }

    /** {@return the horizontal centre of the polar panel} */
    public double centerX() {
        var b = fanBounds();
        return panelX + panelW / 2.0 - (b[0] + b[2]) / 2.0 * radius();
    }

    /** {@return the vertical centre of the polar panel} */
    public double centerY() {
        var b = fanBounds();
        return panelY + panelH / 2.0 - (b[1] + b[3]) / 2.0 * radius();
    }

    /** {@return the radius of the polar disc, in pixels, inside the panel} */
    public double radius() {
        var b = fanBounds();
        double w = Math.max(b[2] - b[0], 1e-9);
        double h = Math.max(b[3] - b[1], 1e-9);
        return RADIUS_FACTOR * Math.min(panelW / w, panelH / h);
    }

    /**
     * Sets the pixel bounds of the panel area on the canvas.
     *
     * @param x left edge
     * @param y top edge
     * @param w width
     * @param h height
     */
    public void setPanelBounds(double x, double y, double w, double h) {
        panelX = x;
        panelY = y;
        panelW = w;
        panelH = h;
    }

    /**
     * Prepares the coordinate system with the scales and theme computed for the
     * current facet, so geometry can be resolved without threading the pair
     * through every call.
     *
     * @param sx the x-axis scale
     * @param sy the y-axis scale
     * @param t  the theme
     */
    public void prepare(Scale sx, Scale sy, Theme t) {
        scaleX = sx;
        scaleY = sy;
        theme = t;
    }

    /**
     * Sets the text drawn alongside the radial axis on the left of the panel,
     * the polar counterpart of the Cartesian y-axis title ("Count" for a
     * coxcomb). Rendered rotated and reading bottom-up next to the break
     * labels, mirroring {@code axisRenderer("r", "left")} of the default.
     *
     * @param radialAxisTitle the radial axis title, or {@code null} to omit it
     */
    public void setRadialAxisTitle(String radialAxisTitle) {
        this.radialAxisTitle = radialAxisTitle;
    }

    /**
     * The radial scale's own ticks when it is continuous (a meaningful radius
     * quantity such as a count); {@code null} for a discrete radial scale,
     * whose bands carry no continuous axis values worth labelling.
     */
    private List<Double> continuousRadialTicks() {
        var scale = radialScale();
        if (scale == null || scale.isDiscrete()) {
            return null;
        }
        if (isTemporalAxis(scale)) {
            return temporalTicks(scale, radialTarget());
        }
        if (isPartialFan()) {
            return fanRadialBreaks(scale);
        }
        var target = radialTarget();
        var ticks = scale.calculateTicks(target, null);
        if (ticks == null || ticks.isEmpty()) {
            return null;
        }
        return ticks;
    }

    /** The number of radial breaks a polar panel of the current size aims for. */
    private int radialTarget() {
        return Math.max(4, (int) Math.round(radius() / 90.0) + 1);
    }

    /**
     * The theta axis breaks: calendar breaks for a date or timestamp scale,
     * otherwise the scale's own evenly spaced ticks — the polar counterpart of
     * a Cartesian temporal axis, whose positions read as dates and times instead
     * of raw epoch days or milliseconds.
     */
    private List<Double> thetaTicks() {
        var theta = thetaScale();
        if (isTemporalAxis(theta)) {
            return temporalTicks(theta, THETA_BREAKS);
        }
        return theta.calculateTicks(THETA_BREAKS, null);
    }

    /** The theta axis label: a calendar label on a temporal axis, else the scale's label. */
    private String thetaTickLabel(double tick) {
        var theta = thetaScale();
        if (isTemporalAxis(theta)) {
            return temporalLabel(tick, theta, THETA_BREAKS);
        }
        return theta.getLabel(tick);
    }

    /** The radial axis label: a calendar label on a temporal axis, else the scale's label. */
    private String radialTickLabel(double tick) {
        var scale = radialScale();
        if (isTemporalAxis(scale)) {
            return temporalLabel(tick, scale, radialTarget());
        }
        return scale.getLabel(tick);
    }

    /**
     * Whether the scale maps a temporal column, and so needs calendar breaks
     * rather than the scale's own numeric ones.
     *
     * @param scale the scale, may be {@code null}
     * @return {@code true} for a date or timestamp scale
     */
    private static boolean isTemporalAxis(Scale scale) {
        return scale != null && (scale.isDateScale() || scale.isTimestampScale());
    }

    /**
     * Calendar-aligned breaks for a temporal axis, resolved through
     * {@link Temporals} exactly as the Cartesian axes resolve theirs, so the two
     * coordinate systems cannot drift apart.
     *
     * @param scale  the temporal scale
     * @param target the wanted number of breaks
     * @return the break positions
     */
    private static List<Double> temporalTicks(Scale scale, int target) {
        if (scale.isTimestampScale()) {
            return Temporals.timestampTicks(scale.minData(), scale.maxData(), target);
        }
        return Temporals.dateTicks(scale.minData(), scale.maxData());
    }

    /**
     * The label of a break on a temporal axis: a year on a date axis, and a date
     * or date-time at the granularity the axis breaks at on a timestamp axis.
     *
     * @param tick   the break position in data units
     * @param scale  the temporal scale
     * @param target the wanted number of breaks, sizing the granularity
     * @return the formatted label
     */
    private static String temporalLabel(double tick, Scale scale, int target) {
        if (scale.isTimestampScale()) {
            return Temporals.timestampLabel(tick, Temporals.granularityOf(scale.minData(), scale.maxData(), target));
        }
        return Temporals.dateLabel(tick);
    }

    /**
     * The "nice" breaks of a partial fan's radial axis: round steps of 1, 2 or
     * 5 times a power of ten — the 1, 2, 3 …; 5, 10, 15 …; 1, 1.5, 2 … breaks
     * of everyday plotting — spanning the padded display range, so every guide
     * ring lands on a value that reads naturally instead of an arbitrary
     * fraction of the data span.
     */
    private List<Double> fanRadialBreaks(Scale scale) {
        var lo = scale.minData();
        var hi = scale.maxData();
        var range = hi - lo;
        if (range <= 0) {
            return List.of(lo);
        }
        var target = Math.max(4, (int) Math.round(radius() / 90.0) + 1);
        var step = niceStep(range / target);
        var dispLo = lo - RADIAL_EXPANSION * range;
        var dispHi = hi + RADIAL_EXPANSION * range;
        var ticks = new ArrayList<Double>();
        for (var v = Math.ceil(dispLo / step) * step; v <= dispHi + 1e-9; v += step) {
            ticks.add(Math.round(v * 1e10) / 1e10);
        }
        return ticks.isEmpty() ? List.of(lo) : ticks;
    }

    /**
     * The "nice" step nearest the raw estimate: one of 1, 2 or 5 times a power
     * of ten (1, 2, 3 …; 5, 10, 15 …; 1, 1.5, 2 …), so break values read as
     * round numbers instead of arbitrary fractions.
     */
    private static double niceStep(double raw) {
        var mag = Math.pow(10.0, Math.floor(Math.log10(raw)));
        var norm = raw / mag;
        if (norm < 1.5) {
            return mag;
        }
        if (norm < 3.0) {
            return 2.0 * mag;
        }
        if (norm < 7.0) {
            return 5.0 * mag;
        }
        return 10.0 * mag;
    }

    /**
     * Whether the continuous theta and radial scales expand their limits by
     * {@link #EXPANSION}. Always inside a partial fan (so the extreme
     * observations keep a margin from the angular edges and the rim), and for
     * a full turn only when {@link #expand(ScaleExpansion)} turns it on — the
     * {@code coordRadial(expand = TRUE)}.
     */
    private boolean continuousExpansion() {
        return isPartialFan() || expand == ScaleExpansion.ON;
    }

    /**
     * The lower bound of the theta scale's display range: the data minimum,
     * pulled out by {@link #EXPANSION} for a continuous scale so the extreme
     * angular positions keep a margin from the edges.
     */
    private double thetaLow() {
        var s = thetaScale();
        if (s.isDiscrete() || !continuousExpansion()) {
            return s.minData();
        }
        return s.minData() - EXPANSION * (s.maxData() - s.minData());
    }

    /** @return the upper bound of the theta scale's display range, see {@link #thetaLow()} */
    private double thetaHigh() {
        var s = thetaScale();
        if (s.isDiscrete() || !isPartialFan()) {
            return s.maxData();
        }
        return s.maxData() + EXPANSION * (s.maxData() - s.minData());
    }

    /**
     * The angular fraction of a theta value, its share of a full turn. A
     * discrete theta axis places its range from the first band edge, so the
     * first category runs from twelve o'clock onward and each band covers an
     * equal sector — the {@code theta.range = [0.5, n - 0.5]} convention of
     * the {@code Coords.coordPolar()} — instead of centring the first band on
     * twelve o'clock. A continuous scale inside a fan interpolates over the
     * expanded {@link #thetaLow()}/{@link #thetaHigh()} range.
     */
    private double thetaFraction(double value) {
        var scale = thetaScale();
        if (scale.isDiscrete()) {
            var lo = scale.minData();
            var hi = scale.maxData();
            return hi == lo ? 0.5 : (value - lo) / (hi - lo);
        }
        var lo = thetaLow();
        var hi = thetaHigh();
        return hi == lo ? 0.5 : (value - lo) / (hi - lo);
    }

    /**
     * Maps a theta data value to its angular position in radians: the fraction
     * of a full turn (plus the configured offset), flipped by direction,
     * measured from twelve o'clock — the {@code Coords.coordPolar} layout.
     * A positive angle is clockwise on screen.
     *
     * @param thetaData data value along the theta axis
     * @return the angle in radians
     */
    public double thetaAngle(double thetaData) {
        return start + direction.sign() * (thetaFraction(thetaData) * angularSpan()) - HALF_PI;
    }

    /**
     * The rotation of a label at a theta position, the
     * {@code textAngleAdjustment()} for {@code coordRadial(rotateAngle = TRUE)}:
     * the user's {@code angle} is offset by the label's angular position — 0
     * reads tangent to the ring, 90 along the radius — and any label that
     * would end up upside down is turned right side up.
     *
     * @param thetaData theta value of the label's data point
     * @param userAngle the user-set angle in degrees
     * @return the clockwise rotation in degrees for a {@link DrawSurface}, and
     *         whether the label was flipped (the caller then mirrors hjust and
     *         vjust as well)
     */
    public TextRotation textRotation(double thetaData, double userAngle) {
        // The conventional theta coordinate, 0 at twelve o'clock and clockwise —
        // our thetaAngle already carries the extra -90° to the screen axes.
        var thetaG = Math.toDegrees(thetaAngle(thetaData)) + 90.0;
        var effective = ((userAngle - thetaG) % 360.0 + 360.0) % 360.0;
        var flipped = effective > 90.0 && effective < 270.0;
        if (flipped) {
            effective += 180.0;
        }
        // the default rotates grid text counter-clockwise; our surface is clockwise.
        return new TextRotation(-effective, flipped);
    }

    /** The rotation of a single label and whether it was turned right side up.
     * @param degrees the clockwise rotation in degrees for a {@link DrawSurface}
     * @param flipped whether the label was turned right side up
     */
    public record TextRotation(double degrees, boolean flipped) {
    }

    /**
     * Maps a radius data value to its pixel distance from the centre, scaled so
     * that the panel radius fills the range and the inner radius is respected.
     * A continuous scale inside a partial fan interpolates over the expanded
     * range (see {@link #RADIAL_EXPANSION}), so the minimum and maximum
     * observations keep a margin from the inner border and the rim instead of
     * hugging them.
     *
     * @param radiusData data value along the radial axis
     * @return the distance in pixels
     */
    public double rPixel(double radiusData) {
        var scale = radialScale();
        var lo = scale.minData();
        var hi = scale.maxData();
        double frac;
        if (hi == lo) {
            frac = 0.5;
        } else if (!scale.isDiscrete() && continuousExpansion()) {
            var pad = RADIAL_EXPANSION * (hi - lo);
            frac = (radiusData - (lo - pad)) / ((hi + pad) - (lo - pad));
        } else {
            frac = (radiusData - lo) / (hi - lo);
        }
        return (innerRadius + (1.0 - innerRadius) * frac) * radius();
    }

    /**
     * Resolves the horizontal pixel position of a data point through the polar
     * mapping — its angle and radius instead of a Cartesian position.
     */
    @Override
    public double xPixel(Scale sx, Scale sy, double xData, double yData) {
        if (this.scaleX == null) {
            this.scaleX = sx;
            this.scaleY = sy;
        }
        var angle = thetaAngle(isThetaX() ? xData : yData);
        var r = rPixel(isThetaX() ? yData : xData);
        return centerX() + r * Math.cos(angle);
    }

    /**
     * Resolves the vertical pixel position of a data point through the polar
     * mapping — the {@link #xPixel(Scale, Scale, double, double)} counterpart.
     */
    @Override
    public double yPixel(Scale sx, Scale sy, double xData, double yData) {
        if (this.scaleX == null) {
            this.scaleX = sx;
            this.scaleY = sy;
        }
        var angle = thetaAngle(isThetaX() ? xData : yData);
        var r = rPixel(isThetaX() ? yData : xData);
        return centerY() + r * Math.sin(angle);
    }

    /**
     * Draws an annular wedge between two angles and two radii, the polar image
     * of a bar rectangle. A wedge reaching the centre is emitted with the
     * cheap atomic pie-slice arc; an inner ring is composed from two true arc
     * segments joined by straight radii. Wedges are filled only — abutting
     * fills tile the disc, and the rim and guides come from
     * {@link #renderBackground(DrawSurface)}.
     *
     * @param gc     the surface to draw onto
     * @param thLow  the lower theta value in data space
     * @param thHigh the upper theta value in data space
     * @param rLow   the inner radius value in data space
     * @param rHigh  the outer radius value in data space
     */
    public void drawWedge(DrawSurface gc, double thLow, double thHigh, double rLow, double rHigh) {
        var a0 = thetaAngle(thLow);
        var a1 = thetaAngle(thHigh);
        var rInner = rPixel(rLow);
        var rOuter = rPixel(rHigh);

        boolean forward = a1 >= a0;
        if (forward) {
            a0 -= OVERLAP;
            a1 += OVERLAP;
        } else {
            a0 += OVERLAP;
            a1 -= OVERLAP;
        }

        var cx = centerX();
        var cy = centerY();
        var deg0 = Math.toDegrees(a0);
        var sweep = Math.toDegrees(a1 - a0);

        if (rInner <= 0.5) {
            gc.fillArc(cx - rOuter, cy - rOuter, 2.0 * rOuter, 2.0 * rOuter, deg0, sweep);
            return;
        }

        gc.beginPath();
        gc.moveTo(cx + rOuter * Math.cos(a0), cy + rOuter * Math.sin(a0));
        gc.arc(cx - rOuter, cy - rOuter, 2.0 * rOuter, 2.0 * rOuter, deg0, sweep);
        gc.lineTo(cx + rInner * Math.cos(a1), cy + rInner * Math.sin(a1));
        gc.arc(cx - rInner, cy - rInner, 2.0 * rInner, 2.0 * rInner, Math.toDegrees(a1), -sweep);
        gc.closePath();
        gc.fill();
    }

    /**
     * Outlines the annular wedge between two angles and two radii with the same
     * hair of overlap {@link #drawWedge(DrawSurface, double, double, double,
     * double)} fills with, so a boxplot box or another filled shape keeps its
     * outline registered with its fill. A wedge reaching the centre is outlined
     * as a pie slice with straight edges from the middle.
     *
     * @param gc     the surface to draw onto
     * @param thLow  the lower theta value in data space
     * @param thHigh the upper theta value in data space
     * @param rLow   the inner radius value in data space
     * @param rHigh  the outer radius value in data space
     */
    public void strokeWedge(DrawSurface gc, double thLow, double thHigh, double rLow, double rHigh) {
        var a0 = thetaAngle(thLow);
        var a1 = thetaAngle(thHigh);
        var rInner = rPixel(rLow);
        var rOuter = rPixel(rHigh);

        boolean forward = a1 >= a0;
        if (forward) {
            a0 -= OVERLAP;
            a1 += OVERLAP;
        } else {
            a0 += OVERLAP;
            a1 -= OVERLAP;
        }

        var cx = centerX();
        var cy = centerY();
        var deg0 = Math.toDegrees(a0);
        var sweep = Math.toDegrees(a1 - a0);

        gc.beginPath();
        if (rInner <= 0.5) {
            gc.moveTo(cx, cy);
            gc.lineTo(cx + rOuter * Math.cos(a0), cy + rOuter * Math.sin(a0));
            gc.arc(cx - rOuter, cy - rOuter, 2.0 * rOuter, 2.0 * rOuter, deg0, sweep);
            gc.closePath();
        } else {
            gc.moveTo(cx + rOuter * Math.cos(a0), cy + rOuter * Math.sin(a0));
            gc.arc(cx - rOuter, cy - rOuter, 2.0 * rOuter, 2.0 * rOuter, deg0, sweep);
            gc.lineTo(cx + rInner * Math.cos(a1), cy + rInner * Math.sin(a1));
            gc.arc(cx - rInner, cy - rInner, 2.0 * rInner, 2.0 * rInner, Math.toDegrees(a1), -sweep);
            gc.closePath();
        }
        gc.stroke();
    }

    /**
     * Clips the current path to the polar panel — the disc for a full turn,
     * the sector between the two limb rays for a {@link #end(double) partial
     * fan}. Geometry extrapolated beyond the data range — a LOESS confidence
     * band, a density kernel's tails, a contour grid's rim — is then cut off
     * at the rim and the angular edges instead of painting outside the plot.
     * The path is appended to the caller's current path, which issues
     * {@link DrawSurface#clip()} afterwards.
     *
     * @param gc the surface to draw onto
     */
    public void clipPanel(DrawSurface gc) {
        var cx = centerX();
        var cy = centerY();
        var r = radius();
        var a0 = thetaAngle(thetaLow());
        var a1 = thetaAngle(thetaHigh());
        if (Math.abs(angularSpan()) >= TWO_PI - 1e-9) {
            gc.moveTo(cx + r, cy);
            gc.arc(cx - r, cy - r, 2.0 * r, 2.0 * r, 0, 360);
        } else {
            gc.moveTo(cx, cy);
            gc.lineTo(cx + r * Math.cos(a0), cy + r * Math.sin(a0));
            gc.arc(cx - r, cy - r, 2.0 * r, 2.0 * r, Math.toDegrees(a0), Math.toDegrees(a1 - a0));
        }
        gc.closePath();
    }

    /**
     * Outlines the full circle or fan arc at a constant radius, the polar image
     * of a horizontal reference line ({@code Geoms.hline()}): the significance
     * threshold ring of a coxcomb, or the value ring of a radial plot. A full
     * turn strokes the whole circle; a {@link #end(double) narrow fan} strokes
     * the sector arc of the same opening as the panel background.
     *
     * @param gc         the surface to draw onto
     * @param radiusData data value of the radial scale the ring sits on
     */
    public void strokeRing(DrawSurface gc, double radiusData) {
        var a0 = thetaAngle(thetaLow());
        var a1 = thetaAngle(thetaHigh());
        var fullTurn = Math.abs(angularSpan()) >= TWO_PI - 1e-9;
        drawRing(gc, centerX(), centerY(), rPixel(radiusData), a0, a1, fullTurn);
    }

    /**
     * Draws a radial ray at a constant theta, the polar image of a vertical
     * reference line ({@code Geoms.vline()}): the cut-off spoke of a fan or a
     * full-turn plot. The ray spans the radial scale's data range, from the
     * inner border (or the centre of a full disc) out to the rim.
     *
     * @param gc        the surface to draw onto
     * @param thetaData data value of the theta scale the spoke sits on
     */
    public void strokeSpoke(DrawSurface gc, double thetaData) {
        var scale = radialScale();
        var ang = thetaAngle(thetaData);
        var cx = centerX();
        var cy = centerY();
        var r0 = rPixel(scale.minData());
        var r1 = rPixel(scale.maxData());
        gc.strokeLine(cx + r0 * Math.cos(ang), cy + r0 * Math.sin(ang),
                cx + r1 * Math.cos(ang), cy + r1 * Math.sin(ang));
    }

    /**
     * The angle of the middle of the drawn angular span, in screen radians —
     * the midpoint between the two limb rays of a {@link #end(double) partial
     * fan}. A full turn has no meaningful middle, so its angle is returned as
     * twelve o'clock, where the first angular band begins. Reference-line
     * annotations hang their captions on this ray.
     *
     * @return the screen angle in radians
     */
    public double fanMidAngle() {
        if (Math.abs(angularSpan()) >= TWO_PI - 1e-9) {
            return -HALF_PI;
        }
        return (thetaAngle(thetaLow()) + thetaAngle(thetaHigh())) / 2.0;
    }

    /**
     * Renders the polar panel: background fill, concentric radius guides, the
     * angular spokes, and the rim, with the theta tick values labelled around
     * the outside so the angular positions of the wedges can be read off.
     * When {@link #end(double)} narrows the span to a fan, the grey panel and
     * the rings are all drawn as sectors of the same opening instead of full
     * circles — the {@code coordRadial(start, end)} look. A partial fan skips
     * the circular rim (the outer x-axis ring), keeping only the spokes, their
     * tick marks, and the labels.
     *
     * @param gc the surface to draw onto
     */
    public void renderBackground(DrawSurface gc) {
        var cx = centerX();
        var cy = centerY();
        var r = radius();
        var span = angularSpan();
        var fullTurn = Math.abs(span) >= TWO_PI - 1e-9;
        var a0 = thetaAngle(thetaLow());
        var a1 = thetaAngle(thetaHigh());
        var rInnerA = innerRadius * r;

        gc.setFill(theme.plotBackground());
        if (fullTurn) {
            gc.fillRect(panelX, panelY, panelW, panelH);
        } else {
            fillSector(gc, rInnerA, r, a0, a1);
        }

        gc.setStroke(theme.gridLineColor());
        gc.setLineWidth(theme.gridLineWidth());

        // Concentric radius guides sit on the radial scale's own breaks, so
        // each ring lines up with its tick on the left radial axis (the
        // the radial major gridlines). A discrete radial scale — a pie's
        // single shared band — keeps evenly spaced rings instead.
        var rTicks = continuousRadialTicks();
        if (rTicks != null) {
            for (var v : rTicks) {
                var ri = rPixel(v);
                if (ri >= 2.0) {
                    drawRing(gc, cx, cy, ri, a0, a1, fullTurn);
                }
            }
        } else {
            var rings = Math.max(2, (int) Math.round(r / 90.0) + 1);
            for (var i = 1; i < rings; i++) {
                var ri = r * i / rings;
                drawRing(gc, cx, cy, ri, a0, a1, fullTurn);
            }
        }

        if (showThetaAxis) {
            var spokes = thetaTicks();
            var labelR = r + 18;
            gc.setStroke(theme.gridLineColor());
            gc.setLineWidth(theme.gridLineWidth());
            for (var tick : spokes) {
                var ang = thetaAngle(tick);
                gc.strokeLine(cx, cy, cx + r * Math.cos(ang), cy + r * Math.sin(ang));
                var tickX = cx + r * Math.cos(ang);
                var tickY = cy + r * Math.sin(ang);
                gc.setStroke(theme.axisTickColor());
                gc.setLineWidth(theme.axisLineWidth());
                gc.strokeLine(tickX, tickY, cx + (r + 4) * Math.cos(ang), cy + (r + 4) * Math.sin(ang));
                gc.setFont(theme.tickLabelFont());
                gc.setFill(theme.tickLabelColor());
                gc.setTextAlign(TextAlignment.CENTER);
                gc.setTextBaseline(VPos.CENTER);
                var label = thetaTickLabel(tick);
                if (thetaLabelAngle != null) {
                    // The axisGuide angle is relative to the tangent at
                    // the label's position: 0 lays the text along the ring, 90
                    // along the radius, upside-down labels turned over.
                    gc.save();
                    gc.translate(cx + labelR * Math.cos(ang), cy + labelR * Math.sin(ang));
                    gc.rotate(textRotation(tick, thetaLabelAngle).degrees());
                    gc.fillText(label, 0, 0);
                    gc.restore();
                } else {
                    gc.fillText(label, cx + labelR * Math.cos(ang), cy + labelR * Math.sin(ang));
                }
                gc.setStroke(theme.gridLineColor());
                gc.setLineWidth(theme.gridLineWidth());
            }
        }

        if (showThetaAxis && !isPartialFan()) {
            gc.setStroke(theme.axisLineColor());
            gc.setLineWidth(theme.axisLineWidth());
            drawRing(gc, cx, cy, r, a0, a1, fullTurn);
        }

        if (showRadialAxis && rTicks != null && rTicks.size() >= 2) {
            drawRadialAxis(gc, rTicks, cx, cy, r);
        }

        // The centered baseline is local to polar labels; reset it so later
        // layers (guides, axis titles) keep their own vertical alignment.
        gc.setTextBaseline(VPos.BASELINE);
    }

    /** Outlines a circle (full turn) or a sector arc (narrow fan) at a radius. */
    private void drawRing(DrawSurface gc, double cx, double cy, double ri, double a0, double a1, boolean fullTurn) {
        if (fullTurn) {
            gc.strokeOval(cx - ri, cy - ri, 2.0 * ri, 2.0 * ri);
        } else {
            gc.strokeArc(cx - ri, cy - ri, 2.0 * ri, 2.0 * ri, Math.toDegrees(a0), Math.toDegrees(a1 - a0));
        }
    }

    /**
     * Fills the annular sector between two angles and two radii — the fan
     * background of a {@code coordRadial} with a narrow span. A sector
     * reaching the centre uses the cheap atomic pie slice.
     */
    private void fillSector(DrawSurface gc, double rInner, double rOuter, double a0, double a1) {
        var cx = centerX();
        var cy = centerY();
        var deg0 = Math.toDegrees(a0);
        var sweep = Math.toDegrees(a1 - a0);
        if (rInner <= 0.5) {
            gc.fillArc(cx - rOuter, cy - rOuter, 2.0 * rOuter, 2.0 * rOuter, deg0, sweep);
            return;
        }
        gc.beginPath();
        gc.moveTo(cx + rInner * Math.cos(a0), cy + rInner * Math.sin(a0));
        gc.lineTo(cx + rOuter * Math.cos(a0), cy + rOuter * Math.sin(a0));
        gc.arc(cx - rOuter, cy - rOuter, 2.0 * rOuter, 2.0 * rOuter, deg0, sweep);
        gc.lineTo(cx + rInner * Math.cos(a1), cy + rInner * Math.sin(a1));
        gc.arc(cx - rInner, cy - rInner, 2.0 * rInner, 2.0 * rInner, Math.toDegrees(a1), Math.toDegrees(a0 - a1));
        gc.closePath();
        gc.fill();
    }

    /**
     * Draws the radial (r) axis, the {@code axisRenderer("r", "left")}
     * for {@code coordPolar()}, with the value labels and the axis title
     * rotated along the side. A break at radius fraction {@code f} has its
     * label level with its own concentric guide ring.
     *
     * <p>For a full turn — the ordinary coxcomb or rose — the axis is the
     * classic vertical spine a short gap left of the disc's bounding square,
     * running from the panel top down to the centre while values grow from
     * center (0) toward the top. For a partial plot — the {@code coordRadial}
     * fan with an {@link #end(double) end} — the classifier is inverted: the
     * ticks and their labels hug the fan's left limb as a compact stacked
     * guide, one entry per break like a small legend, instead of a long spine
     * at the plot background edge. Only the rotated axis title stays put on
     * the plot's left side.
     *
     * @param gc    the surface to draw onto
     * @param ticks the radial scale's break values
     * @param cx    the horizontal centre of the polar panel
     * @param cy    the vertical centre of the polar panel
     * @param r     the radius of the polar panel, in pixels
     */
    private void drawRadialAxis(DrawSurface gc, List<Double> ticks, double cx, double cy, double r) {
        var axisX = panelX;
        var labelFont = theme.tickLabelFont();
        gc.setFont(labelFont);
        gc.setTextBaseline(VPos.CENTER);
        var helper = new Text();
        helper.setFont(labelFont);
        var widest = 0.0;

        if (isPartialFan()) {
            widest = drawFanRadialAxis(gc, ticks, cx, cy, helper);
        } else if (effectiveRAxisInside()) {
            widest = drawInsideRadialAxis(gc, ticks, cx, cy, r, helper);
        } else {
            // The vertical spine on the left edge of the plot background runs
            // from the disc center upward to the top of the panel.
            gc.setStroke(theme.axisLineColor());
            gc.setLineWidth(theme.axisLineWidth());
            gc.strokeLine(axisX, panelY, axisX, cy);

            for (var v : ticks) {
                var py = cy - rPixel(v);
                gc.strokeLine(axisX, py, axisX - 5, py);
                var label = radialTickLabel(v);
                helper.setText(label);
                widest = Math.max(widest, helper.getLayoutBounds().getWidth());
                gc.setFill(theme.tickLabelColor());
                gc.setTextAlign(TextAlignment.RIGHT);
                gc.fillText(label, axisX - 10, py + 4);
            }
        }

        if (radialAxisTitle != null && !radialAxisTitle.isEmpty()) {
            gc.save();
            gc.setFill(theme.axisTitleColor());
            gc.setFont(theme.axisTitleFont());
            gc.setTextAlign(TextAlignment.CENTER);
            // Centred on the vertical extent of the drawn radial plot itself —
            // the fan or the full disc — rather than on the whole panel, so the
            // title hangs level with the plot it names even when a narrow fan
            // only fills the top of the disc.
            gc.translate(axisX - 10 - widest - 8, plotCenterY());
            gc.rotate(-90);
            gc.fillText(radialAxisTitle, 0, 0);
            gc.restore();
        }
    }

    /**
     * The vertical midpoint, in pixels, of the drawn radial plot — the sector
     * between the two limb rays of a partial fan, or the full disc of a whole
     * turn. The radial axis title is hung on this so it is centred on the plot
     * rather than on the empty panel below a narrow fan.
     */
    private double plotCenterY() {
        var a0 = thetaAngle(thetaLow());
        var a1 = thetaAngle(thetaHigh());
        if (Math.abs(a1 - a0) < 1e-9) {
            return centerY();
        }
        var minSin = Double.POSITIVE_INFINITY;
        var maxSin = Double.NEGATIVE_INFINITY;
        for (var i = 0; i <= 64; i++) {
            var s = Math.sin(a0 + (a1 - a0) * i / 64.0);
            minSin = Math.min(minSin, s);
            maxSin = Math.max(maxSin, s);
        }
        return centerY() + radius() * (minSin + maxSin) / 2.0;
    }

    /** @return whether {@link #end(double)} narrows the plot to a fan arc */
    private boolean isPartialFan() {
        return Math.abs(angularSpan()) < TWO_PI - 1e-9;
    }

    /**
     * Whether the radial axis is drawn on the inner radius. Follows the
     * explicit {@link #rAxisInside(Mode)} choice when set, otherwise the
     * default for {@code radialAxisInside = NULL}: inside for a partial
     * fan, outside for a full turn.
     */
    private boolean effectiveRAxisInside() {
        return rAxisInside == Mode.AUTO ? isPartialFan() : rAxisInside == Mode.YES;
    }

    /**
     * Draws the radial axis of a full-turn polar plot pulled inside the donut
     * hole, the {@code radialAxisInside = TRUE} of the
     * {@code coordRadial()}: a short spine running up the twelve-o'clock ray
     * from the inner radius to the rim — the data range of a hollow-centred
     * plot — with a tick mark stepping right at each break and its label
     * reading to the left, level with its own concentric guide ring. The labels
     * thus sit on the hole's boundary rather than off the side of the panel.
     *
     * @param gc     the surface to draw onto
     * @param ticks  the radial scale's break values
     * @param cx     the horizontal centre of the polar panel
     * @param cy     the vertical centre of the polar panel
     * @param r      the radius of the polar panel, in pixels
     * @param helper a scratch text node used to measure label widths
     * @return the width of the widest value label, in pixels
     */
    private double drawInsideRadialAxis(DrawSurface gc, List<Double> ticks, double cx, double cy, double r, Text helper) {
        var innerR = innerRadius * r;
        var widest = 0.0;
        gc.setStroke(theme.axisLineColor());
        gc.setLineWidth(theme.axisLineWidth());
        gc.strokeLine(cx, cy - innerR, cx, cy - r);
        for (var v : ticks) {
            var py = cy - rPixel(v);
            gc.strokeLine(cx, py, cx + 5, py);
            var label = radialTickLabel(v);
            helper.setText(label);
            widest = Math.max(widest, helper.getLayoutBounds().getWidth());
            gc.setFill(theme.tickLabelColor());
            gc.setTextAlign(TextAlignment.RIGHT);
            gc.fillText(label, cx - 6, py + 4);
        }
        return widest;
    }

    /**
     * Draws the radial axis of a partial {@code coordRadial} fan as a compact
     * guide sitting on the fan's left limb: the boundary ray with the smaller
     * horizontal reach out of the two. Each tick lands on that ray at its own
     * radius and traces the tangent of its own concentric ring, so the mark
     * follows the arc of the radial grid as it steps outside the fan. Its
     * value label sits to the left of the mark, and the breaks read as a
     * little vertical legend beside the plot rather than as a spine divorced
     * from the geometry.
     *
     * @param gc     the surface to draw onto
     * @param ticks  the radial scale's break values
     * @param cx     the horizontal centre of the polar panel
     * @param cy     the vertical centre of the polar panel
     * @param helper a scratch text node used to measure label widths
     * @return the width of the widest value label, in pixels
     */
    private double drawFanRadialAxis(DrawSurface gc, List<Double> ticks, double cx, double cy, Text helper) {
        var a0 = thetaAngle(thetaLow());
        var a1 = thetaAngle(thetaHigh());
        var onLeft = Math.cos(a0) <= Math.cos(a1);
        var aL = onLeft ? a0 : a1;
        var dx = Math.cos(aL);
        var dy = Math.sin(aL);
        // The ring's tangent direction at the limb stepping out of the fan, so
        // the tick mark continues the arc it sits on.
        var tx = onLeft ? Math.sin(aL) : -Math.sin(aL);
        var ty = onLeft ? -Math.cos(aL) : Math.cos(aL);
        final var TICK_LEN = 7.0;
        // The label hangs beyond the tick tip along the tangent, pulled a few
        // pixels further out along the limb's outward radius, so it sits clear
        // of the tick in the margin outside the fan — whatever the fan's
        // orientation. A fixed screen offset would only be right for one limb.
        final var LABEL_GAP = 6.0;
        final var SIDE_GAP = 3.0;

        var widest = 0.0;
        gc.setStroke(theme.axisLineColor());
        gc.setLineWidth(theme.axisLineWidth());
        for (var v : ticks) {
            var rho = rPixel(v);
            if (rho < 2.0) {
                continue;
            }
            var px = cx + rho * dx;
            var py = cy + rho * dy;
            var tickTipX = px + TICK_LEN * tx;
            var tickTipY = py + TICK_LEN * ty;
            gc.strokeLine(px, py, tickTipX, tickTipY);
            var label = radialTickLabel(v);
            helper.setText(label);
            widest = Math.max(widest, helper.getLayoutBounds().getWidth());
            gc.setFill(theme.tickLabelColor());
            gc.setTextAlign(TextAlignment.RIGHT);
            gc.fillText(label,
                    tickTipX + LABEL_GAP * tx + SIDE_GAP * dx,
                    tickTipY + LABEL_GAP * ty + SIDE_GAP * dy);
        }
        return widest;
    }
}
