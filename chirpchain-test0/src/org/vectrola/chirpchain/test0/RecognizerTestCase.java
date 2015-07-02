package org.vectrola.chirpchain.test0;

/**
 * Created by jlunder on 6/28/15.
 */
public class RecognizerTestCase {
    public class Results {
        private int recognizedCount;
        private int unrecognizedCount;
        private int misrecognizedCount;
        private int spuriousCount;

        public int getRecognizedCount() {
            return recognizedCount;
        }

        public int getUnrecognizedCount() {
            return unrecognizedCount;
        }

        public int getMisrecognizedCount() {
            return misrecognizedCount;
        }

        public int getSpuriousCount() {
            return spuriousCount;
        }

        public Results(int recognizedCount, int unrecognizedCount, int misrecognizedCount, int spuriousCount) {
            this.recognizedCount = recognizedCount;
            this.unrecognizedCount = unrecognizedCount;
            this.misrecognizedCount = misrecognizedCount;
            this.spuriousCount = spuriousCount;
        }
    }

    public static class ExpectedSymbol {
        private float time;
        private int symbol;

        public float getTime() {
            return time;
        }

        public int getSymbol() {
            return symbol;
        }

        public ExpectedSymbol(float time, int symbol) {
            this.time = time;
            this.symbol = symbol;
        }

        public static ExpectedSymbol[] makePattern(CodeLibrary l, int[] sequence, float startTime, float repeatPeriod, int repeatCount) {
            ExpectedSymbol[] pattern = new ExpectedSymbol[sequence.length * repeatCount];

            if (l.getMaxCodeRows() * FrequencyTransformer.ROW_TIME <= RECOGNIZE_WINDOW * 2f) {
                throw new Error("Code shorter than recognition window -- tests will be screwy!");
            }

            for (int j = 0; j < repeatCount; ++j) {
                float time = startTime + repeatPeriod * j;
                int offset = sequence.length * j;
                for (int i = 0; i < sequence.length; ++i) {
                    float symbolTime = l.getCodeForSymbol(sequence[i]).size() / SampleSeries.SAMPLE_RATE;
                    pattern[offset + i] = new ExpectedSymbol(time, sequence[i]);
                    time += symbolTime;
                    if (i % 2 == 1) {
                        time += FrequencyTransformer.WAVELET_WINDOW_SAMPLES / SampleSeries.SAMPLE_RATE;
                    }
                }
            }

            return pattern;
        }
    }

    private static final float RECOGNIZE_WINDOW = 0.09f;

    private boolean log = false;
    private String name;
    private float weight;
    private FrequencyTransformer frequencyTransformer;
    private SampleSeries testSeries;
    private ExpectedSymbol[] expectedSymbols;

    public void enableLogging() {
        log = true;
    }

    public void disableLogging() {
        log = false;
    }

    public float getWeight() {
        return weight;
    }

    public String getName() {
        return name;
    }

    public SampleSeries getTestSeries() {
        return testSeries;
    }

    public FrequencyTransformer getFrequencyTransformer() {
        return frequencyTransformer;
    }

    public ExpectedSymbol[] getExpectedSymbols() {
        return expectedSymbols;
    }

    public RecognizerTestCase(String name, float weight, CodeLibrary l, SampleSeries testSeries, int[] sequence, float startTime, float repeatPeriod, int repeatCount) {
        this.weight = weight;
        this.name = name;
        this.testSeries = testSeries;
        this.frequencyTransformer = new MockFrequencyTransformer(testSeries);
        this.expectedSymbols = ExpectedSymbol.makePattern(l, sequence, startTime, repeatPeriod, repeatCount);
    }

    public Results testRecognizer(CodeRecognizer recognizer) {
        int recognizedCount = 0;
        int unrecognizedCount = 0;
        int misrecognizedCount = 0;
        int spuriousCount = 0;

        int i = 0;
        boolean misrecognizedThis = false;

        if(recognizer.getFrequencyTransformer() != frequencyTransformer) {
            throw new IllegalArgumentException("Recognizer must be using the test case's FrequencyTransformer");
        }

        if (log) {
            System.out.print("Expecting:");
            for (ExpectedSymbol es : expectedSymbols) {
                System.out.print(String.format(" %d (%.3f)", es.symbol, es.time));
            }
            System.out.println();
            System.out.print("Found:");
        }
        while (recognizer.hasNextSymbol()) {
            int sym = recognizer.nextSymbol();
            float t = recognizer.getLastSymbolTime();
            if (log && sym >= 0) {
                System.out.print(String.format(" %d (%.3f)", sym, t));
            }
            while (i < expectedSymbols.length && t > expectedSymbols[i].time + RECOGNIZE_WINDOW) {
                if(misrecognizedThis) {
                    ++misrecognizedCount;
                }
                else {
                    ++unrecognizedCount;
                }
                ++i;
                misrecognizedThis = false;
            }
            if (sym >= 0) {
                if (i >= expectedSymbols.length) {
                    ++spuriousCount;
                } else if (t < expectedSymbols[i].time - RECOGNIZE_WINDOW) {
                    ++spuriousCount;
                } else if (t >= expectedSymbols[i].time - RECOGNIZE_WINDOW && t <= expectedSymbols[i].time + RECOGNIZE_WINDOW) {
                    if (sym == expectedSymbols[i].symbol) {
                        ++recognizedCount;
                        ++i;
                        misrecognizedThis = false;
                    } else if (sym >= 0) {
                        misrecognizedThis = true;
                    }
                }
            }
        }
        if (log) {
            System.out.println();
        }
        unrecognizedCount += expectedSymbols.length - i;
        frequencyTransformer.reset();

        return new Results(recognizedCount, unrecognizedCount, misrecognizedCount, spuriousCount);
    }
}
