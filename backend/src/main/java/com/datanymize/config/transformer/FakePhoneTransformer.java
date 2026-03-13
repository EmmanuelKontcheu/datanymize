package com.datanymize.config.transformer;

import java.util.Map;
import java.util.Random;

/**
 * Transformer that generates fake phone numbers.
 */
public class FakePhoneTransformer implements ITransformer {

    @Override
    public Object transform(Object value, Map<String, Object> params) {
        Random random = new Random();
        int areaCode = 200 + random.nextInt(800);
        int exchange = 200 + random.nextInt(800);
        int lineNumber = random.nextInt(10000);
        return String.format("+1-%03d-%03d-%04d", areaCode, exchange, lineNumber);
    }

    @Override
    public Object transformDeterministic(Object value, String seed, Map<String, Object> params) {
        Random random = new Random(seed.hashCode());
        int areaCode = 200 + random.nextInt(800);
        int exchange = 200 + random.nextInt(800);
        int lineNumber = random.nextInt(10000);
        return String.format("+1-%03d-%03d-%04d", areaCode, exchange, lineNumber);
    }

    @Override
    public String getName() {
        return "fake_phone";
    }

    @Override
    public boolean supportsDeterministic() {
        return true;
    }
}
