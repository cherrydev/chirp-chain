package org.vectrola.chirpchain.test0;

import java.util.Random;

/**
 * Created by jlunder on 6/10/15.
 */
public class CodeLibrary {
    public static int NUM_SYMBOLS = 256;

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
            BASE_FREQ * (float)Math.pow(2.0f, 0 * 1.0f / 12.0f),
            BASE_FREQ * (float)Math.pow(2.0f, 3 * 1.0f / 12.0f),
            BASE_FREQ * (float)Math.pow(2.0f, 6 * 1.0f / 12.0f),
            BASE_FREQ * (float)Math.pow(2.0f, 9 * 1.0f / 12.0f),
    };

    private static SampleSeries makeChirpCodeForSymbol(int symbol) {
        SampleSeries code = new SampleSeries((int)Math.ceil(0.5f * SampleSeries.SAMPLE_RATE));
        int modeA = (symbol >> 0) & 0x07;
        int modeB = (symbol >> 4) & 0x07;
        float durationA = 0.15f;
        float durationB = 0.25f;

        makeChirpComponent(code, 0.0f, durationA, modeA, ((symbol & 0x08) == 0) ? FREQ_SERIES[0] : FREQ_SERIES[2]);
        makeChirpComponent(code, durationA, durationB, modeB, ((symbol & 0x80) == 0) ? FREQ_SERIES[1] : FREQ_SERIES[3]);
        return code;
    }

    private static void makeChirpComponent(SampleSeries code, float start, float duration, int mode, float frequency)
    {
        switch(mode) {
            case 0:
                generateLinearSweep(code, start, duration, frequency * 0.75f, frequency * 1.25f, makeStandardAdsrEnvelope(duration));
                break;
            case 1:
                generateLinearSweep(code, start + duration * 0.00f, duration * 0.25f, frequency * 0.75f, frequency * 1.25f, makeStandardAdsrEnvelope(duration));
                generateLinearSweep(code, start + duration * 0.25f, duration * 0.25f, frequency * 0.75f, frequency * 1.25f, makeStandardAdsrEnvelope(duration));
                generateLinearSweep(code, start + duration * 0.50f, duration * 0.25f, frequency * 0.75f, frequency * 1.25f, makeStandardAdsrEnvelope(duration));
                generateLinearSweep(code, start + duration * 0.75f, duration * 0.25f, frequency * 0.75f, frequency * 1.25f, makeStandardAdsrEnvelope(duration));
                //generateLinearWarble(code, start, duration, frequency, frequency * 0.25f, 0.5f / duration, makeStandardAdsrEnvelope(duration));
                break;
            case 2:
                generateLinearWarble(code, start, duration, frequency, frequency * 0.25f, 1f / duration, makeStandardAdsrEnvelope(duration));
                break;
            case 3:
                generateLinearSweep(code, start, duration, frequency * 1.25f, frequency * 0.75f, makeStandardAdsrEnvelope(duration));
                break;
            case 4:
                generateLinearWarble(code, start, duration, frequency, frequency * -0.25f, 0.5f / duration, makeStandardAdsrEnvelope(duration));
                break;
            case 5:
                generateLinearWarble(code, start, duration, frequency, frequency * -0.25f, 1f / duration, makeStandardAdsrEnvelope(duration));
                break;
            case 6:
                generateLinearSweep(code, start + duration * 0.0f, duration * 0.5f, frequency * 0.75f, frequency * 1.25f, makeStandardAdsrEnvelope(duration));
                generateLinearSweep(code, start + duration * 0.5f, duration * 0.5f, frequency * 0.75f, frequency * 1.25f, makeStandardAdsrEnvelope(duration));
                break;
            case 7:
                generateLinearSweep(code, start + duration * 0.0f, duration * 0.5f, frequency * 1.25f, frequency * 0.75f, makeStandardAdsrEnvelope(duration));
                generateLinearSweep(code, start + duration * 0.5f, duration * 0.5f, frequency * 1.25f, frequency * 0.75f, makeStandardAdsrEnvelope(duration));
                break;
        }
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

    private static void generateLinearSweep(SampleSeries samples, float startTime, float duration,
                                            float startFrequency, float finishFrequency, AdsrEnvelope envelope) {
        generateLinearToneSweep(samples, startTime, duration,
                new SweepPhaseGenerator(duration, startFrequency, finishFrequency), envelope);
    }

    private static void generateLinearWarble(SampleSeries samples, float startTime, float duration,
                                             double centerFrequency, double frequencyRange, double modulationFrequency,
                                             AdsrEnvelope envelope) {
        generateLinearToneSweep(samples, startTime, duration,
                new WarblePhaseGenerator(centerFrequency, frequencyRange, modulationFrequency), envelope);
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
