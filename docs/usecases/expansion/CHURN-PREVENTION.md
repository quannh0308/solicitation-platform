# Use Case: Churn Prevention

## Overview

**Business Goal**: Reduce customer churn by 30% through predictive modeling and personalized retention offers, saving $2M+ annually.

**Processing Mode**: Batch (Weekly scheduled job)

**Action Type**: Retention Offer (not solicitation)

**Actors**:
- Customer (at-risk)
- Customer success team
- Retention team
- Data warehouse
- Multi-channel delivery services

---

## Actor Interaction Diagram

```
┌──────────┐         ┌──────────────┐         ┌─────────────────┐
│ Customer │         │ Success Team │         │ Retention Team  │
└────┬─────┘         └──────┬───────┘         └────────┬────────┘
     │                      │                          │
     │ 1. Shows Declining   │                          │
     │    Engagement        │                          │
     ├──────────────────────┼──────────────────────────┤
     │                      │                          │
     │                      │ 2. Monitors Churn        │
     │                      │    Signals               │
     │                      │<─────────────────────────┤
     │                      │                          │
     │                      │ 3. Configures            │
     │                      │    Retention Program     │
     │                      ├─────────────────────────>│
     │                      │                          │
     │                      │                          │ 4. Schedules
     │                      │                          │    Weekly Batch
     │                      │                          ├────────────┐
     │                      │                          │            │
     │                      │                          │<───────────┘
     │                      │                          │
     │ 5. Receives          │                          │
     │    Personalized Offer│<─────────────────────────┤
     │<─────────────────────┤    (Email + Call)        │
     │                      │                          │
     │ 6. Accepts Offer     │                          │
     │    (Stays)           │                          │
     ├─────────────────────>│                          │
     │                      │                          │
     │                      │ 7. Tracks Retention      │
     │                      │    Success               │
     │                      ├─────────────────────────>│
     │                      │                          │
     ▼                      ▼                          ▼
```

---

## Data Ingestion Flow (Batch Processing)

### Weekly Batch Job - Monday 2:00 AM

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         DATA INGESTION FLOW                              │
│                    (Batch - Weekly on Monday 2:00 AM)                    │
└─────────────────────────────────────────────────────────────────────────┘

Step 1: ETL Lambda (2:00 AM - 2:45 AM)
┌──────────────────┐
│  Data Warehouse  │
│  (Snowflake)     │
└────────┬─────────┘
         │ Query: SELECT customer_id, engagement_score,
         │        login_frequency, purchase_frequency,
         │        support_tickets, feature_usage,
         │        subscription_status, lifetime_value
         │        FROM customer_analytics
         │        WHERE engagement_trend = 'declining'
         │        OR login_frequency < historical_avg * 0.5
         │        OR days_since_last_purchase > 60
         ▼
┌─────────────────────────────────────┐
│    Snowflake Connector              │
│  - Extract 500K customer records    │
│  - Filter by churn signals          │
│  - Map fields to candidate model    │
│  - Validate schema                  │
└────────┬────────────────────────────┘
         │ 500K raw candidates
         ▼
┌─────────────────────────────────────┐
│         ETL Lambda                  │
│  - Transform to unified model       │
│  - Add context (segment, tier)      │
│  - Calculate churn indicators       │
│  - Set event metadata               │
└────────┬────────────────────────────┘
         │ 500K candidates with churn signals
         │ {customerId, engagement, churnSignals, ...}
         ▼

Step 2: Filter Lambda (2:45 AM - 3:15 AM)
┌─────────────────────────────────────┐
│      Filter Chain Executor          │
└────────┬────────────────────────────┘
         │
         ├─> Trust Filter
         │   ├─ Verify customer authenticity
         │   ├─ Check account status: Active
         │   └─ Result: Remove 25K (5%)
         │
         ├─> Churn Signal Filter
         │   ├─ Check engagement decline: >30%
         │   ├─ Check login frequency: <50% of avg
         │   ├─ Check purchase recency: >60 days
         │   ├─ Minimum 2 signals required
         │   └─ Result: Remove 200K (40%)
         │
         ├─> Customer Value Filter
         │   ├─ Check lifetime value: >$500
         │   ├─ Check subscription tier: Premium+
         │   └─ Result: Remove 150K (30%)
         │
         ├─> Eligibility Filter
         │   ├─ Customer hasn't received retention offer in 90 days
         │   ├─ Customer hasn't opted out
         │   └─ Result: Remove 75K (15%)
         │
         ├─> Frequency Cap Filter
         │   ├─ Max 1 retention offer per quarter
         │   └─ Result: Remove 25K (5%)
         │
         ▼
    25,000 eligible candidates
         │
         ▼

