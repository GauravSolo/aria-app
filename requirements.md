# Build My Personal Assistant Planner App

I want you to build a complete personal assistant planner app for me.

I do not want only ideas, plans, or suggestions. I want you to actually generate the app code, folder structure, database schema, UI components, reminder logic, setup instructions, and runnable MVP.

The app should work on:

* Android Phone
* Mac Laptop 
* Web browser

The app should help me manage my day, tasks, reminders, water intake, habits, streaks, and productivity.

Important:

* Do not include any AI or LLM feature.
* I do not have an LLM API key.
* The app should use buttons, forms, checkboxes, dropdowns, and selections.
* I should not need to type natural-language commands like “track this”.
* Habit creation should happen through a normal form.

---

## Core Features

### 1. Daily Planner

The app should allow me to plan my full day.

Features:

* Today’s timeline
* Hour-by-hour schedule
* Add task
* Edit task
* Delete task
* Mark task as complete
* Task title
* Task description
* Start time
* End time
* Priority: Low, Medium, High
* Category: Work, Study, Health, Personal, Other
* Due date
* Recurring task option

---

### 2. Reminder System

The app should send reminders for:

* Tasks
* Habits
* Water drinking
* Custom reminders

Reminder features:

* One-time reminders
* Daily reminders
* Weekly reminders
* Custom repeat interval
* Snooze option
* Mark as done from app
* Notification history

---

### 3. Water Drinking Reminder

Add a dedicated water tracking feature.

Features:

* Set daily water goal
* Set reminder interval, for example every 30, 45, or 60 minutes
* Log water intake
* Button to add one glass
* Custom amount option
* Progress bar for daily goal
* Daily water total
* Weekly water summary
* Monthly water summary
* Reminder notifications

Example:

* Daily goal: 4 liters
* Reminder: every 45 minutes
* Add glass: 250 ml

---

### 4. Habit & Streak Tracking

The app should have a habit tracker using normal UI options.

I should be able to create a habit using a form with:

* Habit name
* Habit type
* Frequency: Daily, Weekly, Custom
* Target count
* Reminder time
* Start date
* Category
* Notes

Examples of habits:

* Solve 1 coding question
* Read 20 pages
* Gym
* Meditation
* Sleep before 11 PM
* Study for 1 hour

For each habit, the app should show:

* Checkbox or tick button to mark today as complete
* Current streak
* Longest streak
* Total completed days
* Missed days
* Weekly progress
* Monthly progress
* Completion calendar
* Success percentage

Streak rules:

* If a daily habit is completed today, streak increases.
* If a daily habit is missed, streak resets.
* Weekly habits should calculate streak based on weekly completion.
* Custom habits should follow their selected schedule.

---

### 5. Analytics Dashboard

The app should include an analytics page.

Analytics should show:

* Total tasks completed
* Total tasks missed
* Task completion rate
* Habit completion rate
* Current streaks
* Longest streaks
* Water intake consistency
* Best performing day
* Weekly summary
* Monthly summary
* Productivity trends
* Missed task list
* Habit progress charts
* Water progress charts

No AI-generated analysis is needed. Use normal charts, numbers, percentages, and summaries.

---

### 6. Home Screen Widget

Create phone widget support.

Widget should show:

* Today’s next task
* Pending tasks count
* Water progress
* Next water reminder
* Active streaks
* Today’s habit progress

Widget sizes:

* Small widget
* Medium widget
* Large widget

---

### 7. Main Dashboard

Main dashboard should show:

* Today’s schedule
* Pending tasks
* Completed tasks
* Water progress
* Habit streaks
* Upcoming reminders
* Productivity score
* Quick add buttons

Quick add buttons:

* Add task
* Add habit
* Add water
* Add reminder

---

### 8. Offline First

The app should work offline.

Requirements:

* Local database
* Offline reminders
* Offline task management
* Offline habit tracking
* Offline streak calculation
* Sync when internet is available

---

## Recommended Tech Stack

Use a practical beginner-friendly stack.

Preferred stack:

* Mobile: React Native + Expo
* Web/Laptop: Next.js
* Backend: Supabase or Firebase
* Database: PostgreSQL or SQLite for local-first storage
* Notifications: Expo Notifications
* Charts: Recharts or Victory Native
* State Management: Zustand

Choose the best option and explain why.

---

## UI Style

The design should be:

* Modern
* Clean
* Minimal
* Fast
* Premium-looking
* Easy to use

Inspired by:

* Todoist
* TickTick
* Google Calendar
* Apple Reminders
* Notion

Include:

* Light mode
* Dark mode
* Responsive layout
* Mobile-first design

---

## Development Output Required

Please generate:

1. Final app architecture
2. Folder structure
3. Database schema
4. Data models
5. Screen list
6. Navigation flow
7. UI component structure
8. Reminder logic
9. Notification logic
10. Water tracking logic
11. Habit tracking logic
12. Streak calculation logic
13. Analytics calculation logic
14. Widget implementation plan
15. Complete MVP source code
16. Setup commands
17. Run instructions for phone and laptop
18. Testing checklist
19. Future improvement ideas

---

## Important Instruction

Do not stop at giving a plan.

Actually build the MVP by generating the code file by file.

Start with:

1. Architecture
2. Folder structure
3. Database schema
4. MVP code
5. Setup instructions

The final result should be a buildable personal assistant planner app.
