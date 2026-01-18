# Use Case: Win-Back Campaigns

## Overview

**Business Goal**: Re-engage churned customers and recover $800K+ annually in lost revenue through personalized win-back offers.

**Processing Mode**: Batch (Monthly scheduled job)

**Action Type**: Win-Back Offer (not solicitation)

**Actors**:
- Churned customer
- Customer success team
- Marketing team
- Data warehouse
- Email/SMS services

---

## Actor Interaction Diagram

```
┌──────────┐         ┌──────────────┐         ┌─────────────────┐
│ Customer │         │ Success Team │         │ Marketing Team  │
└────┬─────┘         └──────┬───────┘         └────────┬────────┘
     │                      │                          │
     │ 1. Churned 30-90     │                          │
     │    Days Ago          │                          │
     ├──────────────────────┼──────────────────────────┤
     │                      │                          │
     │                      │ 2. Analyzes Churn        │
     │                      │    Patterns              │
     │                      │<─────────────────────────┤
     │                      │                          │
     │                      │ 3. Configures            │
     │                      │    Win-Back Program      │
     │                      ├─────────────────────────>│
     │                      │                          │
     │                      │                          │ 4. Schedules
     │                      │                          │    Monthly Batch
     │                      │                          ├────────────┐
     │                      │                          │            │
     │                      │                          │<───────────┘
     │                      │                          │
     │ 5. Receives Win-Back │                          │
     │    Offer (50% off)   │<─────────────────────────┤
     │<─────────────────────┤                          │
     │                      │                          │
     │ 6. Accepts Offer     │                          │
     │    Returns!          │                          │
     ├─────────────────────>│                          │
     │                      │                          │
     │                      │ 7. Tracks Win-Back       │
     │                      │    Success               │
     │                      ├─────────────────────────>│
     │                      │                          │
     ▼                      ▼                          ▼
```

---

## Data Ingestion Flow (Batch Processing)

### Monthly Batch Job - 1st Monday 2:00 AM

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         DATA INGESTION FLOW                              │
│                  (Batch - Monthly on 1st Monday 2:00 AM)                 │
└─────────────────────────────────────────────────────────────────────────┘

Step 1: ETL Lambda (2:00 AM - 2:30 AM)
┌──────────────────┐
│  Data Warehouse  │
│  (Snowflake)     │
└────────┬─────────┘
         │ Query: SELECT customer_id, churn_date, churn_reason,
         │        lifetime_value, last_purchase_date,
         │        subscription_tier, engagement_score,
         │        win_back_attempts
         │        FROM churned_customers
         │        WHERE churn_date BETWEEN CURRENT_DATE - 90 AND CURRENT_DATE - 30
         │        AND win_back_attempts < 2
         ▼
┌─────────────────────────────────────┐
│    Snowflake Connector              │
│  - Extract 100K churned customers   │
│  - Filter: Churned 30-90 days ago   │
│  - Filter: <2 win-back attempts     │
│  - Map fields to candidate model    │
└────────┬────────────────────────────┘
         │ 100K raw candidates
         ▼
┌─────────────────────────────────────┐
│         ETL Lambda                  │
│  - Transform to unified model       │
│  - Add context (churn reason, tier) │
│  - Calculate win-back potential     │
│  - Set event metadata               │
└────────┬────────────────────────────┘
         │ 100K candidates
         │ {customerId, churnDate, churnReason, ...}
         ▼

Step 2: Filter Lambda (2:30 AM - 2:50 AM)
┌─────────────────────────────────────┐
│      Filter Chain Executor          │
└────────┬────────────────────────────┘
         │
         ├─> Trust Filter
         │   ├─ Verify customer authenticity
         │   ├─ Check account status: Churned
         │   └─ Result: Remove 5K (5%)
         │
         ├─> Churn Timing Filter
         │   ├─ Check days since churn: 30-90
         │   ├─ Too recent (<30): Not ready
         │   ├─ Too old (>90): Less likely
         │   └─ Result: Remove 20K (20%)
         │
         ├─> Customer Value Filter
         │   ├─ Check lifetime value: >$500
         │   ├─ Check subscription tier: Premium+
         │   └─ Result: Remove 40K (40%)
         │
         ├─> Churn Reason Filter
         │   ├─ Addressable reasons: price, features, competition
         │   ├─ Non-addressable: moved, deceased
         │   └─ Result: Remove 15K (15%)
         │
         ├─> Frequency Cap Filter
         │   ├─ Max 2 win-back attempts per customer
         │   └─ Result: Remove 5K (5%)
         │
         ▼
    15,000 eligible candidates
         │
         ▼

