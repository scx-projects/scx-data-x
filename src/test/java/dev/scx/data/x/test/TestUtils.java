package dev.scx.data.x.test;

import dev.scx.node.*;

import static dev.scx.node.NullNode.NULL;
import static org.testng.Assert.*;

final class TestUtils {

    static ObjectNode assertObjectNode(Node node) {
        assertTrue(node instanceof ObjectNode, "node must be ObjectNode");
        return (ObjectNode) node;
    }

    static ArrayNode assertArrayNode(Node node) {
        assertTrue(node instanceof ArrayNode, "node must be ArrayNode");
        return (ArrayNode) node;
    }

    static void assertType(ObjectNode node, String expectedType) {
        var typeNode = node.get("@type");
        assertTrue(typeNode instanceof StringNode, "@type must be StringNode");
        assertEquals(((StringNode) typeNode).asString(), expectedType);
    }

    static String assertString(ObjectNode node, String name) {
        var valueNode = node.get(name);
        assertTrue(valueNode instanceof StringNode, name + " must be StringNode");
        return ((StringNode) valueNode).asString();
    }

    static boolean assertBoolean(ObjectNode node, String name) {
        var valueNode = node.get(name);
        assertTrue(valueNode instanceof ValueNode, name + " must be ValueNode");
        return ((ValueNode) valueNode).asBoolean();
    }

    static long assertLong(ObjectNode node, String name) {
        var valueNode = node.get(name);
        assertTrue(valueNode instanceof ValueNode, name + " must be ValueNode");
        return ((ValueNode) valueNode).asLong();
    }

    static void assertNullNode(ObjectNode node, String name) {
        assertSame(node.get(name), NULL, name + " must be NullNode.NULL");
    }

    static void assertNumber(Object value, long expectedValue) {
        assertTrue(value instanceof Number, "value must be Number");
        assertEquals(((Number) value).longValue(), expectedValue);
    }

}
