# Use Case: Loan Offer Targeting

## Overview

**Business Goal**: Increase loan origination by 40% through ML-powered pre-approved loan offers to qualified customers, generating $50M+ in annual loan volume.

**Processing Mode**: Batch (Weekly scheduled job)

**Action Type**: Pre-Approved Loan Offer (not solicitation)

**Actors**:
- Customer (potential borrower)
- Lending team
- Risk management team
- Data warehouse
- Email/SMS/In-App services

---

## Actor Interaction Diagram

```
┌──────────┐         ┌──────────────┐         ┌─────────────────┐
│ Customer │         │ Lending Team │         │ Risk Team       │
└────┬─────┘         └──────┬───────┘         └────────┬────────┘
     │                      │                          │
     │ 1. Banking Activity  │                          │
     │    (Good Credit)     │                          │
     ├──────────────────────┼──────────────────────────┤
     │                      │                          │
     │                      │ 2. Analyzes Credit       │
     │                      │    Profiles              │
     │                      │<─────────────────────────┤
     │                      │                          │
     │                      │ 3. Configures Loan       │
     │                      │    Offer Program         │
     │                      ├─────────────────────────>│
     │                      │                          │
     │                      │                          │ 4. Schedules
     │                      │                          │    Weekly Batch
     │                      │                          ├────────────┐
     │                      │                          │            │
     │                      │                          │<───────────┘
     │                      │                          │
     │ 5. Receives Pre-     │                          │
     │    Approved Offer    │<─────────────────────────┤
     │<─────────────────────┤    (Personalized)        │
     │                      │                          │
     │ 6. Applies for Loan  │                          │
     │    (1-Click)         │                          │
     ├─────────────────────>│                          │
     │                      │                          │
     │ 7. Loan Approved     │                          │
     │    & Funded          │                          │
     │<─────────────────────┤                          │
     │                      │                          │
     │                      │ 8. Tracks Origination    │
     │                      │    Metrics               │
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
         │ Query: SELECT customer_id, credit_score, income,
         │        debt_to_income_ratio, account_age,
         │        deposit_balance, transaction_history,
         │        existing_loans, payment_history
         │        FROM customer_credit_profiles
         │        WHERE credit_score >= 680
         │        AND debt_to_income_ratio <= 0.43
         │        AND existing_loans < 3
         ▼
┌─────────────────────────────────────┐
│    Snowflake Connector              │
│  - Extract 200K customer records    │
│  - Filter: Credit score >= 680      │
│  - Filter: DTI <= 43%               │
│  - Map fields to candidate model    │
└────────┬────────────────────────────┘
         │ 200K raw candidates
         ▼
┌─────────────────────────────────────┐
│         ETL Lambda                  │
│  - Transform to unified model       │
│  - Add context (loan type, amount)  │
│  - Calculate affordability          │
│  - Set event metadata               │
└────────┬────────────────────────────┘
         │ 200K candidates
         │ {customerId, creditScore, income, ...}
         ▼

Step 2: Filter Lambda (2:45 AM - 3:10 AM)
┌─────────────────────────────────────┐
│      Filter Chain Executor          │
└────────┬────────────────────────────┘
         │
         ├─> Trust Filter
         │   ├─ Verify customer identity
         │   ├─ Check account status: Active
         │   └─ Result: Remove 10K (5%)
         │
         ├─> Credit Qualification Filter
         │   ├─ Check credit score: >=680
         │   ├─ Check debt-to-income: <=43%
         │   ├─ Check payment history: No late payments in 12 months
         │   └─ Result: Remove 50K (25%)
         │
         ├─> Affordability Filter
         │   ├─ Calculate max loan amount
         │   ├─ Check income stability
         │   ├─ Check employment status
         │   └─ Result: Remove 40K (20%)
         │
         ├─> Eligibility Filter
         │   ├─ Customer hasn't received loan offer in 90 days
         │   ├─ Customer hasn't opted out
         │   └─ Result: Remove 30K (15%)
         │
         ├─> Regulatory Compliance Filter
         │   ├─ Check state lending regulations
         │   ├─ Check federal compliance
         │   └─ Result: Remove 20K (10%)
         │
         ▼
    50,000 eligible candidates
         │
         ▼

Step 3: Score Lambda (3:10 AM - 3:40 AM)
┌─────────────────────────────────────┐
│     Feature Store Client            │
│  - Retrieve customer features       │
│    • credit_score: 750              │
│    • income: $85,000                │
│    • debt_to_income: 0.28           │
│    • account_age: 5 years           │
│    • deposit_balance: $15,000       │
│  - Retrieve behavioral features     │
│    • payment_history: Perfect       │
│    • transaction_patterns: Stable   │
│    • savings_rate: 15%              │
└────────┬────────────────────────────┘
         │ Features for 50,000 candidates
         ▼
┌─────────────────────────────────────┐
│    SageMaker Scoring Provider       │
│  - Call loan approval model         │
│  - Call default risk model          │
│  - Batch scoring (1000 at a time)   │
│  - Approval likelihood: 0.0 - 1.0   │
│  - Recommended loan amount          │
│  - Recommended interest rate        │
└────────┬────────────────────────────┘
         │ Scores computed
         ▼
┌─────────────────────────────────────┐
│       Score Cache Repository        │
│  - Cache scores in DynamoDB         │
│  - TTL: 90 days                     │
└────────┬────────────────────────────┘
         │ 50,000 scored candidates
         │ Score distribution:
         │   Excellent (0.8-1.0): 15,000
         │   Good (0.6-0.8): 25,000
         │   Fair (0.4-0.6): 10,000
         │
         │ Loan offers:
         │   $50K @ 5.5% APR: 15,000
         │   $30K @ 6.5% APR: 25,000
         │   $15K @ 7.5% APR: 10,000
         ▼

Step 4: Store Lambda (3:40 AM - 3:55 AM)
┌─────────────────────────────────────┐
│   DynamoDB Candidate Repository     │
│  - Batch write (25 items per batch) │
│  - Primary key: CustomerId:Program  │
│  - GSI1: Program:CreditTier:Score   │
│  - GSI2: Program:LoanAmount:Date    │
│  - TTL: 90 days from now            │
└────────┬────────────────────────────┘
         │ 50,000 candidates stored
         ▼
┌─────────────────────────────────────┐
│         DynamoDB Tables             │
│  ┌─────────────────────────────┐   │
│  │ Candidates Table            │   │
│  │ - 50,000 new items          │   │
│  │ - Partitioned by customerId │   │
│  │ - Indexed by credit tier    │   │
│  │ - Indexed by loan amount    │   │
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘

Batch Job Complete: 3:55 AM
Total Time: 1 hour 55 minutes
Throughput: 7,194 candidates/minute
```

