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
import mnist.Match;
import mnist.Memory;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.*;

import static com.jogamp.opencl.CLMemory.Mem.WRITE_ONLY;
import static java.lang.System.out;
import static opencl.Utils.*;

/**
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 */
public class SubClasses {

    static int idsX;
    static int idsY;

    public static List<Result> computer(Memory memory, Integer id, int label, long[] _trans_, boolean create) throws IOException {

        double minLevel = 0.5;

        List<Result> res = new ArrayList<>();

        idsX = memory.ids.w;
        idsY = memory.ids.h;

        List<Integer> set = new ArrayList<>(memory.classes.keySet());

        if (set.isEmpty()) {

            long[] image = vector(idsX, idsY, _trans_, 0);

            int cId = create(memory, id, label, image);

            res.add(new Result(new int[]{cId, id, 0}, cId, image, _trans_));
            return res;
        }

        int N_T = memory.Context_ids.size();

        CLContext context = CLContext.create();
//        out.println("created "+context);

        try {
            CLDevice device = context.getMaxFlopsDevice();

            LinkedList<Match> matches = new LinkedList<>();

            double max = 0;

            int delta = 200;

            int steps = set.size() / delta;
            for (int step = 0; step <= steps; step++ ) {

                int offset = step * delta;
                int end = offset + delta;
                List<Integer> subSet = set.subList(offset, set.size() > end ? end : set.size());

                int count = subSet.size();
                if (count == 0) continue;

                CLBuffer<LongBuffer> classes = fillBufferClasses(context, memory, subSet);

                //optimize?
                CLProgram program = context.createProgram(Elements.class.getResourceAsStream("SubClasses.cl")).build(device);
                CLKernel kernel = program.createCLKernel("classes");

                CLBuffer<LongBuffer> trans = context.createLongBuffer(idsX * idsY * N_T / 64, WRITE_ONLY);
                trans.getBuffer().put(_trans_);
                trans.getBuffer().rewind();

                CLBuffer<ShortBuffer> ids = fillBuffer(context, memory.ids.matrix);
                CLBuffer<ShortBuffer> cycleMask = fillBuffer(context, memory.ids.cycleMask);

                CLBuffer<FloatBuffer> cor = context.createFloatBuffer(N_T * count, WRITE_ONLY);

                kernel.putArgs(trans, classes, ids, cycleMask, cor)
                    .putArg(N_T)
                    .putArg(memory.ids.w)
                    .putArg(memory.ids.h)
                    .putArg(count);

//                CLEventList events = new CLEventList(1);

                int wSize = device.getMaxWorkGroupSize();

                // create command queue on device.
                CLCommandQueue queue = device.createCommandQueue();

                queue.putWriteBuffer(trans, false)
                    .putWriteBuffer(classes, false)
                    .putWriteBuffer(ids, false)
                    .putWriteBuffer(cycleMask, false)
                    .putWriteBuffer(cor, false)

                    .put2DRangeKernel(kernel,
                        0, 0,
                        roundUp(wSize, N_T), roundUp(1, count),
                        wSize, 1
                    )

                    .putReadBuffer(cor, true)
                    .finish();

//                events.forEach(out::println);
//
//                out.println("results: ");
//                for(int i = 0; i < (subSet.size() > 10 ? 10 : subSet.size()); i++)
//                    out.print(cor.getBuffer().get() + ", ");
//                out.println("...; " + cor.getBuffer().remaining() + " more");

                FloatBuffer buffer = cor.getBuffer();
                buffer.rewind();

                for (int cId : subSet) {

                    max = 0;
                    for (int t = 0; t < N_T; t++) {
                        float n = buffer.get();

                        if (n > max) {
                            max = n;
                        }

                        if (n < minLevel) {
                            if (!create) {
                                remove(memory, id, t, cId);
                            }
                            continue;
                        }

                        Match match = new Match(cId, t, n);
                        matches.add(match);
                    }

                    //System.out.println(cId + " " +max);
                }
            }

            //out.println("matches: "+matches.size()+" of "+memory.classes.size());

            boolean doCreate = matches.isEmpty();
            int countAdded = 0;

            out.print(":::\t"+id+"\t"+memory.classes.size()+"\t"+matches.size()+"\t"+max+"\t");

            Collections.sort(matches);

            while (!matches.isEmpty()) {
                Match match = matches.pollLast();

                //remove matches around this max
                for (Integer tId : memory.around_transformation_point(match.tId, 3)) {
                    Iterator<Match> it = matches.iterator();
                    while (it.hasNext()) {
                        Match next = it.next();

                        if (next.tId == tId) {
                            it.remove();
                        }
                    }
                }

                double mLevel = minLevel;
                int classSize = memory.classToImages.prefixSubMap(new int[] {match.cId}).size();

                if (classSize > 100) mLevel = 0.6;
                if (classSize > 200) mLevel = 0.7;
                if (classSize > 300) mLevel = 0.8;
                if (classSize > 400) mLevel = 0.9;
                if (classSize > 500) mLevel = 1.0;


                long[] image = vector(idsX, idsY, _trans_, 0);
                if (match.sim > mLevel) {
                    add(memory, id, label, match.tId, image, match.cId, match.sim);
                    countAdded++;
                }
                res.add(new Result(new int[]{match.cId, id, match.tId}, match.sim, image, _trans_));
            }


            if (create && doCreate) {
                long[] image = vector(idsX, idsY, _trans_, 0);
                int cId = create(memory, id, label, image);
                res.add(new Result(new int[]{cId, id, 0}, 0f, image, _trans_));
                countAdded++;
            }

            out.println(res.size()+"\t"+countAdded);

        } finally {
            context.release();
        }
        return res;
    }

