package com.ryanair.flights.service.impl;

import com.ryanair.flights.client.RouteClient;
import com.ryanair.flights.exception.RestClientException;
import com.ryanair.flights.model.ConnectionRoute;
import com.ryanair.flights.model.Route;
import com.ryanair.flights.service.RouteServiceI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RouteService implements RouteServiceI {

    private final RouteClient routeClient;

    @Autowired
    public RouteService(RouteClient routeClient) {
        this.routeClient = routeClient;
    }

    /**
     * Gets all the routes available that have RYANAIR as operator and null as connecting airport.
     * @return a List of Route that can be empty.
     * @throws RestClientException in case communication with API fails.
     */
    @Override
    public List<Route> getRoutes() throws RestClientException {
        return routeClient.getRoutes().stream()
                .filter(r -> null == r.getConnectingAirport() && "RYANAIR".equalsIgnoreCase(r.getOperator()))
                .collect(Collectors.toList());
    }

    /**
     * Gets all the routes that connect the given airports.
     * @param departure airport expressed in IATA code.
     * @param arrival airport expressed in IATA code.
     * @param allRoutes available.
     * @return a List of ConnectionRoute.
     */
    @Override
    public List<ConnectionRoute> getConnectionRoutes(String departure, String arrival, List<Route> allRoutes) {
        List<Route> departureRoutes = allRoutes.stream().filter(r -> r.getAirportFrom().equals(departure)
                && !r.getAirportTo().equals(arrival)).collect(Collectors.toList());
        List<Route> arrivalRoutes = allRoutes.stream().filter(r -> !r.getAirportFrom().equals(departure)
                && r.getAirportTo().equals(arrival)).collect(Collectors.toList());

        List<ConnectionRoute> response = new ArrayList<>();

        for (Route dep : departureRoutes) {
            for (Route arr : arrivalRoutes) {
                if (dep.getAirportTo().equals(arr.getAirportFrom())) {
                    response.add(new ConnectionRoute(dep, arr));
                }
            }
        }
        return response;
    }

    /**
     * Finds if a direct flight exists for the given routes.
     * @param departure airport expressed in IATA code.
     * @param arrival airport expressed in IATA code.
     * @param allRoutes to search for flights.
     * @return true if a direct flight exists, else false.
     */
    @Override
    public boolean existDirectFlight(String departure, String arrival, List<Route> allRoutes) {
        return allRoutes.stream().anyMatch(r -> r.getAirportFrom().equalsIgnoreCase(departure)
                && r.getAirportTo().equalsIgnoreCase(arrival));
    }
}
