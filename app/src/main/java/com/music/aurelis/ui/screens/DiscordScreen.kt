package com.music.aurelis.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
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
    val sdkBundled = remember { DiscordSocialPresence.sdkBundled }
    val discordInstalled = remember(context) { DiscordSocialPresence.isDiscordInstalled(context) }
    val supported = sdkBundled && discordInstalled

    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(contentPadding),
    ) {
        SettingsPageTitle(stringResource(R.string.discord))
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
    positionMs: Long,
    durationMs: Long,
) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = stringResource(R.string.discord_listening_aurelis).uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val shape = RoundedCornerShape(18.dp)
            Box(
                modifier = Modifier.size(84.dp).clip(shape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.size(76.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (durationMs > 0) {
                    Text(
                        text = "${clock(positionMs)} / ${clock(durationMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun clock(ms: Long): String {
    val seconds = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(Locale.ROOT, seconds / 60, seconds % 60)
}
