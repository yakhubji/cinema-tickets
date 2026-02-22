package uk.gov.dwp.uc.pairtest.cinema.tickets;

import uk.gov.dwp.uc.pairtest.cinema.tickets.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.cinema.tickets.exception.InvalidPurchaseException;

public interface TicketService {

    /**
     * Purchases tickets for the given account.
     *
     * @param accountId      a valid account id
     * @param ticketTypeRequests one or more ticket requests 
     * @throws InvalidPurchaseException
     */
    void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests)
            throws InvalidPurchaseException;
}
