package com.datanymize.anonymization;

import com.datanymize.config.transformer.ITransformer;

import java.util.Map;

/**
 * Base class for deterministic transformers.
 * Ensures same input + seed = same output.
 */
public abstract class DeterministicTransformer implements ITransformer {
    protected String seed;

    public DeterministicTransformer(String seed) {
        this.seed = seed;
    }

    @Override
    public Object transform(Object value, Map<String, Object> params) {
        return transformDeterministic(value, seed, params);
    }

    @Override
    public boolean supportsDeterministic() {
        return true;
    }

    protected long generateSeed(String seedValue) {
        if (seedValue == null || seedValue.isEmpty()) {
            return System.currentTimeMillis();
        }
        return seedValue.hashCode();
    }
}
