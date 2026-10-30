package com.flora.app.tools

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import com.flora.app.automation.FloraNotificationListener

/**
 * SD-P1 Batch 4 (8.8 music-app control, Lane A).
 *
 * Pehla raasta MediaSession hai: jo bhi music app is waqt
 * media session chala rahi hai, us par play/pause/next/prev.
 * Iske liye Flora ka NotificationListener Settings me ON hona
 * chahiye (user khud ON karega — Flora kabhi khud ON nahi
 * kar sakti); ON na ho to SecurityException aata hai, jise
 * ye helper imaandaar "notification access chahiye" message
 * me badal deta hai — chhupke se fail nahi hota.
 *
 * Fallback (caller ke paas): koi active session na mile to
 * music app verified app-open se kholo (Sd8 screen par
 * Spotify button) — control tab bhi MediaSession se hi hoga,
 * jab session active ho jayega.
 */
object MusicControl {

    data class NowPlaying(
        val pkg: String,
        val title: String,
        val artist: String,
        val playing: Boolean,
    )

    private fun activeControllers(context: Context): SdResult<List<MediaController>> {
        return try {
            val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val component = ComponentName(context, FloraNotificationListener::class.java)
            val sessions = manager.getActiveSessions(component)
            SdResult.Ok(sessions)
        } catch (e: SecurityException) {
            SdResult.Failure(
                "Notification access ON nahi hai — Settings me Flora ka notification access ON karo, phir dobara koshish karo.",
            )
        } catch (e: Exception) {
            SdResult.Failure("Music sessions padhne me dikkat aayi.")
        }
    }

    fun nowPlaying(context: Context): SdResult<NowPlaying?> {
        return when (val res = activeControllers(context)) {
            is SdResult.Failure -> res
            is SdResult.Ok -> {
                val controller = res.value.firstOrNull { it.playbackState != null }
                    ?: return SdResult.Ok(null)
                val metadata = controller.metadata
                val title = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: ""
                val artist = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST) ?: ""
                val playing = controller.playbackState?.state == PlaybackState.STATE_PLAYING
                SdResult.Ok(NowPlaying(controller.packageName, title, artist, playing))
            }
        }
    }

    private fun transport(context: Context, action: (MediaController) -> Unit): SdResult<Unit> {
        return when (val res = activeControllers(context)) {
            is SdResult.Failure -> res
            is SdResult.Ok -> {
                val controller = res.value.firstOrNull { it.playbackState != null }
                    ?: return SdResult.Failure("Koi music session active nahi hai — pehle music app me gaana chalao.")
                try {
                    action(controller)
                    SdResult.Ok(Unit)
                } catch (e: Exception) {
                    SdResult.Failure("Music app ne command mana kar diya.")
                }
            }
        }
    }

    fun play(context: Context): SdResult<Unit> =
        transport(context) { it.transportControls.play() }

    fun pause(context: Context): SdResult<Unit> =
        transport(context) { it.transportControls.pause() }

    fun next(context: Context): SdResult<Unit> =
        transport(context) { it.transportControls.skipToNext() }

    fun previous(context: Context): SdResult<Unit> =
        transport(context) { it.transportControls.skipToPrevious() }
}
