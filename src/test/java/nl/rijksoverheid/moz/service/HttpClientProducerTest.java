package nl.rijksoverheid.moz.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class HttpClientProducerTest {

    @Inject
    HttpClient httpClient;

    @Test
    void testProducedHttpClientIsNotNull() {
        assertNotNull(httpClient);
    }

    @Test
    void testProducedHttpClientUsesHttp2() {
        assertEquals(HttpClient.Version.HTTP_2, httpClient.version());
    }

    @Test
    void testProducedHttpClientHasDefaultConnectTimeout() {
        assertTrue(httpClient.connectTimeout().isPresent());
        assertEquals(Duration.ofSeconds(10), httpClient.connectTimeout().get());
    }
}