Step 3: Score Lambda (3:15 AM - 3:45 AM)
┌─────────────────────────────────────┐
│     Feature Store Client            │
│  - Retrieve customer features       │
│    • engagement_score               │
│    • login_frequency                │
│    • purchase_frequency             │
│    • support_ticket_count           │
│    • feature_usage_score            │
│    • lifetime_value                 │
│    • subscription_tenure            │
│  - Retrieve behavioral features     │
│    • engagement_trend: -35%         │
│    • days_since_last_login: 14      │
│    • days_since_last_purchase: 75   │
│    • competitor_research: detected  │
└────────┬────────────────────────────┘
         │ Features for 25,000 candidates
         ▼
┌─────────────────────────────────────┐
│    SageMaker Scoring Provider       │
│  - Call churn risk model            │
│  - Call offer effectiveness model   │
│  - Batch scoring (1000 at a time)   │
│  - Churn risk: 0.0 - 1.0            │
│  - Offer type recommendation        │
└────────┬────────────────────────────┘
         │ Scores computed
         ▼
┌─────────────────────────────────────┐
│       Score Cache Repository        │
│  - Cache scores in DynamoDB         │
│  - TTL: 30 days                     │
└────────┬────────────────────────────┘
         │ 25,000 scored candidates
         │ Score distribution:
         │   Critical (0.8-1.0): 5,000
         │   High (0.6-0.8): 10,000
         │   Medium (0.4-0.6): 10,000
         │
         │ Offer recommendations:
         │   Discount: 12,000
         │   Upgrade: 8,000
         │   Personal call: 5,000
         ▼

Step 4: Store Lambda (3:45 AM - 4:00 AM)
┌─────────────────────────────────────┐
│   DynamoDB Candidate Repository     │
│  - Batch write (25 items per batch) │
│  - Primary key: CustomerId:Program  │
│  - GSI1: Program:RiskLevel:Score    │
│  - GSI2: Program:OfferType:Date     │
│  - TTL: 30 days from now            │
└────────┬────────────────────────────┘
         │ 25,000 candidates stored
         ▼
┌─────────────────────────────────────┐
│         DynamoDB Tables             │
│  ┌─────────────────────────────┐   │
│  │ Candidates Table            │   │
│  │ - 25,000 new items          │   │
│  │ - Partitioned by customerId │   │
│  │ - Indexed by risk level     │   │
│  │ - Indexed by offer type     │   │
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘

