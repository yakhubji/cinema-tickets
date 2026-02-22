package uk.gov.dwp.uc.pairtest.cinema.tickets;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.cinema.tickets.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.cinema.tickets.domain.TicketTypeRequest.Type;
import uk.gov.dwp.uc.pairtest.cinema.tickets.exception.InvalidPurchaseException;
import uk.gov.dwp.uc.pairtest.cinema.tickets.repository.TicketPriceRepository;

public class TicketServiceImpl implements TicketService {

    static final int MAX_TICKETS_PER_PURCHASE = 25;

    private final TicketPaymentService   paymentService;
    private final SeatReservationService seatReservationService;
    private final TicketPriceRepository  priceRepository;

    public TicketServiceImpl(TicketPaymentService paymentService,
                             SeatReservationService seatReservationService,
                             TicketPriceRepository priceRepository) {
        this.paymentService         = paymentService;
        this.seatReservationService = seatReservationService;
        this.priceRepository        = priceRepository;
    }

    public TicketServiceImpl(TicketPaymentService paymentService,
                             SeatReservationService seatReservationService) {
        this(paymentService, seatReservationService, new TicketPriceRepository());
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests)
            throws InvalidPurchaseException {

        try {
            validateAccountId(accountId);
            validateTicketRequests(ticketTypeRequests);

            int adultCount  = countTickets(ticketTypeRequests, Type.ADULT);
            int childCount  = countTickets(ticketTypeRequests, Type.CHILD);
            int infantCount = countTickets(ticketTypeRequests, Type.INFANT);

            int totalTickets = adultCount + childCount + infantCount;

            validateBusinessRules(adultCount, childCount, infantCount, totalTickets);

            int adultPrice  = priceRepository.getPriceByType("ADULT");
            int childPrice  = priceRepository.getPriceByType("CHILD");
            int infantPrice = priceRepository.getPriceByType("INFANT");

            int totalAmount = (adultCount  * adultPrice)
                            + (childCount  * childPrice)
                            + (infantCount * infantPrice);

            // infants sit on adult laps so no seat needed for them
            int totalSeats = adultCount + childCount;

            processPayment(accountId, totalAmount);
            reserveSeats(accountId, totalSeats);

        } catch (InvalidPurchaseException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidPurchaseException(
                "An unexpected error occurred while processing the ticket purchase: " + e.getMessage());
        }
    }

    private void processPayment(Long accountId, int totalAmount) {
        try {
            paymentService.makePayment(accountId, totalAmount);
        } catch (Exception e) {
            throw new InvalidPurchaseException(
                "Payment processing failed for account " + accountId + ": " + e.getMessage());
        }
    }

    private void reserveSeats(Long accountId, int totalSeats) {
        try {
            seatReservationService.reserveSeat(accountId, totalSeats);
        } catch (Exception e) {
            throw new InvalidPurchaseException(
                "Seat reservation failed for account " + accountId + ": " + e.getMessage());
        }
    }

    private void validateAccountId(Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw new InvalidPurchaseException(
                    "Account ID must be a valid positive integer, got: " + accountId);
        }
    }

    private void validateTicketRequests(TicketTypeRequest[] requests) {
        if (requests == null || requests.length == 0) {
            throw new InvalidPurchaseException("At least one ticket type request must be provided.");
        }
        for (TicketTypeRequest request : requests) {
            if (request == null) {
                throw new InvalidPurchaseException("Ticket request entries must not be null.");
            }
            if (request.getNoOfTickets() < 0) {
                throw new InvalidPurchaseException(
                        "Ticket quantity must not be negative for type: " + request.getTicketType());
            }
        }
    }

    private void validateBusinessRules(int adultCount, int childCount,
                                       int infantCount, int totalTickets) {
        if (totalTickets == 0) {
            throw new InvalidPurchaseException("At least one ticket must be purchased.");
        }

        if (totalTickets > MAX_TICKETS_PER_PURCHASE) {
            throw new InvalidPurchaseException(
                    "Cannot purchase more than " + MAX_TICKETS_PER_PURCHASE
                    + " tickets in a single transaction. Requested: " + totalTickets);
        }

        if ((childCount > 0 || infantCount > 0) && adultCount == 0) {
            throw new InvalidPurchaseException(
                    "Child and Infant tickets cannot be purchased without at least one Adult ticket.");
        }

        if (infantCount > adultCount) {
            throw new InvalidPurchaseException(
                    "Number of Infant tickets (" + infantCount
                    + ") cannot exceed the number of Adult tickets (" + adultCount
                    + "), as each infant must sit on an adult's lap.");
        }
    }

    private int countTickets(TicketTypeRequest[] requests, Type type) {
        int total = 0;
        for (TicketTypeRequest request : requests) {
            if (request.getTicketType() == type) {
                total += request.getNoOfTickets();
            }
        }
        return total;
    }
}
