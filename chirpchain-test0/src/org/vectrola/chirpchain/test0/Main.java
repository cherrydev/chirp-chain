package org.vectrola.chirpchain.test0;

import java.io.IOException;

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

        // 8 4 5 6 / 12 6 12 6 / 15 6 0 2 / 7 7 15 6 / 2 7 12 6 / 4 6 1 2
        int[] sequence = new int[] {8, 4, 5, 6, 12, 6, 12, 6, 15, 6, 0, 2, 7, 7, 15, 6, 2, 7, 12, 6, 4, 6, 1, 2};
        RecognizerTestCase cleanTestCase = new RecognizerTestCase("clean", l, hw, sequence, 0.000f, 10f, 1);
        RecognizerTestCase closeLoudTestCase =
                new RecognizerTestCase("close loud", l, SampleSeries.readFromFile("chirpSamples/cone-close-loud.wav"),
                        sequence, 5.190f, 10f, 5);
        RecognizerTestCase closeSoftTestCase =
                new RecognizerTestCase("close soft", l, SampleSeries.readFromFile("chirpSamples/cone-close-soft.wav"),
                        sequence, 2.465f, 10f, 5);
        RecognizerTestCase farLoudTestCase =
                new RecognizerTestCase("far loud", l, SampleSeries.readFromFile("chirpSamples/cone-loud.wav"),
                        sequence, 3.420f, 10f, 6);
        RecognizerTestCase farSoftTestCase =
                new RecognizerTestCase("far soft", l, SampleSeries.readFromFile("chirpSamples/cone-soft.wav"),
                        sequence, 2.210f, 10f, 6);
        RecognizerTestCase tableTestCase =
                new RecognizerTestCase("table", l, SampleSeries.readFromFile("chirpSamples/closeRange.wav"),
                        sequence, 4.270f, 10f, 2);

        RecognizerTestCase[] testCases = new RecognizerTestCase[] { cleanTestCase, closeLoudTestCase,
                closeSoftTestCase, farLoudTestCase, farSoftTestCase, tableTestCase };

        System.out.println("Running tests!");
        for(RecognizerTestCase testCase: testCases) {
            RecognizerTestCase.Results results = testCase.testRecognizer(new CorrelationRecognizer(l));
            System.out.println(String.format(
                    "Testing %20s: %3d recognized, %3d unrecognized, %3d misrecognized, %3d spurious",
                    testCase.getName(), results.getRecognizedCount(), results.getUnrecognizedCount(),
                    results.getMisrecognizedCount(), results.getSpuriousCount()));
        }

        //printFingerprints(new TimeCorrelatingRecognizer(l));
        //printTestFrequencyHeatMap(closeLoudTestCase, new TimeCorrelatingRecognizer(l));
        printFrequencyHeatMap(cleanTestCase.getTestSeries(), true);
        printQualityHeatMap(cleanTestCase.getTestSeries(), new TimeCorrelatingRecognizer(l));

        System.out.println("Done.\n");
    }

    private static void printFingerprints(CodeRecognizer rec) {
        FrequencyTransformer ft = new FrequencyTransformer(false, false);
        float[] heatMap = new float[rec.getLibrary().getMaxCodeRows() * FrequencyTransformer.BINS_PER_ROW];
        for(int i = 0; i < CodeLibrary.NUM_SYMBOLS; ++i) {
            CodeRecognizer.Fingerprint fp = rec.getFingerprintForSymbol(i);
            int rows;
            ft.flush();
            ft.addSamples(fp.code);
            rows = ft.getAvailableRows();
            ft.getBinRows(heatMap, rows);
            System.out.println(String.format("Symbol %d:", i));
            printHeatMap(heatMap, rows, FrequencyTransformer.BINS_PER_ROW, false, true, true, new RowAnnotator() {
                @Override
                public int annotateChar(int row, int col, float[] bins, float value) {
                    if (fp instanceof TimeCorrelatingRecognizer.Fingerprint) {
                        TimeCorrelatingRecognizer.Fingerprint tcfp = (TimeCorrelatingRecognizer.Fingerprint) fp;
                        for (int bin : tcfp.getPattern()[row]) {
                            if (bin == row * FrequencyTransformer.BINS_PER_ROW + col) {
                                return (int) '/';
                            }
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
            TimeCorrelatingRecognizer.Fingerprint fp = (TimeCorrelatingRecognizer.Fingerprint)rec.getFingerprintForSymbol(es.getSymbol());
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

    private static void printFrequencyHeatMap(SampleSeries series, boolean adaptiveNoiseReject) {
        int numRows = series.size() / FrequencyTransformer.ROW_SAMPLES;
        FrequencyTransformer ft = new FrequencyTransformer(adaptiveNoiseReject, false);
        float[] heatMap = new float[numRows * FrequencyTransformer.BINS_PER_ROW];
        ft.warmup(series);
        ft.addSamples(series);
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

    private static void printQualityHeatMap(SampleSeries series, CodeRecognizer rec) {
        CodeLibrary l = rec.getLibrary();
        int numRows = (int)(series.size() / FrequencyTransformer.ROW_SAMPLES) -
                l.getMaxCodeRows();
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
        rec.warmup(series);
        rec.process(series);
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
