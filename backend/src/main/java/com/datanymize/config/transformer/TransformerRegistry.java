package com.datanymize.config.transformer;

import java.util.*;

/**
 * Registry for managing transformers.
 * Provides access to built-in and custom transformers.
 */
public class TransformerRegistry {
    private final Map<String, ITransformer> transformers = new HashMap<>();

    public TransformerRegistry() {
        // Register built-in transformers
        registerTransformer(new FakeNameTransformer());
        registerTransformer(new FakeEmailTransformer());
        registerTransformer(new FakePhoneTransformer());
        registerTransformer(new HashTransformer());
        registerTransformer(new MaskTransformer());
        registerTransformer(new NullTransformer());
        registerTransformer(new ConstantTransformer());
        registerTransformer(new RandomStringTransformer());
        registerTransformer(new RandomNumberTransformer());
    }

    /**
     * Register a transformer.
     * @param transformer Transformer to register
     */
    public void registerTransformer(ITransformer transformer) {
        transformers.put(transformer.getName(), transformer);
    }

    /**
     * Get a transformer by name.
     * @param name Transformer name
     * @return Transformer or null if not found
     */
    public ITransformer getTransformer(String name) {
        return transformers.get(name);
    }

    /**
     * Check if a transformer is available.
     * @param name Transformer name
     * @return true if transformer is available
     */
    public boolean hasTransformer(String name) {
        return transformers.containsKey(name);
    }

    /**
     * Get list of available transformer names.
     * @return Set of transformer names
     */
    public Set<String> listAvailableTransformers() {
        return new HashSet<>(transformers.keySet());
    }

    /**
     * Get all available transformers.
     * @return Map of transformer name to transformer
     */
    public Map<String, ITransformer> getAllTransformers() {
        return new HashMap<>(transformers);
    }

    /**
     * Register a custom transformer from JavaScript code.
     * @param name Transformer name
     * @param code JavaScript source code
     * @throws CustomTransformerCompiler.CustomTransformerCompilationException if compilation fails
     */
    public void registerCustomJavaScriptTransformer(String name, String code) {
        CustomTransformerCompiler compiler = new CustomTransformerCompiler();
        ITransformer transformer = compiler.compile(code, CustomTransformerCompiler.Language.JAVASCRIPT);
        transformers.put(name, transformer);
    }

    /**
     * Register a custom transformer from Python code.
     * @param name Transformer name
     * @param code Python source code
     * @throws CustomTransformerCompiler.CustomTransformerCompilationException if compilation fails
     */
    public void registerCustomPythonTransformer(String name, String code) {
        CustomTransformerCompiler compiler = new CustomTransformerCompiler();
        ITransformer transformer = compiler.compile(code, CustomTransformerCompiler.Language.PYTHON);
        transformers.put(name, transformer);
    }
}
