package com.ryanair.flights.service.impl;

import com.ryanair.flights.client.ScheduleClient;
import com.ryanair.flights.exception.RestClientException;
import com.ryanair.flights.exception.ServiceException;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
public class ScheduleService implements ScheduleServiceI {

    private final ScheduleClient scheduleClient;
    private final ForkJoinPool threadPool;

    @Autowired
    public ScheduleService(ScheduleClient scheduleClient, ForkJoinPool threadPool) {
        this.scheduleClient = scheduleClient;
        this.threadPool = threadPool != null ? threadPool : new ForkJoinPool(4);
    }

    /**
     * Gets a List o Schedule for the given date range.
     * @param departure airport expressed in IATA code.
     * @param arrival airport expressed in IATA code.
     * @param departureDate expressed in LocalDateTime.
     * @param arrivalDate expressed in LocalDateTime.
     * @return a List of Schedule for the given date range.
     * @throws ValidationException when date validation fails.
     */
    @Override
    public List<Schedule> getSchedules(String departure, String arrival, LocalDateTime departureDate,
        LocalDateTime arrivalDate) throws ValidationException, ServiceException {

        List<Schedule> schedules;
        try {
            schedules = departureDate.getYear() == arrivalDate.getYear()
                    ? getSchedulesForSameYear(departure, arrival, departureDate, arrivalDate)
                    : getSchedulesForSeveralYears(departure, arrival, departureDate, arrivalDate);
        } catch (ExecutionException | InterruptedException e) {
            throw new ServiceException("Error during Schedule fetching: " + e.getMessage(), e);
        }

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
     * @throws ValidationException when date validation fails.
     * @throws ExecutionException when parallel processing fails.
     * @throws InterruptedException when parallel processing fails.
     */
    List<Schedule> getSchedulesForSeveralYears(String departure, String arrival, LocalDateTime departureDate,
        LocalDateTime arrivalDate) throws ValidationException, ExecutionException, InterruptedException {

        List<Integer> years = getRange(departureDate.getYear(), arrivalDate.getYear());
        if (years.size() <= 1) {
            String msg = "Date range is not valid for departure: " + departureDate + "and arrival: "+ arrivalDate + ".";
            throw new ValidationException(msg);
        }
        int firstYear = years.get(0);
        int lastYear = years.get(years.size() - 1);
        List<Integer> inBetweenYears = years.size() > 2 ? getRange(firstYear + 1, lastYear - 1) : new ArrayList<>();

        // Get schedules for first year.
        List<Schedule> schedules = new ArrayList<>(getSchedulesForSameYear(departure, arrival, departureDate,
                LocalDateTime.of(firstYear, 12, 31, 23, 59)));

        // Get schedules for in between years.
        for (Integer y : inBetweenYears) {
            schedules.addAll(getSchedulesForSameYear(departure, arrival, LocalDateTime.of(y, 1, 1, 0, 0),
                    LocalDateTime.of(y, 12, 31, 23, 59)));
        }
        // Get schedules for last year.
        schedules.addAll(getSchedulesForSameYear(departure, arrival, LocalDateTime.of(lastYear, 1, 1, 0, 0),
                arrivalDate));

        return schedules;
    }

    /**
     * Finds all the schedules per month for a range between two months of the same year.
     * @param departure airport expressed in IATA code.
     * @param arrival airport expressed in IATA code.
     * @param departureDate in LocalDateTime.
     * @param arrivalDate in LocalDateTime.
     * @return a List of Schedule.
     */
    List<Schedule> getSchedulesForSameYear(String departure, String arrival, LocalDateTime departureDate,
        LocalDateTime arrivalDate) throws ValidationException, ExecutionException, InterruptedException {

        if (arrivalDate.isBefore(departureDate)) {
            String msg = "Date range is not valid for departure: " + departureDate + "and arrival: "+ arrivalDate + ".";
            throw new ValidationException(msg);
        }

        List<Integer> monthsRange = getRange(departureDate.getMonthValue(), arrivalDate.getMonthValue());

        return threadPool.submit(() -> monthsRange.parallelStream().map(month -> {
            Optional<Schedule> schedule = Optional.empty();
            try {
                schedule = scheduleClient.getSchedule(departure, arrival, departureDate.getYear(), month);
                schedule.ifPresent(schedule1 -> schedule1.setYear(departureDate.getYear()));
                return schedule;
            } catch (RestClientException e) {
                log.warn("Error getting schedules, status code: " + e.getHttpStatus() + ". Message: " + e.getMessage());
                return schedule;
            }
        }).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList())).get();
    }

    /**
     * Gets all the items between two integers including edges.
     * @param from expressed in Integer.
     * @param to expressed in Integer.
     * @return a List of Integer representing the range of numbers. For inverted numbers returns an empty List.
     */
    List<Integer> getRange(Integer from, Integer to) {
        return Objects.equals(from, to) ? Collections.singletonList(from)
                : IntStream.rangeClosed(from, to).boxed().collect(Collectors.toList());
    }
}
