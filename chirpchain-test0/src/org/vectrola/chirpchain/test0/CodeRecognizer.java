package org.vectrola.chirpchain.test0;

import com.sun.org.apache.bcel.internal.classfile.Code;

/**
 * Created by jlunder on 6/10/15.
 */
public class CodeRecognizer {
    private FrequencyTransformer frequencyTransformer = new FrequencyTransformer();
    private CodeLibrary library;
    private int maxCodeRows;
    private float[][] codeFingerprints =
            new float[CodeLibrary.NUM_SYMBOLS][maxCodeRows * FrequencyTransformer.BINS_PER_ROW];

    public CodeRecognizer(CodeLibrary library) {
        this.library = library;
        this.maxCodeRows = (int)Math.ceil(library.getMaxCodeLength() / FrequencyTransformer.ROW_TIME);

        SampleSeries pad = new SampleSeries(FrequencyTransformer.ROW_SAMPLES - 1);
        for(int i = 0; i < CodeLibrary.NUM_SYMBOLS; ++i) {
            FrequencyTransformer fingerprinter = new FrequencyTransformer();
            fingerprinter.addSamples(library.getCodeForSymbol(i));
            fingerprinter.addSamples(pad);

            int fingerprintRows = fingerprinter.availableRows();
            assert fingerprintRows <= maxCodeRows;
            float[] fp = new float[fingerprintRows * FrequencyTransformer.BINS_PER_ROW];
            fingerprinter.getBinRows(fp, fingerprintRows);
            codeFingerprints[i] = fp;
        }
    }

    public float[] getFingerprintForSymbol(int symbol)
    {
        return codeFingerprints[symbol];
    }

    public void process(SampleSeries samples) {
        frequencyTransformer.addSamples(samples);
        while (frequencyTransformer.availableRows() > maxCodeRows) {

        }
    }

    int recognizeCode() {
        return -1;
    }
}
