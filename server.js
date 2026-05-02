const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 5000;
const HOST = '0.0.0.0';

function getProjectStructure() {
  const packages = [
    { name: 'adapters', files: ['ActiveAppsAdapter', 'LearningPagerAdapter', 'LearningSourceAdapter', 'TaskAdapter', 'TasksAdapter'], desc: 'RecyclerView and ViewPager adapters for UI lists' },
    { name: 'analysis', files: ['GameSceneAnalyzer'], desc: 'Game scene and visual analysis logic' },
    { name: 'core', files: ['AIController', 'AutoAIController', 'PermissionBypassManager', 'SecurityBypassManager', 'TaskSchedulerManager'], desc: 'Central AI controller and autonomous behavior management' },
    { name: 'database', files: ['AppDatabase', 'dao/LearnedDataDao', 'dao/TaskDao'], desc: 'Room database setup and DAO interfaces' },
    { name: 'detection', files: ['ContentRecognizer', 'ElementUtils', 'EnemyDetector', 'GameAppElementDetector', 'ObjectDetector', 'TextRecognizer', 'UIElement'], desc: 'Object, enemy, and UI element detection' },
    { name: 'learning', files: ['ActionPattern', 'ActionSuggestion', 'ConceptModel', 'ContextExtractor', 'LearningEngine', 'LearningManager', 'SequencePattern', 'video/VideoProcessor'], desc: 'Core machine learning and pattern learning engine' },
    { name: 'ml', files: ['DeepRLModel', 'GamePatternRecognizer', 'GameTrainer', 'PredictiveActionSystem', 'RuleExtractionSystem', 'TensorflowLiteObjectDetector', 'UserFeedbackSystem'], desc: 'TensorFlow Lite models and ML inference' },
    { name: 'models', files: ['Action', 'AIMode', 'AIState', 'AppInfo', 'GameAction', 'GameState', 'LearnedData', 'ScheduledTask', 'Task', 'UIElement'], desc: 'Data models and entities' },
    { name: 'receivers', files: ['BootReceiver', 'DeviceAdminReceiver'], desc: 'Android broadcast receivers' },
    { name: 'rl', files: ['DQNAgent', 'PPOAgent', 'QLearningAgent', 'RLAgent', 'RLEnvironment', 'SARSAAgent', 'AlgorithmSelector'], desc: 'Reinforcement learning agents (DQN, PPO, Q-Learning, SARSA)' },
    { name: 'scheduler', files: ['TaskSchedulerManager', 'ScheduledTask', 'ActionSequence', 'Trigger', 'executor/ActionExecutor', 'executor/handlers/*'], desc: 'Task scheduling, triggers, and action execution pipeline' },
    { name: 'services', files: ['AIAccessibilityService', 'AIBackgroundService', 'AIService', 'LearningService', 'TaskSchedulerService'], desc: 'Android foreground/background services and accessibility service' },
    { name: 'ui', files: ['MainActivity', 'SettingsActivity', 'fragments/HomeFragment', 'fragments/AIFragment', 'fragments/LearningFragment', 'fragments/TasksFragment', 'fragments/SettingsFragment'], desc: 'Activities and Fragments for the app UI' },
    { name: 'utils', files: ['AccessibilityUtils', 'ActivityTracker', 'AppDetector', 'EncryptionHelper', 'NetworkHelper', 'PermissionHelper', 'ScreenshotManager', 'ShellCommandExecutor'], desc: 'Utility classes for common operations' },
    { name: 'viewmodels', files: ['AIViewModel', 'LearningViewModel', 'SettingsViewModel', 'TaskSchedulerViewModel'], desc: 'MVVM ViewModels connecting UI to data layer' },
    { name: 'workers', files: ['TaskWorker'], desc: 'WorkManager background workers' },
  ];
  return packages;
}

function getDependencies() {
  return [
    { name: 'TensorFlow Lite', version: '2.10.0', purpose: 'On-device ML inference for object detection and RL models' },
    { name: 'TFLite Support', version: '0.4.2', purpose: 'Preprocessing and postprocessing for TFLite models' },
    { name: 'AndroidX AppCompat', version: '1.6.1', purpose: 'Backward-compatible Android UI components' },
    { name: 'Material Design', version: '1.10.0', purpose: 'Google Material Design UI components' },
    { name: 'ConstraintLayout', version: '2.1.4', purpose: 'Flexible UI layout system' },
    { name: 'SendGrid', version: '8.1.4', purpose: 'Email sending for notification actions' },
  ];
}

