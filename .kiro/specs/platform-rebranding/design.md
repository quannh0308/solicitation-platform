# Design: Platform Rebranding

## Overview

This design document outlines the approach for rebranding the platform from "General Solicitation Platform" to "Customer Engagement & Action Platform (CEAP)" through documentation updates only, with zero code changes.

---

## Design Principles

1. **Documentation-Only**: No code changes required
2. **Backward Compatible**: Existing package names remain unchanged
3. **Non-Disruptive**: Existing specs and work continue unaffected
4. **Clear Communication**: Migration notes explain the evolution
5. **Future-Proof**: New branding supports all use cases

---

## Architecture Impact

### What Changes: Documentation, Branding & Spec Directory
- README.md header and description
- Architecture document titles
- GitHub repository metadata
- Marketing materials
- Spec directory name: `solicitation-platform` → `customer-engagement-platform`
- FOUNDATION files: Update references from "solicitation" to "CEAP"

### What Stays the Same: Code & Implementation
- Package names: `com.solicitation.*`
- Module names: `solicitation-channels`, `solicitation-filters`, etc.
- Class names: `ChannelAdapter`, `Filter`, `ScoringProvider`, etc.
- Database schemas
- API endpoints
- Infrastructure code

---

## Component Design

### 1. Documentation Updates

#### 1.1 README.md
**Location**: `README.md`

**Changes**:
```markdown
# Customer Engagement & Action Platform (CEAP)
### Formerly: General Solicitation Platform

> Intelligent customer targeting and multi-channel action delivery at scale

## Evolution Note
This platform started as a "solicitation system" for collecting customer feedback.
We realized the architecture is far more powerful—it can deliver ANY customer 
action across ANY channel with ML-powered intelligence.

**Current capabilities**: 17 use cases, $280M+ annual impact, 5,286% average ROI

## What is CEAP?
[Rest of README...]
```

**Implementation**:
- Add new header with CEAP branding
- Add evolution note explaining the name change
- Keep all existing content
- Add link to REBRANDING-STRATEGY.md

---

#### 1.2 Architecture Documents
**Location**: `docs/VISUAL-ARCHITECTURE.md`, `docs/USE-CASES.md`

**Changes**:
- Update title from "General Solicitation Platform" to "Customer Engagement & Action Platform (CEAP)"
- Add migration note at top
- Update system overview description
- Keep all diagrams and technical content unchanged

**Implementation**:
```markdown
# Visual Architecture - Customer Engagement & Action Platform (CEAP)
> Formerly: General Solicitation Platform

## Migration Note
This platform has been rebranded from "General Solicitation Platform" to 
"Customer Engagement & Action Platform (CEAP)" to reflect its capabilities 
beyond solicitation. Package names remain unchanged for backward compatibility.

[Rest of document...]
```

---

#### 1.3 Use Case Documents
**Location**: `docs/usecases/*.md`, `docs/usecases/expansion/*.md`

**Changes**:
- Add migration note to README files
- Update references to "platform" to use CEAP
- Keep all use case content unchanged

