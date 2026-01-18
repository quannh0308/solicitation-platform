# Use Case: Credit Card Fraud Detection

## Overview

**Business Goal**: Prevent credit card fraud in real-time, blocking $5M+ in fraudulent transactions annually while maintaining <0.5% false positive rate.

**Processing Mode**: Reactive (Real-time event-driven, ultra-low latency)

**Action Type**: Transaction Protection (not solicitation)

**Actors**:
- Cardholder (customer)
- Fraud prevention team
- Customer service team
- EventBridge
- SMS/Email/Push gateway

---

## Actor Interaction Diagram

```
┌──────────┐         ┌──────────────┐         ┌─────────────────┐
│Cardholder│         │ Fraud Team   │         │ Service Team    │
└────┬─────┘         └──────┬───────┘         └────────┬────────┘
     │                      │                          │
     │ 1. Transaction       │                          │
     │    Attempted         │                          │
     ├──────────────────────┼──────────────────────────┤
     │                      │                          │
     │ 2. Fraud Risk        │                          │
     │    Detected          │                          │
     ├──────────────────────┼──────────────────────────┤
     │                      │                          │
     │                      │ 3. Monitors Fraud        │
     │                      │    Patterns              │
     │                      │<─────────────────────────┤
     │                      │                          │
     │ 4. Transaction       │                          │
     │    Blocked           │<─────────────────────────┤
     │<─────────────────────┤    (Immediate)           │
     │                      │                          │
     │ 5. Receives Alert    │                          │
     │    (SMS Instant)     │<─────────────────────────┤
     │<─────────────────────┤                          │
     │                      │                          │
     │ 6. Verifies or       │                          │
     │    Reports Fraud     │                          │
     ├─────────────────────>│                          │
     │                      │                          │
     │                      │ 7. Updates Fraud         │
     │                      │    Models                │
     │                      ├─────────────────────────>│
     │                      │                          │
     ▼                      ▼                          ▼
```

---

## Reactive Data Flow (Real-Time Processing)

### Event-Driven Processing - <50ms End-to-End

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      REACTIVE DATA FLOW                                  │
│              (Ultra-Low Latency Fraud Detection)                         │
└─────────────────────────────────────────────────────────────────────────┘

T+0ms: Transaction Attempted
┌─────────────────────────────────────┐
│      Payment Processing System      │
│  - Card swipe at merchant           │
│  - Amount: $2,500                   │
│  - Merchant: "Electronics Store"    │
│  - Location: Lagos, Nigeria         │
│  - Cardholder typical: Seattle, US  │
│  - Publishes event to EventBridge   │
└────────┬────────────────────────────┘
         │ Event: {
         │   cardId: "CARD-123",
         │   customerId: "C789",
         │   transactionId: "TXN-456",
         │   amount: 2500.00,
         │   currency: "USD",
         │   merchant: {
         │     name: "Electronics Store",
         │     category: "electronics",
         │     location: "Lagos, Nigeria"
         │   },
         │   cardPresent: false,
         │   timestamp: "2026-01-18T22:15:30Z"
         │ }
         ▼

T+2ms: EventBridge Routes Event
┌─────────────────────────────────────┐
│         EventBridge Rule            │
│  Pattern: {                         │
│    source: "payment.processing",    │
│    detail-type: "TransactionAttempt"│
│  }                                  │
│  Target: Reactive Lambda            │
└────────┬────────────────────────────┘
         │ Event matched, trigger Lambda
         ▼

T+5ms: Reactive Lambda Invoked
┌─────────────────────────────────────┐
│      Reactive Lambda Handler        │
│  1. Parse event (1ms)               │
│  2. Check deduplication (2ms)       │
│  3. Create candidate object (2ms)   │
└────────┬────────────────────────────┘
         │ Candidate created
         ▼

