package dev.scx.data.x;

import dev.scx.data.field_policy.*;
import dev.scx.data.x.exception.FieldPolicyToNodeException;
import dev.scx.data.x.exception.NodeToFieldPolicyException;
import dev.scx.node.*;
import dev.scx.reflect.TypeReference;

import java.util.Map;

import static dev.scx.data.field_policy.FilterMode.EXCLUDED;
import static dev.scx.node.NullNode.NULL;

/// FieldPolicyNodeConverter
///
/// - FieldPolicy -> Node 采用严格编码.
/// - Node -> FieldPolicy 采用宽松解析.
///
/// @author scx567888
public final class FieldPolicyNodeConverter {

    // ************************* FieldPolicy *************************

    /// fieldPolicy 不允许 null.
    public static Node fieldPolicyToNode(FieldPolicy fieldPolicy) throws FieldPolicyToNodeException {
        if (fieldPolicy == null) {
            throw new FieldPolicyToNodeException("fieldPolicy cannot be null");
        }

        var node = new ObjectNode();
        node.put("@type", "FieldPolicy");
        node.put("filterMode", filterModeToNode(fieldPolicy.getFilterMode()));
        node.put("fieldNames", fieldNamesToNode(fieldPolicy.getFieldNames()));
        node.put("virtualFields", virtualFieldsToNode(fieldPolicy.getVirtualFields()));
        node.put("assignFields", assignFieldsToNode(fieldPolicy.getAssignFields()));
        node.put("ignoreNull", fieldPolicy.getIgnoreNull());
        node.put("ignoreNulls", ignoreNullsToNode(fieldPolicy.getIgnoreNulls()));
        return node;
    }

    /// 宽松解析 : null / NullNode 会被解释为 FieldPolicy (包含所有)
    public static FieldPolicy nodeToFieldPolicy(Node node) throws NodeToFieldPolicyException {
        if (node == null || node == NULL) {
            return new FieldPolicyImpl(EXCLUDED);
        }

        var fieldPolicyNode = nodeToObjectNode(node, "FieldPolicy");

        var filterMode = nodeToFilterMode(fieldPolicyNode.get("filterMode"));

        var fieldPolicy = new FieldPolicyImpl(filterMode);

        switch (filterMode) {
            case INCLUDED -> fieldPolicy.include(nodeToFieldNames(fieldPolicyNode.get("fieldNames")));
            case EXCLUDED -> fieldPolicy.exclude(nodeToFieldNames(fieldPolicyNode.get("fieldNames")));
        }

        fieldPolicy.virtualFields(nodeToVirtualFields(fieldPolicyNode.get("virtualFields")));

        fieldPolicy.assignFields(nodeToAssignFields(fieldPolicyNode.get("assignFields")));

        fieldPolicy.ignoreNull(nodeToBoolean(fieldPolicyNode, "ignoreNull", true));

        var ignoreNulls = nodeToIgnoreNulls(fieldPolicyNode.get("ignoreNulls"));
        for (var entry : ignoreNulls.entrySet()) {
            fieldPolicy.ignoreNull(entry.getKey(), entry.getValue());
        }

        return fieldPolicy;
    }

    // ************************* FilterMode *************************

    /// filterMode 永不可能为 null.
    private static StringNode filterModeToNode(FilterMode filterMode) {
        return new StringNode(filterMode.name());
    }

    /// FilterMode 没有可解释的默认值, 不允许 null.
    private static FilterMode nodeToFilterMode(Node node) throws NodeToFieldPolicyException {
        if (node == null || node == NULL) {
            throw new NodeToFieldPolicyException("filterMode cannot be null");
        }
        if (!(node instanceof StringNode filterModeNode)) {
            throw new NodeToFieldPolicyException("filterMode must be StringNode");
        }
        try {
            return FilterMode.valueOf(filterModeNode.asString());
        } catch (Exception e) {
            throw new NodeToFieldPolicyException("invalid filterMode: " + filterModeNode.asString(), e);
        }
    }

