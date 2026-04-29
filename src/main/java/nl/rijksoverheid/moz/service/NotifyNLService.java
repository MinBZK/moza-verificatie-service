package nl.rijksoverheid.moz.service;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import nl.rijksoverheid.moz.entity.VerificationCode;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@ApplicationScoped
public class NotifyNLService {

    private static final Logger LOG = Logger.getLogger(NotifyNLService.class);

    @Inject
    HttpClient httpClient;

    @ConfigProperty(name = "notifynl.emailverificatie.url")
    String url;

    @ConfigProperty(name = "notifynl.emailverificatie.template-id")
    String templateId;

    @ConfigProperty(name = "notifynl.emailverificatie.api-key")
    String apiKey;

    @ConfigProperty(name = "rate.limit.max-requests", defaultValue = "5")
    int rateLimitMaxRequests;

    @ConfigProperty(name = "rate.limit.window-minutes", defaultValue = "15")
    int rateLimitWindowMinutes;

    private static final int HTTP_OK = 200;
    private static final int HTTP_CREATED = 201;
    private static final int API_KEY_MIN_LENGTH = 74;
    private static final int SERVICE_ID_START_OFFSET = 73;
    private static final int SERVICE_ID_LENGTH = 36;
    private static final int SECRET_LENGTH = 36;

    private final Map<String, List<Instant>> rateLimits = new ConcurrentHashMap<>();

    /**
     * Calls NotifyNL service via HTTP POST.
     * Returns true if successful (200 OK or 201 Created), false otherwise.
     */
    public boolean sendVerificationEmail(VerificationCode code, String email) {
        return sendVerificationEmail(code, email, null, null);
    }

    /**
     * Calls NotifyNL service via HTTP POST with optional custom API key and template ID.
     * Returns true if successful (200 OK or 201 Created), false otherwise.
     * 
     * @param code The verification code entity
     * @param email The recipient email address
     * @param customApiKey Optional custom API key (uses configured default if null or blank)
     * @param customTemplateId Optional custom template ID (uses configured default if null or blank)
     * @return true if email sent successfully, false otherwise
     */
    @CircuitBreaker(
            requestVolumeThreshold = 5,
            failureRatio = 1.0,
            delay = 30,
            delayUnit = ChronoUnit.SECONDS,
            successThreshold = 2
    )
    @Fallback(SendVerificationEmailFallbackHandler.class)
    public boolean sendVerificationEmail(VerificationCode code, String email, String customApiKey, String customTemplateId) {
        if (isRateLimited(email)) {
            LOG.warn("Rate limit exceeded for email: " + email);
            return false;
        }

        String effectiveApiKey = (customApiKey != null && !customApiKey.isBlank()) ? customApiKey : apiKey;
        String effectiveTemplateId = (customTemplateId != null && !customTemplateId.isBlank()) ? customTemplateId : templateId;

        String jsonBody = buildJsonBody(code.getCode(), email, effectiveTemplateId);
        ApiKeyDetails keys = extractServiceIdAndApiKey(effectiveApiKey);
        String token = createToken(keys.secret(), keys.serviceId());
        HttpRequest request = buildHttpRequest(jsonBody, token);

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (isSuccessResponse(response)) {
                LOG.info("Verification code sent for reference ID: " + code.getReferenceId());
                return true;
            }
            LOG.error("Failed to send verification code for reference ID: " + code.getReferenceId()
                    + ", status code: " + response.statusCode() + ", response: " + response.body());
            return false;
        } catch (IOException | InterruptedException e) {
            LOG.error("Failed to communicate with NotifyNL service for reference ID: " + code.getReferenceId()
                    + ", error: " + e.getMessage());
            throw new RuntimeException("Failed to communicate with NotifyNL service", e);
        }
    }

/**
     * Builds the JSON request body for NotifyNL API.
     */
    private String buildJsonBody(String code, String email, String effectiveTemplateId) {
        return String.format(
                "{\"personalisation\":{\"code\":\"%s\"},\"template_id\":\"%s\",\"email_address\":\"%s\"}",
                code, effectiveTemplateId, email
        );
    }

    /**
     * Builds the HTTP POST request with required headers.
     */
    private HttpRequest buildHttpRequest(String jsonBody, String token) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
    }

    /**
     * Validates if the HTTP response indicates success.
     */
    private boolean isSuccessResponse(HttpResponse<String> response) {
        return response.statusCode() == HTTP_OK || response.statusCode() == HTTP_CREATED;
    }


    private boolean isRateLimited(String email) {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(rateLimitWindowMinutes));
        AtomicBoolean limited = new AtomicBoolean(false);

        rateLimits.compute(email, (key, timestamps) -> {
            List<Instant> list = timestamps != null ? timestamps : new ArrayList<>();
            list.removeIf(t -> t.isBefore(cutoff));
            if (list.size() >= rateLimitMaxRequests) {
                limited.set(true);
                return list;
            }
            list.add(Instant.now());
            return list;
        });

        return limited.get();
    }

    @Scheduled(every = "{rate.limit.cleanup.schedule:1h}")
    void cleanupRateLimits() {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(rateLimitWindowMinutes));
        rateLimits.forEach((email, ignored) ->
            rateLimits.compute(email, (key, list) -> {
                if (list == null) return null;
                list.removeIf(t -> t.isBefore(cutoff));
                return list.isEmpty() ? null : list;
            })
        );
    }

    private record ApiKeyDetails(String serviceId, String secret) {}

    /**
     * Extracts service ID and secret from the NotifyNL API key.
     *
     * @param fromApiKey The API key to extract from
     * @return ApiKeyDetails containing service ID and secret
     * @throws IllegalArgumentException if API key is invalid
     */
    private static ApiKeyDetails extractServiceIdAndApiKey(String fromApiKey) {
        if (fromApiKey == null || fromApiKey.isBlank() || fromApiKey.contains(" ") || fromApiKey.length() < API_KEY_MIN_LENGTH) {
            throw new IllegalArgumentException(
                    "The API Key provided is invalid. Please ensure you are using a v2 API Key that is not empty or null");
        }

        int keyLength = fromApiKey.length();
        String serviceId = fromApiKey.substring(keyLength - SERVICE_ID_START_OFFSET, keyLength - SERVICE_ID_START_OFFSET + SERVICE_ID_LENGTH);
        String secret = fromApiKey.substring(keyLength - SECRET_LENGTH);

        return new ApiKeyDetails(serviceId, secret);
    }

    /**
     * Creates a JWT token for NotifyNL API authentication.
     *
     * @param secret    The API secret
     * @param serviceId The service ID
     * @return JWT token string
     */
    private static String createToken(String secret, String serviceId) {
        return Jwt.issuer(serviceId)
                .issuedAt(Instant.now())
                .signWithSecret(secret);
    }
}
