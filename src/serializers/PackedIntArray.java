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

import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.serializer.SerializerIntArray;

import java.io.IOException;

/**
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 */
public class PackedIntArray extends SerializerIntArray {

    public final static PackedIntArray POSITIVE_INTS_ARRAY = new PackedIntArray();

    @Override
    public void serialize(DataOutput2 out, int[] value) throws IOException {
        out.packInt(value.length);
        for (int c : value) {
            out.packInt(c);
        }
    }

    @Override
    public int[] deserialize(DataInput2 in, int available) throws IOException {
        final int size = in.unpackInt();
        int[] ret = new int[size];
        for (int i = 0; i < size; i++) {
            ret[i] = in.unpackInt();
        }
        return ret;
    }
}
