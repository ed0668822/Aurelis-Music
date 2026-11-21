package com.music.aurelis.data.discord

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.music.aurelis.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.TimeUnit

/** Public, least-privilege OAuth2 (identify only), independent of Rich Presence. */
data class DiscordOAuthAccount(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
)

data class DiscordOAuthState(
    val account: DiscordOAuthAccount? = null,
    val busy: Boolean = false,
    val error: String? = null,
)

/**
 * OAuth2 Authorization Code + PKCE S256.
 *
 * The verifier, pending state, access token and refresh token never leave this
 * app's Keystore-backed EncryptedSharedPreferences. An unavailable Keystore
 * prevents login instead of silently writing tokens to unencrypted storage.
 * We do NOT access Discord's user-token gateway or claim that OAuth publishes
 * a Rich Presence activity; that requires the separately licensed Social SDK.
 */
object DiscordOAuth {
    private const val REDIRECT = "discord-1573536052935794802:/authorize/callback"
    private const val FILE_NAME = "aurelis_discord_oauth"
    private const val AUTHORIZE = "https://discord.com/oauth2/authorize"
    private const val API = "https://discord.com/api/v10"
    private const val PENDING_LIMIT_MS = 10 * 60 * 1000L
    private const val SCOPE = "identify"

    val state = MutableStateFlow(DiscordOAuthState())

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val http = OkHttpClient.Builder()
        .callTimeout(25, TimeUnit.SECONDS)
        .build()

    private fun securePrefs(context: Context) = EncryptedSharedPreferences.create(
        context.applicationContext,
        FILE_NAME,
        MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun initialize(context: Context) {
        val prefs = runCatching { securePrefs(context) }.getOrElse {
            state.value = DiscordOAuthState(error = context.getString(R.string.discord_oauth_storage_error))
            return
        }
        val access = prefs.getString("access", null)
        val refresh = prefs.getString("refresh", null)
        val id = prefs.getString("id", null)
        val username = prefs.getString("username", null)
        if (access.isNullOrBlank() || id.isNullOrBlank() || username.isNullOrBlank()) {
            state.value = DiscordOAuthState()
            return
        }
        state.value = DiscordOAuthState(
            account = DiscordOAuthAccount(
                id = id,
                username = username,
                displayName = prefs.getString("display_name", username) ?: username,
                avatarUrl = prefs.getString("avatar_url", null),
            ),
        )
        if (prefs.getLong("expires_at", 0) <= System.currentTimeMillis() + 60_000L) {
            if (refresh.isNullOrBlank()) {
                disconnect(context)
            } else {
                scope.launch { refreshAccount(context.applicationContext, refresh) }
            }
        }
    }

    /** Starts the browser flow. The Discord account credentials are never entered in Aurelis. */
    fun begin(context: Context) {
        val prefs = runCatching { securePrefs(context) }.getOrElse {
            state.value = state.value.copy(error = context.getString(R.string.discord_oauth_storage_error))
            return
        }
        val verifier = randomUrlSafe(48)
        val oauthState = randomUrlSafe(32)
        val challenge = challenge(verifier)
        val saved = prefs.edit()
            .putString("pending_verifier", verifier)
            .putString("pending_state", oauthState)
            .putLong("pending_time", System.currentTimeMillis())
            .commit()
        if (!saved) {
            state.value = state.value.copy(error = context.getString(R.string.discord_oauth_storage_error))
            return
        }

        val uri = Uri.parse(AUTHORIZE).buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", DiscordApplication.ID)
            .appendQueryParameter("redirect_uri", REDIRECT)
            .appendQueryParameter("scope", SCOPE)
            .appendQueryParameter("state", oauthState)
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .build()
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        state.value = state.value.copy(error = null)
        runCatching { context.startActivity(intent) }.onFailure {
            prefs.edit().remove("pending_verifier").remove("pending_state").remove("pending_time").apply()
            state.value = state.value.copy(error = context.getString(R.string.discord_oauth_browser_error))
        }
    }

    /** Called from MainActivity.onCreate and onNewIntent for singleTask callbacks. */
    fun handleRedirect(context: Context, intent: Intent): Boolean {
        val uri = intent.data ?: return false
        if (intent.action != Intent.ACTION_VIEW ||
            uri.scheme != "discord-${DiscordApplication.ID}" ||
            uri.encodedAuthority != null ||
            uri.path != "/authorize/callback") return false

        val prefs = runCatching { securePrefs(context) }.getOrElse {
            state.value = state.value.copy(error = context.getString(R.string.discord_oauth_storage_error))
            return true
        }
        val pending = prefs.getString("pending_state", null)
        val verifier = prefs.getString("pending_verifier", null)
        val at = prefs.getLong("pending_time", 0)
        // Consume the one-time request even on errors; intercepted callbacks
        // cannot be replayed or exchanged twice.
        val consumed = prefs.edit()
            .remove("pending_state").remove("pending_verifier").remove("pending_time").commit()
        if (!consumed || pending.isNullOrBlank() || verifier.isNullOrBlank() ||
            System.currentTimeMillis() - at !in 0..PENDING_LIMIT_MS ||
            uri.getQueryParameter("state") != pending ||
            uri.getQueryParameter("iss")?.let { it != "https://discord.com" } == true) {
            state.value = state.value.copy(error = context.getString(R.string.discord_oauth_failed))
            return true
        }
        val code = uri.getQueryParameter("code")
        if (uri.getQueryParameter("error") != null || code.isNullOrBlank()) {
            state.value = state.value.copy(error = context.getString(R.string.discord_oauth_failed))
            return true
        }
        state.value = state.value.copy(busy = true, error = null)
        scope.launch {
            runCatching { exchangeAndLoadProfile(context.applicationContext, code, verifier) }
                .onSuccess { state.value = DiscordOAuthState(account = it) }
                .onFailure {
                    state.value = DiscordOAuthState(
                        account = state.value.account,
                        error = context.getString(R.string.discord_oauth_config_error),
                    )
                }
        }
        return true
    }

    private fun exchangeAndLoadProfile(context: Context, code: String, verifier: String): DiscordOAuthAccount {
        val form = FormBody.Builder()
            .add("client_id", DiscordApplication.ID)
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("redirect_uri", REDIRECT)
            .add("code_verifier", verifier)
            .build()
        val tokens = requestToken(form)
        return saveAccount(context, tokens)
    }

    private fun refreshAccount(context: Context, refresh: String) {
        state.value = state.value.copy(busy = true)
        val result = runCatching {
            val form = FormBody.Builder()
                .add("client_id", DiscordApplication.ID)
                .add("grant_type", "refresh_token")
                .add("refresh_token", refresh)
                .build()
            saveAccount(context, requestToken(form))
        }
        result.onSuccess { state.value = DiscordOAuthState(account = it) }
            .onFailure {
                // Old credentials are no longer usable. Clear only OAuth data;
                // don't touch local playback, its preferences or Rich Presence.
                runCatching { securePrefs(context).edit().clear().apply() }
                state.value = DiscordOAuthState(error = context.getString(R.string.discord_oauth_failed))
            }
    }

    private fun requestToken(form: FormBody): JSONObject {
        val request = Request.Builder()
            .url("$API/oauth2/token")
            .header("Accept", "application/json")
            .post(form)
            .build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("OAuth token exchange rejected")
            return JSONObject(response.body?.string() ?: error("Empty OAuth response"))
        }
    }

