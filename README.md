# Cinema Ticket Service
## Approach

I used TDD to build this solution. Started with the simplest test case, made it pass, then added more complex scenarios. Each test was written before the implementation code. This meant every piece of functionality was driven by an actual requirement. I've tried showcasing this through multiple commits.

## Running the Tests

Requires Java 21 and Maven.

```bash
mvn clean test
```

Build the project:

```bash
mvn clean install
```

24 tests should pass.

## Design Decisions

### Constructor Injection
The service takes its dependencies through the constructor rather than creating them internally. Makes testing straightforward since I can pass in mocks as well as following dependency injection.

### Input Validation
All validation happens upfront before any calculations or external calls. If something's wrong with the request, we fail immediately rather than doing partial work.

### Request Aggregation  
Multiple ticket requests of the same type get combined into a single map entry. This handles cases where someone might request adults in separate batches but still need to respect the total limit.

### Method Organization
Kept the public interface minimal (just the one required method) with private methods handling specific tasks. Each method does one thing validate account, calculate price, count seats, etc.

## Business Rules

### Ticket Prices
- Adult: £25
- Child: £15  
- Infant: £0

### Purchase Rules
- Maximum 25 tickets per purchase
- Must include at least one adult ticket
- Children and infants need an accompanying adult
- Infants sit on adult laps (can't have more infants than adults)

### Seat Allocation
- Adults get seats
- Children get seats
- Infants don't get seats

### Invalid Requests
The service throws `InvalidPurchaseException` for:
- Invalid account ID (null, zero, or negative)
- No tickets requested
- Only child/infant tickets without adults
- More than 25 total tickets
- More infants than adults
- Negative ticket quantities

## Test Structure

Tests are split into three groups using JUnit 5's nested classes:
- Valid purchases
- Invalid purchases  
- Service integration 

Used Mockito to mock the external payment and seat reservation services. Tests verify both the business logic and that external services are called with the correct parameters in the right order.

## Implementation Notes

The service reserves seats first, then processes payment. This order made sense to me since you'd want to ensure seats are available before taking payment.

The aggregation logic uses Java's `Map.merge()` to combine duplicate ticket type requests cleanly.

All the validation happens in sequence -> account validation, then request validation, then business rules. This keeps the flow logical and makes it easier to debug when something fails.

## Assumptions

Following the requirements document:
- Account IDs greater than 0 are valid
- External services always succeed (no need to handle payment failures or reservation errors)
- The interfaces in the `thirdparty` package couldn't be modified
- `TicketTypeRequest` remains immutable
