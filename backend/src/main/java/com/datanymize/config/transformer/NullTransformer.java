package com.datanymize.config.transformer;

import java.util.Map;

/**
 * Transformer that sets values to null.
 */
public class NullTransformer implements ITransformer {

    @Override
    public Object transform(Object value, Map<String, Object> params) {
        return null;
    }

    @Override
    public Object transformDeterministic(Object value, String seed, Map<String, Object> params) {
        return null;
    }

    @Override
    public String getName() {
        return "null";
    }

    @Override
    public boolean supportsDeterministic() {
        return true;
    }
}
