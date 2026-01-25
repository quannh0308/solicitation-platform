# Use Case: Low Balance & Overdraft Prevention

## Overview

**Business Goal**: Prevent overdraft fees and improve customer satisfaction by proactively alerting customers of low balances and offering overdraft protection, reducing overdraft incidents by 60%.

**Processing Mode**: Reactive (Real-time event-driven)

**Action Type**: Proactive Alert + Protection Offer

**Actors**:
- Account holder (customer)
- Customer service team
- Risk management team
- EventBridge
- SMS/Push gateway

---

## Actor Interaction Diagram

```
┌──────────┐         ┌──────────────┐         ┌─────────────────┐
│ Customer │         │ Service Team │         │ Risk Team       │
└────┬─────┘         └──────┬───────┘         └────────┬────────┘
     │                      │                          │
     │ 1. Balance Drops     │                          │
     │    Below Threshold   │                          │
     ├──────────────────────┼──────────────────────────┤
     │                      │                          │
     │ 2. Low Balance       │                          │
     │    Event Published   │                          │
     ├──────────────────────┼──────────────────────────┤
     │                      │                          │
     │                      │ 3. Monitors Account      │
     │                      │    Health                │
     │                      │<─────────────────────────┤
     │                      │                          │
     │ 4. Receives Alert    │                          │
     │    (SMS Instant)     │<─────────────────────────┤
     │<─────────────────────┤                          │
     │                      │                          │
     │ 5. Takes Action:     │                          │
     │    - Transfer funds  │                          │
     │    - Enable overdraft│                          │
     │    - Ignore          │                          │
     ├─────────────────────>│                          │
     │                      │                          │
     │                      │ 6. Tracks Overdraft      │
     │                      │    Prevention            │
     │                      ├─────────────────────────>│
     │                      │                          │
     ▼                      ▼                          ▼
```

---

## Reactive Data Flow (Real-Time Processing)

### Event-Driven Processing - <100ms End-to-End

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      REACTIVE DATA FLOW                                  │
│              (Real-Time Low Balance Detection)                           │
└─────────────────────────────────────────────────────────────────────────┘

T+0ms: Balance Drops Below Threshold
┌─────────────────────────────────────┐
│      Core Banking System            │
│  - Transaction processed            │
│  - Previous balance: $125           │
│  - Transaction: -$100               │
│  - New balance: $25                 │
│  - Pending transactions: $50        │
│  - Projected balance: -$25          │
│  - Publishes event to EventBridge   │
└────────┬────────────────────────────┘
         │ Event: {
         │   customerId: "C456",
         │   accountId: "ACC-789",
         │   currentBalance: 25.00,
         │   pendingTransactions: 50.00,
         │   projectedBalance: -25.00,
         │   overdraftProtection: false,
         │   linkedAccounts: ["SAV-123"],
         │   timestamp: "2026-01-18T14:30:00Z"
         │ }
         ▼

T+5ms: EventBridge Routes Event
┌─────────────────────────────────────┐
│         EventBridge Rule            │
│  Pattern: {                         │
│    source: "banking.core",          │
│    detail-type: "BalanceChanged",   │
│    detail: {                        │
│      projectedBalance: [-999999, 0] │
│    }                                │
│  }                                  │
│  Target: Reactive Lambda            │
└────────┬────────────────────────────┘
         │ Event matched, trigger Lambda
         ▼

T+10ms: Reactive Lambda Invoked
┌─────────────────────────────────────┐
│      Reactive Lambda Handler        │
│  1. Parse event (2ms)               │
│  2. Check deduplication (3ms)       │
│  3. Create candidate object (2ms)   │
└────────┬────────────────────────────┘
         │ Candidate created
         ▼

