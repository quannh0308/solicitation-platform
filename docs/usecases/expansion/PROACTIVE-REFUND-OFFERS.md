# Use Case: Proactive Refund Offers

## Overview

**Business Goal**: Maintain customer satisfaction and prevent churn by proactively offering refunds/credits for negative experiences before customers complain.

**Processing Mode**: Batch (Daily scheduled job)

**Action Type**: Goodwill Gesture

**Actors**:
- Customer (affected)
- Customer service team
- Operations team
- Data warehouse
- Email/SMS services

---

## Actor Interaction Diagram

```
┌──────────┐         ┌──────────────┐         ┌─────────────────┐
│ Customer │         │ Service Team │         │ Operations Team │
└────┬─────┘         └──────┬───────┘         └────────┬────────┘
     │                      │                          │
     │ 1. Experiences       │                          │
     │    Delivery Delay    │                          │
     ├──────────────────────┼──────────────────────────┤
     │                      │                          │
     │                      │ 2. Detects Service       │
     │                      │    Failure               │
     │                      │<─────────────────────────┤
     │                      │                          │
     │                      │ 3. Configures            │
     │                      │    Refund Program        │
     │                      ├─────────────────────────>│
     │                      │                          │
     │                      │                          │ 4. Schedules
     │                      │                          │    Daily Batch
     │                      │                          ├────────────┐
     │                      │                          │            │
     │                      │                          │<───────────┘
     │                      │                          │
     │ 5. Receives Proactive│                          │
     │    Refund Offer      │<─────────────────────────┤
     │<─────────────────────┤    (Before complaining!) │
     │                      │                          │
     │ 6. Accepts Refund    │                          │
     │    (Auto-Applied)    │                          │
     ├─────────────────────>│                          │
     │                      │                          │
     │                      │ 7. Tracks Satisfaction   │
     │                      │    Recovery              │
     │                      ├─────────────────────────>│
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

Step 1: ETL Lambda (2:00 AM - 2:20 AM)
┌──────────────────┐
│  Data Warehouse  │
│  (Athena)        │
└────────┬─────────┘
         │ Query: SELECT customer_id, order_id, delivery_date,
         │        expected_delivery_date, delay_days,
         │        order_value, item_condition, customer_complaints
         │        FROM order_tracking
         │        WHERE delivery_date = YESTERDAY
         │        AND (delay_days > 2 OR item_condition = 'damaged')
         ▼
┌─────────────────────────────────────┐
│      Athena Connector               │
│  - Extract 50,000 order records     │
│  - Filter: Delayed or damaged       │
│  - Map fields to candidate model    │
│  - Validate schema                  │
└────────┬────────────────────────────┘
         │ 50,000 raw candidates
         ▼
┌─────────────────────────────────────┐
│         ETL Lambda                  │
│  - Transform to unified model       │
│  - Add context (marketplace, tier)  │
│  - Calculate impact score           │
│  - Set event metadata               │
└────────┬────────────────────────────┘
         │ 50,000 candidates
         │ {customerId, orderId, delayDays, ...}
         ▼

Step 2: Filter Lambda (2:20 AM - 2:35 AM)
┌─────────────────────────────────────┐
│      Filter Chain Executor          │
└────────┬────────────────────────────┘
         │
         ├─> Trust Filter
         │   ├─ Verify customer authenticity
         │   ├─ Check account status: Active
         │   └─ Result: Remove 2.5K (5%)
         │
         ├─> Issue Severity Filter
         │   ├─ Check delay: >2 days
         │   ├─ Check damage: Yes/No
         │   ├─ Check customer complaints: None yet
         │   └─ Result: Remove 10K (20%)
         │
         ├─> Customer Value Filter
         │   ├─ Check lifetime value: >$200
         │   ├─ Check order frequency: >2/year
         │   └─ Result: Remove 15K (30%)
         │
         ├─> Eligibility Filter
         │   ├─ Customer hasn't received refund in 90 days
         │   ├─ Order is eligible for refund
         │   └─ Result: Remove 7.5K (15%)
         │
         ├─> Frequency Cap Filter
         │   ├─ Max 2 proactive refunds per year
         │   └─ Result: Remove 5K (10%)
         │
         ▼
    10,000 eligible candidates
         │
         ▼

Step 3: Score Lambda (2:35 AM - 2:50 AM)
┌─────────────────────────────────────┐
│     Feature Store Client            │
│  - Retrieve customer features       │
│    • lifetime_value: $1,200         │
│    • satisfaction_history: 4.5/5.0  │
│    • complaint_history: 0           │
│    • churn_risk_baseline: 0.15      │
│  - Retrieve order features          │
│    • delay_days: 5                  │
│    • order_value: $85               │
│    • item_condition: "good"         │
│    • delivery_priority: "standard"  │
└────────┬────────────────────────────┘
         │ Features for 10,000 candidates
         ▼
┌─────────────────────────────────────┐
│    SageMaker Scoring Provider       │
│  - Call satisfaction impact model   │
│  - Call churn risk model            │
│  - Batch scoring (1000 at a time)   │
│  - Satisfaction impact: 0.0 - 1.0   │
│  - Refund amount recommendation     │
└────────┬────────────────────────────┘
         │ Scores computed
         ▼
┌─────────────────────────────────────┐
│       Score Cache Repository        │
│  - Cache scores in DynamoDB         │
│  - TTL: 30 days                     │
└────────┬────────────────────────────┘
         │ 10,000 scored candidates
         │ Score distribution:
         │   High impact (0.7-1.0): 3,000
         │   Medium impact (0.4-0.7): 5,000
         │   Low impact (0.0-0.4): 2,000
         │
         │ Refund recommendations:
         │   Full refund: 1,000
         │   Partial refund: 4,000
         │   Credit: 5,000
         ▼

Step 4: Store Lambda (2:50 AM - 3:00 AM)
┌─────────────────────────────────────┐
│   DynamoDB Candidate Repository     │
│  - Batch write (25 items per batch) │
│  - Primary key: CustomerId:Program  │
│  - GSI1: Program:ImpactLevel:Score  │
│  - GSI2: Program:RefundType:Date    │
│  - TTL: 30 days from now            │
└────────┬────────────────────────────┘
         │ 10,000 candidates stored
         ▼
┌─────────────────────────────────────┐
│         DynamoDB Tables             │
│  ┌─────────────────────────────┐   │
│  │ Candidates Table            │   │
│  │ - 10,000 new items          │   │
│  │ - Partitioned by customerId │   │
│  │ - Indexed by impact level   │   │
│  │ - Indexed by refund type    │   │
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘

Batch Job Complete: 3:00 AM
Total Time: 1 hour
Throughput: 13,889 candidates/minute
```