---

## Action Delivery Flow (Loan Offer Campaign)

### Multi-Channel Delivery - Monday 10:00 AM

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      ACTION DELIVERY FLOW                                │
│                   (Pre-Approved Loan Offers)                             │
└─────────────────────────────────────────────────────────────────────────┘

Segment 1: Excellent Credit (5.5% APR) - 15,000 Customers
┌─────────────────────────────────────┐
│      Serving API Query              │
│  Query: GetCandidatesByCreditTier   │
│  Params:                            │
│    - program: "loan-offers"         │
│    - creditTier: "excellent"        │
│    - minScore: 0.8                  │
└────────┬────────────────────────────┘
         │ 15,000 excellent credit customers
         ▼
┌─────────────────────────────────────┐
│   Multi-Channel Campaign            │
│  - Email (primary)                  │
│  - In-app banner                    │
│  - SMS (high-value only)            │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│      Email Campaign Service         │
│  Subject: "You're Pre-Approved:     │
│           $50,000 at 5.5% APR"      │
│  Body:                              │
│  - "Congratulations, [Name]!"       │
│  - "Based on your excellent credit, │
│     you're pre-approved for:"       │
│    • Loan amount: Up to $50,000     │
│    • Interest rate: 5.5% APR        │
│    • Term: 3-5 years                │
│    • Monthly payment: $920          │
│  - "No impact on credit score"      │
│  - "Apply in 5 minutes"             │
│  - CTA: "Get Your Loan"             │
└────────┬────────────────────────────┘
         │ 15,000 emails sent
         │ 9,000 opens (60%)
         │ 4,500 clicks (30%)
         │ 2,250 applications (15%)
         │ 2,025 funded (90% approval)
         ▼

Segment 1 Results:
- Applications: 2,250 (15%)
- Funded: 2,025 (13.5%)
- Total loan volume: $101.25M
- Average loan: $50,000

---

Segment 2: Good Credit (6.5% APR) - 25,000 Customers
┌─────────────────────────────────────┐
│      Serving API Query              │
│  Query: GetCandidatesByCreditTier   │
│  Params:                            │
│    - program: "loan-offers"         │
│    - creditTier: "good"             │
│    - minScore: 0.6                  │
└────────┬────────────────────────────┘
         │ 25,000 good credit customers
         ▼
