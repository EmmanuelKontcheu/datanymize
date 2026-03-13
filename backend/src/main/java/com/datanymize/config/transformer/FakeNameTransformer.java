package com.datanymize.config.transformer;

import java.util.Map;
import java.util.Random;

/**
 * Transformer that generates fake names.
 */
public class FakeNameTransformer implements ITransformer {
    private static final String[] FIRST_NAMES = {
        "John", "Jane", "Michael", "Emily", "David", "Sarah", "Robert", "Jessica",
        "James", "Mary", "William", "Patricia", "Richard", "Jennifer", "Joseph", "Linda"
    };

    private static final String[] LAST_NAMES = {
        "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
        "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas"
    };

    @Override
    public Object transform(Object value, Map<String, Object> params) {
        Random random = new Random();
        String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
        String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
        return firstName + " " + lastName;
    }

    @Override
    public Object transformDeterministic(Object value, String seed, Map<String, Object> params) {
        Random random = new Random(seed.hashCode());
        String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
        String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
        return firstName + " " + lastName;
    }

    @Override
    public String getName() {
        return "fake_name";
    }

    @Override
    public boolean supportsDeterministic() {
        return true;
    }
}
