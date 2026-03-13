package com.datanymize.config;

import com.datanymize.config.transformer.*;
import net.jqwik.api.*;
import net.jqwik.api.constraints.StringLength;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for transformer availability.
 * **Validates: Requirements 4.4**
 *
 * Property 11: Transformer Availability
 * For any predefined transformer name, the transformer registry should provide a working transformer implementation.
 */
@PropertyDefaults(tries = 50)
class TransformerAvailabilityProperties {

    private static final List<String> PREDEFINED_TRANSFORMERS = List.of(
        "fake_name",
        "fake_email",
        "fake_phone",
        "hash",
        "mask",
        "null",
        "constant",
        "random_string",
        "random_number"
    );

    /**
     * Property 11a: All predefined transformers are available
     * For each predefined transformer name, the registry should have it available.
     */
    @Property
    @Label("All predefined transformers are available")
    void testAllPredefinedTransformersAvailable() {
        TransformerRegistry registry = new TransformerRegistry();

        for (String transformerName : PREDEFINED_TRANSFORMERS) {
            assertTrue(
                registry.hasTransformer(transformerName),
                "Transformer '" + transformerName + "' should be available"
            );
            assertNotNull(
                registry.getTransformer(transformerName),
                "Transformer '" + transformerName + "' should not be null"
            );
        }
    }

    /**
     * Property 11b: Transformer registry contains exactly 9 transformers
     * The registry should contain all 9 predefined transformers.
     */
    @Property
    @Label("Transformer registry contains exactly 9 transformers")
    void testTransformerRegistrySize() {
        TransformerRegistry registry = new TransformerRegistry();

        Set<String> availableTransformers = registry.listAvailableTransformers();
        assertEquals(
            9,
            availableTransformers.size(),
            "Registry should contain exactly 9 transformers"
        );

        for (String transformerName : PREDEFINED_TRANSFORMERS) {
            assertTrue(
                availableTransformers.contains(transformerName),
                "Registry should contain transformer '" + transformerName + "'"
            );
        }
    }

    /**
     * Property 11c: Each transformer produces valid output
     * For each transformer, applying it to a test value should produce non-null output.
     */
    @Property
    @Label("Each transformer produces valid output")
    void testEachTransformerProducesValidOutput(
        @ForAll @StringLength(min = 1, max = 50) String testValue
    ) {
        TransformerRegistry registry = new TransformerRegistry();

        for (String transformerName : PREDEFINED_TRANSFORMERS) {
            ITransformer transformer = registry.getTransformer(transformerName);
            assertNotNull(transformer, "Transformer '" + transformerName + "' should not be null");

            // Test non-deterministic transformation
            Object result = transformer.transform(testValue, new HashMap<>());
            assertNotNull(
                result,
                "Transformer '" + transformerName + "' should produce non-null output"
            );
        }
    }

    /**
     * Property 11d: Deterministic transformers support deterministic transformation
     * For transformers that support deterministic transformation, applying with seed should work.
     */
    @Property
    @Label("Deterministic transformers support deterministic transformation")
    void testDeterministicTransformersWork(
        @ForAll @StringLength(min = 1, max = 50) String testValue,
        @ForAll @StringLength(min = 1, max = 50) String seed
    ) {
        TransformerRegistry registry = new TransformerRegistry();

        // Transformers that support deterministic transformation
        List<String> deterministicTransformers = List.of(
            "fake_name",
            "fake_email",
            "fake_phone",
            "hash",
            "random_string",
            "random_number"
        );

        for (String transformerName : deterministicTransformers) {
            ITransformer transformer = registry.getTransformer(transformerName);
            assertTrue(
                transformer.supportsDeterministic(),
                "Transformer '" + transformerName + "' should support deterministic transformation"
            );

            Object result = transformer.transformDeterministic(testValue, seed, new HashMap<>());
            assertNotNull(
                result,
                "Transformer '" + transformerName + "' should produce non-null deterministic output"
            );
        }
    }

    /**
     * Property 11e: Transformer names are consistent
     * Each transformer's name should match its registry key.
     */
    @Property
    @Label("Transformer names are consistent")
    void testTransformerNamesConsistent() {
        TransformerRegistry registry = new TransformerRegistry();

        Map<String, ITransformer> allTransformers = registry.getAllTransformers();
        for (Map.Entry<String, ITransformer> entry : allTransformers.entrySet()) {
            String registryKey = entry.getKey();
            ITransformer transformer = entry.getValue();

            assertEquals(
                registryKey,
                transformer.getName(),
                "Transformer name should match registry key"
            );
        }
    }

    /**
     * Property 11f: Transformer registry is immutable after creation
     * Getting all transformers should return a copy, not the internal map.
     */
    @Property
    @Label("Transformer registry is immutable after creation")
    void testTransformerRegistryImmutability() {
        TransformerRegistry registry = new TransformerRegistry();

        Map<String, ITransformer> transformers1 = registry.getAllTransformers();
        Map<String, ITransformer> transformers2 = registry.getAllTransformers();

        // Modifying one copy should not affect the other
        transformers1.clear();

        assertEquals(
            9,
            transformers2.size(),
            "Modifying returned map should not affect registry"
        );
        assertEquals(
            9,
            registry.getAllTransformers().size(),
            "Registry should still have all transformers"
        );
    }

    /**
     * Property 11g: Unknown transformers are not available
     * For any unknown transformer name, the registry should not have it.
     */
    @Property
    @Label("Unknown transformers are not available")
    void testUnknownTransformersNotAvailable(
        @ForAll @StringLength(min = 1, max = 50) String unknownName
    ) {
        // Skip if the name happens to be a known transformer
        Assume.that(!PREDEFINED_TRANSFORMERS.contains(unknownName));

        TransformerRegistry registry = new TransformerRegistry();

        assertFalse(
            registry.hasTransformer(unknownName),
            "Unknown transformer '" + unknownName + "' should not be available"
        );
        assertNull(
            registry.getTransformer(unknownName),
            "Unknown transformer '" + unknownName + "' should return null"
        );
    }

    /**
     * Property 11h: Transformer output types are appropriate
     * Each transformer should produce output of appropriate type.
     */
    @Property
    @Label("Transformer output types are appropriate")
    void testTransformerOutputTypes(
        @ForAll @StringLength(min = 1, max = 50) String testValue
    ) {
        TransformerRegistry registry = new TransformerRegistry();

        // Test null transformer produces null
        ITransformer nullTransformer = registry.getTransformer("null");
        Object nullResult = nullTransformer.transform(testValue, new HashMap<>());
        assertNull(nullResult, "null transformer should produce null");

        // Test other transformers produce non-null
        List<String> nonNullTransformers = List.of(
            "fake_name",
            "fake_email",
            "fake_phone",
            "hash",
            "mask",
            "constant",
            "random_string",
            "random_number"
        );

        for (String transformerName : nonNullTransformers) {
            ITransformer transformer = registry.getTransformer(transformerName);
            Object result = transformer.transform(testValue, new HashMap<>());
            assertNotNull(
                result,
                "Transformer '" + transformerName + "' should produce non-null output"
            );
        }
    }
}
