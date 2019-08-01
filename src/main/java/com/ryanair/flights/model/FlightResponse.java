package com.ryanair.flights.model;

import lombok.Data;

import java.util.List;

@Data
public class FlightResponse {

    private Integer stops;
    private List<Leg> legs;

    public FlightResponse(Integer stops, List<Leg> legs) {
        this.stops = stops;
        this.legs = legs;
    }

    public FlightResponse(Integer stops) {
        this.stops = stops;
    }
}
