# PROJECT 1 — Order & Payment Processing Service (FLAGSHIP)

## Module 9: Error handling & API error contracts

## Controller boundary

## Controller Responsibilities 

Controller must do:

### 1. Accept input

`POST` `/orders/{orderId}/payments`

**Purpose -**
Process payment for an order.

**Headers (mandatory):** `Idempotency-Key: <UUID>`

**Request body (json):**

{

    "amount": 150.00,
  
    "currency": "USD"
}

Accept input fields "amount" &  "currency" from this `POST` request.

### 2. Validate input

**Payload:**

{

    "amount": 150.00,
  
    "currency": "USD"
}

With this payload controller validate the input fields using validation constraints.

**Example:**

* @NotNull
* @NotBlank
* @Valid

### 3. Enforce idempotency key

Controller must enforce the idempotency key from request header and proceed to service for idempotency retry.

**Headers (mandatory):** `Idempotency-Key: <UUID>`

### 4. Enforce security context
Controller must authenticate the user identity at entry level before delegating to service layer for authorizated db operations.


### 5. Trace propagation
Controller must attach `traceId` with requested data to service layer for logging/ reconciliation systems.


### 6. Call service atomically
* Controller sends the validated inputs with idempotency key to service layer to perform atomic DB operations.
* Service must perform operations inside `@Transactional` to become atomic in nature.

### 7. Performs HTTP contract shaping
* Service performs business logics & atomic DB operations and return the domain to controller.
* Controller sends the domain response from service layer as a HTTP response via `ResponseEntity` to the client.


## Service responsibilities
Service should receive input from controller and do:
### 1. Authorization & Ownership validation

Service must check whether the user is allowed to process payment.

**Reason:**
* Payment is allowed only for owner/ customer
* Prevents privilege escalation
* Prevents fraudulent payments

**Example:**

**Pre-condition:** customerId = 872

**Request:** userId = 976

**Response:** 
* 404 NOT FOUND (cloaked)
* userId must match order.customerId

### 2. Business validation rules
**Pre-condition:**

orderState = CREATED

**Payload:**

{

    "amount": 150.00,
  
    "currency": "USD"
}

**Validation:** checks if the order is in `CREATED` state to process payment

### 3. Apply business logic
**Payload:**

{

    "amount": 15000.00,
  
    "currency": "USD"
}

**Apply**: 

**Strict Amount & Currency Locking**. Check that `request.amount` equals 
`order.totalAmount`. This prevents partial payments and ensures the transaction fulfills the financial obligation exactly.

### 4. Execute atomic DB operations

`POST` `/orders/{orderId}/payments`

**Purpose -**
Process payment for an order.

**Headers (mandatory):** `Idempotency-Key: <UUID>`

**Action:** 

Persist in both order and payment table.
* order state changed from `CREATED` to `PAID`.
* payment state is marked as `COMPLETED`.

### 5. Enforce Domain FSM
Service enforces order transitions as
* `CREATED` -> `PAID` allowed
* `PAID` -> `PAID` forbidden
* `CANCELLED` -> `PAID` forbidden

### 6. Map entity -> domain
* Persisted resources return the entity object.
* Service maps entity to domain DTO with restriced view to prevent ORM leaks.

### 7. Return to controller
* Controller receives the domain DTO from service.
* Returns the response via ResponseEntity for more HTTP control.

## Exception boundaries
Exceptions belongs to controller layer are of four types:

### 1. Malformed JSON
**Request:**

`POST` `/orders/{orderId}/payments`

**Purpose -**
Process payment for an order.

**Headers (mandatory):** `Idempotency-Key: <UUID>`

**Request body (json):**

{

    "amount": 150.00,
  
    "currency": "USD"

**Expected outcome:**
* 400 Bad request
* Malformed JSON format

**Reason:** Missed closing parentheses 

**Retry Advice:** Retry with correct JSON format

### 2. Field/ Bean Validation Exception
**Request:**

`POST` `/orders/{orderId}/payments`

**Purpose -**
Process payment for an order.

**Headers (mandatory):** `Idempotency-Key: <UUID>`

**Request body (json):**

{

    "amount": ,
  
    "currency": "USD"
}

**Expected outcome:**
* 400 Bad request
* Field validation failed

**Reason:** Missing "amount" request field to validate

**Retry Advice:** Retry with all required request fields. 

### 3. Deserialization failure
**Request:**

`POST` `/orders/{orderId}/payments`

**Purpose -**
Process payment for an order.

**Headers (mandatory):** `Idempotency-Key: <UUID>`

**Request body (json):**

{

    "amount": "one fifty rupees",
  
    "currency": "USD"
}

**Expected outcome:**
* 400 Bad request
* Input mapping failure (Json -> Domain)

**Reason:** Cannot convert String to BigDecimal for "amount" field.

**Retry Advice:** Retry with correct input format.

### 4. Resource/ Identity Exception
**Request:**

`GET` `/orders/999`

**Purpose -**
Get order details.

**Response:**
* 404 Not found
* Order not found for the given orderId


**Retry Advice:** Retry with corrected orderId.

### 5. Server/ Infra Exception
**Request:**

`POST` `/orders/{orderId}/payments`

**Purpose -**
Process payment for an order.

**Headers (mandatory):** `Idempotency-Key: <UUID>`

**Request body (json):**

{

    "amount": 150.00,
  
    "currency": "USD"
}

**Response:** 500 Internal Server error

**Reason:** 
* Server crash
* Network gateway time-out
* Internal DB error

**Retry Advice:** Retry with same payload and same idempotency key.

### Error semantics categories:
| category       | status        | retry             | meaning              |
|:---------------|:--------------|:------------------|:---------------------|
| Authentication | 401           | no                | caller unknown       |
| Authorization  | 403           | no                | caller not permitted |
| Ownership      | 404 (cloaked) | no                | caller not owner     |
| Validation     | 400           | no                | client fixable       |
| Resource       | 404           | yes               | wrong identity       |
| Conflict       | 409           | no                | lifecycle conflict   |
| System         | 500/ 504      | yes               | safe retry           |
| Replay         | 200           | yes               | idempotent           |

### Status decision logic
| Logic              | Status        | Meaning                                    |
|:-------------------|:--------------|:-------------------------------------------|
| New payment        | 201           | Service processed a new record             |
| Idempotency replay | 200           | Returning already processed payment        |
| Malformed input    | 400           | Client correctable input                   |
| Not found          | 404           | Resource not found                         |
| System failure     | 500/ 504      | Retry safe                                 |
| Lifecycle conflict | 409           | Requesting payment for already paid orders |
| Unauthenticated    | 401           | Authentication failure                     |
| Unauthorized       | 403           | Authorization failure                      |
| Ownership conflict | 404 (cloaked) | Denied ownership                           |


Controller always takes care of new & existing records in response through boolean flags to return Http Status accordingly.

**Example:**

**Scenario - 1:** New resource

**Http status:** `201 CREATED`

**Scenario - 2:** Existing resource

**Http status:** `200 OK`

## Trace + Idempotency Propagation rules
Controller must propagate 3 IDs:

| Field          | Purpose             |
|:---------------|:--------------------|
| traceId        | distributed tracing |
| requestId      | audit + debugging   |
| idempotencyKey | retry semantics     |





