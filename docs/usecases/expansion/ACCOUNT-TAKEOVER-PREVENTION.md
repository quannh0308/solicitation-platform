# Use Case: Account Takeover Prevention

## Overview

**Business Goal**: Prevent account takeovers through real-time detection and immediate protective actions, achieving 98% prevention rate and saving $800K+ annually.

**Processing Mode**: Reactive (Real-time event-driven, ultra-low latency)

**Action Type**: Account Protection

**Actors**:
- Customer (account owner)
- Security team
- Fraud prevention team
- EventBridge
- SMS/Email gateway

---

## Actor Interaction Diagram

```
┌──────────┐         ┌──────────────┐         ┌─────────────────┐
│ Customer │         │Security Team │         │ Fraud Team      │
└────┬─────┘         └──────┬───────┘         └────────┬────────┘
     │                      │                          │
     │ 1. Attacker Changes  │                          │
     │    Password + Email  │                          │
     ├──────────────────────┼──────────────────────────┤
     │                      │                          │
     │ 2. Takeover Detected │                          │
     │    Event Published   │                          │
     ├──────────────────────┼──────────────────────────┤
     │                      │                          │
     │                      │ 3. Monitors Takeover     │
     │                      │    Patterns              │
     │                      │<─────────────────────────┤
     │                      │                          │
     │ 4. Account Locked    │                          │
     │    Immediately       │<─────────────────────────┤
     │<─────────────────────┤    (Automatic)           │
     │                      │                          │
     │ 5. Receives Alert    │                          │
     │    (SMS + Email)     │<─────────────────────────┤
     │<─────────────────────┤                          │
     │                      │                          │
     │ 6. Verifies Identity │                          │
     │    Recovers Account  │                          │
     ├─────────────────────>│                          │
     │                      │                          │
     │                      │ 7. Logs Security         │
     │                      │    Incident              │
     │                      ├─────────────────────────>│
     │                      │                          │
     ▼                      ▼                          ▼
```

---

## Reactive Data Flow (Real-Time Processing)

### Event-Driven Processing - <200ms End-to-End

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      REACTIVE DATA FLOW                                  │
│              (Ultra-Low Latency Takeover Detection)                      │
└─────────────────────────────────────────────────────────────────────────┘

T+0ms: Suspicious Account Changes
┌─────────────────────────────────────┐
│      Account Management Service     │
│  - Password changed                 │
│  - Email changed (5 minutes later)  │
│  - Recovery phone removed           │
│  - Payment method added             │
│  - Publishes event to EventBridge   │
└────────┬────────────────────────────┘
         │ Event: {
         │   customerId: "C456",
         │   accountChangeId: "AC-789",
         │   changes: [
         │     {type: "password", timestamp: "T+0"},
         │     {type: "email", timestamp: "T+300000"},
         │     {type: "recovery_phone", action: "removed", timestamp: "T+360000"},
         │     {type: "payment_method", action: "added", timestamp: "T+420000"}
         │   ],
         │   changeVelocity: "high",
         │   ipAddress: "203.0.113.42",
         │   location: "Lagos, Nigeria",
         │   deviceFingerprint: "unknown",
         │   timestamp: "2026-01-18T20:15:00Z"
         │ }
         ▼

T+5ms: EventBridge Routes Event
┌─────────────────────────────────────┐
│         EventBridge Rule            │
│  Pattern: {                         │
│    source: "account.management",    │
│    detail-type: "AccountChanged",   │
│    detail: {                        │
│      changeVelocity: ["high"]       │
│    }                                │
│  }                                  │
│  Target: Reactive Lambda            │
└────────┬────────────────────────────┘
         │ Event matched, trigger Lambda
         ▼

T+10ms: Reactive Lambda Invoked
┌─────────────────────────────────────┐
│      Reactive Lambda Handler        │
│  1. Parse event (3ms)               │
│  2. Check deduplication (4ms)       │
│  3. Create candidate object (3ms)   │
└────────┬────────────────────────────┘
         │ Candidate created
         ▼

