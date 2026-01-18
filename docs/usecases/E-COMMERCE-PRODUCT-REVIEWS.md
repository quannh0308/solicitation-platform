# Use Case: E-Commerce Product Reviews

## Overview

**Business Goal**: Increase product review volume by 30% by soliciting reviews from verified purchasers at optimal times.

**Processing Mode**: Batch (Daily scheduled job)

**Actors**:
- Customer (purchaser)
- Product team
- Marketing team
- Data warehouse
- Email campaign service

---

## Actor Interaction Diagram

```
┌──────────┐         ┌──────────────┐         ┌─────────────────┐
│ Customer │         │ Product Team │         │ Marketing Team  │
└────┬─────┘         └──────┬───────┘         └────────┬────────┘
     │                      │                          │
     │ 1. Purchases         │                          │
     │    Product           │                          │
     ├──────────────────────┼──────────────────────────┤
     │                      │                          │
     │ 2. Receives          │                          │
     │    Delivery          │                          │
     ├──────────────────────┼──────────────────────────┤
     │                      │                          │
     │                      │ 3. Configures            │
     │                      │    Review Program        │
     │                      ├─────────────────────────>│
     │                      │                          │
     │                      │                          │ 4. Schedules
     │                      │                          │    Daily Batch
     │                      │                          ├────────────┐
     │                      │                          │            │
     │                      │                          │<───────────┘
     │                      │                          │
     │ 5. Receives Review   │                          │
     │    Request Email     │<─────────────────────────┤
     │<─────────────────────┤                          │
     │                      │                          │
     │ 6. Submits Review    │                          │
     ├─────────────────────>│                          │
     │                      │                          │
     │                      │ 7. Reviews Metrics       │
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

Step 1: ETL Lambda (2:00 AM - 2:15 AM)
┌──────────────────┐
│  Data Warehouse  │
│  (Athena/Glue)   │
└────────┬─────────┘
         │ Query: SELECT * FROM deliveries
         │        WHERE delivery_date = YESTERDAY
         │        AND delivery_status = 'COMPLETED'
         ▼
┌─────────────────────────────────────┐
│      Athena Connector               │
│  - Extract 500,000 delivery records │
│  - Map fields to candidate model    │
│  - Validate schema                  │
└────────┬────────────────────────────┘
         │ 500,000 raw candidates
         ▼
┌─────────────────────────────────────┐
│         ETL Lambda                  │
│  - Transform to unified model       │
│  - Add context (marketplace, prog)  │
│  - Set event metadata               │
└────────┬────────────────────────────┘
         │ 500,000 candidates
         │ {customerId, productId, deliveryDate, ...}
         ▼

Step 2: Filter Lambda (2:15 AM - 2:25 AM)
┌─────────────────────────────────────┐
│      Filter Chain Executor          │
└────────┬────────────────────────────┘
         │
         ├─> Trust Filter
         │   ├─ Check customer-product relationship
         │   ├─ Verify purchase authenticity
         │   └─ Result: Remove 50,000 (10%)
         │
         ├─> Eligibility Filter
         │   ├─ Customer hasn't reviewed this product
         │   ├─ Product is eligible for reviews
         │   ├─ Customer hasn't opted out
         │   └─ Result: Remove 100,000 (20%)
         │
         ├─> Frequency Cap Filter
         │   ├─ Check review requests in last 30 days
         │   ├─ Max 3 requests per customer per month
         │   └─ Result: Remove 150,000 (30%)
         │
         ▼
    200,000 eligible candidates
         │
         ▼

Step 3: Score Lambda (2:25 AM - 2:45 AM)
┌─────────────────────────────────────┐
│     Feature Store Client            │
│  - Retrieve customer features       │
│    • engagement_score               │
│    • review_history                 │
│    • trust_score                    │
│  - Retrieve product features        │
│    • category                       │
│    • price                          │
│    • rating_count                   │
└────────┬────────────────────────────┘
         │ Features for 200,000 candidates
         ▼
┌─────────────────────────────────────┐
│    SageMaker Scoring Provider       │
│  - Call review propensity model     │
│  - Batch scoring (1000 at a time)   │
│  - Score range: 0.0 - 1.0           │
└────────┬────────────────────────────┘
         │ Scores computed
         ▼
┌─────────────────────────────────────┐
│       Score Cache Repository        │
│  - Cache scores in DynamoDB         │
│  - TTL: 7 days                      │
└────────┬────────────────────────────┘
         │ 200,000 scored candidates
         │ Score distribution:
         │   High (0.7-1.0): 50,000
         │   Medium (0.4-0.7): 100,000
         │   Low (0.0-0.4): 50,000
         ▼

Step 4: Store Lambda (2:45 AM - 3:00 AM)
┌─────────────────────────────────────┐
│   DynamoDB Candidate Repository     │
│  - Batch write (25 items per batch) │
│  - Primary key: CustomerId:Program  │
│  - GSI1: Program:Channel:Score      │
│  - GSI2: Program:Date               │
│  - TTL: 30 days from now            │
└────────┬────────────────────────────┘
         │ 200,000 candidates stored
         ▼
┌─────────────────────────────────────┐
│         DynamoDB Tables             │
│  ┌─────────────────────────────┐   │
│  │ Candidates Table            │   │
│  │ - 200,000 new items         │   │
│  │ - Partitioned by customerId │   │
│  │ - Indexed by program+score  │   │
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘

Batch Job Complete: 3:00 AM
Total Time: 1 hour
Throughput: 8,333 candidates/minute
```