    // ************************* fieldNames *************************

    /// fieldNames 永不可能为 null.
    private static Node fieldNamesToNode(String[] fieldNames) throws FieldPolicyToNodeException {
        try {
            return ConverterHelper.objectToNode(fieldNames);
        } catch (Exception e) {
            throw new FieldPolicyToNodeException("fieldNames to Node error", e);
        }
    }

    /// fieldNames 具有无歧义的默认值, null / NullNode 会被解释为 空数组.
    private static String[] nodeToFieldNames(Node fieldNamesNode) throws NodeToFieldPolicyException {
        if (fieldNamesNode == null || fieldNamesNode == NULL) {
            return new String[0];
        }
        try {
            return ConverterHelper.nodeToObject(fieldNamesNode, String[].class);
        } catch (Exception e) {
            throw new NodeToFieldPolicyException("node to fieldNames error", e);
        }
    }

    // ************************* VirtualField *************************

    /// virtualFields 永不可能为 null.
    private static ArrayNode virtualFieldsToNode(VirtualField[] virtualFields) {
        var node = new ArrayNode();
        for (var virtualField : virtualFields) {
            node.add(virtualFieldToNode(virtualField));
        }
        return node;
    }

    /// VirtualField[] 具有无歧义的默认值, null / NullNode 会被解释为 空数组.
    private static VirtualField[] nodeToVirtualFields(Node node) throws NodeToFieldPolicyException {
        if (node == null || node == NULL) {
            return new VirtualField[0];
        }

        if (!(node instanceof ArrayNode virtualFieldsNode)) {
            throw new NodeToFieldPolicyException("virtualFields must be ArrayNode");
        }

        var virtualFields = new VirtualField[virtualFieldsNode.size()];

        var i = 0;
        for (var virtualFieldNode : virtualFieldsNode) {
            virtualFields[i] = nodeToVirtualField(virtualFieldNode);
            i = i + 1;
        }

        return virtualFields;
    }

    /// virtualField 永不可能为 null.
    private static ObjectNode virtualFieldToNode(VirtualField virtualField) {
        var node = new ObjectNode();
        node.put("@type", "VirtualField");
        node.put("virtualFieldName", virtualField.virtualFieldName());
        node.put("expression", virtualField.expression());
        return node;
    }

    /// VirtualField 没有可解释的默认值, 不允许 null.
    private static VirtualField nodeToVirtualField(Node node) throws NodeToFieldPolicyException {
        if (node == null || node == NULL) {
            throw new NodeToFieldPolicyException("virtualField cannot be null");
        }

        var virtualFieldNode = nodeToObjectNode(node, "VirtualField");

        return new VirtualField(
            nodeToString(virtualFieldNode, "virtualFieldName"),
            nodeToString(virtualFieldNode, "expression")
        );
    }

    // ************************* AssignField *************************

    /// assignFields 永不可能为 null.
    private static ArrayNode assignFieldsToNode(AssignField[] assignFields) {
        var node = new ArrayNode();
        for (var assignField : assignFields) {
            node.add(assignFieldToNode(assignField));
        }
        return node;
    }

    /// AssignField[] 具有无歧义的默认值, null / NullNode 会被解释为 空数组.
    private static AssignField[] nodeToAssignFields(Node node) throws NodeToFieldPolicyException {
        if (node == null || node == NULL) {
            return new AssignField[0];
        }

        if (!(node instanceof ArrayNode assignFieldsNode)) {
            throw new NodeToFieldPolicyException("assignFields must be ArrayNode");
        }

        var assignFields = new AssignField[assignFieldsNode.size()];
        for (var i = 0; i < assignFields.length; i = i + 1) {
            assignFields[i] = nodeToAssignField(assignFieldsNode.get(i));
        }
        return assignFields;
    }

