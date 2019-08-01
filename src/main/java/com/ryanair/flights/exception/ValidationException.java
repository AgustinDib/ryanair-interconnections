package com.ryanair.flights.exception;

/**
 * Exception class that represents errors during validations.
 */
public class ValidationException extends Exception {

    public ValidationException(String errorMessage) {
        super(errorMessage);
    }
}
