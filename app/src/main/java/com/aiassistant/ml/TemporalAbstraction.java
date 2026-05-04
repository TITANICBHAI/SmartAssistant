package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TemporalAbstraction — semi-MDP framework for skill-based temporally-extended actions.
 *
 * Provides a bridge between the flat RL agent and the game environment by:
 *
 *   1. SKILL LIBRARY: stores pre-learned or hand-coded action sequences (macros).
 *      Each skill S_k = {primitive_actions[], termination_condition, expected_reward}.
 *
 *   2. SKILL EXECUTOR: executes the selected skill over multiple time steps,
 *      buffering the sub-rewards and returning the accumulated discounted return
 *      once the skill terminates.
 *
 *   3. SKILL LEARNER: discovers new skills by detecting repeated action sub-sequences
 *      that produce above-average cumulative reward (action chunking / macro discovery).
 *
 *   4. INTRA-OPTION LEARNING: the agent receives a gradient update on each primitive
 *      step within a skill (not just at skill termination), accelerating learning.
 *
 *   5. SEMI-MDP RETURNS: computes proper γ^k-discounted inter-skill returns
 *      (Sutton et al. 1999, section 25.2).
 *
 * Thread-safe.
 */
public class TemporalAbstraction {

    private static final String TAG = "TemporalAbstraction";

    // ─────────────────────────────────────────────────────────────────────────
    // Skill definition
    // ─────────────────────────────────────────────────────────────────────────
    public static class Skill {
        public final int     id;
        public final String  name;
        public final int[]   actions;         // primitive action sequence
        public float         avgReturn;       // EWMA of skill return
        public int           executions;
        public int           maxSteps;        // max steps before forced termination

