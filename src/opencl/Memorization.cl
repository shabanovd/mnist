kernel void classes(
    global const ulong* trans,

    global const ulong* classes,

    global const short* ids,
    global const short* cycleMask,

    global float* cor,

    int tT,

    int idsX, int idsY, int idsN
) {

    int contextN = get_global_id(0);
    int classN = get_global_id(1);

    if (contextN >= tT || classN >= idsN) return;




        int wp = image.w;
        int hp = image.h;

        double kx = wp / (double) ids.w;
        double ky = hp / (double) ids.h;

        //ids.init(wp, hp);
        ids.init();

        List<Integer>[][] measure = new ArrayList[wp][hp];

        for (int x = 0; x < wp; x++) {
            for (int y = 0; y < hp; y++) {

                int[][] varVector = new int[w][h];

                for (int mx = 0; mx < w; mx++) {
                    for (int my = 0; my < h; my++) {

                        int cx = x + mx;
                        int cy = y + my;

                        if (cx >= 0 && cx < image.w && cy >= 0 && cy < image.h) {
                            varVector[mx][my] = image.pixel(cx, cy) * mask[mx][my];
                        }
                    }
                }

                double[] alphas = match(varVector);

                double max = minLevel;

                for (int i = 0; i < alphas.length; i++) {
                    if (alphas[i] > max) {
                        max = alphas[i];
                    }
                }

                for (int i = 0; i < alphas.length; i++) {
//                    System.out.println(x + ":" + y + ":" + i + " = " + alphas[i]);
                    if (alphas[i] >= max) {
//                        System.out.println(x + ":" + y + ":" + i + " = " + alphas[i]);
                        if (measure[x][y] == null) {
                            measure[x][y] = new ArrayList<>();
                        }
                        measure[x][y].add(i);
                    }
                }
            }

        }

        for (int x = 0; x < ids.w; x++) {
            for (int y = 0; y < ids.h; y++) {

                int dx = (int) (kx * x);
                int dy = (int) (ky * y);

                List<Integer> list = measure[dx][dy];
                if (list != null) {
                    for (Integer a : list) {
                        ids.set(vector, x, y, a);
                    }
                }
            }
        }
    }
