package nl.rijksoverheid.moz.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.http.HttpClient;
import java.time.Duration;

@ApplicationScoped
public class HttpClientProducer {

    @ConfigProperty(name = "httpclient.connect-timeout-seconds", defaultValue = "10")
    int connectTimeoutSeconds;

    @ConfigProperty(name = "httpclient.request-timeout-seconds", defaultValue = "30")
    int requestTimeoutSeconds;

    @Produces
    @ApplicationScoped
    public HttpClient produceHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                .version(HttpClient.Version.HTTP_2)
                .build();
    }
}