---

## Data Contribution Flow (Customer Interaction)

### Email Delivery - 10:00 AM

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      DATA CONTRIBUTION FLOW                              │
│                   (Customer Review Submission)                           │
└─────────────────────────────────────────────────────────────────────────┘

Step 1: Email Channel Adapter (10:00 AM)
┌─────────────────────────────────────┐
│      Serving API Query              │
│  Query: GetCandidatesForChannel     │
│  Params:                            │
│    - channel: "email"               │
│    - program: "product-reviews"     │
│    - minScore: 0.7                  │
│    - limit: 50,000                  │
└────────┬────────────────────────────┘
         │ Query DynamoDB GSI1
         │ GSI1PK = "PROGRAM#reviews#CHANNEL#email"
         │ Filter: score >= 0.7
         ▼
┌─────────────────────────────────────┐
│    Top 50,000 Candidates            │
│  - Sorted by score (descending)     │
│  - All have score >= 0.7            │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│   Email Channel Adapter             │
│  - Group by marketplace             │
│  - Create campaign per marketplace  │
│  - Set template: "review-request"   │
│  - Set send time: 10 AM local       │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│   Email Campaign Service            │
│  - Create 3 campaigns (US, UK, DE)  │
│  - Upload recipient lists           │
│  - Schedule delivery                │
│  - Enable tracking                  │
└────────┬────────────────────────────┘
         │ 50,000 emails sent
         ▼

Step 2: Customer Receives Email (10:00 AM - 11:00 AM)
┌─────────────────────────────────────┐
│         Customer Inbox              │
│  Subject: "How was your [Product]?" │
│  Body: Personalized review request  │
│  CTA: "Write a Review" button       │
└────────┬────────────────────────────┘
         │
         │ 12,500 customers open email (25%)
         ▼
┌─────────────────────────────────────┐
│      Customer Opens Email           │
│  - Tracking pixel fires             │
│  - Open event recorded              │
└────────┬────────────────────────────┘
         │
         │ 5,000 customers click CTA (10% CTR)
         ▼
┌─────────────────────────────────────┐
│    Review Submission Page           │
│  - Pre-filled: Product, Customer    │
│  - Form: Rating (1-5 stars)         │
│  - Form: Review text (optional)     │
│  - Form: Photos (optional)          │
└────────┬────────────────────────────┘
         │
         │ 2,500 customers submit (5% conversion)
         ▼
┌─────────────────────────────────────┐
│      Review Service API             │
│  - Validate review                  │
│  - Store in review database         │
│  - Publish review event             │
│  - Update product rating            │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│    Candidate Status Update          │
│  - Mark candidate as "consumed"     │
│  - Record delivery timestamp        │
│  - Record submission timestamp      │
│  - Update metrics                   │
└─────────────────────────────────────┘

