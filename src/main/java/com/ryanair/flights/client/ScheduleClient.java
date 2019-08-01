package com.ryanair.flights.client;

import com.ryanair.flights.exception.RestClientException;
import com.ryanair.flights.model.Schedule;

import java.util.Optional;

public interface ScheduleClient {

    /**
     * Fetchs a Schedul via REST using the configured URL and the given parameters.
     * @param departure airport IATA code.
     * @param arrival airport IATA code.
     * @param year expressed as an Integer.
     * @param month expressed as an Integer.
     * @return Optional of a Schedule.
     * @throws RestClientException when there is an error during REST call.
     */
    Optional<Schedule> getSchedule(String departure, String arrival, Integer year, Integer month)
        throws RestClientException;
}
