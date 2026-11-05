package it.leonardo.mediaprojector

import android.hardware.display.DisplayManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import it.leonardo.mediaprojector.ui.MediaProjectorTheme
import it.leonardo.mediaprojector.ui.PlayerScreen

class MainActivity : ComponentActivity() {

    private val vm: PlayerViewModel by viewModels()

    private lateinit var displayManager: DisplayManager
    private var presentation: ExternalDisplayPresentation? = null

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = refreshPresentation()
        override fun onDisplayRemoved(displayId: Int) = refreshPresentation()
        override fun onDisplayChanged(displayId: Int) = refreshPresentation()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Il tablet non deve spegnere lo schermo durante lo spettacolo
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        displayManager = getSystemService(DisplayManager::class.java)

        setContent {
            MediaProjectorTheme {
                PlayerScreen(vm = vm, onExit = ::exitApp)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        displayManager.registerDisplayListener(displayListener, Handler(Looper.getMainLooper()))
        refreshPresentation()
    }

    override fun onStop() {
        displayManager.unregisterDisplayListener(displayListener)
        dismissPresentation()
        super.onStop()
    }

    private fun exitApp() {
        vm.stop()
        dismissPresentation()
        finishAndRemoveTask()
    }

    // ---------- Gestione schermo esterno ----------

    private fun findExternalDisplay(): Display? {
        val presentationDisplays = displayManager
            .getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
            .filter { it.displayId != Display.DEFAULT_DISPLAY }
        if (presentationDisplays.isNotEmpty()) return presentationDisplays.first()

        // Fallback: qualunque altro display non privato
        return displayManager.displays.firstOrNull {
            it.displayId != Display.DEFAULT_DISPLAY && (it.flags and Display.FLAG_PRIVATE) == 0
        }
    }

    private fun refreshPresentation() {
        val target = findExternalDisplay()
        if (target == null) {
            dismissPresentation()
            vm.setExternalDisplay(null)
            return
        }

        val current = presentation
        if (current != null && current.isShowing && current.display.displayId == target.displayId) {
            vm.setExternalDisplay(target.name)
            return
        }

        dismissPresentation()
        try {
            presentation = ExternalDisplayPresentation(this, target, vm.player).also { it.show() }
            vm.setExternalDisplay(target.name)
        } catch (e: WindowManager.InvalidDisplayException) {
            presentation = null
            vm.setExternalDisplay(null)
        }
    }

    private fun dismissPresentation() {
        try {
            presentation?.dismiss()
        } catch (_: Exception) {
        }
        presentation = null
    }
}
