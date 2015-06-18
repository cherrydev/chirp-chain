package org.vectrola.chirpchain.test0;

import java.util.Random;
import java.util.Vector;

/**
 * Created by jlunder on 6/10/15.
 */
public class CodeLibrary {
    public static int NUM_SYMBOLS = 16;

    private SampleSeries[] codeSamples = new SampleSeries[NUM_SYMBOLS];
    private float maxCodeLength = -1f;

    public SampleSeries getCodeForSymbol(int symbol) {
        return codeSamples[symbol];
    }

    public int maxCodeRows() {
        return (int) Math.ceil(getMaxCodeLength() / FrequencyTransformer.ROW_TIME);
    }

    public float getMaxCodeLength() {
        if(maxCodeLength < 0) {
            maxCodeLength = 0f;

            for (int i = 0; i < NUM_SYMBOLS; ++i) {
                maxCodeLength = Math.max(maxCodeLength, (float) codeSamples[i].size() / SampleSeries.SAMPLE_RATE);
            }
        }

        return maxCodeLength;
    }

    private static float[] computeNewEntryCrossMatch(Vector<CodeEntry> entries, CodeEntry e) {
        float[] matchQs = new float[entries.size()];
        for(int i = 0; i < entries.size(); ++i) {
            CodeEntry ea = entries.get(i);
            float q = e.getCodeMatchQ(ea);
            matchQs[i] = q;
            if(q > e.highestQ) {
                e.highestQ = q;
            }
            e.totalQ += q;
        }
        e.totalQ /= entries.size();
        return matchQs;
    }

    private static void computeCrossMatch(Vector<CodeEntry> entries) {
        for(CodeEntry e: entries) {
            e.highestQ = 0;
            e.totalQ = 0;
        }
        for(int j = 0; j < entries.size(); ++j) {
            CodeEntry ea = entries.get(j);
            for(int i = j + 1; i < entries.size(); ++i) {
                CodeEntry eb = entries.get(i);
                float q = ea.getCodeMatchQ(eb);
                if(q > ea.highestQ) {
                    ea.highestQ = q;
                }
                if(q > eb.highestQ) {
                    eb.highestQ = q;
                }
                ea.totalQ += q;
                eb.totalQ += q;
            }
            ea.totalQ /= (entries.size() - 1);
        }
    }

    private static CodeEntry findMaxHighestQEntry(Vector<CodeEntry> entries) {
        CodeEntry hqe = entries.get(0);
        for(CodeEntry e: entries) {
            if(e.highestQ > hqe.highestQ) {
                hqe = e;
            }
        }

        return hqe;
    }

    private static CodeEntry findMaxTotalQEntry(Vector<CodeEntry> entries) {
        CodeEntry hqe = entries.get(0);
        for(CodeEntry e: entries) {
            if(e.totalQ > hqe.totalQ) {
                hqe = e;
            }
        }

        return hqe;
    }

