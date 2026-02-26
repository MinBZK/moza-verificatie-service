package nl.rijksoverheid.moz.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import nl.rijksoverheid.moz.job.VerificationCleanupJob;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Path("/admin/cleanup-job")
public class CleanupJobHealthController {

    @Inject
    VerificationCleanupJob cleanupJob;

    @GET
    @Path("/metrics")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Get cleanup job metrics",
            description = "Returns metrics and health information about the verification cleanup job"
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Cleanup job metrics retrieved successfully"
            )
    })
    public Map<String, Object> getCleanupJobMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        long lastSuccessfulCleanup = cleanupJob.getLastSuccessfulCleanupTimestamp();
        long lastExpiredCleanup = cleanupJob.getLastExpiredCleanupTimestamp();
        
        metrics.put("successfulCleanupCount", cleanupJob.getSuccessfulCleanupCount());
        metrics.put("expiredCleanupCount", cleanupJob.getExpiredCleanupCount());
        metrics.put("totalSuccessfulCodesProcessed", cleanupJob.getTotalSuccessfulCodesProcessed());
        metrics.put("totalExpiredCodesProcessed", cleanupJob.getTotalExpiredCodesProcessed());
        
        if (lastSuccessfulCleanup > 0) {
            metrics.put("lastSuccessfulCleanupTimestamp", Instant.ofEpochMilli(lastSuccessfulCleanup).toString());
        } else {
            metrics.put("lastSuccessfulCleanupTimestamp", "Never executed");
        }
        
        if (lastExpiredCleanup > 0) {
            metrics.put("lastExpiredCleanupTimestamp", Instant.ofEpochMilli(lastExpiredCleanup).toString());
        } else {
            metrics.put("lastExpiredCleanupTimestamp", "Never executed");
        }
        
        // Calculate time since last execution
        long currentTime = System.currentTimeMillis();
        if (lastSuccessfulCleanup > 0) {
            metrics.put("secondsSinceLastSuccessfulCleanup", (currentTime - lastSuccessfulCleanup) / 1000);
        }
        if (lastExpiredCleanup > 0) {
            metrics.put("secondsSinceLastExpiredCleanup", (currentTime - lastExpiredCleanup) / 1000);
        }
        
        return metrics;
    }
}
