package com.ryanair.flights.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Route {

    private String airportFrom;
    private String airportTo;
    private String connectingAirport;
    private Boolean newRoute;
    private Boolean seasonalRoute;
    private String operator;
    private String group;

    /**
     * Only needed for deserialization.
     */
    public Route() {}

    public Route(String airportFrom, String airportTo, String connectingAirport, Boolean newRoute,
                 Boolean seasonalRoute, String operator, String group) {

        this.airportFrom = airportFrom;
        this.airportTo = airportTo;
        this.connectingAirport = connectingAirport;
        this.newRoute = newRoute;
        this.seasonalRoute = seasonalRoute;
        this.operator = operator;
        this.group = group;
    }
}
