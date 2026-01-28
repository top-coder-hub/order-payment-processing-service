# PROJECT 1 — Order & Payment Processing Service (FLAGSHIP)

## Module 10 — Controller Design (V1)
## Controller Design (V1)

This document defines the **HTTP controller boundary** for the
Order & Payment Processing Service.

The controller is a **thin layer** that translates HTTP requests
into domain service calls.

---

## 1. Responsibilities

The controller must:

- Accept HTTP requests
- Validate request structure and formats
- Enforce authentication presence
- Extract and validate idempotency key
- Ensure trace propagation
- Delegate to service layer
- Map domain outcomes → HTTP responses

The controller **must not**:
- Contain business logic
- Access the database
- Enforce ownership or lifecycle rules

---

## 2. Endpoint
`POST` `/orders/{orderId}/payments`

**Purpose**: Process payment for an order.

---

## 3. Inputs

### Path
- `orderId` (required)

### Headers (mandatory)

| Header | Rule |
|------|-----|
| Authorization | Bearer token |
| Idempotency-Key | **Required, UUID format** |
| Trace-Id | Required for observability (generated if missing) |

**Validation Rules**
- Invalid Idempotency-Key format → **400 BAD REQUEST**
- Missing Authorization → **401 UNAUTHENTICATED**

---

### Body

**Request (json):**
    
    {
        "amount": 150.00,
        "currency": "USD"
    }


**Amount Handling Rule:**

* Amount **must be processed using high-precision types** (e.g. BigDecimal).
* Floating-point types (`float`, `double`) are **strictly forbidden**.

**Reason**: Prevents rounding errors and financial loss.

---------

## 4. Validation (Controller-Level)

Controller validates only **input correctness**, not business rules:

* amount: required, positive, high-precision
* currency: required, ISO-3
* JSON must be well-formed
* Idempotency-Key must be UUID

No DB or lifecycle validation here.

---------

## 5. Authentication

* Token validated at entry (filter / controller boundary)
* Missing / invalid token → **401 UNAUTHENTICATED**
* Extracted identity passed to service

------

## 6. Idempotency

* Controller validates and forwards Idempotency-Key unchanged
* Controller does not decide replay vs new execution
* Replay logic belongs to service layer

----

## 7. Trace Propagation

* Trace-Id must exist for every request
* If missing, controller **generates a new Trace-Id**
* Trace-Id is passed to service for logging and audit

----

## 8. Service Call

Controller passes:

* orderId
* userId (from token)
* payment payload
* idempotencyKey
* traceId
* requestId
Service executes atomically.

-------

## 9. HTTP Mapping

| Outcome               | Status         |
|:----------------------|:---------------|
| New payment           | 201            |
| Idempotent replay     | 200            |
| Ownership violation   | 404 (cloaked)  |
| Invalid lifecycle     | 409            |
| Validation error      | 400            |
| Infra failure         | 500 / 504      |


Controller owns HTTP semantics only.

------

## 10. V1 Guarantees

* Precise monetary handling
* Deterministic retries
* Secure request boundaries
* Strong observability
* Fintech-safe controller design