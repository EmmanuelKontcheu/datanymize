package com.datanymize.config.transformer;

import java.util.Map;

/**
 * Transformer that masks values according to a pattern.
 */
public class MaskTransformer implements ITransformer {

    @Override
    public Object transform(Object value, Map<String, Object> params) {
        return transformDeterministic(value, "", params);
    }

    @Override
    public Object transformDeterministic(Object value, String seed, Map<String, Object> params) {
        if (value == null) {
            return null;
        }

        String pattern = (String) params.getOrDefault("pattern", "XXX-XXX-XXXX");
        String input = value.toString();

        // Replace X with characters from input, or keep X if input is shorter
        StringBuilder result = new StringBuilder();
        int inputIndex = 0;

        for (char c : pattern.toCharArray()) {
            if (c == 'X') {
                if (inputIndex < input.length()) {
                    result.append(input.charAt(inputIndex++));
                } else {
                    result.append('X');
                }
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    @Override
    public String getName() {
        return "mask";
    }

    @Override
    public boolean supportsDeterministic() {
        return true;
    }
}
