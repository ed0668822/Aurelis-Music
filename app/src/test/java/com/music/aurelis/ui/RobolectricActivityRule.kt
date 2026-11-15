// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import org.junit.rules.ExternalResource
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.robolectric.annotation.Config
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/** Unit-test-only activity registration: release APKs do not include ui-test-manifest. */
class RobolectricActivityRule : ExternalResource() {
    private var localeQualifier = "es-rES"
    override fun apply(base: Statement, description: Description): Statement {
        val qualifiers = description.getAnnotation(Config::class.java)?.qualifiers?.takeIf { it.isNotBlank() }
            ?: description.testClass?.getAnnotation(Config::class.java)?.qualifiers.orEmpty()
        localeQualifier = Regex("(?:^|-)([a-z]{2}(?:-r[A-Z]{2})?)(?=-|$)")
            .find(qualifiers)?.groupValues?.get(1) ?: "es-rES"
        return super.apply(base, description)
    }
    override fun before() {
        // Historical UI contracts are Spanish; locale-specific tests override it explicitly.
        RuntimeEnvironment.setQualifiers("+" + localeQualifier)
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
