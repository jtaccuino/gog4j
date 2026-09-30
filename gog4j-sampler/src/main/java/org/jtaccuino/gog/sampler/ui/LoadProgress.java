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
package org.jtaccuino.gog.sampler.ui;

import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import org.jtaccuino.gog.controls.PlotLoadProgress;

/**
 * Coalesced global counter of in-flight plot loads (running factories) and
 * queued figure installs. The sampler binds a single header indicator to
 * {@link #pendingProperty()} so a filter burst shows one "Loading\u2026"
 * status instead of a per-card spinner storm. Counter mutation is thread-safe;
 * the observable property is refreshed at most once per FX pulse, so a rapid
 * burst of counter changes costs a single UI update.
 */
public final class LoadProgress implements PlotLoadProgress {

    private static final LoadProgress INSTANCE = new LoadProgress();

    private final AtomicInteger inFlight = new AtomicInteger();
    private final AtomicInteger queued = new AtomicInteger();
    private final ReadOnlyIntegerWrapper pending = new ReadOnlyIntegerWrapper(0);
    private volatile boolean publishScheduled;

    /**
     * Creates a coordinator. The application uses the {@link #get() singleton};
     * a distinct instance is created only so the coordinator can be unit-tested
     * without sharing counters with the card machinery.
     */
    LoadProgress() {
    }

    /** The shared coordinator used by every card and the filter bar. */
    public static LoadProgress get() {
        return INSTANCE;
    }

    /** Records a plot factory about to run on a loader thread. */
        @Override
    public void beginLoad() {
        inFlight.incrementAndGet();
        schedulePublish();
    }

    /** Records a finished or cancelled plot factory. */
        @Override
    public void endLoad() {
        inFlight.decrementAndGet();
        schedulePublish();
    }

    /** Records a figure install queued for a later FX pulse. */
        @Override
    public void enqueueInstall() {
        queued.incrementAndGet();
        schedulePublish();
    }

    /** Records a queued install actually drained (or dropped as stale). */
        @Override
    public void dequeueInstall() {
        queued.decrementAndGet();
        schedulePublish();
    }

    /** The number of loads and installs still pending, refreshable by the UI. */
    public ReadOnlyIntegerProperty pendingProperty() {
        return pending.getReadOnlyProperty();
    }

    /** The current pending count, without waiting for the next FX publish. */
    public int pendingCount() {
        return inFlight.get() + queued.get();
    }

    /** Resets the counters and property; package-private for tests. */
    void resetForTest() {
        publishScheduled = false;
        inFlight.set(0);
        queued.set(0);
        pending.set(0);
    }

    private void schedulePublish() {
        if (publishScheduled) {
            return;
        }
        publishScheduled = true;
        Platform.runLater(this::publish);
    }

    private void publish() {
        publishScheduled = false;
        pending.set(pendingCount());
    }
}
