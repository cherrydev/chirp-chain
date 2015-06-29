package org.vectrola.chirpchain.test0;

import com.sun.xml.internal.rngom.digested.DDataPattern;
import jdk.nashorn.internal.codegen.CompilerConstants;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by jlunder on 5/16/15.
 */
public class Main {
    private static CodeLibrary library = CodeLibrary.makeChirpCodes();
    private static int[] sequence = new int[] {8, 4, 5, 6, 12, 6, 12, 6, 15, 6, 0, 2, 7, 7, 15, 6, 2, 7, 12, 6, 4, 6, 1, 2};
    private static RecognizerTestCase closeLoudTestCase =
            new RecognizerTestCase("close loud", 1.0f, library, tryReadSamples("chirpSamples/cone-close-loud.wav"),
                    sequence, 5.190f, 10f, 5);
    private static RecognizerTestCase closeSoftTestCase =
            new RecognizerTestCase("close soft", 0.5f, library, tryReadSamples("chirpSamples/cone-close-soft.wav"),
                    sequence, 2.465f, 10f, 5);
    private static RecognizerTestCase farLoudTestCase =
            new RecognizerTestCase("far loud", 0.5f, library, tryReadSamples("chirpSamples/cone-loud.wav"),
                    sequence, 3.420f, 10f, 6);
    private static RecognizerTestCase farSoftTestCase =
            new RecognizerTestCase("far soft", 0.25f, library, tryReadSamples("chirpSamples/cone-soft.wav"),
                    sequence, 2.210f, 10f, 6);
    private static RecognizerTestCase tableTestCase =
            new RecognizerTestCase("table", 1.0f, library, tryReadSamples("chirpSamples/closeRange.wav"),
                    sequence, 4.270f, 10f, 2);
    private static RecognizerTestCase[] testCases = new RecognizerTestCase[] { closeLoudTestCase,
            closeSoftTestCase, farLoudTestCase, farSoftTestCase, tableTestCase };

    private static SampleSeries tryReadSamples(String filename) {
        try {
            return SampleSeries.readFromFile(filename);
        }
        catch(IOException e) {
            return null;
        }
    }

