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
package opencl;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLContext;
import mnist.Memory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.file.*;
import java.util.BitSet;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import static com.jogamp.opencl.CLMemory.Mem.READ_ONLY;

/**
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 */
public class Utils {

    static int roundUp(int groupSize, int globalSize) {
        int r = globalSize % groupSize;
        if (r == 0) {
            return globalSize;
        } else {
            return globalSize + groupSize - r;
        }
    }

    public static CLBuffer<ShortBuffer> fillBuffer(CLContext context, short[][] matrix) {
        int w = matrix.length;
        int h = matrix[0].length;

        CLBuffer<ShortBuffer> result = context.createShortBuffer(w * h, READ_ONLY);
        ShortBuffer buffer = result.getBuffer();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                buffer.put(matrix[x][y]);
            }
        }
        buffer.rewind();

        return result;
    }

    public static CLBuffer<LongBuffer> fillBufferClasses(CLContext context, Memory memory, Collection<Integer> set) {
        int step = memory.ids.w * memory.ids.h / 64;

        CLBuffer<LongBuffer> result = context.createLongBuffer(set.size() * step, READ_ONLY);
        LongBuffer buffer = result.getBuffer();

        set.forEach(k -> {
            long[] bs = memory.classes.get(k);

            buffer.put(bs);

//            for (int i = bs.length; i < step; i++) {
//                buffer.put((byte) 0);
//            }
        });
        buffer.rewind();

        return result;
    }

    public static CLBuffer<ByteBuffer> fillBufferActivity(CLContext context, Memory memory, TreeSet<Integer> set) {
        int step = memory.ids.w * memory.ids.h / 8;

        CLBuffer<ByteBuffer> result = context.createByteBuffer(set.size() * step, READ_ONLY);
        ByteBuffer buffer = result.getBuffer();

        set.forEach(k -> {
            long[] longs = memory.memories.get(k);

            byte[] bs = BitSet.valueOf(longs).toByteArray();

            buffer.put(bs);

            for (int i = bs.length; i < step; i++) {
                buffer.put((byte)0);
            }
        });
        buffer.rewind();

        return result;
    }

    public static CLBuffer<ByteBuffer> fillBufferImages(CLContext context, Memory memory, Integer id) {

        CLBuffer<ByteBuffer> result = context.createByteBuffer((int)(memory.N_X * memory.N_Y), READ_ONLY);
        ByteBuffer buffer = result.getBuffer();

//        TreeSet<Integer> set = new TreeSet<>(memory.elements.keySet());
//
//        set.forEach(id -> {
            byte[] elements = memory.elements.get(id);
            buffer.put(elements);
//        });
        buffer.rewind();

        return result;
    }

    static byte[] cache = null;

    public static CLBuffer<ByteBuffer> fillBufferTransformations(CLContext context, Memory memory) {
        int N_X = (int)memory.N_X;
        int N_Y = (int)memory.N_Y;
        int N_O = (int)memory.N_O;

        int N_T = memory.Context_ids.size();

        //System.out.println("N_T: "+N_T);

        int num = N_X * N_Y * N_O * N_T * 3;

        CLBuffer<ByteBuffer> result = context.createByteBuffer(num, READ_ONLY);
        ByteBuffer buffer = result.getBuffer();

        Path path = Paths.get("data/cache_transformations");
        if (cache == null) {
            if (Files.isReadable(path)) {
                try {
                    cache = Files.readAllBytes(path);
                } catch (IOException e) {
                }
            }

        }

        if (cache != null) {
            buffer.put(cache);
            buffer.rewind();
            return result;
        }

//        int count = 0;
        //trans
        for (int iT = 0; iT < N_T; iT++) {
//            boolean flag = true;
            for (int iO = 0; iO < N_O; iO++) {
                for (int iY = 0; iY < N_Y; iY++) {
                    for (int iX = 0; iX < N_X; iX++) {

//                        int objID = N_Y*N_X*iO + N_X*iY + iX;
//                        int index = (totalObjects * iT + objID) * 3;
//
//                        if (count != index) {
//                            System.out.println("ERROR");
//                        }
//                        count++;

                        int[] coordinate = memory.trans(iT, iX, iY, iO);

                        if (coordinate == null) {
//                            flag = false;

                            buffer.put((byte) -1);
                            buffer.put((byte) -1);
                            buffer.put((byte) -1);
                        } else {
//                            if (flag && (iX != coordinate[0] || iY != coordinate[1] || iO != coordinate[2])) {
//                                flag = false;
//                            }
//                            if (iT == 0) {
//                                System.out.println(iX+":"+iY+":"+iO+" => "+coordinate[0]+":"+coordinate[1]+":"+coordinate[2]+" ["+iT+"]");
//                            }
                            buffer.put((byte) coordinate[0]);
                            buffer.put((byte) coordinate[1]);
                            buffer.put((byte) coordinate[2]);
                        }
                    }
                }
            }
            if (iT % 1000 == 0) {
                System.out.println("tID: "+iT+" of "+N_T);
            }
        }
        buffer.rewind();

        cache = new byte[num];
        buffer.get(cache);

        try {
            Files.write(path, cache, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        buffer.rewind();

        System.out.println("trans matrix ready");

        return result;
    }
}
