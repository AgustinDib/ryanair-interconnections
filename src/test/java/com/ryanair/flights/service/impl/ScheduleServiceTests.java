package com.ryanair.flights.service.impl;

import com.ryanair.flights.client.ScheduleClient;
import com.ryanair.flights.exception.RestClientException;
import com.ryanair.flights.exception.ServiceException;
import com.ryanair.flights.exception.ValidationException;
import com.ryanair.flights.model.Day;
import com.ryanair.flights.model.Flight;
import com.ryanair.flights.model.Schedule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.BDDMockito.given;

@RunWith(SpringRunner.class)
public class ScheduleServiceTests {

    @Mock
    ScheduleClient scheduleClient;

    @InjectMocks
    private ScheduleService scheduleService;

    LocalDateTime jan2019 = LocalDateTime.of(2019, 1, 15, 12, 0);
    LocalDateTime aug2019 = LocalDateTime.of(2019, 8, 15, 12, 0);
    LocalDateTime dec2019 = LocalDateTime.of(2019, 12, 15, 12, 0);
    LocalDateTime dec2020 = LocalDateTime.of(2020, 12, 15, 12, 0);

    Flight flight1 = new Flight(1, "10:00", "10:00");
    Flight flight2 = new Flight(2, "10:00", "11:00");
    Flight flight3 = new Flight(3, "14:00", "15:00");
    Flight flight4 = new Flight(4, "14:00", "15:00");
    List<Flight> flights = Stream.of(flight1, flight2, flight3, flight4).collect(Collectors.toList());

    Day day1 = new Day(1, flights);
    Day day15 = new Day(15, flights);
    Day day30 = new Day(30, flights);
    List<Day> days = Stream.of(day1, day15, day30).collect(Collectors.toList());

    Schedule scheduleDep = new Schedule(2019, 1, days);
    Schedule scheduleMid = new Schedule(2019, 4, days);
    Schedule scheduleArr = new Schedule(2019, 8, days);

    LocalDateTime from = LocalDateTime.of(2019, 6, 15, 0, 0);
    LocalDateTime fromSameYear = LocalDateTime.of(2019, 12, 15, 0, 0);
    LocalDateTime to = LocalDateTime.of(2022, 6, 15, 0, 0);

    @Before
    public void beforeEachTest() throws RestClientException {
        for (int month = 1; month <= 12; month++) {
            Schedule schedule = new Schedule(month, days);
            given(scheduleClient.getSchedule("EZE", "MDQ", 2019, month)).willReturn(Optional.of(schedule));
            given(scheduleClient.getSchedule("EZE", "MDQ", 2020, month)).willReturn(Optional.of(schedule));
            given(scheduleClient.getSchedule("MAD", "MDQ", 2020, month)).willReturn(Optional.of(schedule));
        }
    }

    /**
     * getSchedules should get schedules within the same year.
     */
    @Test
    public void getSchedulesSameYear() throws ValidationException, ServiceException {
        List<Schedule> result = scheduleService.getSchedules("EZE", "MDQ", jan2019, aug2019);

        Assert.assertTrue(result.size() == 8);
        Assert.assertTrue(result.get(0).getDays().size() == 2);
    }

    /**
     * getSchedules should get schedules across different years.
     */
    @Test
    public void getSchedulesSeveralYears() throws ValidationException, ServiceException {
        List<Schedule> result = scheduleService.getSchedules("EZE", "MDQ", jan2019, dec2020);

        Assert.assertTrue(result.size() == 24);
        Assert.assertTrue(result.get(23).getDays().size() == 2);
    }

    /**
     * filterNonValid should filter non valid days and flights for a departure month.
     */
    @Test
    public void filterNonValidFilterDaysAndFlightsForDepartureMonth() {
        Optional<Schedule> schedule = scheduleService.filterNonValid(scheduleDep, jan2019, aug2019);
        List<Day> filtered = schedule.get().getDays();

        Assert.assertTrue(filtered.size() == 2);
        Assert.assertTrue(filtered.get(0).getFlights().size() == 2);
        Assert.assertTrue(filtered.get(0).getFlights().get(0).getDepartureTime().equals(flight3.getDepartureTime()));
    }

    /**
     * filterNonValid should filter no days and flights for a month that is not of arrival nor departure.
     */
    @Test
    public void filterNonValidFilterNothingForMidMonth() {
        Optional<Schedule> schedule = scheduleService.filterNonValid(scheduleMid, jan2019, aug2019);
        List<Day> filtered = schedule.get().getDays();

        Assert.assertTrue(filtered.size() == 3);
        Assert.assertTrue(filtered.get(0).getFlights().size() == 4);
        Assert.assertTrue(filtered.get(0).getFlights().get(0).getDepartureTime().equals(flight1.getDepartureTime()));
    }

