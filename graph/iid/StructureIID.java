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

package grakn.core.graph.iid;

import grakn.core.common.bytes.ByteArray;
import grakn.core.graph.common.Encoding;
import grakn.core.graph.common.KeyGenerator;

import static grakn.core.common.bytes.ByteArray.join;
import static grakn.core.common.bytes.ByteArray.slice;
import static grakn.core.common.bytes.Bytes.sortedByteArrayToShort;

public abstract class StructureIID extends IID {

    StructureIID(ByteArray byteArray) {
        super(byteArray);
    }

    abstract Encoding.Structure encoding();

    @Override
    public String toString() {
        return null;
    }

    public static class Rule extends StructureIID {

        public static final int LENGTH = PrefixIID.LENGTH + 2;

        Rule(ByteArray byteArray) {
            super(byteArray);
            assert byteArray.length() == LENGTH;
        }

        public static Rule of(ByteArray byteArray) {
            return new Rule(byteArray);
        }

        /**
         * Generate an IID for a {@code RuleStructure} for a given {@code Encoding}
         *
         * @param keyGenerator to generate the IID for a {@code RuleStructure}
         * @return a byte array representing a new IID for a {@code RuleStructure}
         */
        public static Rule generate(KeyGenerator.Schema keyGenerator) {
            return of(join(Encoding.Structure.RULE.prefix().byteArray(), keyGenerator.forRule()));
        }

        @Override
        public Encoding.Structure.Rule encoding() {
            return Encoding.Structure.RULE;
        }

        @Override
        public String toString() {
            if (readableString == null) {
                readableString = "[" + PrefixIID.LENGTH + ": " + encoding().toString() + "][" +
                        (LENGTH - PrefixIID.LENGTH) + ": " +
                        sortedByteArrayToShort(slice(byteArray(), PrefixIID.LENGTH, LENGTH - PrefixIID.LENGTH)) + "]";
            }
            return readableString;
        }
    }
}
