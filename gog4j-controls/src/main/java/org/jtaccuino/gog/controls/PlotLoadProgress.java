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
package org.jtaccuino.gog.controls;

/**
 * Accounting for in-flight plot work, so a host can surface a single "loading"
 * indicator across many {@link PlotCard}s. {@link PlotCard} never depends on a
 * concrete implementation; the sampler's shared {@code LoadProgress} adapter
 * implements this.
 */
public interface PlotLoadProgress {

    /** A plot factory started running on a worker thread. */
    void beginLoad();

    /** A plot factory finished (success, failure or cancellation). */
    void endLoad();

    /** A finished figure was queued for installation on the FX thread. */
    void enqueueInstall();

    /** A queued figure was installed. */
    void dequeueInstall();

    /** No-op progress used when the host does not report loading. */
    PlotLoadProgress NONE = new PlotLoadProgress() {
        @Override
        public void beginLoad() {
        }

        @Override
        public void endLoad() {
        }

        @Override
        public void enqueueInstall() {
        }

        @Override
        public void dequeueInstall() {
        }
    };
}
