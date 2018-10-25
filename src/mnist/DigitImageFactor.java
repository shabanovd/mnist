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
import java.util.Arrays;

/**
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 */
public class DigitImageFactor extends DigitImage {

    double factor;

    int[][] data;

    public DigitImageFactor(double factor, DigitImage original) {
        super(original.id, original.label, null);

        this.factor = factor;

//        double[][] img = zoom(original, factor);
//
//        for (int i = 0; i < 4; i++) {
//            img = blur(img);
//        }

        double[][] img = zoom2(original, factor);

        data = round(img);
    }

    static double[][] gaussianMatrix;
    private double[][] zoom2(DigitImage original, double factor) {
        w = (int) (original.w * factor);
        h = (int) (original.h * factor);

        if (gaussianMatrix == null) {
            int r = (int)factor * 2; //(int)(factor * 3.0 / 4.0); //(int) (factor / 2.0); //
            int d = 2 * r + 1;
            gaussianMatrix = new double[d][d];

            double sigma = r / 3.0;
            double twoSigmaSquare = 2.0 * sigma * sigma;
            double sigmaRoot = twoSigmaSquare * Math.PI;
            double total = 0.0;

            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    float distance = dx * dx + dy * dy;
                    gaussianMatrix[dx + r][dy + r] = Math.exp(-distance / twoSigmaSquare) / sigmaRoot;
                    total += gaussianMatrix[dx + r][dy + r];
                }
            }

            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    //gaussianMatrix[dx + r][dy + r] /= total;
                    gaussianMatrix[dx + r][dy + r] *= 128.0;
                }
            }

//            for (int dx = -r; dx <= r; dx++) {
//                for (int dy = -r; dy <= r; dy++) {
//                    System.out.print(gaussianMatrix[dx + r][dy + r]+" ");
//                }
//                System.out.println();
//            }
        }

        double[][] img = new double[w][h];

        //zeros
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                img[x][y] = 0.0;
            }
        }

        int r = (int)((gaussianMatrix.length - 1) / 2.0);

        for (int x = 0; x < original.w; x++) {
            for (int y = 0; y < original.h; y++) {

                for (int dx = -r; dx <= r; dx++) {
                    for (int dy = -r; dy <= r; dy++) {

                        int nx = (int)(x * factor) + dx;
                        int ny = (int)(y * factor) + dy;
                        if (nx >= 0 && nx < w && ny >= 0 && ny < h) {
                            img[nx][ny] += original.pixel(x, y) * gaussianMatrix[dx+r][dy+r];
                        }
                    }
                }
            }
        }

//        System.out.println("image "+w+" "+h);
//        for (int x = 0; x < w; x++) {
//            for (int y = 0; y < h; y++) {
//                System.out.print(img[x][y]+" ");
//            }
//            System.out.println();
//        }
//        System.out.println("image done");

        return img;
    }

    private double[][] zoom(DigitImage original, double factor) {
        w = (int) (original.w * factor);
        h = (int) (original.h * factor);

        double[][] img = new double[w][h];

        for (int cx = 0; cx < w; cx++) {
            for (int cy = 0; cy < h; cy++) {
                int x = (int)(cx / factor);
                int y = (int)(cy / factor);

                img[cx][cy] = original.pixel(x, y);
            }
        }

        return img;
    }

    float matrix[][] = {
        {0.0625f, 0.125f, 0.0625f},
        {0.1250f, 0.250f, 0.1250f},
        {0.0625f, 0.125f, 0.0625f}
    };

    private double[][] blur(double[][] src) {
        double[][] blur = new double[w][h];
        for (int cx = 1; cx < w - 1; cx++) {
            for (int cy = 1; cy < h - 1; cy++) {

                for (int dx = 0; dx < 3; dx++) {
                    for (int dy = 0; dy < 3; dy++) {
                        blur[cx][cy] += src[cx+dx-1][cy+dy-1] * matrix[dx][dy];
                    }
                }
            }
        }

        return blur;
    }

    private int[][] round(double[][] src) {
        int[][] dst = new int[w][h];
        for (int cx = 1; cx < w - 1; cx++) {
            for (int cy = 1; cy < h - 1; cy++) {
                int v = (int)src[cx][cy];
                dst[cx][cy] = v >= 255 ? 255 : v;
            }
        }

//        System.out.println("image");
//        for (int x = 0; x < w; x++) {
//            for (int y = 0; y < h; y++) {
//                System.out.print(dst[x][y]+" ");
//            }
//            System.out.println();
//        }
//        System.out.println("image done");


        return dst;
    }

    public int pixel(int x, int y) {
        return data[x][y];
    }

    public BufferedImage canvas() {
        BufferedImage canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        System.out.println("w: " + w + "; h: " + h);

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int c = pixel(x, y);

                Color color = new Color(c, c, c);

                canvas.setRGB(x, y, color.getRGB());
            }
        }

        return canvas;
    }
}
