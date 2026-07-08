package dev.scx.data.x;

import dev.scx.data.query.*;
import dev.scx.data.x.exception.NodeToQueryException;
import dev.scx.data.x.exception.QueryToNodeException;
import dev.scx.node.*;

import static dev.scx.node.NullNode.NULL;

/// QueryNodeConverter
///
/// - Query -> Node 采用严格编码.
/// - Node -> Query 采用宽松解析.
///
/// @author scx567888
public final class QueryNodeConverter {

    // ************************* Query *************************

    /// query 不允许 null.
    public static Node queryToNode(Query query) throws QueryToNodeException {
        if (query == null) {
            throw new QueryToNodeException("query cannot be null");
        }

        var node = new ObjectNode();
        node.put("@type", "Query");
        node.put("where", whereToNode(query.getWhere()));
        node.put("orderBys", orderBysToNode(query.getOrderBys()));
        node.put("offset", longToNode(query.getOffset()));
        node.put("limit", longToNode(query.getLimit()));
        return node;
    }

    /// 宽松解析 : null / NullNode 会被解释为 空 Query
    public static Query nodeToQuery(Node node) throws NodeToQueryException {
        if (node == null || node == NULL) {
            return new QueryImpl();
        }

        var queryNode = nodeToObjectNode(node, "Query");

        var query = new QueryImpl();

        query.where(nodeToWhere(queryNode.get("where")));
        query.orderBys(nodeToOrderBys(queryNode.get("orderBys")));

        var offset = nodeToLong(queryNode, "offset");
        // query.offset() 不支持传入 null, 这里跳过
        if (offset != null) {
            query.offset(offset);
        }

        var limit = nodeToLong(queryNode, "limit");
        // query.limit() 不支持传入 null, 这里跳过
        if (limit != null) {
            query.limit(limit);
        }

        return query;
    }

    /// null 保持为 NullNode
    private static Node longToNode(Long value) {
        if (value == null) {
            return NULL;
        }

        return new LongNode(value);
    }

    /// null / NullNode 会被解释为 null
    private static Long nodeToLong(ObjectNode objectNode, String name) throws NodeToQueryException {
        var node = objectNode.get(name);

        if (node == null || node == NULL) {
            return null;
        }

        if (!(node instanceof ValueNode valueNode)) {
            throw new NodeToQueryException(name + " must be ValueNode");
        }

        long value;
        try {
            value = valueNode.asLong();
        } catch (Exception e) {
            throw new NodeToQueryException(name + " format error", e);
        }

        // 范围检查 offset 和 limit 都需要大于等于 0.
        if (value < 0) {
            throw new NodeToQueryException(name + " must be >= 0");
        }
        return value;
    }

    // ************************* Where *************************

    /// where 可能是 null, 保持为 NullNode
    private static Node whereToNode(Where where) throws QueryToNodeException {
        return switch (where) {
            case Condition c -> conditionToNode(c);
            case And a -> andToNode(a);
            case Or o -> orToNode(o);
            case Not n -> notToNode(n);
            case WhereClause w -> whereClauseToNode(w);
            case null -> NULL;
            default -> throw new QueryToNodeException("Unknown Where type: " + where.getClass());
        };
    }

    /// null / NullNode 解释为 null.
    private static Where nodeToWhere(Node node) throws NodeToQueryException {
        if (node == null || node == NULL) {
            return null;
        }

        // 1, 检查 node 类型
        if (!(node instanceof ObjectNode whereNode)) {
            throw new NodeToQueryException("where must be ObjectNode");
        }

        // 2, 检查 @type
        var typeNode = whereNode.get("@type");

        // 2.1, 检查 @type 是否存在
        if (typeNode == null || typeNode == NULL) {
            throw new NodeToQueryException("where @type is missing");
        }

        // 2.2, 检查 @type 格式是否正确
        if (!(typeNode instanceof StringNode type)) {
            throw new NodeToQueryException("where @type must be StringNode");
        }

        // 3, 根据 @type 值分发
        return switch (type.asString()) {
            case "Condition" -> nodeToCondition(whereNode);
            case "And" -> nodeToAnd(whereNode);
            case "Or" -> nodeToOr(whereNode);
            case "Not" -> nodeToNot(whereNode);
            case "WhereClause" -> nodeToWhereClause(whereNode);
            default -> throw new NodeToQueryException("Unknown Where type: " + type.asString());
        };
    }

