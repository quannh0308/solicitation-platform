# Implementation Tasks - Current Cycle

> **Platform Rebranding Note**: This platform was formerly known as the "General Solicitation Platform". We've rebranded to "Customer Engagement & Action Platform (CEAP)" to better reflect its capabilities beyond solicitation. This is a documentation update only—package names and code remain unchanged.

## Current Focus: Task 27 - Final checkpoint - Ensure all tests pass

This task list shows the current 2-task implementation cycle. After completing these tasks, the next cycle will be loaded from FOUNDATION.

**Note**: Completed tasks are tracked in `completed-tasks.md` to keep this file focused on current work.

## Task Status Legend
- `[ ]` - Not started
- `[~]` - In progress  
- `[x]` - Complete
- `[*]` - Property-based test task

---

## Current Task Cycle

- [x] Task 27: Final checkpoint - Ensure all tests pass
- [-] Complete cycle - Commit, push, and setup next tasks

---

## Task 27 Details: Final checkpoint - Ensure all tests pass

Run all tests across all modules to ensure the platform is fully functional and all correctness properties are validated.

### Subtasks:

- [ ] 27.1 Run all unit tests
  - Execute: `./gradlew test`
  - Verify all unit tests pass
  - Fix any failing tests

- [ ] 27.2 Run all property-based tests
  - Execute: `./gradlew test --tests "*PropertyTest"`
  - Verify all property tests pass (100+ iterations each)
  - Fix any failing property tests

- [ ] 27.3 Run all integration tests
  - Execute: `./gradlew test --tests "*IntegrationTest"`
  - Verify all integration tests pass
  - Fix any failing integration tests

- [ ] 27.4 Run end-to-end tests
  - Execute: `./gradlew test --tests "*EndToEndPropertyTest"`
  - Verify all end-to-end tests pass
  - Fix any failing end-to-end tests

- [ ] 27.5 Verify build succeeds
  - Execute: `./gradlew clean build`
  - Verify all modules compile successfully
  - Verify no warnings or errors

---

## Complete Cycle: Commit, Push, and Setup Next Tasks

After Task 27 completion, commit any fixes, push to git, and prepare tasks.md for the next cycle.

**IMPORTANT**: When setting up the next cycle, ALL tasks in the new tasks.md must be marked as `[ ]` not started. This is a fresh cycle start.

### Subtasks:

- [-] Commit and push any fixes
  - Stage all changes with `git add -A`
  - Create descriptive commit message if fixes were needed
  - Push to origin/main

- [ ] Setup next task cycle in tasks.md
  - Read FOUNDATION/tasks.md to identify next tasks (Task 28 from FOUNDATION)
  - Move completed Task 27 to completed-tasks.md with full details
  - Update tasks.md with Task 28 as the new main task
  - **CRITICAL**: Ensure ALL tasks in tasks.md are marked as `[ ]` not started (including Task 28 AND "Complete cycle" task)
  - **CRITICAL**: Ensure tasks in FOUNDATION/tasks.md are updated correctly (mark only the current finished task as done)
  - Update the "Complete cycle" subtask to reference Task 29 for the next iteration
  - Commit and push the updated files

---

## Next Cycle Preview

After Task 27 & cycle completion, the next cycle will focus on:
- **Task 28**: Rename code modules and packages to CEAP (from FOUNDATION)
- **Complete cycle**: Commit, push, and setup next tasks

---

## Notes

- Property tests marked with `*` are required for correctness validation
- Each task references specific requirements for traceability
- Use the design document for detailed implementation guidance
- Refer to FOUNDATION/tasks.md for the complete task list
- Refer to completed-tasks.md for history of completed work
- DynamoDB local can be used for testing without AWS credentials
