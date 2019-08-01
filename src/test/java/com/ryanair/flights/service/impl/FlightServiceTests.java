package com.ryanair.flights.service.impl;

import com.ryanair.flights.exception.RestClientException;
import com.ryanair.flights.exception.ValidationException;
import com.ryanair.flights.model.*;
import com.ryanair.flights.service.RouteServiceI;
import com.ryanair.flights.service.ScheduleServiceI;
import com.ryanair.flights.validation.FlightServiceValidation;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@RunWith(SpringRunner.class)
public class FlightServiceTests {

    @Mock
    RouteServiceI routeService;

    @Mock
    ScheduleServiceI scheduleService;

    @Mock
    FlightServiceValidation flightServiceValidation;

    @InjectMocks
    private FlightService flightService;

    Route routeDep = new Route("EZE", "RIO", null, false, false, "RYANAIR", null);
    Route routeArr = new Route("RIO", "MDQ", null, false, false, "RYANAIR", null);
    Route routeRand = new Route("RIO", "JFK", null, false, false, "RYANAIR", null);
    Route routeDirect = new Route("EZE", "MDQ", null, false, false, "RYANAIR", null);

    List<ConnectionRoute> connections = Stream.of(new ConnectionRoute(routeDep, routeArr)).collect(Collectors.toList());

    LocalDateTime jan2019 = LocalDateTime.of(2019, 1, 14, 12, 0);
    LocalDateTime feb2019 = LocalDateTime.of(2019, 2, 16, 12, 0);

    Flight flight1 = new Flight(1, "13:00", "14:00");
    Flight flight2 = new Flight(2, "17:00", "18:00");
    List<Flight> flights = Stream.of(flight1, flight2).collect(Collectors.toList());

    Day day1 = new Day(1, flights);
    Day day15 = new Day(15, flights);
    Day day30 = new Day(28, flights);

    Schedule scheduleJan = new Schedule(2019, 1, Stream.of(day15, day30).collect(Collectors.toList()));
    Schedule scheduleFeb = new Schedule(2019, 2, Stream.of(day1, day15).collect(Collectors.toList()));

    List<Schedule> schedules = Stream.of(scheduleJan, scheduleFeb).collect(Collectors.toList());

    /**
     * Should return a List of FlightResponse with stops set as 0 for the first one and 1 for the rest, for 1 stop
     * responses 2 legs, departure airport "EZE" on leg 1 and    arrival airport "MDQ" on leg 2 are expected.
     */
    @Test
    public void  findInterconnectionsHappyPath() throws RestClientException, ValidationException {
        List<Route> allRoutes = Stream.of(routeDep, routeArr, routeRand, routeDirect).collect(Collectors.toList());
        given(routeService.getRoutes()).willReturn(allRoutes);
        given(scheduleService.getSchedules("EZE",  "MDQ", jan2019, feb2019)).willReturn(schedules);
        given(scheduleService.getSchedules("EZE",  "RIO", jan2019, feb2019)).willReturn(schedules);
        given(scheduleService.getSchedules("RIO",  "MDQ", jan2019, feb2019)).willReturn(schedules);
        given(routeService.existDirectFlight("EZE",  "MDQ", allRoutes)).willReturn(true);
        given(routeService.getConnectionRoutes("EZE", "MDQ", allRoutes)).willReturn(connections);

        List<FlightResponse> result = flightService.findInterconnections("EZE", "MDQ", jan2019, feb2019);
        Assert.assertTrue(!result.isEmpty());
        Assert.assertTrue(result.get(0).getStops() == 0);

        FlightResponse connectionFlight = result.get(1);
        List<Leg> legs = connectionFlight.getLegs();

        Assert.assertTrue(connectionFlight.getStops() == 1);
        Assert.assertTrue(legs.size() == 2);
        Assert.assertTrue(legs.get(0).getDepartureAirport().equals("EZE"));
        Assert.assertFalse(legs.get(0).getArrivalAirport().equals("EZE"));
        Assert.assertFalse(legs.get(1).getDepartureAirport().equals("EZE"));
        Assert.assertTrue(legs.get(1).getArrivalAirport().equals("MDQ"));
    }