T+20ms: Filter Lambda Processing
┌─────────────────────────────────────┐
│      Filter Chain Executor          │
│  Parallel filter execution (60ms)   │
└────────┬────────────────────────────┘
         │
         ├─> Trust Filter (10ms)
         │   ├─ Verify customer account exists
         │   ├─ Check account status: Active
         │   └─ Result: PASS
         │
         ├─> Takeover Pattern Filter (20ms)
         │   ├─ Check change velocity
         │   │   • Password + Email + Phone in 7 minutes
         │   │   • Velocity: CRITICAL
         │   ├─ Check change sequence
         │   │   • Classic takeover pattern detected
         │   ├─ Check location anomaly
         │   │   • Typical: Seattle, US
         │   │   • Current: Lagos, Nigeria
         │   │   • Distance: 7,800 miles
         │   └─ Result: PASS (takeover pattern detected)
         │
         ├─> Device Fingerprint Filter (15ms)
         │   ├─ Check device match
         │   │   • Known devices: 3
         │   │   • Current: Unknown
         │   ├─ Check IP reputation
         │   │   • IP: 203.0.113.42
         │   │   • Reputation: Known fraud IP
         │   └─ Result: PASS (device mismatch)
         │
         ├─> Account Value Filter (15ms)
         │   ├─ Check account value
         │   │   • Stored payment methods: 2
         │   │   • Account balance: $500
         │   │   • Recent transactions: $2,000
         │   │   • Lifetime value: $5,000
         │   └─ Result: PASS (high-value account)
         │
         ▼
    Candidate eligible for scoring
         │
         ▼

T+80ms: Score Lambda Processing
┌─────────────────────────────────────┐
│     Feature Store Client (20ms)     │
│  - Retrieve customer features       │
│    • typical_locations: ["Seattle"] │
│    • known_devices: 3               │
│    • account_age_days: 1095         │
│    • security_incidents: 0          │
│  - Retrieve change features         │
│    • change_velocity: "critical"    │
│    • change_count: 4 in 7 minutes   │
│    • location_distance: 7800 miles  │
│    • device_match: false            │
│    • ip_reputation: 0.05 (fraud)    │
└────────┬────────────────────────────┘
         │ Features retrieved
         ▼
┌─────────────────────────────────────┐
│    SageMaker Scoring Provider (30ms)│
│  - Call account takeover model      │
│  - Input: customer + change features│
│  - Output: takeover_risk = 0.98     │
│  - Risk level: CRITICAL             │
│  - Recommended action: LOCK + ALERT │
│  - Confidence: VERY HIGH            │
└────────┬────────────────────────────┘
         │ Score computed
         ▼
┌─────────────────────────────────────┐
│       Score Cache Repository (15ms) │
│  - Cache score in DynamoDB          │
│  - TTL: 30 days                     │
│  - Key: CustomerId:ChangeId         │
└────────┬────────────────────────────┘
         │ Score cached
         ▼

T+145ms: Store Lambda + Immediate Protection
┌─────────────────────────────────────┐
│   DynamoDB Candidate Repository     │
│  - Write candidate (15ms)           │
│  - Primary key: CustomerId:Program  │
│  - GSI1: Program:RiskLevel:Date     │
│  - TTL: 30 days from now            │
│  - Attributes:                      │
│    • takeover_risk: 0.98            │
│    • risk_level: "CRITICAL"         │
│    • action: "LOCK_AND_ALERT"       │
│    • createdAt: T+0                 │
└────────┬────────────────────────────┘
         │ Candidate stored
         │
         ├─> IMMEDIATE PROTECTION ACTIONS
         │
         ├─> Lock Account
         │   ├─ Account service notified: LOCK
         │   ├─ All sessions terminated
         │   └─ Password reset required
         │
         ├─> Revert Changes
         │   ├─ Restore original email
         │   ├─ Restore recovery phone
         │   └─ Remove fraudulent payment method
         │
         └─> Trigger Multi-Channel Alert
             └─ SMS + Email to original contact info
         ▼

