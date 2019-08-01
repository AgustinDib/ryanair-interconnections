package com.ryanair.flights.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Leg {

    private String departureAirport;
    private String arrivalAirport;
    private LocalDateTime departureDateTime;
    private LocalDateTime arrivalDateTime;

    public Leg(String departureAirport, String arrivalAirport, LocalDateTime departureDateTime,
        LocalDateTime arrivalDateTime) {

        this.departureAirport = departureAirport;
        this.arrivalAirport = arrivalAirport;
        this.departureDateTime = departureDateTime;
        this.arrivalDateTime = arrivalDateTime;
    }
}