    /**
     * Should return a List of FlightResponse with stops set as 1, 2 legs and departure airport "EZE" on leg 1 and
     * arrival airport "MDQ" on leg 2.
     */
    @Test
    public void getConnectingFlightsHappyPath() throws RestClientException, ValidationException {
        List<Route> allRoutes = Stream.of(routeDep, routeArr, routeRand).collect(Collectors.toList());
        given(routeService.getConnectionRoutes("EZE", "MDQ", allRoutes)).willReturn(connections);
        given(scheduleService.getSchedules(any(), any(), any(), any())).willReturn(schedules);

        List<FlightResponse> result = flightService.getConnectingFlights("EZE", "MDQ", jan2019, feb2019, allRoutes);

        Assert.assertTrue(!result.isEmpty());
        Assert.assertTrue(result.get(0).getStops() == 1);
        Assert.assertTrue(result.get(0).getLegs().size() == 2);
        Assert.assertTrue(result.get(0).getLegs().get(0).getDepartureAirport().equals("EZE"));
        Assert.assertFalse(result.get(0).getLegs().get(0).getArrivalAirport().equals("EZE"));
        Assert.assertFalse(result.get(0).getLegs().get(1).getDepartureAirport().equals("EZE"));
        Assert.assertTrue(result.get(0).getLegs().get(1).getArrivalAirport().equals("MDQ"));
    }

    /**
     * Should return an empty List if there are no connections.
     */
    @Test
    public void getConnectingFlightsNoConnections() throws RestClientException, ValidationException {
        List<Route> allRoutes = Stream.of(routeDep, routeArr, routeRand).collect(Collectors.toList());
        given(routeService.getConnectionRoutes("EZE", "MDQ", allRoutes)).willReturn(new ArrayList<>());

        List<FlightResponse> result = flightService.getConnectingFlights("EZE", "MDQ", jan2019, feb2019, allRoutes);

        Assert.assertTrue(result.isEmpty());
    }

    /**
     * Should return an empty List if there are no Schedules for the given date range.
     */
    @Test
    public void getConnectingFlightsNoSchedules() throws RestClientException, ValidationException {
        List<Route> allRoutes = Stream.of(routeDep, routeArr, routeRand).collect(Collectors.toList());
        given(routeService.getConnectionRoutes("EZE", "MDQ", allRoutes)).willReturn(connections);
        given(scheduleService.getSchedules(any(), any(), any(), any())).willReturn(new ArrayList<>());

        List<FlightResponse> result = flightService.getConnectingFlights("EZE", "MDQ", jan2019, feb2019, allRoutes);

        Assert.assertTrue(result.isEmpty());
    }

    /**
     * Should return a single FlightResponse and it's Legs.
     */
    @Test
    public void getDirectFlightsHappyPath() throws RestClientException, ValidationException {
        List<Schedule> schedules = Stream.of(scheduleJan, scheduleFeb).collect(Collectors.toList());
        given(scheduleService.getSchedules(any(), any(), any(), any())).willReturn(schedules);

        FlightResponse result = flightService.getDirectFlights("EZE", "MDQ", jan2019, feb2019);

        Assert.assertTrue(result.getStops() == 0);
        Assert.assertTrue(result.getLegs().size() == 8);
    }

    /**
     * Should return an empty List if there are no Schedules for the given parameters.
     */
    @Test
    public void getDirectFlightsNoSchedules() throws RestClientException, ValidationException {
        given(scheduleService.getSchedules(any(), any(), any(), any())).willReturn(new ArrayList<>());

        FlightResponse result = flightService.getDirectFlights("EZE", "MDQ", jan2019, feb2019);

        Assert.assertTrue(result.getStops() == 0);
        Assert.assertTrue(result.getLegs().size() == 0);
    }
}
