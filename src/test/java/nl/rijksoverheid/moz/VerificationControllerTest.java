package nl.rijksoverheid.moz;

import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import nl.rijksoverheid.moz.service.NotifyNLService;
import nl.rijksoverheid.moz.dto.request.VerificationApplicationRequest;
import nl.rijksoverheid.moz.dto.request.VerificationRequest;
import nl.rijksoverheid.moz.entity.StatisticFailureReason;
import nl.rijksoverheid.moz.entity.VerificationCode;
import nl.rijksoverheid.moz.entity.VerificationStatistics;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import nl.rijksoverheid.moz.job.VerificationCleanupJob;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@QuarkusTest
class VerificationControllerTest {


    private static final int VERIFICATION_CODE_VALIDITY_MINUTES = 10;

    @Inject
    EntityManager entityManager;

    @InjectMock
    NotifyNLService notifyNLService;

    @BeforeEach
    void setup() {
        Mockito.when(notifyNLService.sendVerificationEmail(any(), any(), any(), any())).thenReturn(true);
        clearStats();
    }

    @Transactional
    void clearStats() {
        VerificationStatistics.deleteAll();
        VerificationCode.deleteAll();
    }

    @Test
    @TestTransaction
    void testAddServiceProviderEndpoint() {
        VerificationApplicationRequest request = new VerificationApplicationRequest();
        request.setEmail("test@example.com");

        String referenceId = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/request")
                .then()
                .statusCode(200)
                .extract().asString();

        assertNotNull(referenceId);
        assertFalse(referenceId.isEmpty());

        VerificationCode code = VerificationCode.find("referenceId", referenceId).firstResult();
        assertNotNull(code);
        assertEquals(referenceId, code.getReferenceId());
    }

    @Test
    void testVerifySuccess() {
        // Create request via the API
        VerificationApplicationRequest appRequest = new VerificationApplicationRequest();
        appRequest.setEmail("success@example.com");

        String referenceId = given()
                .contentType(ContentType.JSON)
                .body(appRequest)
                .when().post("/request")
                .then()
                .statusCode(200)
                .extract().asString();

        VerificationCode code = VerificationCode.find("referenceId", referenceId).firstResult();
        assertNotNull(code);

        VerificationRequest request = new VerificationRequest();
        request.setReferenceId(referenceId);
        request.setCode(code.getCode());

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/verify")
                .then()
                .statusCode(200)
                .body("success", is(true));
    }

    @Test
    void testVerifyNotFound() {
        VerificationRequest request = new VerificationRequest();
        request.setReferenceId("invalid-ref-id");
        request.setCode("123456");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/verify")
                .then()
                .statusCode(200)
                .body("success", is(false))
                .body("reasonId", is(1))
                .body("reasonMessage", is("Reference ID not found"));
    }

    @Test
    void testVerifyExpired() {
        // Create request via the API
        VerificationApplicationRequest appRequest = new VerificationApplicationRequest();
        appRequest.setEmail("expired@example.com");

        String referenceId = given()
                .contentType(ContentType.JSON)
                .body(appRequest)
                .when().post("/request")
                .then()
                .statusCode(200)
                .extract().asString();

        expireCode(referenceId);
        VerificationCode code = VerificationCode.find("referenceId", referenceId).firstResult();
        VerificationRequest request = new VerificationRequest();
        request.setReferenceId(referenceId);
        request.setCode(code.getCode());

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/verify")
                .then()
                .statusCode(200)
                .body("success", is(false))
                .body("reasonId", is(2))
                .body("reasonMessage", is("Code expired"));
    }

    @Transactional
    void expireCode(String referenceId) {
        VerificationCode.update("validUntil = ?1 where referenceId = ?2",
            LocalDateTime.now().minusMinutes(VERIFICATION_CODE_VALIDITY_MINUTES), referenceId);
    }
    @Test
    void testVerifyAlreadyUsed() {
        // Create request via the API
        VerificationApplicationRequest appRequest = new VerificationApplicationRequest();
        appRequest.setEmail("used@example.com");

        String referenceId = given()
                .contentType(ContentType.JSON)
                .body(appRequest)
                .when().post("/request")
                .then()
                .statusCode(200)
                .extract().asString();

        VerificationCode code = VerificationCode.find("referenceId", referenceId).firstResult();

        // First verification - should succeed
        VerificationRequest request = new VerificationRequest();
        request.setReferenceId(referenceId);
        request.setCode(code.getCode());

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/verify")
                .then()
                .statusCode(200)
                .body("success", is(true));

        // Second verification - should fail with 200 and reasonId 3
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/verify")
                .then()
                .statusCode(200)
                .body("success", is(false))
                .body("reasonId", is(3))
                .body("reasonMessage", is("Code already used"));
    }

