package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
final class TicketServiceImplTest {

    @Mock
    private TicketPaymentService ticketPaymentService;

    @Mock
    private SeatReservationService seatReservationService;

    private TicketServiceImpl ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketServiceImpl(ticketPaymentService, seatReservationService);
    }

    @Nested
    @DisplayName("Valid Purchase Scenarios")
    class ValidPurchaseTests {

        @Test
        @DisplayName("Should process a single adult ticket purchase")
        void shouldProcessSingleAdultTicket() {
            final long accountId = 1L;
            final TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);

            ticketService.purchaseTickets(accountId, adultRequest);

            verify(seatReservationService).reserveSeat(accountId, 1);
            verify(ticketPaymentService).makePayment(accountId, 25);
        }

        @Test
        @DisplayName("Should process multiple adult tickets")
        void shouldProcessMultipleAdultTickets() {
            final long accountId = 2L;
            final TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 5);

            ticketService.purchaseTickets(accountId, adultRequest);

            verify(seatReservationService).reserveSeat(accountId, 5);
            verify(ticketPaymentService).makePayment(accountId, 125);
        }

        @Test
        @DisplayName("Should process adult and child tickets")
        void shouldProcessAdultAndChildTickets() {
            final long accountId = 3L;
            final TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
            final TicketTypeRequest childRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 3);

            ticketService.purchaseTickets(accountId, adultRequest, childRequest);

            verify(seatReservationService).reserveSeat(accountId, 5);
            verify(ticketPaymentService).makePayment(accountId, 95);
        }

        @Test
        @DisplayName("Should process adult, child, and infant tickets")
        void shouldProcessAllTicketTypes() {
            final long accountId = 4L;
            final TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
            final TicketTypeRequest childRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
            final TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2);

            ticketService.purchaseTickets(accountId, adultRequest, childRequest, infantRequest);

            verify(seatReservationService).reserveSeat(accountId, 3);
            verify(ticketPaymentService).makePayment(accountId, 65);
        }

        @Test
        @DisplayName("Should handle the maximum number of allowed tickets")
        void shouldHandleMaximumTickets() {
            final long accountId = 5L;
            final TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 25);

            ticketService.purchaseTickets(accountId, adultRequest);

            verify(seatReservationService).reserveSeat(accountId, 25);
            verify(ticketPaymentService).makePayment(accountId, 625);
        }

        @Test
        @DisplayName("Should aggregate multiple requests of the same type")
        void shouldAggregateMultipleRequestsOfSameType() {
            final long accountId = 6L;
            final TicketTypeRequest adultRequest1 = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
            final TicketTypeRequest adultRequest2 = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 3);
            final TicketTypeRequest childRequest1 = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
            final TicketTypeRequest childRequest2 = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2);

            ticketService.purchaseTickets(accountId, adultRequest1, adultRequest2, childRequest1, childRequest2);

            verify(seatReservationService).reserveSeat(accountId, 8);
            verify(ticketPaymentService).makePayment(accountId, 170);
        }

        @Test
        @DisplayName("Should allow an equal number of infants and adults")
        void shouldAllowEqualInfantsAndAdults() {
            final long accountId = 7L;
            final TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 3);
            final TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 3);

            ticketService.purchaseTickets(accountId, adultRequest, infantRequest);

            verify(seatReservationService).reserveSeat(accountId, 3);
            verify(ticketPaymentService).makePayment(accountId, 75);
        }
    }

    @Nested
    @DisplayName("Invalid Purchase Scenarios")
    class InvalidPurchaseTests {

        @Test
        @DisplayName("Should reject a null account ID")
        void shouldRejectNullAccountId() {
            final TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);

            assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(null, adultRequest));

            verifyNoInteractions(ticketPaymentService, seatReservationService);
        }

        @Test
        @DisplayName("Should reject a zero account ID")
        void shouldRejectZeroAccountId() {
            final TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);

            assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(0L, adultRequest));

            verifyNoInteractions(ticketPaymentService, seatReservationService);
        }

        @Test
        @DisplayName("Should reject a negative account ID")
        void shouldRejectNegativeAccountId() {
            final TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);

            assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(-1L, adultRequest));

            verifyNoInteractions(ticketPaymentService, seatReservationService);
        }

        @Test
        @DisplayName("Should reject null ticket requests")
        void shouldRejectNullTicketRequests() {
            assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L, (TicketTypeRequest[]) null));

            verifyNoInteractions(ticketPaymentService, seatReservationService);
        }

        @Test
        @DisplayName("Should reject empty ticket requests")
        void shouldRejectEmptyTicketRequests() {
            assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L));

            verifyNoInteractions(ticketPaymentService, seatReservationService);
        }

        @Test
        @DisplayName("Should reject a null ticket request within the array")
        void shouldRejectNullTicketRequestInArray() {
            final TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);

            assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L, adultRequest, null));

            verifyNoInteractions(ticketPaymentService, seatReservationService);
        }

        @Test
        @DisplayName("Should reject a negative ticket count")
        void shouldRejectNegativeTicketCount() {
            final TicketTypeRequest negativeRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, -1);

            assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L, negativeRequest));

            verifyNoInteractions(ticketPaymentService, seatReservationService);
        }

        @Test
        @DisplayName("Should reject a purchase of zero tickets")
        void shouldRejectZeroTickets() {
            final TicketTypeRequest zeroRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 0);

            assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L, zeroRequest));

            verifyNoInteractions(ticketPaymentService, seatReservationService);
        }

        @Test
        @DisplayName("Should reject more than 25 tickets")
        void shouldRejectMoreThanMaximumTickets() {
            final TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 26);

            assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L, adultRequest));

            verifyNoInteractions(ticketPaymentService, seatReservationService);
        }

        @Test
        @DisplayName("Should reject combined tickets exceeding the maximum")
        void shouldRejectCombinedTicketsExceedingMaximum() {
            final TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 20);
            final TicketTypeRequest childRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 6);

            assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L, adultRequest, childRequest));

            verifyNoInteractions(ticketPaymentService, seatReservationService);
        }

        @Test
        @DisplayName("Should reject child tickets without an adult")
        void shouldRejectChildTicketsWithoutAdult() {
            final TicketTypeRequest childRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2);

            assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L, childRequest));

            verifyNoInteractions(ticketPaymentService, seatReservationService);
        }

        @Test
        @DisplayName("Should reject infant tickets without an adult")
        void shouldRejectInfantTicketsWithoutAdult() {
            final TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);

            assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L, infantRequest));

            verifyNoInteractions(ticketPaymentService, seatReservationService);
        }

        @Test
        @DisplayName("Should reject child and infant tickets without an adult")
        void shouldRejectChildAndInfantTicketsWithoutAdult() {
            final TicketTypeRequest childRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2);
            final TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);

            assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L, childRequest, infantRequest));

            verifyNoInteractions(ticketPaymentService, seatReservationService);
        }

        @Test
        @DisplayName("Should reject more infants than adults")
        void shouldRejectMoreInfantsThanAdults() {
            final TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
            final TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 3);

            assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L, adultRequest, infantRequest));

            verifyNoInteractions(ticketPaymentService, seatReservationService);
        }
    }

    @Nested
    @DisplayName("Service Integration Tests")
    class ServiceIntegrationTests {

        @Test
        @DisplayName("Should call services in the correct order")
        void shouldCallServicesInCorrectOrder() {
            final long accountId = 10L;
            final TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);

            ticketService.purchaseTickets(accountId, adultRequest);

            final InOrder inOrder = inOrder(seatReservationService, ticketPaymentService);
            inOrder.verify(seatReservationService).reserveSeat(accountId, 2);
            inOrder.verify(ticketPaymentService).makePayment(accountId, 50);
        }

        @Test
        @DisplayName("Should calculate the correct payment for mixed ticket types")
        void shouldCalculateCorrectPaymentForMixedTypes() {
            final long accountId = 11L;
            final TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 3);
            final TicketTypeRequest childRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2);
            final TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);

            ticketService.purchaseTickets(accountId, adultRequest, childRequest, infantRequest);

            verify(ticketPaymentService).makePayment(accountId, 105);
        }

        @Test
        @DisplayName("Should not reserve seats for infants")
        void shouldNotReserveSeatsForInfants() {
            final long accountId = 12L;
            final TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
            final TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2);

            ticketService.purchaseTickets(accountId, adultRequest, infantRequest);

            verify(seatReservationService).reserveSeat(accountId, 2);
        }
    }
}

