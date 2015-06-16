package org.vectrola.chirpchain.test0;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.*;

/**
 * Created by jlunder on 6/10/15.
 */
public class SampleSeries {

    public static final float SAMPLE_RATE = 48000f;

    private float[] samples = null;

    public SampleSeries() {
    }

    public SampleSeries(int numSamples) {
        samples = new float[numSamples];
    }

    public void resize(int newSize) {
        float[] newSamples = new float[newSize];
        if(samples != null) {
            System.arraycopy(samples, 0, newSamples, 0, Math.min(samples.length, newSize));
        }
        samples = newSamples;
    }

    public void append(SampleSeries newSeries) {
        if(newSeries.size() > 0) {
            int start = size();
            resize(size() + newSeries.size());
            System.arraycopy(newSeries.samples, 0, samples, start, newSeries.samples.length);
        }
    }

    public void setSample(int index, float value) {
        samples[index] = value;
    }

    public float getSample(int index) {
        return samples[index];
    }

    public int size() {
        return samples == null ? 0 : samples.length;
    }

    public float[] getSamples() {
        return samples;
    }

    public static SampleSeries readFromFile(String filename) throws IOException {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        AudioInputStream ais;
        try {
            ais = AudioSystem.getAudioInputStream(format, AudioSystem.getAudioInputStream(new File(filename)));
        }
        catch(UnsupportedAudioFileException e) {
            throw new IOException(String.format("Unusable or unrecognizable audio file format for '%s'", filename), e);
        }
        byte[] bytes = new byte[4096];
        SampleSeries series = new SampleSeries(ais.available() / 2);
        int totalSamples = 0;

        do {
            int bytesRead = ais.read(bytes);
            assert (bytesRead & 1) == 0;
            if (totalSamples + bytesRead / 2 > series.samples.length) {
                series.resize((totalSamples + bytesRead / 2) * 3 / 2);
            }
            for (int i = 0; i < bytesRead; i += 2) {
                short sample = (short)(((int)bytes[i] & 0xFF) | ((int) bytes[i + 1] << 8));
                series.samples[totalSamples++] = (float) sample / 32767f;
            }
        } while (ais.available() > 0);

        if (series.samples.length > totalSamples) {
            series.resize(totalSamples);
        }

        return series;
    }

    public void writeToFile(String filename) throws IOException {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        byte[] bytes = new byte[samples.length * 2];

        for (int i = 0; i < samples.length; ++i) {
            short sample = (short) Math.max(Math.min(Math.rint(samples[i] * 32767f), 32768f), -32768f);
            bytes[i * 2] = (byte) sample;
            bytes[i * 2 + 1] = (byte) (sample >>> 8);
        }

        AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(bytes), format, bytes.length),
                AudioFileFormat.Type.WAVE, new File(filename));
    }
}
