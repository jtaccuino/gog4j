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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.jtaccuino.gog.MinMax;
import org.jtaccuino.gog.guide.Guide;
import org.jtaccuino.gog.guide.Guide.Direction;
import org.jtaccuino.gog.guide.GuideAlpha;
import org.jtaccuino.gog.guide.GuideColorbar;
import org.jtaccuino.gog.guide.GuideColorsteps;
import org.jtaccuino.gog.guide.GuideLegend;
import org.jtaccuino.gog.guide.GuideShading;
import org.jtaccuino.gog.scale.ColorScale;

/**
 * The registry of guides per aesthetic, following the {@code guides()}
 * function: a map from aesthetic to guide. An aesthetic that is not registered
 * falls back to the scale's default guide type; an explicitly suppressed
 * aesthetic ({@link #suppress(Aesthetic)}) renders no guide at all.
 * <p>
 * Guides are bound to aesthetics through {@link #guide(Aesthetic, Guide)}
 * mappings passed to {@code Plot.guides(...)}:
 *
 * <pre>{@code
 * ggplot(df, aes().x("displ").y("hwy").color("class"))
 *         .guides(guide(COLOR, guideLegend().ncol(2)))
 * }</pre>
 * <p>
 * Instances are immutable; each {@code set}/{@code suppress} call returns a new
 * registry with that single mapping changed.
 */
public final class Guides {

    /**
     * An aesthetic paired with the guide that should render for it.
     *
     * @param aesthetic the aesthetic the guide renders for
     * @param guide     the guide to render
     */
    public record Mapping(Aesthetic aesthetic, Guide<?> guide) {
    }

    /**
     * Binds a guide to an aesthetic — the building block of
     * {@code Plot.guides(...)}.
     *
     * @param aesthetic the aesthetic the guide renders for
     * @param guide the guide to render
     * @return a new mapping
     * @throws NullPointerException when either argument is null
     */
    public static Mapping guide(Aesthetic aesthetic, Guide<?> guide) {
        if (aesthetic == null || guide == null) {
            throw new NullPointerException("aesthetic and guide must not be null");
        }
        return new Mapping(aesthetic, guide);
    }

    private final Map<Aesthetic, Guide<?>> byAesthetic;
    private final boolean allSuppressed;

    private Guides(Map<Aesthetic, Guide<?>> byAesthetic, boolean allSuppressed) {
        this.byAesthetic = byAesthetic;
        this.allSuppressed = allSuppressed;
    }

    /** {@return a new empty, unsuppressed registry} */
    public static Guides empty() {
        return new Guides(new HashMap<>(), false);
    }

    /**
     * A registry that suppresses every guide, hiding all legends — the
     * {@code Guides} equivalent of the historical {@code legend(false)} toggle
     * (mirrors the {@code theme(GuidePosition = "none")}).
     *
     * @return a fully suppressed registry
     */
    public static Guides none() {
        return new Guides(new HashMap<>(), true);
    }

    /**
     * Creates a colorbar guide for a continuous scale — a gradient bar with
     * tick marks and labels at the break positions.
     *
     * @return a new colorbar guide with conventional defaults
     */
    public static GuideColorbar guideColorbar() {
        return new GuideColorbar(null, Direction.VERTICAL, false, 0,
                20.0, 180.0, 100, true, null, 0.5,
                null, 0.5, null, true, true);
    }

    /**
     * Creates a 3-D colorbar guide for a continuous scale — a gradient bar
     * shaded as if lit from one side. The default shading spans the full
     * {@code [-1, 1]} shade range in HSV.
     *
     * @return a new shaded colorbar guide with conventional defaults
     */
    public static GuideColorbar guideColorbar3d() {
        return guideColorbar().shade(GuideShading.DEFAULT);
    }

    /**
     * Creates a 3-D colorbar guide shading its gradient across the given range
     * of shade factors.
     *
     * @param range the shade factors spanned across the bar
     * @return a new shaded colorbar guide
     */
    public static GuideColorbar guideColorbar3d(MinMax range) {
        return guideColorbar().shade(GuideShading.of(range));
    }

    /**
     * Creates a 3-D colorbar guide shading its gradient across the given range
     * and direction.
     *
     * @param range         the shade factors spanned across the bar
     * @param reverseShade  {@code true} to flip the shading direction (mirrors
     *                      {@code reverse_shade = TRUE})
     * @return a new shaded colorbar guide
     */
    public static GuideColorbar guideColorbar3d(MinMax range, boolean reverseShade) {
        return guideColorbar().shade(GuideShading.of(range, reverseShade));
    }

    /**
     * Creates a colorsteps guide for a binned continuous scale — a colour bar
     * divided into discrete steps (mirrors {@code Guides.guideColorsteps()}).
     *
     * @return a new colorsteps guide with conventional defaults
     */
    public static GuideColorsteps guideColorsteps() {
        return new GuideColorsteps(null, Direction.VERTICAL, false, 0,
                20.0, 180.0, 100, true, null, 0.5,
                null, 0.5, null, true, true);
    }

