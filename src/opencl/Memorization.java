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
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import static com.jogamp.opencl.CLMemory.Mem.WRITE_ONLY;
import static java.lang.System.out;
import static opencl.Utils.*;

/**
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 */
public class Memorization {

    public static void computer(Memory memory) throws IOException {

        int idsX = memory.ids.w;
        int idsY = memory.ids.h;

//        int N_X = (int)memory.N_X;
//        int N_Y = (int)memory.N_Y;
//        int N_O = (int)memory.N_O;

//        int totalObjects = N_X * N_Y * N_O;

        int count = memory.classes.size();

        int N_T = memory.Context_ids.size();

        //out.println("N_X: "+N_X+"; N_Y: "+N_Y+"; N_O: "+N_O+"; N_T: "+N_T);

        CLContext context = CLContext.create();
//        out.println("created "+context);

        try {
            CLDevice device = context.getMaxFlopsDevice();
//            CLDevice device = context.getMaxFlopsDevice(CLDevice.Type.CPU);
//            out.println("using "+device);

            // create command queue on device.
            CLCommandQueue queue = device.createCommandQueue();

            CLProgram program = context.createProgram(Elements.class.getResourceAsStream("Memorization.cl")).build(device);

//            long time = nanoTime();

            //CLBuffer<LongBuffer> classes = fillBufferClasses(context, memory);

            CLBuffer<ShortBuffer> ids = fillBuffer(context, memory.ids.matrix);
            CLBuffer<ShortBuffer> cycleMask = fillBuffer(context, memory.ids.cycleMask);

            CLBuffer<FloatBuffer> cor = context.createFloatBuffer(N_T * count, WRITE_ONLY);

//            time = nanoTime() - time;
//            out.println("data preparation took: "+(time/1000000)+"ms");

//            out.println("used device memory: "
//                + (trans.getCLSize()+classes.getCLSize()+ids.getCLSize()+cycleMask.getCLSize())/1000000 +"MB");
//
//            out.println("alloc device memory: "
//                + (cor.getCLSize())/1000000 +"MB");

            CLKernel kernel = program.createCLKernel("memorization");

            kernel.putArgs(ids, cycleMask, cor)
                .putArg(N_T)
                .putArg(memory.ids.w)
                .putArg(memory.ids.h)
                .putArg(count);

            //CLEventList events = new CLEventList(1);

            int wSize = device.getMaxWorkGroupSize();

//            long time = nanoTime();
            queue.putWriteBuffer(ids, false)
                .putWriteBuffer(cycleMask, false)
                .putWriteBuffer(cor, false)

                .put2DRangeKernel(kernel,
                    0, 0,
                    roundUp(wSize, N_T), roundUp(1, count),
                    wSize, 1
                )

                .putReadBuffer(cor, true)
                .finish();

//            time = nanoTime() - time;

//            events.forEach(out::println);

//            out.println("results: ");
//            for(int i = 0; i < 10; i++)
//                out.print(cor.getBuffer().get() + ", ");
//            out.println("...; " + cor.getBuffer().remaining() + " more");

//            out.println("Classes computation took: "+(time/1000000)+"ms");

        } finally {
            context.release();
        }
    }
}