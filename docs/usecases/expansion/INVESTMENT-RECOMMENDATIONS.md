# Use Case: Investment Recommendations

## Overview

**Business Goal**: Increase assets under management (AUM) by $500M+ through personalized investment recommendations based on customer financial profiles and market conditions.

**Processing Mode**: Batch (Daily scheduled job) + Reactive (Market events)

**Action Type**: Investment Recommendation

**Actors**:
- Investor (customer)
- Wealth management team
- Investment advisory team
- Data warehouse
- Email/In-App/Push services

---

## Actor Interaction Diagram

```
┌──────────┐         ┌──────────────┐         ┌─────────────────┐
│ Investor │         │ Wealth Team  │         │ Advisory Team   │
└────┬─────┘         └──────┬───────┘         └────────┬────────┘
     │                      │                          │
     │ 1. Has Idle Cash     │                          │
     │    in Account        │                          │
     ├──────────────────────┼──────────────────────────┤
     │                      │                          │
     │                      │ 2. Analyzes Investment   │
     │                      │    Opportunities         │
     │                      │<─────────────────────────┤
     │                      │                          │
     │                      │ 3. Configures            │
     │                      │    Recommendation Program│
     │                      ├─────────────────────────>│
     │                      │                          │
     │                      │                          │ 4. Schedules
     │                      │                          │    Daily Batch
     │                      │                          ├────────────┐
     │                      │                          │            │
     │                      │                          │<───────────┘
     │                      │                          │
     │ 5. Receives          │                          │
     │    Recommendations   │<─────────────────────────┤
     │<─────────────────────┤    (Personalized)        │
     │                      │                          │
     │ 6. Invests           │                          │
     │    (1-Click)         │                          │
     ├─────────────────────>│                          │
     │                      │                          │
     │                      │ 7. Tracks AUM Growth     │
     │                      │<─────────────────────────┤
     │                      │                          │
     ▼                      ▼                          ▼
```

---

## Data Ingestion Flow (Batch Processing)

### Daily Batch Job - 6:00 AM (After Market Open)

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         DATA INGESTION FLOW                              │
│                      (Batch - Daily at 6:00 AM)                          │
└─────────────────────────────────────────────────────────────────────────┘

Step 1: ETL Lambda (6:00 AM - 6:30 AM)
┌──────────────────┐
│  Data Warehouse  │
│  (Redshift)      │
└────────┬─────────┘
         │ Query: SELECT customer_id, account_balance,
         │        idle_cash, risk_tolerance, investment_goals,
         │        portfolio_allocation, age, income,
         │        investment_experience, time_horizon
         │        FROM customer_investment_profiles
         │        WHERE idle_cash > 5000
         │        AND account_status = 'active'
         ▼
┌─────────────────────────────────────┐
│    Redshift Connector               │
│  - Extract 150K customer records    │
│  - Filter: Idle cash > $5,000       │
│  - Map fields to candidate model    │
│  - Enrich with market data          │
└────────┬────────────────────────────┘
         │ 150K raw candidates
         ▼
┌─────────────────────────────────────┐
│         ETL Lambda                  │
│  - Transform to unified model       │
│  - Add context (risk profile, goals)│
│  - Calculate investment capacity    │
│  - Set event metadata               │
└────────┬────────────────────────────┘
         │ 150K candidates
         │ {customerId, idleCash, riskTolerance, ...}
         ▼

Step 2: Filter Lambda (6:30 AM - 6:50 AM)
┌─────────────────────────────────────┐
│      Filter Chain Executor          │
└────────┬────────────────────────────┘
         │
         ├─> Trust Filter
         │   ├─ Verify customer identity
         │   ├─ Check account status: Active
         │   └─ Result: Remove 7.5K (5%)
         │
         ├─> Investment Qualification Filter
         │   ├─ Check idle cash: >$5,000
         │   ├─ Check account age: >6 months
         │   ├─ Check investment experience: Documented
         │   └─ Result: Remove 30K (20%)
         │
         ├─> Risk Profile Filter
         │   ├─ Check risk tolerance: Defined
         │   ├─ Check investment goals: Set
         │   ├─ Check time horizon: >1 year
         │   └─ Result: Remove 37.5K (25%)
         │
         ├─> Eligibility Filter
         │   ├─ Customer hasn't received recommendation in 7 days
         │   ├─ Customer hasn't opted out
         │   └─ Result: Remove 22.5K (15%)
         │
         ├─> Regulatory Compliance Filter
         │   ├─ Check accredited investor status (if needed)
         │   ├─ Check suitability requirements
         │   └─ Result: Remove 7.5K (5%)
         │
         ▼
    45,000 eligible candidates
         │
         ▼

