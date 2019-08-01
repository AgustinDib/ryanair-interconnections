package com.ryanair.flights.service;

import com.ryanair.flights.model.FlightResponse;
import com.ryanair.flights.exception.RestClientException;
import com.ryanair.flights.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.List;

public interface FlightServiceI {

    /**
     * Gets all the flights that one one trip combined in two get from departure to arrival for a given date range.
     * @param departure airport expressed in IATA code.
     * @param arrival airport expressed in IATA code.
     * @param departureDate in LocalDateTime.
     * @param arrivalDate in LocalDateTime.
     * @returna List FlightResponse with Legs for no stops and one stop flights.
     * @throws ValidationException when RestClient fails.
     * @throws ValidationException when input data is not valid.
     */
    List<FlightResponse> findInterconnections(String departure, String arrival, LocalDateTime departureDate,
        LocalDateTime arrivalDate) throws ValidationException, RestClientException;
}
