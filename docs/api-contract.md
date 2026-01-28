
# PROJECT 1 — Order & Payment Processing Service (FLAGSHIP)

## Module 2 - REST API design & endpoint contracts


**Core API Endpoints:**
-------


### ***1. Create Order***

`POST` `/orders`

**Purpose -**
Create a new order resource.

**Characteristics**
* Unsafe
* Non-idempotent

**Request (json)**
* customerId
* totalAmount
* currency

**Response**

201 created
* orderId
* orderState = CREATED

### ***2. Get order by ID***

`GET` `/orders/{orderId}`

**Purpose -**
Fetch order details.

**Characteristics**
* Safe
* Idempotent
* Cacheable (future)

**Request (Path variable)**
orderId

**Response**

200 OK -> order details

404 Not Found -> invalid orderId

### ***3. Get orders(Order History)***

`GET` `/orders?customerId=&page=&size=&orderState=`

**orderState:** Filter by current state (`CREATED`, `PAID`, `SHIPPED`, `CANCELLED`).

**Purpose -**
Fetch order details with mandatory pagination

**Request (Query parameters)**
* customerId
* page
* size
* orderState

**Reasoning**

* Query parameters represent **resource representation** not identity.
* Mandatory pagination prevents unbounded result sets (production safety).

**Response**

200 OK -> List of orders (possibly empty)

**Note:** An Empty list is a valid response, not an error.

### ***4. Cancel order***

`POST` `/orders/{orderId}/cancel`

**Purpose -**
Cancel an order as a business action.

**Why POST (not DELETE)?**

* Cancellation is a state transition, not data removal.
* Order history must be preserved for audit.
* Explicit action endpoints avoid accidental destructive semantics.

**Request (Path variable)**
orderId

**Valid only**
orderState = CREATED

**Response**

* 200 OK -> cancelled
* 409 Conflict -> invalid state transition
* 404 Not Found -> invalid orderId

Order State Machine
---

`CREATED` ➔ `PAID` ➔ `SHIPPED`

`CREATED` ➔ `CANCELLED`

**Payment API - Contract design:**
---

### ***Make payment (Idempotent operation)***

`POST` `/orders/{orderId}/payments`

**Purpose -**
Process payment for an order.

**Headers (mandatory):** `Idempotency-Key: <UUID>`

**Request body (json):**

{

    "amount": 150.00,
  
    "currency": "USD"
}

**Rules:**
* One successful payment per order.
* Same Idempotency-key -> same result.
* Retry safe operation.

**Response:**
* 201 CREATED -> Payment processed (first attempt)
* 200 OK -> duplicate retry; already processed
* 409 Conflict -> order already paid/ cancelled
* 404 Not Found -> invalid orderId

**Why Nested under orders?**
* Payment has no meaning without an order.
* Ownership and lifecycle is clearly defined.

**Important**
* Payments are append-only.
* No Update or Delete APIs exposed.

### **HTTP methods summary**

1. `POST` `/orders`
2. `GET` `/orders/{orderId}`
3. `GET` `/orders?customerId=&page=&size=&orderState=`
4. `POST` `/orders/{orderId}/cancel`
5. `POST` `/orders/{orderId}/payments`

### **Request fields (high level)**

1. orderId
2. customerId
3. page
4. size
5. orderState

### **Error contract**

All errors must be consistent.

#### **Conceptual structure:**

* timestamp
* status
* errorCode
* message
* traceId

#### Example

{

        "timestamp": "2026-01-13T23:58:51.240454",
        "status": 409,
        "errorCode": "ORDER_CURRENCY_MISMATCH",
        "message": "The requested payment currency (INR) does not match the order currency (USD)",
        "traceId": "4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b"
}


#### **Reasoning**

* Predictable Client handling
* Centralized logging & monitoring
* Easier debugging in distributed systems


### **Status code strategy**

| Scenario                   | Status |
|:---------------------------|:-------|
| Create order               | 201    |
| Fetch order                | 200    |
| Invalid input              | 400    |
| Order not found            | 404    | 
| Invalid state transition   | 409    |
| Duplicate payment retry    | 200    |
| Server failure             | 500    |


### **Idempotency summary**

| Method | Endpoint                       | Description        | Idempotent            |
|:-------|:-------------------------------|:-------------------|:----------------------| 
| `GET`  | `/orders/{orderId}`            | Retrieve order     | Yes                   |
| `POST` | `/orders `                     | Create new order   | NO                    |
| `POST` | `/orders/{orderId}/payments`   | Process payment    | Yes (idempotency-key) |


### Final Assessment (Engineer POV)

This API design
* Protects data integrity
* Handles retries safely
* Preserves audit history
* Clearly communicates business intent
* Is suitable for production deployment 