    public static void main(String[] args) throws IOException {
        /*
        for (int i = 0; i < CodeLibrary.NUM_SYMBOLS; ++i) {
            library.getCodeForSymbol(i).writeToFile(String.format("chirp%03d.wav", i));
        }
        */


        //SampleSeries challenge = SampleSeries.readFromFile("helloworld-challenge.wav");

        System.out.print("Encoding string\n");

        SampleSeries hw = encodeString(library, "Hello world!");
        hw.writeToFile("helloworld.wav");

        // 8 4 5 6 / 12 6 12 6 / 15 6 0 2 / 7 7 15 6 / 2 7 12 6 / 4 6 1 2
        RecognizerTestCase cleanTestCase = new RecognizerTestCase("clean", 0f, library, hw, sequence, 0.000f, 10f, 1);


        System.out.println("Running tests!");

        TimeCorrelatingRecognizer.Parameters autoTunedParams = autoTune();
        /*
        TimeCorrelatingRecognizer.Parameters autoTunedParams = new TimeCorrelatingRecognizer.Parameters();
        autoTunedParams.rowThreshold = 0.201643f;
        autoTunedParams.binMinimum = -0.251239f;
        autoTunedParams.zoneThreshold = 0.00811279f;
        autoTunedParams.zoneCount = 12;
        autoTunedParams.zoneCountThreshold = 0.630518f;
        autoTunedParams.matchBaseThreshold = 0.191583f;
        autoTunedParams.matchBestToSecondBestThreshold = 0.127950f;
        */

        TimeCorrelatingRecognizer.Parameters handTunedParams = new TimeCorrelatingRecognizer.Parameters();
        handTunedParams.rowThreshold = 0.326846f;
        handTunedParams.binMinimum = -0.130795f;
        handTunedParams.zoneThreshold = 0.171726f;
        handTunedParams.zoneCount = 11;
        handTunedParams.zoneCountThreshold = 0.146021f;
        handTunedParams.matchBaseThreshold = 0.244680f;
        handTunedParams.matchBestToSecondBestThreshold = 0.126661f;

        /*
        New best fitness (i=848): 184.000
          p.rowThreshold = 0.326846f;
          p.binMinimum = -0.130795f;
          p.zoneThreshold = 0.171726f;
          p.zoneCount = 11;
          p.zoneCountThreshold = 0.146021f;
          p.matchBaseThreshold = 0.244680f;
          p.matchBestToSecondBestThreshold = 0.126661f;

        // 179?
        autoTunedParams.rowThreshold = 0.201643f;
        autoTunedParams.binMinimum = -0.251239f;
        autoTunedParams.zoneThreshold = 0.00811279f;
        autoTunedParams.zoneCount = 12;
        autoTunedParams.zoneCountThreshold = 0.630518f;
        autoTunedParams.matchBaseThreshold = 0.191583f;
        autoTunedParams.matchBestToSecondBestThreshold = 0.127950f;
        */

        /*174.500
        p.rowThreshold = 0.576793f;
        p.binMinimum = 0.00156026f;
        p.zoneThreshold = 0.242255f;
        p.zoneCount = 6f;
        p.zoneCountThreshold = 0.0564383f;
        p.matchBaseThreshold = 0.441303f;
        p.matchBestToSecondBestThreshold = 0.273035f;
        */
        /*

        handTunedParams.rowThreshold = 0.613164f;
        handTunedParams.binMinimum = 0.0247785f;
        handTunedParams.zoneThreshold = 0.0670108f;
        handTunedParams.zoneCount = 10;
        handTunedParams.zoneCountThreshold = 0.518978f;
        handTunedParams.matchBaseThreshold = 0.314252f;
        handTunedParams.matchBestToSecondBestThreshold = 0.0857519f;

        handTunedParams.rowThreshold = 0.7f;
        handTunedParams.binMinimum = -0.5f;
        handTunedParams.zoneThreshold = 0.5f;
        handTunedParams.zoneCount = 8;
        handTunedParams.zoneCountThreshold = 0.875f;
        handTunedParams.matchBaseThreshold = 0.5f;
        handTunedParams.matchBestToSecondBestThreshold = 0.01f;
        */

        for(RecognizerTestCase testCase: testCases) {
            RecognizerTestCase.Results autoTunedResults = testCase.testRecognizer(new TimeCorrelatingRecognizer(library,
                    testCase.getFrequencyTransformer(), autoTunedParams));
            System.out.println(String.format(
                    "Testing %20s (auto): %3d recognized, %3d unrecognized, %3d misrecognized, %3d spurious (score %g)",
                    testCase.getName(), autoTunedResults.getRecognizedCount(), autoTunedResults.getUnrecognizedCount(),
                    autoTunedResults.getMisrecognizedCount(), autoTunedResults.getSpuriousCount(),
                    scoreTestResults(autoTunedResults) * testCase.getWeight()));

            RecognizerTestCase.Results handTunedResults = testCase.testRecognizer(new TimeCorrelatingRecognizer(library,
                    testCase.getFrequencyTransformer(), handTunedParams));
            System.out.println(String.format(
                    "Testing %20s (hand): %3d recognized, %3d unrecognized, %3d misrecognized, %3d spurious (score %g)",
                    testCase.getName(), handTunedResults.getRecognizedCount(), handTunedResults.getUnrecognizedCount(),
                    handTunedResults.getMisrecognizedCount(), handTunedResults.getSpuriousCount(),
                    scoreTestResults(handTunedResults) * testCase.getWeight()));
        }

        //printFingerprints(new TimeCorrelatingRecognizer(l));
        //printTestFrequencyHeatMap(closeLoudTestCase, new TimeCorrelatingRecognizer(l));
        //printFrequencyHeatMap(cleanTestCase.getFrequencyTransformer(), true);
        //cleanTestCase.getFrequencyTransformer().reset();
        //printQualityHeatMap(new TimeCorrelatingRecognizer(library, cleanTestCase.getFrequencyTransformer()));
        //cleanTestCase.getFrequencyTransformer().reset();

        System.out.println("Done.\n");
    }

