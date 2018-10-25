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

import org.mapdb.DB;
import org.mapdb.Serializer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.BitSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 */
public class IDS {

    static double factor = 2;
    public int lR = (int) (12 / factor);
    public short[][] cycleMask = Utils.cycle(lR);

    public int w = (int) ((28 / factor) * 8);
    public int h = (int) ((28 / factor) * 8);
    int n;

    public short[][] matrix = new short[w][h];

    IDS(DB db, M1 vars) {
        n = vars.maxAlpha;

        if (db.exists("ids")) {
            Set<int[]> set = db.treeSet("ids", Serializer.INT_ARRAY).open();

            set.forEach(e -> matrix[e[0]][e[1]] = (short) e[2]);
        } else {
            Set<int[]> set = db.treeSet("ids", Serializer.INT_ARRAY).create();

            Random rnd = new Random();
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    short v = matrix[x][y] = (short) rnd.nextInt(n);

                    set.add(new int[] {x, y, v});
                }
            }

            db.commit();

            System.out.println("mnist.IDS: generated");
        }
    }

    public BitSet init() {
        return new BitSet(); //w*h
    }

    public boolean set(BitSet vector, int x, int y, int a) {
        if (mask(x, y, a) == 1) {
            vector.set(index(x, y));
            return true;
        }
        return false;
    }

    private int next(int a) {
        int next = a + 1;
        if (next >= n) {
            return 0;
        }
        return next;
    }

    private int prev(int a) {
        int prev = a - 1;
        if (prev < 0) {
            return n - 1;
        }
        return prev;
    }

    public int size() {
        return w*h;
    }

    public int mask(int x, int y, int a) {

        int alpha = matrix[x][y];
        if (alpha == a || alpha == next(a) || alpha == prev(a)) return 1;

        return 0;
    }

    public int activity(BitSet vector, int x, int y) {
        return vector.get(index(x, y)) ? 1 : 0;
    }

    private int index(int x, int y) {
        return w * y + x;
    }

    public BufferedImage canvas(BitSet vector, Memory matrix) {
        BufferedImage canvas = new BufferedImage(1 + 3 * w, 1 + (h + 1) * n, BufferedImage.TYPE_INT_ARGB);

        Graphics g = canvas.getGraphics();
        g.setColor(Color.GRAY);
        g.fillRect(0, 0 , canvas.getWidth(), canvas.getHeight());

        for (int a = 0; a < n; a++) {

            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    if (a == 0) {
                        canvas.setRGB(2 * w + x, y, (vector.get(index(x, y)) ? Color.WHITE : Color.BLACK).getRGB());
                    }
                    canvas.setRGB(w + x, (h + 1) * a + y, (this.matrix[x][y] == a && vector.get(index(x, y)) ? Color.WHITE : Color.BLACK).getRGB());
                    canvas.setRGB(x, (h + 1) * a + y, (this.matrix[x][y] == a ? Color.WHITE : Color.BLACK).getRGB());
                }
                //canvas.setRGB(x, (w + 1) * a, Color.GRAY.getRGB());
            }

            matrix.canvas(canvas, a, 0, (h + 1) * a);
        }

        return canvas;
    }

    public BufferedImage canvasSmall(BitSet vector) {
        BufferedImage canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        Graphics g = canvas.getGraphics();
        g.setColor(Color.GRAY);
        g.fillRect(0, 0 , canvas.getWidth(), canvas.getHeight());

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                canvas.setRGB(x, y, (vector.get(index(x, y)) ? Color.WHITE : Color.BLACK).getRGB());
            }
        }

        return canvas;
    }
}