T+200ms: Reactive Processing Complete
Total Latency: 200ms
Account locked, changes reverted, customer alerted
```

---

## Action Delivery Flow (Account Protection)

### Immediate Multi-Channel Alert + Recovery

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      ACTION DELIVERY FLOW                                │
│                   (Immediate Account Protection)                         │
└─────────────────────────────────────────────────────────────────────────┘

T+200ms: Trigger Immediate Alert
┌─────────────────────────────────────┐
│      Channel Selection Logic        │
│  - Takeover risk: 0.98 (critical)   │
│  - Priority: URGENT                 │
│  - Channels: SMS + Email (parallel) │
│  - Action: Lock + Revert + Alert    │
└────────┬────────────────────────────┘
         │
         ├─> SMS Channel (Primary)
         │
         ▼
┌─────────────────────────────────────┐
│      SMS Channel Adapter            │
│  - Send to ORIGINAL phone number    │
│  - Format urgent SMS message        │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│         SMS Gateway                 │
│  Message: "SECURITY ALERT: We       │
│  detected unauthorized changes to   │
│  your account. We've locked it and  │
│  reverted changes. Verify identity: │
│  [Recovery Link]"                   │
└────────┬────────────────────────────┘
         │ SMS sent (T+210ms)
         │
         ├─> Email Channel (Secondary)
         │
         ▼
┌─────────────────────────────────────┐
│      Email Channel Adapter          │
│  - Send to ORIGINAL email address   │
│  - Format detailed security email   │
│  - Include recovery instructions    │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│      Email Campaign Service         │
│  Subject: "URGENT: Account Takeover │
│           Attempt Blocked"          │
│  Body:                              │
│  - "We detected unauthorized changes"│
│  - Changes detected:                │
│    • Password changed               │
│    • Email changed                  │
│    • Recovery phone removed         │
│    • Payment method added           │
│  - "We've protected your account:"  │
│    • Account locked                 │
│    • Changes reverted               │
│    • Sessions terminated            │
│  - "To recover your account:"       │
│    1. Click recovery link           │
│    2. Verify identity (2FA)         │
│    3. Reset password                │
│    4. Review recent activity        │
│  - Security recommendations         │
└────────┬────────────────────────────┘
         │ Email sent (T+250ms)
         ▼

T+250ms: Customer Receives Alerts
┌─────────────────────────────────────┐
│         Customer Phone              │
│  - SMS arrives within seconds       │
│  - Email arrives within seconds     │
└────────┬────────────────────────────┘
         │
         │ Customer sees alerts immediately
         │ (within 1 minute)
         ▼

T+60000ms (1 minute later): Customer Recovery
┌─────────────────────────────────────┐
│      Customer Clicks Recovery Link  │
│  - Redirected to identity verification│
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│      Identity Verification          │
│  Step 1: SMS Code                   │
│  - Code sent to original phone      │
│  - Customer enters code             │
│  - Verified ✓                       │
│                                     │
│  Step 2: Security Questions         │
│  - "What's your mother's maiden     │
│     name?"                          │
│  - Customer answers correctly       │
│  - Verified ✓                       │
│                                     │
│  Step 3: Photo ID (Optional)        │
│  - Upload driver's license          │
│  - AI verification                  │
│  - Verified ✓                       │
└────────┬────────────────────────────┘
         │ Identity confirmed
         ▼
┌─────────────────────────────────────┐
│      Account Recovery               │
│  - Account unlocked                 │
│  - Customer resets password         │
│  - Customer reviews recent activity │
│  - Customer confirms no unauthorized│
│    transactions                     │
│  - Recovery complete                │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│    Candidate Status Update          │
│  - Mark candidate as "recovered"    │
│  - Record alert timestamp           │
│  - Record recovery timestamp        │
│  - Update metrics                   │
│  - Recovery time: 5 minutes         │
│  - Takeover prevented: Yes          │
└─────────────────────────────────────┘

Protection Complete
Account Takeover Prevented: Yes
Customer Notified: Within seconds
Account Recovered: 5 minutes
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
│ Every    │ Unrecovered Account Escalation                             │
│ Hour     │ - Query locked accounts not yet recovered                  │
│ :00      │ - Filter: locked >2 hours ago                              │
│          │ - Action: Escalate to security team                        │
│          │ - Duration: 5 minutes                                      │
└──────────┴────────────────────────────────────────────────────────────┘

Daily Schedule:
┌──────────┬────────────────────────────────────────────────────────────┐
│   Time   │                        Job                                 │
├──────────┼────────────────────────────────────────────────────────────┤
│ 3:00 AM  │ Takeover Pattern Analysis                                  │
│          │ - Aggregate yesterday's takeover attempts                  │
│          │ - Identify attack patterns                                 │
│          │ - Update fraud rules                                       │
│          │ - Alert security team                                      │
│          │ - Duration: 30 minutes                                     │
├──────────┼────────────────────────────────────────────────────────────┤
│ 6:00 AM  │ Security Metrics Dashboard                                 │
│          │ - Calculate prevention metrics                             │
│          │ - Generate executive report                                │
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
│ 3:00 AM  │ - Retrain account takeover model                           │
│          │ - Evaluate model performance                               │
│          │ - Deploy if improved                                       │
│          │ - Duration: 2 hours                                        │
└──────────┴────────────────────────────────────────────────────────────┘
```

---

## Metrics & Success Criteria

### Processing Metrics
- **Reactive Latency**: 200ms average (target: < 500ms)
- **P99 Latency**: 350ms (target: < 1000ms)
- **Throughput**: 5,000 events/second
- **Filter Pass Rate**: 8% (high-risk patterns only)

