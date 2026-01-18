# Use Case: Suspicious Activity Alerts

## Overview

**Business Goal**: Prevent fraud and account takeovers by detecting suspicious activity in real-time, saving $500K+ annually in fraud losses.

**Processing Mode**: Reactive (Real-time event-driven, ultra-low latency)

**Action Type**: Security Alert & Protection (not solicitation)

**Actors**:
- Customer (account owner)
- Security team
- Fraud prevention team
- EventBridge
- SMS/Email/Push gateway

---

## Actor Interaction Diagram

```
┌──────────┐         ┌──────────────┐         ┌─────────────────┐
│ Customer │         │Security Team │         │ Fraud Team      │
└────┬─────┘         └──────┬───────┘         └────────┬────────┘
     │                      │                          │
     │ 1. Login Attempt     │                          │
     │    (New Device)      │                          │
     ├──────────────────────┼──────────────────────────┤
     │                      │                          │
     │ 2. Anomaly Detected  │                          │
     │    Event Published   │                          │
     ├──────────────────────┼──────────────────────────┤
     │                      │                          │
     │                      │ 3. Monitors Fraud        │
     │                      │    Patterns              │
     │                      │<─────────────────────────┤
     │                      │                          │
     │ 4. Receives Security │                          │
     │    Alert (Immediate) │<─────────────────────────┤
     │<─────────────────────┤    (SMS + Email)         │
     │                      │                          │
     │ 5. Verifies Identity │                          │
     │    (2FA Challenge)   │                          │
     ├─────────────────────>│                          │
     │                      │                          │
     │ 6. Access Granted    │                          │
     │    or Blocked        │                          │
     │<─────────────────────┤                          │
     │                      │                          │
     │                      │ 7. Logs Security         │
     │                      │    Event                 │
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
│              (Ultra-Low Latency Fraud Detection)                         │
└─────────────────────────────────────────────────────────────────────────┘

T+0ms: Suspicious Login Attempt
┌─────────────────────────────────────┐
│      Authentication Service         │
│  - Login attempt from new device    │
│  - Location: Moscow, Russia         │
│  - Device: Unknown Android          │
│  - User's typical location: Seattle │
│  - Publishes event to EventBridge   │
└────────┬────────────────────────────┘
         │ Event: {
         │   customerId: "C123",
         │   loginAttemptId: "LA-456",
         │   deviceFingerprint: "unknown",
         │   ipAddress: "185.220.101.5",
         │   location: {
         │     country: "RU",
         │     city: "Moscow",
         │     coordinates: [55.7558, 37.6173]
         │   },
         │   userAgent: "Android 10",
         │   timestamp: "2026-01-18T18:30:00Z"
         │ }
         ▼

T+5ms: EventBridge Routes Event
┌─────────────────────────────────────┐
│         EventBridge Rule            │
│  Pattern: {                         │
│    source: "auth.service",          │
│    detail-type: "LoginAttempt",     │
│    detail: {                        │
│      deviceFingerprint: ["unknown"] │
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
         │   ├─ Verify customer account exists
         │   ├─ Check account status: Active
         │   └─ Result: PASS
         │
         ├─> Anomaly Detection Filter (12ms)
         │   ├─ Check location anomaly
         │   │   • Typical: Seattle, US
         │   │   • Current: Moscow, RU
         │   │   • Distance: 4,800 miles
         │   │   • Time since last login: 2 hours
         │   │   • Impossible travel: YES
         │   ├─ Check device anomaly
         │   │   • Known devices: 3 (iPhone, MacBook, iPad)
         │   │   • Current: Unknown Android
         │   │   • Device fingerprint match: NO
         │   ├─ Check IP reputation
         │   │   • IP: 185.220.101.5
         │   │   • Reputation: Suspicious (VPN/Proxy)
         │   └─ Result: PASS (high anomaly score)
         │
         ├─> Behavior Pattern Filter (10ms)
         │   ├─ Check login time pattern
         │   │   • Typical: 8 AM - 10 PM PST
         │   │   • Current: 6:30 PM PST (normal)
         │   ├─ Check login frequency
         │   │   • Typical: 2-3 times/day
         │   │   • Recent: 1 failed attempt 5 min ago
         │   └─ Result: PASS (suspicious pattern)
         │
         ├─> Account Value Filter (5ms)
         │   ├─ Check account value
         │   │   • Stored payment methods: 2
         │   │   • Account balance: $1,200
         │   │   • Recent transactions: $3,500
         │   └─ Result: PASS (high-value account)
         │
         ▼
    Candidate eligible for scoring
         │
         ▼

T+52ms: Score Lambda Processing
┌─────────────────────────────────────┐
│     Feature Store Client (12ms)     │
│  - Retrieve customer features       │
│    • typical_locations: ["Seattle"] │
│    • known_devices: 3               │
│    • account_age_days: 730          │
│    • recent_activity: normal        │
│  - Retrieve login features          │
│    • location_distance: 4800 miles  │
│    • device_match: false            │
│    • ip_reputation: 0.2 (suspicious)│
│    • impossible_travel: true        │
└────────┬────────────────────────────┘
         │ Features retrieved
         ▼
┌─────────────────────────────────────┐
│    SageMaker Scoring Provider (18ms)│
│  - Call fraud risk model            │
│  - Input: customer + login features │
│  - Output: fraud_risk = 0.95        │
│  - Risk level: CRITICAL             │
│  - Recommended action: BLOCK + 2FA  │
│  - Confidence: VERY HIGH            │
└────────┬────────────────────────────┘
         │ Score computed
         ▼
┌─────────────────────────────────────┐
│       Score Cache Repository (8ms)  │
│  - Cache score in DynamoDB          │
│  - TTL: 1 day                       │
│  - Key: CustomerId:LoginAttemptId   │
└────────┬────────────────────────────┘
         │ Score cached
         ▼

T+90ms: Store Lambda Processing + Immediate Action
┌─────────────────────────────────────┐
│   DynamoDB Candidate Repository     │
│  - Write candidate (10ms)           │
│  - Primary key: CustomerId:Program  │
│  - GSI1: Program:RiskLevel:Date     │
│  - TTL: 30 days from now            │
│  - Attributes:                      │
│    • fraud_risk: 0.95               │
│    • risk_level: "CRITICAL"         │
│    • action: "BLOCK_AND_ALERT"      │
│    • createdAt: T+0                 │
└────────┬────────────────────────────┘
         │ Candidate stored
         │
         ├─> IMMEDIATE ACTION TRIGGERED
         │
         ├─> Block Login Attempt
         │   └─ Auth service notified: DENY
         │
         └─> Trigger Multi-Channel Alert
             └─ SMS + Email sent immediately
         ▼

T+100ms: Reactive Processing Complete
Total Latency: 100ms
Login blocked, customer alerted
```

