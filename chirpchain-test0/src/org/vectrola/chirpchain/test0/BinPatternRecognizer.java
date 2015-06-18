package org.vectrola.chirpchain.test0;

import com.sun.org.apache.xml.internal.utils.IntVector;

import java.util.Vector;

/**
 * Created by jlunder on 6/16/15.
 */
public class BinPatternRecognizer extends CodeRecognizer {
    public static class BinPatternFingerprint extends CodeRecognizer.CodeFingerprint {
        protected static final FrequencyTransformer ft = new FrequencyTransformer();
        protected static final SampleSeries pad = new SampleSeries(FrequencyTransformer.WAVELET_WINDOW_SAMPLES);

        private int[] binPattern;

        public int[] getBinPattern() {
            return binPattern;
        }

        protected BinPatternFingerprint(SampleSeries code) {
            super(code);
            binPattern = findBinPattern(getBins());
        }
    }

    BinPatternRecognizer(CodeLibrary library) {
        super(library);
        fingerprintLibrary();
    }

    public void fingerprintLibrary() {
        for (int i = 0; i < CodeLibrary.NUM_SYMBOLS; ++i) {
            SampleSeries code = library.getCodeForSymbol(i);
            codeFingerprints[i] = new BinPatternFingerprint(code);
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
            BinPatternFingerprint fp = (BinPatternFingerprint)getFingerprintForSymbol(i);
            float q = matchQuality(fp.getBinPattern(), inputBinRows, mx * 0.5f + ex * 0.5f);
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

    public static float matchQuality(int[] binPattern, float[] inputBinRows, float threshold) {
        int hits = 0;
        for (int i = 0; i < binPattern.length; ++i) {
            int offset = binPattern[i];
            if (inputBinRows[offset] > threshold) {
                ++hits;
            }
        }
        return (float) hits / (float) binPattern.length;
    }

    public static int[] findBinPattern(float[] binRows) {
        int[] pattern = new int[binRows.length];
        int patternUsed = 0;
        int rows = binRows.length / FrequencyTransformer.BINS_PER_ROW;
        float mx = max(binRows, 0, binRows.length);
        float threshold = mx * 0.25f;
        for (int j = 0; j < rows; ++j) {
            for(int i = 0; i < FrequencyTransformer.BINS_PER_ROW; ++i) {
                int offset = j * FrequencyTransformer.BINS_PER_ROW + i;
                if(binRows[offset] > threshold) {
                    pattern[patternUsed++] = offset;
                }
            }
        }
        int[] minPattern = new int[patternUsed];
        System.arraycopy(pattern, 0, minPattern, 0, patternUsed);
        return minPattern;
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
