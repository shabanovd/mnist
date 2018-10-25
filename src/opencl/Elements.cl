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

    if (alpha == a || alpha == next(a, alphas) || alpha == prev(a, alphas)) return true;

    return false;
}

kernel void elements(
    global const short* ids,
    global const short* cycleMask,
    global const char* activity,
    global char* recognition,

    int idsX, int idsY, int idsN,
    int N_X, int N_Y, int alphas, int lR
) {

    int iX = get_global_id(0);
    int iY = get_global_id(1);
    int n = get_global_id(2);

    if (iX >= N_X || iY >= N_Y || n >= idsN) return;

    int lD = lR * 2 + 1;

    float lSx = (idsX - lR) / (float)N_X;
    float lSy = (idsY - lR) / (float)N_Y;

    float max = 0.1f; //0.85f;
    int index = -1;

    for (int iO = 0; iO < alphas; iO++) {

        int res = 0;
        int sA = 0;
        int sB = 0;

        for (int rX = 0; rX < lD; rX++) {
            for (int rY = 0; rY < lD; rY++) {

                if (cycleMask[lD * rY + rX] <= 0) continue;

                int x = (int)((iX * lSx) + rX);
                int y = (int)((iY * lSy) + rY);

                if (x >= 0 && x < idsX && y >= 0 && y < idsY) {
                    int bIndex = (idsY*idsX*n) + (idsX*y) + x;
                    int sIndex = (idsX*y) + x;

                    int a = ( activity[bIndex/8] & (1<<(bIndex%8)) ) != 0 ? 1 : 0;

                    int b = IsAlpha(iO, ids[sIndex], alphas) ? 1 : 0;

                    res += a * b;
                    sA += a * a;
                    sB += b * b;
                }
            }
        }

        float result = 0.0f;

        if (res != 0) {
            result = res / (float)sB;
        }

        if (result == max) {
            index = -2;
        } else if (result > max) {
            max = result;
            index = iO;
        }

        //if (n == 0 && iX == 8 && iY == 11) {
        //    printf("max: %f; index: %i; result: %f; %i %i %i %i\n", max, index, result, res, sA, sB, result == max ? 1 : 0);
        //}
    }

    recognition[N_Y*N_X*n + N_X*iY + iX] = index;
}