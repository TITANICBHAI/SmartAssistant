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
      core/           # AIController, AutoAIController, TaskSchedulerManager
      database/       # Room DB, DAOs
      detection/      # Object, enemy, UI element detection
      learning/       # LearningEngine, VideoProcessor
      ml/             # TensorFlow Lite models, DeepRLModel
      models/         # Data models (Task, AIState, GameState, etc.)
      receivers/      # BootReceiver, DeviceAdminReceiver
      rl/             # RL agents: DQN, PPO, Q-Learning, SARSA
      scheduler/      # Task scheduling and action execution pipeline
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
- **Task Scheduling:** Trigger-based pipeline with custom ActionExecutor
- **Source Files:** 195 Java source files across 16 packages
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 34 (Android 14)
