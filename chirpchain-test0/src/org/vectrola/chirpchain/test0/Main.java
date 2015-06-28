package org.vectrola.chirpchain.test0;

import java.io.IOException;
import java.security.InvalidParameterException;

/**
 * Created by jlunder on 5/16/15.
 */
public class Main {
    public static void main(String[] args) throws IOException {
        System.out.print("Generating code library\n");

        CodeLibrary l = CodeLibrary.makeChirpCodes();
        for (int i = 0; i < CodeLibrary.NUM_SYMBOLS; ++i) {
            //l.getCodeForSymbol(i).writeToFile(String.format("chirp%03d.wav", i));
        }


        SampleSeries challenge = SampleSeries.readFromFile("helloworld-challenge.wav");

        System.out.print("Encoding string\n");

        SampleSeries hw = encodeString(l, "Hello world!");
        hw.writeToFile("helloworld.wav");

        SampleSeries closeLoudChallenge = SampleSeries.readFromFile("chirpSamples/cone-close-loud.wav");
        SampleSeries easyChallenge = SampleSeries.readFromFile("chirpSamples/closeRange.wav");

        FrequencyTransformer xf = new FrequencyTransformer(true, false);
        // warm up adaptive noise rejection
        xf.warmup(closeLoudChallenge);
        xf.addSamples(closeLoudChallenge);
        float[] bins = new float[xf.availableRows() * FrequencyTransformer.BINS_PER_ROW];
        for(int i = 0; i < 10; ++i) {
            xf.getBinRows(bins, bins.length / FrequencyTransformer.BINS_PER_ROW);
            //System.out.println(i * (bins.length / FrequencyTransformer.BINS_PER_ROW) * FrequencyTransformer.ROW_TIME);
            //printHeatMap(bins, FrequencyTransformer.BINS_PER_ROW, i == 0, i == 9, null);
            xf.discardRows(bins.length / FrequencyTransformer.BINS_PER_ROW);
        }

        // 8 4 5 6 / 12 6 12 6 / 15 6 0 2 / 7 7 15 6 / 2 7 12 6 / 4 6 1 2
        int[] sequence = new int[] {8, 4, 5, 6, 12, 6, 12, 6, 15, 6, 0, 2, 7, 7, 15, 6, 2, 7, 12, 6, 4, 6, 1, 2};
        RecognizerTestCase[] testCases = new RecognizerTestCase[] {
                new RecognizerTestCase("clean", l, hw, sequence, 0.000f, 10f, 1),
                new RecognizerTestCase("close loud", l, SampleSeries.readFromFile("chirpSamples/cone-close-loud.wav"),
                        sequence, 5.190f, 10f, 5),
                new RecognizerTestCase("close soft", l, SampleSeries.readFromFile("chirpSamples/cone-close-soft.wav"),
                        sequence, 2.465f, 10f, 5),
                new RecognizerTestCase("far loud", l, SampleSeries.readFromFile("chirpSamples/cone-loud.wav"),
                        sequence, 3.420f, 10f, 6),
                new RecognizerTestCase("far soft", l, SampleSeries.readFromFile("chirpSamples/cone-soft.wav"),
                        sequence, 2.210f, 10f, 6),
                new RecognizerTestCase("table", l, SampleSeries.readFromFile("chirpSamples/closeRange.wav"),
                        sequence, 4.270f, 10f, 2),
        };

        System.out.println("Running tests!");
        for(RecognizerTestCase testCase: testCases) {
            RecognizerTestCase.Results results = testCase.testRecognizer(new TimeCorrelatingRecognizer(l));
            System.out.println(String.format(
                    "Testing %20s: %3d recognized, %3d unrecognized, %3d misrecognized, %3d spurious",
                    testCase.getName(), results.getRecognizedCount(), results.getUnrecognizedCount(),
                    results.getMisrecognizedCount(), results.getSpuriousCount()));
        }

        /*
        System.out.print("Recognizing with SimilarSignalMaxRecognizer, close loud challenge:\n");
        printQualityHeatMap(l, closeLoudChallenge, new SimilarSignalMaxRecognizer(l));
        recognize(new SimilarSignalMaxRecognizer(l), closeLoudChallenge);
        printQualityHeatMap(l, closeLoudChallenge, new TimeCorrelatingRecognizer(l));
        recognize(new TimeCorrelatingRecognizer(l), closeLoudChallenge);
        System.out.print("Recognizing with SimilarSignalMaxRecognizer, easy challenge:\n");
        printQualityHeatMap(l, easyChallenge, new TimeCorrelatingRecognizer(l));
        recognize(new TimeCorrelatingRecognizer(l), easyChallenge);
        */

        System.out.println("Done.\n");
    }

    private static void printQualityHeatMap(CodeLibrary l, SampleSeries series, CodeRecognizer rec) {
        int numRows = (int)(series.size() / FrequencyTransformer.ROW_SAMPLES) -
                l.maxCodeRows();
        float[] heatMap = new float[numRows * CodeLibrary.NUM_SYMBOLS];
        rec.warmup(series);
        rec.process(series);
        rec.matchQualityGraph(heatMap);
        printHeatMap(heatMap, CodeLibrary.NUM_SYMBOLS, true, true, new RowAnnotator() {
            public String annotateColBefore(int col, float[] bins) {
                if(col < 0) {
                    return "    ";
                }
                else {
                    return String.format("%2d: ", col);
                }
            }
        });
    }


    private static void recognize(CodeRecognizer r, SampleSeries series)
    {
        System.out.print("Symbols:");
        r.warmup(series);
        r.process(series);
        while(r.hasNextSymbol()) {
            int sym = r.nextSymbol();
            float t = r.getLastSymbolTime();
            if(sym >= 0) {
                System.out.print(String.format(" %d (%.3f)", sym, t));
            }
        }
        System.out.println();
    }

    private static void skipRows(FrequencyTransformer ft, int rowsToSkip) {
        while(rowsToSkip > 0) {
            int skipping = Math.min(rowsToSkip, ft.availableRows());
            if(skipping == 0) {
                throw new InvalidParameterException("rowsToSkip exceeds rows in input");
            }
            ft.discardRows(skipping);
            rowsToSkip -= skipping;
        }
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

    private static void printHeatMap(float[] data, int width, boolean topFrame, boolean bottomFrame, RowAnnotator annotator) {
        char[] graphChars = new char[] {' ', '.', ':', 'i', 'u', '*', '@', 'X'};
        int length = data.length / width;

        if(annotator == null) {
            annotator = new RowAnnotator();
        }

        if(topFrame) {
            System.out.print(String.format("%s+", annotator.annotateColBefore(-1, data)));
            for (int i = 0; i < length; ++i) {
                System.out.print("-");
            }
            System.out.println(String.format("+%s", annotator.annotateColAfter(-1, data)));
        }

        for(int i = 0; i < width; ++i) {
            System.out.print(String.format("%s|", annotator.annotateColBefore(i, data)));
            for(int j = 0; j < length; ++j) {
                float val = data[j * width + i];
                int c = annotator.annotateChar(j, i, data, val);
                if(c < 0) {
                    c = graphChars[Math.max(0, Math.min(graphChars.length - 1, (int) Math.floor(graphChars.length * val)))];
                }
                System.out.print((char)c);
            }
            System.out.println(String.format("| %s", annotator.annotateColAfter(i, data)));
        }

        if(bottomFrame) {
            System.out.print(String.format("%s+", annotator.annotateColBefore(-1, data)));
            for (int i = 0; i < length; ++i) {
                System.out.print("-");
            }
            System.out.println(String.format("+%s", annotator.annotateColAfter(-1, data)));
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