---

## Action Delivery Flow (Proactive Refund Campaign)

### Multi-Channel Delivery - 10:00 AM

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      ACTION DELIVERY FLOW                                │
│                   (Proactive Refund/Credit Offers)                       │
└─────────────────────────────────────────────────────────────────────────┘

Segment 1: High Impact (Full Refund) - 3,000 Customers
┌─────────────────────────────────────┐
│      Serving API Query              │
│  Query: GetCandidatesByImpact       │
│  Params:                            │
│    - program: "proactive-refunds"   │
│    - impactLevel: "high"            │
│    - refundType: "full"             │
└────────┬────────────────────────────┘
         │ 1,000 customers (severe delays/damage)
         ▼
┌─────────────────────────────────────┐
│   Email + SMS Channel               │
│  - Email with apology + refund      │
│  - SMS confirmation                 │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│      Email Campaign Service         │
│  Subject: "We're sorry - Full       │
│           refund issued"            │
│  Body:                              │
│  - "We sincerely apologize for the  │
│     delay/damage with your order"   │
│  - "We've issued a full refund of   │
│     $85 to your original payment"   │
│  - "No action needed - automatic"   │
│  - "We hope to serve you better"    │
│  - "Here's $20 credit for next      │
│     purchase: CODE20"               │
└────────┬────────────────────────────┘
         │ 1,000 emails sent
         │
         ├─> Refund Processing System
         │   └─ Auto-process 1,000 refunds
         │       Total: $85,000
         │
         ▼
┌─────────────────────────────────────┐
│      Customer Receives Email        │
│  - 900 opens (90%)                  │
│  - Refund already processed         │
│  - $20 credit available             │
│  - 400 use credit (40%)             │
└─────────────────────────────────────┘

Segment 1 Results:
- Refunds issued: 1,000
- Total refund amount: $85,000
- Credits used: 400 (40%)
- Satisfaction recovery: 95%

---

Segment 2: Medium Impact (Partial Refund) - 4,000 Customers
┌─────────────────────────────────────┐
│      Serving API Query              │
│  Query: GetCandidatesByImpact       │
│  Params:                            │
│    - program: "proactive-refunds"   │
│    - impactLevel: "medium"          │
│    - refundType: "partial"          │
└────────┬────────────────────────────┘
         │ 4,000 customers (moderate delays)
         ▼