    /**
     * filterNonValid should filter non valid days and flights for an arrival month.
     */
    @Test
    public void filterNonValidFilterDaysAndFlightsForArrivalMonth() {
        Optional<Schedule> schedule = scheduleService.filterNonValid(scheduleArr, jan2019, aug2019);
        List<Day> filtered = schedule.get().getDays();

        Assert.assertTrue(filtered.size() == 2);
        Assert.assertTrue(filtered.get(1).getFlights().size() == 2);
        Assert.assertTrue(filtered.get(1).getFlights().get(0).getDepartureTime().equals(flight1.getDepartureTime()));
    }

    /**
     * getSchedulesForSeveralYears for a date range of two years should return 24 items.
     */
    @Test
    public void getSchedulesForSeveralYearsWholeYear() throws Exception {
        List<Schedule> result = scheduleService.getSchedulesForSeveralYears("EZE", "MDQ", jan2019, dec2020);

        Assert.assertTrue(result.size() == 24);
        Assert.assertTrue(result.get(0).getMonth() == 1);
        Assert.assertTrue(result.get(11).getMonth() == 12);
        Assert.assertTrue(result.get(23).getMonth() == 12);
    }

    /**
     * getSchedulesForSeveralYears for a date range of N months should return N items.
     */
    @Test
    public void getSchedulesForSeveralYearsPartialYears() throws Exception {
        List<Schedule> result = scheduleService.getSchedulesForSeveralYears("EZE", "MDQ", aug2019, dec2020);

        Assert.assertTrue(result.size() == 17);
        Assert.assertTrue(result.get(0).getMonth() == 8);
        Assert.assertTrue(result.get(16).getMonth() == 12);
    }

    /**
     * getSchedulesForSeveralYears for non valid dates should throw a ValidationException.
     */
    @Test(expected = ValidationException.class)
    public void getSchedulesForSeveralYearsNonValidDateRange() throws Exception {
        scheduleService.getSchedulesForSeveralYears("EZE", "MDQ", dec2020, jan2019);
    }

    /**
     * getSchedulesForSameYear for a date range of one year should return 12 items.
     */
    @Test
    public void getSchedulesForSameYearWholeYear() throws Exception {
        List<Schedule> result = scheduleService.getSchedulesForSameYear("EZE", "MDQ", jan2019, dec2019);

        Assert.assertTrue(result.size() == 12);
        Assert.assertTrue(result.get(0).getMonth() == 1);
        Assert.assertTrue(result.get(11).getMonth() == 12);
    }

    /**
     * getSchedulesForSameYear for a date range of N months should return N items.
     */
    @Test
    public void getSchedulesForSameYearPartialYear() throws Exception {
        List<Schedule> result = scheduleService.getSchedulesForSameYear("EZE", "MDQ", jan2019, aug2019);

        Assert.assertTrue(result.size() == 8);
        Assert.assertTrue(result.get(0).getMonth() == 1);
        Assert.assertTrue(result.get(7).getMonth() == 8);
    }

    /**
     * getSchedulesForSameYear for non valid dates should throw a ValidationException.
     */
    @Test(expected = ValidationException.class)
    public void getSchedulesForSameYearNonValidDateRange() throws Exception {
        scheduleService.getSchedulesForSameYear("EZE", "MDQ", aug2019, jan2019);
    }

    /**
     * A range of years for a date range across two years should be of two items length.
     */
    @Test
    public void getYearsRangeIncludeEdges() {
        List<Integer> result = scheduleService.getRange(from.getYear(), to.getYear());

        Assert.assertTrue(result.size() == 4);
        Assert.assertTrue(result.get(0) == 2019);
        Assert.assertTrue(result.get(3) == 2022);
    }

    /**
     * A range of years for a date range within the same year should be one item only.
     */
    @Test
    public void getYearsRangeSameYear() {
        List<Integer> result = scheduleService.getRange(from.getYear(), fromSameYear.getYear());

        Assert.assertTrue(result.size() == 1);
        Assert.assertTrue(result.get(0) == 2019);
    }

    /**
     * A range of years for a non valid date range should be empty.
     */
    @Test
    public void getYearsRangeInvertedDates() {
        List<Integer> result = scheduleService.getRange(to.getYear(), from.getYear());

        Assert.assertTrue(result.size() == 0);
    }
}
