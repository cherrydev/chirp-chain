package org.vectrola.chirpchain.test0;

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

        for(int a = 0; a < 2; ++a) {
            float freqMin = 2000f + a * 1000f;
            float freqRange = 500f;
            float freqMid = freqMin + 1500f;
            float freqMax = freqMin + 3000f;

            SampleSeries sample;

            sample = new SampleSeries((int)Math.rint(SampleSeries.SAMPLE_RATE * 0.25f));
            generateLinearSweep(sample, 0f, 0.1f, freqMin, freqMin, makeStandardAdsrEnvelope(0.1f));
            generateLinearSweep(sample, 0.125f, 0.1f, freqMid, freqMax, makeStandardAdsrEnvelope(0.1f));
            l.codeSamples[a * 8 + 0] = sample;

            sample = new SampleSeries((int)Math.rint(SampleSeries.SAMPLE_RATE * 0.25f));
            generateLinearWarble(sample, 0f, 0.225f, freqMid, freqRange, 10f, makeStandardAdsrEnvelope(0.225f));
            l.codeSamples[a * 8 + 1] = sample;

            sample = new SampleSeries((int)Math.rint(SampleSeries.SAMPLE_RATE * 0.25f));
            generateLinearSweep(sample, 0f, 0.075f, freqMax, freqMin, makeStandardAdsrEnvelope(0.075f));
            generateLinearSweep(sample, 0.075f, 0.075f, freqMax, freqMin, makeStandardAdsrEnvelope(0.075f));
            generateLinearSweep(sample, 0.15f, 0.075f, freqMax, freqMin, makeStandardAdsrEnvelope(0.075f));
            l.codeSamples[a * 8 + 2] = sample;

            sample = new SampleSeries((int)Math.rint(SampleSeries.SAMPLE_RATE * 0.25f));
            generateLinearWarble(sample, 0f, 0.225f, freqMid, freqRange, 0.67f / 0.225f, makeStandardAdsrEnvelope(0.225f));
            l.codeSamples[a * 8 + 3] = sample;

            sample = new SampleSeries((int)Math.rint(SampleSeries.SAMPLE_RATE * 0.25f));
            generateLinearSweep(sample, 0f, 0.1f, freqMid, freqMid, makeStandardAdsrEnvelope(0.1f));
            generateLinearSweep(sample, 0.125f, 0.1f, freqMax, freqMin, makeStandardAdsrEnvelope(0.1f));
            l.codeSamples[a * 8 + 4] = sample;

            sample = new SampleSeries((int)Math.rint(SampleSeries.SAMPLE_RATE * 0.25f));
            generateLinearSweep(sample, 0f, 0.1f, freqMid, freqMax, makeStandardAdsrEnvelope(0.1f));
            generateLinearSweep(sample, 0.125f, 0.1f, freqMid, freqMax, makeStandardAdsrEnvelope(0.1f));
            l.codeSamples[a * 8 + 5] = sample;

            sample = new SampleSeries((int)Math.rint(SampleSeries.SAMPLE_RATE * 0.25f));
            generateLinearSweep(sample, 0f, 0.1f, freqMax, freqMax, makeStandardAdsrEnvelope(0.1f));
            generateLinearSweep(sample, 0.125f, 0.1f, freqMid, freqMax, makeStandardAdsrEnvelope(0.1f));
            l.codeSamples[a * 8 + 6] = sample;

            sample = new SampleSeries((int)Math.rint(SampleSeries.SAMPLE_RATE * 0.25f));
            generateLinearWarble(sample, 0f, 0.225f, freqMax - freqRange, -freqRange, 1f / 0.225f, makeStandardAdsrEnvelope(0.225f));
            l.codeSamples[a * 8 + 7] = sample;
        }

        return l;
    }

    public static AdsrEnvelope makeStandardAdsrEnvelope(float duration) {
        return new AdsrEnvelope(0.001f, 0.8f, 0.02f, 0.4f, duration - 0.031f, 0.01f);
    }

    private static abstract class PhaseGenerator {
        protected double time;

        public PhaseGenerator() {
            this.time = 0d;
        }

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
