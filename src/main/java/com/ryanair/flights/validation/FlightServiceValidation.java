package com.ryanair.flights.validation;

import com.ryanair.flights.exception.ValidationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class FlightServiceValidation {

    /**
     * Validates that parameters needed for finding interconnections are correct.
     * @param departure airport expressed as a IATA code.
     * @param arrival airport expressed as a IATA code.
     * @param departureDateTime as a LocalDateTime.
     * @param arrivalDateTime as a LocalDateTime.
     * @throws ValidationException in case validation fails.
     */
    public void validateInterconnectionsParameters(String departure, String arrival, LocalDateTime departureDateTime,
                                                    LocalDateTime arrivalDateTime) throws ValidationException {
        validateIataCode(departure);
        validateIataCode(arrival);
        validateDateTimes(departureDateTime, arrivalDateTime);
    }

    // TODO: Add more validations considering possible business requirements not defined yet.
    private void validateIataCode(String code) throws ValidationException {
        if (null == code) {
            throw new ValidationException("Airport IATA code can not be null.");
        } else if (code.length() != 3) {
            throw new ValidationException("Airport IATA code: " + code + " is not valid.");
        }
    }

    // TODO: Add more validations considering possible business requirements not defined yet.
    private void validateDateTimes(LocalDateTime departure, LocalDateTime arrival) throws ValidationException {
        if (null == departure) {
            throw new ValidationException("Departure time can not be null.");
        } else if(null == arrival) {
            throw new ValidationException("Arrival time can not be null.");
        } else if (arrival.isBefore(departure)) {
            throw new ValidationException("Arrival time can not be before departure time.");
        }
    }
}
