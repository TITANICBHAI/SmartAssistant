# SelfLearningAI – Android Project

## Overview

This is an **Android application** project called SelfLearningAI. It is an autonomous AI assistant for Android that uses TensorFlow Lite, reinforcement learning, and the Android Accessibility Service to analyze screens, detect objects, and execute tasks automatically.

**Important:** This is an Android app — it cannot run in the browser preview. The web server (`server.js`) serves a project explorer dashboard as a reference interface.

## Running in Replit

- **Workflow:** `Start application` → runs `node server.js` on port 5000
- **Preview:** Shows a project explorer dashboard describing the Android codebase

## Building the Android App

To build and run the actual Android application:
1. Open in **Android Studio**
2. Connect an Android 7.0+ (API 24) device or start an emulator
3. Click **Run** to build and deploy

Build system: **Gradle 8.1** with Android SDK 34

## Project Structure

```
app/
  src/main/
    java/com/aiassistant/
      adapters/       # RecyclerView adapters
      analysis/       # Game scene analysis
      core/           # AIController, AutoAIController
      database/       # Room DB, DAOs
      detection/      # Object, enemy, UI element detection
      learning/       # LearningEngine (JSON persistence, temporal sequences)
      ml/             # TensorFlow Lite models, DeepRLModel
      models/         # Data models (Task, AIState, GameState, etc.)
      monitoring/     # PerformanceMonitor, NetworkStateMonitor,
                      #   SmartBatteryOptimizer, CrashRecoveryManager
      receivers/      # BootReceiver, DeviceAdminReceiver
      rl/             # DQNAgent (PER + Double DQN + N-step)
                      # PPOAgent (GAE-λ + value clipping + KL stop)
                      # QLearningAgent, SARSAAgent
      scheduler/      # TaskSchedulerManager (real trigger/condition eval)
      services/       # AIAccessibilityService, AIBackgroundService
      ui/             # Activities and Fragments
      utils/          # Utility classes
      viewmodels/     # MVVM ViewModels
      workers/        # WorkManager workers
    res/              # Android resources (layouts, drawables, etc.)
    AndroidManifest.xml
models/               # Mock/standalone model classes
utils/                # Mock/standalone utility classes
android/              # Mock Android SDK shims (for non-Android compilation)
```

## Key Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| TensorFlow Lite | 2.10.0 | On-device ML inference |
| TFLite Support | 0.4.2 | Preprocessing/postprocessing |
| AndroidX AppCompat | 1.6.1 | Backward-compatible UI |
| Material Design | 1.10.0 | Google Material components |
| ConstraintLayout | 2.1.4 | Flexible UI layouts |
| SendGrid | 8.1.4 | Email notifications |

## Architecture

- **Pattern:** MVVM (ViewModel + Room + WorkManager)
- **AI/ML:** TensorFlow Lite for object detection; DQN, PPO, Q-Learning, SARSA agents
- **Automation:** Android Accessibility Service for cross-app UI interaction
- **Task Scheduling:** Trigger-based pipeline with real system-state evaluation
- **Source Files:** 195+ Java source files across 17 packages
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 34 (Android 14)

## Major Improvements Log (Session 2)

### AIController.java
- Removed all reflection hacks; uses direct typed `PredictionCallback`
- Retry-with-exponential-backoff for action execution (up to 3 retries)
- Per-action-type success/failure tracking with `getSuccessRate()`
- Real gesture dispatch via `AIAccessibilityService.getInstance()`
- Adaptive suggestion polling with back-pressure on consecutive errors

### AIAccessibilityService.java
- Static `getInstance()` for direct cross-component gesture dispatch
- Real `GestureDescription`-based gestures: `performClick`, `performLongPress`, `performSwipe`
- `LearningEngine` integration — every accessibility event feeds the learning pipeline
- Handles `TYPE_VIEW_LONG_CLICKED` and `TYPE_VIEW_SCROLLED`
- Rate-limiting: max 20 events/second forwarded to background thread
- Bounded-depth iterative BFS tree traversal (avoids StackOverflow)
- Privacy mode: skips a hard-coded set of sensitive package names

### AIBackgroundService.java
- Real initialization of all subsystems: AIController, LearningEngine,
  TaskSchedulerManager, PerformanceMonitor, NetworkStateMonitor, SmartBatteryOptimizer
- Adaptive polling loop with battery-aware interval selection
- Proper task delegation to TaskSchedulerManager (`executeTask`/`stopTask`)
- `getStatusSnapshot()` returns live data from all subsystems

### DQNAgent.java
- Prioritized Experience Replay (PER) — proportional priority sampling
- Double DQN — online net selects action, target net evaluates value
- N-step returns (N=3) with proper discount accumulation
- Epsilon decay over 10,000 steps (1.0 → 0.05)
- Xavier-initialized weight matrices; save/load full state

### PPOAgent.java
- Generalized Advantage Estimation (GAE-λ=0.95)
- Separate policy and value weight matrices (Xavier init)
- Value function loss clipping (PPO2-style)
- Gradient norm clipping (max 0.5)
- Per-epoch KL early stopping (threshold 0.015)
- Correct softmax entropy bonus
- Full model save/load

### LearningEngine.java
- Full JSON persistence: every pattern's fields written to `learning_patterns.json`
- LRU-based pattern eviction (LinkedHashMap access-order, max 1000 patterns)
- Temporal sequence detection: bigram/trigram N-gram pattern promotion
- Cross-app pattern keys for cross-package learning
- `recordUserInteraction()` entry point for accessibility events
- Thread-safe via ConcurrentHashMap

