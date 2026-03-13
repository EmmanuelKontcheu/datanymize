package com.datanymize.config.transformer;

import java.util.Map;

/**
 * Compiler for custom transformers written in JavaScript or Python.
 * Provides sandboxed execution environment for custom transformation code.
 *
 * Requirements: 4.5
 */
public class CustomTransformerCompiler {

    /**
     * Supported languages for custom transformers.
     */
    public enum Language {
        JAVASCRIPT,
        PYTHON
    }

    /**
     * Compile a custom transformer from source code.
     *
     * @param code Source code of the transformer function
     * @param language Language of the source code (JAVASCRIPT or PYTHON)
     * @return Compiled transformer
     * @throws CustomTransformerCompilationException if compilation fails
     */
    public ITransformer compile(String code, Language language) {
        if (code == null || code.trim().isEmpty()) {
            throw new CustomTransformerCompilationException("Transformer code cannot be empty");
        }

        if (language == null) {
            throw new CustomTransformerCompilationException("Language must be specified");
        }

        switch (language) {
            case JAVASCRIPT:
                return compileJavaScript(code);
            case PYTHON:
                return compilePython(code);
            default:
                throw new CustomTransformerCompilationException("Unsupported language: " + language);
        }
    }

    /**
     * Compile a JavaScript transformer.
     *
     * @param code JavaScript source code
     * @return Compiled transformer
     * @throws CustomTransformerCompilationException if compilation fails
     */
    private ITransformer compileJavaScript(String code) {
        try {
            // Validate JavaScript syntax
            validateJavaScriptSyntax(code);

            // Create sandboxed JavaScript transformer
            return new JavaScriptTransformer(code);
        } catch (Exception e) {
            throw new CustomTransformerCompilationException(
                "Failed to compile JavaScript transformer: " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Compile a Python transformer.
     *
     * @param code Python source code
     * @return Compiled transformer
     * @throws CustomTransformerCompilationException if compilation fails
     */
    private ITransformer compilePython(String code) {
        try {
            // Validate Python syntax
            validatePythonSyntax(code);

            // Create sandboxed Python transformer
            return new PythonTransformer(code);
        } catch (Exception e) {
            throw new CustomTransformerCompilationException(
                "Failed to compile Python transformer: " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Validate JavaScript syntax.
     *
     * @param code JavaScript source code
     * @throws CustomTransformerCompilationException if syntax is invalid
     */
    private void validateJavaScriptSyntax(String code) {
        // Check for required function definition
        if (!code.contains("function") && !code.contains("=>")) {
            throw new CustomTransformerCompilationException(
                "JavaScript transformer must define a function"
            );
        }

        // Check for balanced braces
        int braceCount = 0;
        for (char c : code.toCharArray()) {
            if (c == '{') braceCount++;
            if (c == '}') braceCount--;
        }
        if (braceCount != 0) {
            throw new CustomTransformerCompilationException(
                "JavaScript transformer has unbalanced braces"
            );
        }
    }

    /**
     * Validate Python syntax.
     *
     * @param code Python source code
     * @throws CustomTransformerCompilationException if syntax is invalid
     */
    private void validatePythonSyntax(String code) {
        // Check for required function definition
        if (!code.contains("def ")) {
            throw new CustomTransformerCompilationException(
                "Python transformer must define a function with 'def'"
            );
        }

        // Check for proper indentation
        String[] lines = code.split("\n");
        boolean inFunction = false;
        for (String line : lines) {
            if (line.contains("def ")) {
                inFunction = true;
            }
            if (inFunction && !line.trim().isEmpty() && !line.startsWith(" ") && !line.startsWith("\t")) {
                if (!line.contains("def ")) {
                    throw new CustomTransformerCompilationException(
                        "Python transformer function body must be indented"
                    );
                }
            }
        }
    }

    /**
     * Exception thrown when custom transformer compilation fails.
     */
    public static class CustomTransformerCompilationException extends RuntimeException {
        public CustomTransformerCompilationException(String message) {
            super(message);
        }

        public CustomTransformerCompilationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
