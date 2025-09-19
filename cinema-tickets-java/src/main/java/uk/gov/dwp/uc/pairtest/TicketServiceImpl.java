package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

public class TicketServiceImpl implements TicketService {

    private final TicketPaymentService ticketPaymentService;
    private final SeatReservationService seatReservationService;

    public TicketServiceImpl(TicketPaymentService ticketPaymentService,
                             SeatReservationService seatReservationService) {
        this.ticketPaymentService = ticketPaymentService;
        this.seatReservationService = seatReservationService;
    }

    @Override
    public void purchaseTickets(final Long accountId, final TicketTypeRequest... ticketTypeRequests)
            throws InvalidPurchaseException {

        int totalSeatsToAllocate = 0;
        int totalAmountToPay = 0;

        for (final TicketTypeRequest request : ticketTypeRequests) {
            if (request.getTicketType() == TicketTypeRequest.Type.ADULT) {
                totalSeatsToAllocate += request.getNoOfTickets();
                totalAmountToPay += request.getNoOfTickets() * 20;
            }
        }

        seatReservationService.reserveSeat(accountId, totalSeatsToAllocate);
        ticketPaymentService.makePayment(accountId, totalAmountToPay);
    }
}

