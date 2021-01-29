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

public class PrefixIID extends IID {

    public static final int LENGTH = 1;

    private PrefixIID(ByteArray byteArray) {
        super(byteArray);
        assert byteArray.length() == LENGTH;
    }

    public static PrefixIID of(Encoding.Prefix prefix) {
        return new PrefixIID(prefix.byteArray());
    }

    public static PrefixIID of(Encoding.Vertex encoding) {
        return new PrefixIID(encoding.prefix().byteArray());
    }

    @Override
    public String toString() {
        if (readableString == null) readableString = "[" + Encoding.Prefix.of(byteArray().get(0)).toString() + "]";
        return readableString;
    }
}
