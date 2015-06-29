package org.vectrola.chirpchain.test0;

/**
 * Created by jlunder on 6/16/15.
 */
public class TimeCorrelatingRecognizer extends CodeRecognizer {
    TimeCorrelatingRecognizer(CodeLibrary library) {
        super(library, 0.5f, 0.3f);
    }

    public float matchQuality(CodeRecognizer.Fingerprint fp, float[] inputBinRows) {
        float q = 1f;
        int zoneHits = 0;
        int zoneSamples = 0;
        int lastZone = 0;
        for (int i = 0; i < fp.getPattern().length; ++i) {
            float patternSum = 0f;
            for (int p: fp.getPattern()[i]) {
                patternSum += inputBinRows[p];
            }
            if (patternSum > 0.10f) {
                ++zoneHits;
            }
            ++zoneSamples;

            int zone = (i + 1) * 6 / fp.getPattern().length;
            if (zone != lastZone && zoneSamples > 0) {
                float zoneQ = (float) zoneHits / zoneSamples;
                q *= zoneQ;
                lastZone = zone;
                zoneHits = 0;
                zoneSamples = 0;
            }
        }

        return q;
    }
}
