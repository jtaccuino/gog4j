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
package org.jtaccuino.gog;

import org.jtaccuino.gog.coord.Coord;
import org.jtaccuino.gog.coord.Coord3D;
import org.jtaccuino.gog.coord.CubeFace;
import org.jtaccuino.gog.coord.CubePanel;
import org.jtaccuino.gog.coord.Light3d;
import org.jtaccuino.gog.coord.ScaleMode;
import org.jtaccuino.gog.scale.ScaleConfigurator;
import org.jtaccuino.gog.spi.DataExtractor;

/**
 * The 3D-flavoured {@link PlotDescriptor}: a descriptor that already carries a
 * {@link Coord3D} and adds the cube's view controls (rotation, camera, panels,
 * depth scaling) as first-class, fluid methods.
 * <p>
 * A {@code PlotDescriptor3D} <em>is a</em> {@link PlotDescriptor}, so it can be
 * rendered by {@link Ggplot#ggplot3d(PlotDescriptor3D)}, cloned into a
 * {@code PlotMatrix} cell, or passed anywhere a descriptor is accepted.  Being
 * a 3D descriptor, it is created through {@link Ggplot#plot3d(Object, Aes)} and
 * its matching node shortcut is {@link Ggplot#ggplot3d(Object, Aes)}:
 *
 * <pre>{@code
 * ggplot3d(df, aes().x("displ").y("hwy").z("drv"))
 *     .view(35, -75, -55)
 *     .panels(CubePanel.NONE)
 *     .light(Light3d.defaultLight())
 *     .geoms(col3d());
 * }</pre>
 * <p>
 * The methods declared here return the 3D type so the cube DSL chains without
 * losing it; the generic {@link PlotDescriptor} mutators ({@code geoms},
 * {@code theme}, {@code labs}, &hellip;) are inherited unchanged.  3D view
 * configuration is naturally applied before those generic calls, so the
 * inherited methods returning the plain descriptor is rarely noticed.
 * <p>
 * The coordinate system is fixed to {@link Coord3D}: {@link #coord(Coord)}
 * rejects any other {@link Coord}, since the cube controls below would have
 * nothing to act on in a 2D plot.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 * @see PlotDescriptor
 * @see Ggplot#plot3d(Object, Aes)
 */
public class PlotDescriptor3D<DF> extends PlotDescriptor<DF> {

    /**
     * Constructs a 3D descriptor holding the dataset, its extractor and the
     * global aesthetic mapping, with a fresh {@link Coord3D} installed.
     *
     * @param df        the dataset (e.g., DFLib {@code DataFrame})
     * @param extractor the extractor strategy for the dataset type
     * @param aes       the global aesthetic mappings (x, y, z, color, &hellip;)
     */
    PlotDescriptor3D(DF df, DataExtractor<DF> extractor, Aes aes) {
        super(df, extractor, aes);
        super.coord(new Coord3D());
    }

    /** {@return the installed 3D coordinate system} */
    private Coord3D coord3d() {
        return (Coord3D) coord();
    }

    /**
     * Sets the cube rotation in one call, the shorthand for
     * {@link #pitch(double)}, {@link #roll(double)} and {@link #yaw(double)}.
     *
     * @param pitch the rotation about the y axis in degrees
     * @param roll  the rotation about the x axis in degrees
     * @param yaw   the rotation about the z axis in degrees
     * @return this descriptor for fluid chaining
     */
    public PlotDescriptor3D<DF> view(double pitch, double roll, double yaw) {
        coord3d().pitch(pitch).roll(roll).yaw(yaw);
        return this;
    }

    /**
     * Sets the rotation about the y axis in degrees (default 0).
     *
     * @param pitch the rotation angle in degrees
     * @return this descriptor for fluid chaining
     */
    public PlotDescriptor3D<DF> pitch(double pitch) {
        coord3d().pitch(pitch);
        return this;
    }

    /**
     * Sets the rotation about the x axis in degrees (default -60).
     *
     * @param roll the rotation angle in degrees
     * @return this descriptor for fluid chaining
     */
    public PlotDescriptor3D<DF> roll(double roll) {
        coord3d().roll(roll);
        return this;
    }

    /**
     * Sets the rotation about the z axis in degrees (default -30).
     *
     * @param yaw the rotation angle in degrees
     * @return this descriptor for fluid chaining
     */
    public PlotDescriptor3D<DF> yaw(double yaw) {
        coord3d().yaw(yaw);
        return this;
    }

    /**
     * Enables (default) or disables perspective projection.
     *
     * @param persp {@code true} for perspective, {@code false} for orthographic
     * @return this descriptor for fluid chaining
     */
    public PlotDescriptor3D<DF> persp(boolean persp) {
        coord3d().persp(persp);
        return this;
    }

    /**
     * Sets the camera distance from the cube center (default 2, perspective only).
     *
     * @param dist the camera distance
     * @return this descriptor for fluid chaining
     */
    public PlotDescriptor3D<DF> dist(double dist) {
        coord3d().dist(dist);
        return this;
    }

    /**
     * Sets the global zoom factor (default 1, &gt; 1 zooms in).
     *
     * @param zoom the zoom factor
     * @return this descriptor for fluid chaining
     */
    public PlotDescriptor3D<DF> zoom(double zoom) {
        coord3d().zoom(zoom);
        return this;
    }