T+17ms: Filter Lambda Processing
┌─────────────────────────────────────┐
│      Filter Chain Executor          │
│  Parallel filter execution (35ms)   │
└────────┬────────────────────────────┘
         │
         ├─> Trust Filter (8ms)
         │   ├─ Verify account is active
         │   ├─ Check customer status: Active
         │   └─ Result: PASS
         │
         ├─> Balance Threshold Filter (10ms)
         │   ├─ Check current balance: $25
         │   ├─ Check pending: $50
         │   ├─ Projected balance: -$25
         │   ├─ Threshold: $50
         │   └─ Result: PASS (below threshold)
         │
         ├─> Overdraft Risk Filter (12ms)
         │   ├─ Check overdraft protection: NO
         │   ├─ Check linked accounts: YES (savings)
         │   ├─ Savings balance: $2,500
         │   └─ Result: PASS (can prevent overdraft)
         │
         ├─> Frequency Filter (5ms)
         │   ├─ Check alerts in last 7 days: 0
         │   ├─ Max 2 alerts per week
         │   └─ Result: PASS
         │
         ▼
    Candidate eligible for scoring
         │
         ▼

T+52ms: Score Lambda Processing
┌─────────────────────────────────────┐
│     Feature Store Client (15ms)     │
│  - Retrieve customer features       │
│    • account_age: 3 years           │
│    • overdraft_history: 2 incidents │
│    • avg_balance: $500              │
│    • income_frequency: "biweekly"   │
│  - Retrieve transaction features    │
│    • projected_balance: -$25        │
│    • pending_count: 3               │
│    • linked_savings: $2,500         │
└────────┬────────────────────────────┘
         │ Features retrieved
         ▼
┌─────────────────────────────────────┐
│    SageMaker Scoring Provider (20ms)│
│  - Call overdraft risk model        │
│  - Input: customer + balance        │
│  - Output: overdraft_risk = 0.85    │
│  - Recommended action: ALERT + OFFER│
│  - Suggested transfer: $50          │
│  - Confidence: HIGH                 │
└────────┬────────────────────────────┘
         │ Score computed
         ▼

T+87ms: Store Lambda + Immediate Action
┌─────────────────────────────────────┐
│   DynamoDB Candidate Repository     │
│  - Write candidate (13ms)           │
│  - Attributes:                      │
│    • overdraft_risk: 0.85           │
│    • action: "ALERT_AND_OFFER"      │
│    • suggested_transfer: 50         │
└────────┬────────────────────────────┘
         │
         └─> Trigger Multi-Channel Alert
             └─ SMS + Push sent immediately
         ▼

T+100ms: Reactive Processing Complete
Total Latency: 100ms
Customer alerted, protection offered
```

---

## Action Delivery Flow (Low Balance Alert)

### Immediate Multi-Channel Alert

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      ACTION DELIVERY FLOW                                │
│                   (Proactive Low Balance Alert)                          │
└─────────────────────────────────────────────────────────────────────────┘

T+100ms: Trigger Immediate Alert
┌─────────────────────────────────────┐
│      Channel Selection Logic        │
│  - Overdraft risk: 0.85 (high)      │
│  - Priority: URGENT                 │
│  - Channels: SMS + Push (parallel)  │
│  - Action: Alert + Offer            │
└────────┬────────────────────────────┘
         │
         ├─> SMS Channel (Primary)
         │
         ▼
┌─────────────────────────────────────┐
│      SMS Channel Adapter            │
│  - Format urgent SMS                │
│  - Include action options           │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│         SMS Gateway                 │
│  Message: "LOW BALANCE ALERT: Your  │
│  checking account will overdraft    │
│  with pending transactions. Current:│
│  $25, Pending: $50. Reply:          │
│  1=Transfer from savings            │
│  2=Enable overdraft protection      │
│  3=View details"                    │
└────────┬────────────────────────────┘
         │ SMS sent (T+110ms)
         │
         ├─> Push Notification (Secondary)
         │
         ▼
┌─────────────────────────────────────┐
│      Push Notification Service      │
│  Title: "Low Balance Alert"         │
│  Body: "Your account will overdraft.│
│        Tap to transfer funds or     │
│        enable protection."          │
│  Action: Open app to transfer screen│
└────────┬────────────────────────────┘
         │ Push sent (T+120ms)
         ▼

T+120ms: Customer Receives Alerts
┌─────────────────────────────────────┐
│         Customer Phone              │
│  - SMS arrives within 1 second      │
│  - Push notification displayed      │
└────────┬────────────────────────────┘
         │
         │ Customer responds within 2 minutes
         ▼

Action 1: Transfer from Savings (60% of customers)
┌─────────────────────────────────────┐
│      Customer Replies "1"           │
│  - SMS response processed           │
│  - Transfer initiated               │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│      Automatic Transfer             │
│  - Transfer $50 from savings        │
│  - New checking balance: $75        │
│  - Overdraft prevented ✓            │
│  - Confirmation SMS sent            │
└─────────────────────────────────────┘

Action 2: Enable Overdraft Protection (25% of customers)
┌─────────────────────────────────────┐
│      Customer Replies "2"           │
│  - SMS response processed           │
│  - Overdraft protection enabled     │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│      Overdraft Protection Enabled   │
│  - Linked to savings account        │
│  - Auto-transfer enabled            │
│  - Confirmation SMS sent            │
│  - Future overdrafts prevented      │
└─────────────────────────────────────┘

Action 3: View Details (10% of customers)
┌─────────────────────────────────────┐
│      Customer Replies "3"           │
│  - SMS response processed           │
│  - Deep link sent to app            │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│      Banking App Opened             │
│  - Transaction details displayed    │
│  - Transfer options shown           │
│  - Customer takes action            │
└─────────────────────────────────────┘

Action 4: No Response (5% of customers)
┌─────────────────────────────────────┐
│      No Response After 1 Hour       │
│  - Follow-up SMS sent               │
│  - Email sent with details          │
│  - In-app banner displayed          │
└─────────────────────────────────────┘

Alert Complete
Overdraft Prevented: 95% (actions 1+2+3)
Customer Notified: Within 1 second
Overdraft Fee Saved: $35 per customer
```