function renderHTML() {
  const packages = getProjectStructure();
  const deps = getDependencies();

  const packageCards = packages.map(pkg => `
    <div class="card">
      <div class="card-header">
        <span class="package-icon">&#128230;</span>
        <code class="package-name">com.aiassistant.<strong>${pkg.name}</strong></code>
      </div>
      <p class="card-desc">${pkg.desc}</p>
      <div class="files">
        ${pkg.files.map(f => `<span class="file-chip">${f}.java</span>`).join('')}
      </div>
    </div>
  `).join('');

  const depRows = deps.map(d => `
    <tr>
      <td><strong>${d.name}</strong></td>
      <td><code>${d.version}</code></td>
      <td>${d.purpose}</td>
    </tr>
  `).join('');

  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>SelfLearningAI – Android Project Explorer</title>
  <style>
    *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      background: #0f1117;
      color: #e2e8f0;
      min-height: 100vh;
    }

    header {
      background: linear-gradient(135deg, #1a1f2e 0%, #16213e 50%, #0f3460 100%);
      border-bottom: 1px solid #2d3748;
      padding: 2rem 2rem 1.5rem;
    }

    .hero {
      max-width: 900px;
      margin: 0 auto;
    }

    .badge {
      display: inline-block;
      background: #e53e3e;
      color: white;
      font-size: 0.7rem;
      font-weight: 700;
      padding: 0.15rem 0.5rem;
      border-radius: 999px;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      margin-bottom: 0.75rem;
    }

    h1 {
      font-size: 2rem;
      font-weight: 800;
      color: #f7fafc;
      margin-bottom: 0.5rem;
    }

    h1 span { color: #63b3ed; }

    .subtitle {
      color: #a0aec0;
      font-size: 1rem;
      line-height: 1.6;
      max-width: 700px;
    }

    .stats {
      display: flex;
      gap: 2rem;
      margin-top: 1.5rem;
      flex-wrap: wrap;
    }

    .stat {
      text-align: center;
    }

    .stat-value {
      font-size: 1.8rem;
      font-weight: 800;
      color: #63b3ed;
    }

    .stat-label {
      font-size: 0.75rem;
      color: #718096;
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }

    main {
      max-width: 900px;
      margin: 0 auto;
      padding: 2rem;
    }

    section { margin-bottom: 2.5rem; }

    h2 {
      font-size: 1.1rem;
      font-weight: 700;
      color: #cbd5e0;
      text-transform: uppercase;
      letter-spacing: 0.08em;
      margin-bottom: 1rem;
      padding-bottom: 0.5rem;
      border-bottom: 1px solid #2d3748;
    }

    .notice {
      background: #2d3748;
      border-left: 4px solid #f6ad55;
      border-radius: 6px;
      padding: 1rem 1.25rem;
      color: #fbd38d;
      font-size: 0.9rem;
      line-height: 1.6;
      margin-bottom: 2rem;
    }

    .notice strong { color: #f6ad55; }

    .grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 1rem;
    }

    .card {
      background: #1a202c;
      border: 1px solid #2d3748;
      border-radius: 10px;
      padding: 1rem;
      transition: border-color 0.2s;
    }

    .card:hover { border-color: #4a5568; }

    .card-header {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      margin-bottom: 0.5rem;
    }

    .package-icon { font-size: 1rem; }

    .package-name {
      font-size: 0.8rem;
      color: #63b3ed;
      word-break: break-all;
    }

    .package-name strong { color: #90cdf4; }

    .card-desc {
      font-size: 0.8rem;
      color: #718096;
      line-height: 1.5;
      margin-bottom: 0.75rem;
    }

    .files {
      display: flex;
      flex-wrap: wrap;
      gap: 0.3rem;
    }

    .file-chip {
      background: #2d3748;
      color: #a0aec0;
      font-size: 0.65rem;
      font-family: monospace;
      padding: 0.15rem 0.4rem;
      border-radius: 4px;
    }

    table {
      width: 100%;
      border-collapse: collapse;
      font-size: 0.875rem;
    }

    th {
      text-align: left;
      padding: 0.6rem 0.75rem;
      color: #718096;
      font-size: 0.75rem;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      border-bottom: 1px solid #2d3748;
    }

    td {
      padding: 0.7rem 0.75rem;
      border-bottom: 1px solid #1a202c;
      color: #cbd5e0;
      vertical-align: top;
    }

    td code {
      background: #2d3748;
      padding: 0.1rem 0.35rem;
      border-radius: 4px;
      font-size: 0.75rem;
      color: #68d391;
    }

    .arch-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
      gap: 0.75rem;
    }

    .arch-item {
      background: #1a202c;
      border: 1px solid #2d3748;
      border-radius: 8px;
      padding: 0.85rem;
      text-align: center;
    }

    .arch-item .arch-icon { font-size: 1.5rem; margin-bottom: 0.4rem; }
    .arch-item .arch-title { font-size: 0.85rem; font-weight: 700; color: #e2e8f0; margin-bottom: 0.25rem; }
    .arch-item .arch-sub { font-size: 0.72rem; color: #718096; }

    footer {
      text-align: center;
      padding: 2rem;
      color: #4a5568;
      font-size: 0.8rem;
      border-top: 1px solid #2d3748;
    }
  </style>
</head>
<body>

<header>
  <div class="hero">
    <div class="badge">Android Application</div>
    <h1>&#129302; Self<span>Learning</span>AI</h1>
    <p class="subtitle">
      An autonomous AI assistant for Android that uses TensorFlow Lite, reinforcement learning,
      and the Accessibility Service to analyze screens, detect objects, and execute tasks automatically.
    </p>
    <div class="stats">
      <div class="stat"><div class="stat-value">195</div><div class="stat-label">Java Source Files</div></div>
      <div class="stat"><div class="stat-value">16</div><div class="stat-label">Packages</div></div>
      <div class="stat"><div class="stat-value">5</div><div class="stat-label">RL Agents</div></div>
      <div class="stat"><div class="stat-value">Android 7+</div><div class="stat-label">Min SDK 24</div></div>
    </div>
  </div>
</header>

<main>

  <div class="notice">
    <strong>&#9888; Note:</strong> This is an Android application project. It cannot run directly in a browser —
    it requires an Android device or emulator with Android 7.0+ (API 24).
    This page serves as a project explorer and reference dashboard for the codebase.
    To build and run: open this project in Android Studio and deploy to a device or emulator.
  </div>

  <section>
    <h2>&#127959; Architecture Overview</h2>
    <div class="arch-grid">
      <div class="arch-item">
        <div class="arch-icon">&#128065;</div>
        <div class="arch-title">Accessibility Service</div>
        <div class="arch-sub">Reads and interacts with UI elements across all apps</div>
      </div>
      <div class="arch-item">
        <div class="arch-icon">&#129504;</div>
        <div class="arch-title">TensorFlow Lite</div>
        <div class="arch-sub">On-device object detection and scene analysis</div>
      </div>
      <div class="arch-item">
        <div class="arch-icon">&#127918;</div>
        <div class="arch-title">RL Agents</div>
        <div class="arch-sub">DQN, PPO, Q-Learning & SARSA for autonomous decision making</div>
      </div>
      <div class="arch-item">
        <div class="arch-icon">&#128196;</div>
        <div class="arch-title">Task Scheduler</div>
        <div class="arch-sub">Trigger-based automated task execution pipeline</div>
      </div>
      <div class="arch-item">
        <div class="arch-icon">&#127909;</div>
        <div class="arch-title">Video Learning</div>
        <div class="arch-sub">Learns behaviors by processing screen recordings</div>
      </div>
      <div class="arch-item">
        <div class="arch-icon">&#128202;</div>
        <div class="arch-title">MVVM Architecture</div>
        <div class="arch-sub">ViewModels + Room DB + WorkManager</div>
      </div>
    </div>
  </section>

  <section>
    <h2>&#128230; Java Packages</h2>
    <div class="grid">
      ${packageCards}
    </div>
  </section>

  <section>
    <h2>&#128230; Dependencies</h2>
    <table>
      <thead>
        <tr><th>Library</th><th>Version</th><th>Purpose</th></tr>
      </thead>
      <tbody>
        ${depRows}
      </tbody>
    </table>
  </section>

</main>

<footer>SelfLearningAI &mdash; Android Project &bull; Build with Gradle 8.1 &bull; Target SDK 34</footer>

</body>
</html>`;
}

const server = http.createServer((req, res) => {
  res.writeHead(200, {
    'Content-Type': 'text/html; charset=utf-8',
    'Cache-Control': 'no-cache',
  });
  res.end(renderHTML());
});

server.listen(PORT, HOST, () => {
  console.log(`SelfLearningAI project explorer running at http://${HOST}:${PORT}`);
});
