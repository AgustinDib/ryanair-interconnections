package com.ryanair.flights.controller;

import com.ryanair.flights.exception.RestClientException;
import com.ryanair.flights.exception.ValidationException;
import com.ryanair.flights.service.FlightServiceI;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Slf4j
@Api(value="Flight Controller", description="Flight related operations.")
@RestController
@RequestMapping("/v1/flight")
class FlightController {

    private final FlightServiceI flightService;
    private final CacheManager cacheManager;

    @Autowired
    public FlightController(FlightServiceI flightService, CacheManager cacheManager) {
        this.flightService = flightService;
        this.cacheManager = cacheManager;
    }

    /**
     * Returns a list of flights departing from a given departure airport not earlier than the specified departure
     * datetime and arriving to a given arrival airport not later than the specified arrival datetime.
     * @param departure airport expressed in IATA code.
     * @param arrival airport expressed in IATA code.
     * @param depDate departure date in ISO.DATE_TIME format.
     * @param arrDate arrival date in ISO.DATE_TIME format.
     * @return a ResponseEntity with an HttpStatus and a List of FlightResponse for successful hits, or a body
     * explaining the problem for failures.
     */
    @ApiOperation(value = "Gets interconnection flights.", response = ResponseEntity.class)
    @GetMapping("/interconnections")
    ResponseEntity<?> interconnections(
        @ApiParam(value = "Departure airport expressed in IATA code") @NotNull @RequestParam("departure") String departure,
        @NotNull @RequestParam("Arrival airport expressed in IATA code") String arrival,
        @NotNull @RequestParam("Departure date in ISO.DATE_TIME format") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime depDate,
        @NotNull @RequestParam("Arrival date in ISO.DATE_TIME format") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime arrDate
    ) {
        String logHeader = "FlightController.interconnections: ";
        log.info(logHeader + "request received for departure: " + departure + ", arrival: " + arrival +
                ", departureDateTime: " + depDate + ", arrivalDateTime: " + arrDate);

        try {
            return ResponseEntity.status(HttpStatus.OK)
                .body(flightService.findInterconnections(departure, arrival, depDate, arrDate));
        } catch (ValidationException e) {
            log.error(logHeader + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (RestClientException e) {
            log.error(logHeader + e.getMessage(), e);
            return ResponseEntity.status(e.getHttpStatus()).body(e.getMessage());
        } catch (Exception e) {
            log.error(logHeader + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    /**
     * Cache evicting task.
     */
    @Scheduled(cron = "0 0/30 * * * ?") // execute after every 30 min
    public void clearCacheSchedule(){
        for (String name : cacheManager.getCacheNames()) {
            cacheManager.getCache(name).clear();
        }
    }
}
