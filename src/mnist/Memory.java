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
package mnist;

import opencl.Elements;
import org.mapdb.Atomic;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.HTreeMap;
import serializers.PackedIntArray;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.mapdb.Serializer.*;

/**
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 */
public class Memory {

    public static double minLevel = 0.8;//0.85;

    int w, h;

    public IDS ids;

    short[][] mask;

    List<int[][]> vars;

    //original ids vectors
    public Map<Integer, long[]> memories;

    //extracted elements
    public Map<Integer, byte[]> elements;

    //original ids vectors
    public Map<Integer, long[]> classes;
    public Map<Integer, Integer> classToNumber;
    public BTreeMap<int[], long[]> classesDiffs;

    public Atomic.Integer nextClass;
    public NavigableSet<Integer> freeClass;

    public BTreeMap<int[], long[]> classToImages;

    public BTreeMap<int[], Float> classToImageSimilarity;

    public BTreeMap<Integer, float[]> classParameters;

    public ConcurrentNavigableMap<Integer, float[]> errors;

    Map<Integer, double[]> correlation;
    HTreeMap<Integer, int[]> cIds;

    Memory() {

        mask = Utils.cycle(4);

        debug(mask);

        w = mask.length;
        h = mask[0].length;
    }

    DecimalFormat df2 = new DecimalFormat("#.0000000");

    public double N_X = 15;
    public double N_Y = 15;
    public double N_O;

    double X_step;
    double Y_step;
    double A_step;

    double X_border;
    double Y_border;
    double A_border;

    public ConcurrentMap<Integer, double[]> Context_ids;
    public ConcurrentMap<double[], Integer> Context_points;

    Map<int[], int[]> transformations;


