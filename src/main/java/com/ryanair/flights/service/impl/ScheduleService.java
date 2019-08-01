package com.ryanair.flights.service.impl;

import com.ryanair.flights.client.ScheduleClient;
import com.ryanair.flights.exception.RestClientException;
import com.ryanair.flights.exception.ValidationException;
import com.ryanair.flights.model.Day;
import com.ryanair.flights.model.Flight;
import com.ryanair.flights.model.Schedule;
import com.ryanair.flights.service.ScheduleServiceI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
public class ScheduleService implements ScheduleServiceI {

    private final ScheduleClient scheduleClient;

    @Autowired
    public ScheduleService(ScheduleClient scheduleClient) {
        this.scheduleClient = scheduleClient;
    }

    /**
     * Gets a List o Schedule for the given date range.
     * @param departure airport expressed in IATA code.
     * @param arrival airport expressed in IATA code.
     * @param departureDate expressed in LocalDateTime.
     * @param arrivalDate expressed in LocalDateTime.
     * @return a List of Schedule for the given date range.
     * @throws RestClientException when rest client fails.
     * @throws ValidationException when date validation fails.
     */
    @Override
    public List<Schedule> getSchedules(String departure, String arrival, LocalDateTime departureDate,
        LocalDateTime arrivalDate) throws RestClientException, ValidationException {

        List<Schedule> schedules = departureDate.getYear() == arrivalDate.getYear()
            ? getSchedulesForSameYear(departure, arrival, departureDate, arrivalDate)
            : getSchedulesForSeveralYears(departure, arrival, departureDate, arrivalDate);

        return schedules.stream().map(s -> filterNonValid(s, departureDate, arrivalDate))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    /**
     * Removes days and flights from a Schedule that are not within expected date range.
     * @param schedule to be filtered.
     * @param departure expressed in LocalDateTime.
     * @param arrival expressed in LocalDateTime.
     * @return Optional of a Schedule that can be empty if Schedule is not within expected date range.
     */
    Optional<Schedule> filterNonValid(Schedule schedule, LocalDateTime departure, LocalDateTime arrival) {
        int scheduleYear = schedule.getYear();
        int scheduleMonth = schedule.getMonth();
        int departureYear = departure.getYear();
        int arrivalYear = arrival.getYear();
        int departureMonth = departure.getMonthValue();
        int arrivalMonth = arrival.getMonthValue();

        if (isValidSchedule(schedule, departure, arrival)) {
            String errorMessage = "Schedule: " + schedule.toString() + ". Date range not valid for departure: "
                    + departure.toString() + "and arrival: "+ arrival.toString() + ".";
            log.warn(errorMessage);
            return Optional.empty();
        }

        // Clean days and flights before departure.
        if (scheduleYear == departureYear && scheduleMonth == departureMonth) {
            List<Day> days = removeDaysBeforeDeparture(schedule, departure);

            if (!days.isEmpty()) {
                List<Flight> flights = removeFlightsBeforeDeparture(departure, days);
                days.get(0).setFlights(flights);
            }
            schedule.setDays(days);

        // Clean days and flights after arrival.
        } else if (scheduleYear == arrivalYear && scheduleMonth == arrivalMonth) {
            List<Day> days = removeDaysAfterArrival(schedule, arrival);

            if (!days.isEmpty()) {
                List<Flight> flights = removeFlightsAfterArrival(departure, days);
                days.get(days.size() - 1).setFlights(flights);
            }
            schedule.setDays(days);
        }
        return Optional.of(schedule);
    }

    private List<Flight> removeFlightsAfterArrival(LocalDateTime departure, List<Day> days) {
        return days.get(days.size() - 1).getFlights().stream().filter(f -> {
            Integer hours = Integer.valueOf(f.getDepartureTime().substring(0,2));
            Integer minutes = Integer.valueOf(f.getDepartureTime().substring(3));
            return LocalTime.of(hours, minutes).isBefore(departure.toLocalTime());
        }).collect(Collectors.toList());
    }

    private List<Day> removeDaysAfterArrival(Schedule schedule, LocalDateTime arrival) {
        return schedule.getDays().stream().filter(d -> d.getDay() <= arrival.getDayOfMonth())
                .sorted(Comparator.comparingInt(Day::getDay)).collect(Collectors.toList());
    }

    private List<Flight> removeFlightsBeforeDeparture(LocalDateTime departure, List<Day> days) {
        return days.get(0).getFlights().stream().filter(f -> {
                Integer hours = Integer.valueOf(f.getDepartureTime().substring(0,2));
                Integer minutes = Integer.valueOf(f.getDepartureTime().substring(3));
                return LocalTime.of(hours, minutes).isAfter(departure.toLocalTime());
            }).collect(Collectors.toList());
    }

    private List<Day> removeDaysBeforeDeparture(Schedule schedule, LocalDateTime departure) {
        return schedule.getDays().stream().filter(d -> d.getDay() >= departure.getDayOfMonth())
            .sorted(Comparator.comparingInt(Day::getDay)).collect(Collectors.toList());
    }

    /**
     * Validates if a Schedule falls between a give range of dates.
     */
    private boolean isValidSchedule(Schedule schedule, LocalDateTime departure, LocalDateTime arrival) {
        return (schedule.getYear() < departure.getYear() || schedule.getYear() > arrival.getYear())
                || (schedule.getYear() == departure.getYear() && schedule.getMonth() < departure.getMonthValue())
                || (schedule.getYear() == arrival.getYear() && schedule.getMonth() > arrival.getMonthValue());
    }

    /**
     * Finds all the schedules per month for a range between two months of different years.
     * @param departure airport expressed in IATA code.
     * @param arrival airport expressed in IATA code.
     * @param departureDate in LocalDateTime.
     * @param arrivalDate in LocalDateTime.
     * @return a List of Schedule.
     * @throws RestClientException when rest client fails.
     * @throws ValidationException when date validation fails.
     */
    List<Schedule> getSchedulesForSeveralYears(String departure, String arrival, LocalDateTime departureDate,
        LocalDateTime arrivalDate) throws RestClientException, ValidationException {

        List<Schedule> schedules = new ArrayList<>();
        List<Integer> years = getYearsRange(departureDate, arrivalDate);
        if (years.size() == 0) {
            String msg = "Date range is not valid for departure: " + departureDate + "and arrival: "+ arrivalDate + ".";
            throw new ValidationException(msg);
        }
        int year = years.get(0);
        int month = departureDate.getMonthValue();

        while (year <= years.get(years.size() - 1)) {
            Optional<Schedule> schedule = scheduleClient.getSchedule(departure, arrival, year, month);

            if (schedule.isPresent()) {
                schedule.get().setYear(year);
                schedules.add(schedule.get());
            }
            month++;

            if (month > 12) {
                month = 1;
                year++;
            }
            // Compare current searching date with departure date.
            if (LocalDateTime.of(year, month, departureDate.getDayOfMonth(), departureDate.getHour(),
                    departureDate.getMinute(), departureDate.getSecond()).isAfter(arrivalDate)) {
                break;
            }
        }
        return schedules;
    }

    /**
     * Finds all the schedules per month for a range between two months of the same year.
     * @param departure airport expressed in IATA code.
     * @param arrival airport expressed in IATA code.
     * @param departureDate in LocalDateTime.
     * @param arrivalDate in LocalDateTime.
     * @return a List of Schedule.
     * @throws RestClientException when rest client fails.
     * @throws ValidationException when date validation fails.
     */
    List<Schedule> getSchedulesForSameYear(String departure, String arrival, LocalDateTime departureDate,
        LocalDateTime arrivalDate) throws RestClientException, ValidationException {

        if (arrivalDate.isBefore(departureDate)) {
            String msg = "Date range is not valid for departure: " + departureDate + "and arrival: "+ arrivalDate + ".";
            throw new ValidationException(msg);
        }

        List<Schedule> schedules = new ArrayList<>();

        for (int month = departureDate.getMonthValue(); month <= arrivalDate.getMonthValue(); month++) {
            Optional<Schedule> schedule = scheduleClient.getSchedule(departure, arrival, departureDate.getYear(), month);
            if (schedule.isPresent()) {
                schedule.get().setYear(departureDate.getYear());
                schedules.add(schedule.get());
            }
        }
        return schedules;
    }

    /**
     * Gets all the yeares between two dates including edges.
     * @param from expressed in LocalDateTime.
     * @param to expressed in LocalDateTime.
     * @return a List of Integer representing the range of years. For inverted dates returns an empty List.
     */
    List<Integer> getYearsRange(LocalDateTime from, LocalDateTime to) {
        return from.getYear() == to.getYear()
                ? Collections.singletonList(from.getYear())
                : IntStream.rangeClosed(from.getYear(), to.getYear()).boxed().collect(Collectors.toList());
    }
}
