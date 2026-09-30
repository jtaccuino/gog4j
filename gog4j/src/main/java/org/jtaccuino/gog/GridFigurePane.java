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

import javafx.geometry.VPos;
import javafx.scene.text.TextAlignment;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.theme.Theme;

/**
 * Shared figure chrome for the two grid composites — a {@link PlotMatrix} and
 * a {@link ComposedPlot}. Both reserve an optional title/subtitle band above
 * the cell grid and a caption below it, drawn by the two rendering helpers
 * {@link #drawMainTitle(DrawSurface, double, double, Theme)} and
 * {@link #drawCaption(DrawSurface, double, double, double, Theme)}. The fluent
 * setters keep the concrete subclass as their return type so chaining never
 * widens to this base.
 *
 * @param <S> the self type of the concrete figure, for fluid chaining
 */
public sealed abstract class GridFigurePane<S extends GridFigurePane<S>> extends GgFigurePane
        permits PlotMatrix, ComposedPlot {

    /** The figure title, or {@code null} when unset. */
    protected String title;
    /** The figure subtitle, or {@code null} when unset. */
    protected String subtitle;
    /** The figure caption, or {@code null} when unset. */
    protected String caption;

    /**
     * Constructs a grid figure with no chrome attached.
     */
    protected GridFigurePane() {
    }

    /**
     * Sets the figure title, drawn centered in the reserved top band. A figure
     * without a title reserves no top band.
     *
     * @param title the title text, or {@code null}/{@code ""} for none
     * @return this figure for fluid chaining
     */
    public S title(String title) {
        this.title = title;
        return self();
    }

    /**
     * The figure title.
     *
     * @return the title text, or {@code null} when unset
     */
    public String title() {
        return title;
    }

    /**
     * Sets a subtitle rendered directly beneath the title in the top band.
     * A figure with a subtitle but no title still reserves the top band.
     *
     * @param subtitle the subtitle text, or {@code null}/{@code ""} for none
     * @return this figure for fluid chaining
     */
    public S subtitle(String subtitle) {
        this.subtitle = subtitle;
        return self();
    }

    /**
     * The figure subtitle.
     *
     * @return the subtitle text, or {@code null} when unset
     */
    public String subtitle() {
        return subtitle;
    }

    /**
     * Sets a caption rendered right-aligned beneath the whole figure.
     *
     * @param caption the caption text, or {@code null}/{@code ""} for none
     * @return this figure for fluid chaining
     */
    public S caption(String caption) {
        this.caption = caption;
        return self();
    }

    /**
     * The figure caption.
     *
     * @return the caption text, or {@code null} when unset
     */
    public String caption() {
        return caption;
    }

    /**
     * The height of the reserved title band, or zero when neither title nor
     * subtitle is present.
     *
     * @param theme the theme carrying the band height
     * @return the band height in pixels
     */
    protected double reservedTitleBand(Theme theme) {
        return (title != null || subtitle != null) ? theme.titleBand() : 0.0;
    }

    /**
     * Draws the title and optional subtitle centered in the reserved top band.
     *
     * @param surface the surface to draw onto
     * @param width   the full figure width, in pixels
     * @param titleH  the reserved top band height, in pixels
     * @param theme   the theme carrying the title/subtitle fonts and colours
     */
    protected final void drawMainTitle(DrawSurface surface, double width, double titleH, Theme theme) {
        surface.save();
        surface.setTextAlign(TextAlignment.CENTER);
        surface.setTextBaseline(VPos.BASELINE);
        if (title != null && subtitle == null) {
            surface.setFont(theme.titleFont());
            surface.setFill(theme.titleColor());
            surface.fillText(title, width / 2.0, titleH * 0.65);
        } else if (title != null) {
            surface.setFont(theme.titleFont());
            surface.setFill(theme.titleColor());
            surface.fillText(title, width / 2.0, titleH * 0.42);
            surface.setFont(theme.subtitleFont());
            surface.setFill(theme.subtitleColor());
            surface.fillText(subtitle, width / 2.0, titleH * 0.82);
        } else {
            // subtitle only (no title)
            surface.setFont(theme.subtitleFont());
            surface.setFill(theme.subtitleColor());
            surface.fillText(subtitle, width / 2.0, titleH * 0.65);
        }
        surface.restore();
    }

    /**
     * Draws the caption right-aligned beneath the figure, avoiding the legend band.
     *
     * @param surface the surface to draw onto
     * @param width   the full figure width, in pixels
     * @param height  the full figure height, in pixels
     * @param legendW the width of the legend band to the right, zero when none
     * @param theme   the theme carrying the caption font and colour
     */
    protected final void drawCaption(DrawSurface surface, double width, double height,
            double legendW, Theme theme) {
        double rightEdge = legendW > 0 ? width - legendW - 12.0 : width - 12.0;
        surface.save();
        surface.setFont(theme.captionFont());
        surface.setFill(theme.captionColor());
        surface.setTextAlign(TextAlignment.RIGHT);
        surface.setTextBaseline(VPos.BOTTOM);
        surface.fillText(caption, rightEdge, height - 6.0);
        surface.restore();
    }

    @SuppressWarnings("unchecked")
    private S self() {
        return (S) this;
    }
}
