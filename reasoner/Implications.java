/*
 * Copyright (C) 2020 Grakn Labs
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package grakn.core.reasoner;

import grakn.core.common.cache.CommonCache;
import grakn.core.concept.ConceptManager;

public class Implications {
    private ConceptManager conceptMgr;
    private final CommonCache<String, Implication> implicationCache;

    public Implications(ConceptManager conceptMgr, ReasonerCache cache) {
        this.conceptMgr = conceptMgr;
        this.implicationCache = cache.implicationCache();
    }

    public Implication getImplication(String name) {
        assert conceptMgr.getRule(name) != null;
        Implication implication = implicationCache.get(name, (label) -> new Implication(conceptMgr.getRule(label)));
        return implication;
    }

    public boolean containsPositiveCycle() {
        // TODO useful for optimising reasoner reiteration
        return false;
    }

    public boolean withinCycleWithNegation(Implication implication) {
        // TODO for rule validation
        return false;
    }

}
