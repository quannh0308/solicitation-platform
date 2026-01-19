# Implementation Tasks - Current Cycle

> **Platform Rebranding Note**: This platform was formerly known as the "General Solicitation Platform". We've rebranded to "Customer Engagement & Action Platform (CEAP)" to better reflect its capabilities beyond solicitation. This is a documentation update only—package names and code remain unchanged.

## Current Focus: Task 22 - Checkpoint - Ensure lifecycle and security tests pass

This task list shows the current 2-task implementation cycle. After completing these tasks, the next cycle will be loaded from FOUNDATION.

**Note**: Completed tasks are tracked in `completed-tasks.md` to keep this file focused on current work.

## Task Status Legend
- `[ ]` - Not started
- `[~]` - In progress  
- `[x]` - Complete
- `[*]` - Property-based test task

---

## Current Task Cycle

- [x] Task 22: Checkpoint - Ensure lifecycle and security tests pass
- [-] Complete cycle - Commit, push, and setup next tasks

---

## Task 22 Details: Checkpoint - Ensure lifecycle and security tests pass

Ensure all tests pass, ask the user if questions arise.

### Subtasks:

- [ ] 22.1 Run all tests to verify lifecycle and security features
  - Run full test suite with `./gradlew test`
  - Verify all property-based tests pass
  - Verify all unit tests pass
  - Check for any test failures or warnings

- [ ] 22.2 Review test results and address any issues
  - If tests fail, investigate root cause
  - Ask user for guidance if issues are unclear
  - Document any known issues or limitations

---

## Complete Cycle: Commit, Push, and Setup Next Tasks

After Task 22 completion, commit any fixes, push to git, and prepare tasks.md for the next cycle.

**IMPORTANT**: When setting up the next cycle, ALL tasks in the new tasks.md must be marked as `[ ]` not started. This is a fresh cycle start.

### Subtasks:

- [ ] Commit and push any fixes
  - Stage all changes with `git add -A`
  - Create descriptive commit message if fixes were needed
  - Push to origin/main

- [ ] Setup next task cycle in tasks.md
  - Read FOUNDATION/tasks.md to identify next tasks (Task 23 from FOUNDATION)
  - Move completed Task 22 to completed-tasks.md with full details
  - Update tasks.md with Task 23 as the new main task
  - **CRITICAL**: Ensure ALL tasks in tasks.md are marked as `[ ]` not started (including Task 23 AND "Complete cycle" task)
  - **CRITICAL**: Ensure tasks in FOUNDATION/tasks.md are updated correctly (mark only the current finished task as done)
  - Update the "Complete cycle" subtask to reference Task 24 for the next iteration
  - Commit and push the updated files

---

## Next Cycle Preview

After Task 22 & cycle completion, the next cycle will focus on:
- **Task 23**: Implement real-time channel features (from FOUNDATION)
- **Complete cycle**: Commit, push, and setup next tasks

---

## Notes

- Property tests marked with `*` are required for correctness validation
- Each task references specific requirements for traceability
- Use the design document for detailed implementation guidance
- Refer to FOUNDATION/tasks.md for the complete task list
- Refer to completed-tasks.md for history of completed work
- DynamoDB local can be used for testing without AWS credentials