    private static TimeCorrelatingRecognizer.Parameters autoTune() {
        Random r = new Random();
        TimeCorrelatingRecognizer.Parameters bestParams = TimeCorrelatingRecognizer.Parameters.makeRandom(r);
        float bestFitness = evaluate(bestParams);
        int maxJ = 4;
        int maxI = 250;
        int iter = maxJ * maxI;

        for(int j = 0; j < maxJ; ++j) {
            TimeCorrelatingRecognizer.Parameters annealParams = bestParams;
            float annealFitness = bestFitness;
            for (int i = 0; i < maxI; ++i) {
                float temp = ((float)iter / (maxI * maxJ)) * 50f;
                TimeCorrelatingRecognizer.Parameters params = annealParams.mutate(r, (float) (1f - i / maxI));
                float fitness = evaluate(params);
                if(fitness > bestFitness) {
                    bestParams = params;
                    bestFitness = fitness;
                    annealParams = params;
                    annealFitness = fitness;
                    System.out.println(String.format("New best fitness (i=%d): %g", iter, bestFitness));
                    System.out.println(String.format("  p.rowThreshold = %gf;", bestParams.rowThreshold));
                    System.out.println(String.format("  p.binMinimum = %gf;", bestParams.binMinimum));
                    System.out.println(String.format("  p.zoneThreshold = %gf;", bestParams.zoneThreshold));
                    System.out.println(String.format("  p.zoneCount = %d;", bestParams.zoneCount));
                    System.out.println(String.format("  p.zoneCountThreshold = %gf;", bestParams.zoneCountThreshold));
                    System.out.println(String.format("  p.matchBaseThreshold = %gf;", bestParams.matchBaseThreshold));
                    System.out.println(String.format("  p.matchBestToSecondBestThreshold = %gf;",
                            bestParams.matchBestToSecondBestThreshold));
                }
                else if(fitness + temp > annealFitness) {
                    annealParams = params;
                    annealFitness = fitness;
                }
                --iter;
            }
        }

        return bestParams;
    }

    private static ExecutorService executorService = Executors.newWorkStealingPool();

    private static float evaluate(TimeCorrelatingRecognizer.Parameters params) {
        Vector<Callable<Float>> callables = new Vector<Callable<Float>>();

        for (RecognizerTestCase testCase : testCases) {
            callables.add(new Callable<Float>() {
                @Override
                public Float call() throws Exception {
                    return testCase.getWeight() * scoreTestResults(testCase.testRecognizer(
                            new TimeCorrelatingRecognizer(library, testCase.getFrequencyTransformer(), params)));
                }
            });
        }

        try {
            float totalScore = 0f;
            List<Future<Float>> futures = executorService.invokeAll(callables);
            for (Future<Float> f : futures) {
                totalScore += f.get().floatValue();
            }
            return totalScore;
        } catch (Exception e) {
            return Float.NEGATIVE_INFINITY;
        }
    }

    private static float scoreTestResults(RecognizerTestCase.Results results) {
        return (1f * results.getRecognizedCount()) +
                (-1f * results.getUnrecognizedCount()) +
                (-1f * results.getMisrecognizedCount()) +
                (-2f * results.getSpuriousCount());
    }

    private static void printFingerprints(CodeRecognizer rec) {
        LiveFrequencyTransformer ft = new LiveFrequencyTransformer(false, false);
        float[] heatMap = new float[rec.getLibrary().getMaxCodeRows() * FrequencyTransformer.BINS_PER_ROW];
        for(int i = 0; i < CodeLibrary.NUM_SYMBOLS; ++i) {
            CodeLibrary.Fingerprint fp = rec.getLibrary().getFingerprintForSymbol(i);
            int rows;
            ft.reset();
            ft.addSamples(fp.code);
            rows = ft.getAvailableRows();
            ft.getBinRows(heatMap, rows);
            System.out.println(String.format("Symbol %d:", i));
            printHeatMap(heatMap, rows, FrequencyTransformer.BINS_PER_ROW, false, true, true, new RowAnnotator() {
                @Override
                public int annotateChar(int row, int col, float[] bins, float value) {
                    for (int bin : fp.getPattern()[row]) {
                        if (bin == row * FrequencyTransformer.BINS_PER_ROW + col) {
                            return (int) '/';
                        }
                    }
                    return -1;
                }
            });
            System.out.println();
        }
    }

