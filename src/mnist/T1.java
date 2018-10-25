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

/**
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 */
public class T1 {
    public void run(DigitImage image) {
//        double minLevel = 0.7;
//
//        mnist.DigitImage image = images.get(iI);
//
//        System.out.println(image.label);
//
//        double[][] f = new double[border.oneDirShift][border.maxAlpha + 2];
//
//        int step = 2;//border.r;
//
//        int w = (int)((image.w - border.w) / (double) step);
//        int h = (int)((image.h - border.h) / (double) step);
//
////        shiftCanvas = new BufferedImage[border.oneDirShift];
////        for (int shift = 0; shift < border.oneDirShift; shift++) {
////            shiftCanvas[shift] = new BufferedImage(w * (border.w + 1), h * (border.h + 1), BufferedImage.TYPE_INT_ARGB);
////        }
//
//        int[][] shifts = new int[w][h];
//        int[][] ids = new int[w][h];
//        double[][] maxs = new double[w][h];
//
//        for (int dx = 0; dx < w; dx++) {
//            for (int dy = 0; dy < h; dy++) {
//
//                for (int alpha = -2; alpha < border.maxAlpha; alpha++) {
//
//                    for (int shift = 0; shift < border.oneDirShift; shift++) {
//
//                        double sum = 0, sA2 = 0, sB2 = 0;
//                        for (int ix = 0; ix < border.w; ix++) {
//                            for (int iy = 0; iy < border.h; iy++) {
//
//                                int a = image.pixel((step * dx) + ix, (step * dy) + iy);
//                                int b = border.get(alpha, shift, ix, iy);
//
//                                sum += a * b;
//
//                                sA2 += a * a;
//                                sB2 += b * b;
//                            }
//                        }
//
//                        if (sA2 * sB2 == 0) {
//                            f[shift][alpha + 2] = 0;
//                        } else {
//                            f[shift][alpha + 2] = sum / Math.sqrt(sA2 * sB2);
//                        }
//                    }
//                }
//
//                System.out.println(dx + " : " + dy);
//
//                for (int alpha = -2; alpha < border.maxAlpha; alpha++) {
//                    for (int shift = 0; shift < border.oneDirShift; shift++) {
//                        System.out.print(df2.format(f[shift][alpha + 2])+" ");
//                    }
//                    System.out.println();
//                }
//
//                Set<Integer> other = new HashSet<>();
//                int maxIndex = -2, maxShift = -1, count = 0;
//                double max = 0;
//                for (int alpha = 0; alpha < border.maxAlpha + 2; alpha++) {
//                    for (int shift = 0; shift < border.oneDirShift; shift++) {
//                        if (f[shift][alpha] > minLevel) {
//                            if (f[shift][alpha] == max) {
//                                count++;
//                                other.add(alpha);
//                            } else if (f[shift][alpha] > max) {
//                                other.clear();
//                                other.add(alpha);
//                                max = f[shift][alpha];
//                                maxIndex = alpha;
//                                maxShift = shift;
//                                count = 1;
//                            }
//                        }
//                    }
//                }
//
//                System.out.println(maxIndex + " " + maxShift + " " +df2.format(max) + " " + count);
//
//                if (count == 1) {
//                    ids[dx][dy] = maxIndex;
//                    shifts[dx][dy] = maxShift;
//                    maxs[dx][dy] = max;
//                } else {
//                    if (count > 1) {
//                        System.out.println(Arrays.toString(other.toArray()));
//
//                        double maxSum = 0;
//                        int sAlpha = -2;
//                        for (int alpha : other) {
//                            double sum = 0;
//                            for (int shift = 0; shift < border.oneDirShift; shift++) {
//                                sum += f[shift][alpha];
//                            }
//
//                            if (sum > max) {
//                                sAlpha = alpha;
//                            }
//                        }
//
//                        ids[dx][dy] = sAlpha;
//                        shifts[dx][dy] = maxShift;
//                        maxs[dx][dy] = max;
//                    } else {
//                        ids[dx][dy] = -2;
//                        shifts[dx][dy] = -1;
//                        maxs[dx][dy] = 0;
//                    }
//                }
//            }
//        }
//
//        System.out.println("=====");
//        for (int dy = 0; dy < h; dy++) {
//            for (int dx = 0; dx < w; dx++) {
//                System.out.print(df2.format(maxs[dx][dy])+":"+ids[dx][dy]+" ");
//            }
//            System.out.println();
//        }
//
////        int factor = 3;
////
////        int rw = (int)(w / (double) factor);
////        int rh = (int)(h / (double) factor);
////
////        int[][] rids = new int[rw][rh];
////        double[][] rmaxs = new double[rw][rh];
////
////        for (int rx = 0; rx < rw; rx++) {
////            for (int ry = 0; ry < rh; ry++) {
////
////                int maxIndex = -2;
////                double max = 0;
////
////                for (int dx = 0; dx < factor; dx++) {
////                    for (int dy = 0; dy < factor; dy++) {
////                        int cx = (rx * factor) + dx;
////                        int cy = (ry * factor) + dy;
////
////                        if (maxs[cx][cy] > max) {
////                            max = maxs[cx][cy];
////                            maxIndex = ids[cx][cy];
////                        }
////                    }
////                }
////
////                if (max > minLevel) {
////                    rids[rx][ry] = maxIndex;
////                    rmaxs[rx][ry] = max;
////                } else {
////                    rids[rx][ry] = -2;
////                    rmaxs[rx][ry] = 0;
////                }
////            }
////        }
////
////        System.out.println("------");
////        for (int ry = 0; ry < rh; ry++) {
////            for (int rx = 0; rx < rw; rx++) {
////                System.out.print(df2.format(rmaxs[rx][ry])+" ");
////            }
////            System.out.println();
////        }
//
//        idsCanvas = new BufferedImage(w * (border.w + 1), h * (border.h + 1), BufferedImage.TYPE_INT_ARGB);
//
//        for (int dx = 0; dx < w; dx++) {
//            for (int dy = 0; dy < h; dy++) {
//                border.canvas(idsCanvas, ids[dx][dy], shifts[dx][dy], dx * (border.w + 1), dy * (border.h + 1));
//            }
//        }
//
//        repaint();

    }
}
