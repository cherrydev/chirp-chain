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
        super(library, 0.6f, 0.3f);
        fingerprintLibrary();
    }

    public void fingerprintLibrary() {
        for (int i = 0; i < CodeLibrary.NUM_SYMBOLS; ++i) {
            SampleSeries code = library.getCodeForSymbol(i);
            codeFingerprints[i] = new Fingerprint(code);
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
}
