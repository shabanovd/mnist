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
public class Match implements Comparable<Match> {

    public final int cId;
    public final int tId;

    public final float sim;

    public Match(int cId, int tId, float sim) {
        this.cId = cId;
        this.tId = tId;
        this.sim = sim;
    }

    @Override
    public int compareTo(Match o) {
        return Float.compare(sim, o.sim);
    }
}