    // ************************* OrderBy *************************

    /// orderBys 永不可能为 null.
    private static ArrayNode orderBysToNode(OrderBy[] orderBys) {
        var node = new ArrayNode();
        for (var orderBy : orderBys) {
            node.add(orderByToNode(orderBy));
        }
        return node;
    }

    /// OrderBy[] 具有无歧义的默认值, null / NullNode 会被解释为 空数组.
    private static OrderBy[] nodeToOrderBys(Node node) throws NodeToQueryException {
        if (node == null || node == NULL) {
            return new OrderBy[0];
        }

        if (!(node instanceof ArrayNode orderBysNode)) {
            throw new NodeToQueryException("orderBys must be ArrayNode");
        }

        var orderBys = new OrderBy[orderBysNode.size()];
        for (var i = 0; i < orderBys.length; i = i + 1) {
            orderBys[i] = nodeToOrderBy(orderBysNode.get(i));
        }
        return orderBys;
    }

    /// orderBy 永不可能为 null.
    private static ObjectNode orderByToNode(OrderBy orderBy) {
        var node = new ObjectNode();
        node.put("@type", "OrderBy");
        node.put("selector", orderBy.selector());
        node.put("orderByType", orderByTypeToNode(orderBy.orderByType()));
        node.put("useExpression", orderBy.useExpression());
        return node;
    }

    /// OrderBy 没有可解释的默认值, 不允许 null.
    private static OrderBy nodeToOrderBy(Node node) throws NodeToQueryException {
        if (node == null || node == NULL) {
            throw new NodeToQueryException("orderBy cannot be null");
        }

        var orderByNode = nodeToObjectNode(node, "OrderBy");

        return new OrderBy(
            nodeToString(orderByNode, "selector"),
            nodeToOrderByType(orderByNode.get("orderByType")),
            nodeToBoolean(orderByNode, "useExpression", false)
        );
    }

    /// orderByType 永不可能为 null.
    private static Node orderByTypeToNode(OrderByType orderByType) {
        return new StringNode(orderByType.name());
    }

    /// OrderByType 没有可解释的默认值, 所以无论是 编码 还是 解码 都不允许 null.
    private static OrderByType nodeToOrderByType(Node node) throws NodeToQueryException {
        if (node == null || node == NULL) {
            throw new NodeToQueryException("orderByType cannot be null");
        }
        if (!(node instanceof StringNode orderByTypeNode)) {
            throw new NodeToQueryException("orderByType must be StringNode");
        }
        try {
            return OrderByType.valueOf(orderByTypeNode.asString());
        } catch (Exception e) {
            throw new NodeToQueryException("invalid orderByType: " + orderByTypeNode.asString(), e);
        }
    }

    // ************************* Condition *************************

    /// condition 永不可能为 null.
    private static ObjectNode conditionToNode(Condition condition) throws QueryToNodeException {
        var node = new ObjectNode();
        node.put("@type", "Condition");
        node.put("selector", condition.selector());
        node.put("conditionType", conditionTypeToNode(condition.conditionType()));
        node.put("value1", valueToNode(condition.value1()));
        node.put("value2", valueToNode(condition.value2()));
        node.put("useExpression", condition.useExpression());
        node.put("useExpressionValue", condition.useExpressionValue());
        node.put("skipIfInfo", skipIfInfoToNode(condition.skipIfInfo()));
        return node;
    }

    /// conditionNode 永不可能为 null.
    private static Condition nodeToCondition(ObjectNode conditionNode) throws NodeToQueryException {
        return new Condition(
            nodeToString(conditionNode, "selector"),
            nodeToConditionType(conditionNode.get("conditionType")),
            nodeToValue(conditionNode.get("value1")),
            nodeToValue(conditionNode.get("value2")),
            nodeToBoolean(conditionNode, "useExpression", false),
            nodeToBoolean(conditionNode, "useExpressionValue", false),
            nodeToSkipIfInfo(conditionNode.get("skipIfInfo"))
        );
    }

    /// conditionType 永不可能为 null.
    private static StringNode conditionTypeToNode(ConditionType conditionType) {
        return new StringNode(conditionType.name());
    }

