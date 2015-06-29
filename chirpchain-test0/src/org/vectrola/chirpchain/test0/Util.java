package org.vectrola.chirpchain.test0;

/**
 * Created by jlunder on 6/29/15.
 */
public class Util {
    public static float max(float[] values) {
        float maxValue = values[0];
        for (int i = 0; i < values.length; ++i) {
            maxValue = Math.max(maxValue, values[i]);
        }
        return maxValue;
    }

    public static float mean(float[] vals) {
        float sum = 0f;

        for (int i = 0; i < vals.length; ++i) {
            sum += vals[i];
        }

        return sum / vals.length;
    }
}
