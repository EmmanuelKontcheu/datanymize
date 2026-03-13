package com.datanymize.config.transformer;

import java.util.Map;

/**
 * Interface for data transformers (anonymization rules).
 */
public interface ITransformer {
    /**
     * Transform a value.
     * @param value Value to transform
     * @param params Transformer parameters
     * @return Transformed value
     */
    Object transform(Object value, Map<String, Object> params);

    /**
     * Transform a value deterministically.
     * @param value Value to transform
     * @param seed Seed for deterministic transformation
     * @param params Transformer parameters
     * @return Transformed value
     */
    Object transformDeterministic(Object value, String seed, Map<String, Object> params);

    /**
     * Get transformer name.
     * @return Transformer name
     */
    String getName();

    /**
     * Check if transformer supports deterministic transformation.
     * @return true if deterministic transformation is supported
     */
    boolean supportsDeterministic();
}
