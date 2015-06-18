package org.vectrola.chirpchain.test0;

/**
 * Created by jlunder on 6/16/15.
 */
public class PeakListRecognizer extends CodeRecognizer {
    public static class Fingerprint extends CodeRecognizer.Fingerprint {
        protected static final FrequencyTransformer ft = new FrequencyTransformer();
        protected static final SampleSeries pad = new SampleSeries(FrequencyTransformer.WAVELET_WINDOW_SAMPLES);

        private float[] peaks;

        public float[] getPeaks() {
            return peaks;
        }

        protected Fingerprint(SampleSeries code) {
            super(code);

            peaks = new float[getMatchRows()];
            findPeaksFingerprint(bins, peaks);
        }
    }

    PeakListRecognizer(CodeLibrary library) {
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
        float[] inputFingerprint = new float[library.maxCodeRows() * FrequencyTransformer.BINS_PER_ROW];
        float[] inputPeaks = new float[library.maxCodeRows()];
        int bestSym = -1;
        float bestQ = 0f;
        float secondQ = 0f;
        frequencyTransformer.getBinRows(inputFingerprint, library.maxCodeRows());
        findPeaksInput(inputFingerprint, inputPeaks);

        for (int i = 0; i < CodeLibrary.NUM_SYMBOLS; ++i) {
            Fingerprint fp = (Fingerprint)getFingerprintForSymbol(i);
            float q = matchQuality(fp.getPeaks(), inputPeaks);
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

    public static float matchQuality(float[] codePeaks, float[] inputPeaks) {
        float q = 0;
        int samples = 0;
        for (int i = 1; i < Math.min(codePeaks.length - 1, inputPeaks.length); ++i) {
            if (codePeaks[i] > 0f) {
                float diff = Math.abs(inputPeaks[i] - codePeaks[i]) / FrequencyTransformer.BIN_BANDWIDTH;
                float margin = 0;
                if (codePeaks[i - 1] > 0) {
                    margin = Math.abs(codePeaks[i - 1] - codePeaks[i]);
                }
                if (codePeaks[i + 1] > 0) {
                    margin = Math.max(Math.abs(codePeaks[i + 1] - codePeaks[i]), margin);
                }
                margin /= FrequencyTransformer.BIN_BANDWIDTH;
                float thisQ = Math.min(Math.max(1f - (diff - margin - 0.6f), 0f), 1f);
                q += thisQ * thisQ;
                ++samples;
            }
        }
        if (samples == 0) {
            return 0f;
        } else {
            return q / (float) samples;
        }
    }

    public static void findPeaksFingerprint(float[] fingerprint, float[] peaks) {
        int rows = fingerprint.length / FrequencyTransformer.BINS_PER_ROW;
        float ex = mean(fingerprint);
        for (int j = 0; j < rows; ++j) {
            int rowOffset = j * FrequencyTransformer.BINS_PER_ROW;
            float fTot = 0f, fVarTot = 0f, sigTot = 0f;
            float mx = max(fingerprint, rowOffset, FrequencyTransformer.BINS_PER_ROW);
            float baseline = (mx * 0.5f + ex * 0.5f);
            float f = 0f, fVar = 0f;
            for (int i = 0; i < FrequencyTransformer.BINS_PER_ROW; ++i) {
                float sig = Math.max(fingerprint[rowOffset + i] - baseline, 0f);
                sigTot += sig;
                fTot += sig * FrequencyTransformer.BIN_FREQUENCIES[i];
            }
            if (sigTot > 0f) {
                f = fTot / sigTot;
                for (int i = 0; i < FrequencyTransformer.BINS_PER_ROW; ++i) {
                    float sig = Math.max(fingerprint[rowOffset + i] - baseline, 0f);
                    float thisFv = sig * (FrequencyTransformer.BIN_FREQUENCIES[i] - f);
                    fVarTot += thisFv * thisFv;
                }
                fVar = (float) Math.sqrt(fVarTot) / sigTot;
            }
            if (fVar < FrequencyTransformer.BIN_BANDWIDTH * 1.5f) {
                peaks[j] = f;
            } else {
                peaks[j] = 0f;
            }
        }
    }

    public static void findPeaksInput(float[] input, float[] peaks) {
        int rows = input.length / FrequencyTransformer.BINS_PER_ROW;
        for (int j = 0; j < rows; ++j) {
            int rowOffset = j * FrequencyTransformer.BINS_PER_ROW;
            int peak = 0;
            for (int i = 1; i < FrequencyTransformer.BINS_PER_ROW; ++i) {
                if (input[rowOffset + i] > input[rowOffset + peak]) {
                    peak = i;
                }
            }
            peaks[j] = FrequencyTransformer.BIN_FREQUENCIES[peak];
        }
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