Step 3: Score Lambda (2:50 AM - 3:15 AM)
┌─────────────────────────────────────┐
│     Feature Store Client            │
│  - Retrieve customer features       │
│    • lifetime_value                 │
│    • engagement_history             │
│    • churn_reason                   │
│    • subscription_tenure            │
│    • competitor_activity            │
│  - Retrieve offer features          │
│    • discount_sensitivity           │
│    • feature_requests               │
│    • price_comparison               │
└────────┬────────────────────────────┘
         │ Features for 15,000 candidates
         ▼
┌─────────────────────────────────────┐
│    SageMaker Scoring Provider       │
│  - Call win-back likelihood model   │
│  - Call offer effectiveness model   │
│  - Batch scoring (1000 at a time)   │
│  - Win-back likelihood: 0.0 - 1.0   │
│  - Optimal offer recommendation     │
└────────┬────────────────────────────┘
         │ Scores computed
         ▼
┌─────────────────────────────────────┐
│       Score Cache Repository        │
│  - Cache scores in DynamoDB         │
│  - TTL: 90 days                     │
└────────┬────────────────────────────┘
         │ 15,000 scored candidates
         │ Score distribution:
         │   High (0.7-1.0): 3,000
         │   Medium (0.4-0.7): 8,000
         │   Low (0.0-0.4): 4,000
         │
         │ Offer recommendations:
         │   50% discount: 3,000
         │   30% discount: 8,000
         │   20% discount: 4,000
         ▼

Step 4: Store Lambda (3:15 AM - 3:30 AM)
┌─────────────────────────────────────┐
│   DynamoDB Candidate Repository     │
│  - Batch write (25 items per batch) │
│  - Primary key: CustomerId:Program  │
│  - GSI1: Program:WinBackScore:Date  │
│  - GSI2: Program:ChurnReason:Date   │
│  - TTL: 90 days from now            │
└────────┬────────────────────────────┘
         │ 15,000 candidates stored
         ▼
┌─────────────────────────────────────┐
│         DynamoDB Tables             │
│  ┌─────────────────────────────┐   │
│  │ Candidates Table            │   │
│  │ - 15,000 new items          │   │
│  │ - Partitioned by customerId │   │
│  │ - Indexed by win-back score │   │
│  │ - Indexed by churn reason   │   │
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘

