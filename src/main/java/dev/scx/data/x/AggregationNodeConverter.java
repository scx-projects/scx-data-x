package dev.scx.data.x;

import dev.scx.data.aggregation.*;
import dev.scx.data.x.exception.AggregationToNodeException;
import dev.scx.data.x.exception.NodeToAggregationException;
import dev.scx.node.ArrayNode;
import dev.scx.node.Node;
import dev.scx.node.ObjectNode;
import dev.scx.node.StringNode;

import static dev.scx.node.NullNode.NULL;

/// AggregationNodeConverter
///
/// - Aggregation -> Node 采用严格编码.
/// - Node -> Aggregation 采用宽松解析.
///
/// @author scx567888
public final class AggregationNodeConverter {

    // ************************* Aggregation *************************

    /// aggregation 不允许 null.
    public static Node aggregationToNode(Aggregation aggregation) throws AggregationToNodeException {
        if (aggregation == null) {
            throw new AggregationToNodeException("aggregation cannot be null");
        }

        var node = new ObjectNode();
        node.put("@type", "Aggregation");
        node.put("groupBys", groupBysToNode(aggregation.getGroupBys()));
        node.put("aggs", aggsToNode(aggregation.getAggs()));
        return node;
    }

    /// 宽松解析 : null / NullNode 会被解释为 空 Aggregation
    public static Aggregation nodeToAggregation(Node node) throws NodeToAggregationException {
        if (node == null || node == NULL) {
            return new AggregationImpl();
        }

        var aggregationNode = nodeToObjectNode(node, "Aggregation");

        var aggregation = new AggregationImpl();
        aggregation.groupBys(nodeToGroupBys(aggregationNode.get("groupBys")));
        aggregation.aggs(nodeToAggs(aggregationNode.get("aggs")));
        return aggregation;
    }

    // ************************* GroupBy *************************

    /// groupBys 永不可能为 null.
    private static ArrayNode groupBysToNode(GroupBy[] groupBys) {
        var node = new ArrayNode();
        for (var groupBy : groupBys) {
            node.add(groupByToNode(groupBy));
        }
        return node;
    }

    /// groupBy 永不可能为 null.
    private static ObjectNode groupByToNode(GroupBy groupBy) {
        return switch (groupBy) {
            case FieldGroupBy f -> fieldGroupByToNode(f);
            case ExpressionGroupBy e -> expressionGroupByToNode(e);
        };
    }

    /// fieldGroupBy 永不可能为 null.
    private static ObjectNode fieldGroupByToNode(FieldGroupBy fieldGroupBy) {
        var node = new ObjectNode();
        node.put("@type", "FieldGroupBy");
        node.put("fieldName", fieldGroupBy.fieldName());
        return node;
    }

    /// expressionGroupBy 永不可能为 null.
    private static ObjectNode expressionGroupByToNode(ExpressionGroupBy expressionGroupBy) {
        var node = new ObjectNode();
        node.put("@type", "ExpressionGroupBy");
        node.put("alias", expressionGroupBy.alias());
        node.put("expression", expressionGroupBy.expression());
        return node;
    }

    /// GroupBy[] 具有无歧义的默认值, null / NullNode 会被解释为 空数组.
    private static GroupBy[] nodeToGroupBys(Node node) throws NodeToAggregationException {
        if (node == null || node == NULL) {
            return new GroupBy[0];
        }

        if (!(node instanceof ArrayNode groupBysNode)) {
            throw new NodeToAggregationException("groupBys must be ArrayNode");
        }

        var groupBys = new GroupBy[groupBysNode.size()];
        for (var i = 0; i < groupBys.length; i = i + 1) {
            groupBys[i] = nodeToGroupBy(groupBysNode.get(i));
        }
        return groupBys;
    }

