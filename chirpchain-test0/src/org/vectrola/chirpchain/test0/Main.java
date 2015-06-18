package org.vectrola.chirpchain.test0;

import java.io.IOException;
import java.security.InvalidParameterException;

/**
 * Created by jlunder on 5/16/15.
 */
public class Main {
    public static void main(String[] args) throws IOException {
        /*
        SampleSeries s = new SampleSeries((int)(SampleSeries.SAMPLE_RATE * 0.15f));
        for(int i = 0; i < s.size() / 2; ++i) {
            float t = ((float)i / SampleSeries.SAMPLE_RATE);
            float sample = (float)Math.sin(2f * (float)Math.PI * (2000f * t + (1000f / 0.075f) * 0.5f * t * t));
            s.setSample(i, sample);
            s.setSample(s.size() - 1 - i, sample);
        }
        FrequencyTransformer ft = new FrequencyTransformer();
        for(int i = 0; i < 30; ++i) {
            ft.addSamples(s);
        }
        System.out.println(String.format("Frequency transformer test (%d rows):", ft.availableRows()));
        char[] graphChars = new char[] {' ', '.', ':', 'i', 'u', '*', '@', 'X'};
        int rowCount = 0;
        while(ft.availableRows() > 0) {
            float[] bins = new float[FrequencyTransformer.BINS_PER_ROW * 3];
            int rows = Math.min(ft.availableRows(), 3);
            ft.getBinRows(bins, rows);
            for (int j = 0; j < rows; ++j) {
                System.out.print('|');
                for (int i = 0; i < FrequencyTransformer.BINS_PER_ROW; ++i) {
                    char c = graphChars[Math.max(0, Math.min(graphChars.length - 1, (int) Math.floor(graphChars.length * bins[j * FrequencyTransformer.BINS_PER_ROW + i])))];
                    System.out.print(c);
                }
                System.out.println(String.format("| %d", rowCount++));
            }
            ft.discardRows(rows);
        }
        System.out.println();

        System.exit(0);
        */

        System.out.print("Generating code library\n");

        CodeLibrary l = CodeLibrary.makeChirpCodes();
        for (int i = 0; i < CodeLibrary.NUM_SYMBOLS; ++i) {
            //l.getCodeForSymbol(i).writeToFile(String.format("chirp%03d.wav", i));
        }


        /*
        System.out.print("Fingerprinting library\n");

        PeakListRecognizer cr = new PeakListRecognizer(l);

        for(int i = 0; i < CodeLibrary.NUM_SYMBOLS; ++i) {
            //System.out.println(String.format("Fingerprint for symbol %d:", symbol));
            //printFingerprint(cr, i);
        }
        */


        /*
        System.out.println("Correlation");
        System.out.print("   ");
        for(int i = 0; i < CodeLibrary.NUM_SYMBOLS; ++i) {
            System.out.print(String.format("  %5d", i));
        }
        System.out.println();
        for(int i = 0; i < CodeLibrary.NUM_SYMBOLS; ++i) {
            System.out.print(String.format("%2d:", i));
            for (int j = 0; j < CodeLibrary.NUM_SYMBOLS; ++j) {
                float cc = CodeRecognizer.correlation(cr.getFingerprintForSymbol(i).getBins(), 0.25f,
                        cr.getFingerprintForSymbol(j).getBins(),
                        CodeRecognizer.mean(cr.getFingerprintForSymbol(j).getBins()));
                System.out.print(String.format("  %5.2f", cc));
            }
            System.out.println();
        }
        */




        /*
        System.out.println("Match quality");
        System.out.print("   ");
        for(int i = 0; i < CodeLibrary.NUM_SYMBOLS; ++i) {
            System.out.print(String.format("  %5d", i));
        }
        System.out.println();
        for(int i = 0; i < CodeLibrary.NUM_SYMBOLS; ++i) {
            System.out.print(String.format("%2d:", i));
            float[] inputPeaks = new float[cr.getFingerprintForSymbol(i).getMatchRows()];
            PeakListRecognizer.findPeaksInput(((PeakListRecognizer.Fingerprint)cr.getFingerprintForSymbol(i)).getBins(), inputPeaks);
            for (int j = 0; j < CodeLibrary.NUM_SYMBOLS; ++j) {
                float q = PeakListRecognizer.matchQuality(((PeakListRecognizer.Fingerprint)cr.getFingerprintForSymbol(j)).getPeaks(), inputPeaks);
                System.out.print(String.format("  %5.2f", q));
            }
            System.out.println();
        }
        */

        SampleSeries challenge = SampleSeries.readFromFile("helloworld-challenge.wav");

        BinPatternRecognizer bpr = new BinPatternRecognizer(l);

        printFingerprint((BinPatternRecognizer.Fingerprint)bpr.getFingerprintForSymbol(8));
        FrequencyTransformer xf = new FrequencyTransformer();
        xf.addSamples(challenge);
        skipRows(xf, (int)(9.1000f / FrequencyTransformer.ROW_TIME));
        float[] bins = new float[xf.availableRows() * FrequencyTransformer.BINS_PER_ROW];
        for(int i = 0; i < 1; ++i) {
            xf.getBinRows(bins, bins.length / FrequencyTransformer.BINS_PER_ROW);
            System.out.println(i * (bins.length / FrequencyTransformer.BINS_PER_ROW) * FrequencyTransformer.ROW_TIME);
            printNormalizedBinRows(bins, null);
            xf.discardRows(bins.length / FrequencyTransformer.BINS_PER_ROW);
        }



        System.out.print("Encoding string\n");

        SampleSeries hw = encodeString(l, "Hello world!");
        hw.writeToFile("helloworld.wav");

        /*
        for(int i = 0; i < CodeLibrary.NUM_SYMBOLS; ++i) {
            BinPatternRecognizer.BinPatternFingerprint fp =
                    (BinPatternRecognizer.Fingerprint)bpr.getFingerprintForSymbol(i);
            float ex = PeakListRecognizer.mean(fp.getBins());
            float mx = PeakListRecognizer.max(fp.getBins(), 0, fp.getBins().length);

            float q = BinPatternRecognizer.matchQuality(fp.getBinPattern(), fp.getBins(), mx * 0.5f + ex * 0.5f);
            System.out.println(String.format("self-q for %d: %.4f", i, q));
        }
        */


        System.out.print("Recognizing with TimeCorrelatingRecognizer, hw:\n");
        recognize(new TimeCorrelatingRecognizer(l), hw);
        System.out.print("Recognizing with TimeCorrelatingRecognizer, challenge:\n");
        recognize(new TimeCorrelatingRecognizer(l), challenge);
        System.out.print("Recognizing with BinPatternRecognizer, hw:\n");
        recognize(new BinPatternRecognizer(l), hw);
        System.out.print("Recognizing with BinPatternRecognizer, challenge:\n");
        recognize(new BinPatternRecognizer(l), challenge);
        System.out.print("Recognizing with PeakListRecognizer, hw:\n");
        recognize(new PeakListRecognizer(l), hw);
        System.out.print("Recognizing with PeakListRecognizer, challenge:\n");
        recognize(new PeakListRecognizer(l), challenge);

        System.out.print("Done.\n");
    }

