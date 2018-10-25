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

    int num = idsX * idsY / 64;

    int aOffset = num*contextN;
    int bOffset = num*classN;

    ulong sum = 0;
    ulong sA = 0;
    ulong sB = 0;

    for (int i = 0; i < num; i++) {

        int aIndex = aOffset + i;
        int bIndex = bOffset + i;

        ulong aN = trans[aIndex];
        ulong bN = classes[bIndex];

        ulong s = popcount( aN & bN );
        ulong a = popcount( aN );
        ulong b = popcount( bN );

        sum += s;
        sA += a;
        sB += b;
    }

//    r = sum / total;
//
//    r0 = s1 * s2 / (total * total)
//    r1 = min(s1, s2) / total

//     cor[tT*classN + contextN] = sum == 0 ? 0.0f : sum / native_sqrt((float)(sA * sB));
   cor[tT*classN + contextN] = sum == 0 ? 0.0f : sum / (float)sB;
}