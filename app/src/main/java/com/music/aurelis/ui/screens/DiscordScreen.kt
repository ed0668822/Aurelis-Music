package com.music.aurelis.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import coil3.compose.AsyncImage
import com.music.aurelis.data.discord.DiscordOAuth
import com.music.aurelis.data.LocalEmbeddedArtwork
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.aurelis.R
import com.music.aurelis.data.discord.DiscordSocialPresence
import com.music.aurelis.data.model.Song
import com.music.aurelis.data.settings.AppSettings
import com.music.aurelis.ui.components.IosSwitch
import java.util.Locale

/** Token-free, optional official Discord Social SDK integration. */
@Composable
fun DiscordScreen(
    song: Song?,
    positionMs: Long,
    durationMs: Long,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val enabled by AppSettings.discordRpcEnabled.collectAsStateWithLifecycle()
    val login by DiscordOAuth.state.collectAsStateWithLifecycle()
    val sdkBundled = remember { DiscordSocialPresence.sdkBundled }
    val discordInstalled = remember(context) { DiscordSocialPresence.isDiscordInstalled(context) }
    val supported = sdkBundled && discordInstalled
    // Use the actual local embedded cover when the track has no remote thumbnail.
    // This is a device-only preview; local artwork is never uploaded to Discord.
    val previewCover by produceState<String?>(
        initialValue = song?.thumbnailUrl,
        key1 = song?.localUri,
        key2 = song?.thumbnailUrl,
    ) {
        value = if (song?.localUri != null) {
            LocalEmbeddedArtwork.resolve(context, song) ?: song.thumbnailUrl
        } else {
            song?.thumbnailUrl
        }
    }

    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(contentPadding),
    ) {
        SettingsPageTitle(stringResource(R.string.discord))
        SettingsGroup(
            header = stringResource(R.string.discord_oauth_account_title),
            footer = stringResource(R.string.discord_oauth_account_note),
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                val account = login.account
                if (account != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (account.avatarUrl != null) {
                            AsyncImage(
                                model = account.avatarUrl,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)),
                            )
                            Spacer(Modifier.width(12.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(account.displayName, style = MaterialTheme.typography.titleMedium)
                            Text("@${account.username}", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { DiscordOAuth.disconnect(context) }) {
                            Text(stringResource(R.string.discord_oauth_disconnect))
                        }
                    }
                } else {
                    Button(onClick = { DiscordOAuth.begin(context) }, enabled = !login.busy) {
                        Text(stringResource(R.string.discord_oauth_connect))
                    }
                }
                if (login.busy) {
                    Text(stringResource(R.string.discord_oauth_connecting),
                        style = MaterialTheme.typography.bodySmall)
                }
                login.error?.let { message ->
                    Spacer(Modifier.height(8.dp))
                    Text(message, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                }
            }
        }

        SettingsGroup(
            header = stringResource(R.string.rich_presence),
            footer = stringResource(R.string.discord_no_credentials),
        ) {
            SettingsRow(
                icon = ImageVector.vectorResource(R.drawable.ic_discord),
                title = stringResource(R.string.show_what_playing),
                subtitle = when {
                    !sdkBundled -> stringResource(R.string.discord_sdk_not_bundled)
                    !discordInstalled -> stringResource(R.string.discord_app_not_installed)
                    else -> stringResource(R.string.discord_official_available)
                },
                enabled = supported,
                trailing = {
                    IosSwitch(
                        checked = enabled && supported,
                        onCheckedChange = AppSettings::setDiscordRpcEnabled,
                        enabled = supported,
                    )
                },
                onClick = { if (supported) AppSettings.setDiscordRpcEnabled(!enabled) },
            )
        }
        SettingsGroup(
            header = stringResource(R.string.preview),
            footer = stringResource(R.string.discord_preview_disclaimer),
        ) {
            DiscordActivityPreview(
                title = song?.title ?: stringResource(R.string.widget_preview_title),
                artist = song?.artist ?: stringResource(R.string.widget_preview_artist),
                artworkUrl = previewCover,
                positionMs = positionMs,
                durationMs = durationMs,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DiscordActivityPreview(
    title: String,
    artist: String,
    artworkUrl: String?,
    positionMs: Long,
    durationMs: Long,
) {
    val primaryText = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val secondaryText = primaryText.copy(alpha = 0.72f)
    val progress = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    } else 0f

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = stringResource(R.string.discord_listening_aurelis).uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = secondaryText,
        )
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val shape = RoundedCornerShape(18.dp)
            Box(
                modifier = Modifier.size(84.dp).clip(shape)
                    .background(Color(0xFF323443)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.84f),
                    modifier = Modifier.size(42.dp),
                )
                if (!artworkUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = artworkUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (durationMs > 0L) {
                    Text(
                        text = "${clock(positionMs.coerceIn(0L, durationMs))} / ${clock(durationMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryText,
                    )
                }
            }
        }
        if (durationMs > 0L) {
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(primaryText.copy(alpha = 0.16f)),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(progress).fillMaxHeight()
                        .background(Color(0xFF5865F2)),
                )
            }
        }
    }
}

private fun clock(ms: Long): String {
    val seconds = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(Locale.ROOT, seconds / 60, seconds % 60)
}
