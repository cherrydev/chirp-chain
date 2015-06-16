package org.vectrola.chirpchain.test0;

import java.util.Random;

/**
 * Created by jlunder on 6/10/15.
 */
public class CodeLibrary {
    public static int NUM_SYMBOLS = 16;

    private SampleSeries[] codeSamples = new SampleSeries[NUM_SYMBOLS];

    public SampleSeries getCodeForSymbol(int symbol) {
        return codeSamples[symbol];
    }

    public float getMaxCodeLength() {
        float max = 0f;

        for (int i = 0; i < NUM_SYMBOLS; ++i) {
            max = Math.max(max, (float) codeSamples[i].size() / SampleSeries.SAMPLE_RATE);
        }

        return max;
    }

    public static CodeLibrary makeChirpCodes() {
        CodeLibrary l = new CodeLibrary();

        for(int i = 0; i < NUM_SYMBOLS; ++i) {
            l.codeSamples[i] = makeChirpCodeForSymbol(i);
        }
        /*
        Random r = new Random();
        for(int i = 0; i < NUM_SYMBOLS; ++i) {
            int j = r.nextInt(NUM_SYMBOLS);
            SampleSeries t = l.codeSamples[i];
            l.codeSamples[i] = l.codeSamples[j];
            l.codeSamples[j] = t;
        }
        */

        return l;
    }

    private static final float CODE_DURATION = 0.5f;
    private static final float BASE_FREQ = 1500;
    private static final float[] FREQ_SERIES = new float[] {
            /*
            BASE_FREQ * (float)Math.pow(2.0f, 0 * 1.0f / 12.0f),
            BASE_FREQ * (float)Math.pow(2.0f, 1 * 1.0f / 12.0f),
            BASE_FREQ * (float)Math.pow(2.0f, 2 * 1.0f / 12.0f),
            BASE_FREQ * (float)Math.pow(2.0f, 3 * 1.0f / 12.0f),
            BASE_FREQ * (float)Math.pow(2.0f, 4 * 1.0f / 12.0f),
            BASE_FREQ * (float)Math.pow(2.0f, 5 * 1.0f / 12.0f),
            BASE_FREQ * (float)Math.pow(2.0f, 6 * 1.0f / 12.0f),
            BASE_FREQ * (float)Math.pow(2.0f, 7 * 1.0f / 12.0f),
            BASE_FREQ * (float)Math.pow(2.0f, 8 * 1.0f / 12.0f),
            BASE_FREQ * (float)Math.pow(2.0f, 9 * 1.0f / 12.0f),
            */
            BASE_FREQ * (1f + 0f / 4f),
            BASE_FREQ * (1f + 1f / 4f),
            BASE_FREQ * (1f + 2f / 4f),
            BASE_FREQ * (1f + 3f / 4f),
            BASE_FREQ * (1f + 4f / 4f),
            BASE_FREQ * (1f + 5f / 4f),
            BASE_FREQ * (1f + 6f / 4f),
            BASE_FREQ * (1f + 7f / 4f),
            BASE_FREQ * (1f + 8f / 4f),
            BASE_FREQ * (1f + 9f / 4f),
            BASE_FREQ * (1f + 10f / 4f),
    };

    private static SampleSeries makeChirpCodeForSymbol(int symbol) {
        int modeA = (symbol >> 0) & 0x03;
        int modeB = (symbol >> 2) & 0x03;
        float dur = 0.20f;
        SampleSeries code = null;
        float f0;
        float f1;

        switch(modeA) {
            case 0:
                f0 = FREQ_SERIES[modeB + 1];
                f1 = FREQ_SERIES[modeB + 7];
                code = generateLinearSweep(dur, f0, f1);
                break;
            case 1:
                f0 = FREQ_SERIES[modeB + 7];
                f1 = FREQ_SERIES[modeB + 1];
                code = generateLinearSweep(dur, f0, f1);
                break;
            case 2:
                f0 = FREQ_SERIES[modeB * 2 + 2];
                f1 = FREQ_SERIES[modeB * 2 + 0];
                code = generateLinearSweep(dur / 3, f0, f1);
                code.append(generateLinearSweep(dur / 3, f0, f1));
                code.append(generateLinearSweep(dur / 3, f0, f1));
                break;
            case 3:
                f0 = FREQ_SERIES[modeB * 2 + 0];
                f1 = FREQ_SERIES[modeB * 2 + 2];
                code = generateLinearSweep(dur / 3, f0, f1);
                code.append(generateLinearSweep(dur / 3, f0, f1));
                code.append(generateLinearSweep(dur / 3, f0, f1));
                break;
        }

        return code;
    }

