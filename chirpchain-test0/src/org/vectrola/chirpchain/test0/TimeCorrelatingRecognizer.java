package org.vectrola.chirpchain.test0;

import com.sun.org.apache.xml.internal.utils.IntVector;

import java.util.Vector;

/**
 * Created by jlunder on 6/16/15.
 */
public class TimeCorrelatingRecognizer extends CodeRecognizer {
    public static class Fingerprint extends CodeRecognizer.Fingerprint {
        protected static final FrequencyTransformer ft = new FrequencyTransformer();
        protected static final SampleSeries pad = new SampleSeries(FrequencyTransformer.WAVELET_WINDOW_SAMPLES);

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
            float mx = max(getBins(), 0, getBins().length);
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

        float ex = mean(inputBinRows);
        float mx = max(inputBinRows, 0, inputBinRows.length);

        for (int i = 0; i < CodeLibrary.NUM_SYMBOLS; ++i) {
            Fingerprint fp = (Fingerprint)getFingerprintForSymbol(i);
            float q = matchQuality(fp, inputBinRows, ex, mx);
            if (q > bestQ) {
                secondQ = bestQ;
                bestQ = q;
                bestSym = i;
            }
        }

        if ((bestQ > 0.5f) && ((secondQ / bestQ) < 0.7f)) {
            return bestSym;
        } else {
            return -1;
        }
    }

    public static float matchQuality(Fingerprint fp, float[] inputBinRows, float ex, float mx) {
        float q = 1f;
        int zoneHits = 0;
        int zoneSamples = 0;
        int lastZone = 0;
        float threshold = mx * 0.5f + ex * 0.5f;
        for (int i = 0; i < fp.pattern.length; ++i) {
            float patternSum = 0f;
            for (int j = 0; j < fp.pattern[i].length; ++j) {
                patternSum += inputBinRows[fp.pattern[i][j]];
            }
            if (patternSum > threshold) {
                ++zoneHits;
            }
            ++zoneSamples;

            int zone = (i + 1) * 4 / fp.pattern.length;
            if (zone != lastZone && zoneSamples > 0) {
                q *= (float) zoneHits / zoneSamples;
                lastZone = zone;
            }
        }
        return q;
    }

    public static float max(float[] values, int offset, int length) {
        float maxValue = values[0];
        for (int i = offset; i < offset + length; ++i) {
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

    public static float stddev(float[] vals, float ex) {
        float sumsq = 0f;

        for (int i = 0; i < vals.length; ++i) {
            float diff = vals[i] - ex;
            sumsq += diff * diff;
        }

        return (float) Math.sqrt(sumsq / vals.length);
    }

    public static float covariance(float[] fp, float fpEx, float[] input, float inputEx) {
        int length = Math.min(fp.length, input.length);

        float cov = 0f;
        for (int i = 0; i < length; ++i) {
            cov += (fp[i] - fpEx) * (input[i] - inputEx);
        }
        cov /= length;

        return cov;
    }

    public static float correlation(float[] fp, float fpEx, float[] input, float inputEx) {
        return covariance(fp, fpEx, input, inputEx) / (stddev(fp, fpEx) * stddev(input, inputEx));
    }
}
