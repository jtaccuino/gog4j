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

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * A composable quality-vs-speed tradeoff for interactive rendering.
 * <p>
 * Fidelity-affecting optimizations (stroke suppression for small points,
 * overdraw binning in dense cells, coarser density grids) are gated behind
 * individual {@link Flag}s. The two presets {@link #FULL} and {@link #FAST}
 * cover the common cases — print/export quality versus interactive speed —
 * while {@link #of(Flag...)} and the {@code with}/{@code without} copy
 * setters allow fine-tuning a custom combination. Pure-compute deduplication
 * (cached density fits, memoized statistics, hoisted min/max scans) is
 * visually identical and therefore never gated.
 * <p>
 * When a plot renders to a vector backend ({@code SvgDrawSurface}) the
 * effective mode is always {@link #FULL}: an exported or zoomed SVG shows every
 * detail regardless of the interactive mode, so the attribute only ever lowers
 * fidelity on the interactive Canvas path.
 */
public final class RenderMode {

    /** A single fidelity-affecting optimization that may be enabled or disabled. */
    public enum Flag {
        /** Suppress the outline primitive for small points (fill-only). */
        SUPPRESS_STROKES,
        /** Pixel-quantize and drop fully-occluded points in dense panels. */
        BIN_OVERDRAW,
        /** Use a coarser grid for density/smoothing fits. */
        COARSE_DENSITY
    }

    private final Set<Flag> flags;

    private RenderMode(Set<Flag> flags) {
        this.flags = flags;
    }

    /** Full fidelity: every optimization disabled. */
    public static final RenderMode FULL = new RenderMode(EnumSet.noneOf(Flag.class));

    /** Maximum speed: every fidelity-affecting optimization enabled. */
    public static final RenderMode FAST = new RenderMode(EnumSet.allOf(Flag.class));

    /**
     * A custom mode enabling exactly the given flags.
     *
     * @param flags the optimizations to enable; empty enables none (i.e. {@link #FULL})
     * @return a new {@link RenderMode}
     */
    public static RenderMode of(Flag... flags) {
        return new RenderMode(EnumSet.copyOf(java.util.List.of(flags)));
    }

    /**
     * Whether the given fidelity-affecting optimization is enabled.
     *
     * @param flag the optimization to query
     * @return {@code true} when the flag is set
     */
    public boolean has(Flag flag) {
        return flags.contains(flag);
    }

    /**
     * Returns a copy with the given optimization enabled.
     *
     * @param flag the optimization to enable
     * @return an updated {@link RenderMode}
     */
    public RenderMode with(Flag flag) {
        var updated = EnumSet.copyOf(flags);
        updated.add(flag);
        return new RenderMode(updated);
    }

    /**
     * Returns a copy with the given optimization disabled.
     *
     * @param flag the optimization to disable
     * @return an updated {@link RenderMode}
     */
    public RenderMode without(Flag flag) {
        var updated = EnumSet.copyOf(flags);
        updated.remove(flag);
        return new RenderMode(updated);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RenderMode other && flags.equals(other.flags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flags);
    }

    @Override
    public String toString() {
        return flags.isEmpty() ? "FULL" : "RenderMode" + flags;
    }
}
