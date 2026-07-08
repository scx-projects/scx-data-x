package dev.scx.data.x.test;

import dev.scx.data.query.*;
import dev.scx.data.x.exception.NodeToQueryException;
import dev.scx.data.x.exception.QueryToNodeException;
import dev.scx.node.ArrayNode;
import dev.scx.node.LongNode;
import dev.scx.node.ObjectNode;
import dev.scx.node.StringNode;
import org.testng.annotations.Test;

import static dev.scx.data.query.BuildControl.*;
import static dev.scx.data.query.QueryBuilder.*;
import static dev.scx.data.x.QueryNodeConverter.nodeToQuery;
import static dev.scx.data.x.QueryNodeConverter.queryToNode;
import static dev.scx.data.x.test.TestUtils.*;
import static dev.scx.node.NullNode.NULL;
import static org.testng.Assert.*;

public class QueryNodeConverterTest {

    public static void main(String[] args) {
        queryToNodeShouldEncodeEmptyQueryStrictly();
        nodeToQueryShouldDecodeNullAsEmptyQuery();
        nodeToQueryShouldDecodeMissingOptionalMembersAsDefaults();
        queryShouldRoundTripComplexWhereAndOrderBy();
        conditionShouldDecodeMissingBooleanAndSkipIfInfoAsDefaults();
        queryToNodeShouldEncodeWhereClauseWithNulls();
        queryToNodeShouldRejectNullQuery();
        nodeToQueryShouldRejectWrongRootType();
        nodeToQueryShouldRejectNullOrderByElement();
        nodeToQueryShouldRejectNegativeOffset();
        nodeToQueryShouldWrapInvalidOffsetValue();
    }

    @Test
    public static void queryToNodeShouldEncodeEmptyQueryStrictly() {
        var node = assertObjectNode(queryToNode(query()));

        assertType(node, "Query");
        assertNullNode(node, "where");
        assertEquals(assertArrayNode(node.get("orderBys")).size(), 0);
        assertNullNode(node, "offset");
        assertNullNode(node, "limit");
    }

    @Test
    public static void nodeToQueryShouldDecodeNullAsEmptyQuery() {
        assertEmptyQuery(nodeToQuery(null));
        assertEmptyQuery(nodeToQuery(NULL));
    }

    @Test
    public static void nodeToQueryShouldDecodeMissingOptionalMembersAsDefaults() {
        var node = new ObjectNode();
        node.put("@type", "Query");

        var query = nodeToQuery(node);

        assertEmptyQuery(query);
    }

    @Test
    public static void queryShouldRoundTripComplexWhereAndOrderBy() {
        var query = and(
            eq("name", "scx", USE_EXPRESSION, USE_EXPRESSION_VALUE, SKIP_IF_NULL),
            between("age", 18, 30, SKIP_IF_EMPTY_LIST),
            or(
                whereClause("status = ?", "OK"),
                not(null)
            )
        )
            .asc("name")
            .desc("count(*)", USE_EXPRESSION)
            .offset(5)
            .limit(10);

        var roundTripped = nodeToQuery(queryToNode(query));

        assertEquals(roundTripped.getOffset(), Long.valueOf(5));
        assertEquals(roundTripped.getLimit(), Long.valueOf(10));

        var orderBys = roundTripped.getOrderBys();
        assertEquals(orderBys.length, 2);
        assertEquals(orderBys[0].selector(), "name");
        assertEquals(orderBys[0].orderByType(), OrderByType.ASC);
        assertFalse(orderBys[0].useExpression());
        assertEquals(orderBys[1].selector(), "count(*)");
        assertEquals(orderBys[1].orderByType(), OrderByType.DESC);
        assertTrue(orderBys[1].useExpression());

        assertTrue(roundTripped.getWhere() instanceof And);
        var and = (And) roundTripped.getWhere();
        assertEquals(and.clauses().length, 3);

        var nameCondition = (Condition) and.clauses()[0];
        assertEquals(nameCondition.selector(), "name");
        assertEquals(nameCondition.conditionType(), ConditionType.EQ);
        assertEquals(nameCondition.value1(), "scx");
        assertNull(nameCondition.value2());
        assertTrue(nameCondition.useExpression());
        assertTrue(nameCondition.useExpressionValue());
        assertTrue(nameCondition.skipIfInfo().skipIfNull());
        assertFalse(nameCondition.skipIfInfo().skipIfEmptyList());

        var ageCondition = (Condition) and.clauses()[1];
        assertEquals(ageCondition.selector(), "age");
        assertEquals(ageCondition.conditionType(), ConditionType.BETWEEN);
        assertNumber(ageCondition.value1(), 18);
        assertNumber(ageCondition.value2(), 30);
        assertFalse(ageCondition.skipIfInfo().skipIfNull());
        assertTrue(ageCondition.skipIfInfo().skipIfEmptyList());

        var or = (Or) and.clauses()[2];
        assertEquals(or.clauses().length, 2);

        var whereClause = (WhereClause) or.clauses()[0];
        assertEquals(whereClause.expression(), "status = ?");
        assertNotNull(whereClause.params());
        assertEquals(whereClause.params().length, 1);
        assertEquals(whereClause.params()[0], "OK");

        var not = (Not) or.clauses()[1];
        assertNull(not.clause());
    }

