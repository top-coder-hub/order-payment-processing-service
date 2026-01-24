# PROJECT 1 — Order & Payment Processing Service (FLAGSHIP)

## Module 10 — Authorization & Ownership Rules (V1)

This document defines **who can do what**, **on which resources**, and **where checks are enforced**
in the Order & Payment Processing Service.

---

## 1. Role Matrix (Who can do what)

| Role| Action | Endpoint  | Resource Scope  |
|-----|--------|-----------------------------------|-----------------|
| Customer | View order | GET /orders/{orderId}             | Own orders only |
| Customer | Make payment | POST /orders/{orderId}/payments  | Own orders only |
| System  | View orders| GET /orders/{orderId}             | All orders      |
| System  | Ship orders| POST /orders/{orderId}/ship       | All orders      |
| Admin   | View orders| GET /orders/{orderId}             | All orders      |
| Admin   | View payments | GET /payments/{paymentId}         | All payments    |

---

## 2. Ownership Invariant (Core Rule)

A user may access or mutate **only the orders they own**.

order.customerId == authenticatedUserId

Applies to:
- Viewing orders
- Making payments
- Cancelling orders

---

## 3. Where Checks Live (Layered Responsibility)

| Check Type      | Layer      | Reason |
|-----------------|------------|--------|
| Authentication  | Controller | Entry gate (no DB required) |
| Authorization   | Controller | Role-based decision |
| Ownership       | Service    | Requires DB lookup |
| Business FSM    | Service    | Domain rules |

**Principle:**  
Controller guards the **door**, Service protects the **data**.

---

## 4. Failure Semantics (Security-Correct)

| Scenario                                   | HTTP Status | Reason |
|-------------------------------------------|-------------|--------|
| Missing / invalid token                   | 401         | Identity unknown |
| Valid user, forbidden role                | 403         | Permission denied |
| Order owned by another user               | 404 (cloaked) | Prevent ID enumeration |
| Order does not exist                      | 404         | Resource not found |

**Cloaking Rule:**  
Ownership violations must be indistinguishable from non-existent resources.

---

## 5. Correct HTTP Mapping Summary

| Condition              | Status |
|------------------------|--------|
| New authorized payment | 201    |
| Idempotent retry       | 200    |
| Lifecycle conflict     | 409    |
| Ownership violation    | 404    |
| Authorization failure  | 403    |
| Authentication failure | 401    |

---

## 6. Fintech-Specific Notes

- Payments are **private resources**
- **Ownership isolation prevents:**

  - Account takeover
  - Order enumeration
  - Fraudulent payments
- Cloaked `404` is a **security feature**
- Authorization failures must not leak resource existence

---

## 7. V1 Scope Clarification

- No delegated access
- No cross-account payments
- No multi-tenant role hierarchy
- Simple and strict ownership model

This keeps V1 **secure, predictable, and auditable**.