Batch Job Complete: 4:00 AM
Total Time: 2 hours
Throughput: 6,944 candidates/minute
```

---

## Action Delivery Flow (Retention Campaign)

### Multi-Channel Retention - Monday 10:00 AM

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      ACTION DELIVERY FLOW                                │
│                   (Personalized Retention Campaign)                      │
└─────────────────────────────────────────────────────────────────────────┘

Segment 1: Critical Risk (Churn Risk 0.8-1.0) - Personal Outreach
┌─────────────────────────────────────┐
│      Serving API Query              │
│  Query: GetCandidatesByRiskLevel    │
│  Params:                            │
│    - program: "churn-prevention"    │
│    - riskLevel: "critical"          │
│    - minScore: 0.8                  │
└────────┬────────────────────────────┘
         │ 5,000 critical risk customers
         ▼
┌─────────────────────────────────────┐
│   Multi-Channel Orchestration       │
│  - Email (immediate)                │
│  - Personal call (within 24 hours)  │
│  - In-app message                   │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│      Email Channel                  │
│  Subject: "We'd love to keep you -  │
│           Exclusive offer inside"   │
│  Body:                              │
│  - "We noticed you haven't been     │
│     using [Product] as much"        │
│  - "We value your business"         │
│  - Personalized offer:              │
│    • 50% off for 6 months           │
│    • Free upgrade to Premium+       │
│    • Dedicated account manager      │
│  - "Let's talk: [Schedule Call]"    │
└────────┬────────────────────────────┘
         │ 5,000 emails sent
         │ 2,500 opens (50%)
         │ 1,250 clicks (25%)
         ▼
┌─────────────────────────────────────┐
│      Personal Call Campaign         │
│  - Account manager assigned         │
│  - Call scheduled within 24 hours   │
│  - Call script personalized         │
│  - Offer details included           │
└────────┬────────────────────────────┘
         │ 3,000 calls made (60% of segment)
         │ 2,400 answered (80%)
         │ 1,800 accepted offer (75%)
         ▼

Segment 1 Results:
- Total: 5,000
- Email conversion: 500 (10%)
- Call conversion: 1,800 (36%)
- Total retention: 2,300 (46%)

---

Segment 2: High Risk (Churn Risk 0.6-0.8) - Automated Offers
┌─────────────────────────────────────┐
│      Serving API Query              │
│  Query: GetCandidatesByRiskLevel    │
│  Params:                            │
│    - program: "churn-prevention"    │
│    - riskLevel: "high"              │
│    - minScore: 0.6                  │
└────────┬────────────────────────────┘
         │ 10,000 high risk customers
         ▼
┌─────────────────────────────────────┐
│   Email + In-App Campaign           │
│  - Email with discount offer        │
│  - In-app banner with upgrade offer │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│      Email Channel                  │
│  Subject: "Special offer just for   │
│           you - 30% off!"           │
│  Body:                              │
│  - "We miss you!"                   │
│  - Personalized offer: 30% off      │
│  - Feature highlights               │
│  - "Redeem now: [Link]"             │
└────────┬────────────────────────────┘
         │ 10,000 emails sent
         │ 4,000 opens (40%)
         │ 2,000 clicks (20%)
         │ 1,200 conversions (12%)
         ▼

Segment 2 Results:
- Total: 10,000
- Email conversion: 1,200 (12%)
- In-app conversion: 800 (8%)
- Total retention: 2,000 (20%)

---

Segment 3: Medium Risk (Churn Risk 0.4-0.6) - Engagement Campaigns
┌─────────────────────────────────────┐
│      Serving API Query              │
│  Query: GetCandidatesByRiskLevel    │
│  Params:                            │
│    - program: "churn-prevention"    │
│    - riskLevel: "medium"            │
│    - minScore: 0.4                  │
└────────┬────────────────────────────┘
         │ 10,000 medium risk customers
         ▼
┌─────────────────────────────────────┐
│   Re-Engagement Campaign            │
│  - Email with feature tips          │
│  - In-app tutorial prompts          │
│  - Push notifications               │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│      Email Channel                  │
│  Subject: "Discover what's new in   │
│           [Product]"                │
│  Body:                              │
│  - "Here's what you're missing"     │
│  - New feature highlights           │
│  - Usage tips and tricks            │
│  - "Get started: [Link]"            │
└────────┬────────────────────────────┘
         │ 10,000 emails sent
         │ 3,500 opens (35%)
         │ 1,750 clicks (17.5%)
         │ 700 re-engaged (7%)
         ▼

Segment 3 Results:
- Total: 10,000
- Email conversion: 700 (7%)
- In-app conversion: 300 (3%)
- Total retention: 1,000 (10%)

---

Overall Campaign Results:
- Total At-Risk: 25,000
- Total Retained: 5,300 (21.2%)
- Churn Prevented: 30% (vs baseline)
- Revenue Saved: $2.1M (5,300 × $400 LTV)
- Campaign Cost: $75K
- ROI: 2,800%
```

---

## Scheduled Jobs

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           SCHEDULED JOBS                                 │
└─────────────────────────────────────────────────────────────────────────┘