    public static class Result {
        public int[] classImage;
        public double sim;

        public long[] image;

        long[] _trans_;

        public Result(int[] classImage, double sim, long[] image, long[] _trans_) {
            this.classImage = classImage;
            this.sim = sim;
            this.image = image;

            this._trans_ = _trans_;
        }

        public Integer cId() {
            return classImage[0];
        }

        public long[] vector(int t) {
            return SubClasses.vector(idsX, idsY, _trans_, t);
        }

        public int tId() {
            return classImage[2];
        }
    }

    private static void makeDiffs(Memory memory) {
        for (Integer cId : memory.classes.keySet()) {
            for (Integer rCID : memory.classes.keySet()) {
                makeDiffs(memory, cId, rCID);
            }
        }
    }

    private static void makeDiffs(Memory memory, Integer cId, Integer rCID) {
        if (Integer.compare(cId, rCID) == 0) return;

        if (memory.classesDiffs.prefixSubMap(new int[] {cId, rCID}).isEmpty()) {
            BitSet fn = BitSet.valueOf(memory.classes.get(rCID));
            BitSet fp = BitSet.valueOf(memory.classes.get(cId));

            //reverse
            fp.flip(0, fp.size());

            //diff
            fn.and(fp);

            if (fn.cardinality() != 0) {
                memory.classesDiffs.put(new int[]{cId, rCID}, fn.toLongArray());
            }
        }
    }

    private static int create(Memory memory, Integer id, int label, long[] vector) {
        int cId;
        if (memory.freeClass.isEmpty()) {
            cId = memory.nextClass.incrementAndGet();
        } else {
            cId = memory.freeClass.pollFirst();
        }

        //out.println("create " + cId);
        memory.classes.put(cId, vector);

        add(memory, id, label, 0, vector, cId, 1f);

        //makeDiffs(memory);

        return cId;
    }

    private static void add(Memory memory, Integer id, int label, int nT, long[] vector, int cId, float sim) {
        //out.println("adding " + cId);

        if (sim >= 0.99) return;

        int[] k = new int[]{cId, id, nT};

        memory.classToImageSimilarity.put(k, sim);
        memory.classToImages.put(k, vector);
        memory.classToNumber.put(cId, label);

        //Portrait.computerParameters(memory, cId);
        Portrait.computer(memory, cId);
    }

    private static void remove(Memory memory, Integer id, int nT, int cId) {
        //out.println("adding " + cId);

        int[] k = new int[]{cId, id, nT};

        memory.classToImageSimilarity.remove(k);
        memory.classToImages.remove(k);

        //Portrait.computerParameters(memory, cId);
    }


