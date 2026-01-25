# Use Case: Product Recommendations

## Overview

**Business Goal**: Increase revenue by $5M+ annually through ML-powered personalized product recommendations delivered at optimal times.

**Processing Mode**: Batch (Daily scheduled job)

**Action Type**: Personalized Recommendation

**Actors**:
- Customer (shopper)
- Marketing team
- Product team
- Data warehouse
- Email/In-App services

---

## Actor Interaction Diagram

```
┌──────────┐         ┌──────────────┐         ┌─────────────────┐
│ Customer │         │Marketing Team│         │ Product Team    │
└────┬─────┘         └──────┬───────┘         └────────┬────────┘
     │                      │                          │
     │ 1. Browses Products  │                          │
     │    (No Purchase)     │                          │
     ├──────────────────────┼──────────────────────────┤
     │                      │                          │
     │                      │ 2. Analyzes Browse       │
     │                      │    Behavior              │
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
     │<─────────────────────┤    (Email + In-App)      │
     │                      │                          │
     │ 6. Clicks Product    │                          │
     │    Makes Purchase    │                          │
     ├─────────────────────>│                          │
     │                      │                          │
     │                      │ 7. Tracks Conversion     │
     │                      │    Metrics               │
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

Step 1: ETL Lambda (2:00 AM - 2:30 AM)
┌──────────────────┐
│  Data Warehouse  │
│  (Athena)        │
└────────┬─────────┘
         │ Query: SELECT customer_id, product_id, view_count,
         │        time_spent, add_to_cart, purchase_status,
         │        category, price, timestamp
         │        FROM product_interactions
         │        WHERE date = YESTERDAY
         │        AND view_count >= 3
         │        AND purchase_status = 'not_purchased'
         ▼
┌─────────────────────────────────────┐
│      Athena Connector               │
│  - Extract 1M interaction records   │
│  - Filter: Viewed 3+ times          │
│  - Filter: Not purchased            │
│  - Map fields to candidate model    │
└────────┬────────────────────────────┘
         │ 1M raw candidates
         ▼
┌─────────────────────────────────────┐
│         ETL Lambda                  │
│  - Transform to unified model       │
│  - Add context (category, price)    │
│  - Calculate engagement metrics     │
│  - Deduplicate by customer+product  │
└────────┬────────────────────────────┘
         │ 800K unique candidates
         │ {customerId, productId, viewCount, ...}
         ▼

Step 2: Filter Lambda (2:30 AM - 2:50 AM)
┌─────────────────────────────────────┐
│      Filter Chain Executor          │
└────────┬────────────────────────────┘
         │
         ├─> Trust Filter
         │   ├─ Verify customer authenticity
         │   ├─ Check account status: Active
         │   └─ Result: Remove 40K (5%)
         │
         ├─> Purchase Intent Filter
         │   ├─ Check view count: >=3
         │   ├─ Check time spent: >=5 minutes
         │   ├─ Check add-to-cart: Yes or No
         │   └─ Result: Remove 400K (50%)
         │
         ├─> Inventory Filter
         │   ├─ Check product in stock
         │   ├─ Check product active
         │   └─ Result: Remove 60K (7.5%)
         │
         ├─> Eligibility Filter
         │   ├─ Customer hasn't purchased this product
         │   ├─ Customer hasn't opted out
         │   └─ Result: Remove 100K (12.5%)
         │
         ├─> Frequency Cap Filter
         │   ├─ Max 3 recommendation emails per week
         │   └─ Result: Remove 100K (12.5%)
         │
         ▼
    100,000 eligible candidates
         │
         ▼

Step 3: Score Lambda (2:50 AM - 3:20 AM)
┌─────────────────────────────────────┐
│     Feature Store Client            │
│  - Retrieve customer features       │
│    • purchase_history               │
│    • category_preferences           │
│    • price_sensitivity              │
│    • email_engagement               │
│  - Retrieve product features        │
│    • category                       │
│    • price                          │
│    • rating                         │
│    • popularity                     │
│    • similar_products_purchased     │
└────────┬────────────────────────────┘
         │ Features for 100,000 candidates
         ▼
┌─────────────────────────────────────┐
│    SageMaker Scoring Provider       │
│  - Call purchase intent model       │
│  - Call product affinity model      │
│  - Batch scoring (2000 at a time)   │
│  - Combined score: 0.0 - 1.0        │
│  - Discount recommendation          │
└────────┬────────────────────────────┘
         │ Scores computed
         ▼
┌─────────────────────────────────────┐
│       Score Cache Repository        │
│  - Cache scores in DynamoDB         │
│  - TTL: 7 days                      │
└────────┬────────────────────────────┘
         │ 100,000 scored candidates
         │ Score distribution:
         │   High (0.7-1.0): 30,000
         │   Medium (0.4-0.7): 50,000
         │   Low (0.0-0.4): 20,000
         ▼

Step 4: Store Lambda (3:20 AM - 3:35 AM)
┌─────────────────────────────────────┐
│   DynamoDB Candidate Repository     │
│  - Batch write (25 items per batch) │
│  - Primary key: CustomerId:Program  │
│  - GSI1: Program:Channel:Score      │
│  - GSI2: Program:Category:Date      │
│  - TTL: 7 days from now             │
└────────┬────────────────────────────┘
         │ 100,000 candidates stored
         ▼
┌─────────────────────────────────────┐
│         DynamoDB Tables             │
│  ┌─────────────────────────────┐   │
│  │ Candidates Table            │   │
│  │ - 100,000 new items         │   │
│  │ - Partitioned by customerId │   │
│  │ - Indexed by program+score  │   │
│  │ - Indexed by category+date  │   │
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘

Batch Job Complete: 3:35 AM
Total Time: 1 hour 35 minutes
Throughput: 8,421 candidates/minute
```

