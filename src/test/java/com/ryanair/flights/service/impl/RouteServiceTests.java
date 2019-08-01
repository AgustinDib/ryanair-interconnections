package com.ryanair.flights.service.impl;

import com.ryanair.flights.client.RouteClient;
import com.ryanair.flights.model.ConnectionRoute;
import com.ryanair.flights.model.Route;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.BDDMockito.given;

@RunWith(SpringRunner.class)
public class RouteServiceTests {

    @Mock
    RouteClient routeClient;

    @InjectMocks
    private RouteService routeService;

    Route ryanairRoute = new Route("EZE", "MDQ", null, null, null, "RYANAIR", null);
    Route nonRyanairRoute = new Route("EZE", "MDQ", null, null, null, "OTHER", null);
    Route connectingAirportRoute = new Route("EZE", "MDQ", "BRC", null, null, "RYANAIR", null);

    Route route1 = new Route("EZE", "MDQ", null, null, null, "RYANAIR", null);
    Route route2 = new Route("MDQ", "MDL", null, null, null, "RYANAIR", null);
    Route route3 = new Route("EZE", "MDL", null, null, null, "RYANAIR", null);
    List<Route> allRoutes = Stream.of(route1, route2, route3).collect(Collectors.toList());

    /**
     * Should filter a Route from a non RYANAIR operator.
     */
    @Test
    public void getRoutesFilterByOperator() throws Exception {
        List<Route> response = Stream.of(ryanairRoute, nonRyanairRoute).collect(Collectors.toList());

        given(routeClient.getRoutes()).willReturn(response);

        Assert.assertTrue(routeService.getRoutes().size() == 1);
    }

    /**
     * Should filter a Route with a connecting airport.
     */
    @Test
    public void getRoutesFilterByConnectingAirport() throws Exception {
        List<Route> response = Stream.of(ryanairRoute, connectingAirportRoute).collect(Collectors.toList());

        given(routeClient.getRoutes()).willReturn(response);

        Assert.assertTrue(routeService.getRoutes().size() == 1);
    }

    /**
     * Should get the desired connection.
     */
    @Test
    public void getConnectionRoutesHappyPath() {
        List<ConnectionRoute> result = routeService.getConnectionRoutes("EZE", "MDL", allRoutes);

        Assert.assertTrue(result.size() == 1);
        Assert.assertTrue(result.get(0).getDeparture().getAirportFrom().equals("EZE"));
        Assert.assertTrue(result.get(0).getArrival().getAirportTo().equals("MDL"));
    }

    /**
     * Should get no connections.
     */
    @Test
    public void getConnectionRoutesNoConnections() {
        List<ConnectionRoute> result = routeService.getConnectionRoutes("MDQ", "MDL", allRoutes);

        Assert.assertTrue(result.isEmpty());
    }

    /**
     * Should return true if direct route exists.
     */
    @Test
    public void existDirectFlightYes() {
        Assert.assertTrue(routeService.existDirectFlight("EZE", "MDQ", allRoutes));
    }

    /**
     * Should return false if direct route does not exist.
     */
    @Test
    public void existDirectFlightNo() {
        Assert.assertTrue(routeService.existDirectFlight("EZE", "MDL", allRoutes));
    }
}
