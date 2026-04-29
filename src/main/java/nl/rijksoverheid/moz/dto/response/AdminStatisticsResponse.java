package nl.rijksoverheid.moz.dto.response;

public record AdminStatisticsResponse(
        double averageVerificationTimeSeconds,
        double unverifiedPercentage,
        long notSentCount,
        long notVerifiedCount) {
}