┌─────────────────────────────────────┐
│      Email Channel                  │
│  Subject: "Apology + $20 credit for │
│           your delayed order"       │
│  Body:                              │
│  - "We apologize for the delay"     │
│  - "We've added $20 credit to your  │
│     account - no action needed"     │
│  - "Use on your next purchase"      │
│  - "Thank you for your patience"    │
└────────┬────────────────────────────┘
         │ 4,000 emails sent
         │
         ├─> Credit Processing System
         │   └─ Auto-apply 4,000 credits
         │       Total: $80,000
         │
         ▼
┌─────────────────────────────────────┐
│      Customer Receives Email        │
│  - 3,200 opens (80%)                │
│  - Credit already applied           │
│  - 2,400 use credit (60%)           │
└─────────────────────────────────────┘

Segment 2 Results:
- Credits issued: 4,000
- Total credit amount: $80,000
- Credits used: 2,400 (60%)
- Satisfaction recovery: 85%

---

Segment 3: Low Impact (Store Credit) - 5,000 Customers
┌─────────────────────────────────────┐
│      Serving API Query              │
│  Query: GetCandidatesByImpact       │
│  Params:                            │
│    - program: "proactive-refunds"   │
│    - impactLevel: "low"             │
│    - refundType: "credit"           │
└────────┬────────────────────────────┘
         │ 5,000 customers (minor delays)
         ▼
┌─────────────────────────────────────┐
│      Email Channel                  │
│  Subject: "Thank you for your       │
│           patience - $10 credit"    │
│  Body:                              │
│  - "We noticed a slight delay"      │
│  - "Here's $10 credit as a thank    │
│     you for your patience"          │
│  - "Use on your next purchase"      │
│  - Discount code: THANKS10          │
└────────┬────────────────────────────┘
         │ 5,000 emails sent
         │
         ├─> Credit Processing System
         │   └─ Auto-apply 5,000 credits
         │       Total: $50,000
         │
         ▼
┌─────────────────────────────────────┐
│      Customer Receives Email        │
│  - 3,500 opens (70%)                │
│  - Credit already applied           │
│  - 2,000 use credit (40%)           │
└─────────────────────────────────────┘

Segment 3 Results:
- Credits issued: 5,000
- Total credit amount: $50,000
- Credits used: 2,000 (40%)
- Satisfaction recovery: 75%

---

Overall Campaign Results:
- Total Customers: 10,000
- Total Refunds/Credits: $215,000
- Credits Redeemed: 4,800 (48%)
- Satisfaction Recovery: 85% average
- Churn Prevention: 70% (vs 30% baseline)
- Net Cost: $85,000 (after redemptions)
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
│          │ - Process yesterday's delivery issues                      │
│          │ - Duration: 1 hour                                         │
│          │ - Output: 10,000 candidates                                │
├──────────┼────────────────────────────────────────────────────────────┤
│ 3:00 AM  │ Refund Processing                                          │
│          │ - Auto-process approved refunds                            │
│          │ - Apply credits to accounts                                │
│          │ - Duration: 30 minutes                                     │
├──────────┼────────────────────────────────────────────────────────────┤
│ 3:30 AM  │ Data Warehouse Export                                      │
│          │ - Export candidates to S3 (Parquet)                        │
│          │ - Update Glue catalog                                      │
│          │ - Duration: 15 minutes                                     │
├──────────┼────────────────────────────────────────────────────────────┤
│ 10:00 AM │ Email Campaign Delivery                                    │
│          │ - Send refund/credit notifications                         │
│          │ - Send 10,000 emails                                       │
│          │ - Duration: 30 minutes                                     │
├──────────┼────────────────────────────────────────────────────────────┤
│ 6:00 PM  │ Satisfaction Recovery Analysis                             │
│          │ - Track customer responses                                 │
│          │ - Calculate satisfaction scores                            │
│          │ - Duration: 20 minutes                                     │
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
│ 3:00 AM  │ - Retrain satisfaction impact model                        │
│          │ - Evaluate model performance                               │
│          │ - Deploy if improved                                       │
│          │ - Duration: 2 hours                                        │
└──────────┴────────────────────────────────────────────────────────────┘
```

---

## Metrics & Success Criteria

### Processing Metrics
- **Batch Duration**: 1 hour (target: < 2 hours)
- **Throughput**: 13,889 candidates/minute
- **Filter Pass Rate**: 20% (10K / 50K)
- **High Impact Rate**: 30% (3K / 10K)

### Delivery Metrics
- **Email Send Rate**: 10,000 per day
- **Email Open Rate**: 80% (high urgency)
- **SMS Delivery Rate**: 98% (for high impact)

### Satisfaction Metrics
- **Satisfaction Recovery Rate**: 85% average
  - High impact: 95%
  - Medium impact: 85%
  - Low impact: 75%
- **Churn Prevention**: 70% (vs 30% baseline)
- **Credit Redemption Rate**: 48%

### Business Metrics
- **Total Refunds/Credits Issued**: $215,000 per day
- **Credits Redeemed**: $103,200 (48%)
- **Net Cost**: $85,000 per day (after unredeemed credits)
- **Churn Prevented**: 7,000 customers (70% of 10K)
- **Revenue Saved**: $2.8M (7,000 × $400 LTV)
- **ROI**: 3,294% ($2.8M / $85K)

### Technical Metrics
- **Serving API Latency**: P99 = 18ms (target: < 30ms)
- **Cache Hit Rate**: 85%
- **Error Rate**: 0.01%
- **Availability**: 99.96%

---

## Component Configuration

### Program Configuration
```yaml
programId: "proactive-refunds"
programName: "Proactive Refund Offers"
enabled: true
marketplaces: ["US", "UK", "DE", "FR", "JP", "IN", "BR", "AU"]

