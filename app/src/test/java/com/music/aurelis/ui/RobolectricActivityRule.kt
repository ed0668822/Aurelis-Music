package com.music.aurelis.ui

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import org.junit.rules.ExternalResource
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/** Unit-test-only activity registration: release APKs do not include ui-test-manifest. */
class RobolectricActivityRule : ExternalResource() {
    override fun before() {
        val app = RuntimeEnvironment.getApplication()
        val component = ComponentName(app, ComponentActivity::class.java)
        try { app.packageManager.getActivityInfo(component, 0); return }
        catch (_: PackageManager.NameNotFoundException) { }
        val info = ActivityInfo().apply {
            name = ComponentActivity::class.java.name; packageName = app.packageName
            applicationInfo = app.applicationInfo; enabled = true; exported = true
            theme = android.R.style.Theme_Material_Light_NoActionBar
        }
        val manager = shadowOf(app.packageManager)
        manager.addOrUpdateActivity(info)
        manager.addIntentFilterForActivity(component, IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) })
    }
}
