package com.datanymize.config.transformer;

import java.util.Map;

/**
 * Transformer that replaces values with a constant.
 */
public class ConstantTransformer implements ITransformer {

    @Override
    public Object transform(Object value, Map<String, Object> params) {
        return params.getOrDefault("value", "REDACTED");
    }

    @Override
    public Object transformDeterministic(Object value, String seed, Map<String, Object> params) {
        return params.getOrDefault("value", "REDACTED");
    }

    @Override
    public String getName() {
        return "constant";
    }

    @Override
    public boolean supportsDeterministic() {
        return true;
    }
}