---

## Action Delivery Flow (Security Alert)

### Immediate Multi-Channel Alert

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      ACTION DELIVERY FLOW                                │
│                   (Immediate Security Alert)                             │
└─────────────────────────────────────────────────────────────────────────┘

T+100ms: Trigger Immediate Alert
┌─────────────────────────────────────┐
│      Channel Selection Logic        │
│  - Fraud risk: 0.95 (critical)      │
│  - Priority: URGENT                 │
│  - Channels: SMS + Email (parallel) │
│  - Action: Block + Alert            │
└────────┬────────────────────────────┘
         │
         ├─> SMS Channel (Primary)
         │
         ▼
┌─────────────────────────────────────┐
│      SMS Channel Adapter            │
│  - Retrieve candidate from DynamoDB │
│  - Format urgent SMS message        │
│  - Send to verified phone number    │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│         SMS Gateway                 │
│  Message: "SECURITY ALERT: Login    │
│  attempt from Moscow, Russia was    │
│  blocked. If this was you, verify:  │
│  [Verification Link]. If not, your  │
│  account is safe."                  │
└────────┬────────────────────────────┘
         │ SMS sent (T+110ms)
         │
         ├─> Email Channel (Secondary)
         │
         ▼
┌─────────────────────────────────────┐
│      Email Channel Adapter          │
│  - Format detailed security email   │
│  - Include login details            │
│  - Provide action steps             │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│      Email Campaign Service         │
│  Subject: "URGENT: Suspicious       │
│           Login Attempt Blocked"    │
│  Body:                              │
│  - "We blocked a login attempt"     │
│  - Location: Moscow, Russia         │
│  - Device: Unknown Android          │
│  - Time: 6:30 PM PST                │
│  - "Was this you?"                  │
│    • Yes → Verify identity          │
│    • No → Your account is safe      │
│  - Security recommendations         │
│  - Change password link             │
└────────┬────────────────────────────┘
         │ Email sent (T+150ms)
         ▼