Batch Job Complete: 3:30 AM
Total Time: 1 hour 30 minutes
Throughput: 11,111 candidates/minute
```

---

## Action Delivery Flow (Win-Back Campaign)

### Multi-Channel Win-Back - Monday 10:00 AM

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      ACTION DELIVERY FLOW                                │
│                   (Personalized Win-Back Campaign)                       │
└─────────────────────────────────────────────────────────────────────────┘

Segment 1: High Win-Back Likelihood (50% Discount) - 3,000 Customers
┌─────────────────────────────────────┐
│      Serving API Query              │
│  Query: GetCandidatesByScore        │
│  Params:                            │
│    - program: "win-back"            │
│    - minScore: 0.7                  │
│    - offerType: "premium"           │
└────────┬────────────────────────────┘
         │ 3,000 high-likelihood customers
         ▼
┌─────────────────────────────────────┐
│   Email + Personal Call Campaign    │
│  - Email with premium offer         │
│  - Personal call from account mgr   │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│      Email Campaign Service         │
│  Subject: "We want you back -       │
│           50% off for 6 months"     │
│  Body:                              │
│  - "We miss you, [Name]!"           │
│  - "We've made improvements based   │
│     on your feedback"               │
│  - Personalized offer:              │
│    • 50% off for 6 months           │
│    • Free upgrade to Premium+       │
│    • Dedicated account manager      │
│    • No commitment required         │
│  - "Welcome back: [Redeem Now]"     │
└────────┬────────────────────────────┘
         │ 3,000 emails sent
         │ 1,500 opens (50%)
         │ 900 clicks (30%)
         ▼
┌─────────────────────────────────────┐
│      Personal Call Campaign         │
│  - Account manager assigned         │
│  - Calls made within 48 hours       │
│  - Personalized conversation        │
│  - Address churn reason             │
└────────┬────────────────────────────┘
         │ 2,000 calls made (67%)
         │ 1,600 answered (80%)
         │ 960 accepted offer (60%)
         ▼

Segment 1 Results:
- Email conversion: 300 (10%)
- Call conversion: 960 (32%)
- Total win-back: 1,260 (42%)

---

Segment 2: Medium Win-Back Likelihood (30% Discount) - 8,000 Customers
┌─────────────────────────────────────┐
│      Serving API Query              │
│  Query: GetCandidatesByScore        │
│  Params:                            │
│    - program: "win-back"            │
│    - minScore: 0.4                  │
│    - offerType: "standard"          │
└────────┬────────────────────────────┘
         │ 8,000 medium-likelihood customers
         ▼
┌─────────────────────────────────────┐
│   Email + SMS Campaign              │
│  - Email with standard offer        │
│  - SMS reminder after 3 days        │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│      Email Campaign Service         │
│  Subject: "Come back - 30% off for  │
│           3 months"                 │
│  Body:                              │
│  - "We'd love to have you back"     │
│  - Offer: 30% off for 3 months      │
│  - "What we've improved:"           │
│    • New features you requested     │
│    • Better pricing                 │
│    • Improved support               │
│  - "Redeem now: [Link]"             │
└────────┬────────────────────────────┘
         │ 8,000 emails sent
         │ 3,200 opens (40%)
         │ 1,600 clicks (20%)
         │ 800 conversions (10%)
         ▼

Segment 2 Results:
- Email conversion: 800 (10%)
- SMS conversion: 400 (5%)
- Total win-back: 1,200 (15%)

---

Segment 3: Low Win-Back Likelihood (20% Discount) - 4,000 Customers
┌─────────────────────────────────────┐
│      Serving API Query              │
│  Query: GetCandidatesByScore        │
│  Params:                            │
│    - program: "win-back"            │
│    - minScore: 0.0                  │
│    - offerType: "basic"             │
└────────┬────────────────────────────┘
         │ 4,000 low-likelihood customers
         ▼
┌─────────────────────────────────────┐
│      Email Campaign                 │
│  Subject: "We miss you - 20% off"   │
│  Body:                              │
│  - "Come back and save 20%"         │
│  - Simple offer, no pressure        │
│  - "Redeem anytime: [Link]"         │
└────────┬────────────────────────────┘
         │ 4,000 emails sent
         │ 1,200 opens (30%)
         │ 400 clicks (10%)
         │ 120 conversions (3%)
         ▼

Segment 3 Results:
- Email conversion: 120 (3%)
- Total win-back: 120 (3%)

---

Overall Campaign Results:
- Total Churned Customers: 15,000
- Total Win-Back: 2,580 (17.2%)
- Revenue Recovered: $1.03M (2,580 × $400 LTV)
- Campaign Cost: $45K
- ROI: 2,289%
```

---

## Scheduled Jobs

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           SCHEDULED JOBS                                 │
└─────────────────────────────────────────────────────────────────────────┘

