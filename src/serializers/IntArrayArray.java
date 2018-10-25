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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 */
public class IntArrayArray extends GroupSerializerObjectArray<List<int[]>> {

    public static final IntArrayArray serializer = new IntArrayArray();

    @Override
    public void serialize(@NotNull DataOutput2 out, @NotNull List<int[]> arrays) throws IOException {
        out.packInt(arrays.size());
        for (int[] array : arrays) {
            out.packInt(array.length);
            for (int c : array) {
                out.writeInt(c);
            }
        }
    }

    @Override
    public List<int[]> deserialize(@NotNull DataInput2 in, int available) throws IOException {
        final int s1 = in.unpackInt();
        List<int[]> list = new ArrayList<>(s1);
        for (int i = 0; i < s1; i++) {

            final int s2 = in.unpackInt();
            int[] ret = new int[s2];
            for (int j = 0; j < s2; j++) {
                ret[j] = in.readInt();
            }
            list.add(ret);
        }
        return list;
    }
}
