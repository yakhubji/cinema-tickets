package uk.gov.dwp.uc.pairtest.cinema.tickets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.cinema.tickets.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.cinema.tickets.domain.TicketTypeRequest.Type;
import uk.gov.dwp.uc.pairtest.cinema.tickets.exception.InvalidPurchaseException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TicketServiceImpl}.
 *
 * <p>Tests are organised by concern:</p>
 * <ul>
 *   <li>Account ID validation</li>
 *   <li>Request validation (null/empty/negative)</li>
 *   <li>Business rule validation</li>
 *   <li>Correct payment amount calculation</li>
 *   <li>Correct seat allocation calculation</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock
    private TicketPaymentService paymentService;

    @Mock
    private SeatReservationService seatReservationService;

    private TicketServiceImpl ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketServiceImpl(paymentService, seatReservationService);
    }

    // Account ID Validation 

    @Nested
    @DisplayName("Account ID validation")
    class AccountIdValidation {

        @Test
        @DisplayName("Null account ID throws InvalidPurchaseException")
        void nullAccountId_throwsException() {
            assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(null, new TicketTypeRequest(Type.ADULT, 1)));
        }

        @Test
        @DisplayName("Zero account ID throws InvalidPurchaseException")
        void zeroAccountId_throwsException() {
            assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(0L, new TicketTypeRequest(Type.ADULT, 1)));
        }

        @Test
        @DisplayName("Negative account ID throws InvalidPurchaseException")
        void negativeAccountId_throwsException() {
            assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(-5L, new TicketTypeRequest(Type.ADULT, 1)));
        }

        @Test
        @DisplayName("Positive account ID is accepted")
        void positiveAccountId_isAccepted() {
            assertDoesNotThrow(() ->
                ticketService.purchaseTickets(1L, new TicketTypeRequest(Type.ADULT, 1)));
        }
    }

   // Request Validation
    
    @Nested
    @DisplayName("Request validation")
    class RequestValidation {

        @Test
        @DisplayName("Null ticket request array throws InvalidPurchaseException")
        void nullRequestArray_throwsException() {
            assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L, (TicketTypeRequest[]) null));
        }

        @Test
        @DisplayName("Empty ticket request array throws InvalidPurchaseException")
        void emptyRequestArray_throwsException() {
            assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L));
        }

        @Test
        @DisplayName("Null element inside request array throws InvalidPurchaseException")
        void nullElementInArray_throwsException() {
            assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L, (TicketTypeRequest) null));
        }

        @Test
        @DisplayName("Negative ticket quantity throws InvalidPurchaseException")
        void negativeTicketQuantity_throwsException() {
            assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L, new TicketTypeRequest(Type.ADULT, -1)));
        }

        @Test
        @DisplayName("All-zero quantities (total = 0) throws InvalidPurchaseException")
        void allZeroQuantities_throwsException() {
            assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L,
                    new TicketTypeRequest(Type.ADULT, 0),
                    new TicketTypeRequest(Type.CHILD, 0)));
        }
    }

   // Business Rule Validation
 
    @Nested
    @DisplayName("Business rule validation")
    class BusinessRuleValidation {

        @Test
        @DisplayName("Purchasing more than 25 tickets throws InvalidPurchaseException")
        void moreThan25Tickets_throwsException() {
            assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L, new TicketTypeRequest(Type.ADULT, 26)));
        }

        @Test
        @DisplayName("Purchasing exactly 25 tickets is accepted")
        void exactly25Tickets_isAccepted() {
            assertDoesNotThrow(() ->
                ticketService.purchaseTickets(1L, new TicketTypeRequest(Type.ADULT, 25)));
        }

        @Test
        @DisplayName("Child ticket without Adult throws InvalidPurchaseException")
        void childWithoutAdult_throwsException() {
            assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L, new TicketTypeRequest(Type.CHILD, 1)));
        }

        @Test
        @DisplayName("Infant ticket without Adult throws InvalidPurchaseException")
        void infantWithoutAdult_throwsException() {
            assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L, new TicketTypeRequest(Type.INFANT, 1)));
        }

        @Test
        @DisplayName("More Infants than Adults throws InvalidPurchaseException")
        void moreInfantsThanAdults_throwsException() {
            // 1 adult, 2 infants — only 1 lap available
            assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L,
                    new TicketTypeRequest(Type.ADULT, 1),
                    new TicketTypeRequest(Type.INFANT, 2)));
        }

        @Test
        @DisplayName("Equal number of Infants and Adults is accepted")
        void equalInfantsAndAdults_isAccepted() {
            assertDoesNotThrow(() ->
                ticketService.purchaseTickets(1L,
                    new TicketTypeRequest(Type.ADULT, 2),
                    new TicketTypeRequest(Type.INFANT, 2)));
        }

        @Test
        @DisplayName("25 tickets split across types with enough Adults is accepted")
        void exactlyAtLimit_mixedTypes_isAccepted() {
            // 10 adults + 10 children + 5 infants = 25 tickets, 10 adults >= 5 infants
            assertDoesNotThrow(() ->
                ticketService.purchaseTickets(1L,
                    new TicketTypeRequest(Type.ADULT, 10),
                    new TicketTypeRequest(Type.CHILD, 10),
                    new TicketTypeRequest(Type.INFANT, 5)));
        }

        @Test
        @DisplayName("26 tickets split across types throws InvalidPurchaseException")
        void overLimit_mixedTypes_throwsException() {
            // 10 adults + 10 children + 6 infants = 26 tickets
            assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L,
                    new TicketTypeRequest(Type.ADULT, 10),
                    new TicketTypeRequest(Type.CHILD, 10),
                    new TicketTypeRequest(Type.INFANT, 6)));
        }
    }

   // Payment Amount Calculation
   
    @Nested
    @DisplayName("Payment amount calculation")
    class PaymentAmountCalculation {

        @Test
        @DisplayName("1 Adult costs £25")
        void oneAdult_charges25() {
            ticketService.purchaseTickets(1L, new TicketTypeRequest(Type.ADULT, 1));
            verify(paymentService).makePayment(1L, 25);
        }

        @Test
        @DisplayName("3 Adults cost £75")
        void threeAdults_charges75() {
            ticketService.purchaseTickets(1L, new TicketTypeRequest(Type.ADULT, 3));
            verify(paymentService).makePayment(1L, 75);
        }

        @Test
        @DisplayName("1 Child with 1 Adult costs £40")
        void oneChildOneAdult_charges40() {
            ticketService.purchaseTickets(1L,
                new TicketTypeRequest(Type.ADULT, 1),
                new TicketTypeRequest(Type.CHILD, 1));
            verify(paymentService).makePayment(1L, 40);
        }

        @Test
        @DisplayName("Infants are free — 1 Adult + 1 Infant costs £25")
        void oneAdultOneInfant_charges25() {
            ticketService.purchaseTickets(1L,
                new TicketTypeRequest(Type.ADULT, 1),
                new TicketTypeRequest(Type.INFANT, 1));
            verify(paymentService).makePayment(1L, 25);
        }

        @Test
        @DisplayName("Mixed purchase: 2 Adults + 3 Children + 2 Infants costs £95")
        void mixedPurchase_correctAmount() {
            // 2 * £25 + 3 * £15 + 2 * £0 = £50 + £45 = £95
            ticketService.purchaseTickets(1L,
                new TicketTypeRequest(Type.ADULT,  2),
                new TicketTypeRequest(Type.CHILD,  3),
                new TicketTypeRequest(Type.INFANT, 2));
            verify(paymentService).makePayment(1L, 95);
        }

        @Test
        @DisplayName("Maximum 25 Adult tickets costs £625")
        void twentyFiveAdults_charges625() {
            ticketService.purchaseTickets(1L, new TicketTypeRequest(Type.ADULT, 25));
            verify(paymentService).makePayment(1L, 625);
        }
    }

  // Seat Reservation Calculation
  
    @Nested
    @DisplayName("Seat reservation calculation")
    class SeatReservationCalculation {

        @Test
        @DisplayName("1 Adult reserves 1 seat")
        void oneAdult_reserves1Seat() {
            ticketService.purchaseTickets(1L, new TicketTypeRequest(Type.ADULT, 1));
            verify(seatReservationService).reserveSeat(1L, 1);
        }

        @Test
        @DisplayName("Infants do not get seats — 1 Adult + 1 Infant reserves 1 seat")
        void oneAdultOneInfant_reserves1Seat() {
            ticketService.purchaseTickets(1L,
                new TicketTypeRequest(Type.ADULT, 1),
                new TicketTypeRequest(Type.INFANT, 1));
            verify(seatReservationService).reserveSeat(1L, 1);
        }

        @Test
        @DisplayName("Children do get seats — 1 Adult + 2 Children reserves 3 seats")
        void oneAdultTwoChildren_reserves3Seats() {
            ticketService.purchaseTickets(1L,
                new TicketTypeRequest(Type.ADULT, 1),
                new TicketTypeRequest(Type.CHILD, 2));
            verify(seatReservationService).reserveSeat(1L, 3);
        }

        @Test
        @DisplayName("Mixed purchase: 2 Adults + 3 Children + 2 Infants reserves 5 seats")
        void mixedPurchase_correctSeats() {
            // Adults (2) + Children (3) = 5 seats; Infants (2) get no seat
            ticketService.purchaseTickets(1L,
                new TicketTypeRequest(Type.ADULT,  2),
                new TicketTypeRequest(Type.CHILD,  3),
                new TicketTypeRequest(Type.INFANT, 2));
            verify(seatReservationService).reserveSeat(1L, 5);
        }

        @Test
        @DisplayName("25 Adults reserve 25 seats")
        void twentyFiveAdults_reserves25Seats() {
            ticketService.purchaseTickets(1L, new TicketTypeRequest(Type.ADULT, 25));
            verify(seatReservationService).reserveSeat(1L, 25);
        }
    }

   
    // Integration-style: no external calls on failure
   

    @Nested
    @DisplayName("No external calls on invalid requests")
    class NoExternalCallsOnFailure {

        @Test
        @DisplayName("Payment service is NOT called when request is invalid")
        void invalidRequest_paymentServiceNotCalled() {
            assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L, new TicketTypeRequest(Type.CHILD, 1)));
            verifyNoInteractions(paymentService);
        }

        @Test
        @DisplayName("Seat reservation service is NOT called when request is invalid")
        void invalidRequest_seatServiceNotCalled() {
            assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L, new TicketTypeRequest(Type.INFANT, 3)));
            verifyNoInteractions(seatReservationService);
        }
    }
}
