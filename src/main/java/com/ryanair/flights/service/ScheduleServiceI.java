package com.ryanair.flights.service;

import com.ryanair.flights.exception.ServiceException;
import com.ryanair.flights.exception.ValidationException;
import com.ryanair.flights.model.Schedule;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleServiceI {

    /**
     * Gets a List o Schedule for the given date range.
     * @param departure airport expressed in IATA code.
     * @param arrival airport expressed in IATA code.
     * @param departureDate expressed in LocalDateTime.
     * @param arrivalDate expressed in LocalDateTime.
     * @return a List of Schedule for the given date range.
     * @throws ValidationException when date validation fails.
     */
    List<Schedule> getSchedules(String departure, String arrival, LocalDateTime departureDate,
        LocalDateTime arrivalDate) throws ValidationException, ServiceException;
}
