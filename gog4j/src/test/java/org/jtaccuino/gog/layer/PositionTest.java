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
package org.jtaccuino.gog.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PositionTest {

    @Test
    void dodgeDefaultFactoryReturnsTheEnumConstant() {
        assertSame(Position.DODGE, Positions.dodge());
        assertEquals(Position.DODGE, Positions.dodge().mode());
    }

    @Test
    void explicitDodgeWidthIsPreserved() {
        PositionAdjust.Dodge dodge = assertInstanceOf(PositionAdjust.Dodge.class, Positions.dodge(0.7));
        assertEquals(0.7, dodge.width());
        assertEquals(Position.DODGE, dodge.mode());
    }

    @Test
    void stackCarriesTheReverseFlag() {
        assertSame(Position.STACK, Positions.stack());
        assertEquals(Position.STACK, Positions.stack().mode());

        PositionAdjust.Stack reversed = assertInstanceOf(PositionAdjust.Stack.class, Positions.stack(true));
        assertEquals(true, reversed.reverse());
    }

    @Test
    void identityFillAndJitterReportTheirModes() {
        assertEquals(Position.IDENTITY, Positions.identity().mode());
        assertEquals(Position.FILL, Positions.fill().mode());
        assertEquals(Position.IDENTITY, Positions.jitter(0.1, 0.2).mode());
        assertEquals(Position.IDENTITY, Positions.nudge(1.0, 2.0).mode());
    }

    @Test
    void barePositionEnumsResolveToThemselves() {
        assertInstanceOf(PositionAdjust.class, Position.DODGE);
        // Each constant is its own default adjustment and reports its own mode.
        assertSame(Position.IDENTITY, Position.IDENTITY.adjust());
        assertSame(Position.DODGE, Position.DODGE.adjust());
        assertSame(Position.STACK, Position.STACK.adjust());
        assertSame(Position.FILL, Position.FILL.adjust());
        assertEquals(Position.IDENTITY, Position.IDENTITY.mode());
        assertEquals(Position.DODGE, Position.DODGE.mode());
        assertEquals(Position.STACK, Position.STACK.mode());
        assertEquals(Position.FILL, Position.FILL.mode());
    }

    @Test
    void chainsStartFromTheEnumConstant() {
        PositionAdjust.Dodge wide = assertInstanceOf(PositionAdjust.Dodge.class, Position.DODGE.dodge(0.5));
        assertEquals(0.5, wide.width());
        assertEquals(Position.DODGE, wide.mode());

        PositionAdjust.Stack reversed = assertInstanceOf(PositionAdjust.Stack.class, Position.STACK.stack(true));
        assertEquals(true, reversed.reverse());

        assertSame(Position.DODGE, Position.DODGE.dodge());
        assertSame(Position.STACK, Position.STACK.stack());
        assertSame(Position.FILL, Position.FILL.fill());
        assertSame(Position.IDENTITY, Position.IDENTITY.identity());
    }

    @Test
    void utilityClassCannotBeConstructed() {
        assertThrows(IllegalAccessException.class,
                () -> Positions.class.getDeclaredConstructor().newInstance());
    }
}
