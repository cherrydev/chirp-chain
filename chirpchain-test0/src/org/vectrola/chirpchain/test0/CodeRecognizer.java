package org.vectrola.chirpchain.test0;

/**
 * Created by jlunder on 6/10/15.
 */
public class CodeRecognizer {
    public static class Fingerprint {
        protected static final FrequencyTransformer ft = new FrequencyTransformer(false, true);
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

            synchronized (ft) {
                ft.flush();
                ft.addSamples(code);
                ft.addSamples(pad);

                bins = new float[matchRows * FrequencyTransformer.BINS_PER_ROW];
                ft.getBinRows(bins, matchRows);
            }

        }
    }

    protected FrequencyTransformer fingerprintFT = new FrequencyTransformer(false, false);
    protected CodeLibrary library;
    protected Fingerprint[] codeFingerprints;

    private boolean hasNextSymbol;
    private int nextSymbol;
    private float time = 0f;
    private float lastSymbolTime = 0f;
    private int rowsSinceSymbolDetected = 0;

    public CodeRecognizer(CodeLibrary library) {
        this.library = library;
        this.codeFingerprints = new Fingerprint[CodeLibrary.NUM_SYMBOLS];
    }

    public Fingerprint getFingerprintForSymbol(int symbol)
    {
        return codeFingerprints[symbol];
    }

    public void process(SampleSeries samples) {
        fingerprintFT.addSamples(samples);
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
        while (!hasNextSymbol && canMatch()) {
            int matchSym = tryFindMatch();

            if(matchSym != -1) {
                assert matchSym != -1;
                int codeRows = getFingerprintForSymbol(matchSym).getMatchRows();
                hasNextSymbol = true;
                rowsSinceSymbolDetected = 0;
                nextSymbol = matchSym;
                lastSymbolTime = time;
                time += codeRows * FrequencyTransformer.ROW_TIME;
                fingerprintFT.discardRows(codeRows - 2);
            }
            else {
                ++rowsSinceSymbolDetected;
                if(rowsSinceSymbolDetected > library.maxCodeRows()) {
                    hasNextSymbol = true;
                    rowsSinceSymbolDetected = 0;
                    nextSymbol = -1; // break
                }
                time += FrequencyTransformer.ROW_TIME;
                fingerprintFT.discardRows(1);
            }
        }
    }

    protected boolean canMatch() {
        return fingerprintFT.availableRows() >= library.maxCodeRows();
    }

    protected int tryFindMatch() {
        return -1;
    }

}