Step 3: Score Lambda (6:50 AM - 7:20 AM)
┌─────────────────────────────────────┐
│     Feature Store Client            │
│  - Retrieve customer features       │
│    • risk_tolerance: "moderate"     │
│    • investment_goals: "growth"     │
│    • time_horizon: 10 years         │
│    • portfolio_allocation: {...}    │
│  - Retrieve market features         │
│    • market_conditions: "bullish"   │
│    • sector_performance: {...}      │
│    • volatility_index: 15           │
└────────┬────────────────────────────┘
         │ Features for 45,000 candidates
         ▼
┌─────────────────────────────────────┐
│    SageMaker Scoring Provider       │
│  - Call investment suitability model│
│  - Call portfolio optimization model│
│  - Batch scoring (1000 at a time)   │
│  - Investment score: 0.0 - 1.0      │
│  - Recommended allocation           │
└────────┬────────────────────────────┘
         │ Scores computed
         ▼
┌─────────────────────────────────────┐
│       Score Cache Repository        │
│  - Cache scores in DynamoDB         │
│  - TTL: 7 days                      │
└────────┬────────────────────────────┘
         │ 45,000 scored candidates
         │ Score distribution:
         │   High (0.7-1.0): 15,000
         │   Medium (0.4-0.7): 20,000
         │   Low (0.0-0.4): 10,000
         │
         │ Recommendations:
         │   Stocks: 18,000
         │   Bonds: 12,000
         │   ETFs: 10,000
         │   Mutual Funds: 5,000
         ▼

Step 4: Store Lambda (7:20 AM - 7:35 AM)
┌─────────────────────────────────────┐
│   DynamoDB Candidate Repository     │
│  - Batch write (25 items per batch) │
│  - Primary key: CustomerId:Program  │
│  - GSI1: Program:RiskProfile:Score  │
│  - GSI2: Program:AssetClass:Date    │
│  - TTL: 7 days from now             │
└────────┬────────────────────────────┘
         │ 45,000 candidates stored
         ▼