    /// ConditionType 没有可解释的默认值, 不允许 null.
    private static ConditionType nodeToConditionType(Node node) throws NodeToQueryException {
        if (node == null || node == NULL) {
            throw new NodeToQueryException("conditionType cannot be null");
        }
        if (!(node instanceof StringNode conditionTypeNode)) {
            throw new NodeToQueryException("conditionType must be StringNode");
        }
        try {
            return ConditionType.valueOf(conditionTypeNode.asString());
        } catch (Exception e) {
            throw new NodeToQueryException("invalid conditionType: " + conditionTypeNode.asString(), e);
        }
    }

    /// value1 / value2 具有无歧义的默认值 null
    private static Node valueToNode(Object object) throws QueryToNodeException {
        if (object == null) {
            return NULL;
        }
        try {
            return ConverterHelper.objectToNode(object);
        } catch (Exception e) {
            throw new QueryToNodeException("value to Node error", e);
        }
    }

    /// value1 / value2 具有无歧义的默认值 null
    private static Object nodeToValue(Node node) throws NodeToQueryException {
        if (node == null) {
            node = NULL;
        }
        try {
            return ConverterHelper.nodeToObject(node, Object.class);
        } catch (Exception e) {
            throw new NodeToQueryException("node to value error", e);
        }
    }

    // ************************* And/Or *************************

    /// and 永不可能为 null.
    private static ObjectNode andToNode(And and) throws QueryToNodeException {
        var node = new ObjectNode();
        node.put("@type", "And");
        node.put("clauses", clausesToNode(and.clauses()));
        return node;
    }

    /// or 永不可能为 null.
    private static ObjectNode orToNode(Or or) throws QueryToNodeException {
        var node = new ObjectNode();
        node.put("@type", "Or");
        node.put("clauses", clausesToNode(or.clauses()));
        return node;
    }

    /// andNode 永不可能为 null.
    private static And nodeToAnd(ObjectNode andNode) throws NodeToQueryException {
        var and = new And();
        and.add(nodeToClauses(andNode.get("clauses")));
        return and;
    }

    /// orNode 永不可能为 null.
    private static Or nodeToOr(ObjectNode orNode) throws NodeToQueryException {
        var or = new Or();
        or.add(nodeToClauses(orNode.get("clauses")));
        return or;
    }

    /// clauses 永不可能为 null.
    private static ArrayNode clausesToNode(Where[] clauses) throws QueryToNodeException {
        var node = new ArrayNode();
        for (var clause : clauses) {
            node.add(whereToNode(clause));
        }
        return node;
    }

    /// null / NullNode 解释为空数组.
    private static Where[] nodeToClauses(Node node) throws NodeToQueryException {
        if (node == null || node == NULL) {
            return new Where[0];
        }

        if (!(node instanceof ArrayNode clausesNode)) {
            throw new NodeToQueryException("clauses must be ArrayNode");
        }

        var clauses = new Where[clausesNode.size()];
        for (int i = 0; i < clauses.length; i = i + 1) {
            clauses[i] = nodeToWhere(clausesNode.get(i));
        }
        return clauses;
    }

    // ************************* Not *************************

    /// not 永不可能为 null.
    private static ObjectNode notToNode(Not not) throws QueryToNodeException {
        var node = new ObjectNode();
        node.put("@type", "Not");
        node.put("clause", whereToNode(not.clause()));
        return node;
    }

    /// notNode 永不可能为 null.
    private static Not nodeToNot(ObjectNode notNode) throws NodeToQueryException {
        return new Not(nodeToWhere(notNode.get("clause")));
    }

    // ************************* WhereClause *************************

    /// whereClause 永不可能为 null.
    private static ObjectNode whereClauseToNode(WhereClause whereClause) throws QueryToNodeException {
        var node = new ObjectNode();
        node.put("@type", "WhereClause");
        node.put("expression", expressionToNode(whereClause.expression()));
        node.put("params", paramsToNode(whereClause.params()));
        return node;
    }

    /// whereClauseNode 永不可能为 null.
    private static WhereClause nodeToWhereClause(ObjectNode whereClauseNode) throws NodeToQueryException {
        return new WhereClause(
            nodeToExpression(whereClauseNode.get("expression")),
            nodeToParams(whereClauseNode.get("params"))
        );
    }

