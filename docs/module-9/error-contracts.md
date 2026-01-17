# PROJECT 1 — Order & Payment Processing Service (FLAGSHIP)

# Module 9: Error handling & API error contracts

## Error contract


### Why Error Contract matters for Payments?

Because payments have
* Validation errors
* Business rules errors
* Server/ Infra errors
* Retries
* Lifecycle change
* Idempotency responses
* Consistent windows
* Domain invariants

**Note:** Mapping must expose meaning without leaking internal details.

**Example:**

**Scenario - 1:**

409 INVALID_ORDER_STATE

**Recommendation:** Retry with correct payload

**Scenario - 2:**

500 INTERNAL_SERVER_ERROR

**Recommendation:** Retry with same payload


### V1 Error Contract Format

Services should return:

{
    
    "success": false,
    "status": 409,
    "errorCode": "INVALID_ORDER_STATE",
    "reason": "Payment cannot be processed for Orders in PAID state",
    "errors": null,
    "retryable": false,
    "timestamp": "2026-01-15T15:23:51.314Z",
    "traceId": "c83d7ef2-9981-473b-bb43-9f2ff95fd532"
}


### Special case: Validation Errors

Validation can produce multiple field errors


{

    "success": false,
    "status": 400,
    "errorCode": "FIELD_VALIDATION_FAILED",
    "reason": null,
    "errors": {
        "amount": "must be > 0",
        "currency": "must be 3-letter ISO code"
    },
    "retryable": true,
    "timestamp": "2026-01-15T16:18:35.243Z",
    "traceId": "c83d7ef2-9981-473b-bb43-9f2ff95fd532"
}


## Error categories

### 1. Validation errors (Client correctable)

**Example:** Malformed JSON, missing input field(s), wrong input format

**Expected Http:** 400

**Front-end action:** Retry with correct and acceptable payload

### 2. Business errors (Domain conflict)

**Example:** Invalid state transition

**Expected Http:** 409

**Front-end action:** Do not retry, correct workflow

### 3. Not found errors (Missing resource)

**Example:** order not found, customer not foundExpected Http: 404

**Front-end action:** Invalid path, retry with correct resource identity

### 4. Server/ Internal error (Infra/ DB/ Crash)

**Example:** DB pool exhaustion, the **mid-commit** server crash, DB deadlocks

**Expected Http:** 500

**Example:** Gateway timeout

**Expected Http:** 504

**Front-end action:** Safe to retry with the same payload + idempotency


## V1 Error Registry

### Category A: Validation errors (400)

**Nature:** Input-correctable failures

| errorCode               | meaning                |
|:------------------------|:-----------------------|
| BAD_JSON                | malformed JSON         |
| INVALID_AMOUNT          | invalid amount format  |
| INVALID_CURRENCY_FORMAT | currency must be ISO-3 |
| FIELD_VALIDATION_FAILED | bean validation failed |
| ID_REQUIRED             | identifier missing     |

### Category B: Business errors (409)

**Nature:** Workflow failures

| errorCode                | meaning                                  |
|:-------------------------|:-----------------------------------------|
| INVALID_ORDER_STATE      | cannot pay non-CREATED orders            |
| ORDER_AMOUNT_MISMATCH    | payload vs order mismatch                |
| CURRENCY_MISMATCH        | payload vs order mismatch                |
| PAYMENT_ALREADY_EXISTS   | idempotent retry scenario                |
| DUPLICATE_PAYMENT        | double charging attempt (non-idempotent) |

### Category C: Resource errors (404)

**Nature:** Missing resources failures

| errorCode          | meaning             |
|:-------------------|:--------------------|
| ORDER_NOT_FOUND    | order id missing    |
| CUSTOMER_NOT_FOUND | customer id missing |


### Category D: Server/ Internal errors (500, 504)

**Nature:** Retry-safe failures

