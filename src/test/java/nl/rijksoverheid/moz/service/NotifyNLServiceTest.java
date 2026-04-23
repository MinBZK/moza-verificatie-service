package nl.rijksoverheid.moz.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import nl.rijksoverheid.moz.entity.VerificationCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import java.lang.reflect.Method;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import static org.mockito.ArgumentMatchers.any;

@QuarkusTest
public class NotifyNLServiceTest {

    private NotifyNLService notifyNLService;

    @BeforeEach
    void setup() throws Exception {
        notifyNLService = new NotifyNLService();
        // Set necessary fields via reflection or assume they are picked up from application.properties if using @QuarkusTest
        // Actually NotifyNLService uses @ConfigProperty, which Quarkus injects.
        // But since we are creating it with 'new', we might need to set them manually or use @Inject.
    }

    @jakarta.inject.Inject
    NotifyNLService injectedNotifyNLService;

    @InjectMock
    HttpClient mockHttpClient;

    @Test
    void testExtractServiceIdAndApiKey() throws Exception {
        Method method = NotifyNLService.class.getDeclaredMethod("extractServiceIdAndApiKey", String.class);
        method.setAccessible(true);

        // Valid API key format: some-prefix-SERVICE_ID-SECRET
        // From code: 
        // serviceId = fromApiKey.substring(keyLength - 73, keyLength - 73 + 36);
        // secret = fromApiKey.substring(keyLength - 36);
        // Total length >= 74
        
        String serviceId = "f7cceea0-ecea-4102-9b93-23a0a3cc0a07";
        String secret = "69f428c9-2567-4e98-932a-8abe83fb89d7";
        String apiKey = "prefix-" + serviceId + "-" + secret;
        // Adjust length to meet requirement >= 74
        while (apiKey.length() < 74) {
            apiKey = "a" + apiKey;
        }

        Object result = method.invoke(null, apiKey);
        
        Method getServiceId = result.getClass().getDeclaredMethod("serviceId");
        Method getSecret = result.getClass().getDeclaredMethod("secret");
        
        Assertions.assertEquals(serviceId, getServiceId.invoke(result));
        Assertions.assertEquals(secret, getSecret.invoke(result));
    }

    @Test
    void testExtractServiceIdAndApiKeyInvalid() throws Exception {
        Method method = NotifyNLService.class.getDeclaredMethod("extractServiceIdAndApiKey", String.class);
        method.setAccessible(true);

        // Too short
        Assertions.assertThrows(Exception.class, () -> {
            try {
                method.invoke(null, "too-short");
            } catch (Exception e) {
                throw (Exception) e.getCause();
            }
        });

        // Null
        Assertions.assertThrows(Exception.class, () -> {
            try {
                method.invoke(null, (String) null);
            } catch (Exception e) {
                throw (Exception) e.getCause();
            }
        });
    }

    @Test
    void testCreateToken() throws Exception {
        Method method = NotifyNLService.class.getDeclaredMethod("createToken", String.class, String.class);
        method.setAccessible(true);

        String secret = "69f428c9-2567-4e98-932a-8abe83fb89d7";
        String serviceId = "f7cceea0-ecea-4102-9b93-23a0a3cc0a07";

        String token = (String) method.invoke(null, secret, serviceId);
        Assertions.assertNotNull(token);
        Assertions.assertFalse(token.isEmpty());
    }

    @Test
    void testBuildJsonBody() throws Exception {
        Method method = NotifyNLService.class.getDeclaredMethod("buildJsonBody", String.class, String.class, String.class);
        method.setAccessible(true);

        String json = (String) method.invoke(injectedNotifyNLService, "123456", "test@example.com", "my-template-id");
        Assertions.assertTrue(json.contains("\"code\":\"123456\""));
        Assertions.assertTrue(json.contains("\"email_address\":\"test@example.com\""));
        Assertions.assertTrue(json.contains("\"template_id\":\"my-template-id\""));
    }

    @Test
    void testExtractServiceIdAndApiKeySpaces() throws Exception {
        Method method = NotifyNLService.class.getDeclaredMethod("extractServiceIdAndApiKey", String.class);
        method.setAccessible(true);

        Assertions.assertThrows(Exception.class, () -> {
            try {
                method.invoke(null, "a".repeat(74) + " ");
            } catch (Exception e) {
                throw (Exception) e.getCause();
            }
        });
    }

