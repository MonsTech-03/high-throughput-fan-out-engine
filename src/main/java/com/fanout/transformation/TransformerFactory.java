package com.fanout.transformation;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory pattern for creating transformers
 */
public class TransformerFactory {
    private static final Map<String, DataTransformer> transformers = new HashMap<>();

    static {
        registerTransformer(new JsonTransformer());
        registerTransformer(new XmlTransformer());
        registerTransformer(new ProtobufTransformer());
        registerTransformer(new AvroTransformer());
    }

    private static void registerTransformer(DataTransformer transformer) {
        transformers.put(transformer.getType(), transformer);
    }

    public static DataTransformer getTransformer(String type) {
        DataTransformer transformer = transformers.get(type.toUpperCase());
        if (transformer == null) {
            throw new IllegalArgumentException("Unknown transformer type: " + type);
        }
        return transformer;
    }

    public static void registerCustomTransformer(DataTransformer transformer) {
        registerTransformer(transformer);
    }
}