    public void memory(DB db, M1 images, int X_pic, int Y_pic) throws IOException {

        X_step = X_pic / (28.0 * 2);
        Y_step = Y_pic / (28.0 * 2);
        A_step = Math.PI / 45.0;

        X_border = X_step * 10.0;
        Y_border = Y_step * 10.0;
        A_border = Math.PI / 2.0; //Math.PI * 3.0 / 4.0;

        vars = new ArrayList<>();

        N_O = images.maxAlpha;
        //N_Cont_Alfa = 16; //N_O;

        for (int alpha = 0; alpha < images.maxAlpha; alpha++) {
            vars.add(process(images, alpha, false));
        }

//        for (int alpha = 0; alpha < images.maxAlpha; alpha++) {
//            vars.add(process(images, alpha, true));
//        }

        System.out.println("init step 1");

        List<Content> C_set = new ArrayList<>((int)(N_X * N_Y * N_O));

        int check = 0;
        for (int ix = 0; ix < N_X; ix++) {
            for (int iy = 0; iy < N_Y; iy++) {
                for (int io = 0; io < N_O; io++) {
                    if ((N_Y * N_O * ix) + (N_O * iy) + io != check) {
                        System.out.println("ERROR1!");
                    }

                    if (ID(ix, iy, io) != check) {
                        System.out.println("ERROR2!");
                    }

                    C_set.add(new Content(ix, iy, io));

                    check++;
                }
            }
        }

        System.out.println("C_set: "+C_set.size());

        System.out.println("init step 2");

        classes = db.treeMap("classes", INTEGER, LONG_ARRAY).counterEnable().createOrOpen();
        classToNumber = db.treeMap("classToNumber", INTEGER, INTEGER).createOrOpen();
        classesDiffs = db.treeMap("classDiffs", INT_ARRAY, LONG_ARRAY).createOrOpen();

        classToImageSimilarity = db.treeMap("classToImageSimilarity", INT_ARRAY, FLOAT).counterEnable().createOrOpen();
        classParameters = db.treeMap("classParameters", INTEGER, FLOAT_ARRAY).counterEnable().createOrOpen();

        classToImages = db.treeMap("classToImages_longs", INT_ARRAY, LONG_ARRAY).createOrOpen();
        errors = db.treeMap("errors", INTEGER, FLOAT_ARRAY).createOrOpen();

        nextClass = db.atomicInteger("nextClass").createOrOpen();
        freeClass = db.treeSet("freeClass", INTEGER).createOrOpen();

        classes.clear();
        classesDiffs.clear();
        classToImageSimilarity.clear();
        classParameters.clear();
        classToImages.clear();
        errors.clear();
        nextClass.set(0);
        freeClass.clear();

        System.out.println("size: "+classToImages.size());

        elements = db.hashMap("elements", INTEGER, BYTE_ARRAY).createOrOpen();

        correlation = db.hashMap("correlation", INTEGER, DOUBLE_ARRAY).createOrOpen();
        cIds = db.hashMap("cIds", INTEGER, INT_ARRAY).createOrOpen();

        memories = db.hashMap("memories", INTEGER, LONG_ARRAY)
            .counterEnable()
            .createOrOpen();

        if (db.exists("context_transformations_ids")) {
            Context_ids = db.hashMap("context_transformations_ids", INTEGER, DOUBLE_ARRAY).counterEnable().open();
            Context_points = db.hashMap("context_transformations_points", DOUBLE_ARRAY, INTEGER).open();
            transformations = db.treeMap("transformations", INT_ARRAY, INT_ARRAY).open();

            db.commit();

            if (true) return;

            Context_ids.clear();
            transformations.clear();

            db.commit();

            Files.deleteIfExists(Paths.get("data/cache_transformations"));
        } else {

            Context_ids = db.hashMap("context_transformations_ids", INTEGER, DOUBLE_ARRAY)
                .counterEnable()
                .create();

            Context_points = db.hashMap("context_transformations_points", DOUBLE_ARRAY, INTEGER)
                .counterEnable()
                .create();
        }

//        kotlin.collections.Iterator<kotlin.Pair<int[], int[]>> data
        DB.TreeMapSink<int[], int[]> data = db.treeMap("transformations", INT_ARRAY, INT_ARRAY).counterEnable().createFromSink();

        int count = 0;

        Context_ids.put(count, new double[] {0, 0, 0});
        Context_points.put(new double[] {0, 0, 0}, count);

        for (int id = 0; id < C_set.size(); id++) {
            Content c = C_set.get(id);

            data.put(
                new int[]{count, id},
                new int[]{c.x, c.y, c.o}
            );
        }
        count++;

//        for (int iX = -(int)N_Cont_X; iX <= N_Cont_X; iX++) {
//            for (int iY = -(int)N_Cont_Y; iY <= N_Cont_Y; iY++) {

//                System.out.println(iX + ":" + iY);

        for (double X_t = - X_border; X_t <= X_border; X_t += X_step) {
            for (double Y_t = - Y_border; Y_t <= Y_border; Y_t += Y_step) {

                System.out.println(X_t + ":" + Y_t);

//                double a90 = Math.PI / 3.0;

                for (double a_t_rad = -A_border; a_t_rad <= A_border; a_t_rad += A_step) {
//                    for (double HT_t = 0.9; HT_t <= 1.1; HT_t += 0.1) {
//                        for (double WT_t = 0.9; WT_t <= 1.1; WT_t += 0.1) {

                    double[] tuple = {X_t, Y_t, a_t_rad};
                    Context_points.put(tuple, count);

                    Context_ids.put(count, tuple);
//                            Context_set_V.put(count, new double[] {X_t, Y_t, a_t_rad, HT_t, WT_t});
//                            Context_set_V.put(count, new double[] {iX, iY, iOT, iHT, iWT, X_t, Y_t, a_t_rad, HT_t, WT_t});

                            for (int id = 0; id < C_set.size(); id++) {
                                Content c = C_set.get(id);

                                double xc = (c.x + 0.5) * X_pic / N_X;
                                double yc = (c.y + 0.5) * Y_pic / N_Y;
                                double a_rad = (c.o / N_O) * 2 * Math.PI;

                                double dx = X_pic / 2.0;
                                double dy = Y_pic / 2.0;

                                //вращаем относительно центра
                                xc = xc - dx;
                                yc = yc - dy;

                                double x = Math.cos(a_t_rad) * xc - Math.sin(a_t_rad) * yc;
                                double y = Math.sin(a_t_rad) * xc + Math.cos(a_t_rad) * yc;

                                xc = x + dx;
                                yc = y + dy;

                                a_rad -= a_t_rad;

                                if (a_rad < 0) a_rad += 2 * Math.PI;
                                if (a_rad >= 2 * Math.PI) a_rad -= 2 * Math.PI;

                                //сдвигаем
                                xc += X_t;
                                yc += Y_t;

                                //сжимаем/растягиваем
//                                xc = xc * HT_t;
//                                yc = yc * WT_t;

                                if (xc >= 0 && xc < X_pic && yc >= 0 && yc < Y_pic) {

                                    int alpha = (int) Math.round((a_rad / (2 * Math.PI)) * N_O);
                                    if (alpha == N_O) alpha = 0;

                                    data.put(
                                        new int[] {count, id},
                                        new int[] {
                                            (int) Math.round(N_X * xc / X_pic - 0.5),
                                            (int) Math.round(N_Y * yc / Y_pic - 0.5),
                                            alpha
                                        }
                                    );
                                }
                            }
                            count++;
//                        }
//                    }
                }
            }
        }

        transformations = data.create();

        db.commit();

        System.out.println("initialization done");
    }

