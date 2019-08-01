package com.ryanair.flights.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Flight {

    private Integer number;
    private String departureTime;
    private String arrivalTime;

    /**
     * Only needed for deserialization.
     */
    public Flight() { }

    public Flight(Integer number, String departureTime, String arrivalTime) {
        this.number = number;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
    }
}
