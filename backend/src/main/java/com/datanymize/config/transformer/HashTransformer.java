package com.datanymize.config.transformer;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * Transformer that hashes values.
 */
public class HashTransformer implements ITransformer {

    @Override
    public Object transform(Object value, Map<String, Object> params) {
        return transformDeterministic(value, "", params);
    }

    @Override
    public Object transformDeterministic(Object value, String seed, Map<String, Object> params) {
        if (value == null) {
            return null;
        }

        String algorithm = (String) params.getOrDefault("algorithm", "SHA-256");
        String input = value.toString();

        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hash = digest.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hash algorithm not supported: " + algorithm, e);
        }
    }

    @Override
    public String getName() {
        return "hash";
    }

    @Override
    public boolean supportsDeterministic() {
        return true;
    }
}
