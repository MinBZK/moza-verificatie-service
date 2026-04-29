package nl.rijksoverheid.moz;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import nl.rijksoverheid.moz.job.VerificationCleanupJob;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.restassured.common.mapper.TypeRef;

import java.time.Instant;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class CleanupJobHealthControllerTest {

    @InjectMock
    VerificationCleanupJob cleanupJob;

    @Test
    void testGetMetricsNeverExecuted() {
        Mockito.when(cleanupJob.getLastSuccessfulCleanupTimestamp()).thenReturn(0L);
        Mockito.when(cleanupJob.getLastExpiredCleanupTimestamp()).thenReturn(0L);
        Mockito.when(cleanupJob.getSuccessfulCleanupCount()).thenReturn(0L);
        Mockito.when(cleanupJob.getExpiredCleanupCount()).thenReturn(0L);
        Mockito.when(cleanupJob.getTotalSuccessfulCodesProcessed()).thenReturn(0L);
        Mockito.when(cleanupJob.getTotalExpiredCodesProcessed()).thenReturn(0L);

        Map<String, Object> metrics = given()
                .when().get("/admin/cleanup-job/metrics")
                .then()
                .statusCode(200)
                .extract().as(new TypeRef<>() {});

        assertNotNull(metrics);
        assertEquals(0, metrics.get("successfulCleanupCount"));
        assertEquals(0, metrics.get("expiredCleanupCount"));
        assertEquals(0, metrics.get("totalSuccessfulCodesProcessed"));
        assertEquals(0, metrics.get("totalExpiredCodesProcessed"));
        assertEquals("Never executed", metrics.get("lastSuccessfulCleanupTimestamp"));
        assertEquals("Never executed", metrics.get("lastExpiredCleanupTimestamp"));
        assertFalse(metrics.containsKey("secondsSinceLastSuccessfulCleanup"));
        assertFalse(metrics.containsKey("secondsSinceLastExpiredCleanup"));
    }

    @Test
    void testGetMetricsAfterExecution() {
        long timestamp = System.currentTimeMillis() - 10_000;
        Mockito.when(cleanupJob.getLastSuccessfulCleanupTimestamp()).thenReturn(timestamp);
        Mockito.when(cleanupJob.getLastExpiredCleanupTimestamp()).thenReturn(timestamp);
        Mockito.when(cleanupJob.getSuccessfulCleanupCount()).thenReturn(3L);
        Mockito.when(cleanupJob.getExpiredCleanupCount()).thenReturn(5L);
        Mockito.when(cleanupJob.getTotalSuccessfulCodesProcessed()).thenReturn(12L);
        Mockito.when(cleanupJob.getTotalExpiredCodesProcessed()).thenReturn(20L);

        String expectedTimestamp = Instant.ofEpochMilli(timestamp).toString();

        Map<String, Object> metrics = given()
                .when().get("/admin/cleanup-job/metrics")
                .then()
                .statusCode(200)
                .extract().as(new TypeRef<>() {});

        assertNotNull(metrics);
        assertEquals(3, metrics.get("successfulCleanupCount"));
        assertEquals(5, metrics.get("expiredCleanupCount"));
        assertEquals(12, metrics.get("totalSuccessfulCodesProcessed"));
        assertEquals(20, metrics.get("totalExpiredCodesProcessed"));
        assertEquals(expectedTimestamp, metrics.get("lastSuccessfulCleanupTimestamp"));
        assertEquals(expectedTimestamp, metrics.get("lastExpiredCleanupTimestamp"));
        assertTrue(metrics.containsKey("secondsSinceLastSuccessfulCleanup"));
        assertTrue(metrics.containsKey("secondsSinceLastExpiredCleanup"));
        assertTrue((Integer) metrics.get("secondsSinceLastSuccessfulCleanup") >= 10);
        assertTrue((Integer) metrics.get("secondsSinceLastExpiredCleanup") >= 10);
    }
}
