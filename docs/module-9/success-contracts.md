# PROJECT 1 — Order & Payment Processing Service (FLAGSHIP)

## Module 9: Error handling & API error contracts

## Success contract


The success JSON contract must be
* Idempotent
* Deterministic
* Reconcilable
* Versionable
* Auditable
* retry-safe

### Case 1: New payment

{

    "success": true,
    "status": 201,
    "idempotent": true,
    "orderState": "PAID",
    "payment": {
        "paymentId": 453,
        "orderId": 78,
        "amount": 1200,
        "paymentState": "COMPLETED",
        "createdAt": "2026-01-16T19:24:21.744Z"
    },
    "reconciliation": {
        "requestId": "req_9a2c8b",
        "idempotencyKey": "idem_5567",
        "committedAt": "2026-01-16T19:24:22.0Z"
    },
    "traceId": "trace_f7a912"

}

### Case 2: Idempotent replay

Exact same response body for same idempotent key but with status as 200 ok.

{

    "success": true,
    "status": 200,
    …
    …
    "traceId": "trace_f7a912"
}


## Idempotent flag

### Scenario - 1: Retry safe

#### Request:
Same payload + same key

#### Response:

* `200 ok`
* Same response (Idempotent)

**Invariants:**
* No duplicate charging
* No lifecycle change

### Scenario - 2: Non-retriable

#### Request:
Same payload + different key

**Response:**

* `409 conflict`
* Payment cannot be allowed for already paid orders


## Reconciliation fields

For auditing & logging some fields are used for cross - checking the records in both success and error response, which is mandatory for production grade.

### Successful payment response:

{

    "success": true,
    "status": 201,
    ....
    ....
    "reconciliation": {
        "requestId": "req_9a2c8b",
        "idempotencyKey": "idem_5567",
        "committedAt": "2026-01-16T19:24:22.0Z"
    },
    "traceId": "trace_f7a912"

}

The reconciliation fields are
* idempotency key
* requestId
* committedAt


## Lifecycle fields

For a successful payment request, both the order and payment status needs to be changed.

### Order & Payment status (Happy path)

**Pre-condition**

orderState = `CREATED`

**Request (JSON)**

{

    "amount": 1500,
    "currency": "USD"

}

**Expected outcome**

* `201 created`
* orderState = `PAID`
* paymentState = `COMPLETED`

#### Lifecycle success invariant

paymentState = `COMPLETED` implies orderState = `PAID`

### Wrong Lifecycle

**Pre-condition**

orderState = `PAID`

**Request (JSON)**

{

    "amount": 1500,
    "currency": "USD"

}

**Expected outcome**

* `409 conflict`
* Payment cannot be allowed for already paid orders

**Client action:**
* No retry
* Make payment for orders that are in `CREATED` state.

**Note:** `PAID` is a terminal state for payments not for shipping fulfillment.

### Request headers

**1. Idempotency-Key**

Unique key string for each request, used for auditing & safe retry.

**Example:** Idempotency-Key: `idem_aaa`

**2. Authorization**

Unique token for each request, used for traceability, auditing & reconciliation.

**Example:** Authorization: Bearer `<token>`

**3. Trace-Id**

Unique id for each user, used for debugging, logging, auditing.

**Example:** Trace-Id: `trace_999`

### Response headers

**1. Idempotency-Key**

Echo back the key used.

**Example:** Idempotency-Key: `idem_aaa`

**2. Location**

The Location URL to the newly created payment resource.

**Example:** /payments/453

