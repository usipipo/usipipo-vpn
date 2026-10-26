package com.usipipo.vpn.widget

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList

class SystemIntegrationResourceTest {

    @Test
    fun `el layout compacto declara los ids del widget`() {
        val ids = layoutIds(WIDGET_LAYOUT_PATH)

        assertEquals(REQUIRED_WIDGET_IDS, ids)
    }

    @Test
    fun `el layout ancho declara los ids del widget`() {
        val ids = layoutIds(WIDGET_WIDE_LAYOUT_PATH)

        assertEquals(REQUIRED_WIDGET_IDS, ids)
    }

    @Test
    fun `los metadatos del widget declaran su tamano y comportamiento`() {
        val provider = widgetProviderInfo()

        assertEquals("0", provider.androidAttribute("updatePeriodMillis"))
        assertEquals("horizontal|vertical", provider.androidAttribute("resizeMode"))
        assertEquals("home_screen", provider.androidAttribute("widgetCategory"))
        assertEquals("@layout/widget_usipipo", provider.androidAttribute("initialLayout"))
    }

    @Test
    fun `el manifiesto declara el receiver del widget`() {
        val receiver = manifestReceiver()

        assertEquals("true", receiver.androidAttribute("exported"))
    }

    @Test
    fun `el receiver del widget escucha las actualizaciones`() {
        val receiver = manifestReceiver()

        assertTrue(
            "el receiver no escucha $APPWIDGET_UPDATE_ACTION",
            APPWIDGET_UPDATE_ACTION in receiverActions(receiver),
        )
    }

    @Test
    fun `el receiver del widget declara los metadatos del proveedor`() {
        val receiver = manifestReceiver()

        assertEquals("@xml/usipipo_widget_info", receiverMetadata(receiver).androidAttribute("resource"))
    }

    @Test
    fun `existen los recursos graficos del widget`() {
        WIDGET_ASSETS.forEach { asset ->
            val file = File("src/main/res/$asset.xml")
            assertTrue("no existe el recurso $asset", file.exists())
        }
    }

    @Test
    fun `el manifiesto declara la Activity de permiso VPN`() {
        val activity = manifestActivity()

        assertEquals("false", activity.androidAttribute("exported"))
        assertEquals("@style/Theme.Usipipo.Transparent", activity.androidAttribute("theme"))
    }

    @Test
    fun `el tema de la Activity de permiso VPN es transparente`() {
        val style = transparentStyle()

        assertEquals("true", style.itemValue("windowIsTranslucent"))
        assertEquals("@android:color/transparent", style.itemValue("windowBackground"))
    }

    private fun manifestActivity(): Element =
        parse(File(MANIFEST_PATH))
            .getElementsByTagName("activity")
            .elements()
            .singleOrNull { it.androidAttribute("name") == ACTIVITY_NAME }
            ?: throw AssertionError("no existe la Activity $ACTIVITY_NAME en el manifiesto")

    private fun manifestReceiver(): Element =
        parse(File(MANIFEST_PATH))
            .getElementsByTagName("receiver")
            .elements()
            .singleOrNull { it.androidAttribute("name") == RECEIVER_NAME }
            ?: throw AssertionError("no existe el receiver $RECEIVER_NAME en el manifiesto")

    private fun layoutIds(path: String): Set<String> =
        parse(File(path))
            .getElementsByTagName("*")
            .elements()
            .mapNotNull { it.androidAttribute("id").takeIf(String::isNotEmpty) }
            .map { it.substringAfterLast('/') }
            .toSet()

    private fun widgetProviderInfo(): Element = parse(File(WIDGET_INFO_PATH)).documentElement

    private fun receiverActions(receiver: Element): List<String> =
        receiver.childElements()
            .filter { it.tagName == "intent-filter" }
            .flatMap { it.childElements() }
            .filter { it.tagName == "action" }
            .map { it.androidAttribute("name") }

    private fun receiverMetadata(receiver: Element): Element =
        receiver.childElements()
            .singleOrNull {
                it.tagName == "meta-data" && it.androidAttribute("name") == PROVIDER_METADATA_NAME
            }
            ?: throw AssertionError("el receiver no declara los metadatos $PROVIDER_METADATA_NAME")

    private fun transparentStyle(): Element =
        parse(File(THEMES_PATH))
            .getElementsByTagName("style")
            .elements()
            .singleOrNull { it.getAttribute("name") == TRANSPARENT_THEME }
            ?: throw AssertionError("no existe el estilo $TRANSPARENT_THEME")

    private fun parse(file: File): Document {
        assertTrue("no existe el archivo de recursos ${file.path}", file.exists())
        return DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(file)
    }

    private fun Element.androidAttribute(name: String): String = getAttributeNS(ANDROID_NAMESPACE, name)

    private fun Element.itemValue(name: String): String {
        val item = childElements()
            .singleOrNull { it.tagName == "item" && it.itemName() == name }
            ?: throw AssertionError("el estilo no declara el atributo $name")
        return item.textContent.trim()
    }

    /** El nombre de un item puede traer prefijo de namespace: "android:windowIsTranslucent". */
    private fun Element.itemName(): String = getAttribute("name").substringAfterLast(':')

    private fun Element.childElements(): List<Element> =
        (0 until childNodes.length)
            .map { childNodes.item(it) }
            .filterIsInstance<Element>()

    private fun NodeList.elements(): List<Element> =
        (0 until length)
            .map { item(it) }
            .filterIsInstance<Element>()

    private companion object {
        const val MANIFEST_PATH = "src/main/AndroidManifest.xml"
        const val THEMES_PATH = "src/main/res/values/themes.xml"
        const val WIDGET_LAYOUT_PATH = "src/main/res/layout/widget_usipipo.xml"
        const val WIDGET_WIDE_LAYOUT_PATH = "src/main/res/layout/widget_usipipo_wide.xml"
        const val WIDGET_INFO_PATH = "src/main/res/xml/usipipo_widget_info.xml"
        const val ACTIVITY_NAME = ".widget.VpnPermissionActivity"
        const val RECEIVER_NAME = ".widget.UsipipoWidgetProvider"
        const val TRANSPARENT_THEME = "Theme.Usipipo.Transparent"
        const val APPWIDGET_UPDATE_ACTION = "android.appwidget.action.APPWIDGET_UPDATE"
        const val PROVIDER_METADATA_NAME = "android.appwidget.provider"
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"

        val REQUIRED_WIDGET_IDS: Set<String> = setOf(
            "widget_root",
            "widget_logo",
            "widget_status",
            "widget_action",
            "widget_progress",
            "widget_profile_name",
            "widget_country",
        )

        val WIDGET_ASSETS: List<String> = listOf(
            "drawable/widget_bg_disconnected",
            "drawable/widget_bg_connected",
            "drawable/widget_bg_failed",
            "drawable/widget_button_disconnected",
            "drawable/widget_button_connected",
            "drawable/widget_button_failed",
            "drawable/ic_widget_power",
            "drawable/ic_widget_power_failed",
            "drawable/ic_widget_add",
            "drawable/ic_widget_shield_check",
        )
    }
}
