# Strong talk

- **Title:** Structured Concurrency in Practice: Replacing CompletableFuture Chains
- **Track:** Core Java
- **Level:** Intermediate
- **Format:** Talk 45 min

## Abstract

Most Java services still fan out to other services with long CompletableFuture
chains. They work, but cancellation leaks, error handling is scattered, and a
thread dump tells you nothing about which request a task belongs to.

In this talk we take a real order-checkout service that calls inventory,
pricing and shipping in parallel, and rewrite it step by step with
StructuredTaskScope on Java 25. We compare the two versions on three things:
how failures propagate, what happens when the client disconnects, and what the
observability tooling (JFR and thread dumps) shows.

You will leave with:
- a mental model of task scopes as "try-with-resources for threads",
- the two shutdown policies and when to pick which,
- a migration checklist we used on 14 production services, including the two
  places where we decided not to migrate.

No prior experience with virtual threads needed; basic CompletableFuture
knowledge helps.
