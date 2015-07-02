package org.vectrola.chirpchain.test0;

/**
 * Created by jlunder on 6/29/15.
 */
public class MockFrequencyTransformer extends FrequencyTransformer {
    private float[] binRows;
    private int rowsReturned = 0;

    public MockFrequencyTransformer(float[] binRows) {
        this.binRows = binRows;
    }

    public MockFrequencyTransformer(SampleSeries sampleSeries) {
        LiveFrequencyTransformer ft = new LiveFrequencyTransformer(true, false);
        ft.warmup(sampleSeries);
        ft.addSamples(sampleSeries);

        int rowCount = ft.getQueuedRows();
        this.binRows = new float[rowCount * BINS_PER_ROW];
        int rowsTranscribed = 0;
        while(rowsTranscribed < rowCount) {
            int rows = Math.min(rowCount - rowsTranscribed, ft.getAvailableRows());
            if(rows == 0) {
                throw new IllegalStateException("Something screwy happened");
            }
            ft.getBinRows(this.binRows, rowsTranscribed * BINS_PER_ROW, rows);
            ft.discardRows(rows);
            rowsTranscribed += rows;
        }
    }

    public float getTime() {
        return rowsReturned * ROW_SAMPLES / SampleSeries.SAMPLE_RATE;
    }

    public int getQueuedRows() {
        return binRows.length / BINS_PER_ROW - rowsReturned;
    }

    public int getAvailableRows() {
        return getQueuedRows();
    }

    public void getBinRows(float[] dest, int offset, int numRows) {
        System.arraycopy(binRows, rowsReturned * BINS_PER_ROW, dest, offset, numRows * BINS_PER_ROW);
    }

    public void discardRows(int numRows) {
        rowsReturned += numRows;
    }

    public void reset() {
        rowsReturned = 0;
    }
}