    @Test
    void testExtractServiceIdAndApiKeyBlank() throws Exception {
        Method method = NotifyNLService.class.getDeclaredMethod("extractServiceIdAndApiKey", String.class);
        method.setAccessible(true);

        Assertions.assertThrows(Exception.class, () -> {
            try {
                method.invoke(null, "   ");
            } catch (Exception e) {
                throw (Exception) e.getCause();
            }
        });
    }

    @Test
    void testExtractServiceIdAndApiKeyEmpty() throws Exception {
        Method method = NotifyNLService.class.getDeclaredMethod("extractServiceIdAndApiKey", String.class);
        method.setAccessible(true);

        Assertions.assertThrows(Exception.class, () -> {
            try {
                method.invoke(null, "");
            } catch (Exception e) {
                throw (Exception) e.getCause();
            }
        });
    }

    private static final String VALID_SERVICE_ID = "f7cceea0-ecea-4102-9b93-23a0a3cc0a07";
    private static final String VALID_SECRET     = "69f428c9-2567-4e98-932a-8abe83fb89d7";
    private static final String VALID_API_KEY    = "prefix-" + VALID_SERVICE_ID + "-" + VALID_SECRET;

    @Test
    void testSendVerificationEmailSuccess() throws Exception {
        HttpResponse<String> mockResponse = Mockito.mock();
        Mockito.when(mockResponse.statusCode()).thenReturn(200);
        Mockito.doReturn(mockResponse).when(mockHttpClient).send(any(), any());

        VerificationCode code = new VerificationCode();
        Assertions.assertTrue(injectedNotifyNLService.sendVerificationEmail(code, "test@example.com"));
    }

    @Test
    void testSendVerificationEmailFailure() throws Exception {
        HttpResponse<String> mockResponse = Mockito.mock();
        Mockito.when(mockResponse.statusCode()).thenReturn(400);
        Mockito.when(mockResponse.body()).thenReturn("Bad request");
        Mockito.doReturn(mockResponse).when(mockHttpClient).send(any(), any());

        VerificationCode code = new VerificationCode();
        Assertions.assertFalse(injectedNotifyNLService.sendVerificationEmail(code, "test@example.com"));
    }

    @Test
    void testSendVerificationEmailException() throws Exception {
        Mockito.doThrow(new RuntimeException("Connection refused")).when(mockHttpClient).send(any(), any());

        VerificationCode code = new VerificationCode();
        Assertions.assertFalse(injectedNotifyNLService.sendVerificationEmail(code, "test@example.com"));
    }

    @Test
    void testSendVerificationEmailWithCustomApiKeyAndTemplateId() throws Exception {
        HttpResponse<String> mockResponse = Mockito.mock();
        Mockito.when(mockResponse.statusCode()).thenReturn(201);
        Mockito.doReturn(mockResponse).when(mockHttpClient).send(any(), any());

        VerificationCode code = new VerificationCode();
        Assertions.assertTrue(injectedNotifyNLService.sendVerificationEmail(
                code, "test@example.com", VALID_API_KEY, "custom-template-id"));
    }

    @Test
    void testIsSuccessResponse() throws Exception {
        Method method = NotifyNLService.class.getDeclaredMethod("isSuccessResponse", java.net.http.HttpResponse.class);
        method.setAccessible(true);

        HttpResponse<String> response200 = Mockito.mock();
        Mockito.when(response200.statusCode()).thenReturn(200);
        Assertions.assertTrue((Boolean) method.invoke(notifyNLService, response200));

        HttpResponse<String> response201 = Mockito.mock();
        Mockito.when(response201.statusCode()).thenReturn(201);
        Assertions.assertTrue((Boolean) method.invoke(notifyNLService, response201));

        HttpResponse<String> response400 = Mockito.mock();
        Mockito.when(response400.statusCode()).thenReturn(400);
        Assertions.assertFalse((Boolean) method.invoke(notifyNLService, response400));
    }
}
