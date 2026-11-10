package com.usipipo.vpn

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * Punto de entrada del lanzador. Es la única actividad expuesta al sistema y al resto
 * de apps (el receptor del widget y el mosaico siguen exportados a propósito, el sistema
 * los necesita), así que se queda con el filtro MAIN/LAUNCHER y tira el intent con el
 * que la abren: los datos que trae podrían forzar una pestaña o abrir el panel de
 * detalles. La UI real vive en `MainActivity`, cerrada a otros procesos.
 */
class LauncherActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Intent nuevo y vacío: ningún dato del llamante llega a MainActivity.
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
