# Use Case: Abandoned Cart Recovery

## Overview

**Business Goal**: Recover abandoned carts and increase revenue by $1M+ annually through intelligent, personalized recovery campaigns.

**Processing Mode**: Reactive (Real-time event-driven with delayed delivery)

**Action Type**: Purchase Incentive

**Actors**:
- Customer (shopper)
- Marketing team
- Sales team
- EventBridge
- Email/SMS/Push gateway

---

## Actor Interaction Diagram

```
┌──────────┐         ┌──────────────┐         ┌─────────────────┐
│ Customer │         │Marketing Team│         │ Sales Team      │
└────┬─────┘         └──────┬───────┘         └────────┬────────┘
     │                      │                          │
     │ 1. Adds Items        │                          │
     │    to Cart           │                          │
     ├──────────────────────┼──────────────────────────┤
     │                      │                          │
     │ 2. Abandons Cart     │                          │
     │    (Leaves Site)     │                          │
     ├──────────────────────┼──────────────────────────┤
     │                      │                          │
     │                      │ 3. Monitors Cart         │
     │                      │    Abandonment           │
     │                      │<─────────────────────────┤
     │                      │                          │
     │ 4. Receives Recovery │                          │
     │    Email (1 hour)    │<─────────────────────────┤
     │<─────────────────────┤    (With discount!)      │
     │                      │                          │
     │ 5. Clicks Email      │                          │
     │    Returns to Cart   │                          │
     ├─────────────────────>│                          │
     │                      │                          │
     │ 6. Completes         │                          │
     │    Purchase          │                          │
     ├─────────────────────>│                          │
     │                      │                          │
     │                      │ 7. Tracks Recovery       │
     │                      │    Metrics               │
     │                      ├─────────────────────────>│
     │                      │                          │
     ▼                      ▼                          ▼
```

---

## Reactive Data Flow (Real-Time Processing)

### Event-Driven Processing - <1 Second + 1 Hour Delay

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      REACTIVE DATA FLOW                                  │
│              (Real-Time Cart Abandonment Detection)                      │
└─────────────────────────────────────────────────────────────────────────┘

T+0ms: Customer Abandons Cart
┌─────────────────────────────────────┐
│      E-Commerce Website             │
│  - Customer adds 3 items to cart    │
│  - Cart value: $150                 │
│  - Customer leaves site             │
│  - Session timeout: 30 minutes      │
│  - Publishes event to EventBridge   │
└────────┬────────────────────────────┘
         │ Event: {
         │   customerId: "C456",
         │   cartId: "CART-789",
         │   cartValue: 150.00,
         │   itemCount: 3,
         │   items: [
         │     {id: "P1", name: "Wireless Headphones", price: 79.99},
         │     {id: "P2", name: "Phone Case", price: 29.99},
         │     {id: "P3", name: "Screen Protector", price: 39.99}
         │   ],
         │   abandonmentReason: "session_timeout",
         │   timestamp: "2026-01-18T16:45:00Z"
         │ }
         ▼

T+10ms: EventBridge Routes Event
┌─────────────────────────────────────┐
│         EventBridge Rule            │
│  Pattern: {                         │
│    source: "ecommerce.cart",        │
│    detail-type: "CartAbandoned",    │
│    detail: {                        │
│      cartValue: [50, 999999]        │
│    }                                │
│  }                                  │
│  Target: Reactive Lambda            │
└────────┬────────────────────────────┘
         │ Event matched, trigger Lambda
         ▼

T+20ms: Reactive Lambda Invoked
┌─────────────────────────────────────┐
│      Reactive Lambda Handler        │
│  1. Parse event (5ms)               │
│  2. Check deduplication (10ms)      │
│  3. Create candidate object (5ms)   │
└────────┬────────────────────────────┘
         │ Candidate created
         ▼

