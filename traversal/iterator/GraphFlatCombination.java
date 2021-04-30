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
 *
 */

package grakn.core.traversal.iterator;

import grakn.core.common.iterator.FunctionalIterator;
import grakn.core.graph.GraphManager;
import grakn.core.graph.vertex.Vertex;
import grakn.core.traversal.Traversal;
import grakn.core.traversal.common.Identifier;
import grakn.core.traversal.common.Identifier.Variable.Retrievable;
import grakn.core.traversal.procedure.GraphProcedure;
import grakn.core.traversal.procedure.ProcedureEdge;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static grakn.core.common.iterator.Iterators.iterate;

public class GraphFlatCombination {

    private final GraphManager graphMgr;
    private final GraphProcedure procedure;
    private final Traversal.Parameters params;
    private final Map<Identifier, Set<? extends Vertex<?, ?>>> answer;

    public GraphFlatCombination(GraphManager graphMgr, GraphProcedure procedure, Traversal.Parameters params) {
        assert procedure.edgesCount() > 0;
        this.graphMgr = graphMgr;
        this.procedure = procedure;
        this.params = params;
        this.answer = new HashMap<>();
    }

    public Map<Retrievable, Vertex<?, ?>> get() {
        answer.put(procedure.startVertex().id(), procedure.startVertex().iterator(graphMgr, params).toSet());
        for (int pos = 0; pos < procedure.edgesCount(); pos++) {
            ProcedureEdge<?, ?> edge = procedure.edge(pos);
            if (answer.containsKey(edge.to().id())) closure(pos);
            else expand(pos);
        }

        return filtered(answer);
    }

    private Map<Retrievable, Vertex<?, ?>> filtered(Map<Identifier, Set<? extends Vertex<?, ?>>> answer) {
        Map<Retrievable, Vertex<?, ?>> filtered = new HashMap<>();
        iterate(answer.entrySet()).filter(entry -> entry.getKey().isRetrievable())
                .forEachRemaining(entry -> answer.put(entry.getKey(), entry.getValue()));
        return filtered;
    }

    private void expand(int pos) {
        ProcedureEdge<?, ?> edge = procedure.edge(pos);
        assert answer.containsKey(edge.from().id());
        edge.branch(graphMgr, ans)
    }

    private void closure(int pos) {
        ProcedureEdge<?, ?> edge = procedure.edge(pos);
        assert answer.containsKey(edge.from().id()) && answer.containsKey(edge.to().id());

    }


}
