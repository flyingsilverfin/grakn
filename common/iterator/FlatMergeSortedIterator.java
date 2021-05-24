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

package com.vaticle.typedb.core.common.iterator;

import com.vaticle.typedb.core.common.exception.TypeDBException;

import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.function.Function;

import static com.vaticle.typedb.core.common.exception.ErrorMessage.Internal.ILLEGAL_STATE;

public class FlatMergeSortedIterator<T, U extends Comparable<U>> extends AbstractFunctionalIterator.Sorted<U> {

    private final FunctionalIterator<T> source;
    private final Function<T, FunctionalIterator.Sorted<U>> flatMappingFn;
    private final PriorityQueue<QueueNode> nextQueue;
    private State state;
    private FunctionalIterator.Sorted<U> notInQueue;

    public FlatMergeSortedIterator(FunctionalIterator<T> source, Function<T, FunctionalIterator.Sorted<U>> flatMappingFn) {
        this.source = source;
        this.flatMappingFn = flatMappingFn;
        nextQueue = new PriorityQueue<>();
        this.state = State.INIT;
        this.notInQueue = null;
    }

    private enum State {
        INIT, NOT_READY, READY, COMPLETED;
    }

    private class QueueNode implements Comparable<QueueNode> {

        private FunctionalIterator.Sorted<U> iter;
        private U value;

        private QueueNode(FunctionalIterator.Sorted<U> iter, U value){
            this.iter = iter;
            this.value = value;
        }

        @Override
        public int compareTo(QueueNode other) {
            return value.compareTo(other.value);
        }
    }

    @Override
    public boolean hasNext() {
        switch (state) {
            case INIT:
                return initialise();
            case READY:
                return true;
            case NOT_READY:
                return fetchAndCheck();
            case COMPLETED:
                return false;
            default:
                throw TypeDBException.of(ILLEGAL_STATE);
        }
    }

    private boolean fetchAndCheck() {
        if (notInQueue != null) {
            assert notInQueue.hasNext();
            nextQueue.add(new QueueNode(notInQueue, notInQueue.peek()));
            notInQueue = null;
        }
        if (nextQueue.isEmpty()) state = State.COMPLETED;
        else state = State.READY;
        return state == State.READY;
    }

    private boolean initialise() {
        source.forEachRemaining(value -> {
            FunctionalIterator.Sorted<U> sortedIterator = flatMappingFn.apply(value);
            if (sortedIterator.hasNext()) {
                nextQueue.add(new QueueNode(sortedIterator, sortedIterator.peek()));
            }
        });
        source.recycle();
        if (nextQueue.isEmpty()) state = State.COMPLETED;
        else state = State.READY;
        return state == State.READY;
    }

    @Override
    public U next() {
        if (!hasNext()) throw new NoSuchElementException();
        QueueNode lowest = this.nextQueue.poll();
        FunctionalIterator.Sorted<U> iter = lowest.iter;
        U value = iter.next();
        state = State.NOT_READY;
        if (iter.hasNext()) notInQueue = iter;
        return value;
    }

    @Override
    public U peek() {
        if (!hasNext()) throw new NoSuchElementException();
        return nextQueue.peek().iter.peek();
    }

    @Override
    public void seek(U target) {
        // TODO
    }

    @Override
    public void recycle() {
        source.recycle();
    }
}