    public static CodeLibrary makeChirpCodes() {
        CodeLibrary l = new CodeLibrary();

        l.codeSamples[0] = makeChirpCode(0.005f, 1000f, 100f, 3, new int[] {0, 2, 20, 28, 14, 1, 1, 27, 23, 17, 0, 2, 37, 19, 13});
        l.codeSamples[1] = makeChirpCode(0.005f, 1000f, 100f, 2, new int[] {0, 2, 5, 0, 19, 1, 2, 32, 10, 15});
        l.codeSamples[2] = makeChirpCode(0.005f, 1000f, 100f, 2, new int[] {1, 1, 18, 16, 10, 0, 1, 22, 0, 16});
        l.codeSamples[3] = makeChirpCode(0.005f, 1000f, 100f, 3, new int[] {1, 1, 5, 10, 11, 0, 1, 13, 33, 12, 0, 1, 3, 19, 11});
        l.codeSamples[4] = makeChirpCode(0.005f, 1000f, 100f, 3, new int[] {1, 1, 14, 23, 16, 1, 2, 22, 20, 10, 1, 1, 24, 19, 14});
        l.codeSamples[5] = makeChirpCode(0.005f, 1000f, 100f, 2, new int[] {0, 1, 23, 15, 12, 1, 2, 4, 5, 14});
        l.codeSamples[6] = makeChirpCode(0.005f, 1000f, 100f, 2, new int[] {1, 1, 0, 18, 18, 1, 1, 13, 17, 15});
        l.codeSamples[7] = makeChirpCode(0.005f, 1000f, 100f, 2, new int[] {0, 2, 23, 46, 17, 1, 1, 7, 14, 15});
        l.codeSamples[8] = makeChirpCode(0.005f, 1000f, 100f, 2, new int[] {0, 1, 3, 3, 17, 0, 1, 7, 17, 14});
        l.codeSamples[9] = makeChirpCode(0.005f, 1000f, 100f, 2, new int[] {0, 2, 9, 11, 10, 0, 1, 9, 33, 19});
        l.codeSamples[10] = makeChirpCode(0.005f, 1000f, 100f, 2, new int[] {0, 1, 37, 24, 12, 1, 2, 47, 23, 19});
        l.codeSamples[11] = makeChirpCode(0.005f, 1000f, 100f, 2, new int[] {0, 2, 6, 14, 17, 1, 1, 8, 8, 18});
        l.codeSamples[12] = makeChirpCode(0.005f, 1000f, 100f, 3, new int[] {1, 1, 10, 12, 13, 1, 2, 22, 18, 16, 0, 1, 23, 17, 13});
        l.codeSamples[13] = makeChirpCode(0.005f, 1000f, 100f, 3, new int[] {0, 1, 20, 25, 15, 0, 2, 15, 32, 12, 1, 2, 24, 42, 17});
        l.codeSamples[14] = makeChirpCode(0.005f, 1000f, 100f, 2, new int[] {0, 2, 24, 47, 16, 0, 1, 8, 1, 19});
        l.codeSamples[15] = makeChirpCode(0.005f, 1000f, 100f, 2, new int[] {1, 1, 12, 7, 12, 1, 1, 5, 8, 16});

        return l;
    }

    public static CodeLibrary generateAndOptimizeChirpCodes() {
        Random r = new Random(4);
        Vector<CodeEntry> entries = new Vector<CodeEntry>();

        /*
        for(int i = 0; i < NUM_SYMBOLS; ++i) {
            entries.add(new CodeEntry(r));
        }
        */
        for(int k = 0; k < 1000; ++k) {
            computeCrossMatch(entries);
            float averageQ = 0f;
            for(CodeEntry ee: entries) {
                averageQ += ee.totalQ / entries.size();
            }

            CodeEntry e = new CodeEntry(r);
            if(e.getSelfMatchQ() < 1.0f) {
                continue;
            }
            int pkZeros = 0;
            float[] peaks = e.fp.getPeaks();
            for(int i = 0; i < peaks.length; ++i) {
                if(peaks[i] == 0f) {
                    ++pkZeros;
                }
            }
            if(pkZeros > (peaks.length * 2 / 10)) {
                continue;
            }

            if(entries.size() >= NUM_SYMBOLS) {
                CodeEntry hqe = findMaxHighestQEntry(entries);
                float[] matchQs = computeNewEntryCrossMatch(entries, e);
                if (e.totalQ >= averageQ || e.highestQ >= hqe.highestQ) {
                    continue;
                }

                // Remove 1/2 of worst match
                System.out.println(String.format("Removing entry: highestQ = %g", hqe.highestQ));
                entries.remove(hqe);
            }
            entries.add(e);
        }

        CodeLibrary l = new CodeLibrary();
        for(int i = 0; i < NUM_SYMBOLS; ++i) {
            CodeEntry e = entries.get(i);
            StringBuilder sb = new StringBuilder();
            for(int j = 0; j < e.params.length; ++j) {
                if(j > 0) {
                    sb.append(", ");
                }
                sb.append(e.params[j]);
            }
            System.out.println(String.format("l.codeSamples[%d] = makeChirpCode(%d, new int[] {%s});", i, e.componentCount, sb.toString()));
            l.codeSamples[i] = entries.get(i).code;
        }

        return l;
    }

    private static class CodeEntry {
        public int componentCount;
        public int[] params;
        public SampleSeries code;
        public PeakListRecognizer.Fingerprint fp;
        float[] inputPeaks;
        public float highestQ;
        public float totalQ;
        public long conflicts;
        public long tests;

