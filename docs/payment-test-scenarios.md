
# PROJECT 1 — Order & Payment Processing Service (FLAGSHIP)

# Module 7 - Payment processing & idempotency


## 1. Functional test scenarios

###    Scenario 1 - Happy path

   **Request:**
   
`POST` `/orders/{orderId}/payments`
   
**Idempotency-Key:** pay123

   **payload:**
    
    amount: 100
   
    currency: USD

   **Pre-condition:**
   
    order:
   
    orderState = CREATED
    amount = 100
    currency = USD

   **Expected outcome:**
*    Payment inserted
*    Order state -> PAID
*    Return 201 created
*    Response contains paymentId + paymentState = COMPLETED

   **Invariants:**
*    No second payment exists
*    No lifecycle regression
*    One transaction commit

### Scenario 2 - Order not found

**Request:**

    orderId = 999

**Expected outcome:**
* 404 ORDER_NOT_FOUND
* No payment inserted
* No lifecycle change

### 	Scenario 3 - Currency mismatch

**Pre-condition:**

    order.currency = USD

**Request:**
    
    currency = INR

**Expected outcome:**
* 409 ORDER_CURRENCY_MISMATCH
* No Payment inserted
* No lifecycle change


### Scenario 4 - Order amount mismatch

**Pre-condition:**

    order.amount = 1000

**Request:**

    amount = 100

**Expected outcome:**
* 409 ORDER_AMOUNT_MISMATCH
* No payment inserted
* No lifecycle change

### 	Scenario 5 - Wrong lifecycle

**Pre-condition:**

    orderState = PAID

**Request:**

    Valid payment

**Expected outcome:**
* 409 INVALID_ORDER_STATE
* No new payment inserted
* No lifecycle change

## 2. Idempotency test scenarios

###    Scenario 6 - Retry success

**1st call:**
*    New Payment inserted
*    orderState -> PAID
*    Return 201 created
*    Response contains paymentId + paymentState = COMPLETED
   
**2nd call:**
*    Existing payment response
*    No duplicate payment insert		
*    200 ok (retry)


**Invariants:**
*    Payment count = 1
*    orderState -> PAID

### Scenario 7 - Retry after timeout (gateway error)

**1st call:** Time out but server committed.

**2nd call:** Comes later

**Expected outcome:**
Same response as Scenario 6

### Scenario 8 - Retry after crash (server restart)

**Flow:**
* Request received
* DB committed
* Server restarted
* Retry with same idempotency key

**Expected outcome:**
* Same paymentId response
* No double payment charge

### Scenario 9 - Retry without idempotency key

**Response:**
* 400 Bad Request - Missing mandatory Header


### Successful retry scenario

* payment count = 1
* orderState = PAID
* No duplicate inserts
* No duplicate state transitions
* Transaction committed or aborted

## 3. Concurrency test scenarios

   ### Concurrency model: Optimistic

###    Scenario 10 - Two payments racing on same order

**Request:**
   
**Thread A:**

`POST` `/orders/5/payments`

**idempotency-key:** pay123
 
**Thread B:**

`POST` `/orders/5/payments`
   
**idempotency-key:** pay124

**Expected outcome:**
   
* Exactly one succeeds
* Another fails with 409 INVALID_ORDER_STATE (Optimistic Lock Failure).
   
**Forbidden:**
* 2 completed payments
* Double payment charge
* Duplicate state transition

### Scenario 11 - Two payments racing with same idempotency key

**Request:**

**Thread A:**

`POST` `/orders/5/payments`

**idempotency-key:** pay123

**Thread B:**

`POST` `/orders/5/payments`

**idempotency-key:** pay123

**Expected outcome:**

* One inserts payment
* Another gets same payment response (idempotent)

## 4. Failure scenarios

### Scenario 12 - Payment after cancelled

**Pre-condition:**

    orderState = CANCELLED

**Expected outcome:**
   
    409 INVALID_ORDER_STATE

### Scenario 13 - Payment after shipment

**Pre-condition:**

    orderState = SHIPPED

**Expected outcome:**

    409 INVALID_ORDER_STATE

## State Transition Table

| Current order state | Action: Pay    | Outcome        |
|:--------------------|:---------------|:---------------|
| CREATED             | Allowed        | 201 Created    |
| PAID                | Denied         | 409 Conflict   |
| CANCELLED           | Denied         | 409 Conflict   |
| SHIPPED             | Denied         | 409 Conflict   |