    private static void printTestFrequencyHeatMap(RecognizerTestCase testCase, TimeCorrelatingRecognizer rec) {
        int numRows = testCase.getTestSeries().size() / FrequencyTransformer.ROW_SAMPLES;
        float[] heatMap = new float[numRows * FrequencyTransformer.BINS_PER_ROW];
        for(RecognizerTestCase.ExpectedSymbol es: testCase.getExpectedSymbols()) {
            int offset = (int)Math.rint(es.getTime() / FrequencyTransformer.ROW_TIME) *
                    FrequencyTransformer.BINS_PER_ROW;
            CodeLibrary.Fingerprint fp = rec.getLibrary().getFingerprintForSymbol(es.getSymbol());
            for(int[] patternRow: fp.getPattern()) {
                for(int binNumber: patternRow) {
                    if(offset + binNumber < heatMap.length) {
                        heatMap[offset + binNumber] = 1f;
                    }
                }
            }
        }
        printHeatMap(heatMap, numRows, FrequencyTransformer.BINS_PER_ROW, true, true, true, new RowAnnotator() {
            public String annotateColBefore(int col, float[] bins) {
                if(col < 0) {
                    return "     ";
                }
                else {
                    return String.format("%5.0f",
                            FrequencyTransformer.MIN_FREQUENCY + col * FrequencyTransformer.BIN_BANDWIDTH);
                }
            }
        });
    }

    private static void printFrequencyHeatMap(FrequencyTransformer ft, boolean adaptiveNoiseReject) {
        int numRows = ft.getQueuedRows();
        float[] heatMap = new float[numRows * FrequencyTransformer.BINS_PER_ROW];
        int rowsTranscribed = 0;
        while(rowsTranscribed < numRows) {
            int rows = Math.min(numRows - rowsTranscribed, ft.getAvailableRows());
            ft.getBinRows(heatMap, rowsTranscribed * FrequencyTransformer.BINS_PER_ROW, rows);
            ft.discardRows(rows);
            rowsTranscribed += rows;
        }
        printHeatMap(heatMap, numRows, FrequencyTransformer.BINS_PER_ROW, true, true, true, new RowAnnotator() {
            public String annotateColBefore(int col, float[] bins) {
                if(col < 0) {
                    return "     ";
                }
                else {
                    return String.format("%5.0f",
                            FrequencyTransformer.MIN_FREQUENCY + col * FrequencyTransformer.BIN_BANDWIDTH);
                }
            }
        });
    }

    private static void printQualityHeatMap(CodeRecognizer rec) {
        CodeLibrary l = rec.getLibrary();
        int numRows = rec.getFrequencyTransformer().getQueuedRows() - rec.getLibrary().getMaxCodeRows();
        float[] heatMap = new float[numRows * CodeLibrary.NUM_SYMBOLS];
        float mx = heatMap[0];
        float mn = heatMap[0];
        for(float f: heatMap) {
            mx = Math.max(mx, f);
            mn = Math.min(mn, f);
        }
        for(int i = 0; i < heatMap.length; ++i) {
            heatMap[i] = (heatMap[i] - mn) / (mx - mn);
        }
        rec.matchQualityGraph(heatMap);
        printHeatMap(heatMap, numRows, CodeLibrary.NUM_SYMBOLS, true, true, true, new RowAnnotator() {
            public String annotateColBefore(int col, float[] bins) {
                if(col < 0) {
                    return "     ";
                }
                else {
                    return String.format("%5d", col);
                }
            }
        });
    }

    private static class RowAnnotator {
        public String annotateRowBefore(int row, float[] bins) {
            return "";
        }
        public String annotateRowAfter(int row, float[] bins) {
            return "";
        }
        public String annotateColBefore(int col, float[] bins) {
            return "";
        }
        public String annotateColAfter(int col, float[] bins) {
            return "";
        }
        public int annotateChar(int row, int col, float[] bins, float value) {
            return -1;
        }
    }