T+150ms: Customer Receives Alerts
┌─────────────────────────────────────┐
│         Customer Phone              │
│  - SMS arrives within seconds       │
│  - Email arrives within seconds     │
│  - Push notification (if app open)  │
└────────┬────────────────────────────┘
         │
         │ Customer sees alerts immediately
         ▼

Scenario 1: Legitimate User (False Positive)
┌─────────────────────────────────────┐
│      Customer Clicks Verification   │
│  - "Yes, this was me"               │
│  - Redirected to 2FA challenge      │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│      2FA Challenge                  │
│  - SMS code sent to verified phone  │
│  - Customer enters code             │
│  - Identity verified                │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│      Access Granted                 │
│  - Login allowed                    │
│  - Device added to trusted list     │
│  - Location added to typical list   │
│  - Candidate marked "false_positive"│
└─────────────────────────────────────┘

Scenario 2: Fraud Attempt (True Positive)
┌─────────────────────────────────────┐
│      Customer Clicks "Not Me"       │
│  - Confirms fraud attempt           │
│  - Account locked immediately       │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│      Account Protection             │
│  - Account locked                   │
│  - All sessions terminated          │
│  - Password reset required          │
│  - Security team notified           │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│      Account Recovery Flow          │
│  - Customer verifies identity       │
│  - Resets password                  │
│  - Reviews recent activity          │
│  - Account unlocked                 │
│  - Candidate marked "true_positive" │
└─────────────────────────────────────┘

Alert Complete
Fraud Prevented: Yes
Account Protected: Yes
Customer Notified: Yes (within seconds)
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
│ Every    │ Unverified Alert Escalation                                │
│ Hour     │ - Query alerts not yet verified                            │
│ :00      │ - Filter: created >1 hour ago                              │
│          │ - Action: Escalate to security team                        │
│          │ - Duration: 5 minutes                                      │
└──────────┴────────────────────────────────────────────────────────────┘

Daily Schedule:
┌──────────┬────────────────────────────────────────────────────────────┐
│   Time   │                        Job                                 │
├──────────┼────────────────────────────────────────────────────────────┤
│ 3:00 AM  │ Fraud Pattern Analysis                                     │
│          │ - Aggregate yesterday's fraud attempts                     │
│          │ - Identify attack patterns                                 │
│          │ - Update fraud rules                                       │
│          │ - Alert security team                                      │
│          │ - Duration: 30 minutes                                     │
├──────────┼────────────────────────────────────────────────────────────┤
│ 6:00 AM  │ Fraud Prevention Report                                    │
│          │ - Calculate fraud prevention metrics                       │
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
│ 3:00 AM  │ - Retrain fraud risk model                                 │
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
- **Throughput**: 10,000 events/second
- **Filter Pass Rate**: 5% (only high-risk attempts)

### Delivery Metrics
- **SMS Delivery Rate**: 99%
- **Email Delivery Rate**: 98%
- **SMS Open Rate**: 95% (within 1 minute)
- **Email Open Rate**: 85% (within 5 minutes)

### Security Metrics
- **Fraud Detection Rate**: 95%
- **False Positive Rate**: 2%
- **True Positive Rate**: 98%
- **Account Takeover Prevention**: 98%
- **Average Response Time**: 30 seconds (customer verification)

