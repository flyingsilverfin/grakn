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

package ai.grakn.graql.internal.query;

import ai.grakn.GraknTx;
import ai.grakn.concept.SchemaConcept;
import ai.grakn.concept.Type;
import ai.grakn.exception.GraqlQueryException;
import ai.grakn.graql.InsertQuery;
import ai.grakn.graql.Match;
import ai.grakn.graql.admin.Answer;
import ai.grakn.graql.admin.InsertQueryAdmin;
import ai.grakn.graql.admin.MatchAdmin;
import ai.grakn.graql.admin.VarPatternAdmin;
import ai.grakn.util.CommonUtil;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A query that will insert a collection of variables into a graph
 *
 * @author Grakn Warriors
 */
@AutoValue
abstract class InsertQueryImpl extends AbstractQuery<List<Answer>, Answer> implements InsertQueryAdmin {

    /**
     * At least one of {@code tx} and {@code match} must be absent.
     *
     * @param vars a collection of Vars to insert
     * @param match the {@link Match} to insert for each result
     * @param tx the graph to execute on
     */
    static InsertQueryImpl create(Collection<VarPatternAdmin> vars, MatchAdmin match, GraknTx tx) {
        if (match != null && match.tx() != null) Preconditions.checkArgument(match.tx().equals(tx));

        if (vars.isEmpty()) {
            throw GraqlQueryException.noPatterns();
        }

        return new AutoValue_InsertQueryImpl(tx, match, ImmutableList.copyOf(vars));
    }

    @Override
    public final InsertQuery withTx(GraknTx tx) {
        if (match() != null) {
            return Queries.insert(varPatterns(), match().withTx(tx).admin());
        } else {
            return Queries.insert(varPatterns(), tx);
        }
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public final Stream<Answer> stream() {
        return executor().run(this);
    }

    @Override
    public InsertQueryAdmin admin() {
        return this;
    }

    @Override
    public Set<SchemaConcept> getSchemaConcepts() {
        if (getTx() == null) throw GraqlQueryException.noTx();
        GraknTx theGraph = getTx();

        Set<SchemaConcept> types = allVarPatterns()
                .map(VarPatternAdmin::getTypeLabel)
                .flatMap(CommonUtil::optionalToStream)
                .map(theGraph::<Type>getSchemaConcept)
                .collect(Collectors.toSet());

        if (match() != null) types.addAll(match().admin().getSchemaConcepts());

        return types;
    }

    private Stream<VarPatternAdmin> allVarPatterns() {
        return varPatterns().stream().flatMap(v -> v.innerVarPatterns().stream());
    }

    private GraknTx getTx() {
        if (match() != null && match().admin().tx() != null) return match().admin().tx();
        else return tx();
    }

    @Override
    public final String toString() {
        StringBuilder builder = new StringBuilder();

        if (match() != null) builder.append(match()).append("\n");
        builder.append("insert ");
        builder.append(varPatterns().stream().map(v -> v + ";").collect(Collectors.joining("\n")).trim());

        return builder.toString();
    }

    @Override
    public final List<Answer> execute() {
        return stream().collect(Collectors.toList());
    }

    @Override
    public final Boolean inferring() {
        if (match() != null) return match().admin().inferring();
        else return false;
    }
}
