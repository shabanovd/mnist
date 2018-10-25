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

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 */
public class Gallery extends JFrame {

    java.util.List<BufferedImage> imgs;

    JPanel panel;

    Gallery(Stream<BufferedImage> stream) {

        imgs = stream.collect(Collectors.toList());

        int cols = 9;//rows;
        int rows = (int)(imgs.size() / (double)cols) + 1; //(int) (Math.sqrt(imgs.size()));

        panel = new JPanel(new GridLayout(rows,cols,2,2));

        int c = 0;
        for (BufferedImage img : imgs) {
            panel.add(new ImagePanel(img, String.valueOf(c++)));
        }


        JScrollPane scroll = new JScrollPane( panel );

        BufferedImage img = imgs.get(0);
        panel.setPreferredSize(new Dimension((img.getWidth()+2)*cols, (img.getHeight()+2)*rows));

        this.add(scroll);
        this.pack();
        this.setVisible(true);
    }

    public class ImagePanel extends JPanel {
        BufferedImage image;
        String label;

        ImagePanel(BufferedImage image, String label) {
            this.image = image;
            this.label = label;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(image, 0, 0, null);
            g.drawString(label, 2, 10);
        }
    }
}