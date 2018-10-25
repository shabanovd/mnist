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

import mnist.Memory;

import java.io.IOException;
import java.util.BitSet;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 */
public class Portrait {

    public static void computer(Memory memory) throws IOException {

        TreeSet<Integer> set = new TreeSet<>(memory.classes.keySet());

        //memory.classToImages.put(new int[]{cId, id, nT}, vector);
        set.forEach(cId -> computer(memory, cId));
    }

    public static void computerParameters(Memory memory, int cId) {
        List<Float> vals = memory.classToImageSimilarity
            .prefixSubMap(new int[]{cId})
            .values()
            .stream()
            .collect(Collectors.toList());

        float avg = 0f;
        for (Float v : vals) {
            avg += v;
        }
        avg /= vals.size();

        double avg2 = Math.pow(avg, 2);

        float variance = 0f;
        for (Float v : vals) {
            variance += Math.pow(v, 2) - avg2;
        }
        variance /= vals.size();

        memory.classParameters.put(cId, new float[]{avg, variance});

    }

    public static void computer(Memory memory, int cId) {
        List<BitSet> images = memory.classToImages
            .prefixSubMap(new int[]{cId})
            .values()
            .stream()
            .map(BitSet::valueOf)
            .collect(Collectors.toList());

        //ignore small groups
        if (images.size() < 10) return;

        int count = 0;
        int size = 0;
        for (BitSet image : images) {
            size = Math.max(size, image.size());
            count++;
        }
        if (count < 10) {
            System.out.println("fail "+cId);
            return;
        }

        double mLevel = 0.6;
        int classSize = memory.classToImages.prefixSubMap(new int[] {cId}).size();

        if (classSize > 100) mLevel = 0.7;
        if (classSize > 200) mLevel = 0.8;
        if (classSize > 300) mLevel = 0.9;
        if (classSize > 500) mLevel = 1.0;

//        System.out.println("change "+cId);

        BitSet portrait = new BitSet(size);

        int ones;
        for (int i = 0; i < size; i++) {
            ones = 0;

            for (BitSet image : images) {
                if (image.get(i)) ones++;
            }

//                if (cId == 1) System.out.println(ones);

            double factor = ones / (double)images.size();

            if (factor > mLevel) {
                portrait.set(i);
            }
        }

//            if (cId == 1) System.out.println("total: "+images.size());

        if (portrait.cardinality() == 0) {
            memory.classToImages.prefixSubMap(new int[]{cId}).clear();
            memory.classes.remove(cId);

            memory.freeClass.add(cId);

            System.out.println("clear "+cId);
        } else {
            memory.classes.put(cId, portrait.toLongArray());
        }
    }
}