Contribution Complete
Reviews Collected: 2,500
Conversion Rate: 5% (2,500 / 50,000)
```

---

## Scheduled Jobs

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           SCHEDULED JOBS                                 │
└─────────────────────────────────────────────────────────────────────────┘

Daily Schedule:
┌──────────┬────────────────────────────────────────────────────────────┐
│   Time   │                        Job                                 │
├──────────┼────────────────────────────────────────────────────────────┤
│ 2:00 AM  │ Batch Ingestion Workflow (ETL → Filter → Score → Store)   │
│          │ - Process yesterday's deliveries                           │
│          │ - Duration: 1 hour                                         │
│          │ - Output: 200,000 candidates                               │
├──────────┼────────────────────────────────────────────────────────────┤
│ 3:00 AM  │ Data Warehouse Export                                      │
│          │ - Export candidates to S3 (Parquet)                        │
│          │ - Update Glue catalog                                      │
│          │ - Duration: 15 minutes                                     │
├──────────┼────────────────────────────────────────────────────────────┤
│ 10:00 AM │ Email Campaign Delivery                                    │
│          │ - Retrieve top candidates (score >= 0.7)                   │
│          │ - Create email campaigns                                   │
│          │ - Send 50,000 emails                                       │
│          │ - Duration: 30 minutes                                     │
├──────────┼────────────────────────────────────────────────────────────┤
│ 11:59 PM │ Metrics Aggregation                                        │
│          │ - Aggregate daily metrics                                  │
│          │ - Publish to CloudWatch                                    │
│          │ - Generate daily report                                    │
│          │ - Duration: 5 minutes                                      │
└──────────┴────────────────────────────────────────────────────────────┘

Weekly Schedule:
┌──────────┬────────────────────────────────────────────────────────────┐
│   Time   │                        Job                                 │
├──────────┼────────────────────────────────────────────────────────────┤
│ Sunday   │ Cleanup Expired Candidates                                 │
│ 1:00 AM  │ - DynamoDB TTL handles automatic deletion                  │
│          │ - Verify cleanup completed                                 │
│          │ - Duration: 10 minutes                                     │
├──────────┼────────────────────────────────────────────────────────────┤
│ Sunday   │ Model Retraining                                           │
│ 3:00 AM  │ - Retrain review propensity model                          │
│          │ - Evaluate model performance                               │
│          │ - Deploy if improved                                       │
│          │ - Duration: 2 hours                                        │
└──────────┴────────────────────────────────────────────────────────────┘
```

---

## Metrics & Success Criteria

### Processing Metrics
- **Batch Duration**: 1 hour (target: < 2 hours)
- **Throughput**: 8,333 candidates/minute
- **Filter Pass Rate**: 40% (200K / 500K)
- **High Score Rate**: 25% (50K / 200K with score >= 0.7)

### Delivery Metrics
- **Email Send Rate**: 50,000 per day
- **Open Rate**: 25% (12,500 opens)
- **Click-Through Rate**: 10% (5,000 clicks)
- **Conversion Rate**: 5% (2,500 reviews)

### Business Metrics
- **Review Volume Increase**: 30% (target achieved)
- **Cost per Review**: $0.02 ($50 batch cost / 2,500 reviews)
- **Customer Satisfaction**: 4.2/5.0 average rating

### Technical Metrics
- **Serving API Latency**: P99 = 18ms (target: < 30ms)
- **Cache Hit Rate**: 85%
- **Error Rate**: 0.01%
- **Availability**: 99.95%

---

## Component Configuration

### Program Configuration
```yaml
programId: "product-reviews"
programName: "E-Commerce Product Reviews"
enabled: true
marketplaces: ["US", "UK", "DE", "FR", "JP"]

dataConnectors:
  - connectorId: "athena-deliveries"
    type: "athena"
    config:
      database: "ecommerce"
      table: "deliveries"
      query: "SELECT * FROM deliveries WHERE delivery_date = ?"

scoringModels:
  - modelId: "review-propensity-v2"
    endpoint: "sagemaker://review-propensity-v2"
    features: ["engagement_score", "review_history", "trust_score"]

filterChain:
  - filterId: "trust"
    type: "trust"
    order: 1
  - filterId: "eligibility"
    type: "eligibility"
    order: 2
  - filterId: "frequency-cap"
    type: "business-rule"
    order: 3
    parameters:
      maxRequestsPerMonth: 3

channels:
  - channelId: "email"
    type: "email"
    enabled: true
    config:
      templateId: "review-request-v3"
      sendTime: "10:00"
      timezone: "local"

batchSchedule: "cron(0 2 * * ? *)"  # Daily at 2 AM UTC
candidateTTLDays: 30
```

---

## Summary

This use case demonstrates:
- ✅ **Batch processing** at scale (500K → 200K candidates)
- ✅ **Multi-stage pipeline** (ETL → Filter → Score → Store)
- ✅ **ML-powered scoring** (review propensity model)
- ✅ **Multi-channel delivery** (email with tracking)
- ✅ **High conversion** (5% review submission rate)
- ✅ **Cost efficiency** ($0.02 per review)
- ✅ **Scheduled automation** (daily batch + email delivery)
