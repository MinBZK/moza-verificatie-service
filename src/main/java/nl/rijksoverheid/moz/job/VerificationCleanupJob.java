package nl.rijksoverheid.moz.job;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import nl.rijksoverheid.moz.entity.StatisticFailureReason;
import nl.rijksoverheid.moz.entity.VerificationCode;
import nl.rijksoverheid.moz.entity.VerificationStatistics;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class VerificationCleanupJob {

    private static final Logger LOG = Logger.getLogger(VerificationCleanupJob.class);

    private final AtomicLong lastSuccessfulCleanupTimestamp = new AtomicLong(0);
    private final AtomicLong lastExpiredCleanupTimestamp = new AtomicLong(0);
    private final AtomicLong successfulCleanupCount = new AtomicLong(0);
    private final AtomicLong expiredCleanupCount = new AtomicLong(0);
    private final AtomicLong totalSuccessfulCodesProcessed = new AtomicLong(0);
    private final AtomicLong totalExpiredCodesProcessed = new AtomicLong(0);

    @Scheduled(every = "{verification.cleanup.schedule}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    @Transactional
    public void cleanUpSuccessfulVerifications() {
        // we can be sure that this method is never executed concurrently
        List<VerificationCode> codes = VerificationCode.findSuccessfulVerifications();
        if (!codes.isEmpty()) {
            LOG.info("Cleaning up " + codes.size() + " successful verifications");
        }

        for (VerificationCode code : codes) {
            VerificationStatistics statistics = new VerificationStatistics();
            statistics.setCreatedAt(code.getCreatedAt());
            statistics.setVerifyEmailSentAt(code.getVerifyEmailSentAt());
            statistics.setVerifiedAt(code.getVerifiedAt());
            statistics.persist();
        }

        codes.forEach(VerificationCode::delete);
        
        // Update metrics
        lastSuccessfulCleanupTimestamp.set(System.currentTimeMillis());
        successfulCleanupCount.incrementAndGet();
        totalSuccessfulCodesProcessed.addAndGet(codes.size());
    }

    @Scheduled(every = "{verification.cleanup.schedule}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    @Transactional
    public void cleanUpExpiredCodes() {
        // we can be sure that this method is never executed concurrently
        List<VerificationCode> codes = VerificationCode.findExpiredCodes(java.time.LocalDateTime.now());
        if (!codes.isEmpty()) {
            LOG.info("Cleaning up " + codes.size() + " expired verification codes");
        }

        for (VerificationCode code : codes) {
            VerificationStatistics statistics = new VerificationStatistics();
            statistics.setCreatedAt(code.getCreatedAt());
            statistics.setVerifyEmailSentAt(code.getVerifyEmailSentAt());
            if (code.getVerifyEmailSentAt() == null) {
                statistics.setFailureReason(StatisticFailureReason.NOT_SENT);
            } else {
                statistics.setFailureReason(StatisticFailureReason.NOT_VERIFIED);
            }
            statistics.persist();
        }

        codes.forEach(VerificationCode::delete);
        
        // Update metrics
        lastExpiredCleanupTimestamp.set(System.currentTimeMillis());
        expiredCleanupCount.incrementAndGet();
        totalExpiredCodesProcessed.addAndGet(codes.size());
    }
    
    // Getter methods for health check monitoring
    public long getLastSuccessfulCleanupTimestamp() {
        return lastSuccessfulCleanupTimestamp.get();
    }
    
    public long getLastExpiredCleanupTimestamp() {
        return lastExpiredCleanupTimestamp.get();
    }
    
    public long getSuccessfulCleanupCount() {
        return successfulCleanupCount.get();
    }
    
    public long getExpiredCleanupCount() {
        return expiredCleanupCount.get();
    }
    
    public long getTotalSuccessfulCodesProcessed() {
        return totalSuccessfulCodesProcessed.get();
    }
    
    public long getTotalExpiredCodesProcessed() {
        return totalExpiredCodesProcessed.get();
    }
}
