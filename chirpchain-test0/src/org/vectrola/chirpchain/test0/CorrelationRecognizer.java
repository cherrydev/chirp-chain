package org.vectrola.chirpchain.test0;

public class CorrelationRecognizer extends CodeRecognizer {
    CorrelationRecognizer(CodeLibrary library) {
        super(library, 0.5f, 0.1f);
    }

    public float matchQuality(CodeRecognizer.Fingerprint fp, float[] inputBins) {
        /*
        float inputSum = 0f;
        float fpSum = 0f;
        for (int i = 0; i < fp.getPattern().length; ++i) {
            for (int p : fp.getPattern()[i]) {
                inputSum += inputBins[p];
                fpSum += fp.getBins()[p];
            }
        }

        if(inputSum <= 0f) {
            return 0f;
        }

        float ratio = inputSum / fpSum;
        float dev = 0f;
        int count = 0;
        for (int i = 0; i < fp.getPattern().length; ++i) {
            for (int p : fp.getPattern()[i]) {
                float diff = inputBins[p] - fp.getBins()[p] * ratio;
                dev += diff * diff;
            }
            count += fp.getPattern()[i].length;
        }
        dev = (float) Math.sqrt(dev / count);

        return ratio / dev;
        */
        float inputMean = CodeRecognizer.mean(inputBins);
        float inputDev = 0f;
        float[] fpBins = fp.getBins();
        float fpMean = fp.getMean();
        float fpDev = 0f;
        float p = 0f;
        for (int i = 0; i < fpBins.length; ++i) {
            float inputDiff = (inputBins[i] - inputMean);
            float fpDiff = (fpBins[i] - fpMean);
            inputDev += inputDiff * inputDiff;
            fpDev += fpDiff * fpDiff;
            p += inputDiff * fpDiff;
        }
        inputDev = (float) Math.sqrt(inputDev / fpBins.length);
        fpDev = (float) Math.sqrt(fpDev / fpBins.length);
        p = (float) Math.sqrt(p / fpBins.length) / (inputDev * fpDev);

        return p;
    }
}
