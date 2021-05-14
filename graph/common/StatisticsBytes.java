/*
 * Copyright (C) 2021 Vaticle
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

package com.vaticle.typedb.core.graph.common;

import com.vaticle.typedb.core.common.bytes.ByteArray;
import com.vaticle.typedb.core.graph.iid.VertexIID;

import static com.vaticle.typedb.core.common.bytes.ByteArray.join;
import static com.vaticle.typedb.core.common.bytes.ByteArray.raw;

public class StatisticsBytes {
    public static ByteArray vertexCountKey(VertexIID.Type typeIID) {
        return join(
                Encoding.Prefix.STATISTICS_THINGS.byteArray(),
                typeIID.byteArray(),
                Encoding.Statistics.Infix.VERTEX_COUNT.byteArray());
    }

    public static ByteArray vertexTransitiveCountKey(VertexIID.Type typeIID) {
        return join(
                Encoding.Prefix.STATISTICS_THINGS.byteArray(),
                typeIID.byteArray(),
                Encoding.Statistics.Infix.VERTEX_TRANSITIVE_COUNT.byteArray());
    }

    public static ByteArray hasEdgeCountKey(VertexIID.Type thingTypeIID, VertexIID.Type attTypeIID) {
        return join(
                Encoding.Prefix.STATISTICS_THINGS.byteArray(),
                thingTypeIID.byteArray(),
                Encoding.Statistics.Infix.HAS_EDGE_COUNT.byteArray(),
                attTypeIID.byteArray());
    }

    public static ByteArray hasEdgeTotalCountKey(VertexIID.Type thingTypeIID) {
        return join(
                Encoding.Prefix.STATISTICS_THINGS.byteArray(),
                thingTypeIID.byteArray(),
                Encoding.Statistics.Infix.HAS_EDGE_TOTAL_COUNT.byteArray());
    }

    public static ByteArray countJobKey() {
        return join(
                Encoding.Prefix.STATISTICS_COUNT_JOB.byteArray());
    }

    public static ByteArray attributeCountJobKey(VertexIID.Attribute<?> attIID) {
        return join(
                Encoding.Prefix.STATISTICS_COUNT_JOB.byteArray(),
                Encoding.Statistics.JobType.ATTRIBUTE_VERTEX.byteArray(),
                attIID.byteArray());
    }

    public static ByteArray attributeCountedKey(VertexIID.Attribute<?> attIID) {
        return join(
                Encoding.Prefix.STATISTICS_COUNTED.byteArray(),
                attIID.byteArray());
    }

    public static ByteArray hasEdgeCountJobKey(VertexIID.Thing thingIID, VertexIID.Attribute<?> attIID) {
        return join(
                Encoding.Prefix.STATISTICS_COUNT_JOB.byteArray(),
                Encoding.Statistics.JobType.HAS_EDGE.byteArray(),
                thingIID.byteArray(),
                attIID.byteArray()
        );
    }

    public static ByteArray hasEdgeCountedKey(VertexIID.Thing thingIID, VertexIID.Attribute<?> attIID) {
        return join(
                Encoding.Prefix.STATISTICS_COUNTED.byteArray(),
                thingIID.byteArray(),
                Encoding.Statistics.Infix.HAS_EDGE_COUNT.byteArray(),
                attIID.byteArray()
        );
    }

    public static ByteArray snapshotKey() {
        return raw(Encoding.Prefix.STATISTICS_SNAPSHOT.bytes());
    }
}
