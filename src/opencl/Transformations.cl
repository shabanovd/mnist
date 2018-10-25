short next(short a, short alphas)
{
    short next = a + 1;
    if (next >= alphas) {
        return 0;
    }
    return next;
}

short prev(short a, short alphas) {
    short prev = a - 1;
    if (prev < 0) {
        return alphas - 1;
    }
    return prev;
}

bool IsAlpha(short a, short alpha, short alphas) {

    if (alpha == a
        //one
        || alpha == next(a, alphas)
        || alpha == prev(a, alphas)
        //two
        //|| alpha == next(next(a, alphas), alphas)
        //|| alpha == prev(prev(a, alphas), alphas)
        //three
        //|| alpha == next(next(next(a, alphas), alphas), alphas)
        //|| alpha == prev(prev(prev(a, alphas), alphas), alphas)
        ) return true;

    return false;
}

kernel void transformations(
    global const char* image,

    global const char* transMatrix,

    global const short* ids,
    global const short* cycleMask,

    global ulong* trans,

    int totalObjects,
    int tT,

    int N_X, int N_Y, int alphas,
    int idsX, int idsY, int lR
) {
    int contextN = get_global_id(0);

    if (contextN >= tT) return;

    int offset = N_Y*N_X*contextN;

    int lD = lR * 2 + 1;

    float lSx = (idsX - lR) / (float)N_X;
    float lSy = (idsY - lR) / (float)N_Y;

    //transformation
    for (int iX = 0; iX < N_X; iX++) {
        for (int iY = 0; iY < N_X; iY++) {
            char iO = image[N_X*iY + iX];
            if (iO >= 0) {

                //trans
                int objID = N_Y*N_X*iO + N_X*iY + iX;

                int index = (totalObjects * contextN + objID) * 3;

                char jX = transMatrix[index + 0];
                char jY = transMatrix[index + 1];
                char jO = transMatrix[index + 2];

                if (jX >= 0) {

                    for (int rX = 0; rX < lD; rX++) {
                        for (int rY = 0; rY < lD; rY++) {

                            if (cycleMask[lD * rY + rX] > 0) {

                                int x = (int)((jX * lSx) + rX);
                                int y = (int)((jY * lSy) + rY);

                                if (x >= 0 && x < idsX && y >= 0 && y < idsY) {
                                    int bIndex = (idsY*idsX*contextN) + (idsX*y) + x;
                                    int sIndex = (idsX*y) + x;

                                    if (IsAlpha(jO, ids[sIndex], alphas)) {
                                        trans[bIndex/64] |= (1ul<<(bIndex%64));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}