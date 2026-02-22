package thirdparty.paymentgateway;

import uk.gov.dwp.uc.pairtest.cinema.tickets.exception.InvalidPurchaseException;

// Third-party payment service interface for processing ticket payments.
public interface TicketPaymentService {

    /**
      @param accountId            // account id (must be greater than 0)
      @param totalAmountToPay     //the total amount in pounds to charge (non-negative integer)
      @throws InvalidPurchaseException // if any constraints are violated
     */
    void makePayment(long accountId, int totalAmountToPay);
}
