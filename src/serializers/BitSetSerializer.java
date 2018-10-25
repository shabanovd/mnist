/*
 * Copyright (C) 2015 The Animo Project
 * http://animotron.org
 *
 * This file is part of Outrunner.
 *
 * Outrunner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package serializers;

import org.jetbrains.annotations.NotNull;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.serializer.GroupSerializerObjectArray;
import org.mapdb.serializer.SerializerUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;

/**
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 */
public class BitSetSerializer extends GroupSerializerObjectArray<BitSet> {

    public final static BitSetSerializer BITSET_SERIALIZER = new BitSetSerializer();

    @Override
    public void serialize(@NotNull DataOutput2 out, @NotNull BitSet value) throws IOException {
        long[] a = value.toLongArray();

        out.packInt(a.length);
        for (long c : a) {
            out.writeLong(c);
        }
    }

    @Override
    public BitSet deserialize(DataInput2 in, int available) throws IOException {
        final int size = in.unpackInt();
        long[] ret = new long[size];
        for (int i = 0; i < size; i++) {
            ret[i] = in.readLong();
        }
        return BitSet.valueOf(ret);
    }


    @Override
    public boolean isTrusted() {
        return true;
    }

    @Override
    public boolean equals(BitSet a1, BitSet a2) {
        return a1 == a2 || (a1 != null && a1.equals(a2));
    }

    @Override
    public int hashCode(BitSet bits, int seed) {
        //return bits.hashCode();

        for (long element : bits.toLongArray()) {
            int elementHash = (int) (element ^ (element >>> 32));
            seed = (-1640531527) * seed + elementHash;
        }
        return seed;
    }

    @Override
    public int compare(BitSet s1, BitSet s2) {
        if (s1 == s2) return 0;

        long[] o1 = s1.toLongArray();
        long[] o2 = s2.toLongArray();

        final int len = Math.min(o1.length, o2.length);
        for (int i = 0; i < len; i++) {
            if (o1[i] == o2[i])
                continue;
            if (o1[i] > o2[i])
                return 1;
            return -1;
        }
        return SerializerUtils.compareInt(o1.length, o2.length);
    }
}