    public List<Integer> around_transformation_point(int tId, int r) {
        double[] tuple = Context_ids.get(tId);

        double dX = X_step * r;
        double dY = Y_step * r;
        double dA = A_step * r;

        List<Integer> set = new ArrayList<>();

        for (double tX = tuple[0] - dX; tX <= tuple[0] + dX; tX += X_step) {
            if (tX < -X_border || tX > X_border) continue;

            for (double tY = tuple[1] - dY; tY <= tuple[1] + dY; tY += Y_step) {
                if (tY < -Y_border || tY > Y_border) continue;

                for (double tA = tuple[2] - dA; tA <= tuple[2] + dA; tA += A_step) {
                    if (tA < -A_border || tA > A_border) continue;

                    Integer id = Context_points.get(new double[]{tX, tY, tA});
                    if (id != null) {
                        set.add(id);
                    }
                }
            }
        }

        return set;
    }

    private int ID(int x, int y, int o) {
        return (int) ((N_Y * N_O * x) + (N_O * y) + o);
    }

    private int ID_from_C(long x, long y, long o) {
        return (int) ((o * N_Y + y) * N_X + x);
    }

    public int[][] process(M1 vars, int alpha, boolean line) {

        int[][] vector = new int[w][h];

        int dx = (int) ((vars.w) / 2.0) - w + 1;
        int dy = (int) ((vars.h) / 2.0) - h + 1;

        int hw = (int)(w / 2.0);
        int hh = (int)(h / 2.0);

        for (int mx = 0; mx < w; mx++) {
            for (int my = 0; my < h; my++) {

                if (mask[mx][my] > 0) {
                    int px = dx + hw + mx;
                    int py = dy + hh + my;

//                    if (line) {
//                        vector[mx][my] = vars.line(alpha, px, py);
//                    } else {
                        vector[mx][my] = vars.border(alpha, px, py);
//                    }
                }
            }
        }

        return vector;
    }

    public void process(DigitImage image, IDS ids, BitSet vector) {

        double kx = image.w / (double) ids.w;
        double ky = image.h / (double) ids.h;

        //ids.init(wp, hp);
        //ids.init();

        List<Integer>[][] measure = new ArrayList[image.w][image.h];

        for (int x = 0; x < image.w; x++) {
            for (int y = 0; y < image.h; y++) {

                int[][] varVector = new int[w][h];

                for (int mx = 0; mx < w; mx++) {
                    for (int my = 0; my < h; my++) {

                        int cx = x + mx;
                        int cy = y + my;

                        if (cx >= 0 && cx < image.w && cy >= 0 && cy < image.h) {
                            varVector[mx][my] = image.pixel(cx, cy) * mask[mx][my];
                        }
                    }
                }

                double[] alphas = match(varVector);

                double max = minLevel;

                for (int i = 0; i < alphas.length; i++) {
                    if (alphas[i] > max) {
                        max = alphas[i];
                    }
                }

                for (int i = 0; i < alphas.length; i++) {
//                    System.out.println(x + ":" + y + ":" + i + " = " + alphas[i]);
                    if (alphas[i] >= max) {
//                        System.out.println(x + ":" + y + ":" + i + " = " + alphas[i]);
                        if (measure[x][y] == null) {
                            measure[x][y] = new ArrayList<>();
                        }
                        measure[x][y].add(i);
                    }
                }
            }

        }

        for (int x = 0; x < ids.w; x++) {
            for (int y = 0; y < ids.h; y++) {

                int dx = (int) (kx * x);
                int dy = (int) (ky * y);

                List<Integer> list = measure[dx][dy];
                if (list != null) {
                    for (Integer a : list) {
                        ids.set(vector, x, y, a);
                    }
                }
            }
        }
    }

