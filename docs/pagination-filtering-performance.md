# MODULE 11 — Pagination, Filtering & Performance (V1)

## Overview

Module 11 focuses on introducing safe pagination, minimal filtering, and performance hardening
to make the Order API production-ready without over-engineering.

This module strictly follows V1 scope:
- Offset-based pagination only
- Fixed sorting
- Minimal filtering
- Defensive limits
- Proper indexing

---

## 1️⃣ Pagination (Mandatory & Safe)

### Default Behavior

- `page = 0`
- `size = 20`
- Offset-based pagination using Spring `PageRequest`
- Sorted by `createdAt DESC` (stable sort)

### Safety Controls

- Max page size cap: `100`
- No unlimited queries allowed
- Defensive capping using:

```java
int pageSize = Math.min(size, MAX_PAGE_SIZE);
```
**If client requests size > 100:**

* Request is sanitized
* WARN log emitted
* Query safely executed

### Response Structure

Custom DTO used (not Spring Page):
```json
{
"content": [...],
"page": 0, 
"requestedSize": 300, 
"appliedSize": 100,
"totalElements": 145,
"totalPages": 2,
"last": false
}
```
### Why custom DTO?

* Stable API contract
* No framework leakage
* Future flexibility
----
## 2️⃣ Filtering (Minimal V1 Scope)

### Supported filters:

* Implicit customerId (via RequestContext)
* Optional orderState

### Not supported (by design):

* Dynamic query engine
* Search DSL
* Multiple field filtering
* Free-text search
* Sorting customization

### Enum validation performed before DB hit:

```java
OrderState.fromString(...)
```

### Invalid values return:

- `400 Bad Request`
- `INVALID_LOGICAL_REQUEST`


### Security Enforced: 

customerId is never accepted as a request parameter; it is extracted from the authenticated session to prevent "Insecure Direct Object Reference" (IDOR) attacks.

------

## 3️⃣ Performance Hardening

### Index Strategy

Defined composite indexes:

```mysql-sql
(customer_id, order_state)
(customer_id, created_at)
```

### Rationale:

* Supports ownership filtering
* Supports filtered view efficiently
* Supports sorted pagination efficiently
* Avoids full table scans

----

### Query Patterns Supported

### A) All Orders View

```mysql-sql
WHERE customer_id = ?
ORDER BY created_at DESC
LIMIT ?, ?
```

**Supported by:**

(customer_id, created_at DESC)

### B) Filtered View

```mysql-sql
WHERE customer_id = ?
AND order_state = ?
ORDER BY created_at DESC
LIMIT ?, ?
```

**Supported by:**

(customer_id, order_state)

----

## 4️⃣ Logging Strategy (Pagination)

* **DEBUG**: Normal fetch
* **WARN**: If requested page size exceeds maximum
* No noisy INFO logs for fetch endpoints

**Example defensive log:**

`Page size 300 exceeds max limit. Capped to 100 for customerId=123`

----

## 5️⃣ Security & Isolation

* Ownership enforced at repository level
* Resource cloaking implemented
* No cross-customer data exposure

---

## 6️⃣ Out of Scope (V1)

* Cursor pagination
* Keyset pagination
* Custom sorting
* Dynamic filtering engine
* Search indexing

----

## Conclusion

### Module 11 ensures:

* Safe pagination
* Controlled filtering
* Optimized queries
* Defensive performance protection
* Stable API contract