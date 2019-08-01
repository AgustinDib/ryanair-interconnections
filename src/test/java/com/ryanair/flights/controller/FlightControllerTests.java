package com.ryanair.flights.controller;

import com.ryanair.flights.model.FlightResponse;
import com.ryanair.flights.exception.RestClientException;
import com.ryanair.flights.exception.ValidationException;
import com.ryanair.flights.service.FlightServiceI;
import jdk.nashorn.internal.runtime.regexp.joni.exception.ValueException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest
public class FlightControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FlightServiceI flightService;

    private String validUrl = ("/v1/flight/interconnections?departure=DUB&arrival=WRO&" +
            "departureDateTime=2019-07-01T07:00&arrivalDateTime=2019-12-03T21:00");
    /**
     * Tests the case when flightService.findInterconnections returns a successful response. OK status should be
     * returned.
     */
    @Test
    public void interconnectionsHappyPath() throws Exception {
        List<FlightResponse> response = new ArrayList<>();
        given(flightService.findInterconnections(any(), any(), any(), any())).willReturn(response);

        this.mockMvc.perform(get(validUrl)).andExpect(status().isOk());
    }

    /**
     * Tests the case when flightService.findInterconnections throws a ValidationException. BAD_REQUEST status should be
     * returned.
     */
    @Test
    public void interconnectionsValidationExceptionThrown() throws Exception {
        given(flightService.findInterconnections(any(), any(), any(), any())).willThrow(new ValidationException(""));

        this.mockMvc.perform(get(validUrl)).andExpect(status().isBadRequest());
    }

    /**
     * Tests the case when flightService.findInterconnections throws a RestClientException. status code from Exception
     * should be returned.
     */
    @Test
    public void interconnectionsRestClientExceptionThrown() throws Exception {
        given(flightService.findInterconnections(any(), any(), any(), any()))
                .willThrow(new RestClientException(
                        "", new HttpClientErrorException(HttpStatus.FORBIDDEN), HttpStatus.FORBIDDEN));

        this.mockMvc.perform(get(validUrl)).andExpect(status().isForbidden());
    }

    /**
     * Tests the case when flightService.findInterconnections throws an Exception. INTERNAL_SERVER_ERROR status should
     * be returned.
     */
    @Test
    public void interconnectionsExceptionThrown() throws Exception {
        given(flightService.findInterconnections(any(), any(), any(), any()))
                .willThrow(new ValueException(""));

        this.mockMvc.perform(get(validUrl)).andExpect(status().isInternalServerError());
    }
}
