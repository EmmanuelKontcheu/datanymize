package com.datanymize.config.transformer;

import java.util.Map;
import java.util.Random;

/**
 * Transformer that generates random numbers.
 */
public class RandomNumberTransformer implements ITransformer {

    @Override
    public Object transform(Object value, Map<String, Object> params) {
        Random random = new Random();
        long min = ((Number) params.getOrDefault("min", 0)).longValue();
        long max = ((Number) params.getOrDefault("max", 1000000)).longValue();
        return min + random.nextLong() % (max - min + 1);
    }

    @Override
    public Object transformDeterministic(Object value, String seed, Map<String, Object> params) {
        Random random = new Random(seed.hashCode());
        long min = ((Number) params.getOrDefault("min", 0)).longValue();
        long max = ((Number) params.getOrDefault("max", 1000000)).longValue();
        return min + random.nextLong() % (max - min + 1);
    }

    @Override
    public String getName() {
        return "random_number";
    }

    @Override
    public boolean supportsDeterministic() {
        return true;
    }
}