    public int[][] elements(int id) {
        int[][] els = new int[(int) N_X][(int) N_Y];

        byte[] bs = elements.get(id);
        for (int y = 0; y < N_Y; y++) {
            for (int x = 0; x < N_X; x++) {
                els[x][y] = bs[(int) (N_X*y + x)];
            }
        }
        return els;
    }

    public int[][] elements(BitSet vector) {
        return Elements.elements(ids, vector, N_X, N_Y, N_O);
    }

    public BitSet ids(int[][] elements) {

        BitSet vector = new BitSet();

        int lD = ids.lR * 2 + 1;
        double lSx = (ids.w - ids.lR) / N_X;
        double lSy = (ids.h - ids.lR) / N_X;

        for (int iX = 0; iX < N_X; iX++) {
            for (int iY = 0; iY < N_Y; iY++) {

                int iO = elements[iX][iY];
                if (iO < 0) continue;

                for (int rX = 0; rX < lD; rX++) {
                    for (int rY = 0; rY < lD; rY++) {
                        if (ids.cycleMask[rX][rY] == 0) continue;

                        int x = (int) ((iX * lSx) + rX);
                        int y = (int) ((iY * lSy) + rY);

                        if (x >= 0 && x < ids.w && y >= 0 && y < ids.h) {
                            ids.set(vector, x, y, iO);
                        }
                    }
                }
            }
        }

        return vector;
    }

    public int[][] contextTransformation(int iT, int[][] recognition) {

        int[][] tMatrix = new int[(int)N_X][(int)N_Y];

        //zero
        for (int iX = 0; iX < N_X; iX++) {
            for (int iY = 0; iY < N_Y; iY++) {
                tMatrix[iX][iY] = -1;
            }
        }

        //trans
        for (int iX = 0; iX < N_X; iX++) {
            for (int iY = 0; iY < N_Y; iY++) {

                int iO = recognition[iX][iY];
                if (iO < 0) continue;

                int[] coordinate = trans(iT, iX, iY, iO);

                if (coordinate != null && coordinate[0] >=0) {
                    tMatrix[coordinate[0]][coordinate[1]] = coordinate[2];
                }
            }
        }

        return tMatrix;
    }

    public BitSet contextTransformationIds(int iT, int[][] recognition) {

        BitSet vector = new BitSet();

        int lD = ids.lR * 2 + 1;
        double lSx = (ids.w - ids.lR) / N_X;
        double lSy = (ids.h - ids.lR) / N_X;

        //trans
        for (int iX = 0; iX < N_X; iX++) {
            for (int iY = 0; iY < N_Y; iY++) {

                int iO = recognition[iX][iY];
                if (iO < 0) continue;

                int[] coordinate = trans(iT, iX, iY, iO);

                if (coordinate != null && coordinate[0] >=0) {

                    if (coordinate[2] < 0) continue;

                    for (int rX = 0; rX < lD; rX++) {
                        for (int rY = 0; rY < lD; rY++) {
                            if (ids.cycleMask[rX][rY] == 0) continue;

                            int x = (int) ((coordinate[0] * lSx) + rX);
                            int y = (int) ((coordinate[1] * lSy) + rY);

                            if (x >= 0 && x < ids.w && y >= 0 && y < ids.h) {
                                ids.set(vector, x, y, coordinate[2]);
                            }
                        }
                    }
                }
            }
        }

        return vector;
    }

