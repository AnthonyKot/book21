package lab;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * The guided preview cases. Each subclass states whether it expects the partitioned cache;
 * the application's own lab.partition property decides what actually runs, so the two can be
 * mismatched for a negative control.
 */
abstract class PreviewCases extends ReviewHttp {
    abstract boolean expectPartitioned();

    @Test
    void ownCold() throws Exception {
        var response = preview("alice", "C-1001");
        assertEquals(200, response.statusCode());
        assertEquals("CEDAR-PRIVATE-SUMMARY", response.body());
        assertEquals(1, store.bodyLoads.get());
    }

    @Test
    void foreignCold() throws Exception {
        noContent(preview("bob", "C-1001"));
        assertEquals(0, previews.entries());
    }

    @Test
    void sameTenantWarm() throws Exception {
        preview("alice", "C-1001");
        assertEquals("CEDAR-PRIVATE-SUMMARY", preview("alice", "C-1001").body());
        assertEquals(1, store.bodyLoads.get());
    }

    @Test
    void cedarWarmsBirchReads() throws Exception {
        preview("alice", "C-1001");
        var response = preview("bob", "C-1001");
        if (expectPartitioned()) {
            noContent(response);
            assertEquals(2, store.bodyLoads.get());
        } else {
            assertEquals(200, response.statusCode());
            assertEquals("CEDAR-PRIVATE-SUMMARY", response.body());
            assertEquals(1, store.bodyLoads.get());
        }
    }

    @Test
    void birchWarmsCedarReads() throws Exception {
        preview("bob", "B-2001");
        var response = preview("alice", "B-2001");
        if (expectPartitioned()) {
            noContent(response);
        } else {
            assertEquals(200, response.statusCode());
            assertEquals("BIRCH-PRIVATE-SUMMARY", response.body());
        }
    }

    @Test
    void deniedMissDoesNotPoisonOwner() throws Exception {
        noContent(preview("bob", "C-1001"));
        assertEquals("CEDAR-PRIVATE-SUMMARY", preview("alice", "C-1001").body());
    }

    @Test
    void queryTenantIsIgnored() throws Exception {
        noContent(get("bob", "C-1001", "preview", "?tenant=cedar"));
    }

    @Test
    void unknownDocument() throws Exception {
        noContent(preview("alice", "missing"));
    }

    @Test
    void anonymousCannotUseWarmCache() throws Exception {
        preview("alice", "C-1001");
        assertEquals(401, preview("", "C-1001").statusCode());
        assertEquals(1, store.bodyLoads.get());
    }

    @Test
    void anotherOwnDocument() throws Exception {
        preview("alice", "C-1001");
        assertEquals("CEDAR-SECOND-SUMMARY", preview("alice", "C-1002").body());
        assertEquals(2, previews.entries());
    }
}