    /// expression 允许 null.
    private static Node expressionToNode(String expression) {
        if (expression == null) {
            return NULL;
        }
        return new StringNode(expression);
    }

    /// expressionNode 允许 null.
    private static String nodeToExpression(Node expressionNode) throws NodeToQueryException {
        if (expressionNode == null || expressionNode == NULL) {
            return null;
        }
        if (!(expressionNode instanceof StringNode stringNode)) {
            throw new NodeToQueryException("expression must be StringNode");
        }
        return stringNode.asString();
    }

    /// params 允许为 null
    private static Node paramsToNode(Object[] params) throws QueryToNodeException {
        if (params == null) {
            return NULL;
        }
        try {
            return ConverterHelper.objectToNode(params);
        } catch (Exception e) {
            throw new QueryToNodeException("params to Node error", e);
        }
    }

    /// node 允许为 null
    private static Object[] nodeToParams(Node node) throws NodeToQueryException {
        if (node == null || node == NULL) {
            return null;
        }
        try {
            return ConverterHelper.nodeToObject(node, Object[].class);
        } catch (Exception e) {
            throw new NodeToQueryException("node to params error", e);
        }
    }

    // ************************* SkipIfInfo *************************

    /// skipIfInfo 永不可能为 null.
    private static ObjectNode skipIfInfoToNode(SkipIfInfo skipIfInfo) {
        var node = new ObjectNode();
        node.put("@type", "SkipIfInfo");
        node.put("skipIfNull", skipIfInfo.skipIfNull());
        node.put("skipIfEmptyList", skipIfInfo.skipIfEmptyList());
        node.put("skipIfEmptyString", skipIfInfo.skipIfEmptyString());
        node.put("skipIfBlankString", skipIfInfo.skipIfBlankString());
        return node;
    }

    /// null 解释为 默认配置
    private static SkipIfInfo nodeToSkipIfInfo(Node node) throws NodeToQueryException {
        if (node == null || node == NULL) {
            return new SkipIfInfo(false, false, false, false);
        }

        var skipIfInfoNode = nodeToObjectNode(node, "SkipIfInfo");

        return new SkipIfInfo(
            nodeToBoolean(skipIfInfoNode, "skipIfNull", false),
            nodeToBoolean(skipIfInfoNode, "skipIfEmptyList", false),
            nodeToBoolean(skipIfInfoNode, "skipIfEmptyString", false),
            nodeToBoolean(skipIfInfoNode, "skipIfBlankString", false)
        );
    }

    // ************************* Other *************************

    private static ObjectNode nodeToObjectNode(Node node, String expectedType) throws NodeToQueryException {
        if (!(node instanceof ObjectNode objectNode)) {
            throw new NodeToQueryException(expectedType + " must be ObjectNode");
        }

        var typeNode = objectNode.get("@type");

        if (typeNode == null || typeNode == NULL) {
            throw new NodeToQueryException(expectedType + " @type is missing");
        }

        if (!(typeNode instanceof StringNode type)) {
            throw new NodeToQueryException(expectedType + " @type must be StringNode");
        }

        if (!expectedType.equals(type.asString())) {
            throw new NodeToQueryException("Expected @type " + expectedType + ", but got " + type.asString());
        }

        return objectNode;
    }

    /// 不允许 null / NullNode.
    private static String nodeToString(ObjectNode objectNode, String name) throws NodeToQueryException {
        var node = objectNode.get(name);
        if (node == null || node == NULL) {
            throw new NodeToQueryException(name + " cannot be null");
        }
        if (!(node instanceof StringNode stringNode)) {
            throw new NodeToQueryException(name + " must be StringNode");
        }
        return stringNode.asString();
    }

    /// null / NullNode 默认解释为 defaultValue
    private static boolean nodeToBoolean(ObjectNode objectNode, String name, boolean defaultValue) throws NodeToQueryException {
        var node = objectNode.get(name);
        if (node == null || node == NULL) {
            return defaultValue;
        }
        if (!(node instanceof ValueNode valueNode)) {
            throw new NodeToQueryException(name + " must be ValueNode");
        }
        return valueNode.asBoolean();
    }

}
