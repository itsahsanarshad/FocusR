package com.focusr.v2

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/**
 * Accessibility Service for instant app detection and blocking.
 * Replaces the UsageStats polling approach with event-driven detection.
 */
class FocusRAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var blockingTimeManager: BlockingTimeManager
    private lateinit var sleepDetectionManager: SleepDetectionManager
    private lateinit var prayerTimeManager: PrayerTimeManager
    private lateinit var usageSessionManager: UsageSessionManager
    
    private var previousApp: String? = null
    private var lastBlockedApp: String? = null
    private var lastBlockTime: Long = 0
    
    companion object {
        private const val TAG = "FocusRAccessibility"
        private const val BLOCK_DEBOUNCE_MS = 500L  // Prevent rapid re-blocking
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "FocusRAccessibilityService created")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "FocusRAccessibilityService connected")
        
        // Initialize managers
        preferencesManager = PreferencesManager(this)
        prayerTimeManager = PrayerTimeManager(this)
        blockingTimeManager = BlockingTimeManager(preferencesManager, prayerTimeManager)
        sleepDetectionManager = SleepDetectionManager(this)
        usageSessionManager = UsageSessionManager(this)
        
        // Configure the service
        serviceInfo = serviceInfo?.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 50
        }
        
        // Perform migration on service creation
        serviceScope.launch {
            preferencesManager.migrateOldBlockedApps()
        }
        
        Log.d(TAG, "FocusRAccessibilityService configured and ready")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val packageName = event.packageName?.toString() ?: return
                
                // Ignore our own app and system UI
                if (packageName == this.packageName || 
                    packageName == "com.android.systemui" ||
                    packageName == "com.android.launcher" ||
                    packageName.contains("launcher")) {
                    return
                }
                
                Log.d(TAG, "Window changed to: $packageName")
                
                serviceScope.launch {
                    handleAppOpen(packageName)
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "FocusRAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "FocusRAccessibilityService destroyed")
    }

    /**
     * Main logic when an app is opened - equivalent to checkCurrentApp() in old service
     */
    private suspend fun handleAppOpen(currentApp: String) {
        try {
            // Track app changes for post-closure break
            if (previousApp != null && previousApp != currentApp) {
                usageSessionManager.recordAppClosed(previousApp!!)
            }
            previousApp = currentApp
            
            // Run background rule management tasks
            runPeriodicChecks()
            
            // Check if THIS specific app should be blocked based on its rules
            if (blockingTimeManager.shouldBlockApp(currentApp)) {
                Log.d(TAG, "Blocking $currentApp based on active rules")
                
                // Check if this is a PRAYER_MODE block
                val allRulesNow = preferencesManager.blockingRules.first()
                val prayerRule = allRulesNow.find { rule ->
                    rule.enabled &&
                    rule.ruleType == com.focusr.v2.models.RuleType.PRAYER_MODE &&
                    rule.getApps().contains(currentApp) &&
                    !rule.currentPrayerUnlocked
                }
                
                if (prayerRule != null) {
                    val currentPrayer = prayerTimeManager.getCurrentPrayer()
                    val canUnlock = blockingTimeManager.canUnlockPrayer(prayerRule)
                    val minutesUntilUnlock = blockingTimeManager.getMinutesUntilUnlock(prayerRule)
                    blockAppForPrayer(
                        currentApp,
                        currentPrayer?.displayName ?: "Prayer",
                        currentPrayer?.overlayMessage ?: "It's prayer time. Take a moment to connect.",
                        canUnlock,
                        minutesUntilUnlock,
                        prayerRule.id
                    )
                } else {
                    blockApp(currentApp)
                }
                return
            }
            
            // Smart Cooldown: Session tracking and blocking
            handleSmartCooldown(currentApp, allRules = preferencesManager.blockingRules.first())
            
            // Check if any enabled rules remain
            val enabledRules = preferencesManager.blockingRules.first().filter { it.enabled }
            if (enabledRules.isEmpty()) {
                Log.d(TAG, "No enabled rules exist")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in handleAppOpen: ${e.message}")
        }
    }

    /**
     * Periodic checks that were previously run on every poll cycle.
     * Now runs on each app open event.
     */
    private suspend fun runPeriodicChecks() {
        val allRules = preferencesManager.blockingRules.first()
        
        // Auto-disable expired SIMPLE rules
        val expiredSimpleRules = allRules.filter { rule ->
            rule.enabled &&
            rule.ruleType == com.focusr.v2.models.RuleType.SIMPLE &&
            rule.durationMinutes != null &&
            rule.activatedAt != null &&
            System.currentTimeMillis() >= rule.activatedAt + (rule.durationMinutes * 60 * 1000L)
        }
        
        expiredSimpleRules.forEach { expiredRule ->
            Log.d(TAG, "Auto-disabling expired SIMPLE rule: ${expiredRule.getDisplayName()}")
            preferencesManager.updateRule(expiredRule.copy(enabled = false))
        }
        
        // Check for wake-up detection for Mental Clarity rules
        val mentalClarityRules = allRules.filter { rule ->
            rule.enabled &&
            rule.ruleType == com.focusr.v2.models.RuleType.MENTAL_CLARITY &&
            rule.morningFuryEnabled &&
            rule.wakeUpDetectedAt == null
        }
        
        mentalClarityRules.forEach { rule ->
            if (sleepDetectionManager.isInMorningWindow(rule.morningWindowStart, rule.morningWindowEnd)) {
                val wakeUpTime = sleepDetectionManager.detectWakeUp(
                    rule.sleepDetectionMinutes,
                    rule.morningWindowStart,
                    rule.morningWindowEnd
                )
                if (wakeUpTime != null) {
                    Log.d(TAG, "Wake-up detected for Mental Clarity rule: ${rule.getDisplayName()}")
                    preferencesManager.updateRule(rule.copy(wakeUpDetectedAt = wakeUpTime))
                }
            }
        }
        
        // Auto-clear expired Mental Clarity morning blocks
        val expiredMentalClarityRules = allRules.filter { rule ->
            rule.enabled &&
            rule.ruleType == com.focusr.v2.models.RuleType.MENTAL_CLARITY &&
            rule.wakeUpDetectedAt != null &&
            rule.morningBlockDuration != null &&
            System.currentTimeMillis() >= rule.wakeUpDetectedAt + (rule.morningBlockDuration * 60 * 1000L)
        }
        
        expiredMentalClarityRules.forEach { rule ->
            Log.d(TAG, "Resetting Mental Clarity rule for next day: ${rule.getDisplayName()}")
            preferencesManager.updateRule(rule.copy(wakeUpDetectedAt = null))
        }
        
        // Prayer Mode: Reset unlock status when prayer window ends
        val prayerModeRules = allRules.filter { rule ->
            rule.enabled &&
            rule.ruleType == com.focusr.v2.models.RuleType.PRAYER_MODE &&
            rule.currentPrayerUnlocked
        }
        
        prayerModeRules.forEach { rule ->
            val currentPrayer = prayerTimeManager.getCurrentPrayer()
            if (currentPrayer == null) {
                Log.d(TAG, "Prayer window ended, resetting unlock for: ${rule.getDisplayName()}")
                preferencesManager.updateRule(rule.copy(
                    currentPrayerUnlocked = false,
                    lastPrayerConfirmedAt = null
                ))
            }
        }
    }

    /**
     * Block an app by showing the overlay
     */
    private fun blockApp(packageName: String) {
        // Debounce to prevent rapid re-blocking
        val now = System.currentTimeMillis()
        if (packageName == lastBlockedApp && now - lastBlockTime < BLOCK_DEBOUNCE_MS) {
            return
        }
        lastBlockedApp = packageName
        lastBlockTime = now
        
        val intent = Intent(this, BlockerOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("blocked_app", packageName)
        }
        startActivity(intent)
    }

    /**
     * Block an app with Prayer Mode overlay
     */
    private fun blockAppForPrayer(
        packageName: String,
        prayerName: String,
        prayerMessage: String,
        canUnlock: Boolean,
        minutesUntilUnlock: Int,
        ruleId: String
    ) {
        // Debounce to prevent rapid re-blocking
        val now = System.currentTimeMillis()
        if (packageName == lastBlockedApp && now - lastBlockTime < BLOCK_DEBOUNCE_MS) {
            return
        }
        lastBlockedApp = packageName
        lastBlockTime = now
        
        val intent = Intent(this, BlockerOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("blocked_app", packageName)
            putExtra("is_prayer_mode", true)
            putExtra("prayer_name", prayerName)
            putExtra("prayer_message", prayerMessage)
            putExtra("can_unlock", canUnlock)
            putExtra("minutes_until_unlock", minutesUntilUnlock)
            putExtra("rule_id", ruleId)
        }
        startActivity(intent)
    }

    /**
     * Handles Smart Cooldown rule logic
     */
    private suspend fun handleSmartCooldown(currentApp: String, allRules: List<com.focusr.v2.models.BlockingRule>) {
        Log.d("SmartCooldown", "handleSmartCooldown called for: $currentApp")
        
        val smartCooldownRules = allRules.filter { rule ->
            rule.enabled &&
            rule.ruleType == com.focusr.v2.models.RuleType.SMART_COOLDOWN &&
            rule.getApps().contains(currentApp)
        }
        
        if (smartCooldownRules.isEmpty()) {
            return
        }
        
        for (rule in smartCooldownRules) {
            val maxUsage = rule.maxUsageMinutes ?: continue
            val cooldownDuration = rule.cooldownMinutes ?: continue
            
            // Check if currently in cooldown
            if (usageSessionManager.isInCooldown(currentApp)) {
                val remaining = usageSessionManager.getCooldownRemainingMinutes(currentApp)
                Log.d(TAG, "Smart Cooldown: $currentApp in cooldown (${remaining}m left)")
                blockApp(currentApp)
                return
            }
            
            // Check post-closure break
            if (rule.postClosureBreakEnabled && rule.postClosureBreakMinutes != null) {
                if (usageSessionManager.isInPostClosureBreak(currentApp, rule.postClosureBreakMinutes)) {
                    val remaining = usageSessionManager.getPostClosureBreakRemainingMinutes(currentApp, rule.postClosureBreakMinutes)
                    Log.d(TAG, "Smart Cooldown: $currentApp in post-closure break (${remaining}m left)")
                    blockApp(currentApp)
                    return
                }
            }
            
            // Update session tracking
            val session = usageSessionManager.updateSession(currentApp, rule.sessionResetMinutes)
            val usedMinutes = session.totalUsageSeconds / 60
            
            Log.d(TAG, "Smart Cooldown: $currentApp used ${usedMinutes}m / ${maxUsage}m")
            
            // Check if limit exceeded
            if (usedMinutes >= maxUsage) {
                Log.d(TAG, "Smart Cooldown: $currentApp limit reached! Starting ${cooldownDuration}m cooldown")
                usageSessionManager.startCooldown(currentApp, cooldownDuration)
                blockApp(currentApp)
                return
            }
        }
    }
}
