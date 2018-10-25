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

/**
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 */
public class DigitImage {

    int w = 28;
    int h = 28;

    int id;
    int label;
    byte[] imageData;

    public DigitImage(int id, int label, byte[] imageData) {
        this.id = id;
        this.label = label;
        this.imageData = imageData;
    }

    public DigitImage(int id, int label, byte[] imageData, int w, int h) {
        this.id = id;
        this.label = label;
        this.imageData = imageData;
        this.w = w;
        this.h = h;
    }

    public int pixel(int x, int y) {
        return imageData[w * y + x] & 0xFF;
    }

    public BufferedImage canvas() {
        BufferedImage canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

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
