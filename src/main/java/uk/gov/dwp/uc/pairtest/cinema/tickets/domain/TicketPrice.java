package uk.gov.dwp.uc.pairtest.cinema.tickets.domain;

public class TicketPrice {

    private final String ticketType;
    private final int    price;
    private final boolean requiresSeat;

    public TicketPrice(String ticketType, int price, boolean requiresSeat) {
        this.ticketType   = ticketType;
        this.price        = price;
        this.requiresSeat = requiresSeat;
    }

    public String getTicketType()  { return ticketType;   }
    public int    getPrice()       { return price;        }
    public boolean isRequiresSeat(){ return requiresSeat; }

    @Override
    public String toString() {
        return "TicketPrice{type=" + ticketType
                + ", price=£" + price
                + ", requiresSeat=" + requiresSeat + "}";
    }
}
