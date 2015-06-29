package org.vectrola.chirpchain.test0;

public class CorrelationRecognizer extends CodeRecognizer {
    CorrelationRecognizer(CodeLibrary library) {
        super(library, 0.5f, 0.1f);
    }

    public float matchQuality(CodeRecognizer.Fingerprint fp, float[] inputBinRows) {
        float inputSum = 0f;
        float fpSum = 0f;
        for (int i = 0; i < fp.getPattern().length; ++i) {
            for (int p : fp.getPattern()[i]) {
                inputSum += inputBinRows[p];
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
                float diff = inputBinRows[p] - fp.getBins()[p] * ratio;
                dev += diff * diff;
            }
            count += fp.getPattern()[i].length;
        }
        dev = (float) Math.sqrt(dev / count);

        return ratio / dev;
    }
}