    @Test
    void testVerifyWrongCode() {
        // Create request via the API
        VerificationApplicationRequest appRequest = new VerificationApplicationRequest();
        appRequest.setEmail("wrongcode@example.com");

        String referenceId = given()
                .contentType(ContentType.JSON)
                .body(appRequest)
                .when().post("/request")
                .then()
                .statusCode(200)
                .extract().asString();

        VerificationRequest request = new VerificationRequest();
        request.setReferenceId(referenceId);
        request.setCode("000000"); // Wrong code

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/verify")
                .then()
                .statusCode(200)
                .body("success", is(false))
                .body("reasonId", is(4))
                .body("reasonMessage", is("Incorrect code"));
    }
    @Test
    void testRequestVerificationFailure() {
        Mockito.when(notifyNLService.sendVerificationEmail(any(), any(), any(), any())).thenReturn(false);

        VerificationApplicationRequest request = new VerificationApplicationRequest();
        request.setEmail("fail@example.com");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/request")
                .then()
                .statusCode(500);
    }

    @Inject
    VerificationCleanupJob cleanupJob;

    @Test
    @TestTransaction
    void testCleanUpSuccessfulVerifications() {
        VerificationCode code = new VerificationCode();
        code.setVerifiedAt(LocalDateTime.now().minusMinutes(1));
        code.persist();

        cleanupJob.cleanUpSuccessfulVerifications();

        // Check that code is deleted
        assertNull(VerificationCode.findById(code.id));

        // Check that statistics are created
        List<VerificationStatistics> stats = VerificationStatistics.listAll();
        assertTrue(stats.stream().anyMatch(s -> s.getVerifiedAt() != null));
    }

    @Test
    @TestTransaction
    void testCleanUpExpiredCodes() {
        VerificationCode code = new VerificationCode();
        code.setValidUntil(LocalDateTime.now().minusMinutes(1));
        code.setVerifyEmailSentAt(LocalDateTime.now().minusMinutes(2));
        code.persist();

        cleanupJob.cleanUpExpiredCodes();

        // Check that code is deleted
        assertNull(VerificationCode.findById(code.id));

        // Check that statistics are created (verifiedAt should be null)
        List<VerificationStatistics> stats = VerificationStatistics.listAll();
        assertTrue(stats.stream().anyMatch(s -> s.getVerifiedAt() == null));
        assertTrue(stats.stream().anyMatch(s -> s.getFailureReason() == StatisticFailureReason.NOT_VERIFIED));
    }

    @Test
    @TestTransaction
    void testCleanUpExpiredCodesNotSent() {
        VerificationCode code = new VerificationCode();
        code.setValidUntil(LocalDateTime.now().minusMinutes(1));
        code.setVerifyEmailSentAt(null);
        code.persist();

        cleanupJob.cleanUpExpiredCodes();

        // Check that code is deleted
        assertNull(VerificationCode.findById(code.id));

        // Check that statistics are created with NOT_SENT reason
        List<VerificationStatistics> stats = VerificationStatistics.listAll();
        assertTrue(stats.stream().anyMatch(s -> s.getFailureReason() == StatisticFailureReason.NOT_SENT));
    }

    @Test
    @TestTransaction
    void testAdminStatisticsIncludeFailureCounts() {
        VerificationCode code1 = new VerificationCode();
        code1.setValidUntil(LocalDateTime.now().minusMinutes(1));
        code1.setVerifyEmailSentAt(null);
        code1.persist();

        VerificationCode code2 = new VerificationCode();
        code2.setValidUntil(LocalDateTime.now().minusMinutes(1));
        code2.setVerifyEmailSentAt(LocalDateTime.now().minusMinutes(2));
        code2.persist();

        cleanupJob.cleanUpExpiredCodes();

        // Check DB directly since the transaction might not be visible to the HTTP server
        List<VerificationStatistics> stats = VerificationStatistics.listAll();
        assertEquals(2, stats.size());
        assertTrue(stats.stream().anyMatch(s -> s.getFailureReason() == StatisticFailureReason.NOT_SENT));
        assertTrue(stats.stream().anyMatch(s -> s.getFailureReason() == StatisticFailureReason.NOT_VERIFIED));
    }
}