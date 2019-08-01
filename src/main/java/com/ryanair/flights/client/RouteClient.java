package com.ryanair.flights.client;

import com.ryanair.flights.exception.RestClientException;
import com.ryanair.flights.model.Route;

import java.util.List;

public interface RouteClient {

    /**
     * Fetchs a List of all Routes via REST using the configured URL.
     * @return a List of Routes.
     * @throws RestClientException
     */
    List<Route> getRoutes() throws RestClientException;
}
