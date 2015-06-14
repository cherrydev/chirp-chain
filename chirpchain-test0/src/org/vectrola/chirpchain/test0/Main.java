package org.vectrola.chirpchain.test0;

import java.io.IOException;

/**
 * Created by jlunder on 5/16/15.
 */
public class Main {
    public static void main(String[] args) throws IOException {
        System.out.print("Hello world!\n");

        CodeLibrary l = CodeLibrary.makeChirpCodes();
        for(int i = 0; i < CodeLibrary.NUM_SYMBOLS; ++i) {
            l.getCodeForSymbol(i).writeToFile(String.format("chirp%02d.wav", i));
        }

        encodeString("Hello world!").writeToFile("helloworld.wav");

        CodeRecognizer cr = new CodeRecognizer(l);
        for(int i = 0; i < CodeLibrary.NUM_SYMBOLS; ++i) {
            printFingerprint(cr, i);
        }
    }

    private static void printFingerprint(CodeRecognizer cr, int symbol) {
        char[] graphChars = new char[] {' ', '.', ':', 'i', 'u', '*', '@', 'X'};
        float[] fp = cr.getFingerprintForSymbol(symbol);
        System.out.println(String.format("Fingerprint for symbol %d:", symbol));
        for(int j = 0; j < fp.length / FrequencyTransformer.BINS_PER_ROW; ++j) {
            System.out.print('|');
            for(int i = 0; i < FrequencyTransformer.BINS_PER_ROW; ++i) {
                System.out.print(graphChars[Math.max(0, Math.min(graphChars.length - 1, (int)Math.floor(graphChars.length * 0.005f * fp[j * FrequencyTransformer.BINS_PER_ROW + i])))]);
            }
            System.out.println('|');
        }
        System.out.println();
    }

    public static SampleSeries encodeString(String s) {
        CodeLibrary l = CodeLibrary.makeChirpCodes();
        int slen = 0;
        SampleSeries series;

        for (int i = 0; i < s.length(); ++i) {
            int c = (int)s.charAt(i);
            int sym0 = (c & 0xF), sym1 = ((c >> 4) & 0xF);
            slen += l.getCodeForSymbol(sym0).size() + l.getCodeForSymbol(sym1).size();
        }
        series = new SampleSeries(slen);
        int j = 0;
        for (int i = 0; i < s.length(); ++i) {
            int c = (int) s.charAt(i);
            int sym0 = (c & 0xF), sym1 = ((c >> 4) & 0xF);
            SampleSeries sym0SS = l.getCodeForSymbol(sym0);
            SampleSeries sym1SS = l.getCodeForSymbol(sym1);
            System.arraycopy(sym0SS.getSamples(), 0, series.getSamples(), j, sym0SS.size());
            j += sym0SS.size();
            System.arraycopy(sym1SS.getSamples(), 0, series.getSamples(), j, sym1SS.size());
            j += sym1SS.size();
        }

        return series;
    }
}
