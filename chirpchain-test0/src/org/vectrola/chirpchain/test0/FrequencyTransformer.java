package org.vectrola.chirpchain.test0;

import java.security.InvalidParameterException;
import java.util.Vector;

/**
 * Created by jlunder on 6/10/15.
 */
public class FrequencyTransformer {
    private static final float DESIRED_TIME_WINDOW = 1f;
    private static final float DESIRED_ROW_TIME = 0.01f;
    private static final float DESIRED_WAVELET_WINDOW = DESIRED_ROW_TIME * 2;
    public static final int ROW_SAMPLES = (int)Math.rint(DESIRED_ROW_TIME * SampleSeries.SAMPLE_RATE);
    public static final float ROW_TIME = ROW_SAMPLES / SampleSeries.SAMPLE_RATE;
    public static final int TOTAL_ROWS = (int) Math.ceil(DESIRED_TIME_WINDOW / ROW_TIME);
    public static final float TIME_WINDOW = TOTAL_ROWS * ROW_TIME;
    public static final float MIN_FREQUENCY = 1000f;
    public static final float MAX_FREQUENCY = 6000f;
    public static final float BIN_BANDWIDTH = 50f;
    public static final int BINS_PER_ROW = (int)((MAX_FREQUENCY - MIN_FREQUENCY) / BIN_BANDWIDTH) + 1;
    public static final int WAVELET_WINDOW_SAMPLES = (int)Math.rint(DESIRED_WAVELET_WINDOW * SampleSeries.SAMPLE_RATE);
    public static final float WAVELET_WINDOW = WAVELET_WINDOW_SAMPLES / SampleSeries.SAMPLE_RATE;
    public static final float[] BIN_FREQUENCIES = makeBinFrequencies(BINS_PER_ROW, MIN_FREQUENCY, MAX_FREQUENCY);

    private static float[] makeBinFrequencies(int bins, float minFreq, float maxFreq)
    {
        float[] freqs = new float[bins];
        for(int i = 0; i < bins; ++i) {
            freqs[i] = minFreq + i * (maxFreq - minFreq) / (bins - 1);
        }
        return freqs;
    }

    private float[][][] motherWavelets = new float[BINS_PER_ROW][2][WAVELET_WINDOW_SAMPLES];
    private float[] bins = new float[BINS_PER_ROW * TOTAL_ROWS];
    private int firstBinRow = 0;
    private int lastBinRow = 0;
    private Vector<SampleSeries> sampleBuffer = new Vector<SampleSeries>();
    private int consumedSamples = 0;
    private int pendingSamples = 0;

    private long samplesProcessed = 0;

    public float getTime() {
        return (samplesProcessed - availableRows() * ROW_SAMPLES) / SampleSeries.SAMPLE_RATE;
    }

    private boolean adaptiveNoiseReject = true;
    private boolean noiseFloorInited = false;
    private float[] noiseFloor = new float[BINS_PER_ROW];
    private float avgDev = 0.1f;

    public int availableRows() {
        return (lastBinRow + TOTAL_ROWS - firstBinRow) % TOTAL_ROWS;
    }
    public float getBufferedTime() {
        return availableRows() * ROW_TIME + (pendingSamples - consumedSamples) / SampleSeries.SAMPLE_RATE;
    }

    public FrequencyTransformer(boolean adaptiveNoiseReject, boolean zeroNoiseFloor)
    {
        //SampleSeries s = new SampleSeries(WAVELET_WINDOW_SAMPLES);
        float[] waveletWindow = new float[WAVELET_WINDOW_SAMPLES];
        double windowK = 2d * Math.PI / WAVELET_WINDOW_SAMPLES;
        for(int i = 0; i < waveletWindow.length; ++i) {
            waveletWindow[i] = (float)(0.5d + 0.5d * Math.cos(windowK * (i - WAVELET_WINDOW_SAMPLES * 0.5f)));
        }

        /*
        System.arraycopy(waveletWindow, 0, s.getSamples(), 0, WAVELET_WINDOW_SAMPLES);
        try {
            s.writeToFile("waveletwindow.wav");
        }
        catch (Exception e) {
        }
        */

        for(int j = 0; j < BINS_PER_ROW; ++j) {
            float f = MIN_FREQUENCY + ((float)j / (BINS_PER_ROW - 1)) * (MAX_FREQUENCY - MIN_FREQUENCY);
            for(int i = 0; i < WAVELET_WINDOW_SAMPLES; ++i) {
                double phase = 2d * Math.PI * f * i / SampleSeries.SAMPLE_RATE;
                motherWavelets[j][0][i] = waveletWindow[i] * (float)Math.sin(phase) * 4f / WAVELET_WINDOW_SAMPLES;
                motherWavelets[j][1][i] = waveletWindow[i] * (float)Math.cos(phase) * 4f / WAVELET_WINDOW_SAMPLES;
            }
            /*
            try {
                System.arraycopy(motherWavelets, sinOffset, s.getSamples(), 0, WAVELET_WINDOW_SAMPLES);
                s.writeToFile(String.format("sinwavelet%03d.wav", j));
                System.arraycopy(motherWavelets, cosOffset, s.getSamples(), 0, WAVELET_WINDOW_SAMPLES);
                s.writeToFile(String.format("coswavelet%03d.wav", j));
            }
            catch(Exception e) {
            }
            */
        }
        bins = new float[TOTAL_ROWS * BINS_PER_ROW];

        this.adaptiveNoiseReject = adaptiveNoiseReject;
        if(zeroNoiseFloor) {
            noiseFloorInited = true;
        }
    }

