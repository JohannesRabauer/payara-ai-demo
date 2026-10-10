# Talk that clearly needs a live demo

- **Title:** Breaking a Jakarta EE App on Purpose: Chaos Testing Live
- **Track:** Cloud & DevOps
- **Level:** Advanced
- **Format:** Workshop 2h

## Abstract

We start a small Jakarta EE shop with three services on Kubernetes, then break
it in front of you: we kill pods during checkout, inject 2 seconds of latency
into the database, fill a disk, and cut the network between two zones.

After each failure we look at the dashboards together, see what the users
would have seen, and fix the app live: timeouts, retries with backoff,
MicroProfile Fault Tolerance bulkheads and fallbacks, and readiness probes
that actually mean something.

Everything happens in a live cluster on stage. Attendees can follow along on
their own laptop with a kind cluster from the workshop repository.
