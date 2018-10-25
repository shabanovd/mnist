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

import java.util.List;

/**
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 */
public class Context {

    int x;
    int y;
    int o;

    int wt;
    int ht;

    double X_t;
    double Y_t;
    double a_t_rad;
    double HT_t;
    double WT_t;

    public List<List<Integer>> transformations;

    Context(int x, int y, int o, int wt, int ht, double X_t, double Y_t, double a_t_rad, double HT_t, double WT_t) {
        this.x = x;
        this.y = y;
        this.o = o;

        this.wt = wt;
        this.ht = ht;

        this.X_t = X_t;
        this.Y_t = Y_t;
        this.a_t_rad = a_t_rad;
        this.HT_t = HT_t;
        this.WT_t = WT_t;
    }
}