    private static void printHeatMap(float[] data, int rows, int cols, boolean transpose, boolean topFrame, boolean bottomFrame, RowAnnotator annotator) {
        char[] graphChars = new char[]{' ', '.', ':', 'i', 'u', '*', '@', 'X'};

        if (rows * cols > data.length) {
            throw new Error("Data too small!");
        }

        if (annotator == null) {
            annotator = new RowAnnotator();
        }

        if (transpose) {
            if (topFrame) {
                System.out.print(String.format("%s+", annotator.annotateColBefore(-1, data)));
                for (int i = 0; i < rows; ++i) {
                    System.out.print("-");
                }
                System.out.println(String.format("+%s", annotator.annotateColAfter(-1, data)));
            }

            for (int i = cols - 1; i >= 0; --i) {
                System.out.print(String.format("%s|", annotator.annotateColBefore(i, data)));
                for (int j = 0; j < rows; ++j) {
                    float val = data[j * cols + i];
                    int c = annotator.annotateChar(j, i, data, val);
                    if (c < 0) {
                        c = graphChars[Math.max(0, Math.min(graphChars.length - 1, (int) Math.floor(graphChars.length * val)))];
                    }
                    System.out.print((char) c);
                }
                System.out.println(String.format("|%s", annotator.annotateColAfter(i, data)));
            }

            if (bottomFrame) {
                System.out.print(String.format("%s+", annotator.annotateColBefore(-1, data)));
                for (int i = 0; i < rows; ++i) {
                    System.out.print("-");
                }
                System.out.println(String.format("+%s", annotator.annotateColAfter(-1, data)));
            }
        } else {
            if (topFrame) {
                System.out.print(String.format("%s+", annotator.annotateRowBefore(-1, data)));
                for (int i = 0; i < cols; ++i) {
                    System.out.print("-");
                }
                System.out.println(String.format("+%s", annotator.annotateRowAfter(-1, data)));
            }

            for (int j = 0; j < rows; ++j) {
                System.out.print(String.format("%s|", annotator.annotateRowBefore(j, data)));
                for (int i = 0; i < cols; ++i) {
                    float val = data[j * cols + i];
                    int c = annotator.annotateChar(j, i, data, val);
                    if (c < 0) {
                        c = graphChars[Math.max(0, Math.min(graphChars.length - 1, (int) Math.floor(graphChars.length * val)))];
                    }
                    System.out.print((char) c);
                }
                System.out.println(String.format("|%s", annotator.annotateRowAfter(j, data)));
            }

            if (bottomFrame) {
                System.out.print(String.format("%s+", annotator.annotateRowBefore(-1, data)));
                for (int i = 0; i < cols; ++i) {
                    System.out.print("-");
                }
                System.out.println(String.format("+%s", annotator.annotateRowAfter(-1, data)));
            }
        }
    }

    public static SampleSeries encodeString(CodeLibrary l, String s) {
        int mcs = (int)Math.ceil(l.getMaxCodeLength() * SampleSeries.SAMPLE_RATE);
        SampleSeries msg = new SampleSeries();
        SampleSeries pad = new SampleSeries(FrequencyTransformer.WAVELET_WINDOW_SAMPLES);

        System.out.print("Encoding string '");
        System.out.print(s);
        System.out.print("': ");
        int j = 0;
        float t = 0;
        for (int i = 0; i < s.length(); ++i) {
            int c = (int) s.charAt(i);
            SampleSeries code;
            int sym = (int)c;
            if(CodeLibrary.NUM_SYMBOLS == 16) {
                sym = (c & 0xF);
                code = l.getCodeForSymbol(sym);
                msg.append(l.getCodeForSymbol(sym));
                System.out.print(String.format("%d (%.3f) ", sym, t));
                t += code.size() / SampleSeries.SAMPLE_RATE;
                sym = ((c >> 4) & 0xF);
            }
            code = l.getCodeForSymbol(sym);
            msg.append(l.getCodeForSymbol(sym));
            System.out.print(String.format("%d (%.3f) ", sym, t));
            t += code.size() / SampleSeries.SAMPLE_RATE;
            msg.append(pad);
            t += pad.size() / SampleSeries.SAMPLE_RATE;
        }
        for(int i = 0; i < 10; ++i) {
            msg.append(pad);
        }

        System.out.println("done!");

        return msg;
    }
}
