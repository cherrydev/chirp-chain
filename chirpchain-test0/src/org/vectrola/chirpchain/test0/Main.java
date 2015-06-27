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
        FrequencyTransformer fingerprintFT = new FrequencyTransformer();
        for(int i = 0; i < 30; ++i) {
            fingerprintFT.addSamples(s);
        }
        System.out.println(String.format("Frequency transformer test (%d rows):", fingerprintFT.availableRows()));
        char[] graphChars = new char[] {' ', '.', ':', 'i', 'u', '*', '@', 'X'};
        int rowCount = 0;
        while(fingerprintFT.availableRows() > 0) {
            float[] bins = new float[FrequencyTransformer.BINS_PER_ROW * 3];
            int rows = Math.min(fingerprintFT.availableRows(), 3);
            fingerprintFT.getBinRows(bins, rows);
            for (int j = 0; j < rows; ++j) {
                System.out.print('|');
                for (int i = 0; i < FrequencyTransformer.BINS_PER_ROW; ++i) {
                    char c = graphChars[Math.max(0, Math.min(graphChars.length - 1, (int) Math.floor(graphChars.length * bins[j * FrequencyTransformer.BINS_PER_ROW + i])))];
                    System.out.print(c);
                }
                System.out.println(String.format("| %d", rowCount++));
            }
            fingerprintFT.discardRows(rows);
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

        /*
        BinPatternRecognizer bpr = new BinPatternRecognizer(l);

        printFingerprint((BinPatternRecognizer.Fingerprint)bpr.getFingerprintForSymbol(8));
        FrequencyTransformer xf = new FrequencyTransformer(false);
        xf.addSamples(challenge);
        skipRows(xf, (int)(9.1000f / FrequencyTransformer.ROW_TIME));
        float[] bins = new float[xf.availableRows() * FrequencyTransformer.BINS_PER_ROW];
        for(int i = 0; i < 1; ++i) {
            xf.getBinRows(bins, bins.length / FrequencyTransformer.BINS_PER_ROW);
            System.out.println(i * (bins.length / FrequencyTransformer.BINS_PER_ROW) * FrequencyTransformer.ROW_TIME);
            printNormalizedBinRows(bins, null);
            xf.discardRows(bins.length / FrequencyTransformer.BINS_PER_ROW);
        }
        */



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

        SampleSeries closeLoudChallenge = SampleSeries.readFromFile("chirpSamples/cone-close-loud.wav");
        SampleSeries easyChallenge = SampleSeries.readFromFile("chirpSamples/closeRange.wav");

        FrequencyTransformer xf = new FrequencyTransformer(true, false);
        // warm up adaptive noise rejection
        xf.addSamples(closeLoudChallenge);
        while(xf.availableRows() > 0) {
            xf.discardRows(xf.availableRows());
        }
        xf.addSamples(closeLoudChallenge);
        float[] bins = new float[xf.availableRows() * FrequencyTransformer.BINS_PER_ROW];
        for(int i = 0; i < 10; ++i) {
            xf.getBinRows(bins, bins.length / FrequencyTransformer.BINS_PER_ROW);
            //System.out.println(i * (bins.length / FrequencyTransformer.BINS_PER_ROW) * FrequencyTransformer.ROW_TIME);
            //printHeatMap(bins, FrequencyTransformer.BINS_PER_ROW, i == 0, i == 9, null);
            xf.discardRows(bins.length / FrequencyTransformer.BINS_PER_ROW);
        }

        // 8 4 5 6 / 12 6 12 6 / 15 6 0 2 / 7 7 15 6 / 2 7 12 6 / 4 6 1 2

        System.out.print("Recognizing with SimilarSignalMaxRecognizer, hw:\n");
        printQualityHeatMap(l, hw, new SimilarSignalMaxRecognizer(l));
        printQualityHeatMap(l, hw, new TimeCorrelatingRecognizer(l));
        recognize(new SimilarSignalMaxRecognizer(l), hw);
        recognize(new TimeCorrelatingRecognizer(l), hw);

        System.out.print("Recognizing with SimilarSignalMaxRecognizer, close loud challenge:\n");
        recognize(new SimilarSignalMaxRecognizer(l), closeLoudChallenge);
        System.out.print("Recognizing with SimilarSignalMaxRecognizer, easy challenge:\n");
        recognize(new SimilarSignalMaxRecognizer(l), easyChallenge);

        System.out.print("Done.\n");
    }

    private static void printQualityHeatMap(CodeLibrary l, SampleSeries series, CodeRecognizer rec) {
        int numRows = (int)(series.size() / (FrequencyTransformer.ROW_TIME * SampleSeries.SAMPLE_RATE)) -
                l.maxCodeRows() - (int)Math.ceil(FrequencyTransformer.WAVELET_WINDOW / FrequencyTransformer.ROW_TIME);
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
