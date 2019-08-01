package com.ryanair.flights.validation;

import com.ryanair.flights.exception.ValidationException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;

@RunWith(SpringRunner.class)
public class FlightServiceValidationTests {

    @InjectMocks
    private FlightServiceValidation flightServiceValidation;

    LocalDateTime departure = LocalDateTime.of(2019, 7, 1, 7, 0);
    LocalDateTime arrival = LocalDateTime.of(2019, 12, 3, 21, 0);

    @Test
    public void parseValidParameters() throws Exception {
        flightServiceValidation.validateInterconnectionsParameters("EZE", "GLH", departure, arrival);
    }

    @Test(expected = ValidationException.class)
    public void parseTooShortAirport() throws Exception {
        flightServiceValidation.validateInterconnectionsParameters("EZ", "GLH", departure, arrival);
    }

    @Test(expected = ValidationException.class)
    public void parseTooLongAirport() throws Exception {
        flightServiceValidation.validateInterconnectionsParameters("EZEX", "GLH", departure, arrival);
    }

    @Test(expected = ValidationException.class)
    public void parseNonValidDates() throws Exception {
        flightServiceValidation.validateInterconnectionsParameters("EZE", "GLH", arrival, departure);
    }
}
