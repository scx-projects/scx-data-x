package dev.scx.data.x.test;

import dev.scx.data.aggregation.Aggregation;
import dev.scx.data.aggregation.AggregationImpl;
import dev.scx.data.aggregation.ExpressionGroupBy;
import dev.scx.data.aggregation.FieldGroupBy;
import dev.scx.data.x.exception.AggregationToNodeException;
import dev.scx.data.x.exception.NodeToAggregationException;
import dev.scx.node.ArrayNode;
import dev.scx.node.ObjectNode;
import org.testng.annotations.Test;

import static dev.scx.data.aggregation.AggregationBuilder.aggregation;
import static dev.scx.data.aggregation.AggregationBuilder.groupBy;
import static dev.scx.data.x.AggregationNodeConverter.aggregationToNode;
import static dev.scx.data.x.AggregationNodeConverter.nodeToAggregation;
import static dev.scx.data.x.test.TestUtils.*;
import static dev.scx.node.NullNode.NULL;
import static org.testng.Assert.*;

public class AggregationNodeConverterTest {

    public static void main(String[] args) {
        aggregationToNodeShouldEncodeEmptyAggregationStrictly();
        nodeToAggregationShouldDecodeNullAsEmptyAggregation();
        nodeToAggregationShouldDecodeMissingArraysAsEmptyArrays();
        aggregationShouldRoundTripComplexAggregation();
        aggregationToNodeShouldEncodeGroupBysAndAggsStrictly();
        aggregationToNodeShouldRejectNullAggregation();
        nodeToAggregationShouldRejectWrongRootType();
        nodeToAggregationShouldRejectNonArrayGroupBys();
        nodeToAggregationShouldRejectNullGroupByElement();
        nodeToAggregationShouldRejectUnknownGroupByType();
        nodeToAggregationShouldRejectNullAggElement();
    }

    @Test
    public static void aggregationToNodeShouldEncodeEmptyAggregationStrictly() {
        var node = assertObjectNode(aggregationToNode(aggregation()));

        assertType(node, "Aggregation");
        assertEquals(assertArrayNode(node.get("groupBys")).size(), 0);
        assertEquals(assertArrayNode(node.get("aggs")).size(), 0);
    }

    @Test
    public static void nodeToAggregationShouldDecodeNullAsEmptyAggregation() {
        assertEmptyAggregation(nodeToAggregation(null));
        assertEmptyAggregation(nodeToAggregation(NULL));
    }

    @Test
    public static void nodeToAggregationShouldDecodeMissingArraysAsEmptyArrays() {
        var node = new ObjectNode();
        node.put("@type", "Aggregation");

        var aggregation = nodeToAggregation(node);

        assertEmptyAggregation(aggregation);
    }

    @Test
    public static void aggregationShouldRoundTripComplexAggregation() {
        var aggregation = groupBy("department")
            .groupBy("month", "date_trunc('month', created_at)")
            .agg("total", "count(*)")
            .agg("amount", "sum(amount)");

        var roundTripped = nodeToAggregation(aggregationToNode(aggregation));

        var groupBys = roundTripped.getGroupBys();
        assertEquals(groupBys.length, 2);

        var fieldGroupBy = (FieldGroupBy) groupBys[0];
        assertEquals(fieldGroupBy.fieldName(), "department");

        var expressionGroupBy = (ExpressionGroupBy) groupBys[1];
        assertEquals(expressionGroupBy.alias(), "month");
        assertEquals(expressionGroupBy.expression(), "date_trunc('month', created_at)");

        var aggs = roundTripped.getAggs();
        assertEquals(aggs.length, 2);
        assertEquals(aggs[0].alias(), "total");
        assertEquals(aggs[0].expression(), "count(*)");
        assertEquals(aggs[1].alias(), "amount");
        assertEquals(aggs[1].expression(), "sum(amount)");
    }

    @Test
    public static void aggregationToNodeShouldEncodeGroupBysAndAggsStrictly() {
        var node = assertObjectNode(aggregationToNode(
            groupBy("status")
                .groupBy("year", "extract(year from created_at)")
                .agg("count", "count(*)")
        ));

        assertType(node, "Aggregation");

        var groupBys = assertArrayNode(node.get("groupBys"));
        assertEquals(groupBys.size(), 2);

        var fieldGroupBy = assertObjectNode(groupBys.get(0));
        assertType(fieldGroupBy, "FieldGroupBy");
        assertEquals(assertString(fieldGroupBy, "fieldName"), "status");

        var expressionGroupBy = assertObjectNode(groupBys.get(1));
        assertType(expressionGroupBy, "ExpressionGroupBy");
        assertEquals(assertString(expressionGroupBy, "alias"), "year");
        assertEquals(assertString(expressionGroupBy, "expression"), "extract(year from created_at)");

        var aggs = assertArrayNode(node.get("aggs"));
        assertEquals(aggs.size(), 1);
        var agg = assertObjectNode(aggs.get(0));
        assertType(agg, "Agg");
        assertEquals(assertString(agg, "alias"), "count");
        assertEquals(assertString(agg, "expression"), "count(*)");
    }

    @Test
    public static void aggregationToNodeShouldRejectNullAggregation() {
        assertThrows(AggregationToNodeException.class, () -> {
            aggregationToNode(null);
        });
    }

    @Test
    public static void nodeToAggregationShouldRejectWrongRootType() {
        assertThrows(NodeToAggregationException.class, () -> {
            var node = new ObjectNode();
            node.put("@type", "NotAggregation");

            nodeToAggregation(node);
        });
    }

    @Test
    public static void nodeToAggregationShouldRejectNonArrayGroupBys() {
        assertThrows(NodeToAggregationException.class, () -> {
            var node = new ObjectNode();
            node.put("@type", "Aggregation");
            node.put("groupBys", new ObjectNode());

            nodeToAggregation(node);
        });
    }

    @Test
    public static void nodeToAggregationShouldRejectNullGroupByElement() {
        assertThrows(NodeToAggregationException.class, () -> {
            var groupBys = new ArrayNode();
            groupBys.add(NULL);

            var node = new ObjectNode();
            node.put("@type", "Aggregation");
            node.put("groupBys", groupBys);

            nodeToAggregation(node);
        });
    }

    @Test
    public static void nodeToAggregationShouldRejectUnknownGroupByType() {
        assertThrows(NodeToAggregationException.class, () -> {
            var groupBy = new ObjectNode();
            groupBy.put("@type", "UnknownGroupBy");

            var groupBys = new ArrayNode();
            groupBys.add(groupBy);

            var node = new ObjectNode();
            node.put("@type", "Aggregation");
            node.put("groupBys", groupBys);

            nodeToAggregation(node);
        });
    }

    @Test
    public static void nodeToAggregationShouldRejectNullAggElement() {
        assertThrows(NodeToAggregationException.class, () -> {
            var aggs = new ArrayNode();
            aggs.add(NULL);

            var node = new ObjectNode();
            node.put("@type", "Aggregation");
            node.put("aggs", aggs);

            nodeToAggregation(node);
        });
    }

    private static void assertEmptyAggregation(Aggregation aggregation) {
        assertTrue(aggregation instanceof AggregationImpl);
        assertEquals(aggregation.getGroupBys().length, 0);
        assertEquals(aggregation.getAggs().length, 0);
    }

}