Weekly Schedule:
┌──────────┬────────────────────────────────────────────────────────────┐
│   Time   │                        Job                                 │
├──────────┼────────────────────────────────────────────────────────────┤
│ Monday   │ Batch Ingestion Workflow (ETL → Filter → Score → Store)   │
│ 2:00 AM  │ - Process last week's customer activity                    │
│          │ - Duration: 2 hours                                        │
│          │ - Output: 25,000 candidates                                │
├──────────┼────────────────────────────────────────────────────────────┤
│ Monday   │ Data Warehouse Export                                      │
│ 4:30 AM  │ - Export candidates to S3 (Parquet)                        │
│          │ - Update Glue catalog                                      │
│          │ - Duration: 30 minutes                                     │
├──────────┼────────────────────────────────────────────────────────────┤
│ Monday   │ Email Campaign Delivery (Segments 2 & 3)                   │
│ 10:00 AM │ - Retrieve high & medium risk candidates                   │
│          │ - Create personalized email campaigns                      │
│          │ - Send 20,000 emails                                       │
│          │ - Duration: 1 hour                                         │
├──────────┼────────────────────────────────────────────────────────────┤
│ Monday   │ Personal Call Campaign (Segment 1)                         │
│ 9:00 AM  │ - Assign critical risk customers to account managers       │
│          │ - Schedule calls throughout the week                       │
│          │ - Duration: 30 minutes                                     │
└──────────┴────────────────────────────────────────────────────────────┘

Daily Schedule:
┌──────────┬────────────────────────────────────────────────────────────┐
│   Time   │                        Job                                 │
├──────────┼────────────────────────────────────────────────────────────┤
│ 6:00 AM  │ Retention Metrics Dashboard                                │
│          │ - Calculate retention rates                                │
│          │ - Track revenue saved                                      │
│          │ - Generate executive report                                │
│          │ - Duration: 15 minutes                                     │
├──────────┼────────────────────────────────────────────────────────────┤
│ 11:59 PM │ Metrics Aggregation                                        │
│          │ - Aggregate daily metrics                                  │
│          │ - Publish to CloudWatch                                    │
│          │ - Generate daily report                                    │
│          │ - Duration: 5 minutes                                      │
└──────────┴────────────────────────────────────────────────────────────┘

Monthly Schedule:
┌──────────┬────────────────────────────────────────────────────────────┐
│   Time   │                        Job                                 │
├──────────┼────────────────────────────────────────────────────────────┤
│ 1st Mon  │ Model Retraining                                           │
│ 3:00 AM  │ - Retrain churn risk model                                 │
│          │ - Retrain offer effectiveness model                        │
│          │ - Evaluate model performance                               │
│          │ - Deploy if improved                                       │
│          │ - Duration: 4 hours                                        │
└──────────┴────────────────────────────────────────────────────────────┘
```

---

## Metrics & Success Criteria

### Processing Metrics
- **Batch Duration**: 2 hours (target: < 3 hours)
- **Throughput**: 6,944 candidates/minute
- **Filter Pass Rate**: 5% (25K / 500K)
- **Critical Risk Rate**: 20% (5K / 25K)

### Delivery Metrics
- **Email Send Rate**: 20,000 per week
- **Email Open Rate**: 40%
- **Email Click-Through Rate**: 20%
- **Call Connection Rate**: 80%
- **In-App Display Rate**: 85%

### Retention Metrics
- **Critical Risk Retention**: 46% (2,300 / 5,000)
- **High Risk Retention**: 20% (2,000 / 10,000)
- **Medium Risk Retention**: 10% (1,000 / 10,000)
- **Overall Retention Rate**: 21.2% (5,300 / 25,000)
- **Churn Reduction**: 30% (vs baseline)

### Business Metrics
- **Revenue Saved**: $2.1M annually (5,300 × $400 LTV)
- **Campaign Cost**: $75K annually
- **ROI**: 2,800%
- **Cost per Retained Customer**: $14.15
- **Customer Lifetime Value**: $400 average

### Technical Metrics
- **Serving API Latency**: P99 = 20ms (target: < 30ms)
- **Cache Hit Rate**: 85%
- **Error Rate**: 0.012%
- **Availability**: 99.95%

---

## Component Configuration

### Program Configuration
```yaml
programId: "churn-prevention"
programName: "Churn Prevention"
enabled: true
marketplaces: ["US", "UK", "DE", "FR", "JP", "IN", "BR", "AU"]