T+30ms: Filter Lambda Processing
┌─────────────────────────────────────┐
│      Filter Chain Executor          │
│  Parallel filter execution (120ms)  │
└────────┬────────────────────────────┘
         │
         ├─> Trust Filter (25ms)
         │   ├─ Verify customer authenticity
         │   ├─ Check account status: Active
         │   ├─ Check payment method on file: Yes
         │   └─ Result: PASS
         │
         ├─> Cart Value Filter (20ms)
         │   ├─ Check cart value: $150
         │   ├─ Minimum threshold: $50
         │   └─ Result: PASS
         │
         ├─> Eligibility Filter (35ms)
         │   ├─ Customer hasn't completed purchase
         │   ├─ Cart still active (not expired)
         │   ├─ Customer hasn't opted out
         │   └─ Result: PASS
         │
         ├─> Frequency Cap Filter (20ms)
         │   ├─ Check recovery emails in last 7 days
         │   ├─ Max 2 per week
         │   ├─ Current: 0
         │   └─ Result: PASS
         │
         ├─> Inventory Filter (20ms)
         │   ├─ Check if items still in stock
         │   ├─ All items available: Yes
         │   └─ Result: PASS
         │
         ▼
    Candidate eligible for scoring
         │
         ▼

T+150ms: Score Lambda Processing
┌─────────────────────────────────────┐
│     Feature Store Client (35ms)     │
│  - Retrieve customer features       │
│    • purchase_history: 12 orders    │
│    • avg_order_value: $85           │
│    • cart_abandonment_rate: 0.35    │
│    • email_engagement: 0.75         │
│  - Retrieve cart features           │
│    • cart_value: $150               │
│    • item_count: 3                  │
│    • category: "Electronics"        │
│    • discount_sensitivity: 0.8      │
└────────┬────────────────────────────┘
         │ Features retrieved
         ▼
┌─────────────────────────────────────┐
│    SageMaker Scoring Provider (60ms)│
│  - Call purchase intent model       │
│  - Input: customer + cart features  │
│  - Output: purchase_intent = 0.78   │
│  - Discount recommendation: 10%     │
│  - Confidence: HIGH                 │
└────────┬────────────────────────────┘
         │ Score computed
         ▼
┌─────────────────────────────────────┐
│       Score Cache Repository (25ms) │
│  - Cache score in DynamoDB          │
│  - TTL: 7 days                      │
│  - Key: CustomerId:CartId           │
└────────┬────────────────────────────┘
         │ Score cached
         ▼

T+270ms: Store Lambda Processing
┌─────────────────────────────────────┐
│   DynamoDB Candidate Repository     │
│  - Write candidate (35ms)           │
│  - Primary key: CustomerId:Program  │
│  - GSI1: Program:Channel:Score      │
│  - GSI2: Program:CartValue:Date     │
│  - TTL: 7 days from now             │
│  - Attributes:                      │
│    • purchase_intent: 0.78          │
│    • cart_value: 150.00             │
│    • discount_offer: 10%            │
│    • delivery_time: T+3600000ms     │
│    • createdAt: T+0                 │
└────────┬────────────────────────────┘
         │ Candidate stored with 1-hour delay
         ▼
┌─────────────────────────────────────┐
│    Delivery Scheduler               │
│  - Schedule email delivery for 1hr  │
│  - Use EventBridge Scheduler        │
│  - Target: Email Channel Lambda     │
│  - Payload: candidateId             │
└─────────────────────────────────────┘

T+305ms: Reactive Processing Complete
Total Latency: 305ms
Email scheduled for T+3600000ms (1 hour)

---

T+3600000ms (1 hour later): Delivery Time
┌─────────────────────────────────────┐
│   EventBridge Scheduler Trigger     │
│  - 1 hour delay complete            │
│  - Invoke Email Channel Lambda      │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│      Email Channel Lambda           │
│  - Retrieve candidate from DynamoDB │
│  - Verify cart still abandoned      │
│  - Format personalized email        │
│  - Send via email service           │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│      Email Campaign Service         │
│  - Send personalized recovery email │
│  - Track delivery status            │
└─────────────────────────────────────┘

