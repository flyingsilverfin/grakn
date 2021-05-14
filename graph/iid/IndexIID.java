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

package com.vaticle.typedb.core.graph.iid;

import com.vaticle.typedb.core.common.bytes.ByteArray;
import com.vaticle.typedb.core.common.exception.TypeDBException;
import com.vaticle.typedb.core.graph.common.Encoding;

import javax.annotation.Nullable;
import java.time.LocalDateTime;

import static com.vaticle.typedb.core.common.bytes.ByteArray.join;
import static com.vaticle.typedb.core.common.bytes.ByteArray.raw;
import static com.vaticle.typedb.core.common.bytes.ByteArray.slice;
import static com.vaticle.typedb.core.common.bytes.Bytes.DOUBLE_SIZE;
import static com.vaticle.typedb.core.common.bytes.Bytes.LONG_SIZE;
import static com.vaticle.typedb.core.common.bytes.Bytes.booleanToByteArray;
import static com.vaticle.typedb.core.common.bytes.Bytes.byteArrayToDateTime;
import static com.vaticle.typedb.core.common.bytes.Bytes.byteArrayToString;
import static com.vaticle.typedb.core.common.bytes.Bytes.byteToBoolean;
import static com.vaticle.typedb.core.common.bytes.Bytes.dateTimeToByteArray;
import static com.vaticle.typedb.core.common.bytes.Bytes.doubleToSortedByteArray;
import static com.vaticle.typedb.core.common.bytes.Bytes.longToSortedByteArray;
import static com.vaticle.typedb.core.common.bytes.Bytes.sortedByteArrayToDouble;
import static com.vaticle.typedb.core.common.bytes.Bytes.sortedByteArrayToLong;
import static com.vaticle.typedb.core.common.bytes.Bytes.stringToByteArray;
import static com.vaticle.typedb.core.common.exception.ErrorMessage.Internal.ILLEGAL_STATE;
import static com.vaticle.typedb.core.graph.common.Encoding.ValueType.STRING_ENCODING;
import static com.vaticle.typedb.core.graph.common.Encoding.ValueType.TIME_ZONE_ID;

public abstract class IndexIID extends IID {

    IndexIID(ByteArray byteBuffer) {
        super(byteBuffer);
    }


    public static abstract class Type extends IndexIID {

        Type(ByteArray byteBuffer) {
            super(byteBuffer);
        }

        public static class Label extends Type {

            Label(ByteArray byteBuffer) {
                super(byteBuffer);
            }

            /**
             * Returns the index address of given {@code TypeVertex}
             *
             * @param label of the {@code TypeVertex}
             * @param scope of the {@code TypeVertex}, which could be null
             * @return a byte array representing the index address of a {@code TypeVertex}
             */
            public static Label of(String label, @Nullable String scope) {
                return new Label(join(Encoding.Index.Prefix.TYPE.prefix().byteArray(),
                                      raw(Encoding.Vertex.Type.scopedLabel(label, scope).getBytes(STRING_ENCODING))));
            }

            @Override
            public String toString() {
                if (readableString == null) {
                    readableString = "[" + PrefixIID.LENGTH + ": " + Encoding.Index.Prefix.TYPE.toString() + "]" +
                            "[" + (byteArray().length() - PrefixIID.LENGTH) +
                            ": " + new String(slice(byteArray(), PrefixIID.LENGTH, byteArray().length() - PrefixIID.LENGTH).bytes(), STRING_ENCODING) + "]";
                }
                return readableString;
            }
        }

        // type -> rule indexing
        public static abstract class Rule extends Type {

            Rule(ByteArray byteBuffer) {
                super(byteBuffer);
            }

            public int length() { return byteArray().length(); }

            public static class Key extends Type.Rule {

                public Key(ByteArray byteBuffer) {
                    super(byteBuffer);
                }

                /**
                 * @return a byte array representing the index of a given type concluded in a given rule
                 */
                public static Key concludedVertex(VertexIID.Type typeIID, StructureIID.Rule ruleIID) {
                    return new Key(join(Encoding.Index.Prefix.TYPE.byteArray(), typeIID.byteArray(),
                                        Encoding.Index.Infix.CONCLUDED_VERTEX.byteArray(), ruleIID.byteArray()));
                }