    private static void recognize(CodeRecognizer r, SampleSeries series)
    {
        System.out.print("Symbols:");
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

    private static void printNormalizedBinRows(float[] bins, RowAnnotator annotator) {
        float ex = BinPatternRecognizer.mean(bins);
        float mx = BinPatternRecognizer.max(bins, 0, bins.length);
        for (int i = 0; i < bins.length; ++i) {
            bins[i] = Math.max(bins[i], 0f) * 3f / mx;
        }
        printBinRows(bins, annotator);
    }

    private static class RowAnnotator {
        public String annotateRow(int row, float[] bins) {
            return "";
        }
        public int annotateChar(int row, int col, float[] bins, float value) {
            return -1;
        }
    }

    private static void printBinRows(float[] binRows, RowAnnotator annotator) {
        char[] graphChars = new char[] {' ', '.', ':', 'i', 'u', '*', '@', 'X'};

        if(annotator == null) {
            annotator = new RowAnnotator();
        }
        System.out.print("+");
        for(int i = 0; i < FrequencyTransformer.BINS_PER_ROW; ++i) {
            System.out.print("-");
        }
        System.out.println("+");
        for(int j = 0; j < binRows.length / FrequencyTransformer.BINS_PER_ROW; ++j) {
            System.out.print('|');
            for(int i = 0; i < FrequencyTransformer.BINS_PER_ROW; ++i) {
                float val = binRows[j * FrequencyTransformer.BINS_PER_ROW + i];
                int c = annotator.annotateChar(j, i, binRows, val);
                if(c < 0) {
                    c = graphChars[Math.max(0, Math.min(graphChars.length - 1, (int) Math.floor(graphChars.length * val)))];
                }
                System.out.print((char)c);
            }
            System.out.println(String.format("| %s", annotator.annotateRow(j, binRows)));
        }
        System.out.print("+");
        for(int i = 0; i < FrequencyTransformer.BINS_PER_ROW; ++i) {
            System.out.print("-");
        }
        System.out.println("+");
    }

    private static void printFingerprint(BinPatternRecognizer.Fingerprint fp) {
        printBinRows(fp.getBins(), new RowAnnotator() {
            @Override
            public int annotateChar(int row, int col, float[] bins, float value) {
                int offset = (row * FrequencyTransformer.BINS_PER_ROW) + col;
                for(int i: fp.getBinPattern()) {
                    if(i == offset) {
                        return 'X';
                    }
                }
                return -1;
            }
        });
        System.out.println();
    }

    private static void printFingerprint(PeakListRecognizer.Fingerprint fp) {
        float[] pk = fp.getPeaks();
        printBinRows(fp.getBins(), new RowAnnotator() {
            @Override
            public int annotateChar(int row, int col, float[] bins, float value) {
                if(Math.abs(pk[row] - FrequencyTransformer.BIN_FREQUENCIES[row]) < FrequencyTransformer.BIN_BANDWIDTH * 0.5f) {
                    return (int)'F';
                }
                else {
                    return -1;
                }
            }

            @Override
            public String annotateRow(int row, float[] bins) {
                return String.format("%7.2f", pk[row]);
            }
        });
        System.out.println();
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
