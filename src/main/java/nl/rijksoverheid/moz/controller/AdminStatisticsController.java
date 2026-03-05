package nl.rijksoverheid.moz.controller;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import nl.rijksoverheid.moz.dto.response.AdminStatisticsResponse;
import nl.rijksoverheid.moz.entity.StatisticFailureReason;
import nl.rijksoverheid.moz.entity.VerificationStatistics;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import java.time.Duration;
import java.util.List;

@Path("/admin/statistics")
public class AdminStatisticsController {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get admin statistics", description = "Returns statistics about the verification process.", hidden = true)
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Statistics retrieved",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AdminStatisticsResponse.class))
            )
    })
    public AdminStatisticsResponse getAdminStatistics() {
        List<VerificationStatistics> allStats = VerificationStatistics.listAll();
        long totalCount = allStats.size();

        if (totalCount == 0) {
            return new AdminStatisticsResponse(0.0, 0.0, 0, 0);
        }

        long unverifiedCount = 0;
        long totalVerifySeconds = 0;
        long verifiedWithTimeCount = 0;
        long notSentCount = 0;
        long notVerifiedCount = 0;

        for (VerificationStatistics s : allStats) {
            if (s.getVerifiedAt() == null) {
                unverifiedCount++;
            }
            if (s.getVerifiedAt() != null && s.getVerifyEmailSentAt() != null) {
                totalVerifySeconds += Duration.between(s.getVerifyEmailSentAt(), s.getVerifiedAt()).toSeconds();
                verifiedWithTimeCount++;
            }
            if (s.getFailureReason() == StatisticFailureReason.NOT_SENT) {
                notSentCount++;
            } else if (s.getFailureReason() == StatisticFailureReason.NOT_VERIFIED) {
                notVerifiedCount++;
            }
        }

        double unverifiedPercentage = (unverifiedCount * 100.0) / totalCount;
        double averageTimeSeconds = verifiedWithTimeCount > 0 ? (double) totalVerifySeconds / verifiedWithTimeCount : 0.0;

        return new AdminStatisticsResponse(averageTimeSeconds, unverifiedPercentage, notSentCount, notVerifiedCount);
    }
}
