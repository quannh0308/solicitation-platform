# Design Document: Final Documentation Cleanup

## Overview

This design outlines the strategy for cleaning up the remaining ~1,891 "solicitation" references in the codebase. After analysis, these references fall into three categories:

1. **Obsolete Specs** (~1,500 references) - Old spec directories with deprecated package names
2. **Archive Documents** (~300 references) - Historical migration documents
3. **Summary/Meta Documents** (~91 references) - Documents describing the rebrand

## Strategy

### Category 1: Obsolete Spec Directories

**Location**: `.kiro/specs/customer-engagement-platform/`

**Analysis**: This spec directory contains the original implementation specs from when the platform used `com.solicitation.*` packages. The code has since been migrated to `com.ceap.*`, making these specs obsolete.

**Decision**: **DELETE** the entire directory

**Rationale**:
- The specs reference old package names that no longer exist
- The implementation is complete and code is working
- Keeping obsolete specs causes confusion
- Current specs (complete-ceap-infrastructure-rebrand, complete-solicitation-rename) are up-to-date

**Alternative**: If there's valuable design documentation, extract it to active docs first

### Category 2: Archive Documents

**Location**: `docs/archive/`

**Analysis**: Contains 25 historical documents from various migrations (Maven→Gradle, Java→Kotlin, CDK migration, etc.)

**Decision**: **SELECTIVE CLEANUP**

**Keep** (valuable historical context):
- `README.md` - Archive index
- `ARCHITECTURE-DECISION.md` - Important architectural decisions
- `MULTI-MODULE-ARCHITECTURE.md` - Architecture documentation
- `TECHNOLOGY-AUDIT-AND-RECOMMENDATIONS.md` - Technology decisions

**Delete** (redundant task summaries):
- All `TASK-*-SUMMARY.md` files (completed work, no longer relevant)
- All `*-COMPLETE.md` files (migration completion summaries)
- `NEXT-STEPS.md` (outdated)
- `WHERE-WE-ARE-NOW.md` (outdated)
- `SOLICITATION-REFERENCES-ANALYSIS.md` (analysis is complete)

**Update** (keep but clean):
- Update remaining files to use CEAP terminology where it doesn't affect historical accuracy

### Category 3: Summary Documents

**Location**: Root directory

**Files**:
- `DOCUMENTATION-REVIEW-SUMMARY.md`
- `REFACTORING-COMPLETE-SUMMARY.md`
- `docs/REBRANDING-STRATEGY.md`

**Decision**: **UPDATE**

**Strategy**:
- Keep before/after examples but minimize repetition
- Use "legacy terminology" instead of repeatedly saying "solicitation"
- Focus on outcomes rather than detailed change lists

## Implementation Plan

### Phase 1: Delete Obsolete Specs
1. Review `.kiro/specs/customer-engagement-platform/` for any valuable content
2. Extract any valuable design docs to active documentation
3. Delete the entire directory

### Phase 2: Clean Up Archive
1. Delete redundant task summary files
2. Delete migration completion summaries
3. Keep architectural decision documents
4. Update remaining archive docs to use CEAP terminology

### Phase 3: Update Summary Documents
1. Simplify DOCUMENTATION-REVIEW-SUMMARY.md
2. Simplify REFACTORING-COMPLETE-SUMMARY.md
3. Update REBRANDING-STRATEGY.md to focus on decision rationale

### Phase 4: Verify
1. Count remaining "solicitation" references
2. Verify all remaining references are in appropriate contexts
3. Ensure active code and docs are 100% clean

## Expected Outcome

After cleanup:
- **Obsolete specs**: 0 references (directory deleted)
- **Archive**: ~50 references (in preserved architectural docs)
- **Summary docs**: ~20 references (in before/after examples)
- **Rebrand specs**: ~100 references (documenting the rebrand itself)
- **Test files**: ~20 references (checking for old names)

**Total**: ~190 references (down from 1,891) - all in appropriate historical/testing contexts