    /*
    private static AdsrEnvelope makeStandardAdsrEnvelope(float duration) {
        return new AdsrEnvelope(0.001f, 0.8f, 0.02f, 0.4f, duration - 0.031f, 0.01f);
    }
    */
    private static AdsrEnvelope makeStandardAdsrEnvelope(float duration) {
        return new AdsrEnvelope(0.001f, 0.8f, 0f, 0.8f, duration - 0.011f, 0.01f);
    }

    private static abstract class PhaseGenerator {
        protected double time = 0d;

        protected double getDPhasePerDT(double t) {
            return 0d;
        }

        protected double getDeltaPhase(double t, double deltaT) {
            double dPhasePerDT = getDPhasePerDT(t + 0.5d * deltaT);
            return dPhasePerDT * deltaT;
        }

        public double nextDeltaPhase(double deltaT) {
            double deltaPhase = getDeltaPhase(time, deltaT);
            time += deltaT;
            return deltaPhase;
        }
    }

    private static class SweepPhaseGenerator extends PhaseGenerator {
        private double startFrequency;
        private double frequencySlope;

        public SweepPhaseGenerator(double duration, double startFrequency, double finishFrequency) {
            this.startFrequency = startFrequency;
            this.frequencySlope = (finishFrequency - startFrequency) / duration;
        }

        protected double getDeltaPhase(double t, double deltaT) {
            return Math.PI * 2d * (startFrequency * deltaT + frequencySlope * (t * deltaT));
        }
    }

    private static class WarblePhaseGenerator extends PhaseGenerator {
        private double centerFrequency;
        private double frequencyRange;
        private double modulationFrequency;

        public WarblePhaseGenerator(double centerFrequency, double frequencyRange, double modulationFrequency) {
            this.centerFrequency = centerFrequency;
            this.frequencyRange = frequencyRange;
            this.modulationFrequency = modulationFrequency;
        }

        protected double getDeltaPhase(double t, double deltaT) {
            double t1 = t + deltaT;
            return Math.PI * 2d * (centerFrequency * deltaT) - (frequencyRange / modulationFrequency) *
                    (Math.cos(Math.PI * 2d * modulationFrequency * t1) -
                            Math.cos(Math.PI * 2d * modulationFrequency * t));
        }
    }

    private static SampleSeries generateLinearSweep(float duration, float startFrequency, float finishFrequency) {
        SampleSeries samples = new SampleSeries((int) Math.rint(duration * SampleSeries.SAMPLE_RATE));
        generateLinearToneSweep(samples, 0f, duration,
                new SweepPhaseGenerator(duration, startFrequency, finishFrequency),
                makeStandardAdsrEnvelope(duration));
        return samples;
    }

    private static SampleSeries generateLinearWarble(float duration, double centerFrequency, double frequencyRange,
                                                     double modulationFrequency) {
        SampleSeries samples = new SampleSeries((int) Math.rint(duration * SampleSeries.SAMPLE_RATE));
        generateLinearToneSweep(samples, 0f, duration,
                new WarblePhaseGenerator(centerFrequency, frequencyRange, modulationFrequency),
                makeStandardAdsrEnvelope(duration));
        return samples;
    }

    private static void generateLinearToneSweep(SampleSeries samples, float startTime, float duration,
                                                PhaseGenerator phaseGenerator, AdsrEnvelope envelope) {
        double deltaT = 1d / SampleSeries.SAMPLE_RATE;
        double phase, t;
        int startSample = (int) Math.ceil(startTime * SampleSeries.SAMPLE_RATE);
        int finishSample = (int) Math.floor((startTime + duration) * SampleSeries.SAMPLE_RATE);

        t = (double) startSample / SampleSeries.SAMPLE_RATE - startTime;
        phase = phaseGenerator.nextDeltaPhase(t);

        for (int i = startSample; i < finishSample; ++i) {
            phase %= 2d * Math.PI;
            float s = (float)Math.sin(phase) * envelope.getEnvelopeValue((float) t);
            //float s = phase > Math.PI ? 1 : -1;
            samples.setSample(i, samples.getSample(i) + s);
            phase += phaseGenerator.nextDeltaPhase(deltaT);
            t += deltaT;
        }
    }
}
