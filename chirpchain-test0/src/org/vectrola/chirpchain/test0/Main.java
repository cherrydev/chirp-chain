package org.vectrola.chirpchain.test0;

import java.io.IOException;

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

        System.out.print("Fingerprinting library\n");

        CodeRecognizer cr = new CodeRecognizer(l);

        for(int i = 0; i < CodeLibrary.NUM_SYMBOLS; ++i) {
            //printFingerprint(cr, i);
        }

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



        System.out.println("Match quality");
        System.out.print("   ");
        for(int i = 0; i < CodeLibrary.NUM_SYMBOLS; ++i) {
            System.out.print(String.format("  %5d", i));
        }
        System.out.println();
        for(int i = 0; i < CodeLibrary.NUM_SYMBOLS; ++i) {
            System.out.print(String.format("%2d:", i));
            float[] inputPeaks = new float[cr.getFingerprintForSymbol(i).getMatchRows()];
            CodeRecognizer.CodeFingerprint.findPeaksInput(cr.getFingerprintForSymbol(i).getBins(), inputPeaks);
            for (int j = 0; j < CodeLibrary.NUM_SYMBOLS; ++j) {
                float q = CodeRecognizer.matchQuality(cr.getFingerprintForSymbol(j).getPeaks(), inputPeaks);
                System.out.print(String.format("  %5.2f", q));
            }
            System.out.println();
        }



        System.out.print("Encoding string\n");

        SampleSeries hw = encodeString(l, "Hello world!");
        hw.writeToFile("helloworld.wav");

        System.out.print("Recognizing!\n");
        cr.process(SampleSeries.readFromFile("helloworld-challenge.wav"));
        while(cr.hasNextSymbol()) {
            int sym = cr.nextSymbol();
            float t = cr.getLastSymbolTime();
            if(sym >= 0) {
                System.out.print(String.format("Recognized '%c' (%d) at %.4fs\n", (char)sym, sym, t));
            }
        }

        System.out.print("Done.\n");
    }

    private static void printFingerprint(CodeRecognizer cr, int symbol) {
        char[] graphChars = new char[] {' ', '.', ':', 'i', 'u', '*', '@', 'X'};
        float[] fp = cr.getFingerprintForSymbol(symbol).getBins();
        float[] pk = cr.getFingerprintForSymbol(symbol).getPeaks();
        System.out.println(String.format("Fingerprint for symbol %d:", symbol));
        for(int j = 0; j < fp.length / FrequencyTransformer.BINS_PER_ROW; ++j) {
            System.out.print('|');
            for(int i = 0; i < FrequencyTransformer.BINS_PER_ROW; ++i) {
                char c = graphChars[Math.max(0, Math.min(graphChars.length - 1, (int)Math.floor(graphChars.length * fp[j * FrequencyTransformer.BINS_PER_ROW + i])))];
                if(Math.abs(pk[j] - FrequencyTransformer.BIN_FREQUENCIES[i]) < FrequencyTransformer.BIN_BANDWIDTH * 0.5f) {
                    c = 'F';
                }
                System.out.print(c);
            }
            System.out.println(String.format("| %7.2f", pk[j]));
        }
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
        for (int i = 0; i < s.length(); ++i) {
            int c = (int) s.charAt(i);
            if(CodeLibrary.NUM_SYMBOLS == 256) {
                System.out.print(String.format("%d ", c));
                msg.append(l.getCodeForSymbol(c));
            }
            else if(CodeLibrary.NUM_SYMBOLS == 16) {
                int sym0 = (c & 0xF), sym1 = ((c >> 4) & 0xF);
                System.out.print(String.format("%d %d ", sym0, sym1));
                msg.append(l.getCodeForSymbol(sym0));
                msg.append(l.getCodeForSymbol(sym1));
            }
            msg.append(pad);
        }
        for(int i = 0; i < 10; ++i) {
            msg.append(pad);
        }

        System.out.println("done!");

        return msg;
    }
}