### TaskSchedulerManager.java
- `evaluateTrigger()` evaluates 9 real trigger types against live system state:
  IMMEDIATE, SCHEDULED, APP_LAUNCH, APP_EXIT, BATTERY, CONNECTIVITY,
  SCREEN_STATE, TIME_RANGE, DATA_CONDITION
- `evaluateCondition()` builds a real context map (battery, network, screen, foreground
  package, time, weekday) and delegates to the existing `Condition.evaluate()` engine

### New: monitoring package
| Class | Purpose |
|-------|---------|
| `PerformanceMonitor` | Samples CPU (`/proc/stat`) and RAM every 5 s; exposes `shouldThrottle()` |
| `NetworkStateMonitor` | `ConnectivityManager.NetworkCallback` on API 21+; legacy BroadcastReceiver fallback |
| `SmartBatteryOptimizer` | Sticky `ACTION_BATTERY_CHANGED` receiver; tiered throttle (warn/low/critical) |
| `CrashRecoveryManager` | UncaughtExceptionHandler + heartbeat stall detector + crash log rotation |

## Major Improvements Log (Session 3)

### QLearningAgent.java — complete rewrite
- **Q(λ) with replacing traces** — eligibility traces propagate credit backwards through
  the trajectory; replacing semantics prevent runaway trace accumulation.
- **Bug fix:** original `replayExperiences()` recursively called `update()`, causing infinite
  loops. Mini-batch replay now applies TD updates directly without any recursion.
- **LRU Q-table** — `LinkedHashMap` in access-order mode; entries beyond `MAX_TABLE_SIZE`
  (50 000) are evicted automatically to bound memory.
- **Epsilon-decay schedule** — linear decay from 1.0 → 0.05 over 5 000 steps.
- **Adaptive learning rate** — α(s,a) = α₀ / (1 + visits × 0.1); rarely-visited pairs
  receive larger updates.
- **Optimistic initialisation** — new Q-values seeded at +0.1 to encourage exploration.
- **Save/load** — persists visit counts, epsilon, bins, and step counter.

### SARSAAgent.java — complete rewrite
- **Bug fix:** original `updateWithEligibilityTraces()` iterated `qTable.keySet()` while
  modifying `eligibilityTraces`, risking `ConcurrentModificationException`; now uses a
  safe snapshot loop with deferred removal.
- **Bug fix:** recursive `update()` self-call inside the trace loop eliminated.
- **Replacing traces** — `applyReplacingTrace()` zeroes all actions in (s) then sets
  e(s,a)=1, preventing accumulation.
- **On-policy next-action selection** — `nextAction` is drawn from the current policy
  once before the trace loop (correct SARSA semantics).
- **Epsilon decay, adaptive α, optimistic init, LRU table** — same improvements as
  QLearningAgent.
- Episode state reset (`hasEpisodeStep = false`) on terminal transitions.

### AlgorithmSelector.java — UCB1 adaptive selection
- **UCB1 bandit selection** — replaces static heuristics; algorithms that perform better
  accumulate higher mean reward and are selected more often; confidence term balances
  exploration of underused algorithms.
- **Per-algorithm performance tracking** — `recordOutcome(AlgorithmType, float)` updates
  running mean reward and pull count for each algorithm.
- **Warm-start** — each algorithm is pulled at least 3 times before UCB1 scores are used.
- **Dyna-Q implemented** — previously fell back silently to Q-Learning; now `DynaQAgent`
  wraps `DQNAgent` and performs 3 planning steps per real update using a compact
  environment model map.
- `getMeanRewards()` exposes per-algorithm performance for monitoring.

### RuleExtractionSystem.java — real IF-THEN rule learner
- Replaced the empty stub (`findRelevantRules` returned `[]`) with a full rule-induction
  system.
- **Observation ingestion** — `observe(state, action, reward)` stores up to 2 000
  rolling (state, action, reward) tuples.
- **Rule induction** — background thread (every 60 s) mines frequent
  (condition → action) pairs using median-threshold APRIORI-style counting.
- **Rule scoring** — each rule tracks support, confidence, and importance; combined
  score = 0.6 × confidence + 0.4 × importance + support bonus.
- **JSON persistence** — rules survive app restarts.
- **Rule pruning** — rules below MIN_CONFIDENCE (40%) or older than 30 days removed.
- `findRelevantRules()` now returns real matched rules sorted by score.

### New: com.aiassistant.learning package (4 new classes)

| Class | Purpose |
|-------|---------|
| `ContextualMemory` | Rolling episodic memory (1 000 episodes). Cosine-similarity retrieval with recency decay. Feature normalisation (running min/max). JSON persistence. |
| `ActionOptimizer` | Beam-search action sequence planner. Expands BEAM_WIDTH candidates per depth step up to HORIZON steps. Falls back to greedy Q-value selection when no world model is available. Configurable gamma discount for cumulative reward scoring. |
| `RewardShaper` | Potential-based reward shaping: r̃ = r + γ·Φ(s') − Φ(s). Built-in potentials: LINEAR, GOAL_DISTANCE, CURIOSITY (1/√visits), COMPOSITE. Action-smoothness penalty. Visit-count map for curiosity bonus. Policy-invariant by construction. |
| `AdaptiveLearningScheduler` | Priority-queue task scheduler. Integrates with PerformanceMonitor and SmartBatteryOptimizer to skip training rounds under resource pressure. Exponential back-off (5 s → 2 min). Per-task run/skip statistics. |