    /// assignField 永不可能为 null.
    private static ObjectNode assignFieldToNode(AssignField assignField) {
        var node = new ObjectNode();
        node.put("@type", "AssignField");
        node.put("fieldName", assignField.fieldName());
        node.put("expression", assignField.expression());
        return node;
    }

    /// AssignField 没有可解释的默认值, 不允许 null.
    private static AssignField nodeToAssignField(Node node) throws NodeToFieldPolicyException {
        if (node == null || node == NULL) {
            throw new NodeToFieldPolicyException("assignField cannot be null");
        }

        var assignFieldNode = nodeToObjectNode(node, "AssignField");

        return new AssignField(
            nodeToString(assignFieldNode, "fieldName"),
            nodeToString(assignFieldNode, "expression")
        );
    }

    // ************************* ignoreNulls *************************

    /// ignoreNulls 永不可能为 null.
    private static Node ignoreNullsToNode(Map<String, Boolean> ignoreNulls) throws FieldPolicyToNodeException {
        try {
            return ConverterHelper.objectToNode(ignoreNulls);
        } catch (Exception e) {
            throw new FieldPolicyToNodeException("ignoreNulls to Node error", e);
        }
    }

    /// ignoreNulls 具有无歧义的默认值, null / NullNode 会被解释为 空 Map.
    private static Map<String, Boolean> nodeToIgnoreNulls(Node ignoreNullsNode) throws NodeToFieldPolicyException {
        if (ignoreNullsNode == null || ignoreNullsNode == NULL) {
            return Map.of();
        }

        Map<String, Boolean> ignoreNulls;
        try {
            ignoreNulls = ConverterHelper.nodeToObject(ignoreNullsNode, new TypeReference<>() {});
        } catch (Exception e) {
            throw new NodeToFieldPolicyException("node to ignoreNulls error", e);
        }

        // 返回前 检查 一遍.
        for (var entry : ignoreNulls.entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();
            // key 和 value 都不允许 null
            if (key == null) {
                throw new NodeToFieldPolicyException("ignoreNulls key cannot be null");
            }
            if (value == null) {
                throw new NodeToFieldPolicyException("ignoreNulls value cannot be null");
            }
        }
        return ignoreNulls;
    }

    // ************************* Other *************************

    private static ObjectNode nodeToObjectNode(Node node, String expectedType) throws NodeToFieldPolicyException {
        if (!(node instanceof ObjectNode objectNode)) {
            throw new NodeToFieldPolicyException(expectedType + " must be ObjectNode");
        }

        var typeNode = objectNode.get("@type");

        if (typeNode == null || typeNode == NULL) {
            throw new NodeToFieldPolicyException(expectedType + " @type is missing");
        }

        if (!(typeNode instanceof StringNode type)) {
            throw new NodeToFieldPolicyException(expectedType + " @type must be StringNode");
        }

        if (!expectedType.equals(type.asString())) {
            throw new NodeToFieldPolicyException("Expected @type " + expectedType + ", but got " + type.asString());
        }

        return objectNode;
    }

    /// 不允许 null / NullNode.
    private static String nodeToString(ObjectNode objectNode, String name) throws NodeToFieldPolicyException {
        var node = objectNode.get(name);
        if (node == null || node == NULL) {
            throw new NodeToFieldPolicyException(name + " cannot be null");
        }
        if (!(node instanceof StringNode stringNode)) {
            throw new NodeToFieldPolicyException(name + " must be StringNode");
        }
        return stringNode.asString();
    }

    /// null / NullNode 默认解释为 defaultValue
    private static boolean nodeToBoolean(ObjectNode objectNode, String name, boolean defaultValue) throws NodeToFieldPolicyException {
        var node = objectNode.get(name);
        if (node == null || node == NULL) {
            return defaultValue;
        }
        if (!(node instanceof ValueNode valueNode)) {
            throw new NodeToFieldPolicyException(name + " must be ValueNode");
        }
        return valueNode.asBoolean();
    }

}
