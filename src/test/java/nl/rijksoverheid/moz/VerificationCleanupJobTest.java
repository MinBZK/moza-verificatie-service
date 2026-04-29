package nl.rijksoverheid.moz;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import nl.rijksoverheid.moz.entity.VerificationCode;
import nl.rijksoverheid.moz.entity.VerificationStatistics;
import nl.rijksoverheid.moz.job.VerificationCleanupJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class VerificationCleanupJobTest {

    @Inject
    VerificationCleanupJob cleanupJob;

    @BeforeEach
    void setup() {
        clearData();
    }

    @Transactional
    void clearData() {
        VerificationStatistics.deleteAll();
        VerificationCode.deleteAll();
    }

    @Test
    @TestTransaction
    void testCleanUpSuccessfulVerificationsUpdatesMetrics() {
        long countBefore = cleanupJob.getSuccessfulCleanupCount();
        long processedBefore = cleanupJob.getTotalSuccessfulCodesProcessed();

        VerificationCode code1 = new VerificationCode();
        code1.setVerifiedAt(LocalDateTime.now().minusMinutes(1));
        code1.persist();

        VerificationCode code2 = new VerificationCode();
        code2.setVerifiedAt(LocalDateTime.now().minusMinutes(2));
        code2.persist();

        long timestampBefore = System.currentTimeMillis();
        cleanupJob.cleanUpSuccessfulVerifications();
        long timestampAfter = System.currentTimeMillis();

        assertEquals(countBefore + 1, cleanupJob.getSuccessfulCleanupCount());
        assertEquals(processedBefore + 2, cleanupJob.getTotalSuccessfulCodesProcessed());
        assertTrue(cleanupJob.getLastSuccessfulCleanupTimestamp() >= timestampBefore);
        assertTrue(cleanupJob.getLastSuccessfulCleanupTimestamp() <= timestampAfter);
    }

    @Test
    @TestTransaction
    void testCleanUpSuccessfulVerificationsWithNoCodesStillUpdatesMetrics() {
        long countBefore = cleanupJob.getSuccessfulCleanupCount();
        long processedBefore = cleanupJob.getTotalSuccessfulCodesProcessed();

        cleanupJob.cleanUpSuccessfulVerifications();

        assertEquals(countBefore + 1, cleanupJob.getSuccessfulCleanupCount());
        assertEquals(processedBefore, cleanupJob.getTotalSuccessfulCodesProcessed());
    }

    @Test
    @TestTransaction
    void testCleanUpExpiredCodesUpdatesMetrics() {
        long countBefore = cleanupJob.getExpiredCleanupCount();
        long processedBefore = cleanupJob.getTotalExpiredCodesProcessed();

        VerificationCode code1 = new VerificationCode();
        code1.setValidUntil(LocalDateTime.now().minusMinutes(1));
        code1.setVerifyEmailSentAt(LocalDateTime.now().minusMinutes(2));
        code1.persist();

        VerificationCode code2 = new VerificationCode();
        code2.setValidUntil(LocalDateTime.now().minusMinutes(1));
        code2.setVerifyEmailSentAt(null);
        code2.persist();

        long timestampBefore = System.currentTimeMillis();
        cleanupJob.cleanUpExpiredCodes();
        long timestampAfter = System.currentTimeMillis();

        assertEquals(countBefore + 1, cleanupJob.getExpiredCleanupCount());
        assertEquals(processedBefore + 2, cleanupJob.getTotalExpiredCodesProcessed());
        assertTrue(cleanupJob.getLastExpiredCleanupTimestamp() >= timestampBefore);
        assertTrue(cleanupJob.getLastExpiredCleanupTimestamp() <= timestampAfter);
    }

    @Test
    @TestTransaction
    void testCleanUpExpiredCodesWithNoCodesStillUpdatesMetrics() {
        long countBefore = cleanupJob.getExpiredCleanupCount();
        long processedBefore = cleanupJob.getTotalExpiredCodesProcessed();

        cleanupJob.cleanUpExpiredCodes();

        assertEquals(countBefore + 1, cleanupJob.getExpiredCleanupCount());
        assertEquals(processedBefore, cleanupJob.getTotalExpiredCodesProcessed());
    }
}
