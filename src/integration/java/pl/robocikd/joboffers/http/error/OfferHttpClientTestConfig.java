package pl.robocikd.joboffers.http.error;

import org.springframework.web.client.RestTemplate;
import pl.robocikd.joboffers.domain.offer.OfferFetchable;
import pl.robocikd.joboffers.infrastructure.offer.http.OfferHttpClientConfig;

import static pl.robocikd.joboffers.BaseIntegrationTest.WIRE_MOCK_HOST;

public class OfferHttpClientTestConfig extends OfferHttpClientConfig {

    public OfferFetchable remoteOfferTestClient(int port, int connectionTimeout, int readTimeout) {
        final RestTemplate restTemplate = restTemplate(connectionTimeout, readTimeout, restTemplateResponseErrorHandler());
        return remoteOfferClient(restTemplate, WIRE_MOCK_HOST, port);
    }
}
