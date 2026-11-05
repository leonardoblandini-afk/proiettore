@file:OptIn(UnstableApi::class)

package it.leonardo.mediaprojector

import android.app.Presentation
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Display
import android.view.ViewGroup
import android.view.WindowManager
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView

/**
 * Finestra a schermo intero mostrata sullo schermo esterno (proiettore HDMI).
 * Contiene solo il video, senza controlli: i controlli restano sul tablet.
 */
class ExternalDisplayPresentation(
    context: Context,
    display: Display,
    private val player: Player
) : Presentation(context, display) {

    private lateinit var playerView: PlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window?.setBackgroundDrawableResource(android.R.color.black)

        playerView = PlayerView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            useController = false
            setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
            setShutterBackgroundColor(Color.BLACK)
            setBackgroundColor(Color.BLACK)
        }
        setContentView(playerView)
    }

    override fun onStart() {
        super.onStart()
        playerView.player = player
    }

    override fun onStop() {
        playerView.player = null
        super.onStop()
    }
}
