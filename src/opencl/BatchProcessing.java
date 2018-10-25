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
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import mnist.Memory;

import java.io.IOException;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import static opencl.Utils.fillBuffer;

/**
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 */
public class BatchProcessing {

    public static void computer(Memory memory, Integer id, int label) throws IOException {
//        CLContext context = CLContext.create();
//        try {
//            CLDevice device = context.getMaxFlopsDevice();
//
//            CLCommandQueue queue = device.createCommandQueue();
//
//            CLBuffer<ShortBuffer> ids = fillBuffer(context, memory.ids.matrix);
//            CLBuffer<ShortBuffer> cycleMask = fillBuffer(context, memory.cycleMask);
//
//            queue
//                .putWriteBuffer(ids, false)
//                .putWriteBuffer(cycleMask, false);
//
//
//            Transformations transformations = new Transformations(context, device, queue, memory);
//            Classes classes = new Classes(context, device, queue, memory);
//
//            transformations.execute(context, device, queue, ids, cycleMask, memory, id);
//            classes.execute(context, device, queue, ids, cycleMask, memory, id, label, transformations);
//
//            LongBuffer buf = transformations.trans.getBuffer();
//
//            buf.rewind();
//            long[] res = new long[memory.ids.w*memory.ids.h*memory.Context_set_V.size() / 64];
//            buf.get(res);
//
//            return res;
//
//        } finally {
//            context.release();
//        }
    }
}
