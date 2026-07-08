package dev.scx.data.x.test;

import dev.scx.data.field_policy.*;
import dev.scx.data.x.exception.FieldPolicyToNodeException;
import dev.scx.data.x.exception.NodeToFieldPolicyException;
import dev.scx.node.ArrayNode;
import dev.scx.node.ObjectNode;
import org.testng.annotations.Test;

import static dev.scx.data.field_policy.FieldPolicyBuilder.*;
import static dev.scx.data.x.FieldPolicyNodeConverter.fieldPolicyToNode;
import static dev.scx.data.x.FieldPolicyNodeConverter.nodeToFieldPolicy;
import static dev.scx.data.x.test.TestUtils.*;
import static dev.scx.node.NullNode.NULL;
import static org.testng.Assert.*;

public class FieldPolicyNodeConverterTest {

    public static void main(String[] args) {
        fieldPolicyToNodeShouldEncodeIncludeAllStrictly();
        nodeToFieldPolicyShouldDecodeNullAsIncludeAll();
        nodeToFieldPolicyShouldDecodeMissingOptionalMembersAsDefaults();
        fieldPolicyShouldRoundTripComplexPolicy();
        fieldPolicyToNodeShouldEncodeVirtualAndAssignFieldsStrictly();
        fieldPolicyToNodeShouldRejectNullFieldPolicy();
        nodeToFieldPolicyShouldRejectMissingFilterMode();
        nodeToFieldPolicyShouldRejectInvalidFilterMode();
        nodeToFieldPolicyShouldRejectNullVirtualFieldElement();
        nodeToFieldPolicyShouldRejectNullIgnoreNullsValue();
    }

    @Test
    public static void fieldPolicyToNodeShouldEncodeIncludeAllStrictly() {
        var node = assertObjectNode(fieldPolicyToNode(includeAll()));

        assertType(node, "FieldPolicy");
        assertEquals(assertString(node, "filterMode"), "EXCLUDED");
        assertEquals(assertArrayNode(node.get("fieldNames")).size(), 0);
        assertEquals(assertArrayNode(node.get("virtualFields")).size(), 0);
        assertEquals(assertArrayNode(node.get("assignFields")).size(), 0);
        assertTrue(assertBoolean(node, "ignoreNull"));
        assertEquals(assertObjectNode(node.get("ignoreNulls")).size(), 0);
    }

    @Test
    public static void nodeToFieldPolicyShouldDecodeNullAsIncludeAll() {
        assertIncludeAll(nodeToFieldPolicy(null));
        assertIncludeAll(nodeToFieldPolicy(NULL));
    }

    @Test
    public static void nodeToFieldPolicyShouldDecodeMissingOptionalMembersAsDefaults() {
        var node = new ObjectNode();
        node.put("@type", "FieldPolicy");
        node.put("filterMode", "EXCLUDED");

        var fieldPolicy = nodeToFieldPolicy(node);

        assertIncludeAll(fieldPolicy);
    }

    @Test
    public static void fieldPolicyShouldRoundTripComplexPolicy() {
        var fieldPolicy = include("id", "name")
            .virtualField("displayName", "concat(first_name, ' ', last_name)")
            .assignField("updatedAt", "now()")
            .ignoreNull(false)
            .ignoreNull("nickname", true)
            .ignoreNull("avatar", false);

        var roundTripped = nodeToFieldPolicy(fieldPolicyToNode(fieldPolicy));

        assertEquals(roundTripped.getFilterMode(), FilterMode.INCLUDED);
        assertEquals(roundTripped.getFieldNames(), new String[]{"id", "name"});
        assertFalse(roundTripped.getIgnoreNull());
        assertEquals(roundTripped.getIgnoreNulls().size(), 2);
        assertEquals(roundTripped.getIgnoreNulls().get("nickname"), Boolean.TRUE);
        assertEquals(roundTripped.getIgnoreNulls().get("avatar"), Boolean.FALSE);

        var virtualFields = roundTripped.getVirtualFields();
        assertEquals(virtualFields.length, 1);
        assertEquals(virtualFields[0].virtualFieldName(), "displayName");
        assertEquals(virtualFields[0].expression(), "concat(first_name, ' ', last_name)");

        var assignFields = roundTripped.getAssignFields();
        assertEquals(assignFields.length, 1);
        assertEquals(assignFields[0].fieldName(), "updatedAt");
        assertEquals(assignFields[0].expression(), "now()");
    }