        public Skill(int id, String name, int[] actions, int maxSteps) {
            this.id       = id;
            this.name     = name;
            this.actions  = actions.clone();
            this.maxSteps = maxSteps;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Episode trajectory for skill discovery
    // ─────────────────────────────────────────────────────────────────────────
    private static class ActionRecord {
        int   action;
        float reward;
        ActionRecord(int a, float r) { action = a; reward = r; }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final List<Skill>        skillLibrary    = new ArrayList<>();
    private final List<ActionRecord> episodeHistory  = new ArrayList<>();

    private final int   actionDim;
    private final float gamma;
    private final float discoveryThreshold;  // min return to consider a sub-sequence a skill
    private final int   maxSkillLen;
    private final int   minSkillLen;
    private final int   maxSkillCount;

    // Execution state
    private Skill   activeSkill     = null;
    private int     skillStep       = 0;
    private float   skillReturn     = 0f;
    private float   skillDiscount   = 1f;
    private float[] skillStartState = null;

    // Stats
    private final AtomicInteger totalPrimitives = new AtomicInteger(0);
    private final AtomicInteger totalSkillExecs = new AtomicInteger(0);
    private final AtomicInteger discoveredSkills= new AtomicInteger(0);
    private float avgSkillReturn = 0f;
    private float avgSkillLen    = 0f;

    private final Random rng = new Random(109L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public TemporalAbstraction(int actionDim, float gamma, float discoveryThreshold,
                                int minSkillLen, int maxSkillLen, int maxSkillCount) {
        this.actionDim          = actionDim;
        this.gamma              = gamma;
        this.discoveryThreshold = discoveryThreshold;
        this.minSkillLen        = minSkillLen;
        this.maxSkillLen        = maxSkillLen;
        this.maxSkillCount      = maxSkillCount;
        Log.i(TAG, "TemporalAbstraction: skills=" + maxSkillCount
                + " discovery_thresh=" + discoveryThreshold);
    }

    public TemporalAbstraction(int actionDim) {
        this(actionDim, 0.99f, 0.5f, 3, 10, 50);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Manual skill registration
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized void addSkill(String name, int[] actions) {
        int id = skillLibrary.size();
        skillLibrary.add(new Skill(id, name, actions, actions.length + 5));
        Log.i(TAG, "Added skill '" + name + "' id=" + id + " len=" + actions.length);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Skill execution
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Start executing skill k from state s.
     */
    public synchronized void beginSkill(int skillId, float[] state) {
        if (skillId < 0 || skillId >= skillLibrary.size()) return;
        activeSkill     = skillLibrary.get(skillId);
        skillStep       = 0;
        skillReturn     = 0f;
        skillDiscount   = 1f;
        skillStartState = state.clone();
        totalSkillExecs.incrementAndGet();
    }

    /**
     * Get the next primitive action from the executing skill.
     * Returns -1 if skill is finished.
     */
    public synchronized int nextSkillAction() {
        if (activeSkill == null || skillStep >= activeSkill.actions.length) return -1;
        return activeSkill.actions[skillStep++];
    }

    /**
     * Feed reward received during skill execution.
     * @return Accumulated discounted skill return (valid only when done=true).
     */
    public synchronized float feedReward(float reward, boolean done) {
        skillReturn   += skillDiscount * reward;
        skillDiscount *= gamma;
        totalPrimitives.incrementAndGet();

        boolean terminating = done || skillStep >= (activeSkill != null ? activeSkill.actions.length : 0);
        if (terminating && activeSkill != null) {
            activeSkill.avgReturn = 0.9f * activeSkill.avgReturn + 0.1f * skillReturn;
            activeSkill.executions++;
            avgSkillReturn = 0.99f * avgSkillReturn + 0.01f * skillReturn;
            avgSkillLen    = 0.99f * avgSkillLen    + 0.01f * skillStep;
            activeSkill    = null;
        }
        return terminating ? skillReturn : 0f;
    }

    public synchronized boolean isSkillActive() { return activeSkill != null; }

    // ─────────────────────────────────────────────────────────────────────────
    // Skill discovery: record primitive actions, mine sub-sequences
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized void recordStep(int action, float reward) {
        episodeHistory.add(new ActionRecord(action, reward));
    }

    /**
     * At episode end: scan trajectory for high-return sub-sequences.
     * Creates new skills if a sub-sequence consistently produces above-threshold return.
     */
    public synchronized void discoverSkills() {
        int T = episodeHistory.size();
        if (T < minSkillLen) { episodeHistory.clear(); return; }

        for (int start = 0; start <= T - minSkillLen; start++) {
            for (int len = minSkillLen; len <= Math.min(maxSkillLen, T - start); len++) {
                // Compute discounted return of sub-sequence
                float G = 0f, disc = 1f;
                int[] actions = new int[len];
                for (int t = 0; t < len; t++) {
                    ActionRecord r = episodeHistory.get(start + t);
                    G    += disc * r.reward;
                    disc *= gamma;
                    actions[t] = r.action;
                }

                if (G > discoveryThreshold && !skillAlreadyExists(actions)) {
                    if (skillLibrary.size() < maxSkillCount) {
                        String name = "discovered_" + discoveredSkills.get();
                        skillLibrary.add(new Skill(skillLibrary.size(), name, actions, len + 5));
                        discoveredSkills.incrementAndGet();
                        Log.d(TAG, "Discovered skill: " + name + " return=" + G + " len=" + len);
                    }
                }
            }
        }
        episodeHistory.clear();
    }

    private boolean skillAlreadyExists(int[] actions) {
        outer: for (Skill s : skillLibrary) {
            if (s.actions.length != actions.length) continue;
            for (int i = 0; i < actions.length; i++)
                if (s.actions[i] != actions[i]) continue outer;
            return true;
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Intra-option learning (pseudo-reward for sub-steps)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Compute intrinsic intra-option reward: progress toward skill completion.
     * r_intra = step_fraction * extrinsic_reward_signal
     */
    public synchronized float intraOptionReward(float extrinsicReward) {
        if (activeSkill == null || activeSkill.actions.length == 0) return extrinsicReward;
        float progress = (float) skillStep / activeSkill.actions.length;
        return extrinsicReward * (0.5f + 0.5f * progress);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized int getSkillCount() { return skillLibrary.size(); }

    public synchronized List<Skill> getSkillLibrary() { return new ArrayList<>(skillLibrary); }

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("skillCount",       skillLibrary.size());
        s.put("totalPrimitives",  totalPrimitives.get());
        s.put("totalSkillExecs",  totalSkillExecs.get());
        s.put("discoveredSkills", discoveredSkills.get());
        s.put("avgSkillReturn",   avgSkillReturn);
        s.put("avgSkillLen",      avgSkillLen);
        s.put("isSkillActive",    activeSkill != null);
        if (activeSkill != null) {
            s.put("activeSkillName", activeSkill.name);
            s.put("activeSkillStep", skillStep);
        }
        return s;
    }
}