T+10ms: Filter Lambda Processing
┌─────────────────────────────────────┐
│      Filter Chain Executor          │
│  Parallel filter execution (20ms)   │
└────────┬────────────────────────────┘
         │
         ├─> Trust Filter (4ms)
         │   ├─ Verify card is active
         │   ├─ Check account status: Active
         │   └─ Result: PASS
         │
         ├─> Transaction Anomaly Filter (6ms)
         │   ├─ Check amount anomaly
         │   │   • Typical transaction: $50-200
         │   │   • Current: $2,500 (12.5x avg)
         │   │   • Anomaly: HIGH
         │   ├─ Check location anomaly
         │   │   • Typical: Seattle, US
         │   │   • Current: Lagos, Nigeria
         │   │   • Distance: 7,800 miles
         │   │   • Last transaction: 2 hours ago in Seattle
         │   │   • Impossible travel: YES
         │   ├─ Check merchant category
         │   │   • High-risk category: Electronics
         │   │   • Fraud rate: 15%
         │   └─ Result: PASS (multiple anomalies)
         │
         ├─> Velocity Filter (5ms)
         │   ├─ Check transaction velocity
         │   │   • Last 24 hours: 1 transaction
         │   │   • Current: 2nd transaction
         │   │   • Velocity: Normal
         │   └─ Result: PASS
         │
         ├─> Card Present Filter (5ms)
         │   ├─ Check if card physically present
         │   │   • Card present: NO (online)
         │   │   • Risk multiplier: 2x
         │   └─ Result: PASS (higher risk)
         │
         ▼
    Candidate eligible for scoring
         │
         ▼

T+30ms: Score Lambda Processing
┌─────────────────────────────────────┐
│     Feature Store Client (8ms)      │
│  - Retrieve cardholder features     │
│    • typical_locations: ["Seattle"] │
│    • typical_amount: $125           │
│    • transaction_history: 450       │
│    • fraud_history: 0               │
│  - Retrieve transaction features    │
│    • amount_ratio: 12.5x            │
│    • location_distance: 7800 miles  │
│    • impossible_travel: true        │
│    • merchant_risk: 0.15            │
│    • card_present: false            │
└────────┬────────────────────────────┘
         │ Features retrieved
         ▼
┌─────────────────────────────────────┐
│    SageMaker Scoring Provider (10ms)│
│  - Call fraud detection model       │
│  - Input: cardholder + txn features │
│  - Output: fraud_risk = 0.96        │
│  - Risk level: CRITICAL             │
│  - Recommended action: BLOCK        │
│  - Confidence: VERY HIGH            │
└────────┬────────────────────────────┘
         │ Score computed
         ▼

T+48ms: Store Lambda + Immediate Action
┌─────────────────────────────────────┐
│   DynamoDB Candidate Repository     │
│  - Write candidate (2ms)            │
│  - Attributes:                      │
│    • fraud_risk: 0.96               │
│    • action: "BLOCK_AND_ALERT"      │
└────────┬────────────────────────────┘
         │
         ├─> IMMEDIATE ACTIONS
         │
         ├─> Block Transaction
         │   ├─ Payment processor notified: DECLINE
         │   ├─ Merchant receives decline code
         │   └─ Transaction blocked
         │
         ├─> Lock Card (Temporary)
         │   └─ Card locked for 1 hour
         │
         └─> Trigger Multi-Channel Alert
             └─ SMS + Push sent immediately
         ▼

