package thirdparty.seatbooking;

// Third-party seat reservation service interface for reserving seats.
public interface SeatReservationService {

    /**
     * Reserves the requested number of seats.
     *
     * @param accountId        the account making the reservation
     * @param totalSeatsToAllocate the total number of seats to allocate (non-negative integer)
     */
    void reserveSeat(long accountId, int totalSeatsToAllocate);
}
