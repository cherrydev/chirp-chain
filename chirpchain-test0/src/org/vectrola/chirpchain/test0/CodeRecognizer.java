package org.vectrola.chirpchain.test0;

import com.sun.org.apache.bcel.internal.classfile.Code;

/**
 * Created by jlunder on 6/10/15.
 */
public class CodeRecognizer {
    public static class CodeFingerprint {
        protected static final FrequencyTransformer ft = new FrequencyTransformer();
        protected static final SampleSeries pad = new SampleSeries(FrequencyTransformer.WAVELET_WINDOW_SAMPLES);

        protected SampleSeries code;
        private int matchRows;

        public int getMatchRows() {
            return matchRows;
        }

        public CodeFingerprint(SampleSeries code) {
            this.code = code;
            this.matchRows = code.size() / FrequencyTransformer.ROW_SAMPLES;
        }
    }

    protected FrequencyTransformer frequencyTransformer = new FrequencyTransformer();
    protected CodeLibrary library;
    protected CodeFingerprint[] codeFingerprints;

    private boolean hasNextSymbol;
    private int nextSymbol;
    private float time = 0f;
    private float lastSymbolTime = 0f;
    private int rowsSinceSymbolDetected = 0;

    public CodeRecognizer(CodeLibrary library) {
        this.library = library;
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
                frequencyTransformer.discardRows(codeRows - 1);
            }
            else {
                ++rowsSinceSymbolDetected;
                if(rowsSinceSymbolDetected > library.maxCodeRows()) {
                    hasNextSymbol = true;
                    rowsSinceSymbolDetected = 0;
                    nextSymbol = -1; // break
                }
                time += FrequencyTransformer.ROW_TIME;
                frequencyTransformer.discardRows(1);
            }
        }
    }

    protected boolean canMatch() {
        return frequencyTransformer.availableRows() >= library.maxCodeRows();
    }

    protected int tryFindMatch() {
        return -1;
    }

}
