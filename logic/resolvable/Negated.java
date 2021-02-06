/*
 * Copyright (C) 2021 Grakn Labs
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

package grakn.core.logic.resolvable;

import grakn.core.pattern.Disjunction;
import grakn.core.pattern.variable.Variable;

import java.util.HashSet;
import java.util.Set;

import static grakn.common.collection.Collections.set;
import static grakn.core.common.iterator.Iterators.iterate;

public class Negated extends Resolvable<Disjunction> {

    private final Set<Variable> generating;
    private final Set<Variable> variables;

    public Negated(Disjunction disjunction) {
        super(disjunction);
        this.generating = set();
        this.variables = new HashSet<>();
        pattern().conjunctions().forEach(c -> iterate(c.variables()).filter(v -> v.reference().isName())
                .forEachRemaining(variables::add));
    }

    @Override
    public Set<Variable> generating() {
        return generating;
    }

    @Override
    public Set<Variable> namedVariables() {
        return this.variables;
    }

    @Override
    public boolean isNegated() {
        return true;
    }

    @Override
    public Negated asNegated() {
        return this;
    }
}
