# Use Case: Music Track Feedback

## Overview

**Business Goal**: Gather feedback on new music releases to inform playlist curation and artist recommendations.

**Processing Mode**: Batch (Daily scheduled job)

**Actors**:
- Music listener (customer)
- Music curation team
- Artist relations team
- Data warehouse
- Email campaign service

---

## Actor Interaction Diagram

```
┌──────────┐         ┌──────────────┐         ┌─────────────────┐
│ Listener │         │ Curation Team│         │ Artist Relations│
└────┬─────┘         └──────┬───────┘         └────────┬────────┘
     │                      │                          │
     │ 1. Listens to        │                          │
     │    New Track         │                          │
     ├──────────────────────┼──────────────────────────┤
     │                      │                          │
     │ 2. Completes         │                          │
     │    Full Playback     │                          │
     ├──────────────────────┼──────────────────────────┤
     │                      │                          │
     │                      │ 3. Configures            │
     │                      │    Feedback Program      │
     │                      ├─────────────────────────>│
     │                      │                          │
     │                      │                          │ 4. Schedules
     │                      │                          │    Daily Batch
     │                      │                          ├────────────┐
     │                      │                          │            │
     │                      │                          │<───────────┘
     │                      │                          │
     │ 5. Receives Feedback │                          │
     │    Request Email     │<─────────────────────────┤
     │<─────────────────────┤                          │
     │                      │                          │
     │ 6. Provides Feedback │                          │
     │    (👍/👎 + comment) │                          │
     ├─────────────────────>│                          │
     │                      │                          │
     │                      │ 7. Updates Playlists     │
     │                      │<─────────────────────────┤
     │                      │                          │
     ▼                      ▼                          ▼
```

---

## Data Ingestion Flow (Batch Processing)

### Daily Batch Job - 2:00 AM

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         DATA INGESTION FLOW                              │
│                      (Batch - Daily at 2:00 AM)                          │
└─────────────────────────────────────────────────────────────────────────┘

Step 1: ETL Lambda (2:00 AM - 2:30 AM)
┌──────────────────┐
│  Data Warehouse  │
│  (Redshift)      │
└────────┬─────────┘
         │ Query: SELECT listener_id, track_id, play_count,
         │        completion_rate, engagement_score
         │        FROM music_streams
         │        WHERE track_release_date >= CURRENT_DATE - 7
         │        AND completion_rate >= 0.95
         │        AND play_count >= 1
         ▼
┌─────────────────────────────────────┐
│    Data Warehouse Connector         │
│  - Extract 2M listening records     │
│  - Filter: New tracks (last 7 days) │
│  - Filter: Full playback (95%+)     │
│  - Map fields to candidate model    │
└────────┬────────────────────────────┘
         │ 2M raw candidates
         ▼
┌─────────────────────────────────────┐
│         ETL Lambda                  │
│  - Transform to unified model       │
│  - Add context (marketplace, genre) │
│  - Set event metadata               │
│  - Deduplicate by listener+track    │
└────────┬────────────────────────────┘
         │ 1.5M unique candidates
         │ {listenerId, trackId, playbackDate, ...}
         ▼

Step 2: Filter Lambda (2:30 AM - 2:45 AM)
┌─────────────────────────────────────┐
│      Filter Chain Executor          │
└────────┬────────────────────────────┘
         │
         ├─> Trust Filter
         │   ├─ Verify listener authenticity
         │   ├─ Check for bot activity
         │   └─ Result: Remove 100K (6.7%)
         │
         ├─> Eligibility Filter
         │   ├─ Listener hasn't given feedback for this track
         │   ├─ Track is eligible for feedback
         │   ├─ Listener hasn't opted out
         │   └─ Result: Remove 400K (26.7%)
         │
         ├─> Engagement Filter
         │   ├─ Check listener engagement score
         │   ├─ Minimum score: 0.6
         │   └─ Result: Remove 500K (33.3%)
         │
         ├─> Frequency Cap Filter
         │   ├─ Max 2 feedback requests per week
         │   └─ Result: Remove 300K (20%)
         │
         ▼
    200,000 eligible candidates
         │
         ▼
