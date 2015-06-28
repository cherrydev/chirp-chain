package org.vectrola.chirpchain.test0;

import com.sun.org.apache.bcel.internal.classfile.Code;

/**
 * Created by jlunder on 6/10/15.
 */
public abstract class CodeRecognizer {
    public static class Fingerprint {
        protected static final FrequencyTransformer fingerprintFT = new FrequencyTransformer(false, true);
        protected static final SampleSeries pad = new SampleSeries(FrequencyTransformer.WAVELET_WINDOW_SAMPLES);

        protected SampleSeries code;
        protected float[] bins;
        private int matchRows;

        public float[] getBins() {
            return bins;
        }

        public int getMatchRows() {
            return matchRows;
        }

        public Fingerprint(SampleSeries code) {
            this.code = code;
            this.matchRows = code.size() / FrequencyTransformer.ROW_SAMPLES;

            int fingerprintRows;

            synchronized (fingerprintFT) {
                fingerprintFT.flush();
                fingerprintFT.addSamples(code);
                fingerprintFT.addSamples(pad);

                bins = new float[matchRows * FrequencyTransformer.BINS_PER_ROW];
                fingerprintFT.getBinRows(bins, matchRows);
            }

        }
    }

    protected FrequencyTransformer frequencyTransformer = new FrequencyTransformer(false, false);
    protected CodeLibrary library;
    protected Fingerprint[] codeFingerprints;

    private boolean hasNextSymbol;
    private int nextSymbol;
    private float lastSymbolTime = 0f;
    private int rowsSinceSymbolDetected = 0;

    private float matchBaseThreshold;
    private float matchBestToSecondBestThreshold;

    public CodeRecognizer(CodeLibrary library, float matchBaseThreshold, float matchBestToSecondBestThreshold) {
        this.library = library;
        this.codeFingerprints = new Fingerprint[CodeLibrary.NUM_SYMBOLS];
        this.matchBaseThreshold = matchBaseThreshold;
        this.matchBestToSecondBestThreshold = matchBestToSecondBestThreshold;
    }

    public Fingerprint getFingerprintForSymbol(int symbol)
    {
        return codeFingerprints[symbol];
    }

    public void warmup(SampleSeries samples) {
        frequencyTransformer.warmup(samples);
        hasNextSymbol = false;
    }

    public void process(SampleSeries samples) {
        frequencyTransformer.addSamples(samples);
    }

    public boolean matchQualityGraph(float[] results) {
        int resultRows = results.length / CodeLibrary.NUM_SYMBOLS;
        float bins[] = new float[library.maxCodeRows() * FrequencyTransformer.BINS_PER_ROW];
        for(int i = 0; i < resultRows; ++i) {
            frequencyTransformer.getBinRows(bins, library.maxCodeRows());
            for(int j = 0; j < CodeLibrary.NUM_SYMBOLS; ++j) {
                results[i * CodeLibrary.NUM_SYMBOLS + j] = matchQuality(getFingerprintForSymbol(j), bins);
            }
            frequencyTransformer.discardRows(1);
        }
        return true;
    }

    public abstract float matchQuality(Fingerprint fp, float[] inputBinRows);

    public boolean hasNextSymbol() {
        if (!hasNextSymbol) {
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
        while (!hasNextSymbol && canMatch()) {
            int matchSym = tryFindMatch();

            if(matchSym != -1) {
                assert matchSym != -1;
                int codeRows = getFingerprintForSymbol(matchSym).getMatchRows();
                hasNextSymbol = true;
                rowsSinceSymbolDetected = 0;
                nextSymbol = matchSym;
                lastSymbolTime = frequencyTransformer.getTime();
                frequencyTransformer.discardRows(codeRows - 2);
            }
            else {
                ++rowsSinceSymbolDetected;
                if(rowsSinceSymbolDetected > library.maxCodeRows()) {
                    hasNextSymbol = true;
                    rowsSinceSymbolDetected = 0;
                    nextSymbol = -1; // break
                }
                frequencyTransformer.discardRows(1);
            }
        }
    }

    protected boolean canMatch() {
        return frequencyTransformer.availableRows() >= library.maxCodeRows();
    }

    protected int tryFindMatch() {
        float[] inputBinRows = new float[library.maxCodeRows() * FrequencyTransformer.BINS_PER_ROW];
        int bestSym = -1;
        float bestQ = 0f;
        float secondQ = 0f;
        frequencyTransformer.getBinRows(inputBinRows, library.maxCodeRows());

        for (int i = 0; i < CodeLibrary.NUM_SYMBOLS; ++i) {
            Fingerprint fp = getFingerprintForSymbol(i);
            float q = matchQuality(fp, inputBinRows);
            if (q > bestQ) {
                secondQ = bestQ;
                bestQ = q;
                bestSym = i;
            }
        }

        if ((bestQ > matchBaseThreshold) && ((secondQ / bestQ) < matchBestToSecondBestThreshold)) {
            //System.out.print("Y!");
            return bestSym;
        } else {
            //System.out.println("n.");
            return -1;
        }
    }

    protected static float max(float[] values) {
        float maxValue = values[0];
        for (int i = 0; i < values.length; ++i) {
            maxValue = Math.max(maxValue, values[i]);
        }
        return maxValue;
    }

    protected static float mean(float[] vals) {
        float sum = 0f;

        for (int i = 0; i < vals.length; ++i) {
            sum += vals[i];
        }

        return sum / vals.length;
    }
}
