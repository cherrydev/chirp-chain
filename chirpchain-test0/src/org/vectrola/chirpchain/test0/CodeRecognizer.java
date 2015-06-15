package org.vectrola.chirpchain.test0;

import com.sun.org.apache.bcel.internal.classfile.Code;

/**
 * Created by jlunder on 6/10/15.
 */
public class CodeRecognizer {
    private FrequencyTransformer frequencyTransformer = new FrequencyTransformer();
    private CodeLibrary library;
    private int maxCodeRows;
    private float[][] codeFingerprints =
            new float[CodeLibrary.NUM_SYMBOLS][maxCodeRows * FrequencyTransformer.BINS_PER_ROW];
    private boolean hasNextSymbol;
    private int nextSymbol;
    private float time = 0f;
    private float lastSymbolTime = 0f;
    private int rowsSinceSymbolDetected = 0;

    public CodeRecognizer(CodeLibrary library) {
        this.library = library;
        this.maxCodeRows = (int)Math.ceil(library.getMaxCodeLength() / FrequencyTransformer.ROW_TIME);

        SampleSeries pad = new SampleSeries(FrequencyTransformer.ROW_SAMPLES - 1);
        SampleSeries phasePads[] = new SampleSeries[8];
        for(int i = 0; i < phasePads.length; ++i) {
            phasePads[i] = new SampleSeries((int)((float)i * FrequencyTransformer.ROW_SAMPLES / phasePads.length));
        }
        for(int i = 0; i < CodeLibrary.NUM_SYMBOLS; ++i) {
            SampleSeries code = library.getCodeForSymbol(i);
            int fingerprintRows = Math.max(0, ((code.size() - FrequencyTransformer.WAVELET_WINDOW_SAMPLES) / FrequencyTransformer.ROW_SAMPLES) + 1);
            float[] accumulatedFP = new float[fingerprintRows * FrequencyTransformer.BINS_PER_ROW];
            for(int j = 0; j < phasePads.length; ++j) {
                FrequencyTransformer fingerprinter = new FrequencyTransformer();
                fingerprinter.addSamples(phasePads[j]);
                fingerprinter.addSamples(code);
                fingerprinter.addSamples(pad);

                assert fingerprintRows <= maxCodeRows;
                float[] fp = new float[fingerprintRows * FrequencyTransformer.BINS_PER_ROW];
                fingerprinter.getBinRows(fp, fingerprintRows);
                for(int k = 0; k < accumulatedFP.length; ++k) {
                    accumulatedFP[k] += fp[k] * (1f / phasePads.length);
                }
            }
            codeFingerprints[i] = accumulatedFP;
        }
    }

    public float[] getFingerprintForSymbol(int symbol)
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
        float[] input = new float[maxCodeRows * FrequencyTransformer.BINS_PER_ROW];
        while (!hasNextSymbol && frequencyTransformer.availableRows() >= maxCodeRows) {
            frequencyTransformer.getBinRows(input, maxCodeRows);
            float bestCorr = 0.01f;
            int bestSym = -1;

            for(int i = 0; i < CodeLibrary.NUM_SYMBOLS; ++i) {
                float corr = correlation(codeFingerprints[i], input);
                if(corr > bestCorr) {
                    bestCorr = corr;
                    bestSym = i;
                }
            }

            if(bestSym != -1) {
                int codeRows = codeFingerprints[bestSym].length / FrequencyTransformer.BINS_PER_ROW;
                hasNextSymbol = true;
                rowsSinceSymbolDetected = 0;
                nextSymbol = bestSym;
                lastSymbolTime = time;
                time += (codeRows - 1) * FrequencyTransformer.ROW_TIME;
                frequencyTransformer.discardRows(codeRows - 1);
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

    private float correlation(float[] fp, float[] input)
    {
        int length = Math.min(fp.length, input.length);
        float meanFP = 0f, meanInput = 0, stdDevFP = 0, stdDevInput = 0f;

        for(int i = 0; i < length; ++i) {
            meanFP += fp[i];
            meanInput += input[i];
        }
        meanFP /= length;
        meanInput /= length;

        for(int i = 0; i < length; ++i) {
            stdDevFP += (fp[i] - meanFP) * (fp[i] - meanFP);
            stdDevInput += (input[i] - meanInput) * (input[i] - meanInput);
        }
        stdDevFP /= length;
        stdDevInput /= length;

        float cov = 0f;
        for(int i = 0; i < length; ++i) {
            cov += (fp[i] - meanFP) * (input[i] - meanInput);
        }
        return cov / (length * stdDevFP * stdDevInput);
    }
}
