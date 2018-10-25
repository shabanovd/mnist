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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.BitSet;

/**
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 */
public class M1 {

    BitSet border;
    BitSet line;

    int maxAlpha;

    int r = 20;

    int w = 2 * r + 1;
    int h = 2 * r + 1;

    M1(int maxAlpha, int r, int sR) {

        this.maxAlpha = maxAlpha;

        this.r = r;

        w = 2 * r + 1;
        h = 2 * r + 1;

        border = new BitSet(maxAlpha * w * h);
        line = new BitSet(maxAlpha * w * h);

        for (int alpha = 0; alpha < maxAlpha; alpha++) {

//            System.out.println("alpha = "+alpha);

            double PI2 = 2 * Math.PI;

            double a = (alpha / (double) maxAlpha) * PI2;

            boolean b1 = a >= 0 && a <= PI2 * (1.0 / 8.0);
            boolean b2 = a >= PI2 * (3.0 / 8.0) && a <= PI2 * (5.0 / 8.0);
            boolean b3 = a >= PI2 * (7.0 / 8.0) && a <= PI2;

//            System.out.println(b1 + " " + b2 + " " + b3);

            if (b1 || b2 || b3) {

                double k = Math.sin(a) / Math.cos(a);

//                System.out.println("1 a = " + a + "; k =  " + k + "; "+(k * 2)+" "+(k * 4));

                for (int iy = -r; iy <= r; iy++) {

                    if (b2) {
                        int tX = (int) (k * iy);
                        line.set(index(alpha, tX, iy));
                        for (int ix = -r; ix <= tX; ix++) {
                            border.set(index(alpha, ix, iy));
                        }
                    } else {
                        int fX = (int) (k * iy);
                        line.set(index(alpha, fX, iy));
                        for (int ix = fX; ix <= r; ix++) {
                            border.set(index(alpha, ix, iy));
                        }
                    }
                }

            } else {

                double k = Math.cos(a) / Math.sin(a);

//                System.out.println("2 a = " + a + "; k =  " + k + "; "+(k * 2)+" "+(k * 4));

                for (int ix = -r; ix <= r; ix++) {
                    if (a < Math.PI) {
                        int tY = (int) (k * ix);
                        line.set(index(alpha, ix, tY));
                        for (int iy = -r; iy <= tY; iy++) {
                            border.set(index(alpha, ix, iy));
                        }
                    } else {
                        int fY = (int) (k * ix);
                        line.set(index(alpha, ix, fY));
                        for (int iy = fY; iy <= r; iy++) {
                            border.set(index(alpha, ix, iy));
                        }
                    }
                }
            }
//            debug(alpha);
        }
    }

    private void debug(int alpha) {
        for (int iy = 0; iy < h; iy++) {
            for (int ix = 0; ix < w; ix ++) {
                System.out.print(border.get(pixel(alpha, ix, iy)) ? "1 " : "0 ");
            }
            System.out.println();
        }
    }

    private int index(int alpha, int x, int y) {
        return w * h * alpha
            + h * (x + r)
            + (y + r);
    }

    private int pixel(int alpha, int x, int y) {
        return w * h * alpha
            + h * x
            + y;
    }

    public BufferedImage canvas(int alpha) {
        BufferedImage canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        canvas(canvas, alpha, 0, 0);

        return canvas;
    }

    public BufferedImage canvas(BufferedImage canvas, int alpha, int dx, int dy) {
        if (alpha < -2) return canvas;

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                canvas.setRGB(dx + x, dy + y, color(alpha, x, y).getRGB());
            }
        }

        return canvas;
    }

    public Color color(int alpha, int x, int y) {
        if (alpha == -2) return Color.BLACK;
        if (alpha == -1) return Color.GRAY;

        return border.get(pixel(alpha, x, y)) ? Color.GRAY : Color.BLACK;
    }

    public int get(int alpha, int x, int y) {
        if (alpha == -2) return 0;
        if (alpha == -1) return 128;

        return border.get(pixel(alpha, x, y)) ? 128 : 0;
    }

    public int border(int alpha, int x, int y) {
        if (alpha == -2) return 0;
        if (alpha == -1) return 1;

        return border.get(pixel(alpha, x, y)) ? 1 : 0;
    }

    public int line(int alpha, int x, int y) {
        if (alpha == -2) return 0;
        if (alpha == -1) return 1;

        return line.get(pixel(alpha, x, y)) ? 1 : 0;
    }
}
