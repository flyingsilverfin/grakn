/*
 * Grakn - A Distributed Semantic Database
 * Copyright (C) 2016-2018 Grakn Labs Limited
 *
 * Grakn is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grakn is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Grakn. If not, see <http://www.gnu.org/licenses/agpl.txt>.
 */

package ai.grakn.graql.internal.query.aggregate;

import ai.grakn.graql.Match;
import ai.grakn.graql.Var;
import ai.grakn.graql.admin.Answer;

import java.util.stream.Stream;

/**
 * Aggregate that finds minimum of a {@link Match}.
 */
class MinAggregate extends AbstractAggregate<Number> {

    private final Var varName;

    MinAggregate(Var varName) {
        this.varName = varName;
    }

    @Override
    public Number apply(Stream<? extends Answer> stream) {
        NumberPrimitiveTypeComparator comparator = new NumberPrimitiveTypeComparator();
        return stream.map(this::getValue).min(comparator).orElse(null);
    }

    @Override
    public String toString() {
        return "min " + varName;
    }

    private Number getValue(Answer result) {
        Object value = result.get(varName).asAttribute().value();

        if (value instanceof Number) return (Number) value;
        else throw new RuntimeException("Invalid attempt to compare non-Numbers in Max Aggregate function");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MinAggregate that = (MinAggregate) o;

        return varName.equals(that.varName);
    }

    @Override
    public int hashCode() {
        return varName.hashCode();
    }
}
