package com.datanymize.anonymization;

import com.datanymize.database.model.Row;

import java.util.*;

/**
 * Processes data in batches for efficient anonymization.
 */
public class BatchProcessor {
    private final int batchSize;
    private final List<Row> currentBatch;

    public BatchProcessor(int batchSize) {
        this.batchSize = batchSize;
        this.currentBatch = new ArrayList<>(batchSize);
    }

    /**
     * Add a row to the current batch.
     * @param row Row to add
     * @return true if batch is full and should be processed
     */
    public boolean addRow(Row row) {
        currentBatch.add(row);
        return currentBatch.size() >= batchSize;
    }

    /**
     * Get the current batch.
     * @return List of rows in current batch
     */
    public List<Row> getCurrentBatch() {
        return new ArrayList<>(currentBatch);
    }

    /**
     * Clear the current batch.
     */
    public void clearBatch() {
        currentBatch.clear();
    }

    /**
     * Check if batch has rows.
     * @return true if batch is not empty
     */
    public boolean hasBatch() {
        return !currentBatch.isEmpty();
    }

    /**
     * Get batch size.
     * @return Batch size
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * Get current batch row count.
     * @return Number of rows in current batch
     */
    public int getCurrentBatchRowCount() {
        return currentBatch.size();
    }
}
