# PROJECT 1 — Order & Payment Processing Service (FLAGSHIP)

## Module 10 — Token Failure Semantics (401 vs 403 vs 404)

This document defines **how authentication, authorization, and ownership failures
map to HTTP status codes** in the Order & Payment Processing Service.

Correct status usage is critical for **security, cloaking, and fintech correctness**.

---

## 1. 401 — UNAUTHENTICATED

### When to return
- Missing bearer token
- Expired token
- Malformed token
- Invalid signature

### Meaning
The caller’s **identity cannot be established**.

### Example
`POST` `/orders/12/payments`

**Authorization:** (missing)

**Response**
401 UNAUTHENTICATED
### Rule
Authentication failures are handled at the **security filter / controller boundary**.
The request must not reach business logic.

---

## 2. 403 — FORBIDDEN

### When to return
- Identity is valid
- Caller’s role is not permitted to perform the action

### Meaning
The caller **is known**, but **not allowed** to perform this operation.

### Example

**Role:** SYSTEM

**Action:** `POST` `/orders/12/payments`


**Response**
403 FORBIDDEN


### Rule
Authorization is role-based and enforced **before ownership checks**.

Ownership violations must NOT return 403.


---

## 3. 404 — NOT FOUND (CLOAKED)

### When to return
- Resource does not exist
- Resource exists but belongs to another user

### Meaning
The system **must not reveal resource existence**.

### Example
User A requests order owned by User B


**Response**
404 NOT FOUND


### Security Principle
This prevents **ID enumeration attacks**.

Ownership violations and missing resources must be **indistinguishable**.

Order existence checks must be performed only after authentication.

### Latency Consistency

Ensure that the time taken to return a 404 for a non-existent order is 
roughly the same as a 404 for an ownership violation. 
This prevents attackers from using "**Response Time Analysis**" to guess 
which IDs are real.

---

## 4. Decision Flow (Simplified)

**Request**

    → Authenticate?
        → No → 401
        → Yes
            → Authorized?
                → No → 403
                → Yes
                    → Owns resource?
                        → No → 404 (cloaked)
                        → Yes → Process request

  
#### Payload Uniformity 

The JSON response body for a 403 (Role check failed) should look 
structurally different from a 404, but all 404 responses 
(whether real or cloaked) must return the exact same message string 
(e.g., "Resource not found"). Never leak ownership details in the error 
message.

---

## 5. Summary Table

| Status | Used When | Security Goal |
|------|----------|---------------|
| 401 | Identity missing or invalid | Block unauthenticated access |
| 403 | Role not permitted | Enforce permissions |
| 404 | Resource hidden | Prevent enumeration |

---
## 6. Observability & Compliance

### Log Integrity

While the user sees a "Cloaked 404," the internal server logs must 
capture the actual reason (e.g., OWNERSHIP_VIOLATION) alongside the 
traceId. This allows developers to debug without compromising the public 
security posture.

-----

## 7. V1 Scope Notes

- No distinction between "not found" and "not owned"
- Cloaking is intentional and mandatory
- Tokens are required for all non-public endpoints

This keeps V1 **secure, predictable, and fintech-correct**.