    private fun saveAccount(context: Context, json: JSONObject): DiscordOAuthAccount {
        val access = json.optString("access_token").takeIf { it.isNotBlank() }
            ?: error("No OAuth bearer token")
        require(json.optString("token_type").equals("Bearer", ignoreCase = true))
        require(json.optString("scope").split(" ").contains(SCOPE))
        val request = Request.Builder()
            .url("$API/users/@me")
            .header("Authorization", "Bearer $access")
            .header("Accept", "application/json")
            .build()
        val me = http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Unable to read authorized user profile")
            JSONObject(response.body?.string() ?: error("Empty identity response"))
        }
        val id = me.optString("id").takeIf { it.matches(Regex("^[0-9]{15,22}$")) }
            ?: error("Missing user id")
        val username = me.optString("username").takeIf { it.isNotBlank() }
            ?: error("Missing username")
        val displayName = me.optString("global_name").takeIf { it.isNotBlank() && it != "null" }
            ?: username
        val hash = me.optString("avatar").takeIf { it.matches(Regex("^[a-zA-Z0-9_]+$")) }
        val avatarUrl = hash?.let { "https://cdn.discordapp.com/avatars/$id/$it.png?size=128" }
        val prefs = securePrefs(context)
        val refresh = json.optString("refresh_token").takeIf { it.isNotBlank() }
            ?: prefs.getString("refresh", null)
        val expiry = System.currentTimeMillis() +
            json.optLong("expires_in", 3600).coerceIn(1, 604800) * 1000L
        val saved = prefs.edit()
            .putString("access", access)
            .putString("refresh", refresh)
            .putLong("expires_at", expiry)
            .putString("id", id)
            .putString("username", username)
            .putString("display_name", displayName)
            .putString("avatar_url", avatarUrl)
            .commit()
        if (!saved) error("Unable to save OAuth session")
        return DiscordOAuthAccount(id, username, displayName, avatarUrl)
    }

    /** Disconnects locally immediately and requests official token revocation. */
    fun disconnect(context: Context) {
        val access = runCatching { securePrefs(context).getString("access", null) }.getOrNull()
        runCatching { securePrefs(context).edit().clear().commit() }
        state.value = DiscordOAuthState()
        if (!access.isNullOrBlank()) scope.launch {
            runCatching {
                val form = FormBody.Builder()
                    .add("client_id", DiscordApplication.ID)
                    .add("token", access)
                    .add("token_type_hint", "access_token")
                    .build()
                http.newCall(Request.Builder().url("$API/oauth2/token/revoke").post(form).build())
                    .execute().close()
            }
        }
    }

    internal fun challenge(verifier: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(
            MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)),
        )

    private fun randomUrlSafe(byteCount: Int): String {
        val bytes = ByteArray(byteCount)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