                /**
                 * @return a byte array representing the index of a given type concluded in a given rule
                 */
                public static Key concludedEdgeTo(VertexIID.Type typeIID, StructureIID.Rule ruleIID) {
                    return new Key(join(Encoding.Index.Prefix.TYPE.byteArray(), typeIID.byteArray(),
                                        Encoding.Index.Infix.CONCLUDED_EDGE_TO.byteArray(), ruleIID.byteArray()));
                }


                /**
                 * @return a byte array representing the index of a given type contained in a given rule
                 */
                public static Key contained(VertexIID.Type typeIID, StructureIID.Rule ruleIID) {
                    return new Key(join(Encoding.Index.Prefix.TYPE.byteArray(), typeIID.byteArray(),
                                        Encoding.Index.Infix.CONTAINED_TYPE.byteArray(), ruleIID.byteArray()));
                }

                @Override
                public String toString() {
                    if (readableString == null) {
                        String prefix = "[" + PrefixIID.LENGTH + ": " + Encoding.Index.Prefix.TYPE.toString() + "]";
                        String typeIID = "[" + (VertexIID.Type.LENGTH) + ": " + byteArrayToString(
                                slice(byteArray(), PrefixIID.LENGTH, VertexIID.Type.LENGTH - PrefixIID.LENGTH), STRING_ENCODING) + "]";
                        String infix = "[" + (Encoding.Index.Infix.LENGTH) + ": " + Encoding.Index.Infix.of(
                                slice(byteArray(), PrefixIID.LENGTH + VertexIID.Type.LENGTH, Encoding.Index.Infix.LENGTH)) + "]";
                        readableString = prefix + typeIID + infix;
                    }
                    return readableString;
                }
            }

            public static class Prefix extends Type.Rule {

                public Prefix(ByteArray byteBuffer) {
                    super(byteBuffer);
                }

                /**
                 * @return a byte array representing the the index scan prefix of a given type concluded in rules
                 */
                public static Prefix concludedVertex(VertexIID.Type typeIID) {
                    return new Prefix(join(Encoding.Index.Prefix.TYPE.byteArray(), typeIID.byteArray(),
                                           Encoding.Index.Infix.CONCLUDED_VERTEX.byteArray()));
                }

                /**
                 * @return a byte array representing the index prefix scan of a given type concluded in rules
                 */
                public static Prefix concludedEdgeTo(VertexIID.Type typeIID) {
                    return new Prefix(join(Encoding.Index.Prefix.TYPE.byteArray(), typeIID.byteArray(),
                                           Encoding.Index.Infix.CONCLUDED_EDGE_TO.byteArray()));
                }

                /**
                 * @return a byte array representing the index scan prefix of a given type contained in rules
                 */
                public static Prefix contained(VertexIID.Type typeIID) {
                    return new Prefix(join(Encoding.Index.Prefix.TYPE.byteArray(), typeIID.byteArray(),
                                           Encoding.Index.Infix.CONTAINED_TYPE.byteArray()));
                }

                @Override
                public String toString() {
                    if (readableString == null) {
                        String prefix = "[" + PrefixIID.LENGTH + ": " + Encoding.Index.Prefix.TYPE.toString() + "]";
                        String typeIID = "[" + (VertexIID.Type.LENGTH) + ": " + byteArrayToString(
                                slice(byteArray(), PrefixIID.LENGTH, VertexIID.Type.LENGTH - PrefixIID.LENGTH), STRING_ENCODING) + "]";
                        String infix = "[" + (Encoding.Index.Infix.LENGTH) + ": " + Encoding.Index.Infix.of(
                                slice(byteArray(), PrefixIID.LENGTH + VertexIID.Type.LENGTH, Encoding.Index.Infix.LENGTH)) + "]";
                        String ruleIID = "[" + (StructureIID.Rule.LENGTH) + ": " + byteArrayToString(
                                slice(byteArray(), PrefixIID.LENGTH + VertexIID.Type.LENGTH + Encoding.Index.Infix.LENGTH,
                                            StructureIID.Rule.LENGTH), STRING_ENCODING) + "]";
                        readableString = prefix + typeIID + infix + ruleIID;
                    }
                    return readableString;
                }
            }
        }
    }


    public static class Rule extends IndexIID {

        Rule(ByteArray byteBuffer) { super(byteBuffer); }

        /**
         * Returns the index address of given {@code RuleStructure}
         *
         * @param label of the {@code RuleStructure}
         * @return a byte array representing the index address of a {@code RuleStructure}
         */
        public static Rule of(String label) {
            return new Rule(join(prefix().byteArray(), raw(label.getBytes(STRING_ENCODING))));
        }

