package uk.gov.dwp.uc.pairtest.cinema.tickets.exception;


public class InvalidPurchaseException extends RuntimeException {

    public InvalidPurchaseException(String message) {
        super(message);
    }
}
