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

import java.util.Arrays;

public class SuffixIID extends IID {

    private SuffixIID(ByteArray byteArray) {
        super(byteArray);
    }

    public static SuffixIID of(ByteArray byteArray) {
        return new SuffixIID(byteArray);
    }

    @Override
    public String toString() {
        if (readableString == null) readableString = "Suffix: " + Arrays.toString(byteArray().bytes());
        return readableString;
    }
}
