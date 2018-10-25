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

import com.jogamp.opencl.*;
import mnist.Memory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

import static com.jogamp.opencl.CLMemory.Mem.READ_WRITE;
import static com.jogamp.opencl.CLMemory.Mem.WRITE_ONLY;
import static java.lang.System.nanoTime;
import static java.lang.System.out;
import static opencl.Utils.fillBuffer;
import static opencl.Utils.roundUp;

/**
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 */
public class Transformations {

    public static long[] computer(Memory memory, Integer id) throws IOException {

        int N_X = (int)memory.N_X;
        int N_Y = (int)memory.N_Y;
        int N_O = (int)memory.N_O;

        int totalObjects = N_X * N_Y * N_O;

        int N_T = memory.Context_ids.size();
//        out.println("N_T: "+N_T);

        int idsX = memory.ids.w;
        int idsY = memory.ids.h;

        CLContext context = CLContext.create();
        try {
            CLDevice device = context.getMaxFlopsDevice();

            CLBuffer<LongBuffer> trans = context.createLongBuffer(idsX*idsY*N_T / 64, WRITE_ONLY);

            CLBuffer<ByteBuffer> image = Utils.fillBufferImages(context, memory, id);
            CLBuffer<ByteBuffer> transMatrix = Utils.fillBufferTransformations(context, memory);

            CLBuffer<ShortBuffer> ids = fillBuffer(context, memory.ids.matrix);
            CLBuffer<ShortBuffer> cycleMask = fillBuffer(context, memory.ids.cycleMask);

            CLProgram program = context.createProgram(Elements.class.getResourceAsStream("Transformations.cl")).build(device);
            CLKernel kernel = program.createCLKernel("transformations");

            kernel.putArgs(image, transMatrix, ids, cycleMask, trans)
                .putArg(totalObjects)
                .putArg(N_T)
                .putArg(N_X)
                .putArg(N_Y)
                .putArg(N_O)
                .putArg(memory.ids.w)
                .putArg(memory.ids.h)
                .putArg(memory.ids.lR);

            int wSize = device.getMaxWorkGroupSize();

//            CLEventList events = new CLEventList(1);

            // create command queue on device.
            CLCommandQueue queue = device.createCommandQueue();

            queue.putWriteBuffer(image, false)
                .putWriteBuffer(transMatrix, false)
                .putWriteBuffer(ids, false)
                .putWriteBuffer(cycleMask, false)
                .putWriteBuffer(trans, false)

                .put1DRangeKernel(kernel,
                    0,
                    roundUp(wSize, N_T),
                    wSize
                )

                .putReadBuffer(trans, true)
                .finish();

            LongBuffer buf = trans.getBuffer();

            buf.rewind();
            long[] res = new long[idsX*idsY*N_T / 64];
            buf.get(res);

            return res;

        } finally {
            context.release();
        }
    }
}