    @Test
    public static void conditionShouldDecodeMissingBooleanAndSkipIfInfoAsDefaults() {
        var conditionNode = new ObjectNode();
        conditionNode.put("@type", "Condition");
        conditionNode.put("selector", "id");
        conditionNode.put("conditionType", "EQ");
        conditionNode.put("value1", 1);
        conditionNode.put("value2", NULL);

        var queryNode = new ObjectNode();
        queryNode.put("@type", "Query");
        queryNode.put("where", conditionNode);

        var condition = (Condition) nodeToQuery(queryNode).getWhere();

        assertEquals(condition.selector(), "id");
        assertEquals(condition.conditionType(), ConditionType.EQ);
        assertNumber(condition.value1(), 1);
        assertNull(condition.value2());
        assertFalse(condition.useExpression());
        assertFalse(condition.useExpressionValue());
        assertFalse(condition.skipIfInfo().skipIfNull());
        assertFalse(condition.skipIfInfo().skipIfEmptyList());
        assertFalse(condition.skipIfInfo().skipIfEmptyString());
        assertFalse(condition.skipIfInfo().skipIfBlankString());
    }

    @Test
    public static void queryToNodeShouldEncodeWhereClauseWithNulls() {
        var node = assertObjectNode(queryToNode(where(whereClause(null, (Object[]) null))));
        var whereNode = assertObjectNode(node.get("where"));

        assertType(whereNode, "WhereClause");
        assertNullNode(whereNode, "expression");
        assertNullNode(whereNode, "params");
    }

    @Test
    public static void queryToNodeShouldRejectNullQuery() {
        assertThrows(QueryToNodeException.class, () -> {
            queryToNode(null);
        });
    }

    @Test
    public static void nodeToQueryShouldRejectWrongRootType() {
        assertThrows(NodeToQueryException.class, () -> {
            var node = new ObjectNode();
            node.put("@type", "NotQuery");
            nodeToQuery(node);
        });
    }

    @Test
    public static void nodeToQueryShouldRejectNullOrderByElement() {
        assertThrows(NodeToQueryException.class, () -> {
            var orderBys = new ArrayNode();
            orderBys.add(NULL);

            var node = new ObjectNode();
            node.put("@type", "Query");
            node.put("orderBys", orderBys);

            nodeToQuery(node);
        });
    }

    @Test
    public static void nodeToQueryShouldRejectNegativeOffset() {
        assertThrows(NodeToQueryException.class, () -> {
            var node = new ObjectNode();
            node.put("@type", "Query");
            node.put("offset", new LongNode(-1));

            nodeToQuery(node);
        });
    }

    @Test
    public static void nodeToQueryShouldWrapInvalidOffsetValue() {
        assertThrows(NodeToQueryException.class, () -> {
            var node = new ObjectNode();
            node.put("@type", "Query");
            node.put("offset", new StringNode("abc"));

            nodeToQuery(node);
        });
    }

    private static void assertEmptyQuery(Query query) {
        assertTrue(query instanceof QueryImpl);
        assertNull(query.getWhere());
        assertEquals(query.getOrderBys().length, 0);
        assertNull(query.getOffset());
        assertNull(query.getLimit());
    }

}