---

## Action Delivery Flow (Recommendation Campaign)

### Multi-Channel Delivery - 10:00 AM

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      ACTION DELIVERY FLOW                                │
│                   (Personalized Product Recommendations)                 │
└─────────────────────────────────────────────────────────────────────────┘

Channel 1: Email (10:00 AM)
┌─────────────────────────────────────┐
│      Serving API Query              │
│  Query: GetCandidatesForChannel     │
│  Params:                            │
│    - channel: "email"               │
│    - program: "product-recs"        │
│    - minScore: 0.6                  │
│    - limit: 50,000                  │
└────────┬────────────────────────────┘
         │ Query DynamoDB GSI1
         │ Top 50,000 candidates
         ▼
┌─────────────────────────────────────┐
│   Email Channel Adapter             │
│  - Group by customer                │
│  - Aggregate products per customer  │
│  - Max 5 products per email         │
│  - Personalize with browse history  │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│   Email Campaign Service            │
│  Subject: "Products you'll love -   │
│           Picked just for you"      │
│  Body:                              │
│  - "Based on your recent browsing"  │
│  - Product 1: [Image] [Price] [CTA] │
│  - Product 2: [Image] [Price] [CTA] │
│  - Product 3: [Image] [Price] [CTA] │
│  - "Limited time: 15% off"          │
│  - Discount code: RECO15            │
└────────┬────────────────────────────┘
         │ 50,000 emails sent
         ▼

Email Results (10:00 AM - 6:00 PM):
┌─────────────────────────────────────┐
│      Customer Engagement            │
│  - Opens: 20,000 (40%)              │
│  - Clicks: 10,000 (20%)             │
│  - Purchases: 7,500 (15%)           │
│  - Avg order value: $85             │
│  - Revenue: $637,500                │
└─────────────────────────────────────┘

---

Channel 2: In-App (Throughout Day)
┌─────────────────────────────────────┐
│      In-App Serving API             │
│  - Customer opens app               │
│  - API called on home screen load   │
│  - Query: GetTopRecommendations     │
│    • customerId: "C123"             │
│    • channel: "in-app"              │
│    • limit: 10                      │
└────────┬────────────────────────────┘
         │ Real-time query (12ms)
         ▼
┌─────────────────────────────────────┐
│      Personalized Feed              │
│  - Top 10 products displayed        │
│  - Sorted by score (descending)     │
│  - "Recommended for You" section    │
└────────┬────────────────────────────┘
         │
         │ 30,000 customers see recommendations
         ▼
┌─────────────────────────────────────┐
│      Customer Interaction           │
│  - 15,000 click products (50%)      │
│  - 4,500 add to cart (30% of clicks)│
│  - 3,000 purchase (20% of cart)     │
│  - Avg order value: $95             │
│  - Revenue: $285,000                │
└─────────────────────────────────────┘

In-App Results:
- Impressions: 30,000
- Clicks: 15,000 (50%)
- Purchases: 3,000 (10%)
- Revenue: $285,000

---