Batch Job Complete: 7:35 AM
Total Time: 1 hour 35 minutes
```

---

## Action Delivery Flow (Investment Recommendations)

### Multi-Channel Delivery - 9:00 AM

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      ACTION DELIVERY FLOW                                │
│                   (Personalized Investment Recommendations)              │
└─────────────────────────────────────────────────────────────────────────┘

Channel 1: In-App (Primary) - Real-Time
┌─────────────────────────────────────┐
│      Customer Opens Banking App     │
│  - App loads dashboard              │
│  - Calls Serving API                │
│  - Request: GetInvestmentRecs       │
│    • customerId: "C123"             │
│    • channel: "in-app"              │
└────────┬────────────────────────────┘
         │ Real-time query (10ms)
         ▼
┌─────────────────────────────────────┐
│      Serving API Response           │
│  - Top 5 recommendations            │
│  - Personalized to risk profile     │
│  - Current market conditions        │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│      Investment Dashboard           │
│  ┌─────────────────────────────┐   │
│  │ "Recommended for You"        │   │
│  │                              │   │
│  │ 1. Tech ETF (VGT)            │   │
│  │    • Expected return: 12%    │   │
│  │    • Risk: Moderate          │   │
│  │    • Amount: $10,000         │   │
│  │    [Invest Now]              │   │
│  │                              │   │
│  │ 2. S&P 500 Index (VOO)       │   │
│  │    • Expected return: 10%    │   │
│  │    • Risk: Low               │   │
│  │    • Amount: $15,000         │   │
│  │    [Invest Now]              │   │
│  │                              │   │
│  │ 3. Corporate Bonds (LQD)     │   │
│  │    • Expected return: 5%     │   │
│  │    • Risk: Very Low          │   │
│  │    • Amount: $5,000          │   │
│  │    [Invest Now]              │   │
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
         │
         │ 30,000 customers see recommendations
         │ 15,000 click (50%)
         │ 6,000 invest (20%)
         ▼

In-App Results:
- Impressions: 30,000
- Clicks: 15,000 (50%)
- Investments: 6,000 (20%)
- Average investment: $18,500
- Total AUM: $111M

---

Channel 2: Email (Secondary) - 9:00 AM
┌─────────────────────────────────────┐
│      Email Campaign Service         │
│  Subject: "Investment opportunities │
│           tailored for you"         │
│  Body:                              │
│  - "Based on your financial goals"  │
│  - Top 3 recommendations            │
│  - Expected returns                 │
│  - Risk analysis                    │
│  - "Invest with 1 click"            │
└────────┬────────────────────────────┘
         │ 45,000 emails sent
         │ 18,000 opens (40%)
         │ 5,400 clicks (12%)
         │ 1,800 investments (4%)
         ▼

Email Results:
- Opens: 18,000 (40%)
- Clicks: 5,400 (12%)
- Investments: 1,800 (4%)
- Average investment: $16,000
- Total AUM: $28.8M

---

Overall Campaign Results:
- Total Customers: 45,000
- Total Investments: 7,800 (17.3%)
- Total AUM Added: $139.8M per week
- Annual AUM Growth: $7.3B
- Management Fee (0.5%): $36.5M annually
- Campaign Cost: $135K per week
- ROI: 5,296%
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
│ 6:00 AM  │ Batch Ingestion Workflow (ETL → Filter → Score → Store)   │
│          │ - Process customer investment profiles                     │
│          │ - Duration: 1 hour 35 minutes                              │
│          │ - Output: 45,000 candidates                                │
├──────────┼────────────────────────────────────────────────────────────┤
│ 7:45 AM  │ Market Data Refresh                                        │
│          │ - Update market conditions                                 │
│          │ - Refresh sector performance                               │
│          │ - Duration: 15 minutes                                     │
├──────────┼────────────────────────────────────────────────────────────┤
│ 9:00 AM  │ Email Campaign Delivery                                    │
│          │ - Send personalized recommendations                        │
│          │ - Send 45,000 emails                                       │
│          │ - Duration: 45 minutes                                     │
├──────────┼────────────────────────────────────────────────────────────┤
│ 4:00 PM  │ Market Close Analysis                                      │
│          │ - Analyze day's investment activity                        │
│          │ - Calculate AUM growth                                     │
│          │ - Duration: 30 minutes                                     │
├──────────┼────────────────────────────────────────────────────────────┤
│ 11:59 PM │ Metrics Aggregation                                        │
│          │ - Aggregate daily metrics                                  │
│          │ - Publish to CloudWatch                                    │
│          │ - Generate daily report                                    │
│          │ - Duration: 10 minutes                                     │
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
│ 3:00 AM  │ - Retrain investment suitability model                     │
│          │ - Retrain portfolio optimization model                     │
│          │ - Evaluate model performance                               │
│          │ - Deploy if improved                                       │
│          │ - Duration: 4 hours                                        │
└──────────┴────────────────────────────────────────────────────────────┘
```

---

## Metrics & Success Criteria

### Processing Metrics
- **Batch Duration**: 1 hour 35 minutes (target: < 2 hours)
- **Throughput**: 7,895 candidates/minute
- **Filter Pass Rate**: 30% (45K / 150K)
- **High Score Rate**: 33% (15K / 45K)

### Delivery Metrics
- **Email Send Rate**: 45,000 per day
- **Email Open Rate**: 40%
- **Email Click-Through Rate**: 12%
- **In-App Display Rate**: 67% (30K / 45K)
- **In-App Click Rate**: 50%

### Investment Metrics
- **Investment Rate**: 17.3% (7,800 / 45,000)
- **Average Investment**: $17,923
- **Daily AUM Growth**: $139.8M
- **Weekly AUM Growth**: $979M
- **Annual AUM Growth**: $50.9B

### Business Metrics
- **Management Fee Revenue**: $254.5M annually (0.5% of AUM)
- **Campaign Cost**: $135K per week
- **Annual Cost**: $7M
- **ROI**: 3,636%
- **Customer Satisfaction**: 4.6/5.0

