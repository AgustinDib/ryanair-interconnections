package com.ryanair.flights.service.impl;

import com.ryanair.flights.model.FlightResponse;
import com.ryanair.flights.model.Leg;
import com.ryanair.flights.exception.RestClientException;
import com.ryanair.flights.exception.ValidationException;
import com.ryanair.flights.model.*;
import com.ryanair.flights.service.FlightServiceI;
import com.ryanair.flights.service.RouteServiceI;
import com.ryanair.flights.service.ScheduleServiceI;
import com.ryanair.flights.validation.FlightServiceValidation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FlightService implements FlightServiceI {

    private final RouteServiceI routeService;
    private final ScheduleServiceI scheduleService;
    private final FlightServiceValidation flightServiceValidation;

    @Autowired
    public FlightService(RouteServiceI routeService, ScheduleServiceI scheduleService,
        FlightServiceValidation flightServiceValidation) {

        this.routeService = routeService;
        this.scheduleService = scheduleService;
        this.flightServiceValidation = flightServiceValidation;
    }

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
    @Override
    public List<FlightResponse> findInterconnections(String departure, String arrival, LocalDateTime departureDate,
         LocalDateTime arrivalDate) throws ValidationException, RestClientException {

        flightServiceValidation.validateInterconnectionsParameters(departure, arrival, departureDate, arrivalDate);

        List<Route> allRoutes = routeService.getRoutes();

        // If direct routes exist, check for direct flights, else create an emtpy list of flights.
        FlightResponse directFlights = routeService.existDirectFlight(departure, arrival, allRoutes)
            ? getDirectFlights(departure, arrival, departureDate, arrivalDate)
            : new FlightResponse(0);

        List<FlightResponse> connectingFlights = getConnectingFlights(departure, arrival, departureDate, arrivalDate, allRoutes);

        List<FlightResponse> responses = new ArrayList<>();
        responses.add(directFlights);
        responses.addAll(connectingFlights);

        return responses;
    }

    /**
     * Gets all the flights that combined in two get from departure to arrival for a given date range.
     * @param departure airport expressed in IATA code.
     * @param arrival airport expressed in IATA code.
     * @param departureDate in LocalDateTime.
     * @param arrivalDate in LocalDateTime.
     * @param allRoutes available to find connections from.
     * @return a List FlightResponse with Legs.
     * @throws RestClientException when RestClient fails.
     * @throws ValidationException when input data is not valid.
     */
    List<FlightResponse> getConnectingFlights(String departure, String arrival, LocalDateTime departureDate,
        LocalDateTime arrivalDate, List<Route> allRoutes) throws RestClientException, ValidationException {

        List<ConnectionRoute> connectionRoutes = routeService.getConnectionRoutes(departure, arrival, allRoutes);
        List<FlightResponse> responses = new ArrayList<>();

        for (ConnectionRoute cr : connectionRoutes) {
            String depFrom = cr.getDeparture().getAirportFrom();
            String depTo = cr.getDeparture().getAirportTo();
            List<Leg> departureLegs = getDirectFlights(depFrom, depTo, departureDate, arrivalDate).getLegs();

            String arrFrom = cr.getArrival().getAirportFrom();
            String arrTo = cr.getArrival().getAirportTo();
            List<Leg> arrivalLegs = getDirectFlights(arrFrom, arrTo, departureDate, arrivalDate).getLegs();

            for (Leg depLeg : departureLegs) {
                LocalDateTime depFromConnection = depLeg.getArrivalDateTime().plusHours(2);
                List<Leg> filteredArrivalLegs = arrivalLegs.stream()
                    .filter(leg -> leg.getDepartureDateTime().isAfter(depFromConnection))
                    .filter(leg -> leg.getDepartureAirport().equals(depLeg.getArrivalAirport()))
                    .collect(Collectors.toList());

                for (Leg leg : filteredArrivalLegs) {
                    responses.add(new FlightResponse(1, Stream.of(depLeg, leg).collect(Collectors.toList())));
                }
            }
        }

        return responses;
    }

    /**
     * Gets all the direct flights between two airports for a given date range.
     * @param departure airport expressed in IATA code.
     * @param arrival airport expressed in IATA code.
     * @param departureDate in LocalDateTime.
     * @param arrivalDate in LocalDateTime.
     * @return a FlightResponse with Legs.
     * @throws RestClientException when RestClient fails.
     * @throws ValidationException when input data is not valid.
     */
    FlightResponse getDirectFlights(String departure, String arrival, LocalDateTime departureDate,
        LocalDateTime arrivalDate) throws RestClientException, ValidationException {

        List<Schedule> schedules = scheduleService.getSchedules(departure, arrival, departureDate, arrivalDate);
        List<Leg> legs = new ArrayList<>();

        // Extract the flights from every day of every schedule.
        for (Schedule schedule : schedules) {
            for (Day day : schedule.getDays()) {
                for (Flight flight : day.getFlights()) {
                    LocalDateTime dep = getLocalDateTime(schedule, day, flight.getDepartureTime());
                    LocalDateTime arr = getLocalDateTime(schedule, day, flight.getArrivalTime());

                    legs.add(new Leg(departure, arrival, dep, arr));
                }
            }
        }
        return new FlightResponse(0, legs);
    }

    private LocalDateTime getLocalDateTime(Schedule schedule, Day day, String time) {
        return LocalDateTime.of(schedule.getYear(), schedule.getMonth(), day.getDay(),
                Integer.valueOf(time.substring(0, 2)),
                Integer.valueOf(time.substring(4)));
    }
}
