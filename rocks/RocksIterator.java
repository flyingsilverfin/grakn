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

package grakn.core.rocks;

import grakn.core.common.bytes.ByteArray;
import grakn.core.common.iterator.AbstractResourceIterator;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

import static grakn.core.common.bytes.ByteArray.raw;
import static grakn.core.common.bytes.Bytes.bytesHavePrefix;

public final class RocksIterator<T> extends AbstractResourceIterator<T> implements AutoCloseable {

    private final ByteArray prefix;
    private final RocksStorage storage;
    private final AtomicBoolean isOpen;
    private final BiFunction<ByteArray, ByteArray, T> constructor;
    private org.rocksdb.RocksIterator internalRocksIterator;
    private State state;
    private T next;

    private enum State {INIT, EMPTY, FETCHED, COMPLETED}

    RocksIterator(RocksStorage storage, ByteArray prefix, BiFunction<ByteArray, ByteArray, T> constructor) {
        this.storage = storage;
        this.prefix = prefix;
        this.constructor = constructor;

        isOpen = new AtomicBoolean(true);
        state = State.INIT;
    }

    private void initalise() {
        this.internalRocksIterator = storage.getInternalRocksIterator();
        this.internalRocksIterator.seek(prefix.bytes());
    }

    private boolean fetchAndCheck() {
        byte[] key;
        if (!internalRocksIterator.isValid() || !bytesHavePrefix(key = internalRocksIterator.key(), prefix.bytes())) {
            state = State.COMPLETED;
            recycle();
            return false;
        }

        next = constructor.apply(raw(key), raw(internalRocksIterator.value()));
        internalRocksIterator.next();
        state = State.FETCHED;
        return true;
    }

    public final T peek() {
        if (!hasNext()) throw new NoSuchElementException();
        return next;
    }

    @Override
    public final boolean hasNext() {
        switch (state) {
            case COMPLETED:
                return false;
            case FETCHED:
                return true;
            case EMPTY:
                return fetchAndCheck();
            case INIT:
                initalise();
                return fetchAndCheck();
            default: // This should never be reached
                return false;
        }
    }

    @Override
    public final T next() {
        if (!hasNext()) throw new NoSuchElementException();
        state = State.EMPTY;
        return next;
    }

    @Override
    public void recycle() {
        close();
    }

    @Override
    public void close() {
        if (isOpen.compareAndSet(true, false)) {
            if (state != State.INIT) storage.recycle(internalRocksIterator);
            state = State.COMPLETED;
            storage.remove(this);
        }
    }
}