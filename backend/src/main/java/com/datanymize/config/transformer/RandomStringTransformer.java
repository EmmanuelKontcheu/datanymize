package com.datanymize.config.transformer;

import java.util.Map;
import java.util.Random;

/**
 * Transformer that generates random strings.
 */
public class RandomStringTransformer implements ITransformer {
    private static final String CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    @Override
    public Object transform(Object value, Map<String, Object> params) {
        Random random = new Random();
        int length = ((Number) params.getOrDefault("length", 10)).intValue();
        return generateRandomString(random, length);
    }

    @Override
    public Object transformDeterministic(Object value, String seed, Map<String, Object> params) {
        Random random = new Random(seed.hashCode());
        int length = ((Number) params.getOrDefault("length", 10)).intValue();
        return generateRandomString(random, length);
    }

    private String generateRandomString(Random random, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(CHARSET.charAt(random.nextInt(CHARSET.length())));
        }
        return sb.toString();
    }

    @Override
    public String getName() {
        return "random_string";
    }

    @Override
    public boolean supportsDeterministic() {
        return true;
    }
}
