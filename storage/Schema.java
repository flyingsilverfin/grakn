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
 *
 */

package hypergraph.storage;

public class Schema {

    /**
     * The values in this class will be used as 'prefixes' within an IID in the
     * of every object database, and must not overlap with each other.
     *
     * The size of a prefix is 1 byte; i.e. min-value = 0 and max-value = 255.
     */
    private enum Prefix {
        INDEX_TYPE(0),
        INDEX_VALUE(5),
        VERTEX_ENTITY_TYPE(20),
        VERTEX_RELATION_TYPE(30),
        VERTEX_ROLE_TYPE(40),
        VERTEX_ATTRIBUTE_TYPE(50),
        VERTEX_ENTITY(60),
        VERTEX_RELATION(70),
        VERTEX_ROLE(80),
        VERTEX_ATTRIBUTE(90),
        VERTEX_VALUE(100),
        VERTEX_RULE(110);

        private final byte key;

        Prefix(int key) {
            this.key = (byte) key;
        }
    }

    /**
     * The values in this class will be used as 'infixes' between two IIDs of
     * two objects in the database, and must not overlap with each other.
     *
     * The size of a prefix is 1 byte; i.e. min-value = 0 and max-value = 255.
     */
    private enum Infix {
        PROPERTY_ABSTRACT(0),
        PROPERTY_DATATYPE(1),
        PROPERTY_REGEX(2),
        PROPERTY_VALUE(3),
        PROPERTY_VALUE_REF(4),
        PROPERTY_WHEN(5),
        PROPERTY_THEN(6),
        EDGE_SUB_OUT(20),
        EDGE_SUB_IN(25),
        EDGE_ISA_OUT(30),
        EDGE_ISA_IN(35),
        EDGE_KEY_OUT(40),
        EDGE_KEY_IN(45),
        EDGE_HAS_OUT(50),
        EDGE_HAS_IN(55),
        EDGE_PLAYS_OUT(60),
        EDGE_PLAYS_IN(65),
        EDGE_RELATES_OUT(70),
        EDGE_RELATES_IN(75),
        EDGE_OPT_ROLE_OUT(100),
        EDGE_OPT_ROLE_IN(105),
        EDGE_OPT_RELATION_OUT(110);

        private final byte key;

        Infix(int key) {
            this.key = (byte) key;
        }
    }

    public enum IndexType {
        TYPE(Prefix.INDEX_TYPE),
        VALUE(Prefix.INDEX_VALUE);

        private final byte prefix;

        IndexType(Prefix prefix) {
            this.prefix = prefix.key;
        }

        public byte prefix(){
            return prefix;
        }
    }

    public enum VertexType {
        ENTITY_TYPE(Prefix.VERTEX_ENTITY_TYPE),
        RELATION_TYPE(Prefix.VERTEX_RELATION_TYPE),
        ROLE_TYPE(Prefix.VERTEX_ROLE_TYPE),
        ATTRIBUTE_TYPE(Prefix.VERTEX_ATTRIBUTE_TYPE),
        ENTITY(Prefix.VERTEX_ENTITY),
        RELATION(Prefix.VERTEX_RELATION),
        ROLE(Prefix.VERTEX_ROLE),
        ATTRIBUTE(Prefix.VERTEX_ATTRIBUTE),
        VALUE(Prefix.VERTEX_VALUE),
        RULE(Prefix.VERTEX_RULE);

        private final byte prefix;

        VertexType(Prefix prefix) {
            this.prefix = prefix.key;
        }

        public byte prefix() {
            return prefix;
        }
    }

    public enum PropertyType {
        ABSTRACT(Infix.PROPERTY_ABSTRACT),
        DATATYPE(Infix.PROPERTY_DATATYPE),
        REGEX(Infix.PROPERTY_REGEX),
        VALUE(Infix.PROPERTY_VALUE),
        VALUE_REF(Infix.PROPERTY_VALUE_REF),
        WHEN(Infix.PROPERTY_WHEN),
        THEN(Infix.PROPERTY_THEN);

        private final byte infix;

        PropertyType(Infix infix) {
            this.infix = infix.key;
        }

        public byte infix() {
            return infix;
        }
    }

    public enum DataType {
        LONG(0),
        DOUBLE(2),
        STRING(4),
        BOOLEAN(6),
        DATE(8);

        private final byte value;

        DataType(int value) {
            this.value = (byte) value;
        }

        public byte value() {
            return value;
        }
    }

    public enum EdgeType {
        SUB_OUT(Infix.EDGE_SUB_OUT),
        SUB_IN(Infix.EDGE_SUB_IN),
        ISA_OUT(Infix.EDGE_ISA_OUT),
        ISA_IN(Infix.EDGE_ISA_IN),
        KEY_OUT(Infix.EDGE_KEY_OUT),
        KEY_IN(Infix.EDGE_KEY_IN),
        HAS_OUT(Infix.EDGE_HAS_OUT),
        HAS_IN(Infix.EDGE_HAS_IN),
        PLAYS_OUT(Infix.EDGE_PLAYS_OUT),
        PLAYS_IN(Infix.EDGE_PLAYS_IN),
        RELATES_OUT(Infix.EDGE_RELATES_OUT),
        RELATES_IN(Infix.EDGE_RELATES_IN),
        OPT_ROLE_OUT(Infix.EDGE_OPT_ROLE_OUT),
        OPT_ROLE_IN(Infix.EDGE_OPT_ROLE_IN),
        OPT_RELATION_OUT(Infix.EDGE_OPT_RELATION_OUT);

        private final byte infix;

        EdgeType(Infix infix) {
            this.infix = infix.key;
        }

        public byte infix() {
            return infix;
        }
    }
}
