# Use Case: Video Ratings (Reactive)

## Overview

**Business Goal**: Collect ratings for videos immediately after watch completion to improve recommendation algorithms.

**Processing Mode**: Reactive (Real-time event-driven)

**Actors**:
- Video viewer (customer)
- Content team
- Recommendation team
- EventBridge
- In-app notification service

---

## Actor Interaction Diagram

```
┌──────────┐         ┌──────────────┐         ┌─────────────────┐
│  Viewer  │         │ Content Team │         │ Recommendation  │
└────┬─────┘         └──────┬───────┘         └────────┬────────┘
     │                      │                          │
     │ 1. Watches Video     │                          │
     │    to Completion     │                          │
     ├──────────────────────┼──────────────────────────┤
     │                      │                          │
     │ 2. Watch Event       │                          │
     │    Published         │                          │
     ├──────────────────────┼──────────────────────────┤
     │                      │                          │
     │                      │ 3. Configures            │
     │                      │    Rating Program        │
     │                      ├─────────────────────────>│
     │                      │                          │
     │ 4. Sees In-App       │                          │
     │    Rating Card       │<─────────────────────────┤
     │<─────────────────────┤    (Real-time)           │
     │                      │                          │
     │ 5. Provides Rating   │                          │
     │    (1-5 stars)       │                          │
     ├─────────────────────>│                          │
     │                      │                          │
     │                      │ 6. Updates Recommendations│
     │                      │<─────────────────────────┤
     │                      │                          │
     ▼                      ▼                          ▼
```

---

## Reactive Data Flow (Real-Time Processing)

### Event-Driven Processing - <1 Second End-to-End

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      REACTIVE DATA FLOW                                  │
│                   (Real-Time Event Processing)                           │
└─────────────────────────────────────────────────────────────────────────┘

T+0ms: Customer Completes Video
┌─────────────────────────────────────┐
│      Video Player Service           │
│  - Detects watch completion         │
│  - watchPercentage >= 80%           │
│  - Publishes event to EventBridge   │
└────────┬────────────────────────────┘
         │ Event: {
         │   customerId: "C123",
         │   videoId: "V456",
         │   watchPercentage: 95,
         │   timestamp: "2026-01-18T12:34:56Z"
         │ }
         ▼

T+10ms: EventBridge Routes Event
┌─────────────────────────────────────┐
│         EventBridge Rule            │
│  Pattern: {                         │
│    source: "video.player",          │
│    detail-type: "WatchCompleted",   │
│    detail: {                        │
│      watchPercentage: [80, 100]     │
│    }                                │
│  }                                  │
│  Target: Reactive Lambda            │
└────────┬────────────────────────────┘
         │ Event matched, trigger Lambda
         ▼

T+20ms: Reactive Lambda Invoked
┌─────────────────────────────────────┐
│      Reactive Lambda Handler        │
│  1. Parse event (5ms)               │
│  2. Check deduplication (10ms)      │
│  3. Create candidate object (5ms)   │
└────────┬────────────────────────────┘
         │ Candidate created
         ▼
