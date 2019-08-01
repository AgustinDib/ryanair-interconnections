package com.ryanair.flights.client.impl;

import com.ryanair.flights.client.RouteClient;
import com.ryanair.flights.exception.RestClientException;
import com.ryanair.flights.model.Route;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
@Slf4j
public class RouteRestClient implements RouteClient {

    private RestTemplate restTemplate;

    @Value("${client.route.url}")
    private String getRoutesUrl;

    @Autowired
    public RouteRestClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Fetchs a List of all Routes via REST using the configured URL.
     * @return a List of Routes.
     * @throws RestClientException
     */
    @Override
    public List<Route> getRoutes() throws RestClientException {
        String logHeader = "RestTemplate.getRoutes. Url: " + getRoutesUrl;
        log.info(logHeader + " Starting request.");

        try {
            ResponseEntity<List<Route>> response = restTemplate.exchange(
                getRoutesUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Route>>(){});

            return response.getBody();
        } catch (HttpClientErrorException ex) {
            String errorMsg = logHeader + " Client error status code: " + ex.getStatusText() + ". " + ex.getMessage();
            log.error(errorMsg, ex);
            throw new RestClientException(errorMsg, ex, ex.getStatusCode());

        } catch (HttpServerErrorException ex) {
            String errorMsg = logHeader + " Server error status code: " + ex.getStatusText() + ". " + ex.getMessage();
            log.error(errorMsg, ex);
            throw new RestClientException(errorMsg, ex, ex.getStatusCode());

        } catch(org.springframework.web.client.RestClientException ex) {
            String errorMsg = logHeader + " Unknown error. " + ex.getMessage();
            log.error(errorMsg, ex);
            throw new RestClientException(errorMsg, ex, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