T+50ms: Reactive Processing Complete
Total Latency: 50ms
Transaction blocked, card locked, customer alerted
```

---

## Action Delivery Flow (Fraud Alert)

### Immediate Multi-Channel Alert

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      ACTION DELIVERY FLOW                                │
│                   (Immediate Fraud Alert)                                │
└─────────────────────────────────────────────────────────────────────────┘

T+50ms: Trigger Immediate Alert
┌─────────────────────────────────────┐
│      Channel Selection Logic        │
│  - Fraud risk: 0.96 (critical)      │
│  - Priority: URGENT                 │
│  - Channels: SMS + Push (parallel)  │
│  - Action: Block + Lock + Alert     │
└────────┬────────────────────────────┘
         │
         ├─> SMS Channel (Primary)
         │
         ▼
┌─────────────────────────────────────┐
│      SMS Channel Adapter            │
│  - Send to registered phone         │
│  - Format urgent SMS                │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│         SMS Gateway                 │
│  Message: "FRAUD ALERT: $2,500      │
│  transaction in Lagos, Nigeria was  │
│  BLOCKED. If this was you, reply    │
│  YES. If not, your card is safe.    │
│  Call: 1-800-XXX-XXXX"              │
└────────┬────────────────────────────┘
         │ SMS sent (T+60ms)
         │
         ├─> Push Notification (Secondary)
         │
         ▼
┌─────────────────────────────────────┐
│      Push Notification Service      │
│  Title: "Transaction Blocked"       │
│  Body: "$2,500 charge blocked in    │
│        Lagos, Nigeria. Tap to       │
│        verify or report fraud."     │
│  Action: Open app to fraud center   │
└────────┬────────────────────────────┘
         │ Push sent (T+70ms)
         ▼

T+70ms: Customer Receives Alerts
┌─────────────────────────────────────┐
│         Customer Phone              │
│  - SMS arrives within 1 second      │
│  - Push notification displayed      │
└────────┬────────────────────────────┘
         │
         │ Customer responds within 30 seconds
         ▼

Scenario 1: Legitimate Transaction (False Positive)
┌─────────────────────────────────────┐
│      Customer Replies "YES"         │
│  - SMS response processed           │
│  - Identity verification required   │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│      2FA Verification               │
│  - SMS code sent                    │
│  - Customer enters code             │
│  - Identity verified ✓              │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│      Transaction Approved           │
│  - Card unlocked                    │
│  - Transaction re-attempted         │
│  - Transaction approved             │
│  - Location added to trusted list   │
│  - Candidate marked "false_positive"│
└─────────────────────────────────────┘

Scenario 2: Fraud Attempt (True Positive)
┌─────────────────────────────────────┐
│      Customer Replies "NO" or       │
│      Calls Fraud Hotline            │
│  - Confirms fraud attempt           │
│  - Card permanently locked          │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│      Fraud Response Actions         │
│  - Card permanently deactivated     │
│  - New card issued (expedited)      │
│  - Fraud investigation opened       │
│  - Merchant reported                │
│  - Law enforcement notified         │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│    Candidate Status Update          │
│  - Mark candidate as "true_positive"│
│  - Record alert timestamp           │
│  - Record response timestamp        │
│  - Update fraud models              │
│  - Fraud prevented: $2,500          │
└─────────────────────────────────────┘

Protection Complete
Fraud Prevented: Yes
Customer Notified: Within 1 second
Financial Loss: $0
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
│ Every    │ Unverified Transaction Escalation                          │
│ Hour     │ - Query blocked transactions not yet verified              │
│ :00      │ - Filter: blocked >1 hour ago                              │
│          │ - Action: Call customer directly                           │
│          │ - Duration: 5 minutes                                      │
└──────────┴────────────────────────────────────────────────────────────┘

Daily Schedule:
┌──────────┬────────────────────────────────────────────────────────────┐
│   Time   │                        Job                                 │
├──────────┼────────────────────────────────────────────────────────────┤
│ 3:00 AM  │ Fraud Pattern Analysis                                     │
│          │ - Aggregate yesterday's fraud attempts                     │
│          │ - Identify merchant fraud rings                            │
│          │ - Update fraud rules                                       │
│          │ - Alert fraud team                                         │
│          │ - Duration: 45 minutes                                     │
├──────────┼────────────────────────────────────────────────────────────┤
│ 6:00 AM  │ Fraud Prevention Report                                    │
│          │ - Calculate prevention metrics                             │
│          │ - Generate executive dashboard                             │
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
│ 3:00 AM  │ - Retrain fraud detection model                            │
│          │ - Evaluate model performance                               │
│          │ - Deploy if improved                                       │
│          │ - Duration: 3 hours                                        │
└──────────┴────────────────────────────────────────────────────────────┘
```

---

## Metrics & Success Criteria

### Processing Metrics
- **Reactive Latency**: 50ms average (target: < 100ms)
- **P99 Latency**: 85ms (target: < 200ms)
- **Throughput**: 50,000 transactions/second
- **Filter Pass Rate**: 2% (high-risk transactions only)

### Delivery Metrics
- **SMS Delivery Rate**: 99.5%
- **Push Delivery Rate**: 98%
- **SMS Response Rate**: 85% (within 5 minutes)
- **Push Response Rate**: 60%

### Fraud Metrics
- **Fraud Detection Rate**: 97%
- **False Positive Rate**: 0.4%
- **True Positive Rate**: 99.6%
- **Average Response Time**: 45 seconds (customer verification)
- **Fraud Amount Blocked**: $5M+ annually

