# Order & Payment Processing Service

## Overview
Order & Payment Processing Service is a production-grade backend system designed to handle order creation and payment processing with strong guarantees around consistency, idempotency, and failure handling.

This project simulates real-world payment system challenges such as retries, partial failures, concurrent requests, and strict order lifecycle rules.

---

## Business Problem
In real payment-based systems:

- Orders and payments must remain consistent even during failures
- Payment APIs must be idempotent to prevent double charges
- Order state transitions must follow strict business rules
- API consumers must receive clear and predictable error contracts

This service is built to address these challenges using clean domain modeling, transactional boundaries, and defensive design.

---

## High-Level Architecture
The application follows a layered architecture with clear separation of concerns:

- **Domain Layer** – Core business entities and rules
- **Service Layer** – Business logic, validations, and transactions
- **API Layer** – REST endpoints and request/response contracts
- **Persistence Layer** – Relational schema with constraints and indexes
- **Documentation Layer** – Explicit API contracts, error models, and consistency rules

### Project Structure
**docs**

├── **module-9**

│    ├──—controller-boundary.md

│    ├──—error-contracts.md

│    ├──—success-contracts.md

├── **module-10**

│    ├──—authorization-ownership.md

│    ├──—controller-design.md

│    ├──—security-model.md

│    ├──—token-failure-semantics.md

├── api-contract.md

└── payment-test-scenarios.md

└──payments-consistency-v1.md

**order**

├── **domain**

│   ├── Order

│ ├── Payment

│ ├── OrderState

│ └── PaymentState

├── **service**

│   ├── PaymentService

|	├──PaymentResult

├── **controller**

|  ├──PaymentController

|	├── OrderController

├── **repository**

|	├──OrderRepository

|	├──PaymentRepository

├── **dto**

|	├──CreateOrderRequest

|	├──OrderResponse

|	├──PaymentRequest

|	├──PaymentResponse

├── **exception**

|	├──AccessDeniedException

|	├──BusinessRulesViolationException

|	├──GlobalExceptionHandler

|	├──InvalidOrderStateException

|	├──OrderAmountMismatchException

|	├──OrderNotFoundException

|	├──PaymentCurrencyMismatchException

|	├──UnauthorizedException

|	├──PaymentNotFoundException

├── **config**

│    ├── FilterConfig.java                (RequestId)

│    └── AuthenticationFilterConfig.java

├── **security**

│    ├── RequestContext.java

│    ├── AuthenticatedUser.java

│    └── UserRole.java



---

## Technical Documentation Architecture

This project is built using **Documentation-Driven Development** (DDD). Each architectural pillar is supported by detailed specifications:

### API & Contracts

[Module 02: REST API Design & Contracts](docs/api-contract.md) – Detailed endpoint specifications and request/response models.

[Module 09: Success JSON Contracts](docs/module-9/success-contracts.md) – Success response structure and reconciliation fields.

[Module 09: API Error Contracts](docs/module-9/error-contracts.md) – Error semantics, status codes, and global error handling.

### Business Logic & Resiliency

[Module 07: Payment Test Scenarios](docs/payment-test-scenarios.md) – Functional, idempotency, and concurrency test cases.

[Module 08: Transaction & Consistency](docs/payments-consistency-v1.md) – Atomicity invariants and failure window handling.

[Module 10: Controller Design Boundary](docs/module-10/controller-design.md) – Input validation and HTTP layer responsibilities.

### Security & Identity

[Module 09: Controller Boundary Rules](docs/module-9/controller-boundary.md) – Validation, trace propagation, and security enforcement.

[Module 10: Security & Ownership Model](docs/module-10/security-model.md) – FSM integrity, ownership isolation, and auditability.

[Module 10: Authorization & Ownership](docs/module-10/authorization-ownership.md) – Role matrix and the "Cloaked 404" strategy.

[Module 10: Token Failure Semantics](docs/module-10/token-failure-semantics.md) – Technical mapping for 401, 403, and 404 status codes.

-------

## Core Domain Modeling

