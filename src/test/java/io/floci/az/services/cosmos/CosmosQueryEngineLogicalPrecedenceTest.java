package io.floci.az.services.cosmos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CosmosQueryEngineLogicalPrecedenceTest {

    private final CosmosQueryEngine engine = new CosmosQueryEngine();

    @ParameterizedTest
    @CsvSource({"true, true", "true, false", "false, true", "false, false"})
    void respectsLogicalPrecedence(boolean a, boolean b) {
        Map<String, Object> document = Map.of("a", a, "b", b);

        assertEquals(!a || b, engine.evalExpr(document, "NOT c.a = true OR c.b = true"));
        assertEquals(!a && b, engine.evalExpr(document, "NOT c.a = true AND c.b = true"));
        assertEquals(!(a || b), engine.evalExpr(document, "NOT (c.a = true OR c.b = true)"));
        assertEquals(!(a && b), engine.evalExpr(document, "NOT (c.a = true AND c.b = true)"));
        assertEquals(!a || b, engine.evalExpr(document, "((NOT c.a = true) OR (c.b = true))"));
        assertEquals(a || b, engine.evalExpr(document, "NOT NOT c.a = true OR c.b = true"));
        assertEquals(!a || b && a,
                engine.evalExpr(document, "NOT c.a = true OR c.b = true AND c.a = true"));
        assertEquals((!a || b) && a,
                engine.evalExpr(document, "((NOT c.a = true OR c.b = true) AND c.a = true)"));
        assertEquals(!a || b, engine.evalExpr(document, "not c.a = true or c.b = true"));
    }

    @Test
    void includesNonDeletedDocuments() {
        List<Map<String, Object>> documents = List.of(
                Map.of("id", "absent"),
                Map.of("id", "false", "isDeleted", false),
                Map.of("id", "true", "isDeleted", true));

        assertEquals(List.of("absent", "false"), engine.execute(
                "SELECT VALUE c.id FROM c WHERE (NOT IS_DEFINED(c.isDeleted) OR c.isDeleted = false)",
                List.of(), documents).items());
    }

    @Test
    void includesAbsentAndNullClaims() {
        Map<String, Object> nullClaim = new LinkedHashMap<>();
        nullClaim.put("id", "null");
        nullClaim.put("claimedUntil", null);
        List<Map<String, Object>> documents = List.of(
                Map.of("id", "absent"), nullClaim,
                Map.of("id", "claimed", "claimedUntil", "2026-09-12T12:00:00Z"),
                Map.of("id", "false", "claimedUntil", false),
                Map.of("id", "true", "claimedUntil", true));

        assertEquals(List.of("absent", "null"), engine.execute(
                "SELECT VALUE c.id FROM c WHERE (NOT IS_DEFINED(c.claimedUntil) OR IS_NULL(c.claimedUntil))",
                List.of(), documents).items());
    }
}