### Business Metrics
- **Fraud Losses Prevented**: $5M+ annually
- **Cost per Alert**: $0.05
- **ROI**: 1,000,000% ($5M saved / $5K cost)
- **Customer Satisfaction**: 4.7/5.0 (despite blocks)
- **False Positive Impact**: <$50K annually (customer friction)

### Technical Metrics
- **Serving API Latency**: P99 = 8ms (target: < 30ms)
- **Cache Hit Rate**: 95%
- **Error Rate**: 0.001%
- **Availability**: 99.99%
- **Deduplication Rate**: 99.99%

---

## Component Configuration

### Program Configuration
```yaml
programId: "credit-card-fraud-detection"
programName: "Credit Card Fraud Detection"
enabled: true
marketplaces: ["US", "UK", "DE", "FR", "JP", "IN", "BR", "AU"]

eventSources:
  - sourceId: "payment-processing-events"
    type: "eventbridge"
    config:
      eventBusName: "payment-platform"
      eventPattern:
        source: ["payment.processing"]
        detail-type: ["TransactionAttempt"]

scoringModels:
  - modelId: "fraud-detection-v7"
    endpoint: "sagemaker://fraud-detection-v7"
    features: ["amount_ratio", "location_distance", "impossible_travel", "merchant_risk", "card_present", "velocity"]
    latencyTarget: 10  # ms

filterChain:
  - filterId: "trust"
    type: "trust"
    order: 1
  - filterId: "transaction-anomaly"
    type: "business-rule"
    order: 2
    parameters:
      maxAmountRatio: 5.0  # 5x typical
      maxLocationDistance: 500  # miles
      checkImpossibleTravel: true
      highRiskMerchantCategories: ["electronics", "jewelry", "gift_cards"]
  - filterId: "velocity"
    type: "business-rule"
    order: 3
    parameters:
      maxTransactionsPerHour: 10
      maxAmountPerHour: 5000
  - filterId: "card-present"
    type: "business-rule"
    order: 4
    parameters:
      riskMultiplierOnline: 2.0

channels:
  - channelId: "sms"
    type: "sms"
    enabled: true
    priority: 1
    config:
      message: "FRAUD ALERT: ${amount} transaction in {location} was BLOCKED. If this was you, reply YES. If not, your card is safe. Call: {fraudHotline}"
      maxLength: 160
      immediate: true
  - channelId: "push"
    type: "push"
    enabled: true
    priority: 2
    config:
      title: "Transaction Blocked"
      body: "${amount} charge blocked in {location}. Tap to verify or report fraud."
      immediate: true
  - channelId: "email"
    type: "email"
    enabled: true
    priority: 3
    config:
      templateId: "fraud-alert-v3"
      subject: "URGENT: Suspicious Transaction Blocked"
      immediate: true

fraudActions:
  enabled: true
  autoBlock: true
  riskThreshold: 0.85
  actions:
    - action: "block_transaction"
      condition: "fraud_risk > 0.85"
    - action: "lock_card_temporary"
      condition: "fraud_risk > 0.9"
      duration: 1  # hour
    - action: "lock_card_permanent"
      condition: "fraud_risk > 0.95 AND customer_no_response > 24h"
    - action: "alert_fraud_team"
      condition: "fraud_risk > 0.95"

verificationFlow:
  enabled: true
  methods:
    - type: "sms_reply"
      timeout: 300  # seconds
    - type: "phone_call"
      timeout: 3600  # seconds
    - type: "app_verification"
      timeout: 300  # seconds

deduplication:
  enabled: true
  ttlDays: 30
  keyFormat: "{cardId}:{transactionId}:{programId}"

candidateTTLDays: 30
```

---

## Summary

This use case demonstrates:
- ✅ **Ultra-low latency** (<50ms end-to-end)
- ✅ **Real-time fraud detection** (SageMaker endpoint)
- ✅ **Immediate protection** (block + lock within 50ms)
- ✅ **Multi-channel alerts** (SMS + push + email)
- ✅ **High accuracy** (97% detection, 0.4% false positives)
- ✅ **Massive savings** ($5M+ fraud losses prevented)
- ✅ **Fast verification** (45 seconds average)
- ✅ **Customer satisfaction** (4.7/5.0 despite blocks)
- ✅ **Extreme scalability** (50,000 transactions/second)

**Key Insight**: This is NOT solicitation—it's real-time financial protection. The platform detects fraudulent transactions and takes immediate action to protect cardholders!