---

## Scheduled Jobs

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           SCHEDULED JOBS                                 │
└─────────────────────────────────────────────────────────────────────────┘

Hourly Schedule:
┌──────────┬────────────────────────────────────────────────────────────┐
│   Time   │                        Job                                 │
├──────────┼────────────────────────────────────────────────────────────┤
│ Every    │ Unresolved Alert Follow-Up                                 │
│ Hour     │ - Query alerts with no customer response                   │
│ :00      │ - Filter: sent >1 hour ago                                 │
│          │ - Action: Send follow-up SMS + email                       │
│          │ - Duration: 5 minutes                                      │
└──────────┴────────────────────────────────────────────────────────────┘

Daily Schedule:
┌──────────┬────────────────────────────────────────────────────────────┐
│   Time   │                        Job                                 │
├──────────┼────────────────────────────────────────────────────────────┤
│ 6:00 AM  │ Overdraft Prevention Report                                │
│          │ - Calculate prevention metrics                             │
│          │ - Track customer actions                                   │
│          │ - Generate executive dashboard                             │
│          │ - Duration: 15 minutes                                     │
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
│ 3:00 AM  │ - Retrain overdraft risk model                             │
│          │ - Evaluate model performance                               │
│          │ - Deploy if improved                                       │
│          │ - Duration: 2 hours                                        │
└──────────┴────────────────────────────────────────────────────────────┘
```

---

## Metrics & Success Criteria

### Processing Metrics
- **Reactive Latency**: 100ms average (target: < 200ms)
- **P99 Latency**: 180ms (target: < 500ms)
- **Throughput**: 20,000 events/second
- **Filter Pass Rate**: 40% (customers at risk)

### Delivery Metrics
- **SMS Delivery Rate**: 99%
- **Push Delivery Rate**: 97%
- **SMS Response Rate**: 75% (within 10 minutes)
- **Push Response Rate**: 50%

### Prevention Metrics
- **Customer Action Rate**: 95%
  - Transfer funds: 60%
  - Enable overdraft protection: 25%
  - View details: 10%
  - No response: 5%
- **Overdraft Prevention Rate**: 60% (vs baseline)
- **Average Response Time**: 3 minutes

### Business Metrics
- **Overdraft Fees Prevented**: $3.5M annually (100K incidents × $35 fee)
- **Customer Satisfaction Improvement**: 4.8/5.0 (vs 3.5 without alerts)
- **Overdraft Protection Adoption**: 25% (from alerts)
- **Cost per Alert**: $0.02
- **Annual Cost**: $200K
- **ROI**: 1,750% ($3.5M saved / $200K cost)

### Technical Metrics
- **Serving API Latency**: P99 = 12ms (target: < 30ms)
- **Cache Hit Rate**: 90%
- **Error Rate**: 0.005%
- **Availability**: 99.98%
- **Deduplication Rate**: 99.9%

---

## Component Configuration

### Program Configuration
```yaml
programId: "low-balance-overdraft-prevention"
programName: "Low Balance & Overdraft Prevention"
enabled: true
marketplaces: ["US", "UK", "DE", "FR", "JP", "IN", "BR", "AU"]