End-to-End: Cart abandonment → Email delivery = 1 hour + 305ms
```

---

## Action Delivery Flow (Cart Recovery Campaign)

### Multi-Channel Recovery - 1 Hour, 24 Hours, 72 Hours

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      ACTION DELIVERY FLOW                                │
│                   (Multi-Touch Cart Recovery)                            │
└─────────────────────────────────────────────────────────────────────────┘

Touch 1: Email (1 Hour After Abandonment)
┌─────────────────────────────────────┐
│         Customer Inbox              │
│  Subject: "You left something       │
│           behind! 10% off inside"   │
│  Body:                              │
│  - "Hi [Name], we saved your cart"  │
│  - Product images with prices       │
│  - "Complete purchase now: 10% off" │
│  - Discount code: CART10            │
│  - "Offer expires in 24 hours"      │
│  - CTA: "Complete My Purchase"      │
└────────┬────────────────────────────┘
         │
         │ 40% customers open email
         ▼
┌─────────────────────────────────────┐
│      Customer Opens Email           │
│  - Tracking pixel fires             │
│  - Open event recorded              │
└────────┬────────────────────────────┘
         │
         │ 25% click CTA (62.5% of opens)
         ▼
┌─────────────────────────────────────┐
│    Cart Recovery Landing Page       │
│  - Pre-filled cart with items       │
│  - Discount auto-applied            │
│  - One-click checkout               │
└────────┬────────────────────────────┘
         │
         │ 15% complete purchase (60% of clicks)
         ▼
┌─────────────────────────────────────┐
│      Purchase Complete              │
│  - Revenue recovered: $135          │
│  - Discount given: $15              │
│  - Net revenue: $135                │
│  - Candidate marked "converted"     │
└─────────────────────────────────────┘

Touch 1 Results:
- Sent: 100%
- Opened: 40%
- Clicked: 25%
- Converted: 15%

---

Touch 2: SMS (24 Hours After Abandonment)
┌─────────────────────────────────────┐
│         Customer Phone              │
│  Message: "Hi [Name]! Your cart is  │
│  waiting. Use code CART10 for 10%   │
│  off. Shop now: [Link]"             │
└────────┬────────────────────────────┘
         │
         │ Only sent if email not converted
         │ 85% of original candidates
         ▼
┌─────────────────────────────────────┐
│      SMS Delivery                   │
│  - Sent to 85 customers             │
│  - 90% delivery rate                │
│  - 35% click rate                   │
│  - 10% conversion rate              │
└─────────────────────────────────────┘

Touch 2 Results:
- Sent: 85% (of original)
- Clicked: 35%
- Converted: 10%

---

Touch 3: Push Notification (72 Hours After Abandonment)
┌─────────────────────────────────────┐
│      Mobile App Notification        │
│  Title: "Last Chance!"              │
│  Body: "Your cart expires soon.     │
│        10% off ends today!"         │
│  Action: Open app to cart           │
└────────┬────────────────────────────┘
         │
         │ Only sent if SMS not converted
         │ 75% of original candidates
         ▼
┌─────────────────────────────────────┐
│      Push Notification Delivery     │
│  - Sent to 75 customers             │
│  - 95% delivery rate                │
│  - 20% open rate                    │
│  - 5% conversion rate               │
└─────────────────────────────────────┘

Touch 3 Results:
- Sent: 75% (of original)
- Opened: 20%
- Converted: 5%

---

Overall Campaign Results:
- Total Conversion Rate: 30% (15% + 10% + 5%)
- Revenue per 100 Carts: $4,500 ($150 avg × 30 conversions)
- Cost per 100 Carts: $50 (email + SMS + push)
- ROI: 9,000% ($4,500 / $50)
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
│ Every    │ Cart Expiration Check                                      │
│ Hour     │ - Query carts abandoned >7 days ago                        │
│ :00      │ - Mark as expired                                          │
│          │ - Remove from active recovery                              │
│          │ - Duration: 5 minutes                                      │
└──────────┴────────────────────────────────────────────────────────────┘

Daily Schedule:
┌──────────┬────────────────────────────────────────────────────────────┐
│   Time   │                        Job                                 │
├──────────┼────────────────────────────────────────────────────────────┤
│ 3:00 AM  │ Recovery Performance Analysis                              │
│          │ - Aggregate yesterday's recovery metrics                   │
│          │ - Calculate conversion rates by channel                    │
│          │ - Identify optimization opportunities                      │
│          │ - Duration: 30 minutes                                     │
├──────────┼────────────────────────────────────────────────────────────┤
│ 6:00 AM  │ Revenue Impact Report                                      │
│          │ - Calculate recovered revenue                              │
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
│ 3:00 AM  │ - Retrain purchase intent model                            │
│          │ - Evaluate model performance                               │
│          │ - Deploy if improved                                       │
│          │ - Duration: 2 hours                                        │
└──────────┴────────────────────────────────────────────────────────────┘
```

---

## Metrics & Success Criteria

