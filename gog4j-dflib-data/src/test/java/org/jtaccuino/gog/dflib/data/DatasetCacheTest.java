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
package org.jtaccuino.gog.dflib.data;

import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/**
 * Verifies that every dataset loader is a single-owner cache: repeated access
 * returns the identical frame (no per-call reparse or duplicate static slots),
 * so a dataset exists once per JVM and browsing the gallery never loads the
 * same data twice.
 */
class DatasetCacheTest {

    @Test
    void repeatedLoadsReturnTheSameFrame() {
        assertSame(MpgDatasets.loadMpg(), MpgDatasets.loadMpg());
        assertSame(DiamondsDatasets.loadDiamonds(), DiamondsDatasets.loadDiamonds());
        assertSame(PenguinsDatasets.loadPenguins(), PenguinsDatasets.loadPenguins());
        assertSame(TipsDatasets.loadTips(), TipsDatasets.loadTips());
        assertSame(FaithfulDatasets.loadFaithful(), FaithfulDatasets.loadFaithful());
        assertSame(FaithfulDatasets.loadFaithfuld(), FaithfulDatasets.loadFaithfuld());
        assertSame(MtcarsDatasets.loadMtcars(), MtcarsDatasets.loadMtcars());
        assertSame(MtcarsDatasets.loadNumericMtcars(), MtcarsDatasets.loadNumericMtcars());
        assertSame(MtcarsDatasets.loadNamedMtcars(), MtcarsDatasets.loadNamedMtcars());
        assertSame(MeatDatasets.getProductionData(), MeatDatasets.getProductionData());
    }

    @Test
    void gwasManhattanIsAStableSingleFrame() {
        // The single plot-ready GWAS frame; both GwasDatasets and GwasPlots
        // route through the one soft-cached slot.
        assertSame(GwasDatasets.loadManhattan(), GwasDatasets.loadManhattan());
        assertSame(GwasDatasets.loadLeadSnps(), GwasDatasets.loadLeadSnps());
    }
}
