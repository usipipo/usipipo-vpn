package com.usipipo.vpn.widget

import com.usipipo.vpn.ui.nav.MainTab
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El acceso directo a Importar es un contrato entre dos archivos: el widget manda una clave
 * en el extra del intent y `MainActivity` decide que pestaña abre. Si uno usa el nombre del
 * enum ("Import") y el otro una literal en minúsculas ("import"), la app abre Inicio y el
 * usuario nunca llega a Importar: un fallo silencioso que ningún test de lógica detecta,
 * porque cada lado por su lado es correcto.
 *
 * Aquí se fija el contrato en los dos extremos: el widget manda el nombre del enum y la
 * actividad resuelve la clave con el criterio compartido [MainTab.fromKey], sin mantener su
 * propia tabla de pestañas. La lectura del fuente es la única forma de comprobarlo en la
 * JVM: `MainActivity` es una Activity de Android y no se puede instanciar sin el framework.
 */
class ImportShortcutContractTest {

    @Test
    fun `la clave que manda el widget abre la pestana Importar`() {
        assertEquals(MainTab.Import, MainTab.fromKey(MainTab.Import.name))
    }

    @Test
    fun `la actividad resuelve la pestana con el criterio compartido`() {
        val codigo = File(MAIN_ACTIVITY).readText()

        assertTrue(
            "MainActivity debe resolver la pestaña con MainTab.fromKey",
            codigo.contains("MainTab.fromKey("),
        )
    }

    @Test
    fun `la actividad no mantiene su propia tabla de pestanas`() {
        val codigo = File(MAIN_ACTIVITY).readText()

        listOf("home", "configs", "import", "diagnostics").forEach { clave ->
            assertFalse(
                "MainActivity no debe tener su propia clave \"$clave\": usa MainTab.fromKey",
                codigo.contains("\"$clave\" ->"),
            )
        }
    }

    @Test
    fun `el widget manda el nombre del enum y no una literal`() {
        val codigo = File(WIDGET_INTENTS).readText()

        assertTrue(
            "el widget debe mandar MainTab.Import.name",
            codigo.contains("MainTab.Import.name"),
        )
        assertFalse(
            "el widget no debe mandar una clave literal",
            codigo.contains("EXTRA_INITIAL_TAB, \""),
        )
    }

    private companion object {
        const val MAIN_ACTIVITY = "src/main/java/com/usipipo/vpn/MainActivity.kt"
        const val WIDGET_INTENTS = "src/main/java/com/usipipo/vpn/widget/WidgetIntents.kt"
    }
}
