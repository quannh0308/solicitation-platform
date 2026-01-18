# Implementation Tasks - Current Cycle

## Current Focus: Task 9 - Implement serving API

This task list shows the current 2-task implementation cycle. After completing these tasks, the next cycle will be loaded from FOUNDATION.

**Note**: Completed tasks are tracked in `completed-tasks.md` to keep this file focused on current work.

## Task Status Legend
- `[ ]` - Not started
- `[~]` - In progress  
- `[x]` - Complete
- `[*]` - Property-based test task

---

## Current Task Cycle

- [x] Task 9: Implement serving API
- [-] Complete cycle - Commit, push, and setup next tasks

---

## Task 9 Details: Implement serving API

Create the low-latency serving API for retrieving eligible candidates for customers.

### Subtasks:

- [x] 9.1 Create ServingAPI interface and Lambda handler
  - Implement GetCandidatesForCustomer endpoint
  - Implement GetCandidatesForCustomers batch endpoint
  - Add request validation
  - _Validates: Requirements 6.1, 6.2, 6.6_

- [x] 9.2 Implement channel-specific ranking logic
  - Create ranking algorithm framework
  - Implement ranking strategies per channel
  - _Validates: Requirements 6.3_

- [x]* 9.3 Write property test for ranking consistency
  - **Property 17: Channel-specific ranking consistency**
  - **Validates: Requirements 6.3**
  - **Status: ✅ PASSED** - All 4 ranking consistency tests passed

- [x] 9.4 Add real-time eligibility refresh support
  - Implement eligibility check with staleness detection
  - Add refresh logic for stale candidates
  - _Validates: Requirements 6.4_

- [x]* 9.5 Write property test for eligibility refresh correctness
  - **Property 18: Eligibility refresh correctness**
  - **Validates: Requirements 6.4**
  - **Status: ✅ PASSED** - All 7 eligibility refresh tests passed

- [x] 9.6 Implement fallback and graceful degradation
  - Add circuit breakers for dependencies
  - Implement fallback to cached results
  - Add degradation logging
  - _Validates: Requirements 6.5_

- [x]* 9.7 Write property test for serving API fallback behavior
  - **Property 19: Serving API fallback behavior**
  - **Validates: Requirements 6.5**
  - **Status: ✅ PASSED** - All 7 fallback behavior tests passed

- [x]* 9.8 Write property test for batch query correctness
  - **Property 20: Batch query correctness**
  - **Validates: Requirements 6.6**
  - **Status: ✅ PASSED** - All 6 batch query tests passed

---

## Complete Cycle: Commit, Push, and Setup Next Tasks

After Task 9 completion, commit any fixes, push to git, and prepare tasks.md for the next cycle.

### Subtasks:

- [-] Commit and push any fixes
  - Stage all changes with `git add -A`
  - Create descriptive commit message if fixes were needed
  - Push to origin/main

- [ ] Setup next task cycle in tasks.md
  - Read FOUNDATION/tasks.md to identify next tasks (Task 10 from FOUNDATION)
  - Update tasks.md with Task 10 and new cycle completion task
  - Move completed Task 9 to completed-tasks.md
  - Commit and push the updated files

---

## Next Cycle Preview

After Task 9 & cycle completion, the next cycle will focus on:
- **Task 10**: Implement channel adapter framework (from FOUNDATION)
- **Complete cycle**: Commit, push, and setup next tasks

---

## Notes

- Property tests marked with `*` are required for correctness validation
- Each task references specific requirements for traceability
- Use the design document for detailed implementation guidance
- Refer to FOUNDATION/tasks.md for the complete task list
- Refer to completed-tasks.md for history of completed work
- DynamoDB local can be used for testing without AWS credentials