### Business Metrics
- **Fraud Losses Prevented**: $500K+ annually
- **Cost per Alert**: $0.10
- **ROI**: 500,000% ($500K saved / $1K cost)
- **Customer Trust Score**: 4.8/5.0

### Technical Metrics
- **Serving API Latency**: P99 = 10ms (target: < 30ms)
- **Cache Hit Rate**: 90%
- **Error Rate**: 0.005%
- **Availability**: 99.99%
- **Deduplication Rate**: 99.95%

---

## Component Configuration

### Program Configuration
```yaml
programId: "suspicious-activity-alerts"
programName: "Suspicious Activity Alerts"
enabled: true
marketplaces: ["US", "UK", "DE", "FR", "JP", "IN", "BR", "AU"]

eventSources:
  - sourceId: "auth-service-events"
    type: "eventbridge"
    config:
      eventBusName: "auth-platform"
      eventPattern:
        source: ["auth.service"]
        detail-type: ["LoginAttempt"]
        detail:
          deviceFingerprint: ["unknown"]

scoringModels:
  - modelId: "fraud-risk-v5"
    endpoint: "sagemaker://fraud-risk-v5"
    features: ["location_distance", "device_match", "ip_reputation", "impossible_travel", "account_age"]

filterChain:
  - filterId: "trust"
    type: "trust"
    order: 1
  - filterId: "anomaly-detection"
    type: "business-rule"
    order: 2
    parameters:
      maxLocationDistance: 500  # miles
      maxTimeSinceLastLogin: 1  # hour (for impossible travel)
      requireDeviceMatch: false
      checkIPReputation: true
  - filterId: "behavior-pattern"
    type: "business-rule"
    order: 3
    parameters:
      checkLoginTimePattern: true
      checkLoginFrequency: true
  - filterId: "account-value"
    type: "business-rule"
    order: 4
    parameters:
      minAccountValue: 0  # Alert for all accounts

channels:
  - channelId: "sms"
    type: "sms"
    enabled: true
    priority: 1
    config:
      message: "SECURITY ALERT: Login attempt from {location} was blocked. If this was you, verify: {verificationLink}. If not, your account is safe."
      maxLength: 160
      immediate: true
  - channelId: "email"
    type: "email"
    enabled: true
    priority: 2
    config:
      templateId: "security-alert-v2"
      subject: "URGENT: Suspicious Login Attempt Blocked"
      immediate: true
  - channelId: "push"
    type: "push"
    enabled: true
    priority: 3
    config:
      title: "Security Alert"
      body: "Suspicious login attempt blocked from {location}"
      immediate: true

securityActions:
  enabled: true
  blockOnHighRisk: true
  riskThreshold: 0.8
  require2FA: true
  lockAccountOnConfirmedFraud: true

deduplication:
  enabled: true
  ttlDays: 1
  keyFormat: "{customerId}:{loginAttemptId}:{programId}"

alerting:
  enabled: true
  unverifiedThreshold: 1  # hour
  escalationRules:
    - condition: "fraud_risk > 0.95"
      action: "alert-security-team"
    - condition: "unverified > 1h"
      action: "escalate-to-manager"

candidateTTLDays: 30
```

---

## Summary

This use case demonstrates:
- ✅ **Ultra-low latency** (<100ms end-to-end)
- ✅ **Real-time fraud detection** (SageMaker endpoint)
- ✅ **Immediate action** (block + alert within seconds)
- ✅ **Multi-channel alerts** (SMS + email + push)
- ✅ **High accuracy** (95% detection, 2% false positives)
- ✅ **Massive savings** ($500K+ fraud losses prevented)
- ✅ **Customer trust** (4.8/5.0 trust score)
- ✅ **Account protection** (98% takeover prevention)
- ✅ **Scalability** (10,000 events/second)

**Key Insight**: This is NOT solicitation—it's proactive security protection. The platform detects threats and takes immediate action to protect customer accounts!
