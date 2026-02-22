package uk.gov.dwp.uc.pairtest.cinema.tickets.domain;

public final class TicketTypeRequest {

    private final Type type;
    private final int noOfTickets;

    public TicketTypeRequest(Type type, int noOfTickets) {
        this.type = type;
        this.noOfTickets = noOfTickets;
    }

    public int getNoOfTickets() {
        return noOfTickets;
    }

    public Type getTicketType() {
        return type;
    }

    public enum Type {
        ADULT, CHILD, INFANT
    }
}
