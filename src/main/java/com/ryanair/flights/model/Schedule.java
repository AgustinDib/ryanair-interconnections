package com.ryanair.flights.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Schedule {

    private Integer year;
    private Integer month;
    private List<Day> days;

    /**
     * Only needed for deserialization.
     */
    public Schedule() {}

    public Schedule(Integer month, List<Day> days) {
        this.month = month;
        this.days = days;
    }

    public Schedule(Integer year, Integer month, List<Day> days) {
        this.year = year;
        this.month = month;
        this.days = days;
    }
}
