package com.ryanair.flights.service;

import com.ryanair.flights.exception.RestClientException;
import com.ryanair.flights.model.ConnectionRoute;
import com.ryanair.flights.model.Route;

import java.util.List;

public interface RouteServiceI {

    /**
     * Gets all the routes available that have RYANAIR as operator and null as connecting airport.
     * @return a List of Route that can be empty.
     * @throws RestClientException in case communication with API fails.
     */
    List<Route> getRoutes() throws RestClientException;

    /**
     * Gets all the routes that connect the given airports.
     * @param departure airport expressed in IATA code.
     * @param arrival airport expressed in IATA code.
     * @param allRoutes available.
     * @return a List of ConnectionRoute.
     */
    List<ConnectionRoute> getConnectionRoutes(String departure, String arrival, List<Route> allRoutes);

    /**
     * Finds if a direct flight exists for the given routes.
     * @param departure airport expressed in IATA code.
     * @param arrival airport expressed in IATA code.
     * @param allRoutes to search for flights.
     * @return true if a direct flight exists, else false.
     */
    boolean existDirectFlight(String departure, String arrival, List<Route> allRoutes);
}
