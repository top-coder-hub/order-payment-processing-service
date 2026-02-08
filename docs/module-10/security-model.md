# PROJECT 1 — Order & Payment Processing Service (FLAGSHIP)

## Module 10: Security Model (V1)

This document defines **security boundaries, invariants, and fintech-grade protections**
for the Order & Payment Processing Service.

Goal:
- Prevent unauthorized access
- Protect customer ownership
- Ensure safe payment execution
- Guarantee deterministic retries
- Maintain auditability & compliance

---

## 1. Authentication Scope (Identity Gate)

### Rule
**Authentication is required for ALL non-public endpoints.**

### Public Exceptions (if any)
- Health checks
- Service discovery
- Static metadata endpoints

### Enforcement
- Bearer Token validated at controller entry
- Missing / invalid token → **401 UNAUTHORIZED**

**Rationale:**  
Unauthenticated users must **never reach business logic or resource lookup**.

---

## 2. Authorization Model (Permissions)

### Roles & Allowed Actions

| Role     | Allowed               |
|----------|-----------------------|
| Customer | View & pay own orders |
| System   | Read & ship orders    |
| Admin    | Full access           |

### Forbidden
- System cannot pay or cancel orders
- Customers cannot access others’ orders

**Failure → 403 FORBIDDEN**

---

## 3. Ownership Invariant (Payments Are Private)

### Rule
A user may only access or mutate **orders they own**.

order.customerId == userId

### Enforcement Layer
- **Service layer** (requires DB lookup)

### Failure Behavior
- Ownership violation → **404 (CLOAKED)**

**Reason:** 

Prevents **ID Enumeration**,
User A requesting User B’s order must see the **same response** as a non-existent order.

---

## 4. Token vs Idempotency Semantics

| Mechanism       | Purpose           | Lifetime         |
|-----------------|-------------------|------------------|
| Bearer Token    | Identity & access | Minutes / Hours  |
| Idempotency Key | Retry safety      | Single operation |

**Rule:**  
- Tokens ≠ replay protection  
- Idempotency keys ≠ identity

---

## 5. Failure Semantics (401 vs 403 vs 404)

| Status | Meaning             | Security Reason              |
|--------|---------------------|------------------------------|
| 401    | Unauthenticated     | Identity missing             |
| 403    | Unauthorized        | Identity valid but forbidden |
| 404    | Not Found (Cloaked) | Hide resource existence      |

### Cloaking Strategy
Requests for:
- Non-existent orders  
- Orders owned by others  

→ **Must return identical 404 responses**

---

## 6. Replay Semantics (Idempotency)

### Rule
Same payload + same idempotency key → **exact same response**

### Guarantees
- No double charging
- No duplicate payments
- No duplicate lifecycle transitions

| Scenario              | Status       |
|-----------------------|--------------|
| First execution       | 201 CREATED  |
| Retry (same key)      | 200 OK       |
| Retry (different key) | 409 CONFLICT |

---

## 7. FSM Enforcement (Lifecycle Rules)

### Order States
* CREATED → PAID → SHIPPED
* CREATED → CANCELLED

### Forbidden Transitions
- PAID → CREATED
- CANCELLED → PAID
- PAID → PAID (duplicate payment)

Violation → **409 INVALID_ORDER_STATE**

---

## 8. Distributed Consistency (DB + Payment Gateway)

### Reality
A database transaction cannot span an external payment gateway
(e.g., Stripe / PayPal).

### Strategy (V1 — Synchronous Payment Model)
- Payment execution is synchronous
- Order lifecycle is updated **only after payment success**
- DB commit happens once, atomically, after payment confirmation
- Failed payments result in transaction rollback

### Guarantee
- No partial commits
- No orphan payments
- No duplicate charging
- Lifecycle state remains consistent

### Note (V1 Assumption)
This service uses a synchronous payment execution model.
Payment confirmation and database commit occur within the same transactional
boundary. Asynchronous gateways and webhook-based reconciliation are deferred
to V2, where an intermediate PAYMENT_PENDING state will be introduced.


---

## 9. Security Invariants (Fintech-Grade)

The system must always guarantee:

1. Ownership isolation
2. FSM integrity
3. Retry determinism
4. Atomic DB updates (order + payment)
5. No orphan payments
6. Full auditability (traceId, requestId, idempotencyKey)
7. Non-repudiation (users cannot deny payments)

---

## 10. Encryption & Data Protection

### Data In Transit
- HTTPS (TLS 1.2 / TLS 1.3 only)

### Data At Rest
- Encrypt sensitive DB fields (PII, internal transaction IDs)
- **Never store card details** (handled by Stripe)

### Compliance Principle
Minimize stored financial data.

---


## 11. Rate Limiting Strategy (V1)

**Algorithm:** Token Bucket

**Burst Handling:**
- Allows short request spikes for legitimate UI behavior
- Example: up to **5 requests per second**

**Sustained Limits:**
- Throttles continuous high-volume traffic
- Example: **60 requests per minute**

**Purpose:**
- Prevents automated order ID enumeration
- Protects cloaked `404` responses from statistical inference
- Preserves availability under abuse

**Enforcement Layer:**
- API Gateway / Load Balancer / Reverse Proxy
- Not implemented inside application code (V1 decision)

------
## 12. Summary

This model ensures:
- Strong identity enforcement
- Ownership-safe access
- Cloaked resource protection
- Safe retries
- Payment lifecycle correctness
- Fintech-grade security & auditability