    @Test
    public static void fieldPolicyToNodeShouldEncodeVirtualAndAssignFieldsStrictly() {
        var node = assertObjectNode(fieldPolicyToNode(
            exclude("password")
                .virtualFields(new VirtualField("ageText", "cast(age as text)"))
                .assignFields(new AssignField("deletedAt", "now()"))
        ));

        assertType(node, "FieldPolicy");
        assertEquals(assertString(node, "filterMode"), "EXCLUDED");

        var virtualFields = assertArrayNode(node.get("virtualFields"));
        assertEquals(virtualFields.size(), 1);
        var virtualField = assertObjectNode(virtualFields.get(0));
        assertType(virtualField, "VirtualField");
        assertEquals(assertString(virtualField, "virtualFieldName"), "ageText");
        assertEquals(assertString(virtualField, "expression"), "cast(age as text)");

        var assignFields = assertArrayNode(node.get("assignFields"));
        assertEquals(assignFields.size(), 1);
        var assignField = assertObjectNode(assignFields.get(0));
        assertType(assignField, "AssignField");
        assertEquals(assertString(assignField, "fieldName"), "deletedAt");
        assertEquals(assertString(assignField, "expression"), "now()");
    }

    @Test
    public static void fieldPolicyToNodeShouldRejectNullFieldPolicy() {
        assertThrows(FieldPolicyToNodeException.class, () -> {
            fieldPolicyToNode(null);
        });
    }

    @Test
    public static void nodeToFieldPolicyShouldRejectMissingFilterMode() {
        assertThrows(NodeToFieldPolicyException.class, () -> {
            var node = new ObjectNode();
            node.put("@type", "FieldPolicy");

            nodeToFieldPolicy(node);
        });
    }

    @Test
    public static void nodeToFieldPolicyShouldRejectInvalidFilterMode() {
        assertThrows(NodeToFieldPolicyException.class, () -> {
            var node = new ObjectNode();
            node.put("@type", "FieldPolicy");
            node.put("filterMode", "UNKNOWN");

            nodeToFieldPolicy(node);
        });
    }

    @Test
    public static void nodeToFieldPolicyShouldRejectNullVirtualFieldElement() {
        assertThrows(NodeToFieldPolicyException.class, () -> {
            var virtualFields = new ArrayNode();
            virtualFields.add(NULL);

            var node = new ObjectNode();
            node.put("@type", "FieldPolicy");
            node.put("filterMode", "EXCLUDED");
            node.put("virtualFields", virtualFields);

            nodeToFieldPolicy(node);
        });
    }

    @Test
    public static void nodeToFieldPolicyShouldRejectNullIgnoreNullsValue() {
        assertThrows(NodeToFieldPolicyException.class, () -> {
            var ignoreNulls = new ObjectNode();
            ignoreNulls.put("name", NULL);

            var node = new ObjectNode();
            node.put("@type", "FieldPolicy");
            node.put("filterMode", "EXCLUDED");
            node.put("ignoreNulls", ignoreNulls);

            nodeToFieldPolicy(node);
        });
    }

    private static void assertIncludeAll(FieldPolicy fieldPolicy) {
        assertTrue(fieldPolicy instanceof FieldPolicyImpl);
        assertEquals(fieldPolicy.getFilterMode(), FilterMode.EXCLUDED);
        assertEquals(fieldPolicy.getFieldNames().length, 0);
        assertEquals(fieldPolicy.getVirtualFields().length, 0);
        assertEquals(fieldPolicy.getAssignFields().length, 0);
        assertTrue(fieldPolicy.getIgnoreNull());
        assertTrue(fieldPolicy.getIgnoreNulls().isEmpty());
    }

}