| errorCode               | status | meaning (User Facing)                                                       | Internal Log (Private)                                                  |
|:------------------------|:-------|:----------------------------------------------------------------------------|:------------------------------------------------------------------------| 
| DB_ERROR                | 500    | "An unexpected error occurred."                                             | Connection timeout to RDS                                               | 
| DEADLOCK                | 500    | "An unexpected error occurred."                                             | Deadlock on Order table                                                 |
| INTERNAL_SERVER_ERROR   | 500    | "An unexpected error occurred. Our team has been notified."                 | java.lang.NullPointerException at PaymentService.java:142               |
| SERVICE_UNAVAILABLE     | 500    | "We are currently experiencing high traffic. Please try again in a moment." | HikariPool-1 connection timeout: DB pool exhausted (100/100 active).    |
| GATEWAY_TIMEOUT         | 504    | "System is taking too long to respond. Please try again."                   | GatewayTimeout: Downstream Stripe API failed to respond within 5000ms.  |


## Mapping strategies

### 1. Domain Exceptions (from Service layer)

Handled as:
* 404 -> Missing resource
* 409 -> Business conflict
* 400 -> Validation
* 500/ 504 -> System

### 2. Validation Exceptions (Controller/ Bean validation)

Handled as:

400 -> Validation + Field errors

### 3. Server/ Internal Exceptions (Unexpected)

Handled as:

500/ 504 -> Retry advice


## Domain-To-Http Mapping Table

| Domain Exception                 | status | errorCode                            |
|:---------------------------------|:-------|:-------------------------------------|
| OrderNotFoundException           | 404    | ORDER_NOT_FOUND                      |
| InvalidOrderStateException       | 409    | INVALID_ORDER_STATE                  |
| OrderAmountMismatchException     | 409    | ORDER_AMOUNT_MISMATCH                |
| PaymentCurrencyMismatchException | 409    | ORDER_CURRENCY_MISMATCH              |
| BusinessRulesViolationException  | 409    | BUSINESS_RULE_VIOLATION              |
| DuplicatePaymentException        | 409    | DUPLICATE_PAYMENT                    |
| PaymentAlreadyExistsException    | 200    | IDEMPOTENT_REPLAY (Idempotent retry) |


**Note**: PaymentAlreadyExistsException (200) - idempotent retry is success not conflict

**Difference:** 

* 409 Conflict - payment for an order that is already paid with different idempotent key.
* 200 ok - payment for an order that is already paid with same idempotent key.



## Validation-To-Http Mapping Table

| Validation Failure     | status | errorCode               |
|:-----------------------|:-------|:------------------------|
| @Valid Bean validation | 400    | FIELD_VALIDATION_FAILED |
| JSON parse failure     | 400    | BAD_JSON                |
| amount format          | 400    | INVALID_AMOUNT_FORMAT   |
| currency format        | 400    | INVALID_CURRENCY_FORMAT |


## Infra-To-Http Mapping Table

| Infra Failure                | status | errorCode             |
|:-----------------------------|:-------|:----------------------|
| Database connection          | 500    | SERVICE_UNAVAILABLE   |
| Database pool exhaustion     | 500    | SERVICE_UNAVAILABLE   |
| Database deadlocks           | 500    | INTERNAL_SERVER_ERROR |
| Server crash                 | 500    | INTERNAL_SERVER_ERROR |
| Unknown                      | 500    | INTERNAL_SERVER_ERROR |
| Network time-outs            | 504    | GATEWAY_TIMEOUT       |



## Retry Advice Block

### Generic:

| status   | client action                 |
|:---------|:------------------------------|
| 400      | Never retry same payload      |
| 404      | Retry with corrected identity |
| 409      | Abort & Correct workflow      |
| 500/ 504 | Safe retry with same payload  |



### Business conflict types:

| Conflict type                                                  | Client action                 | Retry |
|:---------------------------------------------------------------|:------------------------------|:------|
| Lifecycle                                                      | abort                         | no    |
| Value                                                          | correct amount                | no    |
| Currency                                                       | correct currency              | no    | 
| Duplicate                                                      | abort & investigate           | no    |
| Payload collision                                              | same payload + Different Key  | no    |
| Idempotent retry                                               | Same payload + same key       | yes   |




