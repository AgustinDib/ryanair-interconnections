# ryanair-interconnections

## Project overview
The following is a Spring Boot Maven based project that exposes an endpoint for obtaining information about possible flights provided by Ryanair from an airport to another, within a give date range.
## Project design
This application is designed following a layered pattern which includes:
- A Controller layer for dealing with requests and successful responses, as well as transforming an exception into a proper status and message. It only performs type validations for input parameters.
- A Service layer that takes care of collecting information, performing business validations and doing business operations.
- A Client layer responsible for communication with external services. Currently only Rest clients are implemented.
## API
The application responds to following request URI with given query parameters:

http://{HOST}/{VERSION}/{CONTEXT}/interconnections?departure={departure}&arrival=
{arrival}&departureDateTime={departureDateTime}&arrivalDateTime=
{arrivalDateTime}

where:
- **departure:** a departure airport IATA code.
- **departureDateTime:** a departure datetime in the departure airport timezone in
ISO format.
- **arrival:** an arrival airport IATA code.
- **arrivalDateTime:** an arrival datetime in the arrival airport timezone in ISO format.
## Example request
The following request can be used for performing a happy path integration test of the application:

GET: http://localhost:8080/v1/flight/interconnections?departure=STN&arrival=ATH&departureDateTime=2019-07-01T:00&arrivalDateTime=2019-08-04T21:00



