package opencl;/*
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

import com.jogamp.opencl.*;
import mnist.IDS;
import mnist.Memory;
import mnist.Utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.*;

import static java.lang.System.*;
import static com.jogamp.opencl.CLMemory.Mem.*;
import static java.lang.Math.*;
import static opencl.Utils.*;

/**
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 */
public class Elements {

    public static void computer(Memory memory, TreeSet<Integer> set) throws IOException {

        int count = set.size();
        if (count == 0) return;

        int N_X = (int)memory.N_X;
        int N_Y = (int)memory.N_Y;
        int N_O = (int)memory.N_O;

        CLContext context = CLContext.create();
//        out.println("created "+context);

        try{
            CLDevice device = context.getMaxFlopsDevice();
//            CLDevice device = context.getMaxFlopsDevice(CLDevice.Type.CPU);
//            out.println("using "+device);

            // create command queue on device.
            CLCommandQueue queue = device.createCommandQueue();

            int one = 8;
            int localWorkSize = min(device.getMaxWorkGroupSize() / (one * one), 8);
            int globalWorkSize = roundUp(localWorkSize, count);

//            out.println("count: "+count+"; localWorkSize: "+localWorkSize+"; globalWorkSize: "+globalWorkSize);

            CLProgram program = context.createProgram(Elements.class.getResourceAsStream("Elements.cl")).build();

            CLBuffer<ShortBuffer> ids = fillBuffer(context, memory.ids.matrix);
            CLBuffer<ShortBuffer> cycleMask = fillBuffer(context, memory.ids.cycleMask);
            CLBuffer<ByteBuffer> activity = fillBufferActivity(context, memory, set);

            CLBuffer<ByteBuffer> recognition = context.createByteBuffer(N_X*N_Y*count, WRITE_ONLY);

//            out.println("used device memory: "
//                + (ids.getCLSize()+cycleMask.getCLSize()+activity.getCLSize()+recognition.getCLSize())/1000000 +"MB");
//
//            out.println("alloc device memory: "
//                + (recognition.getCLSize())/1000000 +"MB");

            CLKernel kernel = program.createCLKernel("elements");

            kernel.putArgs(ids, cycleMask, activity, recognition)
                .putArg(memory.ids.w)
                .putArg(memory.ids.h)
                .putArg(count)
                .putArg(N_X)
                .putArg(N_Y)
                .putArg(N_O)
                .putArg(memory.ids.lR);

            CLEventList events = new CLEventList(1);

            // asynchronous write of data to GPU device,
            // followed by blocking read to get the computed results back.
//            long time = nanoTime();
            queue.putWriteBuffer(ids, false)
                .putWriteBuffer(cycleMask, false)
                .putWriteBuffer(activity, false)
                .putWriteBuffer(recognition, false)
                .put3DRangeKernel(kernel,
                    0, 0, 0,
                    roundUp(one, N_X), roundUp(one, N_Y), globalWorkSize,
                    one, one, localWorkSize,
                    events
                )
                .putReadBuffer(recognition, true);
                //.finish();

            events.forEach(System.out::println);

//            int c = 0;
//
//            ByteBuffer buf = recognition.getBuffer();
//            buf.rewind();
//            set.forEach(n -> {
//
//                BitSet bits = memory.memories(n);
//                int[][] elements = elements(memory.ids, bits, memory.N_X, memory.N_Y, memory.N_O);
//                int errors = 0;
//                StringBuilder sb = new StringBuilder();
//
//                out.println(n);
//                for (int y = 0; y < N_Y; y++) {
//                    for (int x = 0; x < N_X; x++) {
//                        byte b = buf.get();
////                        if (b < 0 || b > 9) {
////                            out.print(b);
////                        } else {
////                            out.print(" " + b);
////                        }
//
//                        if (elements[x][y] != b) {
//                            sb.append(x+":"+y+" = "+elements[x][y] +" vs "+ b).append("\n");
//                            errors++;
////                            out.print(". ");
//                        } else {
////                            out.print("  ");
//                        }
//                    }
////                    out.println();
//                }
//                if (errors > 2) {
//                    out.println(n + " errors: " + errors+" ["+c+"]");
//                    out.println(sb.toString());
//
//                    throw new RuntimeException();
//                }
////                out.println();
////                c++;
//            });

            ByteBuffer buffer = recognition.getBuffer();
            buffer.rewind();
            set.forEach(id -> {
                byte[] els = new byte[N_X * N_Y];
                buffer.get(els);
                memory.elements.put(id, els);
            });

//            out.println("computation took: "+(time/1000000)+"ms");

        }finally{
            context.release();
        }
    }

    public static int[][] elements(IDS ids, BitSet vector, double N_X, double N_Y, double N_Cont_Alfa) {

        int lD = ids.lR * 2 + 1;
        double lSx = (ids.w - ids.lR) / N_X;
        double lSy = (ids.h - ids.lR) / N_X;

        int[][] recognition = new int[(int)N_X][(int)N_Y];

        for (int iX = 0; iX < N_X; iX++) {
            for (int iY = 0; iY < N_Y; iY++) {

                double max = 0.2; //Memory.minLevel;
                int index = -1;

                for (int iO = 0; iO < N_Cont_Alfa; iO++) {

                    long res = 0;
                    long sA = 0;
                    long sB = 0;

                    for (int rX = 0; rX < lD; rX++) {
                        for (int rY = 0; rY < lD; rY++) {
                            if (ids.cycleMask[rX][rY] == 0) continue;

                            int x = (int) ((iX * lSx) + rX);
                            int y = (int) ((iY * lSy) + rY);

                            if (x >= 0 && x < ids.w && y >= 0 && y < ids.h) {
                                int a = ids.activity(vector, x, y);
                                int b = ids.mask(x, y, iO);

                                res += a * b;
                                sA += a * a;
                                sB += b * b;
                            }
                        }
                    }

                    double result = 0.0f;

                    if (res != 0) {
                        result = res / (double)sB;
                    }

                    if (result == max) {
                        index = -2;
                    } else if (result > max) {
                        max = result;
                        index = iO;
                    }

                    //if (iX == 12 && iY == 8) {
                    //    System.out.println("max: "+max+"; index: "+index+"; result: "+result+"; "+res+" "+sA+" "+sB);
                    //}
                }

                recognition[iX][iY] = index;
            }
        }

        return recognition;
    }
}