    /// GroupBy 没有可解释的默认值, 不允许 null.
    private static GroupBy nodeToGroupBy(Node node) throws NodeToAggregationException {
        if (node == null || node == NULL) {
            throw new NodeToAggregationException("groupBy cannot be null");
        }

        // 0, 检查类型
        if (!(node instanceof ObjectNode groupByNode)) {
            throw new NodeToAggregationException("groupBy must be ObjectNode");
        }

        // 1, 检查 @type
        var typeNode = groupByNode.get("@type");

        if (typeNode == null || typeNode == NULL) {
            throw new NodeToAggregationException("groupBy @type is missing");
        }

        if (!(typeNode instanceof StringNode type)) {
            throw new NodeToAggregationException("groupBy @type must be StringNode");
        }

        return switch (type.asString()) {
            case "FieldGroupBy" -> nodeToFieldGroupBy(groupByNode);
            case "ExpressionGroupBy" -> nodeToExpressionGroupBy(groupByNode);
            default -> throw new NodeToAggregationException("Unknown GroupBy type: " + type.asString());
        };
    }

    /// fieldGroupByNode 永不可能为 null.
    private static FieldGroupBy nodeToFieldGroupBy(ObjectNode fieldGroupByNode) throws NodeToAggregationException {
        return new FieldGroupBy(
            nodeToString(fieldGroupByNode, "fieldName")
        );
    }

    /// expressionGroupByNode 永不可能为 null.
    private static ExpressionGroupBy nodeToExpressionGroupBy(ObjectNode expressionGroupByNode) throws NodeToAggregationException {
        return new ExpressionGroupBy(
            nodeToString(expressionGroupByNode, "alias"),
            nodeToString(expressionGroupByNode, "expression")
        );
    }

    // ************************* Agg *************************

    /// aggs 永不可能为 null.
    private static ArrayNode aggsToNode(Agg[] aggs) {
        var node = new ArrayNode();
        for (var agg : aggs) {
            node.add(aggToNode(agg));
        }
        return node;
    }

    /// Agg[] 具有无歧义的默认值, null / NullNode 会被解释为 空数组.
    private static Agg[] nodeToAggs(Node node) throws NodeToAggregationException {
        if (node == null || node == NULL) {
            return new Agg[0];
        }

        if (!(node instanceof ArrayNode aggsNode)) {
            throw new NodeToAggregationException("aggs must be ArrayNode");
        }

        var aggs = new Agg[aggsNode.size()];
        for (int i = 0; i < aggs.length; i = i + 1) {
            aggs[i] = nodeToAgg(aggsNode.get(i));
        }
        return aggs;
    }

    /// agg 永不可能为 null.
    private static ObjectNode aggToNode(Agg agg) {
        var node = new ObjectNode();
        node.put("@type", "Agg");
        node.put("alias", agg.alias());
        node.put("expression", agg.expression());
        return node;
    }

    /// Agg 没有可解释的默认值, 不允许 null.
    private static Agg nodeToAgg(Node node) throws NodeToAggregationException {
        if (node == null || node == NULL) {
            throw new NodeToAggregationException("agg cannot be null");
        }

        var aggNode = nodeToObjectNode(node, "Agg");

        return new Agg(
            nodeToString(aggNode, "alias"),
            nodeToString(aggNode, "expression")
        );
    }

    // ************************* Other *************************

    private static ObjectNode nodeToObjectNode(Node node, String expectedType) throws NodeToAggregationException {
        if (!(node instanceof ObjectNode objectNode)) {
            throw new NodeToAggregationException(expectedType + " must be ObjectNode");
        }

        var typeNode = objectNode.get("@type");

        if (typeNode == null || typeNode == NULL) {
            throw new NodeToAggregationException(expectedType + " @type is missing");
        }

        if (!(typeNode instanceof StringNode type)) {
            throw new NodeToAggregationException(expectedType + " @type must be StringNode");
        }

        if (!expectedType.equals(type.asString())) {
            throw new NodeToAggregationException("Expected @type " + expectedType + ", but got " + type.asString());
        }

        return objectNode;
    }

    /// 不允许 null / NullNode.
    private static String nodeToString(ObjectNode objectNode, String name) throws NodeToAggregationException {
        var node = objectNode.get(name);
        if (node == null || node == NULL) {
            throw new NodeToAggregationException(name + " cannot be null");
        }
        if (!(node instanceof StringNode stringNode)) {
            throw new NodeToAggregationException(name + " must be StringNode");
        }
        return stringNode.asString();
    }

}