┌─────────────────────────────────────┐
│      Email Campaign Service         │
│  Subject: "Pre-Approved: $30,000    │
│           at 6.5% APR"              │
│  Body: Similar to Segment 1         │
│    • Loan amount: Up to $30,000     │
│    • Interest rate: 6.5% APR        │
│    • Monthly payment: $590          │
└────────┬────────────────────────────┘
         │ 25,000 emails sent
         │ 12,500 opens (50%)
         │ 5,000 clicks (20%)
         │ 2,000 applications (8%)
         │ 1,700 funded (85% approval)
         ▼

Segment 2 Results:
- Applications: 2,000 (8%)
- Funded: 1,700 (6.8%)
- Total loan volume: $51M
- Average loan: $30,000

---

Segment 3: Fair Credit (7.5% APR) - 10,000 Customers
┌─────────────────────────────────────┐
│      Serving API Query              │
│  Query: GetCandidatesByCreditTier   │
│  Params:                            │
│    - program: "loan-offers"         │
│    - creditTier: "fair"             │
│    - minScore: 0.4                  │
└────────┬────────────────────────────┘
         │ 10,000 fair credit customers
         ▼
┌─────────────────────────────────────┐
│      Email Campaign Service         │
│  Subject: "Pre-Approved: $15,000    │
│           at 7.5% APR"              │
│  Body: Similar to Segment 1         │
│    • Loan amount: Up to $15,000     │
│    • Interest rate: 7.5% APR        │
│    • Monthly payment: $300          │
└────────┬────────────────────────────┘
         │ 10,000 emails sent
         │ 4,000 opens (40%)
         │ 1,200 clicks (12%)
         │ 300 applications (3%)
         │ 225 funded (75% approval)
         ▼

Segment 3 Results:
- Applications: 300 (3%)
- Funded: 225 (2.25%)
- Total loan volume: $3.375M
- Average loan: $15,000

---

Overall Campaign Results:
- Total Offers: 50,000
- Total Applications: 4,550 (9.1%)
- Total Funded: 3,950 (7.9%)
- Total Loan Volume: $155.625M
- Average Loan: $39,398
- Campaign Cost: $150K
- Revenue (3% origination fee): $4.67M
- ROI: 3,113%
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
│ 2:00 AM  │ - Process customer credit profiles                         │
│          │ - Duration: 1 hour 55 minutes                              │
│          │ - Output: 50,000 candidates                                │
├──────────┼────────────────────────────────────────────────────────────┤
│ Monday   │ Data Warehouse Export                                      │
│ 4:15 AM  │ - Export candidates to S3 (Parquet)                        │
│          │ - Update Glue catalog                                      │
│          │ - Duration: 30 minutes                                     │
├──────────┼────────────────────────────────────────────────────────────┤
│ Monday   │ Email Campaign Delivery                                    │
│ 10:00 AM │ - Send segmented loan offers                               │
│          │ - Send 50,000 emails                                       │
│          │ - Duration: 1 hour                                         │
└──────────┴────────────────────────────────────────────────────────────┘

Daily Schedule:
┌──────────┬────────────────────────────────────────────────────────────┐
│   Time   │                        Job                                 │
├──────────┼────────────────────────────────────────────────────────────┤
│ 6:00 AM  │ Loan Origination Dashboard                                 │
│          │ - Track application rates                                  │
│          │ - Calculate loan volume                                    │
│          │ - Generate executive report                                │
│          │ - Duration: 20 minutes                                     │
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
│ 3:00 AM  │ - Retrain loan approval model                              │
│          │ - Retrain default risk model                               │
│          │ - Evaluate model performance                               │
│          │ - Deploy if improved                                       │
│          │ - Duration: 4 hours                                        │
└──────────┴────────────────────────────────────────────────────────────┘
```

---

## Metrics & Success Criteria

### Processing Metrics
- **Batch Duration**: 1 hour 55 minutes (target: < 2 hours)
- **Throughput**: 7,194 candidates/minute
- **Filter Pass Rate**: 25% (50K / 200K)
- **Excellent Credit Rate**: 30% (15K / 50K)

### Delivery Metrics
- **Email Send Rate**: 50,000 per week
- **Email Open Rate**: 50%
- **Email Click-Through Rate**: 20%
- **In-App Display Rate**: 85%

### Loan Metrics
- **Application Rate**: 9.1% (4,550 / 50,000)
- **Approval Rate**: 86.8% (3,950 / 4,550)
- **Funding Rate**: 7.9% (3,950 / 50,000)
- **Average Loan Amount**: $39,398
- **Weekly Loan Volume**: $155.625M
- **Annual Loan Volume**: $8.1B

### Business Metrics
- **Origination Fee Revenue**: $4.67M per week
- **Annual Revenue**: $242.8M
- **Campaign Cost**: $150K per week
- **Annual Cost**: $7.8M
- **ROI**: 3,113%
- **Default Rate**: 1.2% (excellent portfolio quality)

### Technical Metrics
- **Serving API Latency**: P99 = 15ms (target: < 30ms)
- **Cache Hit Rate**: 88%
- **Error Rate**: 0.01%
- **Availability**: 99.96%

---

## Component Configuration

### Program Configuration
```yaml
programId: "loan-offer-targeting"
programName: "Loan Offer Targeting"
enabled: true
marketplaces: ["US"]  # US only due to regulatory requirements

