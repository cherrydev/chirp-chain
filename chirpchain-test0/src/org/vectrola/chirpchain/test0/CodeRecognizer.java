package org.vectrola.chirpchain.test0;

/**
 * Created by jlunder on 6/10/15.
 */
public abstract class CodeRecognizer {
    public static class Fingerprint {
        protected static final FrequencyTransformer fingerprintFT = new FrequencyTransformer(false, true);
        protected static final SampleSeries pad = new SampleSeries(FrequencyTransformer.WAVELET_WINDOW_SAMPLES);

        protected SampleSeries code;
        protected float[] bins;
        private int[][] pattern;

        public float[] getBins() {
            return bins;
        }

        public int[][] getPattern() {
            return pattern;
        }

        public int getMatchRows() {
            return pattern.length;
        }

        public Fingerprint(SampleSeries code) {
            this.code = code;

            int fingerprintRows = code.size() / FrequencyTransformer.ROW_SAMPLES;

            synchronized (fingerprintFT) {
                fingerprintFT.flush();
                fingerprintFT.addSamples(code);
                fingerprintFT.addSamples(pad);

                bins = new float[fingerprintRows * FrequencyTransformer.BINS_PER_ROW];
                fingerprintFT.getBinRows(bins, fingerprintRows);
            }

            makePattern();
        }

        private void makePattern() {
            int rows = bins.length / FrequencyTransformer.BINS_PER_ROW;
            pattern = new int[rows][];
            float mx = max(getBins());
            float threshold = mx * 0.5f;
            int[] rowTemp = new int[FrequencyTransformer.BINS_PER_ROW];
            int rowTempUsed;
            for (int j = 0; j < rows; ++j) {
                rowTempUsed = 0;
                for (int i = 0; i < FrequencyTransformer.BINS_PER_ROW; ++i) {
                    int offset = j * FrequencyTransformer.BINS_PER_ROW + i;
                    if (bins[offset] > threshold) {
                        rowTemp[rowTempUsed++] = offset;
                    }
                }
                pattern[j] = new int[rowTempUsed];
                System.arraycopy(rowTemp, 0, pattern[j], 0, rowTempUsed);
            }
        }
    }

    protected FrequencyTransformer frequencyTransformer = new FrequencyTransformer(true, false);
    protected CodeLibrary library;
    protected Fingerprint[] codeFingerprints;

    private boolean hasNextSymbol;
    private int nextSymbol;
    private float lastSymbolTime = 0f;
    private int rowsSinceSymbolDetected = 0;

    private float matchBaseThreshold;
    private float matchBestToSecondBestThreshold;

    public CodeLibrary getLibrary() {
        return library;
    }

    public Fingerprint getFingerprintForSymbol(int symbol) {
        return codeFingerprints[symbol];
    }

    public CodeRecognizer(CodeLibrary library, float matchBaseThreshold, float matchBestToSecondBestThreshold) {
        this.library = library;
        this.codeFingerprints = new Fingerprint[CodeLibrary.NUM_SYMBOLS];
        this.matchBaseThreshold = matchBaseThreshold;
        this.matchBestToSecondBestThreshold = matchBestToSecondBestThreshold;

        fingerprintLibrary();
    }

    public void fingerprintLibrary() {
        for (int i = 0; i < CodeLibrary.NUM_SYMBOLS; ++i) {
            SampleSeries code = library.getCodeForSymbol(i);
            codeFingerprints[i] = new Fingerprint(code);
        }
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
        float bins[] = new float[library.getMaxCodeRows() * FrequencyTransformer.BINS_PER_ROW];
        for (int i = 0; i < resultRows; ++i) {
            frequencyTransformer.getBinRows(bins, library.getMaxCodeRows());
            for (int j = 0; j < CodeLibrary.NUM_SYMBOLS; ++j) {
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

            if (matchSym != -1) {
                assert matchSym != -1;
                int codeRows = getFingerprintForSymbol(matchSym).getMatchRows();
                hasNextSymbol = true;
                rowsSinceSymbolDetected = 0;
                nextSymbol = matchSym;
                lastSymbolTime = frequencyTransformer.getTime();
                frequencyTransformer.discardRows(codeRows - 2);
            } else {
                ++rowsSinceSymbolDetected;
                if (rowsSinceSymbolDetected > library.getMaxCodeRows()) {
                    hasNextSymbol = true;
                    rowsSinceSymbolDetected = 0;
                    nextSymbol = -1; // break
                }
                frequencyTransformer.discardRows(1);
            }
        }
    }

    protected boolean canMatch() {
        return frequencyTransformer.getAvailableRows() >= library.getMaxCodeRows();
    }

    protected int tryFindMatch() {
        float[] inputBinRows = new float[library.getMaxCodeRows() * FrequencyTransformer.BINS_PER_ROW];
        int bestSym = -1;
        float bestQ = 0f;
        float secondQ = 0f;
        frequencyTransformer.getBinRows(inputBinRows, library.getMaxCodeRows());

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
