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

import grakn.core.common.bytes.ByteArray;
import com.vaticle.typedb.core.graph.common.Encoding;

import java.util.Arrays;

import static com.vaticle.typedb.core.common.bytes.ByteArray.join;
import static grakn.core.common.bytes.ByteArray.slice;

public abstract class InfixIID<EDGE_ENCODING extends Encoding.Edge> extends IID {

    static final int LENGTH = 1;

    private InfixIID(ByteArray byteArray) {
        super(byteArray);
    }

    abstract EDGE_ENCODING encoding();

    boolean isOutwards() {
        return Encoding.Edge.isOut(byteArray().get(0));
    }

    public int length() {
        return byteArray().length();
    }

    @Override
    public String toString() { // TODO
        if (readableString == null) {
            readableString = "[1:" + Encoding.Infix.of(byteArray().get(0)).toString() + "]";
            if (byteArray().length() > 1) {
                readableString += "[" + (byteArray().length() - 1) + ": " +
                        Arrays.toString(slice(byteArray(), 1, byteArray().length() - 1).bytes()) + "]";
            }
        }
        return readableString;
    }

    public static class Type extends InfixIID<Encoding.Edge.Type> {

        private Type(ByteArray byteArray) {
            super(byteArray);
            assert byteArray.length() == LENGTH;
        }

        static Type of(Encoding.Infix infix) {
            return new Type(infix.byteArray());
        }

        static Type extract(ByteArray byteArray, int from) {
            return new Type(ByteArray.raw(byteArray.get(from)));
        }

        @Override
        Encoding.Edge.Type encoding() {
            return Encoding.Edge.Type.of(byteArray().get(0));
        }
    }

    public static class Thing extends InfixIID<Encoding.Edge.Thing> {

        private Thing(ByteArray byteArray) {
            super(byteArray);
        }

        static InfixIID.Thing extract(ByteArray byteArray, int from) {
            Encoding.Edge.Thing encoding = Encoding.Edge.Thing.of(byteArray.get(from));
            if ((encoding.equals(Encoding.Edge.Thing.ROLEPLAYER))) {
                return RolePlayer.extract(byteArray, from);
            } else {
                return new InfixIID.Thing(ByteArray.raw(byteArray.get(from)));
            }
        }

        public static InfixIID.Thing of(Encoding.Infix infix) {
            if (Encoding.Edge.Thing.of(infix).equals(Encoding.Edge.Thing.ROLEPLAYER)) {
                return new InfixIID.RolePlayer(infix.byteArray());
            } else {
                return new InfixIID.Thing(infix.byteArray());
            }
        }

        public static InfixIID.Thing of(Encoding.Infix infix, IID... tail) {
            ByteArray[] iidBytes = new ByteArray[tail.length + 1];
            iidBytes[0] = infix.byteArray();
            for (int i = 0; i < tail.length; i++) {
                iidBytes[i + 1] = tail[i].byteArray();
            }

            if (Encoding.Edge.Thing.of(infix).equals(Encoding.Edge.Thing.ROLEPLAYER)) {
                return new InfixIID.RolePlayer(join(iidBytes));
            } else {
                return new InfixIID.Thing(join(iidBytes));
            }
        }

        @Override
        Encoding.Edge.Thing encoding() {
            return Encoding.Edge.Thing.of(byteArray().get(0));
        }

        public InfixIID.Thing outwards() {
            if (isOutwards()) return this;
            return new InfixIID.Thing(join(ByteArray.raw(encoding().out().key()),
                    slice(byteArray(), 1, byteArray().length() - 1)));
        }

        public InfixIID.Thing inwards() {
            if (!isOutwards()) return this;
            return new InfixIID.Thing(join(ByteArray.raw(encoding().in().key()),
                    slice(byteArray(), 1, byteArray().length() - 1)));
        }

        public InfixIID.RolePlayer asRolePlayer() {
            if (this instanceof InfixIID.RolePlayer) return (InfixIID.RolePlayer) this;
            else assert false;
            return null;
        }
    }

    public static class RolePlayer extends InfixIID.Thing {

        private RolePlayer(ByteArray byteArray) {
            super(byteArray);
        }

        public static RolePlayer of(Encoding.Infix infix, VertexIID.Type type) {
            assert type != null && Encoding.Edge.Thing.of(infix).equals(Encoding.Edge.Thing.ROLEPLAYER);
            return new RolePlayer(join(infix.byteArray(), type.byteArray()));
        }

        static RolePlayer extract(ByteArray byteArray, int from) {
            return new RolePlayer(
                    join(ByteArray.raw(byteArray.get(from)),
                    VertexIID.Type.extract(byteArray, from + LENGTH).byteArray())
            );
        }

        public VertexIID.Type tail() {
            return VertexIID.Type.extract(byteArray(), LENGTH);
        }
    }
}
