package org.vectrola.chirpchain.test0;

import com.sun.org.apache.bcel.internal.classfile.Code;

/**
 * Created by jlunder on 6/10/15.
 */
public class CodeRecognizer {
    private FrequencyTransformer frequencyTransformer = new FrequencyTransformer();
    private CodeLibrary library;
    private int maxCodeRows;
    private CodeFingerprint[] codeFingerprints;
    private boolean hasNextSymbol;
    private int nextSymbol;
    private float time = 0f;
    private float lastSymbolTime = 0f;
    private int rowsSinceSymbolDetected = 0;

    public CodeRecognizer(CodeLibrary library) {
        this.library = library;
        this.maxCodeRows = (int)Math.ceil(library.getMaxCodeLength() / FrequencyTransformer.ROW_TIME);
        this.codeFingerprints = CodeFingerprint.fingerprintLibrary(library);
    }

    public CodeFingerprint getFingerprintForSymbol(int symbol)
    {
        return codeFingerprints[symbol];
    }

    public void process(SampleSeries samples) {
        frequencyTransformer.addSamples(samples);
    }

    public boolean hasNextSymbol() {
        if(!hasNextSymbol) {
            tryFillNextSymbol();
        }
        return hasNextSymbol;
    }

    public int nextSymbol() {
        int sym = nextSymbol;
        nextSymbol = -1;
        hasNextSymbol = false;
        return sym;
    }

    public float getLastSymbolTime() {
        return lastSymbolTime;
    }

    private void tryFillNextSymbol() {
        while (!hasNextSymbol && FingerprintMatcher.canMatch(this)) {
            FingerprintMatcher matcher = new FingerprintMatcher(this);
            int matchSym = matcher.getMatch();

            if(matchSym != -1) {
                assert matchSym != -1;
                int codeRows = codeFingerprints[matchSym].getMatchRows();
                hasNextSymbol = true;
                rowsSinceSymbolDetected = 0;
                nextSymbol = matchSym;
                lastSymbolTime = time;
                time += codeRows * FrequencyTransformer.ROW_TIME;
                frequencyTransformer.discardRows(codeRows);
            }
            else {
                ++rowsSinceSymbolDetected;
                if(rowsSinceSymbolDetected > maxCodeRows) {
                    hasNextSymbol = true;
                    rowsSinceSymbolDetected = 0;
                    nextSymbol = -1; // break
                }
                time += FrequencyTransformer.ROW_TIME;
                frequencyTransformer.discardRows(1);
            }
        }
    }

    public static class CodeFingerprint {
        private static final FrequencyTransformer ft = new FrequencyTransformer();
        private static final SampleSeries pad = new SampleSeries(FrequencyTransformer.WAVELET_WINDOW_SAMPLES - FrequencyTransformer.ROW_SAMPLES);

        float[] bins;
        float[] peaks;
        float[] peakStrengths;

        public float[] getBins() {
            return bins;
        }

        public float[] getPeaks() {
            return peaks;
        }

        public float[] getPeakStrengths() {
            return peakStrengths;
        }

        public int getMatchRows() {
            return peaks.length;
        }

        public CodeFingerprint() {
        }

        protected CodeFingerprint(SampleSeries code) {
            int fingerprintRows;

            synchronized (ft) {
                ft.flush();
                ft.addSamples(code);
                ft.addSamples(pad);

                fingerprintRows = ft.availableRows();
                bins = new float[fingerprintRows * FrequencyTransformer.BINS_PER_ROW];
                ft.getBinRows(bins, fingerprintRows);
            }

            peaks = new float[fingerprintRows];
            peakStrengths = new float[fingerprintRows];
            findPeaks(bins, peaks, peakStrengths);
        }

        public static CodeFingerprint[] fingerprintLibrary(CodeLibrary library) {
            CodeFingerprint[] fingerprints = new CodeFingerprint[CodeLibrary.NUM_SYMBOLS];

            for(int i = 0; i < CodeLibrary.NUM_SYMBOLS; ++i) {
                SampleSeries code = library.getCodeForSymbol(i);
                fingerprints[i] = new CodeFingerprint(code);
            }

            return fingerprints;
        }

        public static void findPeaks(float[] fingerprint, float[] peaks, float[] peakStrengths) {
            int rows = fingerprint.length / FrequencyTransformer.BINS_PER_ROW;
            for(int j = 0; j < rows; ++j) {
                int rowOffset = j * FrequencyTransformer.BINS_PER_ROW;
                int peak = 0;
                for(int i = 1; i < FrequencyTransformer.BINS_PER_ROW; ++i) {
                    if(fingerprint[rowOffset + i] > fingerprint[rowOffset + peak]) {
                        peak = i;
                    }
                }
                peaks[j] = FrequencyTransformer.BIN_FREQUENCIES[peak];
                peakStrengths[j] = fingerprint[rowOffset + peak];
            }
        }
    }

    private static class FingerprintMatcher {
        CodeFingerprint[] codeFingerprints;
        FrequencyTransformer frequencyTransformer;
        float[] inputFingerprint;
        float[] inputPeaks;
        float[] inputPeakStrengths;

        public FingerprintMatcher(CodeRecognizer cr) {
            this.codeFingerprints = cr.codeFingerprints;
            this.frequencyTransformer = cr.frequencyTransformer;
            this.inputFingerprint = new float[cr.maxCodeRows * FrequencyTransformer.BINS_PER_ROW];
            this.inputPeaks = new float[cr.maxCodeRows];
            this.inputPeakStrengths = new float[cr.maxCodeRows];

            frequencyTransformer.getBinRows(this.inputFingerprint, cr.maxCodeRows);
            CodeFingerprint.findPeaks(inputFingerprint, inputPeaks, inputPeakStrengths);
        }

        public static boolean canMatch(CodeRecognizer cr) {
            return cr.frequencyTransformer.availableRows() >= cr.maxCodeRows;
        }

        public int getMatch() {
            int bestSym = -1;
            float bestQ = 0f;
            float secondQ = 0f;

            for(int i = 0; i < CodeLibrary.NUM_SYMBOLS; ++i) {
                float q = matchQuality(codeFingerprints[i], inputPeaks);
                if(q > bestQ) {
                    secondQ = bestQ;
                    bestQ = q;
                    bestSym = i;
                }
            }

            if(bestQ > 0.8f && secondQ < 0.5f) {
                return bestSym;
            }
            else {
                return -1;
            }
        }
    }

    public static float matchQuality(CodeFingerprint fp, float[] inputPeaks) {
        int hits = 0;
        for(int i = 0; i < Math.min(fp.peaks.length, inputPeaks.length); ++i) {
            if(Math.abs(inputPeaks[i] - fp.peaks[i]) < FrequencyTransformer.BIN_BANDWIDTH * 1.5f) {
                ++hits;
            }
        }
        return (float)hits / fp.peaks.length;
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

        return (float)Math.sqrt(sumsq / vals.length);
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