dataConnectors:
  - connectorId: "snowflake-customer-analytics"
    type: "snowflake"
    config:
      account: "company.us-east-1"
      database: "customer_data"
      schema: "analytics"
      query: |
        SELECT customer_id, engagement_score, login_frequency,
               purchase_frequency, support_tickets, feature_usage,
               subscription_status, lifetime_value
        FROM customer_analytics
        WHERE engagement_trend = 'declining'
        OR login_frequency < historical_avg * 0.5
        OR days_since_last_purchase > 60

scoringModels:
  - modelId: "churn-risk-v6"
    endpoint: "sagemaker://churn-risk-v6"
    features: ["engagement_score", "login_frequency", "purchase_frequency", "support_tickets"]
  - modelId: "offer-effectiveness-v2"
    endpoint: "sagemaker://offer-effectiveness-v2"
    features: ["lifetime_value", "subscription_tenure", "discount_sensitivity"]

filterChain:
  - filterId: "trust"
    type: "trust"
    order: 1
  - filterId: "churn-signal"
    type: "business-rule"
    order: 2
    parameters:
      minEngagementDecline: 0.3
      maxLoginFrequencyRatio: 0.5
      minDaysSinceLastPurchase: 60
      minSignalsRequired: 2
  - filterId: "customer-value"
    type: "business-rule"
    order: 3
    parameters:
      minLifetimeValue: 500
      minSubscriptionTier: "premium"
  - filterId: "eligibility"
    type: "eligibility"
    order: 4
  - filterId: "frequency-cap"
    type: "business-rule"
    order: 5
    parameters:
      maxOffersPerQuarter: 1

channels:
  - channelId: "email"
    type: "email"
    enabled: true
    priority: 1
    config:
      templateId: "retention-offer-v4"
      sendTime: "10:00"
      timezone: "local"
      personalization: "high"
  - channelId: "phone"
    type: "phone"
    enabled: true
    priority: 1
    config:
      callType: "account-manager"
      scheduleWithin: "24h"
      scriptId: "retention-call-v2"
      criticalRiskOnly: true
  - channelId: "in-app"
    type: "in-app"
    enabled: true
    priority: 2
    config:
      messageType: "retention-offer"
      displayDuration: 7  # days

offerStrategy:
  enabled: true
  segmentedOffers: true
  segments:
    - riskLevel: "critical"
      offerType: "premium"
      discount: 50
      duration: 6  # months
      includeUpgrade: true
      includePersonalCall: true
    - riskLevel: "high"
      offerType: "standard"
      discount: 30
      duration: 3  # months
    - riskLevel: "medium"
      offerType: "engagement"
      discount: 15
      duration: 1  # month

batchSchedule: "cron(0 2 ? * MON *)"  # Weekly on Monday at 2 AM UTC
candidateTTLDays: 30
```

---

## Summary

This use case demonstrates:
- ✅ **Batch processing** at scale (500K → 25K candidates)
- ✅ **Multi-stage pipeline** (ETL → Filter → Score → Store)
- ✅ **Dual ML models** (churn risk + offer effectiveness)
- ✅ **Segmented campaigns** (critical, high, medium risk)
- ✅ **Multi-channel delivery** (email + phone + in-app)
- ✅ **High retention** (21.2% overall, 46% for critical)
- ✅ **Massive savings** ($2.1M revenue saved annually)
- ✅ **Excellent ROI** (2,800% return on investment)
- ✅ **Personalized offers** (50% discount for critical risk)
- ✅ **Human touch** (personal calls for high-value customers)

**Key Insight**: This is NOT solicitation—it's proactive retention through personalized value delivery. The platform predicts churn and delivers targeted offers to keep customers engaged!