Monthly Schedule:
┌──────────┬────────────────────────────────────────────────────────────┐
│   Time   │                        Job                                 │
├──────────┼────────────────────────────────────────────────────────────┤
│ 1st Mon  │ Batch Ingestion Workflow (ETL → Filter → Score → Store)   │
│ 2:00 AM  │ - Process churned customers from last 60 days              │
│          │ - Duration: 1 hour 30 minutes                              │
│          │ - Output: 15,000 candidates                                │
├──────────┼────────────────────────────────────────────────────────────┤
│ 1st Mon  │ Data Warehouse Export                                      │
│ 3:45 AM  │ - Export candidates to S3 (Parquet)                        │
│          │ - Update Glue catalog                                      │
│          │ - Duration: 20 minutes                                     │
├──────────┼────────────────────────────────────────────────────────────┤
│ 1st Mon  │ Email Campaign Delivery                                    │
│ 10:00 AM │ - Send segmented win-back emails                           │
│          │ - Send 15,000 emails                                       │
│          │ - Duration: 1 hour                                         │
├──────────┼────────────────────────────────────────────────────────────┤
│ 1st Thu  │ SMS Reminder Campaign                                      │
│ 10:00 AM │ - Send SMS to non-converters                               │
│          │ - Duration: 30 minutes                                     │
└──────────┴────────────────────────────────────────────────────────────┘

Daily Schedule:
┌──────────┬────────────────────────────────────────────────────────────┐
│   Time   │                        Job                                 │
├──────────┼────────────────────────────────────────────────────────────┤
│ 6:00 AM  │ Win-Back Metrics Dashboard                                 │
│          │ - Track conversion rates                                   │
│          │ - Calculate revenue recovered                              │
│          │ - Generate executive report                                │
│          │ - Duration: 15 minutes                                     │
├──────────┼────────────────────────────────────────────────────────────┤
│ 11:59 PM │ Metrics Aggregation                                        │
│          │ - Aggregate daily metrics                                  │
│          │ - Publish to CloudWatch                                    │
│          │ - Generate daily report                                    │
│          │ - Duration: 5 minutes                                      │
└──────────┴────────────────────────────────────────────────────────────┘

Quarterly Schedule:
┌──────────┬────────────────────────────────────────────────────────────┐
│   Time   │                        Job                                 │
├──────────┼────────────────────────────────────────────────────────────┤
│ 1st Mon  │ Model Retraining                                           │
│ 3:00 AM  │ - Retrain win-back likelihood model                        │
│          │ - Retrain offer effectiveness model                        │
│          │ - Evaluate model performance                               │
│          │ - Deploy if improved                                       │
│          │ - Duration: 4 hours                                        │
└──────────┴────────────────────────────────────────────────────────────┘
```

---

## Metrics & Success Criteria

### Processing Metrics
- **Batch Duration**: 1 hour 30 minutes (target: < 2 hours)
- **Throughput**: 11,111 candidates/minute
- **Filter Pass Rate**: 15% (15K / 100K)
- **High Score Rate**: 20% (3K / 15K with score >= 0.7)

### Delivery Metrics
- **Email Send Rate**: 15,000 per month
- **Email Open Rate**: 40%
- **Email Click-Through Rate**: 20%
- **SMS Send Rate**: 8,000 per month (non-converters)
- **SMS Click Rate**: 30%
- **Call Connection Rate**: 80%

### Win-Back Metrics
- **High Likelihood Win-Back**: 42% (1,260 / 3,000)
- **Medium Likelihood Win-Back**: 15% (1,200 / 8,000)
- **Low Likelihood Win-Back**: 3% (120 / 4,000)
- **Overall Win-Back Rate**: 17.2% (2,580 / 15,000)
- **30-Day Retention**: 85% (of win-backs)

### Business Metrics
- **Revenue Recovered**: $1.03M per month
- **Annual Revenue**: $12.4M
- **Campaign Cost**: $45K per month
- **Annual Cost**: $540K
- **ROI**: 2,289%
- **Cost per Win-Back**: $17.44
- **Customer Lifetime Value**: $400 average

### Technical Metrics
- **Serving API Latency**: P99 = 22ms (target: < 30ms)
- **Cache Hit Rate**: 82%
- **Error Rate**: 0.015%
- **Availability**: 99.94%

---

## Component Configuration

### Program Configuration
```yaml
programId: "win-back-campaigns"
programName: "Win-Back Campaigns"
enabled: true
marketplaces: ["US", "UK", "DE", "FR", "JP", "IN", "BR", "AU"]

