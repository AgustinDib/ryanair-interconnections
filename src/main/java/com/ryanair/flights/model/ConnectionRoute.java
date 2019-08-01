package com.ryanair.flights.model;

import lombok.Data;

@Data
public class ConnectionRoute {
    Route departure;
    Route arrival;

    public ConnectionRoute(Route departure, Route arrival) {
        this.departure = departure;
        this.arrival = arrival;
    }
}