    public static byte[] vector(int idsX, int idsY, byte[] trans, int t) {
        int n = idsX * idsY / 8;

        byte[] bs = new byte[n];
        System.arraycopy(trans, n * t, bs, 0, n);

        return bs;
    }

    public static long[] vector(int idsX, int idsY, long[] trans, int t) {
        int n = idsX * idsY / 64;

        long[] longs = new long[n];
        System.arraycopy(trans, n * t, longs, 0, n);

        return longs;
    }

    public static long[] vector(int idsX, int idsY, CLBuffer<LongBuffer> trans, int t) {
        int n = idsX * idsY / 64;

        long[] longs = new long[n];

        LongBuffer buf = trans.getBuffer();
        buf.rewind();

        buf.get(longs, n * t, n);

        return longs;
    }
//
//    CLKernel kernel;
//
//    CLBuffer<LongBuffer> classes;
//    CLBuffer<FloatBuffer> cor;
//
//    TreeSet<Integer> set;
//
//    int idsX, idsY;
//
//    int count, N_T;
//
//    public Classes(CLContext context, CLDevice device, CLCommandQueue queue, Memory memory) throws IOException {
//
//        CLProgram program = context.createProgram(Elements.class.getResourceAsStream("Classes.cl")).build(device);
//
//        kernel = program.createCLKernel("classes");
//
//        idsX = memory.ids.w;
//        idsY = memory.ids.h;
//
//        N_T = memory.Context_set_V.size();
//
//        count = memory.classes.size();
//    }
//
//    public void execute(
//        CLContext context, CLDevice device, CLCommandQueue queue,
//        CLBuffer<ShortBuffer> ids, CLBuffer<ShortBuffer> cycleMask,
//        Memory memory, Integer id, int label, Transformations transformations
//    ) throws IOException {
//
//        CLBuffer<LongBuffer> trans = transformations.trans;
//
//        if (classes == null) {
//            if (count == 0) {
//                create(memory, id, label, vector(idsX, idsY, trans, 0));
//                count = memory.classes.size();
//                return;
//            }
//
//            set = new TreeSet<>(memory.classes.keySet());
//            classes = fillBufferClasses(context, memory, set);
//
//            cor = context.createFloatBuffer(N_T * count, WRITE_ONLY);
//
////            queue.putWriteBuffer(classes, false)
////                .putWriteBuffer(cor, false);
//        }
//
//        kernel.putArgs(trans, classes, ids, cycleMask, cor)
//            .putArg(N_T)
//            .putArg(memory.ids.w)
//            .putArg(memory.ids.h)
//            .putArg(count);
//
//        int wSize = device.getMaxWorkGroupSize();
//
//        queue.putWriteBuffer(trans, false)
//            .putWriteBuffer(classes, false)
//            .putWriteBuffer(ids, false)
//            .putWriteBuffer(cycleMask, false)
//            .putWriteBuffer(cor, false)
//
//            .put2DRangeKernel(kernel,
//                0, 0,
//                roundUp(wSize, N_T), roundUp(1, count),
//                wSize, 1
//            )
//
//            .putReadBuffer(trans, true)
//            .putReadBuffer(cor, true);
//
//
//        float max = -1;
//        int maxT = -1;
//        int maxC = -1;
//
//        FloatBuffer buffer = cor.getBuffer();
//        buffer.rewind();
//        for (int clN : set) {
//            for (int t = 0; t < N_T; t++) {
//                float n = buffer.get();
//                if (n > max) {
//                    int cId = clN + 1;
//
//                    Integer nn = memory.classToNumber.get(cId);
//
//                    if (Integer.compare(nn, label) == 0) {
//                        maxT = t;
//                        maxC = clN;
//                        max = n;
//                    }
//                }
//            }
//        }
//
//        out.println("maxC: "+maxC+"; maxT: "+maxT+"; max: "+max);
//
//        if (maxC == -1) {
//            create(memory, id, label, vector(idsX, idsY, trans, 0));
//
//            count = memory.classes.size();
//
//            classes.release();
//            classes = null;
//
//            cor.release();
//            cor = null;
//        } else {
//            add(memory, id, label, maxT, vector(idsX, idsY, trans, maxT), maxC);
//        }
//    }
}