### Technical Metrics
- **Serving API Latency**: P99 = 10ms (target: < 30ms)
- **Cache Hit Rate**: 90%
- **Error Rate**: 0.005%
- **Availability**: 99.98%

---

## Component Configuration

### Program Configuration
```yaml
programId: "investment-recommendations"
programName: "Investment Recommendations"
enabled: true
marketplaces: ["US", "UK", "DE", "FR", "JP", "AU"]

dataConnectors:
  - connectorId: "redshift-investment-profiles"
    type: "redshift"
    config:
      cluster: "wealth-analytics"
      database: "customer_data"
      schema: "investments"
      query: |
        SELECT customer_id, account_balance, idle_cash,
               risk_tolerance, investment_goals,
               portfolio_allocation, age, income,
               investment_experience, time_horizon
        FROM customer_investment_profiles
        WHERE idle_cash > 5000
        AND account_status = 'active'

scoringModels:
  - modelId: "investment-suitability-v3"
    endpoint: "sagemaker://investment-suitability-v3"
    features: ["risk_tolerance", "investment_goals", "time_horizon", "age", "income"]
  - modelId: "portfolio-optimization-v2"
    endpoint: "sagemaker://portfolio-optimization-v2"
    features: ["portfolio_allocation", "market_conditions", "sector_performance"]

filterChain:
  - filterId: "trust"
    type: "trust"
    order: 1
  - filterId: "investment-qualification"
    type: "business-rule"
    order: 2
    parameters:
      minIdleCash: 5000
      minAccountAge: 180  # days
      requireInvestmentExperience: true
  - filterId: "risk-profile"
    type: "business-rule"
    order: 3
    parameters:
      requireRiskTolerance: true
      requireInvestmentGoals: true
      minTimeHorizon: 365  # days
  - filterId: "eligibility"
    type: "eligibility"
    order: 4
  - filterId: "regulatory-compliance"
    type: "business-rule"
    order: 5
    parameters:
      checkAccreditedInvestor: true
      checkSuitability: true
      checkKYC: true

channels:
  - channelId: "in-app"
    type: "in-app"
    enabled: true
    priority: 1
    config:
      sectionName: "Recommended Investments"
      maxRecommendations: 5
      refreshInterval: "realtime"
      includeExpectedReturns: true
      includeRiskAnalysis: true
  - channelId: "email"
    type: "email"
    enabled: true
    priority: 2
    config:
      templateId: "investment-recommendations-v2"
      sendTime: "09:00"
      timezone: "local"
      maxRecommendations: 3
  - channelId: "push"
    type: "push"
    enabled: true
    priority: 3
    config:
      title: "New investment opportunities"
      body: "Check out {count} personalized recommendations"
      highScoreOnly: true

recommendationStrategy:
  enabled: true
  personalizedAllocation: true
  riskProfiles:
    - profile: "conservative"
      allocation:
        bonds: 60
        stocks: 30
        cash: 10
    - profile: "moderate"
      allocation:
        stocks: 60
        bonds: 30
        alternatives: 10
    - profile: "aggressive"
      allocation:
        stocks: 80
        alternatives: 15
        cash: 5

regulatoryCompliance:
  enabled: true
  suitabilityAnalysis: true
  riskDisclosure: true
  feeDisclosure: true
  performanceDisclaimer: true

batchSchedule: "cron(0 6 * * ? *)"  # Daily at 6 AM UTC (after market open)
candidateTTLDays: 7
```

---

## Summary

This use case demonstrates:
- ✅ **Batch processing** at scale (150K → 45K candidates)
- ✅ **Multi-stage pipeline** (ETL → Filter → Score → Store)
- ✅ **Dual ML models** (investment suitability + portfolio optimization)
- ✅ **Personalized recommendations** (based on risk profile and goals)
- ✅ **Multi-channel delivery** (in-app + email + push)
- ✅ **High investment rate** (17.3% overall)
- ✅ **Massive AUM growth** ($50.9B annually)
- ✅ **Excellent revenue** ($254.5M management fees annually)
- ✅ **Regulatory compliance** (suitability, risk disclosure, KYC)
- ✅ **Real-time serving** (10ms P99 latency)

**Key Insight**: This is personalized wealth management. The platform identifies investment opportunities and delivers tailored recommendations based on customer financial profiles!