    /**
     * Creates an alpha guide for the transparency aesthetic — a gradient bar
     * that ramps from transparent to opaque in the data's fill colour.
     *
     * @return a new alpha guide with conventional defaults
     */
    public static GuideAlpha guideAlpha() {
        return new GuideAlpha(null, Direction.VERTICAL, false, 0,
                20.0, 180.0, 100, 64.0, true, null, 0.5,
                null, 0.5, null, true, true);
    }

    /**
     * Creates a legend guide for a discrete scale — one key per category.
     *
     * @return a new legend guide with conventional defaults
     */
    public static GuideLegend guideLegend() {
        return new GuideLegend(null, Direction.VERTICAL, false, 0, 14.0, 20.0, 0, 0, false, 8.0);
    }

    /**
     * Creates a 3-D legend guide for a discrete scale — one key per category,
     * each shaded as a uniformly lit tile.
     *
     * @return a new shaded legend guide with conventional defaults
     */
    public static GuideLegend guideLegend3d() {
        return guideLegend().shade(GuideShading.DEFAULT);
    }

    /**
     * Registers the guide used for the given aesthetic.
     *
     * @param aesthetic the aesthetic the guide renders for
     * @param guide the guide to render for that aesthetic
     * @return a new registry with the mapping added
     */
    public Guides set(Aesthetic aesthetic, Guide<?> guide) {
        var next = new HashMap<>(byAesthetic);
        next.put(aesthetic, guide);
        return new Guides(next, allSuppressed);
    }

    /**
     * {@return a new registry with the mappings merged in, later ones winning}
     *
     * @param mappings the mappings to merge in
     */
    public Guides set(Mapping... mappings) {
        var next = this;
        for (var mapping : mappings) {
            next = next.set(mapping.aesthetic(), mapping.guide());
        }
        return next;
    }

    /**
     * Suppresses the guide for the given aesthetic (mirrors
     * {@code guides(aesthetic = "none")}).
     *
     * @param aesthetic the aesthetic to suppress
     * @return a new registry with the aesthetic suppressed
     */
    public Guides suppress(Aesthetic aesthetic) {
        return set(aesthetic, Guide.NONE);
    }

    /**
     * Suppresses every guide in this registry, hiding all legends.
     *
     * @return a fully suppressed copy of this registry
     */
    public Guides suppressAll() {
        return new Guides(byAesthetic, true);
    }

    /**
     * Returns the guide registered for an aesthetic, or {@link Guide#NONE}
     * when the aesthetic is not configured or is suppressed (so nothing is
     * drawn). Never returns {@code null}.
     *
     * @param aesthetic the aesthetic
     * @return the registered guide, or {@link Guide#NONE}
     */
    public Guide<?> forAesthetic(Aesthetic aesthetic) {
        if (allSuppressed) {
            return Guide.NONE;
        }
        return byAesthetic.getOrDefault(aesthetic, Guide.NONE);
    }

    /**
     * {@return whether a guide has been registered or suppressed for the aesthetic}
     *
     * @param aesthetic the aesthetic to check
     */
    public boolean isConfigured(Aesthetic aesthetic) {
        return byAesthetic.containsKey(aesthetic);
    }

    /**
     * {@return whether the aesthetic's guide is suppressed, or all guides are}
     *
     * @param aesthetic the aesthetic to check
     */
    public boolean isSuppressed(Aesthetic aesthetic) {
        return allSuppressed || Objects.equals(byAesthetic.get(aesthetic), Guide.NONE);
    }

    /** {@return whether every guide in this registry is suppressed} */
    public boolean isAllSuppressed() {
        return allSuppressed;
    }

    /** {@return an unmodifiable view of the configured guides keyed by aesthetic} */
    public Map<Aesthetic, Guide<?>> configured() {
        return Collections.unmodifiableMap(byAesthetic);
    }

    /**
     * The default guide type for a scale, following the scale-to-guide
     * mapping: continuous scales render a colorbar, discrete scales a legend.
     *
     * @param continuous {@code true} for a continuous (numeric/date) scale
     * @return a default {@link GuideColorbar} or {@link GuideLegend}
     */
    public static Guide<?> defaultFor(boolean continuous) {
        return continuous ? guideColorbar() : guideLegend();
    }

    /**
     * Validates that an explicitly configured guide accepts the colour scale's
     * value kind, following the error when a colourbar is bound to a
     * discrete scale or a legend to a continuous one.
     *
     * @param guide     the explicitly configured guide
     * @param scale     the scale the aesthetic maps
     * @param aesthetic the aesthetic, for the error message
     * @throws IllegalArgumentException when the guide and scale disagree
     */
    public static void validate(Guide<?> guide, ColorScale scale, Aesthetic aesthetic) {
        if (aesthetic == Aesthetic.SIZE || aesthetic == Aesthetic.SHAPE || aesthetic == Aesthetic.ALPHA) {
            return;
        }
        if (guide instanceof GuideColorbar && !scale.isContinuous()) {
            throw new IllegalArgumentException(
                    "A colorbar guide requires a continuous scale, but the '" + aesthetic.key()
                    + "' aesthetic maps discrete values");
        }
        if (guide instanceof GuideLegend && scale.isContinuous()) {
            throw new IllegalArgumentException(
                    "A legend guide requires a discrete scale, but the '" + aesthetic.key()
                    + "' aesthetic maps continuous values");
        }
    }
}