### Processing Metrics
- **Reactive Latency**: 305ms average (target: < 500ms)
- **P99 Latency**: 480ms (target: < 1000ms)
- **Throughput**: 5,000 events/second
- **Filter Pass Rate**: 60% (qualified carts)

### Delivery Metrics
- **Email Delivery Rate**: 98%
- **SMS Delivery Rate**: 95%
- **Push Delivery Rate**: 97%
- **Email Open Rate**: 40%
- **SMS Click Rate**: 35%
- **Push Open Rate**: 20%

### Conversion Metrics
- **Touch 1 (Email) Conversion**: 15%
- **Touch 2 (SMS) Conversion**: 10%
- **Touch 3 (Push) Conversion**: 5%
- **Overall Conversion Rate**: 30%
- **Average Cart Value**: $150
- **Average Recovered Revenue**: $135 (after discount)

### Business Metrics
- **Annual Recovered Revenue**: $1.2M
- **Cost per Recovery**: $1.50
- **ROI**: 9,000%
- **Customer Satisfaction**: 4.5/5.0

### Technical Metrics
- **Serving API Latency**: P99 = 18ms (target: < 30ms)
- **Cache Hit Rate**: 85%
- **Error Rate**: 0.01%
- **Availability**: 99.96%
- **Deduplication Rate**: 99.8%

---

## Component Configuration

### Program Configuration
```yaml
programId: "abandoned-cart-recovery"
programName: "Abandoned Cart Recovery"
enabled: true
marketplaces: ["US", "UK", "DE", "FR", "JP", "IN", "BR", "AU"]

eventSources:
  - sourceId: "cart-abandonment-events"
    type: "eventbridge"
    config:
      eventBusName: "ecommerce-platform"
      eventPattern:
        source: ["ecommerce.cart"]
        detail-type: ["CartAbandoned"]
        detail:
          cartValue: [50, 999999]  # Min $50

scoringModels:
  - modelId: "purchase-intent-v4"
    endpoint: "sagemaker://purchase-intent-v4"
    features: ["purchase_history", "cart_value", "email_engagement", "discount_sensitivity"]

filterChain:
  - filterId: "trust"
    type: "trust"
    order: 1
  - filterId: "cart-value"
    type: "business-rule"
    order: 2
    parameters:
      minCartValue: 50
  - filterId: "eligibility"
    type: "eligibility"
    order: 3
  - filterId: "frequency-cap"
    type: "business-rule"
    order: 4
    parameters:
      maxRecoveryEmailsPerWeek: 2
  - filterId: "inventory"
    type: "business-rule"
    order: 5
    parameters:
      requireInStock: true

channels:
  - channelId: "email"
    type: "email"
    enabled: true
    priority: 1
    config:
      templateId: "cart-recovery-v3"
      delayMinutes: 60
      subject: "You left something behind! {discountPercent}% off inside"
      includeDiscount: true
  - channelId: "sms"
    type: "sms"
    enabled: true
    priority: 2
    config:
      delayHours: 24
      message: "Hi {customerName}! Your cart is waiting. Use code {discountCode} for {discountPercent}% off. Shop now: {cartLink}"
      onlyIfNotConverted: true
  - channelId: "push"
    type: "push"
    enabled: true
    priority: 3
    config:
      delayHours: 72
      title: "Last Chance!"
      body: "Your cart expires soon. {discountPercent}% off ends today!"
      onlyIfNotConverted: true

discountStrategy:
  enabled: true
  defaultDiscount: 10
  dynamicDiscounting: true
  maxDiscount: 20

deduplication:
  enabled: true
  ttlDays: 7
  keyFormat: "{customerId}:{cartId}:{programId}"

candidateTTLDays: 7
```

---

## Summary

This use case demonstrates:
- ✅ **Reactive processing** (<350ms end-to-end latency)
- ✅ **Multi-touch campaign** (email → SMS → push)
- ✅ **ML-powered scoring** (purchase intent model)
- ✅ **Dynamic discounting** (personalized offers)
- ✅ **High conversion** (30% overall recovery rate)
- ✅ **Massive ROI** (9,000% return on investment)
- ✅ **Revenue impact** ($1.2M+ annually)
- ✅ **Cost efficiency** ($1.50 per recovery)
- ✅ **Scalability** (5,000 events/second)

**Key Insight**: This is revenue recovery through personalized incentives. The platform identifies abandoned carts and delivers targeted offers to complete purchases!
