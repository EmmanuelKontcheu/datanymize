package com.datanymize.config.transformer;

import java.util.Map;
import java.util.Random;

/**
 * Transformer that generates fake email addresses.
 */
public class FakeEmailTransformer implements ITransformer {
    private static final String[] DOMAINS = {
        "example.com", "test.com", "demo.com", "sample.com", "fake.com"
    };

    @Override
    public Object transform(Object value, Map<String, Object> params) {
        Random random = new Random();
        String username = "user" + random.nextInt(1000000);
        String domain = DOMAINS[random.nextInt(DOMAINS.length)];
        return username + "@" + domain;
    }

    @Override
    public Object transformDeterministic(Object value, String seed, Map<String, Object> params) {
        Random random = new Random(seed.hashCode());
        String username = "user" + random.nextInt(1000000);
        String domain = DOMAINS[random.nextInt(DOMAINS.length)];
        return username + "@" + domain;
    }

    @Override
    public String getName() {
        return "fake_email";
    }

    @Override
    public boolean supportsDeterministic() {
        return true;
    }
}
