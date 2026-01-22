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
**order**

├── **domain**

│ ├── Order

│ ├── Payment

│ ├── OrderStatus

│ └── PaymentStatus

├── **service**

│ ├── PaymentService

├── **controller**

├── **repository**

├── **dto**

├── **exception**

├── **config**

└── **docs**


---

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

CREATED → PAID → CANCELLED


Invalid transitions are blocked using custom domain exceptions, ensuring business integrity at the entity level.

---

## REST API Design
The APIs are designed with clear contracts and predictable behavior.

### Key Endpoints
- `POST /orders` – Create a new order
- `POST /payments` – Process payment for an order (idempotent)
- `GET /orders/{id}` – Fetch order details with payment status

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