Overall Campaign Results:
- Total Candidates: 100,000
- Email Revenue: $637,500
- In-App Revenue: $285,000
- Total Revenue: $922,500
- Campaign Cost: $2,000
- ROI: 46,125%
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
│          │ - Process yesterday's browse activity                      │
│          │ - Duration: 1 hour 35 minutes                              │
│          │ - Output: 100,000 candidates                               │
├──────────┼────────────────────────────────────────────────────────────┤
│ 3:45 AM  │ Data Warehouse Export                                      │
│          │ - Export candidates to S3 (Parquet)                        │
│          │ - Update Glue catalog                                      │
│          │ - Duration: 20 minutes                                     │
├──────────┼────────────────────────────────────────────────────────────┤
│ 10:00 AM │ Email Campaign Delivery                                    │
│          │ - Retrieve top candidates (score >= 0.6)                   │
│          │ - Create personalized email campaigns                      │
│          │ - Send 50,000 emails                                       │
│          │ - Duration: 45 minutes                                     │
├──────────┼────────────────────────────────────────────────────────────┤
│ 6:00 PM  │ Recommendation Performance Analysis                        │
│          │ - Calculate conversion rates                               │
│          │ - Identify top-performing products                         │
│          │ - Update recommendation strategies                         │
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
│ 3:00 AM  │ - Retrain purchase intent model                            │
│          │ - Retrain product affinity model                           │
│          │ - Evaluate model performance                               │
│          │ - Deploy if improved                                       │
│          │ - Duration: 3 hours                                        │
└──────────┴────────────────────────────────────────────────────────────┘
```

---

## Metrics & Success Criteria

### Processing Metrics
- **Batch Duration**: 1 hour 35 minutes (target: < 2 hours)
- **Throughput**: 8,421 candidates/minute
- **Filter Pass Rate**: 12.5% (100K / 800K)
- **High Score Rate**: 30% (30K / 100K with score >= 0.7)

### Delivery Metrics
- **Email Send Rate**: 50,000 per day
- **Email Open Rate**: 40%
- **Email Click-Through Rate**: 20%
- **Email Conversion Rate**: 15%
- **In-App Impression Rate**: 30,000 per day
- **In-App Click Rate**: 50%
- **In-App Conversion Rate**: 10%

### Business Metrics
- **Daily Revenue**: $922,500
- **Annual Revenue**: $5.5M+ (assuming 6 days/week)
- **Cost per Conversion**: $0.19
- **Average Order Value**: $88
- **Customer Satisfaction**: 4.4/5.0

### Technical Metrics
- **Serving API Latency**: P99 = 12ms (target: < 30ms)
- **Cache Hit Rate**: 88%
- **Error Rate**: 0.008%
- **Availability**: 99.97%

---

## Component Configuration

### Program Configuration
```yaml
programId: "product-recommendations"
programName: "Product Recommendations"
enabled: true
marketplaces: ["US", "UK", "DE", "FR", "JP", "IN", "BR", "AU"]

dataConnectors:
  - connectorId: "athena-product-interactions"
    type: "athena"
    config:
      database: "ecommerce"
      table: "product_interactions"
      query: |
        SELECT customer_id, product_id, view_count, time_spent,
               add_to_cart, purchase_status, category, price
        FROM product_interactions
        WHERE date = ?
        AND view_count >= 3
        AND purchase_status = 'not_purchased'

scoringModels:
  - modelId: "purchase-intent-v5"
    endpoint: "sagemaker://purchase-intent-v5"
    features: ["view_count", "time_spent", "add_to_cart", "price_sensitivity"]
  - modelId: "product-affinity-v3"
    endpoint: "sagemaker://product-affinity-v3"
    features: ["category_preferences", "purchase_history", "similar_products"]

filterChain:
  - filterId: "trust"
    type: "trust"
    order: 1
  - filterId: "purchase-intent"
    type: "business-rule"
    order: 2
    parameters:
      minViewCount: 3
      minTimeSpent: 300  # seconds
  - filterId: "inventory"
    type: "business-rule"
    order: 3
    parameters:
      requireInStock: true
      requireActive: true
  - filterId: "eligibility"
    type: "eligibility"
    order: 4
  - filterId: "frequency-cap"
    type: "business-rule"
    order: 5
    parameters:
      maxEmailsPerWeek: 3

channels:
  - channelId: "email"
    type: "email"
    enabled: true
    priority: 1
    config:
      templateId: "product-recommendations-v3"
      sendTime: "10:00"
      timezone: "local"
      maxProductsPerEmail: 5
      includeDiscount: true
      discountPercent: 15
  - channelId: "in-app"
    type: "in-app"
    enabled: true
    priority: 2
    config:
      sectionName: "Recommended for You"
      maxProducts: 10
      refreshInterval: "realtime"

batchSchedule: "cron(0 2 * * ? *)"  # Daily at 2 AM UTC
candidateTTLDays: 7
```

---

## Summary

This use case demonstrates:
- ✅ **Batch processing** at scale (800K → 100K candidates)
- ✅ **Multi-stage pipeline** (ETL → Filter → Score → Store)
- ✅ **Dual ML models** (purchase intent + product affinity)
- ✅ **Multi-channel delivery** (email + in-app)
- ✅ **High conversion** (15% email, 10% in-app)
- ✅ **Massive revenue** ($5.5M+ annually)
- ✅ **Excellent ROI** (46,125% return on investment)
- ✅ **Cost efficiency** ($0.19 per conversion)
- ✅ **Real-time serving** (12ms P99 latency)

**Key Insight**: This is revenue generation through intelligent recommendations. The platform identifies purchase intent and delivers personalized product suggestions!