        public CodeEntry(Random r) {
            float minFreq = 1000f;
            float maxFreq = 6000f;
            float freqScale = 100f;
            int freqDivs = (int)((maxFreq - minFreq) / freqScale) + 1;
            float timeScale = 0.005f;

            componentCount = 2 + r.nextInt(2);
            params = new int[componentCount * 5];
            for (int i = 0; i < componentCount; ++i) {
                params[i * 5 + 0] = r.nextInt(2); // style
                params[i * 5 + 1] = 1 + r.nextInt(2); // repeat
                params[i * 5 + 2] = r.nextInt(freqDivs * 1 / 2); // freqA
                params[i * 5 + 3] = params[i * 5 + 2] + r.nextInt(freqDivs * 1 / 2); // freqB
                /*
                params[i * 5 + 2] = r.nextInt(FrequencyTransformer.BINS_PER_ROW - 10) + 2; // freqA
                params[i * 5 + 3] = params[i * 5 + 3] + 4 + r.nextInt(4); // freqB
                */
                if (r.nextBoolean()) {
                    int t = params[i * 5 + 2];
                    params[i * 5 + 2] = params[i * 5 + 3];
                    params[i * 5 + 3] = t;
                }
                params[i * 5 + 4] = 10 + r.nextInt(10); // dur
            }
            code = makeChirpCode(timeScale, minFreq, freqScale, componentCount, params);
            fp = new PeakListRecognizer.Fingerprint(code);
            inputPeaks = new float[fp.getMatchRows()];
            PeakListRecognizer.findPeaksInput(fp.getBins(), inputPeaks);
        }

        private float getSelfMatchQ() {
            FrequencyTransformer ft = new FrequencyTransformer();
            ft.addSamples(new SampleSeries(FrequencyTransformer.ROW_SAMPLES / 2));
            ft.addSamples(code);
            float[] offsetInputBins = new float[ft.availableRows() * FrequencyTransformer.BINS_PER_ROW];
            float[] offsetInputPeaks = new float[ft.availableRows()];
            ft.getBinRows(offsetInputBins, ft.availableRows());
            PeakListRecognizer.findPeaksInput(offsetInputBins, offsetInputPeaks);
            return PeakListRecognizer.matchQuality(fp.getPeaks(), inputPeaks) *
                    PeakListRecognizer.matchQuality(fp.getPeaks(), offsetInputPeaks);
        }

        private float getCodeMatchQ(CodeEntry other) {
            return Math.max(slidingMatchQ(fp.getPeaks(), other.inputPeaks),
                    slidingMatchQ(other.fp.getPeaks(), inputPeaks));
        }

        private static float slidingMatchQ(float[] peaksA, float[] peaksB) {
            float bestQ = 0f;
            float[] testBed = new float[peaksB.length];
            int minOffset, maxOffset;
            if (peaksA.length <= peaksB.length) {
                minOffset = -peaksA.length / 2;
                maxOffset = peaksB.length - peaksA.length / 2;
            } else {
                minOffset = (peaksB.length) / 2 - peaksA.length;
                maxOffset = (peaksB.length + 1) / 2;
            }
            for (int i = minOffset; i <= maxOffset; ++i) {
                int srcPos = 0;
                int destPos = i;
                int size = peaksA.length;

                if (destPos < 0) {
                    srcPos = -destPos;
                    size += destPos;
                    destPos = 0;
                }
                if (destPos + size > peaksB.length) {
                    size = peaksB.length - destPos;
                }

                System.arraycopy(peaksA, srcPos, testBed, destPos, size);
                for (int j = 0; j < destPos; ++j) {
                    testBed[j] = 0f;
                }
                for (int j = destPos + size; j < testBed.length; ++j) {
                    testBed[j] = 0f;
                }

                bestQ = Math.max(PeakListRecognizer.matchQuality(testBed, peaksB), bestQ);
            }
            return bestQ;
        }
    }

    private static SampleSeries makeChirpCode(float timeScale, float minFreq, float freqScale, int componentCount, int[] params) {
        SampleSeries code = new SampleSeries();

        for(int i = 0; i < componentCount; ++i) {
            code.append(makeChirpCodeComponent(params[i * 5 + 0], params[i * 5 + 1], params[i * 5 + 2] * freqScale + minFreq, params[i * 5 + 3] * freqScale + minFreq, params[i * 5 + 4] * timeScale));
        }
        return code;
    }

    private static SampleSeries makeChirpCodeComponent(int style, int repeat, float freqA, float freqB, float duration) {
        SampleSeries code = new SampleSeries();

        if(style == 0) {
            code.append(generateLinearWarble(duration, 0.5f * (freqA + freqB), 0.5 * (freqB - freqA), repeat * 0.25f / duration));
        }
        else {
            for (int i = 0; i < repeat; ++i) {
                code.append(generateLinearSweep(duration / repeat, freqA, freqB));
            }
        }

        return code;
    }

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
