package org.vectrola.chirpchain.test0;

/**
 * Created by jlunder on 6/16/15.
 */
public class TimeCorrelatingRecognizer extends CodeRecognizer {
    TimeCorrelatingRecognizer(CodeLibrary library) {
        super(library, 0.5f, 0.01f);
    }

    public float matchQuality(CodeRecognizer.Fingerprint fp, float[] inputBinRows) {
        int rows = fp.getMatchRows();
        float q = 1f;
        float sum = 0f;
        int zoneHits = 0;
        int zoneSamples = 0;
        int lastZone = 0;
        for (int i = 0; i < rows; ++i) {
            float patternSum = 0f;
            for (int p: fp.getPattern()[i]) {
                patternSum += inputBinRows[p];
            }
            if (patternSum > 0.7f) {
                ++zoneHits;
            }
            ++zoneSamples;
            sum += patternSum;

            int zone = (i + 1) * 8 / rows;
            if (zone != lastZone && zoneSamples > 0) {
                float zoneQ = (float) zoneHits / zoneSamples;
                q *= zoneQ;
                lastZone = zone;
                zoneHits = 0;
                zoneSamples = 0;
            }
        }

        return q;// * sum / rows;
    }
}
