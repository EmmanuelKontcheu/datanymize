package com.datanymize.config.transformer;

import java.util.Map;

/**
 * Sandboxed Python transformer implementation.
 * Executes custom Python code in a restricted environment.
 *
 * Requirements: 4.5
 */
public class PythonTransformer implements ITransformer {
    private final String code;
    private final String name;

    /**
     * Create a Python transformer.
     *
     * @param code Python source code
     */
    public PythonTransformer(String code) {
        this.code = code;
        this.name = "custom_python_" + System.identityHashCode(this);
    }

    @Override
    public Object transform(Object value, Map<String, Object> params) {
        try {
            return executePython(value, params, null);
        } catch (Exception e) {
            throw new CustomTransformerExecutionException(
                "Failed to execute Python transformer: " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public Object transformDeterministic(Object value, String seed, Map<String, Object> params) {
        try {
            return executePython(value, params, seed);
        } catch (Exception e) {
            throw new CustomTransformerExecutionException(
                "Failed to execute Python transformer: " + e.getMessage(),
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
     * Execute Python code in sandboxed environment.
     *
     * @param value Input value to transform
     * @param params Transformer parameters
     * @param seed Optional seed for deterministic transformation
     * @return Transformed value
     * @throws Exception if execution fails
     */
    private Object executePython(Object value, Map<String, Object> params, String seed) throws Exception {
        // Build execution context
        StringBuilder context = new StringBuilder();
        context.append("import json\n");
        context.append("value = ").append(toPythonLiteral(value)).append("\n");
        context.append("params = ").append(toPythonDict(params)).append("\n");
        if (seed != null) {
            context.append("seed = '").append(escapePythonString(seed)).append("'\n");
        }
        context.append(code).append("\n");

        // Execute Python (placeholder - would use Jython/GraalVM in production)
        return executeInSandbox(context.toString());
    }

    /**
     * Execute code in sandboxed environment.
     * This is a placeholder implementation.
     * In production, use Jython or GraalVM Python.
     *
     * @param code Python code to execute
     * @return Result of execution
     * @throws Exception if execution fails
     */
    private Object executeInSandbox(String code) throws Exception {
        // Placeholder: In production, use:
        // - Jython (Python 2.7 compatible)
        // - GraalVM Python (Python 3 compatible)

        // For now, throw informative error
        throw new UnsupportedOperationException(
            "Python execution requires Jython or GraalVM Python. " +
            "Please add 'org.python:jython-standalone' or 'org.graalvm.python:python' dependency."
        );
    }

    /**
     * Convert value to Python literal.
     *
     * @param value Value to convert
     * @return Python literal representation
     */
    private String toPythonLiteral(Object value) {
        if (value == null) {
            return "None";
        }
        if (value instanceof String) {
            return "'" + escapePythonString((String) value) + "'";
        }
        if (value instanceof Number) {
            return value.toString();
        }
        if (value instanceof Boolean) {
            return ((Boolean) value) ? "True" : "False";
        }
        return "'" + escapePythonString(value.toString()) + "'";
    }

    /**
     * Convert map to Python dictionary.
     *
     * @param map Map to convert
     * @return Python dictionary representation
     */
    private String toPythonDict(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(", ");
            sb.append("'").append(entry.getKey()).append("': ").append(toPythonLiteral(entry.getValue()));
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Escape string for Python.
     *
     * @param str String to escape
     * @return Escaped string
     */
    private String escapePythonString(String str) {
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
