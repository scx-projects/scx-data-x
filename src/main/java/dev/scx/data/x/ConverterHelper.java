package dev.scx.data.x;

import dev.scx.node.Node;
import dev.scx.object.NodeToObjectException;
import dev.scx.object.ObjectToNodeException;
import dev.scx.object.x.DefaultObjectNodeConvertConfig;
import dev.scx.object.x.DefaultObjectNodeConvertOptions;
import dev.scx.reflect.TypeReference;

import static dev.scx.object.x.DefaultObjectNodeConverter.DEFAULT_OBJECT_NODE_CONVERTER;

final class ConverterHelper {

    private static final DefaultObjectNodeConvertOptions DEFAULT_OBJECT_NODE_CONVERT_OPTIONS = DefaultObjectNodeConvertConfig.of();

    public static Node objectToNode(Object value) throws ObjectToNodeException {
        return DEFAULT_OBJECT_NODE_CONVERTER.objectToNode(value, DEFAULT_OBJECT_NODE_CONVERT_OPTIONS);
    }

    public static <T> T nodeToObject(Node node, Class<T> clazz) throws NodeToObjectException {
        return DEFAULT_OBJECT_NODE_CONVERTER.nodeToObject(node, clazz, DEFAULT_OBJECT_NODE_CONVERT_OPTIONS);
    }

    public static <T> T nodeToObject(Node node, TypeReference<T> type) throws NodeToObjectException {
        return DEFAULT_OBJECT_NODE_CONVERTER.nodeToObject(node, type, DEFAULT_OBJECT_NODE_CONVERT_OPTIONS);
    }

}