        public static Encoding.Prefix prefix() {
            return Encoding.Index.Prefix.RULE.prefix();
        }

        @Override
        public String toString() {
            if (readableString == null) {
                readableString = "[" + PrefixIID.LENGTH + ": " + Encoding.Index.Prefix.RULE.toString() + "]" +
                        "[" + (byteArray().length() - PrefixIID.LENGTH) +
                        ": " + new String(slice(byteArray(), PrefixIID.LENGTH, byteArray().length() - PrefixIID.LENGTH).bytes(), STRING_ENCODING) + "]";
            }
            return readableString;
        }
    }

    public static class Attribute extends IndexIID {

        static final int VALUE_INDEX = PrefixIID.LENGTH + VertexIID.Attribute.VALUE_TYPE_LENGTH;

        Attribute(ByteArray byteArray) {
            super(byteArray);
        }

        private static Attribute newAttributeIndex(ByteArray valueType, ByteArray value, ByteArray typeIID) {
            return new Attribute(join(Encoding.Index.Prefix.ATTRIBUTE.prefix().byteArray(), valueType, value, typeIID)
            );
        }

        public static Attribute of(boolean value, VertexIID.Type typeIID) {
            return newAttributeIndex(Encoding.ValueType.BOOLEAN.byteArray(), booleanToByteArray(value), typeIID.byteArray());
        }

        public static Attribute of(long value, VertexIID.Type typeIID) {
            return newAttributeIndex(Encoding.ValueType.LONG.byteArray(), longToSortedByteArray(value), typeIID.byteArray());
        }

        public static Attribute of(double value, VertexIID.Type typeIID) {
            return newAttributeIndex(Encoding.ValueType.DOUBLE.byteArray(), doubleToSortedByteArray(value), typeIID.byteArray());
        }

        public static Attribute of(String value, VertexIID.Type typeIID) {
            ByteArray stringBytes;
            try {
                stringBytes = stringToByteArray(value, STRING_ENCODING);
            } catch (Exception e) {
                throw TypeDBException.of(ILLEGAL_STATE);
            }
            return newAttributeIndex(Encoding.ValueType.STRING.byteArray(), stringBytes, typeIID.byteArray());
        }

        public static Attribute of(LocalDateTime value, VertexIID.Type typeIID) {
            return newAttributeIndex(Encoding.ValueType.DATETIME.byteArray(), dateTimeToByteArray(value, TIME_ZONE_ID), typeIID.byteArray());
        }

        @Override
        public String toString() {
            if (readableString == null) {
                Encoding.ValueType valueType = Encoding.ValueType.of(byteArray().get(PrefixIID.LENGTH));
                String value;
                switch (valueType) {
                    case BOOLEAN:
                        value = String.valueOf(byteToBoolean(byteArray().get(VALUE_INDEX)));
                        break;
                    case LONG:
                        value = sortedByteArrayToLong(slice(byteArray(), VALUE_INDEX, LONG_SIZE)) + "";
                        break;
                    case DOUBLE:
                        value = sortedByteArrayToDouble(slice(byteArray(), VALUE_INDEX, DOUBLE_SIZE)) + "";
                        break;
                    case STRING:
                        value = byteArrayToString(slice(byteArray(), VALUE_INDEX, byteArray().length() - VertexIID.Type.LENGTH - VALUE_INDEX), STRING_ENCODING);
                        break;
                    case DATETIME:
                        value = byteArrayToDateTime(slice(byteArray(), VALUE_INDEX, byteArray().length() - VertexIID.Type.LENGTH - VALUE_INDEX), TIME_ZONE_ID).toString();
                        break;
                    default:
                        value = "";
                        break;
                }

                readableString = "[" + PrefixIID.LENGTH + ": " + Encoding.Index.Prefix.ATTRIBUTE.toString() + "]" +
                        "[" + VertexIID.Attribute.VALUE_TYPE_LENGTH + ": " + valueType.toString() + "]" +
                        "[" + (byteArray().length() - (PrefixIID.LENGTH + VertexIID.Attribute.VALUE_TYPE_LENGTH + VertexIID.Type.LENGTH)) + ": " + value + "]" +
                        "[" + VertexIID.Type.LENGTH + ": " + VertexIID.Type.of(slice(byteArray(), byteArray().length() - VertexIID.Type.LENGTH, VertexIID.Type.LENGTH)).toString() + "]";
            }
            return readableString;
        }
    }
}