**Implementation**:
- Add note to `docs/usecases/README.md`
- Add note to `docs/usecases/expansion/README.md`
- No changes to individual use case files (they're already correct)

---

### 2. GitHub Repository Updates

#### 2.1 Repository Description
**Location**: GitHub repository settings

**Current**: (Unknown)

**New**:
```
Customer Engagement & Action Platform (CEAP) - Intelligent customer targeting 
and multi-channel action delivery at scale. Supports fraud prevention, 
recommendations, proactive support, and more. Built with Kotlin, AWS Lambda, 
and EventBridge.
```

**Implementation**:
- Manual update in GitHub settings
- Document the change in commit message

---

#### 2.2 Repository Topics
**Location**: GitHub repository settings

**Add Topics**:
```
customer-engagement, action-delivery, machine-learning, multi-channel, 
event-driven, aws-lambda, kotlin, gradle, fraud-prevention, personalization, 
real-time, batch-processing, customer-experience, fintech, ecommerce
```

**Implementation**:
- Manual update in GitHub settings
- Document the change in commit message

---

#### 2.3 Repository Rename (Optional)
**Location**: GitHub repository settings

**Current**: `solicitation-platform`
**New**: `customer-engagement-platform`

**Implementation**:
- Manual rename in GitHub settings
- GitHub auto-redirects old URLs
- Update local git remote (optional, old URL still works)
- Document the change in README.md

**Decision**: To be decided by user

---

### 3. Migration Notes

#### 3.1 Standard Migration Note
**Usage**: Add to top of key documents

**Template**:
```markdown
> **Platform Rebranding Note**: This platform was formerly known as the 
> "General Solicitation Platform". We've rebranded to "Customer Engagement 
> & Action Platform (CEAP)" to better reflect its capabilities beyond 
> solicitation. Package names (`com.solicitation.*`) remain unchanged for 
> backward compatibility.
```

**Apply To**:
- README.md
- docs/VISUAL-ARCHITECTURE.md
- docs/USE-CASES.md
- docs/usecases/README.md
- docs/usecases/expansion/README.md

---

### 4. Spec Directory Renaming

#### 4.1 Rename Spec Directory
**Location**: `.kiro/specs/solicitation-platform/`

**Changes**:
- Rename directory to: `.kiro/specs/customer-engagement-platform/`
- Use `git mv` to preserve history
- Update any references to the old path

**Implementation**:
```bash
git mv .kiro/specs/solicitation-platform .kiro/specs/customer-engagement-platform
```

---

#### 4.2 Update FOUNDATION Files
**Location**: `.kiro/specs/customer-engagement-platform/FOUNDATION/`

**Files to Update**:
- `requirements.md`
- `design.md`
- `tasks.md`

**Changes**:
- Replace "General Solicitation Platform" with "Customer Engagement & Action Platform (CEAP)"
- Replace "solicitation" references with "customer engagement" or "CEAP" where appropriate
- Keep technical content and task details unchanged
- Add migration note at top of each file

**Implementation**:
```markdown
# Requirements: Customer Engagement & Action Platform (CEAP)
> Formerly: General Solicitation Platform

## Migration Note
This platform has been rebranded from "General Solicitation Platform" to 
"Customer Engagement & Action Platform (CEAP)" to reflect its capabilities 
beyond solicitation. This is a documentation update only—package names and 
code remain unchanged.

[Rest of document...]
```

---

#### 4.3 Update Active Spec Files
**Location**: `.kiro/specs/customer-engagement-platform/`

**Files to Update**:
- `requirements.md`
- `design.md`
- `tasks.md`
- `completed-tasks.md`

**Changes**:
- Add migration note at top
- Update title references to CEAP
- Keep all task details and technical content unchanged
- Ensure task tracking continues seamlessly

**Implementation**:
- Add migration note to each file
- Update headers to reference CEAP
- Preserve all task status and details

---

### 5. Branding Assets

#### 4.1 Tagline
**Selected**: "Intelligent customer engagement at scale"

**Alternatives**:
- "From data to action in milliseconds"
- "The customer engagement engine for modern businesses"

---

#### 4.2 Elevator Pitch
**30-Second Version**:
```
CEAP is an intelligent customer engagement platform that helps businesses 
deliver the right action to the right customer at the right time—whether 
that's a personalized recommendation, a fraud alert, a retention offer, or 
a proactive support message. Powered by ML, delivered across any channel, 
at enterprise scale.
```

---

#### 4.3 Value Propositions by Industry

**E-Commerce**:
- Recover abandoned carts (30% recovery)
- Deliver personalized recommendations ($5.5M revenue)
- Collect product reviews (5% submission)

**Financial Services**:
- Prevent credit card fraud ($5M saved)
- Target pre-approved loans ($8.1B volume)
- Deliver investment recommendations ($50.9B AUM)
- Prevent overdrafts ($3.5M saved)

**SaaS/Tech**:
- Prevent customer churn (30% reduction)
- Win back churned customers (17% win-back)
- Proactively resolve issues (98% resolution)

**Media/Entertainment**:
- Collect video ratings (40% submission)
- Gather music feedback (15% feedback)
- Drive event participation (87.5% attendance)

---

## Implementation Approach

### Phase 1: Documentation Updates (Week 1)

**Files to Update**:
1. `README.md` - Add CEAP header and evolution note
2. `docs/VISUAL-ARCHITECTURE.md` - Update title and add migration note
3. `docs/USE-CASES.md` - Update title and add migration note
4. `docs/usecases/README.md` - Add migration note
5. `docs/usecases/expansion/README.md` - Add migration note

**Approach**:
- Use `strReplace` to update headers and titles
- Add migration notes at the top of each file
- Keep all existing content intact
- Commit changes with clear message

---

### Phase 2: GitHub Updates (Week 2)

**Manual Updates in GitHub Settings**:
1. Update repository description
2. Add repository topics
3. Optionally rename repository

**Documentation**:
- Document changes in commit message
- Update README.md with any GitHub changes

---

### Phase 3: Verification (Week 2)

**Verify**:
1. All documentation uses CEAP consistently
2. Migration notes are present in key documents
3. GitHub repository reflects new branding
4. No code changes were made
5. Existing tests still pass
6. Existing spec files remain unchanged

---

## Testing Strategy

### Documentation Testing
- **Manual Review**: Read through all updated documents
- **Link Checking**: Verify all internal links still work
- **Consistency Check**: Ensure CEAP is used consistently

### Code Testing
- **No Changes**: No code testing required (no code changes)
- **Existing Tests**: Run existing test suite to verify nothing broke
- **Expected Result**: All tests pass (no changes to code)

### GitHub Testing
- **Old URL**: Verify old GitHub URL redirects to new URL (if renamed)
- **Clone**: Verify repository can be cloned with new URL
- **Links**: Verify external links to repository still work

---

## Rollback Plan

If rebranding causes issues:

### Documentation Rollback
- Revert commits using `git revert`
- Restore original documentation
- Timeline: 5 minutes

### GitHub Rollback
- Rename repository back to original name
- GitHub auto-redirects work in both directions
- Timeline: 2 minutes

### Code Rollback
- Not applicable (no code changes)

---

## Success Metrics

### Documentation Metrics
- ✅ All key documents updated with CEAP branding
- ✅ Migration notes present in 5+ documents
- ✅ Zero broken internal links
- ✅ Consistent terminology throughout

### GitHub Metrics
- ✅ Repository description updated
- ✅ 15+ topics added
- ✅ Repository optionally renamed
- ✅ Old URLs redirect correctly (if renamed)

### Code Metrics
- ✅ Zero code changes
- ✅ All existing tests pass
- ✅ No breaking changes
- ✅ Existing spec files unchanged

---

## Dependencies

- None (documentation-only effort)

---

## Risks & Mitigations

### Risk 1: Developer Confusion
**Impact**: Medium
**Probability**: Low
**Mitigation**: Clear migration notes explaining package names stay the same

### Risk 2: External Link Breakage
**Impact**: Low
**Probability**: Very Low
**Mitigation**: GitHub auto-redirects; document the rename

### Risk 3: Incomplete Updates
**Impact**: Low
**Probability**: Low
**Mitigation**: Checklist of all files to update; verification step

---

## References

- Rebranding Strategy: `docs/REBRANDING-STRATEGY.md`
- Platform Expansion Vision: `docs/PLATFORM-EXPANSION-VISION.md`
- Expansion Summary: `docs/EXPANSION-SUMMARY.md`
