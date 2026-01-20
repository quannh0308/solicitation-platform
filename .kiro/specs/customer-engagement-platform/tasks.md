# Implementation Tasks - Current Cycle

> **Platform Rebranding Note**: This platform was formerly known as the "General Solicitation Platform". We've rebranded to "Customer Engagement & Action Platform (CEAP)" to better reflect its capabilities beyond solicitation. This is a documentation update only—package names and code remain unchanged.

## Current Focus: Task 23 - Implement real-time channel features

This task list shows the current 2-task implementation cycle. After completing these tasks, the next cycle will be loaded from FOUNDATION.

**Note**: Completed tasks are tracked in `completed-tasks.md` to keep this file focused on current work.

## Task Status Legend
- `[ ]` - Not started
- `[~]` - In progress  
- `[x]` - Complete
- `[*]` - Property-based test task

---

## Current Task Cycle

- [x] Task 23: Implement real-time channel features
- [-] Complete cycle - Commit, push, and setup next tasks

---

## Task 23 Details: Implement real-time channel features

Implement channel adapters for real-time channels (in-app, push notifications, voice assistant) and add personalized ranking with context and A/B test integration to the serving API.

### Subtasks:

- [ ] 23.1 Create in-app channel adapter
  - Implement serving API integration
  - Add in-app specific formatting
  - _Requirements: 15.2_

- [ ] 23.2 Create push notification channel adapter
  - Integrate with push notification service
  - Add push-specific formatting
  - _Requirements: 15.2_

- [ ] 23.3 Create voice assistant channel adapter
  - Integrate with voice assistant service
  - Add voice-specific formatting
  - _Requirements: 15.2_

- [ ] 23.4 Implement personalized ranking with context
  - Use customer history and preferences for ranking
  - Apply context-aware ranking algorithms
  - _Requirements: 15.4_

- [ ]* 23.5 Write property test for personalized ranking with context
  - **Property 47: Personalized ranking with context**
  - **Validates: Requirements 15.4**

- [ ] 23.6 Add A/B test integration to serving API
  - Return treatment-specific candidates
  - Ensure treatment consistency
  - _Requirements: 15.5_

- [ ]* 23.7 Write property test for treatment-specific candidate serving
  - **Property 48: Treatment-specific candidate serving**
  - **Validates: Requirements 15.5**

---

## Complete Cycle: Commit, Push, and Setup Next Tasks

After Task 23 completion, commit any fixes, push to git, and prepare tasks.md for the next cycle.

**IMPORTANT**: When setting up the next cycle, ALL tasks in the new tasks.md must be marked as `[ ]` not started. This is a fresh cycle start.

### Subtasks:

- [-] Commit and push any fixes
  - Stage all changes with `git add -A`
  - Create descriptive commit message if fixes were needed
  - Push to origin/main

- [ ] Setup next task cycle in tasks.md
  - Read FOUNDATION/tasks.md to identify next tasks (Task 24 from FOUNDATION)
  - Move completed Task 23 to completed-tasks.md with full details
  - Update tasks.md with Task 24 as the new main task
  - **CRITICAL**: Ensure ALL tasks in tasks.md are marked as `[ ]` not started (including Task 24 AND "Complete cycle" task)
  - **CRITICAL**: Ensure tasks in FOUNDATION/tasks.md are updated correctly (mark only the current finished task as done)
  - Update the "Complete cycle" subtask to reference Task 25 for the next iteration
  - Commit and push the updated files

---

## Next Cycle Preview

After Task 23 & cycle completion, the next cycle will focus on:
- **Task 24**: Implement backward compatibility and migration support (from FOUNDATION)
- **Complete cycle**: Commit, push, and setup next tasks

---

## Notes

- Property tests marked with `*` are required for correctness validation
- Each task references specific requirements for traceability
- Use the design document for detailed implementation guidance
- Refer to FOUNDATION/tasks.md for the complete task list
- Refer to completed-tasks.md for history of completed work
- DynamoDB local can be used for testing without AWS credentials
