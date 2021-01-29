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

package grakn.core.graph.common;

import grakn.core.common.bytes.ByteArray;
import grakn.core.common.exception.ErrorMessage;
import grakn.core.common.exception.GraknException;
import grakn.core.common.iterator.ResourceIterator;

import java.util.function.BiFunction;

import static grakn.common.util.Objects.className;
import static grakn.core.common.exception.ErrorMessage.Internal.ILLEGAL_CAST;

public interface Storage {

    boolean isOpen();

    ByteArray get(ByteArray key);

    ByteArray getLastKey(ByteArray prefix);

    void delete(ByteArray key);

    void put(ByteArray key);

    void put(ByteArray key, ByteArray value);

    void putUntracked(ByteArray key);

    void putUntracked(ByteArray key, ByteArray value);

    void mergeUntracked(ByteArray key, ByteArray value);

    <G> ResourceIterator<G> iterate(ByteArray key, BiFunction<ByteArray, ByteArray, G> constructor);

    GraknException exception(ErrorMessage error);

    GraknException exception(Exception exception);

    void close();

    default boolean isSchema() { return false; }

    default Schema asSchema() {
        throw exception(GraknException.of(ILLEGAL_CAST, className(this.getClass()), className(Schema.class)));
    }

    interface Schema extends Storage {

        KeyGenerator.Schema schemaKeyGenerator();

        default boolean isSchema() { return true; }

        default Schema asSchema() { return this; }
    }

    interface Data extends Storage {

        KeyGenerator.Data dataKeyGenerator();
    }
}