### Order
- Represents a customer purchase
- Maintains total amount and lifecycle state
- Enforces valid state transitions

### Payment
- Represents a payment attempt for an order
- Designed to support safe retries
- Linked to orders using foreign key constraints

### Order Lifecycle
Order transitions are strictly controlled:

* `CREATED` ➔ `PAID` (via markAsPaid)
* `PAID` ➔ `SHIPPED` (via markAsShipped)
* `CREATED` ➔ `CANCELLED` (via cancel)


**Note:** Invalid transitions are blocked using custom domain exceptions, ensuring business integrity at the entity level.

---

## REST API Design
The APIs are designed with clear contracts and predictable behavior.

### Key Endpoints
- `POST /orders` – Create a new order
- `POST /payments` – Process payment for an order (idempotent)
- `GET /orders/{id}` – Fetch order details with payment status
- `GET /payments/{paymentId}` – Fetch payment details with payment status

API request and response contracts are documented in:

/docs/api-contract.md

---

## DTO & Validation Layer
- Separate request and response DTOs for orders and payments
- Advanced bean validations applied at API boundaries
- Invalid requests are rejected early with meaningful error responses

This ensures:
- Clean separation between API and domain
- Strong input validation before business logic execution

---

## Database Design
The database schema is designed for consistency and performance.

### Tables
- `orders`
- `payments`

### Key Design Choices
- Foreign key constraints between orders and payments
- Indexes for high-frequency queries
- Explicit column sizing and constraints
- Schema aligned with domain invariants

### Dual-Layer Defense 
Combines Spring Data JPA validation with MySQL `CHECK` constraints to enforce valid 
OrderState transitions at both the application and persistence layers.

### Optimized Indexing 
Implements a composite index on (customer_id, order_state) to optimize 
high-frequency order history queries.

### Fintech-Grade Precision

All monetary values (Order total, Payment amount) are standardized to `DECIMAL(19, 4)` 
to eliminate rounding discrepancies across the distributed system.

---

## Payment Processing & Idempotency
Payment processing is implemented with idempotency as a first-class concern.

Key guarantees:
- Multiple requests with the same payment reference are handled safely
- Duplicate charges are prevented
- Payment retries do not corrupt order state

Testing scenarios are documented in:

/docs/payment-test-scenarios.md

These cover:
- Functional correctness
- Idempotency correctness
- Failure and concurrency behavior

---

## Transaction Management & Consistency
Transactional boundaries are carefully defined to ensure:

- Atomic updates between orders and payments
- Safe rollback during failures
- Database constraints as a second line of defense

Consistency strategy is documented in:

payments-consistency-v1.md


This includes:
- Failure scenarios
- Transactional guarantees
- Known edge cases and future improvements

---

## Error Handling & API Contracts
The system exposes clear and predictable API error contracts.

Documented under:

**/docs/module-09**

├── error-contracts.md

├── success-contracts.md

└── controller-boundary.md


Key principles:
- Centralized error handling
- Consistent response structure
- Clear separation between client errors and server errors

### Failure handling (V1):

In V1, database and infrastructure failures are returned as HTTP 500 (INTERNAL_SERVER_ERROR) with retryable=true.
HTTP 503 (SERVICE_UNAVAILABLE) is intentionally reserved for future infra-aware deployments (e.g., circuit breakers, maintenance windows, load shedding).

---

## Documentation-Driven Development
This project follows a documentation-first mindset.

Key design decisions, API contracts, testing scenarios, and consistency rules are explicitly documented to mirror real-world backend engineering practices.

---

## Tech Stack
- Java
- Spring Boot
- Spring Data JPA
- MySQL
- Maven

---

## Why This Project Matters
This project demonstrates real backend engineering depth, including:

- Domain-driven modeling
- Order lifecycle enforcement
- Idempotent payment processing
- Transaction management under failure
- Production-style API contracts and error handling

It is intentionally designed to reflect payment system complexity found in real production environments.

---

## Project Status
- Core order & payment workflows implemented
- Consistency, idempotency, and error handling completed
- Production readiness modules in progress (logging, tracing, Docker)
