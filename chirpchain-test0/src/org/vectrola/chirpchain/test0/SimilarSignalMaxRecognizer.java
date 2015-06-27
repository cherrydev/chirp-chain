package org.vectrola.chirpchain.test0;

/**
 * Created by jlunder on 6/16/15.
 */
public class SimilarSignalMaxRecognizer extends CodeRecognizer {
    public static class Fingerprint extends CodeRecognizer.Fingerprint {
        private int[][] pattern;

        public int[][] getPattern() {
            return pattern;
        }

        protected Fingerprint(SampleSeries code) {
            super(code);
            makePattern();
        }

        private void makePattern() {
            int rows = getMatchRows();
            pattern = new int[rows][];
            float mx = max(getBins());
            float threshold = mx * 0.25f;
            int[] rowTemp = new int[FrequencyTransformer.BINS_PER_ROW];
            int rowTempUsed;
            for (int j = 0; j < rows; ++j) {
                rowTempUsed = 0;
                for(int i = 0; i < FrequencyTransformer.BINS_PER_ROW; ++i) {
                    int offset = j * FrequencyTransformer.BINS_PER_ROW + i;
                    if(bins[offset] > threshold) {
                        rowTemp[rowTempUsed++] = offset;
                    }
                }
                pattern[j] = new int[rowTempUsed];
                System.arraycopy(rowTemp, 0, pattern[j], 0, rowTempUsed);
            }
        }
    }

    SimilarSignalMaxRecognizer(CodeLibrary library) {
        super(library);
        fingerprintLibrary();
    }

    public void fingerprintLibrary() {
        for (int i = 0; i < CodeLibrary.NUM_SYMBOLS; ++i) {
            SampleSeries code = library.getCodeForSymbol(i);
            codeFingerprints[i] = new Fingerprint(code);
        }
    }

    public int tryFindMatch() {
        float[] inputBinRows = new float[library.maxCodeRows() * FrequencyTransformer.BINS_PER_ROW];
        int bestSym = -1;
        float bestQ = 0f;
        float secondQ = 0f;
        frequencyTransformer.getBinRows(inputBinRows, library.maxCodeRows());

        for (int i = 0; i < CodeLibrary.NUM_SYMBOLS; ++i) {
            Fingerprint fp = (Fingerprint)getFingerprintForSymbol(i);
            float q = matchQuality(fp, inputBinRows);
            if (q > bestQ) {
                secondQ = bestQ;
                bestQ = q;
                bestSym = i;
            }
        }

        //System.out.print(String.format("R b %.4f s %.4f ", bestQ, secondQ));
        if ((bestQ > 0.6f) && ((secondQ / bestQ) < 0.3f)) {
            //System.out.print("Y!");
            return bestSym;
        } else {
            //System.out.println("n.");
            return -1;
        }
    }


    public float matchQuality(CodeRecognizer.Fingerprint genericFp, float[] inputBinRows) {
        Fingerprint fp = (Fingerprint)genericFp;
        int zoneSamples = 0;
        int lastZone = 0;
        float[] zoneMeans = new float[6];
        for (int i = 0; i < fp.pattern.length; ++i) {
            float rowMax = 0f;
            for (int j = 0; j < fp.pattern[i].length; ++j) {
                float val = inputBinRows[fp.pattern[i][j]];
                rowMax = Math.max(rowMax, val);
            }
            zoneMeans[lastZone] += rowMax;
            ++zoneSamples;

            int zone = (i + 1) * zoneMeans.length / fp.pattern.length;
            if (zone != lastZone && zoneSamples > 0) {
                zoneMeans[lastZone] /= zoneSamples;
                zoneSamples = 0;
            }
        }
        float meanOfMeans = mean(zoneMeans);
        float stdDevOfMeans = 0f;
        for(int i = 0; i < zoneMeans.length; ++i) {
            float dev = zoneMeans[i] - meanOfMeans;
            stdDevOfMeans += dev * dev;
        }
        stdDevOfMeans = (float)Math.sqrt(stdDevOfMeans / zoneMeans.length);
        if(meanOfMeans > 0f) {
            return 1.0f / (1.0f + stdDevOfMeans / meanOfMeans);
        }
        return 0f;
    }


    public static float max(float[] values) {
        float maxValue = values[0];
        for (int i = 0; i < values.length; ++i) {
            maxValue = Math.max(maxValue, values[i]);
        }
        return maxValue;
    }

    public static float mean(float[] vals) {
        float sum = 0f;

        for (int i = 0; i < vals.length; ++i) {
            sum += vals[i];
        }

        return sum / vals.length;
    }
}
