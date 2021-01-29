package grakn.core.common.bytes;

import grakn.core.common.exception.ErrorMessage;
import grakn.core.common.exception.GraknException;

import java.util.Arrays;
import java.util.Optional;

// TODO: Remove bytes() -> byte[] from Encoding?
// TODO: Encoding class pre-construct byteArray and byte[]
// TODO: Add slice instance method
// TODO: Clean up Bytes class
public abstract class ByteArray {

    private byte[] bytesCache = null;
    private int hashCodeCache = 0;

    public abstract byte get(int i);

    public abstract int length();

    public abstract void copyBytes(int srcIndex, byte[] dest, int destIndex, int copyLength);

    public boolean isEmpty() {
        return length() == 0;
    }

    public byte[] bytes() {
        if (bytesCache != null) return bytesCache;
        byte[] dest = new byte[length()];
        copyBytes(0, dest, 0, length());
        bytesCache = dest;
        return dest;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ByteArray)) return false;
        ByteArray other = (ByteArray) o;
        return Arrays.equals(bytes(), other.bytes());
    }

    @Override
    public final int hashCode() {
        if (hashCodeCache != 0) return hashCodeCache;
        hashCodeCache = Arrays.hashCode(bytes());
        return hashCodeCache;
    }

    public static Raw raw(byte aByte) {
        return new Raw(new byte[] {aByte});
    }

    public static Raw raw(byte[] bytes) {
        return new Raw(bytes);
    }

    public static Slice slice(ByteArray byteArray, int start, int length) {
        return new Slice(byteArray, start, length);
    }

    public static Join join(ByteArray... byteArrayList) {
        return new Join(byteArrayList);
    }

    public static class Raw extends ByteArray {

        private final byte[] bytes;

        private Raw(byte[] bytes) {
            this.bytes = bytes;
        }

        @Override
        public byte get(int i) {
            return bytes[i];
        }

        @Override
        public int length() {
            return bytes.length;
        }

        @Override
        public void copyBytes(int srcIndex, byte[] dest, int destIndex, int length) {
            assert srcIndex + length <= bytes.length;
            System.arraycopy(bytes, srcIndex, dest, destIndex, length);
        }
    }

    public static class Slice extends ByteArray {

        private final ByteArray byteArray;
        private final int start;
        private final int length;

        private Slice(ByteArray byteArray, int start, int length) {
            assert start >= 0 && length >= 0;
            this.byteArray = byteArray;
            this.start = start;
            this.length = length;
        }

        @Override
        public byte get(int i) {
            assert i >= 0 && i < length;
            return byteArray.get(start + i);
        }

        @Override
        public int length() {
            return length;
        }

        @Override
        public void copyBytes(int srcIndex, byte[] dest, int destIndex, int copyLength) {
            assert srcIndex >= 0 && copyLength >= 0 && srcIndex + copyLength <= length();
            byteArray.copyBytes(start + srcIndex, dest, destIndex, copyLength);
        }
    }

    public static class Join extends ByteArray {

        private final ByteArray[] byteArrayList;
        private final int length;

        private Join(ByteArray[] byteArrayList) {
            this.byteArrayList = byteArrayList;
            int length = 0;
            for (ByteArray byteArray : byteArrayList) {
                length += byteArray.length();
            }
            this.length = length;
        }

        @Override
        public byte get(int i) {
            assert i >= 0 && i < length;
            for (ByteArray byteArray : byteArrayList) {
                if (i < byteArray.length()) {
                    return byteArray.get(i);
                } else {
                    i -= byteArray.length();
                }
            }
            throw GraknException.of(ErrorMessage.Internal.ILLEGAL_STATE);
        }

        @Override
        public int length() {
            return length;
        }

        @Override
        public void copyBytes(int srcIndex, byte[] dest, int destIndex, int copyLength) {
            assert srcIndex >= 0 && copyLength >= 0 && srcIndex + copyLength <= length();
            for (int i = 0; i < byteArrayList.length && copyLength > 0; i++) {
                ByteArray byteArray = byteArrayList[i];
                if (srcIndex < byteArray.length()) {
                    int arrayCopyLength = Math.min(copyLength, byteArray.length() - srcIndex);
                    byteArray.copyBytes(srcIndex, dest, destIndex, arrayCopyLength);
                    srcIndex = 0;
                    destIndex += arrayCopyLength;
                    copyLength -= arrayCopyLength;
                } else {
                    srcIndex -= byteArray.length();
                }
            }
        }
    }
}
