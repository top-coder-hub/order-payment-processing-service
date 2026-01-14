# PROJECT 1 — Order & Payment Processing Service (FLAGSHIP)

## Module 8: Transaction management & consistency

## Payment consistency

### 1. Delivery model: at-least-once 
#### Scenario:

Retry feature supports **at-least-once** due to 
* network time-outs
* server crash
* duplicate request


#### Expected outcome:

* 200 OK
* Guarantees Idempotent response

#### Consistency Guarantee:

At-least-once delivery + Idempotency = Effectively Exactly-Once Execution

### 2. Lifecycle state & Idempotency
#### Scenario - 1:

**Pre-condition:**
orders.status = `CREATED`

**Request:**
Valid payment

#### Expected outcome:

**1st call:** 
* 201 Created
* Order status -> `PAID`
* New Payment inserted

**2nd call (retry):**
    
* 200 OK
* Idempotent response
#### Scenario - 2:
**Pre-condition:**
orders.status = `PAID` / `CANCELLED` / `SHIPPED`

**Request:**
Valid payment

#### Expected outcome:

* 409 conflict
* INVALID_ORDER_STATE

**Note:** Lifecycle state acts as a **reconciliation anchor** for determining committed vs non-committed operations.


### 3. Failure window handling
**Scenario - 1: Input validation failure**

**Pre-condition:**
orders.status = `CREATED`

**Request:**
`POST` `/orders/5/payments`

**Headers (mandatory):** `Idempotency-Key: <UUID>`

**Request body (json):**

{

    "amount": "one fifty rupees",
  
    "currency": "USD"
}

**Response:**
* 400 Bad request
* ERROR_BAD_JSON
* Malformed JSON format

**Professional handle:**
Retry with valid input format

**Scenario - 2: Invalid order**

**Request:**
`POST` `/orders/999/payments`

**Headers (mandatory):** `Idempotency-Key: <UUID>`

**Request body (json):**

{

    "amount": 150.00,
  
    "currency": "USD"
}

#### Expected outcome:

* 404 Not found
* ORDER_NOT_FOUND

**Professional handle:**
Retry with valid order id

**Scenario - 3: Business logic failure**

**Pre-condition:**
orders.status = `PAID`

**Request:**
Valid payment

#### Expected outcome:

* 409 conflict
* INVALID_ORDER_STATE

**Professional handle:**
Attempt payment only for orders with `CREATED` state

**Scenario - 4: DB constraint failure**

**Pre-condition:**
orders.status = `CREATED`

**Request:**
`POST` `/orders/99/payments`

**Headers (mandatory):** `Idempotency-Key: <UUID>`

**Request body (json):**

{

    "amount": 1110.00,
  
    "currency": "US-DOLLAR"
}

#### Expected outcome:

* 400 Bad request
* INVALID_CURRENCY_FORMAT
* Cause: Domain validation or DB constraint violation

**Professional handle:**
Retry with valid currency format

**Scenario - 5: Server failure**

**Pre-condition:**
orders.status = `CREATED`

**Request:**
Valid payment

#### Expected outcome:

* 500 Server error
* INTERNAL_SERVER_ERROR

**Professional handle:**
Retry with same payload

**Scenario - 6: Crash after commit, before response**

**Pre-condition:**
orders.status = `CREATED`

**Request:**
Valid payment

**Action:**
DB commits payment, but server crashes before sending HTTP response.

#### Expected outcome:

* 200 OK
* Idempotent response
* No duplicate charging
* No order lifecycle change
  
**Invariant:**
Lifecycle state is source of truth for reconciliation


### 4. Payment Invariants:

* Atomicity
* No partial order state transitions
* No dangling payment

**Atomicity Invariant** - Payment + lifecycle transition are committed as a single atomic unit.
### 5. Commit behavior

* **Transaction isolation:** Repeatable Read (MySQL default)
* Idempotency resolves retry uncertainty
* Lifecycle transitions ensure domain correctness
* Commit guarantees durability, not response delivery
* Crash-safe retry returns consistent result





