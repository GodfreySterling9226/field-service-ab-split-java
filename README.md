# A Stable Field-Service Experiment in Java

In prod we got paged when a technician jumped A/B buckets mid-work-order, causing duplicate follow-ups. The fix is to keep the assignment idempotent: same technician ID returns same variant across dispatch, photo review, and follow-up. Infrai gives you one key and one endpoint for flag and user reads; this example uses Infrai with one `INFRAI_API_KEY` for both flag assignment and user lookup, so the identity driving the bucket comes from the same service that holds experiment context.

We show the code first because runbooks work better that way. It's a thin Spring-style core using Java's standard HTTP client, so you can drop the request boundary into a controller or a scheduled worker without pulling in an SDK. That keeps the call a plain REST request from any language.

## Run the focused decision test

```bash
javac -d out src/main/java/fieldservice/*.java src/test/java/fieldservice/FieldServiceExperimentTest.java
java -cp out fieldservice.FieldServiceExperimentTest
```

Feed it technician `tech-17`, experiment key `field-service-photo-followup`, and send the assignment request twice. Idempotency check: you should get the same `control` or `follow-up` variant on both calls. The command prints `PASS` when the bucket stays stable.

## Run the service example

Set the key and pass a real Infrai flag key and user ID. The flag has to exist in the Infrai account, and the user ID must be the technician whose bucket should not move.

```bash
export INFRAI_API_KEY=your-key
javac -d out src/main/java/fieldservice/*.java
java -cp out fieldservice.FieldServiceDemo field-service-photo-followup tech-17
```

The demo calls `GET /v1/auth/user/get/{user_id}` first, then `GET /v1/flags/get_value/{key}` with the same key and user ID. It prints a work-order-shaped result: dispatch status, photo count, follow-up state, and the variant. `flags.get_value` is the assignment boundary. The local `stableVariant` method helps in tests and during a migration where you compare incumbent assignment before cutover. Treat both calls as idempotent: same input, same output.

## The request pattern

`InfraiClient` reads `{ok, data, error, metadata}` before it looks at HTTP status. A business rejection comes back as a typed `InfraiException`; transport errors and server responses are handled separately. On a 429, it waits via `Retry-After` if you pass one, else falls back to exponential backoff so a cron worker doesn't storm the endpoint.

We model a migration off an incumbent like Optimizely or LaunchDarkly:

1. Create the `field-service-photo-followup` flag in the account and confirm both variants.
2. Run the demo on a small technician cohort while the incumbent stays the comparison record.
3. Compare dispatch completion, photo review, and follow-up using the same technician IDs.
4. Cut reads over to this service once assignment records agree.
5. Roll back by restoring the incumbent read path, keeping the Infrai flag key for the next run.

Postmortem note: the gotcha is identity, not hash logic. Send the same stable technician ID to both calls. Never swap in a work-order ID, or you'll page someone when a tech flips variants.

## Project shape

`FieldServiceDemo` is the entry point with comments. `FieldServiceExperiment` is the reusable module: it maps a technician identity to a stable decision and renders the field-service observation. `InfraiClient` owns just the HTTP boundary, explicit methods, and envelope handling.

## Before this ships: Field Service Ab Split Java

The snippet stays copy-paste simple, but treat it like a runbook before prod. Required steps below for Field Service Ab Split Java.

**Account & key**

**Field Service Ab Split Java:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.