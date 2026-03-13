package com.datanymize.test;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.configurators.ArbitraryConfigurator;
import net.jqwik.api.providers.TypeUsage;

/**
 * jqwik Configuration for Datanymize property-based tests.
 * 
 * This class configures global settings for jqwik property-based testing:
 * - Default number of tries: 100+ iterations per property
 * - Shrinking: Enabled for better failure diagnostics
 * - Seed: Can be set via system property for reproducibility
 * - Timeout: Configured per test
 * 
 * Configuration can be customized via jqwik.properties file in test resources.
 * 
 * Example jqwik.properties:
 * ```
 * database = in-memory
 * tries = 100
 * max-discard-ratio = 5
 * seed = 1234567890
 * timeout = 5m
 * ```
 */
public class JqwikConfiguration {
    
    /**
     * Default number of tries for property-based tests.
     * This ensures comprehensive coverage of the input space.
     */
    public static final int DEFAULT_TRIES = 100;
    
    /**
     * Maximum number of tries for intensive tests.
     * Used for tests that need extra coverage.
     */
    public static final int INTENSIVE_TRIES = 500;
    
    /**
     * Minimum number of tries for quick smoke tests.
     * Used for tests that are expensive to run.
     */
    public static final int SMOKE_TEST_TRIES = 10;
    
    /**
     * Default timeout for property tests (in seconds).
     * Prevents tests from running indefinitely.
     */
    public static final int DEFAULT_TIMEOUT_SECONDS = 60;
    
    /**
     * Maximum discard ratio for property tests.
     * If more than this ratio of generated values are discarded,
     * the test fails to ensure sufficient coverage.
     */
    public static final double MAX_DISCARD_RATIO = 5.0;
    
    /**
     * Configuration documentation for developers.
     * 
     * To configure jqwik for Datanymize tests:
     * 
     * 1. Create src/test/resources/jqwik.properties with:
     *    ```
     *    database = in-memory
     *    tries = 100
     *    max-discard-ratio = 5
     *    seed = 1234567890
     *    timeout = 5m
     *    ```
     * 
     * 2. Or set system properties when running tests:
     *    ```
     *    mvn test -Djqwik.tries=100 -Djqwik.seed=1234567890
     *    ```
     * 
     * 3. Or use @Property annotation on individual tests:
     *    ```
     *    @Property(tries = 100, seed = "1234567890")
     *    void myTest(@ForAll String input) { ... }
     *    ```
     * 
     * For reproducibility, use the seed value from a failed test:
     *    ```
     *    mvn test -Djqwik.seed=<seed-from-failure>
     *    ```
     */
    public static final String CONFIGURATION_GUIDE = 
            "See JqwikConfiguration class for configuration details";
    
    /**
     * Get the configured number of tries for a test.
     * 
     * @param testType Type of test (NORMAL, INTENSIVE, SMOKE)
     * @return Number of tries to use
     */
    public static int getTries(TestType testType) {
        return switch (testType) {
            case INTENSIVE -> INTENSIVE_TRIES;
            case SMOKE -> SMOKE_TEST_TRIES;
            case NORMAL -> DEFAULT_TRIES;
        };
    }
    
    /**
     * Test type enumeration for configuring tries.
     */
    public enum TestType {
        /**
         * Normal property tests with standard coverage (100 tries).
         */
        NORMAL,
        
        /**
         * Intensive tests requiring extra coverage (500 tries).
         */
        INTENSIVE,
        
        /**
         * Quick smoke tests for fast feedback (10 tries).
         */
        SMOKE
    }
    
    /**
     * Helper class for test annotations.
     * Provides constants for common test configurations.
     */
    public static class Annotations {
        
        /**
         * Standard property test annotation values.
         * Use: @Property(tries = Annotations.STANDARD_TRIES)
         */
        public static final int STANDARD_TRIES = DEFAULT_TRIES;
        
        /**
         * Intensive property test annotation values.
         * Use: @Property(tries = Annotations.INTENSIVE_TRIES)
         */
        public static final int INTENSIVE_TRIES_ANNOTATION = INTENSIVE_TRIES;
        
        /**
         * Smoke test annotation values.
         * Use: @Property(tries = Annotations.SMOKE_TRIES)
         */
        public static final int SMOKE_TRIES = SMOKE_TEST_TRIES;
    }
}
