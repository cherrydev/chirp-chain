package org.vectrola.chirpchain.test0;

/**
 * Created by jlunder on 6/16/15.
 */
public class TimeCorrelatingRecognizer extends CodeRecognizer {
    public float rowThreshold = 0.7f;
    public float binMinimum = -0.5f;
    public float zoneThreshold = 0.5f;
    public int zoneCount = 8;
    public int zoneCountThreshold = 7;

    TimeCorrelatingRecognizer(CodeLibrary library, FrequencyTransformer frequencyTransformer) {
        super(library, frequencyTransformer, 0.5f, 0.01f);
    }

    public float matchQuality(CodeRecognizer.Fingerprint fp, float[] inputBinRows) {
        int rows = fp.getMatchRows();
        float q = 1f;
        int zoneHits = 0;
        int zoneSamples = 0;
        int lastZone = 0;
        int hits = 0;
        for (int i = 0; i < rows; ++i) {
            float patternSum = 0f;
            for (int p: fp.getPattern()[i]) {
                patternSum += Math.max(inputBinRows[p], binMinimum);
            }
            if (patternSum > rowThreshold) {
                ++zoneHits;
            }
            ++zoneSamples;

            int zone = (i + 1) * zoneCount / rows;
            if (zone != lastZone && zoneSamples > 0) {
                float zoneQ = (float) zoneHits / zoneSamples;
                q *= zoneQ;
                if(zoneQ >= zoneThreshold) {
                    ++hits;
                }
                lastZone = zone;
                zoneHits = 0;
                zoneSamples = 0;
            }
        }

        return hits >= zoneCountThreshold ? q : 0f;
    }
}