    public void memorization(Stream<DigitImage> images) {
        images.parallel().forEach(image -> {

            BitSet v = ids.init();

            process(image, ids, v);

            memories.put(image.id, v.toLongArray());

            System.out.println(image.id);

//            BitSet v2 = ids.init();
//
//            process(image, ids, v2);
//
//            if (!v2.equals(v)) {
//                System.out.println("error 1");
//            }
//
//            if (!v2.equals(memories(image.id))) {
//                System.out.println("error 2");
//            }
        });
    }

    public void memorizationCheck(Stream<DigitImage> images) {
        images.forEach(image -> check(image));
    }

    public BitSet check(DigitImage image) {
        BitSet v = ids.init();

        process(image, ids, v);

        System.out.println(image.id);

        BitSet vector = memories(image.id);

        if (!v.equals(vector)) {
            System.out.println("error1");
        }

        System.out.println(v.cardinality());
        System.out.println(vector.cardinality());

        if (!v.equals(vector)) {
            System.out.println("error2");
        }

        return vector;
    }

    public void maxTrans() {

        TreeSet<Integer> nums = new TreeSet<>();
        nums.addAll(memories.keySet());

        int size = nums.last();

        System.out.println(size);

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("debug.txt"))) {

            nums.parallelStream().forEach(id1 -> {
                double[] cor = new double[size];
                int[] cId = new int[size];

                BitSet v1 = memories(id1);

                int sA = v1.cardinality();

                nums.tailSet(id1, false).forEach(id2 -> {

                    BitSet v2 = memories(id2);
                    int[][] elements = elements(v2);

                    Integer maxIndex = -1;
                    double max = -1;

                    for (Integer tId : Context_ids.keySet()) {

                        BitSet v3 = ids(contextTransformation(tId, elements));

                        int sB = v3.cardinality();

                        v3.and(v1);

                        int sum = v3.cardinality();

                        double p = sum / Math.sqrt(sA * sB);


                        if (p > 1) {
                            System.out.println("error !");
                        }

                        if (p > max) {
                            //System.out.println(tId+": "+sum + " / " + sA + " * " + sB + " = "+p);
                            max = p;
                            maxIndex = tId;
                        }

                    }

                    synchronized (writer) {
                        try {
                            writer
                                .append(String.valueOf(id1))
                                .append('\t')
                                .append(String.valueOf(id2))
                                .append('\t')
                                .append(String.valueOf(max))
                                .append('t')
                                .append(String.valueOf(maxIndex))
                                .append('\n');
                        } catch (IOException e) {
                            System.out.println(e.getMessage());
                            //e.printStackTrace();
                        }
                    }

                    System.out.println(id1 + "/" + id2 + " = " + max + " [" + maxIndex + "] ");
                    //iX, iY, iO, iHT, iWT, X_t, Y_t, a_t_rad, HT_t, WT_t
                    //System.out.println(Arrays.toString(Context_set_V.get(maxIndex)));

                    cor[id2] = max;
                    cId[id2] = maxIndex;

                });

                correlation.put(id1, cor);
                cIds.put(id1, cId);

                System.out.println(id1 + " = " + cId + " = " + cor);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String toString(BitSet bs) {
        StringBuilder sb = new StringBuilder();
        for (int p = 0; p < bs.size(); p++) {
            if (bs.get(p)) sb.append(p).append(" | ");
        }
        return sb.toString();
    }

    public static void debug(short[][] matrix) {
        int w = matrix.length;
        int h = matrix[0].length;

        for (int iy = 0; iy < h; iy++) {
            for (int ix = 0; ix < w; ix ++) {
                short n = matrix[ix][iy];
                System.out.print(n == -1 ? ". " : (n == -2 ? "! " : n + " "));
            }
            System.out.println();
        }
    }

    public static void debug(int[][] matrix) {
        int w = matrix.length;
        int h = matrix[0].length;

        for (int iy = 0; iy < h; iy++) {
            for (int ix = 0; ix < w; ix ++) {
                int n = matrix[ix][iy];
                System.out.print(n == -1 ? ".  " : (n == -2 ? "!  " : (n > 9 ? n + " " : n + "  ")));
            }
            System.out.println();
        }
    }

    private int index(int x, int y) {
        return w * y + x;
    }

    //DecimalFormat df2 = new DecimalFormat("#.000");

    public double[] match(int[][] pattern) {

        double[] rs = new double[vars.size()];

        for (int p = 0; p < vars.size(); p++) {
            int[][] mem = vars.get(p);

            rs[p] = convolution(mem, pattern);
        }

        return rs;

//        for (double n : rs) {
//            System.out.print(df2.format(n) + " ");
//        }
//        System.out.println();
    }

    private double convolution(int[][] mem, int[][] pattern) {

        double fr = 0;
        double res = 0;
        double sA = 0;
        double sB = 0;

        for (int x = 0; x < mem.length; x++) {
            for (int y = 0; y < mem[x].length; y++) {
                int a = mem[x][y];
                int b = pattern[x][y];

                res += a * b;

                sA += a * a;
                sB += b * b;
            }
        }

        fr = Math.max(fr, res == 0 ? 0 : res / Math.sqrt(sA * sB));

        return fr;
    }

    public void canvas(BufferedImage canvas, int a, int dx, int dy) {
        int[][] mem = vars.get(a);

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                canvas.setRGB(dx + x, dy + y, (mem[x][y] > 0 ? Color.WHITE : Color.BLACK).getRGB());
            }
        }
    }


    public BufferedImage canvas() {
        int ow = ((w + 1) * vars.get(0).length) + 1;
        BufferedImage canvas = new BufferedImage(ow * 2, (h + 1) * vars.size(), BufferedImage.TYPE_INT_ARGB);

        Graphics g = canvas.getGraphics();
        g.setColor(Color.GRAY);
        g.fillRect(0, 0 , canvas.getWidth(), canvas.getHeight());

        for (int a = 0; a < vars.size(); a++) {

            int[][] mem = vars.get(a);

            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    canvas.setRGB(x, (h + 1) * a + y, (mem[x][y] > 0 ? Color.WHITE : Color.BLACK).getRGB());
                    canvas.setRGB(ow + x, (h + 1) * a + y, (mem[x][y] > 0 ? Color.WHITE : Color.BLACK).getRGB());
                }
            }
        }

        return canvas;
    }

    public BufferedImage restore(int[][] matrix) {
        return restore(matrix, null);
    }

    public BufferedImage restore(int[][] matrix, String label) {

        BufferedImage canvas = new BufferedImage((int)((w + 1) * N_X), (int)((h + 1) * N_Y), BufferedImage.TYPE_INT_ARGB);

        Graphics g = canvas.getGraphics();
        g.setColor(Color.GRAY);
        g.fillRect(0, 0 , canvas.getWidth(), canvas.getHeight());

        for (int iX = 0; iX < N_X; iX++) {
            for (int iY = 0; iY < N_Y; iY++) {
                int a = matrix[iX][iY];

                if (a == -1) {
                    continue;
                }
                if (a == -2) {
                    for (int x = 0; x < w; x++) {
                        for (int y = 0; y < h; y++) {
                            canvas.setRGB((w + 1) * iX + x, (h + 1) * iY + y, Color.DARK_GRAY.getRGB());
                        }
                    }
                    continue;
                }

                int[][] mem = vars.get(a);
                for (int x = 0; x < w; x++) {
                    for (int y = 0; y < h; y++) {
                        canvas.setRGB((w + 1) * iX + x, (h + 1) * iY + y, (mem[x][y] > 0 ? Color.WHITE : Color.BLACK).getRGB());
                    }
                }
            }
        }

        if (label != null) {
            g.setColor(Color.WHITE);
            g.drawString(label, 2, canvas.getHeight()); //10
        }

        return canvas;
    }

    public BitSet memories(int id) {
        return BitSet.valueOf(memories.get(id));
    }

    public int[] trans(int iT, int iX, int iY, int iO) {
        int objID = ID(iX, iY, iO);

        return transformations.get(new int[] {iT, objID});
    }
}
