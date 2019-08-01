package com.ryanair.flights.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Day {

    private Integer day;
    private List<Flight> flights;

    /**
     * Only needed for deserialization.
     */
    public Day() { }

    public Day(Integer day, List<Flight> flights) {
        this.day = day;
        this.flights = flights;
    }
}
