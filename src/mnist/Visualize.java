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

import opencl.*;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.lang.System.nanoTime;
import static java.lang.System.out;

/**
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 */
public class Visualize extends JPanel implements KeyListener {

    int iI = 0;
    int iM = 0;

    List<DigitImage> _images_ = new ArrayList<>();

    DB db;

    Memory memory = new Memory();

    BufferedImage[] inputCanvas = new BufferedImage[50];
    BufferedImage matrixCanvas;
    BufferedImage[] idsCanvas = new BufferedImage[50];
    BufferedImage[] restoredCanvas = new BufferedImage[50];
    BufferedImage[] transCanvas = new BufferedImage[50];

    Visualize() {
        super();

        setPreferredSize(new Dimension(1280, 720));
    }

    private void load() {
        try {
            _images_ = DigitImageLoadingService
                .loadDigitImages(Paths.get("data/train-labels-idx1-ubyte"), Paths.get("data/train-images-idx3-ubyte"));

            System.out.println("images loaded");

            Path location = Paths.get("data/memory");

//            boolean doNotCreate = Files.exists(location);
            Files.createDirectories(location.getParent());

            db = DBMaker.fileDB(location.toFile())
                .closeOnJvmShutdown()
//                .executorEnable()
//                .transactionEnable()
                .fileMmapEnable()
                .make();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        //System.out.println("paintComponent "+iI+" "+iM);

        Graphics2D g2 = (Graphics2D) g;
        //g2.clearRect(0, 0, 200, 200);

        int[] xy;
        int cx = 0;
        int cy = 0;

        xy = process(g2, cx, inputCanvas);
        cx += xy[0]; cy = Math.max(cy, xy[1]);

//        if (matrixCanvas != null) {
//            g2.drawImage(matrixCanvas, cx, 0, null, null);
//            cx += matrixCanvas.getWidth() + 1;
//        }

        xy = process(g2, cx, idsCanvas);
        cx += xy[0]; cy = Math.max(cy, xy[1]);

        xy = process(g2, cx, restoredCanvas);
        cx += xy[0]; cy = Math.max(cy, xy[1]);

        xy = process(g2, cx, transCanvas);
        cx += xy[0]; cy = Math.max(cy, xy[1]);

        setPreferredSize(new Dimension(cx, cy));
    }

    private int[] process(Graphics2D g2, int cx, BufferedImage[] canvases) {
        int maxW = 0;
        int maxH = 0;
        for (BufferedImage canvas : canvases) {
            if (canvas != null) {
                maxW = Math.max(canvas.getWidth(), maxW);
                maxH = Math.max(canvas.getHeight(), maxH);
            }
        }

        int cy = 0;
        for (BufferedImage canvas : canvases) {
            if (canvas != null) {
                g2.drawImage(canvas, cx, cy, null, null);
            }
            cy += maxH + 1;
        }
        return new int[] {maxW + 1, cy};
    }

    public void fillCanvas() {
        //inputCanvas[0] = images.get(iI).canvas();
        repaint();
    }

    private DigitImage image() {
        return image(iI);
    }

    private DigitImage image(int index) {
        return image(_images_.get(index));
    }

    private DigitImage image(DigitImage image) {
        if (image instanceof DigitImageFactor) {
            return image;
        }
        return new DigitImageFactor(4, image);
    }

    private void preparation() {
        M1 vars1 = new M1(24, 20, 0);

        memory.ids = new IDS(db, vars1);

        DigitImage image = image();

        try {
            memory.memory(db, vars1, image.w, image.h);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        matrixCanvas = memory.canvas();
    }

    private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("SelectionDemo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Set up the content pane.
        final Visualize vis = new Visualize();
        vis.load();
        vis.fillCanvas();

        vis.preparation();

        Container container = frame.getContentPane();
        container.setLayout(new BoxLayout(container, BoxLayout.PAGE_AXIS));

        JScrollPane scrollPane = new JScrollPane(vis);

        container.add(scrollPane);

        //scrollPane.setSize(new Dimension(1280*2, 720*2));

        frame.addKeyListener(vis);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) throws IOException {
        javax.swing.SwingUtilities.invokeLater(Visualize::createAndShowGUI);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        //System.out.println("keyTyped "+e.getKeyCode()+" "+e.isAltDown());
    }

    @Override
    public void keyPressed(KeyEvent e) {
        System.out.println("keyPressed "+e.getKeyCode()+" "+e.isAltDown());

        if (e.getKeyCode() == 39) {

            iM++;
            showTransformation(iM);

        } else if (e.getKeyCode() == 37)

        {

            iM--;
            showTransformation(iM);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        System.out.println("keyReleased "+e.getKeyCode()+" "+e.isAltDown());

        //p
        if (e.getKeyCode() == 80) {
            memory.memorization(
                _images_.stream()
                    .limit(3000)
                    .map(this::image)
//                    .filter(image -> image.label == 1)
//                _images_.stream().filter(image -> image.label == 2).map(this::image)
            );

            db.commit();

            processElements();

            out.println("done");

        //c
        } else if (e.getKeyCode() == 67) {

            memory.memories.clear();
            memory.elements.clear();
            db.commit();

            DigitImage master = image(0);

            BufferedImage canvas = inputCanvas[0] = master.canvas();

            BitSet v = memory.ids.init();
            memory.process(master, memory.ids, v);

            memory.memories.put(master.id, v.toLongArray());

            int i = 2;

            int delta = 10;

            for (int dx = -delta; dx <= delta; dx += 2) {
                out.println(dx);
                for (int dy = -delta; dy <= delta; dy += 2) {
                    for (double theta = - Math.PI / 4; theta <= Math.PI / 4; theta += Math.PI / 45) {

                        BufferedImage img = new BufferedImage(master.w, master.h, BufferedImage.TYPE_INT_ARGB);

                        Graphics2D g2d = (Graphics2D) img.getGraphics();

                        AffineTransform at = new AffineTransform();
                        at.rotate(theta, img.getWidth()/2, img.getHeight()/2);
                        at.translate(dx, dy);

                        g2d.drawImage(canvas, at, null);
                        g2d.dispose();

//                        inputCanvas[1] = img;

                        byte[] imageData = new byte[img.getWidth() * img.getHeight()];
                        for (int x = 0; x < master.w; x++) {
                            for (int y = 0; y < master.h; y++) {

                                int rgb = img.getRGB(x, y);
                                int r = (rgb >> 16) & 0xFF;
                                int g = (rgb >> 8) & 0xFF;
                                int b = (rgb & 0xFF);

                                int gray = (r + g + b) / 3;

                                imageData[master.w * y + x] = (byte)gray;
                            }
                        }

                        DigitImage di = new DigitImage(i++, 1, imageData, master.w, master.h);

//                        inputCanvas[1] = img;
//                        inputCanvas[i - 2] = di.canvas();
//                        repaint();
//
//                        if (true) return;

                        v = memory.ids.init();
                        memory.process(di, memory.ids, v);

                        memory.memories.put(di.id, v.toLongArray());
                    }
                }
                db.commit();
            }
            db.commit();

            System.out.println("prepared");

            processElements();

            System.out.println("done");

        //v
        } else if (e.getKeyCode() == 86) {

        } else if (e.getKeyCode() == 11186) {
//            memory.memorizationCheck(
//                _images_.stream().filter(image -> image.label == 2).map(this::image)
//            );

            Set<Integer> set = new HashSet<>();
            List<Integer> cIds = new ArrayList<>();

            Map<Integer, Map<Integer, AtomicInteger>> freq = new HashMap<>();

            out.println("size: "+memory.classToImages.size());

            Stream<BufferedImage> imgs = ((Set<int[]>)memory.classToImages.keySet())
                .stream()
                .filter(kk -> {
                    int cID = kk[0];

                    int l = _images_.get(kk[1]).label;

                    freq.compute(cID, (z, fr) -> {
                        if (fr == null) {
                            fr = new HashMap<>();
                        }

                        fr.compute(l, (k,v) -> {
                            if (v == null) return new AtomicInteger(1);

                            v.incrementAndGet();
                            return v;
                        });

                        return fr;
                    });

                    if (set.contains(cID)) return false;
                    set.add(cID);

                    return true;
                })
                .sorted((k1, k2) -> Integer.compare(
                    _images_.get(k1[1]).label,
                    _images_.get(k2[1]).label
                ))
                .map(k -> {
                    cIds.add(k[0]);

                    int iID = k[1];

                    return _images_.get(iID).canvas();
                });


            Gallery view = new Gallery(imgs);

            view.pack();
            view.setVisible(true);

            cIds.forEach(cID -> {
                out.println(">> "+cID);

                Map<Integer, AtomicInteger> fr = freq.get(cID);

                fr.keySet()
                    .stream()
                    .sorted()
                    .forEach(k -> {
                        out.print(k+" ["+freq.get(k)+"], ");
                    });

                out.println();
            });

        //q
        } else if (e.getKeyCode() == 81) {

            Map<Integer, Map<Integer, AtomicInteger>> freq = new HashMap<>();

            out.println("size: "+memory.classToImages.size());

            Stream<BufferedImage> imgs = memory.classes.values().stream()
                .limit(40)
                .map(BitSet::valueOf)
                .map(bits -> {
                    out.println(bits.cardinality());

                    int[][] elements = memory.elements(bits);

                    return memory.restore(elements);
                });

            Gallery view = new Gallery(imgs);

            view.pack();
            view.setVisible(true);

            out.println("size: "+memory.classToImages.size());

            ((Set<int[]>)memory.classToImages.keySet())
                .forEach(kk -> {
                    int cID = kk[0];

                    int l = _images_.get(kk[1]).label;

                    freq.compute(cID, (z, fr) -> {
                        if (fr == null) {
                            fr = new HashMap<>();
                        }

                        fr.compute(l, (k,v) -> {
                            if (v == null) return new AtomicInteger(1);

                            v.incrementAndGet();
                            return v;
                        });

                        return fr;
                    });
                });

            freq.entrySet().forEach(entry -> {
                out.println(">> "+entry.getKey());

                Map<Integer, AtomicInteger> fr = entry.getValue();
                fr.keySet()
                    .stream()
                    .sorted()
                    .forEach(k -> {
                        out.println(k+" ["+fr.get(k)+"], ");
                    });

                out.println();
            });

        } else if (e.getKeyCode() == 40) {
            //down
            that--;
            prepareTransformation();

//            inputCanvas[0] = _images_.get(0).canvas();
//            inputCanvas[1] = image(0).canvas();
//            repaint();

        } else if (e.getKeyCode() == 38) {
            //up
            that++;
            prepareTransformation();

        //b
        } else if (e.getKeyCode() == 66) {

            int total = memory.elements.size();

            AtomicInteger count = new AtomicInteger(total);
            System.out.println("size: "+total);

            TreeSet<Integer> set = new TreeSet<>(memory.elements.keySet());

            //space
            set.stream()
                .limit(3000)
                .forEach(id -> {
                    try {
                        if (count.get() % 100 == 0) {
                            //Portrait.computer(memory);

                            //System.out.println(id + " [" + count + "]");
                            db.commit();

                            //memory.classToImages.put(new int[]{cId, id, nT}, vector);

                            //recalculateClasses();

                            //out.println("next");

//                            if (count.get() < total) throw new RuntimeException();
                        }

                        calcSub(id, true);

                        count.decrementAndGet();

                        //Thread.sleep(10);

                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                });

            //calcPortrait();

            out.println("done");

        //r
        } else if (e.getKeyCode() == 82) {

            int total = memory.elements.size();

            AtomicInteger count = new AtomicInteger(total);
            System.out.println("size: "+total);

            TreeSet<Integer> set = new TreeSet<>(memory.elements.keySet());

            //space
            set.stream()
                .limit(8)
                .forEach(id -> {
                    try {
                        if (image(id).label != 1) return;

                        if (count.get() % 100 == 0) {
                            db.commit();
                        }

                        calcSub(id, true);

                        count.decrementAndGet();

                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                });

            out.println("done");

        } else if (e.getKeyCode() == 32) {
            if (it == null) {
                it = memory.elements.keySet()
                    .stream()
//                    .filter(id -> {
//                        DigitImage img = image(id);
//                        return img.label == 1 || img.label == 3 || img.label == 5;
//                    })
                    .iterator();

                int total = memory.elements.size();

                count = new AtomicInteger(total);
                System.out.println("size: "+total);
            }

            Set<Integer> allow = new HashSet<>();
            allow.add(1);
            allow.add(7);

            Integer id = it.next();
            while (!allow.contains(image(id).label)) {
                id = it.next();
            }
//            id = 2082;

            try {
//                if (count.get() % 100 == 0) {
//                    Portrait.computer(memory);
//
//                    System.out.println(id + " [" + count + "]");
//                    db.commit();
//
//                    recalculateClasses();
//
//                    out.println("next");
//                }

                AtomicInteger c = new AtomicInteger();
                memory.classes.keySet().forEach(k -> {
                    idsCanvas[c.get()] = memory.restore(memory.elements(BitSet.valueOf(memory.classes.get(k))), String.valueOf(k));
                    c.incrementAndGet();
                });

                List<SubClasses.Result> results = calcSub(id, true);
                out.println("results: "+results.size());

                if (results.isEmpty()) return;

                SubClasses.Result result = results.get(0);

                //original
                restoredCanvas[0] = image(id).canvas();

                restoredCanvas[1] = memory.restore(memory.elements(id));

                //transformed
                restoredCanvas[2] = memory.restore(memory.elements(BitSet.valueOf(result.image)), df.format(result.sim));

                //class
                restoredCanvas[3] = memory.restore(memory.elements(BitSet.valueOf(memory.classes.get(result.cId()))), String.valueOf(result.cId()));

//                //class
//                transCanvas[0] = result.maxCID() == null ? null :
//                    memory.restore(memory.elements(BitSet.valueOf(memory.classes.get(result.maxCID()))), String.valueOf(result.maxCID()));

                c.set(0);
                memory.classes.keySet().forEach(k -> {
                    SubClasses.Result rr = null;
                    for (SubClasses.Result r : results) {
                        if (Objects.equals(r.cId(), k)) {
                            rr = r;
                        }
                    }
                    inputCanvas[c.get()] = rr == null ? null :
                        memory.restore(memory.elements(BitSet.valueOf(result.vector(rr.tId()))), df.format(rr.sim) + " [" + rr.tId() + "]");
                    c.incrementAndGet();
                });

                count.decrementAndGet();

                //Thread.sleep(10);

            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

            //recalculateClasses();

            repaint();
        }
    }

    DecimalFormat df = new DecimalFormat("#.000");

    Iterator<Integer> it;
    AtomicInteger count;

    private void calcPortrait() {
        memory.classes.keySet()
            .forEach(id -> {
                Portrait.computer(memory, id);
            });
    }

    private List<Classes.Result> calc(int id, boolean create) throws IOException {
//        long time = nanoTime();

        long[] trans = Transformations.computer(memory, id);
//        return Classes.computer(memory, id, 1, trans, create);
        return Classes.computer(memory, id, image(id).label, trans, create);

//        time = nanoTime() - time;
//        out.println("cycle took: "+(time/1000000)+"ms");
    }

    private List<SubClasses.Result> calcSub(int id, boolean create) throws IOException {
        long[] trans = Transformations.computer(memory, id);
        return SubClasses.computer(memory, id, image(id).label, trans, create);
    }

    public void restore(int[][] m1, int[][] m2) {
        transCanvas[0] = memory.restore(m1);
        transCanvas[1] = memory.restore(m2);

        repaint();
    }

    int that = 0;
    Classes.Result trans;

    private void prepareTransformation() {
        try {
            System.out.println("Transformations: "+that);
            long[] trans_ = Transformations.computer(memory, that);
            System.out.println("Classes");
            List<Classes.Result> results = Classes.computer(memory, that, image(that).label, trans_, true);
            System.out.println("done");
            trans = results.get(0);

            inputCanvas[0] = image(that).canvas();

            showTransformation(0);

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void showTransformation(int t) {
        System.out.println("t: " + t);

        int[][] els = new int[(int) memory.N_X][(int) memory.N_Y];

        byte[] bs = memory.elements.get(that);
        for (int y = 0; y < memory.N_Y; y++) {
            for (int x = 0; x < memory.N_X; x++) {
                els[x][y] = bs[(int) (memory.N_X*y + x)];
            }
        }
        restoredCanvas[4] = memory.restore(els);

        BitSet v2 = BitSet.valueOf(trans.vector(t));
        int[][] tran1 = memory.elements(v2);

        restoredCanvas[5] = memory.restore(tran1);


        BitSet bits = memory.memories(that);
        int[][] elements = memory.elements(bits);

        restoredCanvas[0] = memory.restore(elements);

        int[][] el = memory.contextTransformation(t, elements);

        BitSet ids = memory.ids(el);

        int[][] tran2 = memory.elements(ids);

        restoredCanvas[1] = memory.restore(el);
        restoredCanvas[2] = memory.restore(tran2);

        BitSet ids2 = memory.contextTransformationIds(t, elements);

        int[][] tran3 = memory.elements(ids2);
        restoredCanvas[3] = memory.restore(tran3);

        idsCanvas[0] = memory.ids.canvasSmall(bits);
        idsCanvas[1] = memory.ids.canvasSmall(ids);
        idsCanvas[2] = memory.ids.canvasSmall(ids2);
        //idsCanvas[3] = memory.ids.canvasSmall(v2);
        idsCanvas[4] = memory.ids.canvasSmall(v2);

        repaint();
    }

    private void processElements() {
        int batchSize = 5000;
        try {
            int steps = (int) (memory.memories.size() / (double)batchSize) + 1;

            for (int i = 0; i < steps; i++) {

                TreeSet<Integer> set = new TreeSet<>();
                for (int m = 0; m <= batchSize; m++) {
                    int num = i * batchSize + m;

                    if (memory.memories.containsKey(num)) {
                        set.add(num);
                    }
                }

                Elements.computer(memory, set);

                db.commit();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