dataConnectors:
  - connectorId: "snowflake-credit-profiles"
    type: "snowflake"
    config:
      account: "bank.us-east-1"
      database: "customer_data"
      schema: "credit"
      query: |
        SELECT customer_id, credit_score, income,
               debt_to_income_ratio, account_age,
               deposit_balance, transaction_history,
               existing_loans, payment_history
        FROM customer_credit_profiles
        WHERE credit_score >= 680
        AND debt_to_income_ratio <= 0.43
        AND existing_loans < 3

scoringModels:
  - modelId: "loan-approval-v4"
    endpoint: "sagemaker://loan-approval-v4"
    features: ["credit_score", "income", "debt_to_income", "payment_history"]
  - modelId: "default-risk-v3"
    endpoint: "sagemaker://default-risk-v3"
    features: ["account_age", "deposit_balance", "transaction_patterns", "savings_rate"]

filterChain:
  - filterId: "trust"
    type: "trust"
    order: 1
  - filterId: "credit-qualification"
    type: "business-rule"
    order: 2
    parameters:
      minCreditScore: 680
      maxDebtToIncome: 0.43
      minPaymentHistory: 12  # months no late payments
  - filterId: "affordability"
    type: "business-rule"
    order: 3
    parameters:
      checkIncomeStability: true
      checkEmploymentStatus: true
  - filterId: "eligibility"
    type: "eligibility"
    order: 4
  - filterId: "regulatory-compliance"
    type: "business-rule"
    order: 5
    parameters:
      checkStateLaws: true
      checkFederalCompliance: true
      excludedStates: []

channels:
  - channelId: "email"
    type: "email"
    enabled: true
    priority: 1
    config:
      templateId: "loan-offer-v3"
      sendTime: "10:00"
      timezone: "local"
      personalization: "high"
      includePreApprovalDetails: true
  - channelId: "in-app"
    type: "in-app"
    enabled: true
    priority: 2
    config:
      bannerType: "loan-offer"
      displayDuration: 30  # days
      placement: "dashboard"
  - channelId: "sms"
    type: "sms"
    enabled: true
    priority: 3
    config:
      message: "You're pre-approved for a ${loanAmount} loan at {apr}% APR. Apply now: {link}"
      excellentCreditOnly: true

loanOfferStrategy:
  enabled: true
  segmentedOffers: true
  segments:
    - creditTier: "excellent"
      creditScoreRange: [750, 850]
      maxLoanAmount: 50000
      interestRate: 5.5
      term: 60  # months
    - creditTier: "good"
      creditScoreRange: [680, 749]
      maxLoanAmount: 30000
      interestRate: 6.5
      term: 48  # months
    - creditTier: "fair"
      creditScoreRange: [640, 679]
      maxLoanAmount: 15000
      interestRate: 7.5
      term: 36  # months

regulatoryCompliance:
  enabled: true
  truthInLending: true
  fairLending: true
  equalCreditOpportunity: true
  disclosures:
    - type: "APR"
      required: true
    - type: "fees"
      required: true
    - type: "terms"
      required: true

batchSchedule: "cron(0 2 ? * MON *)"  # Weekly on Monday at 2 AM UTC
candidateTTLDays: 90
```

---

## Summary

This use case demonstrates:
- ✅ **Batch processing** at scale (200K → 50K candidates)
- ✅ **Multi-stage pipeline** (ETL → Filter → Score → Store)
- ✅ **Dual ML models** (loan approval + default risk)
- ✅ **Segmented offers** (excellent, good, fair credit)
- ✅ **Multi-channel delivery** (email + in-app + SMS)
- ✅ **High application rate** (9.1% overall)
- ✅ **High approval rate** (86.8% of applications)
- ✅ **Massive loan volume** ($8.1B annually)
- ✅ **Excellent revenue** ($242.8M annually)
- ✅ **Regulatory compliance** (Truth in Lending, Fair Lending)

**Key Insight**: This is NOT solicitation—it's personalized financial product delivery. The platform identifies qualified borrowers and delivers pre-approved loan offers with transparent terms!