dataConnectors:
  - connectorId: "athena-order-tracking"
    type: "athena"
    config:
      database: "ecommerce"
      table: "order_tracking"
      query: |
        SELECT customer_id, order_id, delivery_date,
               expected_delivery_date, delay_days,
               order_value, item_condition, customer_complaints
        FROM order_tracking
        WHERE delivery_date = ?
        AND (delay_days > 2 OR item_condition = 'damaged')

scoringModels:
  - modelId: "satisfaction-impact-v2"
    endpoint: "sagemaker://satisfaction-impact-v2"
    features: ["delay_days", "order_value", "lifetime_value", "satisfaction_history"]
  - modelId: "churn-risk-v3"
    endpoint: "sagemaker://churn-risk-v3"
    features: ["complaint_history", "satisfaction_history", "order_frequency"]

filterChain:
  - filterId: "trust"
    type: "trust"
    order: 1
  - filterId: "issue-severity"
    type: "business-rule"
    order: 2
    parameters:
      minDelayDays: 2
      includeDamaged: true
      excludeExistingComplaints: true
  - filterId: "customer-value"
    type: "business-rule"
    order: 3
    parameters:
      minLifetimeValue: 200
      minOrderFrequency: 2  # per year
  - filterId: "eligibility"
    type: "eligibility"
    order: 4
  - filterId: "frequency-cap"
    type: "business-rule"
    order: 5
    parameters:
      maxRefundsPerYear: 2

channels:
  - channelId: "email"
    type: "email"
    enabled: true
    priority: 1
    config:
      templateId: "proactive-refund-v2"
      sendTime: "10:00"
      timezone: "local"
      personalization: "high"
  - channelId: "sms"
    type: "sms"
    enabled: true
    priority: 2
    config:
      message: "Hi {customerName}, we've issued a {refundType} for your delayed order. Check your email for details."
      highImpactOnly: true

refundStrategy:
  enabled: true
  autoProcess: true
  segments:
    - impactLevel: "high"
      refundType: "full"
      additionalCredit: 20
      requireApproval: false
    - impactLevel: "medium"
      refundType: "partial"
      refundAmount: 20
      requireApproval: false
    - impactLevel: "low"
      refundType: "credit"
      creditAmount: 10
      requireApproval: false

batchSchedule: "cron(0 2 * * ? *)"  # Daily at 2 AM UTC
candidateTTLDays: 30
```

---

## Summary

This use case demonstrates:
- ✅ **Batch processing** at scale (50K → 10K candidates)
- ✅ **Multi-stage pipeline** (ETL → Filter → Score → Store)
- ✅ **Dual ML models** (satisfaction impact + churn risk)
- ✅ **Segmented refunds** (full, partial, credit)
- ✅ **Automated processing** (no manual approval needed)
- ✅ **High satisfaction recovery** (85% average)
- ✅ **Massive churn prevention** (70% vs 30% baseline)
- ✅ **Excellent ROI** (3,294% return on investment)
- ✅ **Revenue saved** ($2.8M annually)
- ✅ **Proactive goodwill** (reach out before complaints)

**Key Insight**: This is proactive customer care through automated compensation. The platform detects negative experiences and delivers goodwill gestures before customers complain!
