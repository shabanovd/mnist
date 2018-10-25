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
public class Utils {

    public static short[][] cycle(int lR) {
        int lD = 2 * lR + 1;

        double R2 = (double)(lR * lR);

//        int count = 0;
        short[][] matrix = new short[lD][lD];
        for (int rY = -lR; rY <= lR; rY++) {
            for (int rX = -lR; rX <= lR; rX++) {
                double v = ((rX * rX) / R2) + ((rY * rY) / R2);
                matrix[rX+lR][rY+lR] = v <= 1 ? (short)1 : (short)0;

//                count += memory[rX+lR][rY+lR];
            }
        }
//        System.out.println(count);

        return matrix;
    }
}