### Delivery Metrics
- **SMS Delivery Rate**: 99%
- **Email Delivery Rate**: 98%
- **SMS Open Rate**: 98% (within 1 minute)
- **Email Open Rate**: 90% (within 5 minutes)

### Security Metrics
- **Takeover Detection Rate**: 98%
- **False Positive Rate**: 1%
- **True Positive Rate**: 99%
- **Account Recovery Rate**: 95%
- **Average Recovery Time**: 5 minutes

### Business Metrics
- **Takeover Attempts Blocked**: 10,000 per year
- **Financial Loss Prevented**: $800K+ annually ($80 avg per attempt)
- **Cost per Protection**: $0.15
- **ROI**: 533,333% ($800K saved / $1.5K cost)
- **Customer Trust Score**: 4.9/5.0

### Technical Metrics
- **Serving API Latency**: P99 = 10ms (target: < 30ms)
- **Cache Hit Rate**: 92%
- **Error Rate**: 0.003%
- **Availability**: 99.99%
- **Deduplication Rate**: 99.98%

---

## Component Configuration

### Program Configuration
```yaml
programId: "account-takeover-prevention"
programName: "Account Takeover Prevention"
enabled: true
marketplaces: ["US", "UK", "DE", "FR", "JP", "IN", "BR", "AU"]

eventSources:
  - sourceId: "account-management-events"
    type: "eventbridge"
    config:
      eventBusName: "account-platform"
      eventPattern:
        source: ["account.management"]
        detail-type: ["AccountChanged"]
        detail:
          changeVelocity: ["high", "critical"]

scoringModels:
  - modelId: "account-takeover-v4"
    endpoint: "sagemaker://account-takeover-v4"
    features: ["change_velocity", "location_distance", "device_match", "ip_reputation", "change_sequence"]

filterChain:
  - filterId: "trust"
    type: "trust"
    order: 1
  - filterId: "takeover-pattern"
    type: "business-rule"
    order: 2
    parameters:
      criticalChanges: ["password", "email", "recovery_phone"]
      maxChangeWindow: 10  # minutes
      minChangesForAlert: 2
  - filterId: "device-fingerprint"
    type: "business-rule"
    order: 3
    parameters:
      requireDeviceMatch: false
      checkIPReputation: true
      minIPReputationScore: 0.3
  - filterId: "account-value"
    type: "business-rule"
    order: 4
    parameters:
      minAccountValue: 0  # Protect all accounts

channels:
  - channelId: "sms"
    type: "sms"
    enabled: true
    priority: 1
    config:
      message: "SECURITY ALERT: We detected unauthorized changes to your account. We've locked it and reverted changes. Verify identity: {recoveryLink}"
      maxLength: 160
      immediate: true
      sendToOriginalPhone: true
  - channelId: "email"
    type: "email"
    enabled: true
    priority: 2
    config:
      templateId: "account-takeover-v2"
      subject: "URGENT: Account Takeover Attempt Blocked"
      immediate: true
      sendToOriginalEmail: true

protectionActions:
  enabled: true
  autoLock: true
  autoRevert: true
  riskThreshold: 0.8
  actions:
    - action: "lock_account"
      condition: "takeover_risk > 0.8"
    - action: "revert_changes"
      condition: "takeover_risk > 0.9"
    - action: "terminate_sessions"
      condition: "takeover_risk > 0.8"
    - action: "alert_security_team"
      condition: "takeover_risk > 0.95"

recoveryFlow:
  enabled: true
  verificationSteps:
    - type: "sms_code"
      required: true
    - type: "security_questions"
      required: true
    - type: "photo_id"
      required: false
      condition: "takeover_risk > 0.95"

deduplication:
  enabled: true
  ttlDays: 30
  keyFormat: "{customerId}:{accountChangeId}:{programId}"

candidateTTLDays: 30
```

---

## Summary

This use case demonstrates:
- ✅ **Ultra-low latency** (<200ms end-to-end)
- ✅ **Real-time takeover detection** (SageMaker endpoint)
- ✅ **Immediate protection** (lock + revert within 200ms)
- ✅ **Multi-channel alerts** (SMS + email to original contact info)
- ✅ **High accuracy** (98% detection, 1% false positives)
- ✅ **Massive savings** ($800K+ financial losses prevented)
- ✅ **Fast recovery** (5 minutes average)
- ✅ **Customer trust** (4.9/5.0 trust score)
- ✅ **Scalability** (5,000 events/second)

**Key Insight**: This is proactive account protection. The platform detects takeover attempts and takes immediate protective action to secure customer accounts!
