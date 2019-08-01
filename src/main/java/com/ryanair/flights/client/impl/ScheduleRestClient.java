package com.ryanair.flights.client.impl;

import com.ryanair.flights.client.ScheduleClient;
import com.ryanair.flights.exception.RestClientException;
import com.ryanair.flights.model.Schedule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Component
@Slf4j
public class ScheduleRestClient implements ScheduleClient {

    private RestTemplate restTemplate;

    @Value("${client.schedule.url}")
    private String getScheduleBaseUrl;

    @Autowired
    public ScheduleRestClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Fetchs a Schedul via REST using the configured URL and the given parameters.
     * @param departure airport IATA code.
     * @param arrival airport IATA code.
     * @param year expressed as an Integer.
     * @param month expressed as an Integer.
     * @return Optional of a Schedule.
     * @throws RestClientException when there is an error during REST call.
     */
    @Override
    @Cacheable("schedule")
    public Optional<Schedule> getSchedule(String departure, String arrival, Integer year, Integer month)
        throws RestClientException {

        String url = getScheduleBaseUrl + departure + "/" + arrival + "/years/" + year + "/months/" + month;
        String logHeader = "ScheduleRestClient.getSchedule departure. Url: " + url;

        log.info(logHeader + " Starting request.");

        try {
            ResponseEntity<Schedule> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Schedule>(){});

            return Optional.of(response.getBody());

        } catch (HttpClientErrorException ex) {
            String errorMsg = logHeader + " Client error status code: " + ex.getStatusText() + ". " + ex.getMessage();
            log.warn(errorMsg, ex);
            return Optional.empty();

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