    /**
     * Sets the aspect scaling behaviour, {@link ScaleMode#FREE} (default) or
     * {@link ScaleMode#FIXED}.
     *
     * @param scales the aspect behaviour
     * @return this descriptor for fluid chaining
     */
    public PlotDescriptor3D<DF> scales(ScaleMode scales) {
        coord3d().scales(scales);
        return this;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to preserve the 3D type for fluid chaining, so a z-scale
     * configurator such as {@link org.jtaccuino.gog.scale.Scales#zlim(double, double)}
     * or {@link org.jtaccuino.gog.scale.Scales#scaleZContinuous(java.util.List)}
     * keeps the cube DSL available.
     *
     * @param configurator the scale configurator
     * @return this descriptor for fluid chaining
     */
    @Override
    public PlotDescriptor3D<DF> scales(ScaleConfigurator configurator) {
        super.scales(configurator);
        return this;
    }

    /**
     * Sets per-axis scaling ratios for non-uniform data ranges (default 1,1,1).
     *
     * @param x the scaling ratio applied along the x-axis
     * @param y the scaling ratio applied along the y-axis
     * @param z the scaling ratio applied along the z-axis
     * @return this descriptor for fluid chaining
     */
    public PlotDescriptor3D<DF> ratio(double x, double y, double z) {
        coord3d().ratio(x, y, z);
        return this;
    }

    /**
     * Controls whether axis ranges are expanded beyond the data (default {@code true}).
     *
     * @param expand {@code false} disables the default expansion
     * @return this descriptor for fluid chaining
     */
    public PlotDescriptor3D<DF> expand(boolean expand) {
        coord3d().expand(expand);
        return this;
    }

    /**
     * Controls whether layer content may draw outside the panel (default off).
     *
     * @param clip {@code true} clips to the panel, {@code false} (default) allows overflow
     * @return this descriptor for fluid chaining
     */
    public PlotDescriptor3D<DF> clip(boolean clip) {
        coord3d().clip(clip);
        return this;
    }

    /**
     * Selects which cube faces to render: {@link CubePanel#BACKGROUND} (default),
     * {@link CubePanel#FOREGROUND}, {@link CubePanel#ALL}, {@link CubePanel#NONE},
     * a position rule, or explicit face names.
     *
     * @param panels the faces to draw; empty uses the {@link CubePanel#BACKGROUND} default
     * @return this descriptor for fluid chaining
     */
    public PlotDescriptor3D<DF> panels(CubePanel... panels) {
        coord3d().panels(panels);
        return this;
    }

    /**
     * Sets the placement of the x-axis labels: left empty for the peripheral
     * auto edge (default) or given as a pair of adjacent faces.
     *
     * @param xlabels the placement spec ({@code none} for auto, or two {@link CubeFace}s)
     * @return this descriptor for fluid chaining
     */
    public PlotDescriptor3D<DF> xlabels(CubeFace... xlabels) {
        coord3d().xlabels(xlabels);
        return this;
    }

    /**
     * Sets the placement of the y-axis labels: left empty for the peripheral
     * auto edge (default) or given as a pair of adjacent faces.
     *
     * @param ylabels the placement spec ({@code none} for auto, or two {@link CubeFace}s)
     * @return this descriptor for fluid chaining
     */
    public PlotDescriptor3D<DF> ylabels(CubeFace... ylabels) {
        coord3d().ylabels(ylabels);
        return this;
    }

    /**
     * Sets the placement of the z-axis labels: left empty for the peripheral
     * auto edge (default) or given as a pair of adjacent faces.
     *
     * @param zlabels the placement spec ({@code none} for auto, or two {@link CubeFace}s)
     * @return this descriptor for fluid chaining
     */
    public PlotDescriptor3D<DF> zlabels(CubeFace... zlabels) {
        coord3d().zlabels(zlabels);
        return this;
    }

    /**
     * Sets whether axis labels rotate to align with their projected edge
     * (default {@code true}).
     *
     * @param rotateLabels {@code true} to auto-rotate labels
     * @return this descriptor for fluid chaining
     */
    public PlotDescriptor3D<DF> rotateLabels(boolean rotateLabels) {
        coord3d().rotateLabels(rotateLabels);
        return this;
    }

    /**
     * Sets the depth-scaling strengths for grid, border, ticks, and text
     * (default {@code 1} for all). A strength of 0 disables the perspective
     * size cue; axis titles are never depth-scaled.
     *
     * @param grid   the grid strength
     * @param border the border strength
     * @param ticks  the ticks strength
     * @param text   the text strength
     * @return this descriptor for fluid chaining
     */
    public PlotDescriptor3D<DF> scaleDepth(double grid, double border, double ticks, double text) {
        coord3d().scaleDepth(grid, border, ticks, text);
        return this;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to preserve the 3D type for fluid chaining.
     *
     * @param light the {@link Light3d}, or {@code null} for none
     * @return this descriptor for fluid chaining
     */
    @Override
    public PlotDescriptor3D<DF> light(Light3d light) {
        super.light(light);
        return this;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to preserve the 3D type and to enforce the 3D invariant: only
     * a {@link Coord3D} is accepted, since the cube view controls this
     * descriptor exposes act on it. Use {@link Ggplot#plot(Object, Aes)} for a
     * 2D descriptor.
     *
     * @param coord the coordinate system; must be a {@link Coord3D}
     * @return this descriptor for fluid chaining
     * @throws IllegalArgumentException if {@code coord} is not a {@link Coord3D}
     */
    @Override
    public PlotDescriptor3D<DF> coord(Coord coord) {
        if (!(coord instanceof Coord3D)) {
            throw new IllegalArgumentException(
                    "PlotDescriptor3D requires a Coord3D coordinate system");
        }
        super.coord(coord);
        return this;
    }
}
