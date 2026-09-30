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

/**
 * The layer lifecycle and the concrete geometries.
 *
 * <p>A layer runs a two-phase lifecycle — prepare once over the global data,
 * then render each panel — with hover hit-testing against the same prepared
 * model. This package holds the geometry implementations and their
 * configurators, plus the position adjustments that arrange marks within a
 * panel.
 */
package org.jtaccuino.gog.layer;