    public void warmup(SampleSeries samples) {
        addSamples(samples);
        samplesProcessed -= samples.size();
        while(availableRows() > 0) {
            discardRows(availableRows());
        }
    }

    public void addSamples(SampleSeries samples) {
        if(samples.size() > 0) {
            sampleBuffer.add(samples);
            pendingSamples += samples.size();
            tryConsumeSamples();
        }
    }

    public void getBinRows(float[] dest, int numRows)
    {
        if(numRows > availableRows()) {
            throw new InvalidParameterException("numRows exceeds availableRows()");
        }
        if(firstBinRow + numRows <= TOTAL_ROWS) {
            System.arraycopy(bins, firstBinRow * BINS_PER_ROW, dest, 0, numRows * BINS_PER_ROW);
        }
        else {
            System.arraycopy(bins, firstBinRow * BINS_PER_ROW, dest, 0, (TOTAL_ROWS - firstBinRow) * BINS_PER_ROW);
            System.arraycopy(bins, 0, dest, (TOTAL_ROWS - firstBinRow) * BINS_PER_ROW, (firstBinRow + numRows - TOTAL_ROWS) * BINS_PER_ROW);
        }
    }

    public void discardRows(int numRows) {
        if(numRows > availableRows()) {
            throw new InvalidParameterException("numRows exceeds availableRows()");
        }
        firstBinRow = (firstBinRow + numRows) % TOTAL_ROWS;
        tryConsumeSamples();
    }

    private void tryConsumeSamples()
    {
        float[] blockSamples = new float[WAVELET_WINDOW_SAMPLES];
        while(((pendingSamples - consumedSamples) >= WAVELET_WINDOW_SAMPLES) && (availableRows() < (TOTAL_ROWS - 1))) {
            makeContiguousSampleBlock(blockSamples);
            generateOneRowFromContiguousSampleBlock(blockSamples);
            lastBinRow = (lastBinRow + 1) % TOTAL_ROWS;
            consumedSamples += ROW_SAMPLES;
            flushConsumedSamples();

            samplesProcessed += ROW_SAMPLES;
        }
    }

    private void makeContiguousSampleBlock(float[] blockSamples) {
        int i = 0, j = 0;
        SampleSeries thisSeries;
        int thisSeriesOffset = consumedSamples;

        while(i < blockSamples.length) {
            assert j < sampleBuffer.size();
            thisSeries = sampleBuffer.get(j);
            int samplesToCopy = Math.min(thisSeries.size() - thisSeriesOffset, blockSamples.length - i);
            System.arraycopy(thisSeries.getSamples(), thisSeriesOffset, blockSamples, i, samplesToCopy);
            i += samplesToCopy;
            ++j;
            thisSeriesOffset = 0;
        }
    }

    private void generateOneRowFromContiguousSampleBlock(float[] blockSamples) {
        int offset = lastBinRow * BINS_PER_ROW;

        for(int j = 0; j < BINS_PER_ROW; ++j) {
            float sinSum = 0f;
            float cosSum = 0f;
            for (int i = 0; i < WAVELET_WINDOW_SAMPLES; ++i) {
                sinSum += blockSamples[i] * motherWavelets[j][0][i];
                cosSum += blockSamples[i] * motherWavelets[j][1][i];
            }
            bins[offset + j] = (float) Math.sqrt(sinSum * sinSum + cosSum * cosSum);
        }
        if(adaptiveNoiseReject) {
            if(!noiseFloorInited) {
                for(int j = 0; j < BINS_PER_ROW; ++j) {
                    noiseFloor[j] = bins[offset + j];
                    avgDev = 0.1f;
                }
            }
            float rowStdDev = 0f;
            for(int j = 0; j < BINS_PER_ROW; ++j) {
                float diff = bins[offset + j] - noiseFloor[j];
                rowStdDev += diff * diff;
            }
            rowStdDev = (float)Math.sqrt(rowStdDev / BINS_PER_ROW);
            avgDev = avgDev * (1f - 1f / 128f) + rowStdDev * 4f / 128f;
            for(int j = 0; j < BINS_PER_ROW; ++j) {
                float val = bins[offset + j];
                bins[offset + j] = (val - noiseFloor[j]) / avgDev;

                if(val > noiseFloor[j]) {
                    noiseFloor[j] *= (1f + 1f / 128f);
                }
                else {
                    noiseFloor[j] /= (1f + 1f / 128f);
                }
            }
        }
        noiseFloorInited = true;
    }

    private void flushConsumedSamples() {
        while(!sampleBuffer.isEmpty()) {
            SampleSeries firstSeries = sampleBuffer.get(0);
            if (consumedSamples < firstSeries.size()) {
                break;
            }
            consumedSamples -= firstSeries.size();
            pendingSamples -= firstSeries.size();
            sampleBuffer.remove(0);
        }
    }

    public void flush() {
        firstBinRow = lastBinRow = 0;
        pendingSamples = consumedSamples = 0;
        sampleBuffer.clear();
    }
}