eventSources:
  - sourceId: "banking-core-events"
    type: "eventbridge"
    config:
      eventBusName: "banking-platform"
      eventPattern:
        source: ["banking.core"]
        detail-type: ["BalanceChanged"]
        detail:
          projectedBalance: [-999999, 0]

scoringModels:
  - modelId: "overdraft-risk-v2"
    endpoint: "sagemaker://overdraft-risk-v2"
    features: ["current_balance", "pending_transactions", "overdraft_history", "income_frequency"]

filterChain:
  - filterId: "trust"
    type: "trust"
    order: 1
  - filterId: "balance-threshold"
    type: "business-rule"
    order: 2
    parameters:
      balanceThreshold: 50
      checkPendingTransactions: true
  - filterId: "overdraft-risk"
    type: "business-rule"
    order: 3
    parameters:
      requireOverdraftProtectionDisabled: true
      checkLinkedAccounts: true
  - filterId: "frequency"
    type: "business-rule"
    order: 4
    parameters:
      maxAlertsPerWeek: 2

channels:
  - channelId: "sms"
    type: "sms"
    enabled: true
    priority: 1
    config:
      message: "LOW BALANCE ALERT: Your account will overdraft with pending transactions. Current: ${currentBalance}, Pending: ${pending}. Reply: 1=Transfer from savings, 2=Enable overdraft protection, 3=View details"
      maxLength: 160
      immediate: true
      interactiveOptions: true
  - channelId: "push"
    type: "push"
    enabled: true
    priority: 2
    config:
      title: "Low Balance Alert"
      body: "Your account will overdraft. Tap to transfer funds or enable protection."
      immediate: true
      actionButtons:
        - label: "Transfer Funds"
          action: "transfer"
        - label: "Enable Protection"
          action: "enable_protection"

protectionActions:
  enabled: true
  autoTransferOption: true
  overdraftProtectionOffer: true
  actions:
    - action: "suggest_transfer"
      condition: "linked_accounts_available"
      suggestedAmount: "pending_transactions + 50"
    - action: "offer_overdraft_protection"
      condition: "overdraft_protection_disabled"
    - action: "alert_only"
      condition: "no_linked_accounts"

interactiveResponse:
  enabled: true
  smsReplyProcessing: true
  options:
    - reply: "1"
      action: "transfer_from_savings"
      amount: "suggested_transfer"
    - reply: "2"
      action: "enable_overdraft_protection"
    - reply: "3"
      action: "send_details_link"

deduplication:
  enabled: true
  ttlDays: 7
  keyFormat: "{customerId}:{accountId}:{programId}"

candidateTTLDays: 7
```

---

## Summary

This use case demonstrates:
- ✅ **Reactive processing** (<100ms end-to-end latency)
- ✅ **Real-time balance monitoring** (EventBridge)
- ✅ **Immediate alerts** (SMS + push within seconds)
- ✅ **Interactive SMS** (customer can reply to take action)
- ✅ **High action rate** (95% of customers take action)
- ✅ **Overdraft prevention** (60% reduction in incidents)
- ✅ **Cost savings** ($3.5M in fees prevented annually)
- ✅ **Customer satisfaction** (4.8/5.0 vs 3.5 without alerts)
- ✅ **Excellent ROI** (1,750% return on investment)

**Key Insight**: This is proactive financial protection. The platform monitors account balances and alerts customers before overdrafts occur, offering immediate solutions!
