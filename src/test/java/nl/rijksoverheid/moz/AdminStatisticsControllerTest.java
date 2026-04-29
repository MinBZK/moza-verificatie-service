package nl.rijksoverheid.moz;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import nl.rijksoverheid.moz.dto.response.AdminStatisticsResponse;
import nl.rijksoverheid.moz.entity.StatisticFailureReason;
import nl.rijksoverheid.moz.entity.VerificationStatistics;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class AdminStatisticsControllerTest {

    @Test
    void testGetAdminStatistics() {
        createStats();

        AdminStatisticsResponse response = given()
                .contentType(ContentType.JSON)
                .when().get("/admin/statistics")
                .then()
                .statusCode(200)
                .extract().as(AdminStatisticsResponse.class);

        assertNotNull(response);

        assertEquals(90.0f, response.averageVerificationTimeSeconds());
        assertEquals(50.0f, response.unverifiedPercentage());
        assertEquals(1, response.notSentCount());
        assertEquals(1, response.notVerifiedCount());
    }

    @jakarta.transaction.Transactional
    void createStats() {
        VerificationStatistics.deleteAll();

        // Create a verified statistic
        VerificationStatistics stat1 = new VerificationStatistics();
        stat1.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        stat1.setVerifyEmailSentAt(LocalDateTime.now().minusMinutes(4));
        stat1.setVerifiedAt(LocalDateTime.now().minusMinutes(2)); // 2 minutes (120 seconds) verification time
        stat1.persist();

        // Create another verified statistic
        VerificationStatistics stat2 = new VerificationStatistics();
        stat2.setCreatedAt(LocalDateTime.now().minusMinutes(10));
        stat2.setVerifyEmailSentAt(LocalDateTime.now().minusMinutes(9));
        stat2.setVerifiedAt(LocalDateTime.now().minusMinutes(8)); // 1 minute (60 seconds) verification time
        stat2.persist();

        // Average should be (120 + 60) / 2 = 90 seconds

        // Create an unverified statistic (NOT_VERIFIED)
        VerificationStatistics stat3 = new VerificationStatistics();
        stat3.setCreatedAt(LocalDateTime.now().minusMinutes(20));
        stat3.setVerifyEmailSentAt(LocalDateTime.now().minusMinutes(19));
        stat3.setVerifiedAt(null);
        stat3.setFailureReason(StatisticFailureReason.NOT_VERIFIED);
        stat3.persist();

        // Create an unverified statistic (NOT_SENT)
        VerificationStatistics stat4 = new VerificationStatistics();
        stat4.setCreatedAt(LocalDateTime.now().minusMinutes(30));
        stat4.setVerifiedAt(null);
        stat4.setFailureReason(StatisticFailureReason.NOT_SENT);
        stat4.persist();
    }

    @Test
    void testGetAdminStatisticsEmpty() {
        clearStats();

        AdminStatisticsResponse response = given()
                .contentType(ContentType.JSON)
                .when().get("/admin/statistics")
                .then()
                .statusCode(200)
                .extract().as(AdminStatisticsResponse.class);

        assertNotNull(response);

        assertEquals(0.0f, response.averageVerificationTimeSeconds());
        assertEquals(0.0f, response.unverifiedPercentage());
        assertEquals(0, response.notSentCount());
        assertEquals(0, response.notVerifiedCount());
    }

    @jakarta.transaction.Transactional
    void clearStats() {
        VerificationStatistics.deleteAll();
    }
}
