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

import ai.grakn.graql.Aggregate;
import ai.grakn.graql.Match;
import ai.grakn.graql.Var;
import ai.grakn.graql.admin.Answer;

import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * Aggregate that finds mean of a {@link Match}.
 */
class MeanAggregate extends AbstractAggregate<Number> {

    private final Var varName;
    private final CountAggregate countAggregate;
    private final Aggregate<Number> sumAggregate;

    MeanAggregate(Var varName) {
        this.varName = varName;
        countAggregate = new CountAggregate();
        sumAggregate = Aggregates.sum(varName);
    }

    @Override
    public Number apply(Stream<? extends Answer> stream) {
        List<? extends Answer> list = stream.collect(toList());

        long count = countAggregate.apply(list.stream());

        if (count == 0) {
            return null;
        } else {
            Number sum = sumAggregate.apply(list.stream());
            return sum.doubleValue() / count;
        }
    }

    @Override
    public String toString() {
        return "mean " + varName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MeanAggregate that = (MeanAggregate) o;

        return varName.equals(that.varName);
    }

    @Override
    public int hashCode() {
        return varName.hashCode();
    }
}
