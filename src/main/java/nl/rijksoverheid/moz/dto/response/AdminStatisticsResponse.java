package nl.rijksoverheid.moz.dto.response;

public class AdminStatisticsResponse {
    private double averageVerificationTimeSeconds;
    private double unverifiedPercentage;
    private long notSentCount;
    private long notVerifiedCount;

    public AdminStatisticsResponse() {}

    public AdminStatisticsResponse(double averageVerificationTimeSeconds, double unverifiedPercentage) {
        this.averageVerificationTimeSeconds = averageVerificationTimeSeconds;
        this.unverifiedPercentage = unverifiedPercentage;
    }

    public AdminStatisticsResponse(double averageVerificationTimeSeconds, double unverifiedPercentage, long notSentCount, long notVerifiedCount) {
        this.averageVerificationTimeSeconds = averageVerificationTimeSeconds;
        this.unverifiedPercentage = unverifiedPercentage;
        this.notSentCount = notSentCount;
        this.notVerifiedCount = notVerifiedCount;
    }

    public double getAverageVerificationTimeSeconds() {
        return averageVerificationTimeSeconds;
    }

    public void setAverageVerificationTimeSeconds(double averageVerificationTimeSeconds) {
        this.averageVerificationTimeSeconds = averageVerificationTimeSeconds;
    }

    public double getUnverifiedPercentage() {
        return unverifiedPercentage;
    }

    public void setUnverifiedPercentage(double unverifiedPercentage) {
        this.unverifiedPercentage = unverifiedPercentage;
    }

    public long getNotSentCount() {
        return notSentCount;
    }

    public void setNotSentCount(long notSentCount) {
        this.notSentCount = notSentCount;
    }

    public long getNotVerifiedCount() {
        return notVerifiedCount;
    }

    public void setNotVerifiedCount(long notVerifiedCount) {
        this.notVerifiedCount = notVerifiedCount;
    }
}
