//package com.example.opalforandroid
//
//import android.content.Context
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.content.pm.ResolveInfo
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//
//object AppManager {
//
//    suspend fun getInstalledApps(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
//        val packageManager = context.packageManager
//        val intent = Intent(Intent.ACTION_MAIN).apply {
//            addCategory(Intent.CATEGORY_LAUNCHER)
//        }
//
//        val resolveInfoList: List<ResolveInfo> = packageManager.queryIntentActivities(intent, 0)
//
//        val apps = resolveInfoList.mapNotNull { resolveInfo ->
//            try {
//                val packageName = resolveInfo.activityInfo.packageName
//                val appName = resolveInfo.loadLabel(packageManager).toString()
//                val icon = resolveInfo.loadIcon(packageManager)
//
//                // Skip system apps and our own app
//                if (packageName != context.packageName && !isSystemApp(packageName, packageManager)) {
//                    AppInfo(
//                        packageName = packageName,
//                        appName = appName,
//                        icon = icon
//                    )
//                } else null
//            } catch (e: Exception) {
//                null
//            }
//        }
//
//        apps.sortedBy { it.appName.lowercase() }
//    }
//
//    private fun isSystemApp(packageName: String, packageManager: PackageManager): Boolean {
//        return try {
//            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
//            (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
//        } catch (e: Exception) {
//            false
//        }
//    }
//}

package com.focusr.v2

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppManager {

    suspend fun getInstalledApps(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfoList: List<ResolveInfo> = packageManager.queryIntentActivities(intent, 0)

        val apps = resolveInfoList.mapNotNull { resolveInfo ->
            try {
                val packageName = resolveInfo.activityInfo.packageName
                val appName = resolveInfo.loadLabel(packageManager).toString()
                val icon = resolveInfo.loadIcon(packageManager)

                // Only skip our own app and core system apps that shouldn't be blocked
                if (packageName != context.packageName && !isCoreSystemApp(packageName)) {
                    AppInfo(
                        packageName = packageName,
                        appName = appName,
                        icon = icon
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }

        apps.sortedBy { it.appName.lowercase() }
    }

    private fun isCoreSystemApp(packageName: String): Boolean {
        // Only filter out core system apps that users shouldn't block
        val coreSystemApps = setOf(
            "com.android.settings",
            "com.android.systemui",
            "com.android.launcher",
            "com.android.launcher3",
            "com.google.android.packageinstaller",
            "com.android.packageinstaller",
            "com.android.phone",
            "com.android.contacts",
            "com.android.dialer",
            "com.android.mms",
            "com.android.calendar",
            "com.android.camera",
            "com.android.gallery3d",
            "com.android.music",
            "com.android.calculator2",
            "com.android.clock",
            "com.android.emergency",
            "com.android.keychain",
            "com.android.providers.calendar",
            "com.android.providers.contacts"
        )

        return coreSystemApps.contains(packageName) ||
                packageName.startsWith("com.android.internal") ||
                packageName.startsWith("com.google.android.gms") ||
                packageName.startsWith("com.google.android.gsf")
    }

//    // Alternative approach - if you want to keep the original logic but include specific apps
//    private fun isSystemAppButAllowSocial(packageName: String, packageManager: PackageManager): Boolean {
//        // List of social/entertainment apps that might be system apps but should be available for blocking
//        val socialApps = setOf(
//            "com.facebook.katana",           // Facebook
//            "com.google.android.youtube",    // YouTube
//            "com.instagram.android",         // Instagram
//            "com.twitter.android",           // Twitter
//            "com.snapchat.android",          // Snapchat
//            "com.whatsapp",                  // WhatsApp
//            "com.tiktok",                    // TikTok
//            "com.linkedin.android",          // LinkedIn
//            "com.pinterest",                 // Pinterest
//            "com.reddit.frontpage",          // Reddit
//            "com.spotify.music",             // Spotify
//            "com.netflix.mediaclient",       // Netflix
//            "com.amazon.mShop.android.shopping", // Amazon
//            "com.ebay.mobile"                // eBay
//        )
//
//        // If it's a social app, don't consider it a "system app" for filtering purposes
//        if (socialApps.contains(packageName)) {
//            return false
//        }
//
//        // For other apps, check if they're actually system apps
//        return try {
//            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
//            (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
//        } catch (e: Exception) {
//            false
//        }
//    }
}