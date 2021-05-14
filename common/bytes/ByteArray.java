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

package  com.vaticle.typedb.core.common.bytes;

import com.vaticle.typedb.core.common.exception.TypeDBException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.vaticle.typedb.core.common.exception.ErrorMessage.Internal.ILLEGAL_STATE;

// TODO: Remove bytes() -> byte[] from Encoding?
// TODO: Add slice instance method
// TODO: Clean up Bytes class
public abstract class ByteArray implements Comparable<ByteArray> {

    private byte[] bytesCache = null;
    private int hashCodeCache = 0;

    public abstract byte get(int i);

    public abstract int length();

    public boolean isEmpty() {
        return length() == 0;
    }

    protected abstract byte[] getBytes();

    protected abstract int getHashCode();

    public byte[] bytes() {
        if (bytesCache != null) return bytesCache;
        bytesCache = getBytes();
        return bytesCache;
    }

    @Override
    public final int hashCode() {
        if (hashCodeCache != 0) return hashCodeCache;
        hashCodeCache = getHashCode();
        return hashCodeCache;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ByteArray)) return false;
        ByteArray other = (ByteArray) o;
        return Arrays.equals(bytes(), other.bytes());
    }

    @Override
    public int compareTo(ByteArray that) {
        int n = Math.min(length(), that.length());
        for (int i = 0; i < n; i++) {
            int cmp = Byte.compare(this.get(i), that.get(i));
            if (cmp != 0) return cmp;
        }
        if (length() == that.length()) return 0;
        else if (length() < that.length()) return -1;
        else return 1;
    }

    public static Single raw(byte aByte) {
        return new Single(new byte[] {aByte}, 0, 1);
    }

    public static Single raw(byte[] bytes) {
        return new Single(bytes, 0, bytes.length);
    }

    public static ByteArray slice(ByteArray byteArray, int start, int length) {
        if (byteArray instanceof Single) {
            return new Single(((Single) byteArray).bytes, ((Single) byteArray).start + start, length);
        } else if (byteArray instanceof Multi) {
            List<Single> byteArrayList = ((Multi)byteArray).byteArrays;
            List<Single> newByteArrayList = new ArrayList<>();
            for (int i = 0; i < byteArrayList.size() && length > 0; i++) {
                Single single = byteArrayList.get(i);
                if (start < single.length) {
                    int singleLength = Math.min(length, single.length - start);
                    newByteArrayList.add(new Single(single.bytes, single.start + start, singleLength));
                    start = 0;
                    length -= singleLength;
                } else {
                    start -= single.length;
                }
            }
            return new Multi(newByteArrayList);
        }
        throw TypeDBException.of(ILLEGAL_STATE);
    }

    public static Multi join(ByteArray... byteArrayList) {
        List<Single> newByteArrayList = new ArrayList<>();
        for (ByteArray byteArray : byteArrayList) {
            if (byteArray instanceof Single) {
                newByteArrayList.add((Single) byteArray);
            } else if (byteArray instanceof Multi) {
                newByteArrayList.addAll(((Multi) byteArray).byteArrays);
            }
        }
        return new Multi(newByteArrayList);
    }

    private static class Single extends ByteArray {

        private final byte[] bytes;
        private final int start;
        private final int length;

        public Single(byte[] bytes, int start, int length) {
            this.bytes = bytes;
            this.start = start;
            this.length = length;
        }

        @Override
        public byte get(int i) {
            return bytes[start + i];
        }

        @Override
        public int length() {
            return length;
        }

        @Override
        protected byte[] getBytes() {
            byte[] dest = new byte[length];
            System.arraycopy(bytes, start, dest, 0, length);
            return dest;
        }

        @Override
        protected int getHashCode() {
            int result = 1;
            for (int i = start; i < start + length; i++) {
                result = 31 * result + bytes[i];
            }
            return result;
        }
    }

    private static class Multi extends ByteArray {

        private final List<Single> byteArrays;
        private final int length;

        public Multi(List<Single> byteArrays) {
            this.byteArrays = byteArrays;
            int length = 0;
            for (Single byteArray : byteArrays) {
                length += byteArray.length;
            }
            this.length = length;
        }

        @Override
        public byte get(int i) {
            for (Single byteArray : byteArrays) {
                if (i < byteArray.length) {
                    return byteArray.get(i);
                } else {
                    i -= byteArray.length;
                }
            }
            throw TypeDBException.of(ILLEGAL_STATE);
        }

        @Override
        public int length() {
            return length;
        }

        @Override
        protected byte[] getBytes() {
            byte[] dest = new byte[length];
            int destIndex = 0;
            for (Single byteArray : byteArrays) {
                System.arraycopy(byteArray.bytes, byteArray.start, dest, destIndex, byteArray.length);
                destIndex += byteArray.length;
            }
            return dest;
        }

        @Override
        protected int getHashCode() {
            int result = 1;
            for (Single byteArray : byteArrays) {
                for (int i = byteArray.start; i < byteArray.start + byteArray.length; i++) {
                    result = 31 * result + byteArray.bytes[i];
                }
            }
            return result;
        }
    }
}