dataConnectors:
  - connectorId: "snowflake-churned-customers"
    type: "snowflake"
    config:
      account: "company.us-east-1"
      database: "customer_data"
      schema: "churn"
      query: |
        SELECT customer_id, churn_date, churn_reason,
               lifetime_value, last_purchase_date,
               subscription_tier, engagement_score,
               win_back_attempts
        FROM churned_customers
        WHERE churn_date BETWEEN CURRENT_DATE - 90 AND CURRENT_DATE - 30
        AND win_back_attempts < 2

scoringModels:
  - modelId: "win-back-likelihood-v3"
    endpoint: "sagemaker://win-back-likelihood-v3"
    features: ["lifetime_value", "churn_reason", "subscription_tenure", "engagement_history"]
  - modelId: "offer-effectiveness-v2"
    endpoint: "sagemaker://offer-effectiveness-v2"
    features: ["discount_sensitivity", "feature_requests", "competitor_activity"]

filterChain:
  - filterId: "trust"
    type: "trust"
    order: 1
  - filterId: "churn-timing"
    type: "business-rule"
    order: 2
    parameters:
      minDaysSinceChurn: 30
      maxDaysSinceChurn: 90
  - filterId: "customer-value"
    type: "business-rule"
    order: 3
    parameters:
      minLifetimeValue: 500
      minSubscriptionTier: "premium"
  - filterId: "churn-reason"
    type: "business-rule"
    order: 4
    parameters:
      addressableReasons: ["price", "features", "competition", "support"]
      excludeReasons: ["moved", "deceased", "duplicate_account"]
  - filterId: "frequency-cap"
    type: "business-rule"
    order: 5
    parameters:
      maxWinBackAttempts: 2

channels:
  - channelId: "email"
    type: "email"
    enabled: true
    priority: 1
    config:
      templateId: "win-back-v4"
      sendTime: "10:00"
      timezone: "local"
      personalization: "high"
  - channelId: "phone"
    type: "phone"
    enabled: true
    priority: 1
    config:
      callType: "account-manager"
      scheduleWithin: "48h"
      scriptId: "win-back-call-v2"
      highScoreOnly: true
  - channelId: "sms"
    type: "sms"
    enabled: true
    priority: 2
    config:
      delayDays: 3
      message: "Hi {customerName}! We miss you. Come back and save {discountPercent}%. Use code {discountCode}: {link}"
      onlyIfNotConverted: true

offerStrategy:
  enabled: true
  segmentedOffers: true
  segments:
    - scoreRange: [0.7, 1.0]
      offerType: "premium"
      discount: 50
      duration: 6  # months
      includeUpgrade: true
      includePersonalCall: true
    - scoreRange: [0.4, 0.7]
      offerType: "standard"
      discount: 30
      duration: 3  # months
    - scoreRange: [0.0, 0.4]
      offerType: "basic"
      discount: 20
      duration: 1  # month

batchSchedule: "cron(0 2 ? * 1#1 *)"  # 1st Monday of month at 2 AM UTC
candidateTTLDays: 90
```

---

## Summary

This use case demonstrates:
- ✅ **Batch processing** at scale (100K → 15K candidates)
- ✅ **Multi-stage pipeline** (ETL → Filter → Score → Store)
- ✅ **Dual ML models** (win-back likelihood + offer effectiveness)
- ✅ **Segmented campaigns** (high, medium, low likelihood)
- ✅ **Multi-channel delivery** (email + phone + SMS)
- ✅ **High win-back rate** (17.2% overall, 42% for high-likelihood)
- ✅ **Massive revenue recovery** ($12.4M annually)
- ✅ **Excellent ROI** (2,289% return on investment)
- ✅ **Personalized offers** (50% discount for high-likelihood)
- ✅ **Human touch** (personal calls for high-value customers)

**Key Insight**: This is NOT solicitation—it's revenue recovery through personalized win-back offers. The platform identifies churned customers and delivers targeted incentives to bring them back!
