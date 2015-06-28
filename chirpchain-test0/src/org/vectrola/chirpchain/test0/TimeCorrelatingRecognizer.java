package org.vectrola.chirpchain.test0;

/**
 * Created by jlunder on 6/16/15.
 */
public class TimeCorrelatingRecognizer extends CodeRecognizer {
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

    TimeCorrelatingRecognizer(CodeLibrary library) {
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
        float q = 1f;
        int hits = 0;
        int zoneHits = 0;
        int zoneSamples = 0;
        int lastZone = 0;
        float threshold = 0.01f;
        for (int i = 0; i < fp.pattern.length; ++i) {
            float patternSum = 0f;
            for (int j = 0; j < fp.pattern[i].length; ++j) {
                patternSum += inputBinRows[fp.pattern[i][j]];
            }
            if (patternSum > threshold) {
                ++zoneHits;
            }
            ++zoneSamples;

            int zone = (i + 1) * 6 / fp.pattern.length;
            if (zone != lastZone && zoneSamples > 0) {
                float zoneQ = (float) zoneHits / zoneSamples;
                q *= zoneQ;
                if(zoneQ >= 0.5f) {
                    ++hits;
                }
                lastZone = zone;
                zoneHits = 0;
                zoneSamples = 0;
            }
        }
        return hits >= 6 ? q : 0f;
    }
}
