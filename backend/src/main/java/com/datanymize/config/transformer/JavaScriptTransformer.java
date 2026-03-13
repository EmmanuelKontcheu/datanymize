package com.datanymize.config.transformer;

import java.util.Map;

/**
 * Sandboxed JavaScript transformer implementation.
 * Executes custom JavaScript code in a restricted environment.
 *
 * Requirements: 4.5
 */
public class JavaScriptTransformer implements ITransformer {
    private final String code;
    private final String name;

    /**
     * Create a JavaScript transformer.
     *
     * @param code JavaScript source code
     */
    public JavaScriptTransformer(String code) {
        this.code = code;
        this.name = "custom_javascript_" + System.identityHashCode(this);
    }

    @Override
    public Object transform(Object value, Map<String, Object> params) {
        try {
            return executeJavaScript(value, params, null);
        } catch (Exception e) {
            throw new CustomTransformerExecutionException(
                "Failed to execute JavaScript transformer: " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public Object transformDeterministic(Object value, String seed, Map<String, Object> params) {
        try {
            return executeJavaScript(value, params, seed);
        } catch (Exception e) {
            throw new CustomTransformerExecutionException(
                "Failed to execute JavaScript transformer: " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean supportsDeterministic() {
        return true;
    }

    /**
     * Execute JavaScript code in sandboxed environment.
     *
     * @param value Input value to transform
     * @param params Transformer parameters
     * @param seed Optional seed for deterministic transformation
     * @return Transformed value
     * @throws Exception if execution fails
     */
    private Object executeJavaScript(Object value, Map<String, Object> params, String seed) throws Exception {
        // Build execution context
        StringBuilder context = new StringBuilder();
        context.append("(function() {\n");
        context.append("  var value = ").append(toJavaScriptLiteral(value)).append(";\n");
        context.append("  var params = ").append(toJavaScriptObject(params)).append(";\n");
        if (seed != null) {
            context.append("  var seed = '").append(escapeJavaScriptString(seed)).append("';\n");
        }
        context.append("  ").append(code).append("\n");
        context.append("})();\n");

        // Execute JavaScript (placeholder - would use Nashorn/GraalVM in production)
        return executeInSandbox(context.toString());
    }

    /**
     * Execute code in sandboxed environment.
     * This is a placeholder implementation.
     * In production, use Nashorn (Java 8-14) or GraalVM (Java 15+).
     *
     * @param code JavaScript code to execute
     * @return Result of execution
     * @throws Exception if execution fails
     */
    private Object executeInSandbox(String code) throws Exception {
        // Placeholder: In production, use:
        // - Nashorn ScriptEngine (Java 8-14)
        // - GraalVM JavaScript (Java 15+)
        // - Rhino (alternative)

        // For now, throw informative error
        throw new UnsupportedOperationException(
            "JavaScript execution requires Nashorn or GraalVM. " +
            "Please add 'org.graalvm.js:js' or 'org.graalvm.js:js-scriptengine' dependency."
        );
    }

    /**
     * Convert value to JavaScript literal.
     *
     * @param value Value to convert
     * @return JavaScript literal representation
     */
    private String toJavaScriptLiteral(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return "'" + escapeJavaScriptString((String) value) + "'";
        }
        if (value instanceof Number) {
            return value.toString();
        }
        if (value instanceof Boolean) {
            return value.toString();
        }
        return "'" + escapeJavaScriptString(value.toString()) + "'";
    }

    /**
     * Convert map to JavaScript object.
     *
     * @param map Map to convert
     * @return JavaScript object representation
     */
    private String toJavaScriptObject(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(", ");
            sb.append(entry.getKey()).append(": ").append(toJavaScriptLiteral(entry.getValue()));
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Escape string for JavaScript.
     *
     * @param str String to escape
     * @return Escaped string
     */
    private String escapeJavaScriptString(String str) {
        return str
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    /**
     * Exception thrown when custom transformer execution fails.
     */
    public static class CustomTransformerExecutionException extends RuntimeException {
        public CustomTransformerExecutionException(String message) {
            super(message);
        }

        public CustomTransformerExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
