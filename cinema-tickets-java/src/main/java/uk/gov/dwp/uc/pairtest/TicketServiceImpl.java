package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import java.util.HashMap;
import java.util.Map;

public class TicketServiceImpl implements TicketService {

    private static final int MAX_TICKETS_PER_PURCHASE = 25;
    private static final int ADULT_TICKET_PRICE = 25;
    private static final int CHILD_TICKET_PRICE = 15;
    private static final int INFANT_TICKET_PRICE = 0;

    private final TicketPaymentService ticketPaymentService;
    private final SeatReservationService seatReservationService;

    // This is dependency injection it makes tests far cleaner
    public TicketServiceImpl(TicketPaymentService ticketPaymentService,
                             SeatReservationService seatReservationService) {
        this.ticketPaymentService = ticketPaymentService;
        this.seatReservationService = seatReservationService;
    }

    @Override
    public void purchaseTickets(final Long accountId, final TicketTypeRequest... ticketTypeRequests)
            throws InvalidPurchaseException {

        validateAccountId(accountId);
        validateTicketRequests(ticketTypeRequests);

        final Map<TicketTypeRequest.Type, Integer> ticketCounts = aggregateTicketCounts(ticketTypeRequests);

        validateBusinessRules(ticketCounts);

        final int totalAmount = calculateTotalAmount(ticketCounts);
        final int totalSeats = calculateTotalSeats(ticketCounts);

        seatReservationService.reserveSeat(accountId, totalSeats);
        ticketPaymentService.makePayment(accountId, totalAmount);
    }

    private void validateAccountId(final Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw new InvalidPurchaseException();
        }
    }

    private void validateTicketRequests(final TicketTypeRequest... ticketTypeRequests) {
        if (ticketTypeRequests == null || ticketTypeRequests.length == 0) {
            throw new InvalidPurchaseException();
        }

        for (final TicketTypeRequest request : ticketTypeRequests) {
            if (request == null) {
                throw new InvalidPurchaseException();
            }
            if (request.getNoOfTickets() < 0) {
                throw new InvalidPurchaseException();
            }
        }
    }

    private Map<TicketTypeRequest.Type, Integer> aggregateTicketCounts(
            final TicketTypeRequest... ticketTypeRequests) {
        final Map<TicketTypeRequest.Type, Integer> ticketCounts = new HashMap<>();

        for (final TicketTypeRequest request : ticketTypeRequests) {
            ticketCounts.merge(request.getTicketType(),
                    request.getNoOfTickets(),
                    Integer::sum);
        }

        return ticketCounts;
    }

    private void validateBusinessRules(final Map<TicketTypeRequest.Type, Integer> ticketCounts) {
        final int totalTickets = ticketCounts.values().stream()
                .mapToInt(Integer::intValue)
                .sum();

        if (totalTickets == 0) {
            throw new InvalidPurchaseException();
        }

        if (totalTickets > MAX_TICKETS_PER_PURCHASE) {
            throw new InvalidPurchaseException();
        }

        final int adultTickets = ticketCounts.getOrDefault(TicketTypeRequest.Type.ADULT, 0);
        final int childTickets = ticketCounts.getOrDefault(TicketTypeRequest.Type.CHILD, 0);
        final int infantTickets = ticketCounts.getOrDefault(TicketTypeRequest.Type.INFANT, 0);

        if ((childTickets > 0 || infantTickets > 0) && adultTickets == 0) {
            throw new InvalidPurchaseException();
        }

        if (infantTickets > adultTickets) {
            throw new InvalidPurchaseException();
        }
    }

    private int calculateTotalAmount(final Map<TicketTypeRequest.Type, Integer> ticketCounts) {
        int totalAmount = 0;
        totalAmount += ticketCounts.getOrDefault(TicketTypeRequest.Type.ADULT, 0) * ADULT_TICKET_PRICE;
        totalAmount += ticketCounts.getOrDefault(TicketTypeRequest.Type.CHILD, 0) * CHILD_TICKET_PRICE;
        // Needed as infant amounts might change in the future
        totalAmount += ticketCounts.getOrDefault(TicketTypeRequest.Type.INFANT, 0) * INFANT_TICKET_PRICE;
        return totalAmount;
    }

    private int calculateTotalSeats(final Map<TicketTypeRequest.Type, Integer> ticketCounts) {
        int totalSeats = 0;
        totalSeats += ticketCounts.getOrDefault(TicketTypeRequest.Type.ADULT, 0);
        totalSeats += ticketCounts.getOrDefault(TicketTypeRequest.Type.CHILD, 0);
        return totalSeats;
    }
}

