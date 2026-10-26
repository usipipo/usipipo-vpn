package com.usipipo.vpn.widget

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList

class SystemIntegrationResourceTest {

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

    private fun transparentStyle(): Element =
        parse(File(THEMES_PATH))
            .getElementsByTagName("style")
            .elements()
            .singleOrNull { it.getAttribute("name") == TRANSPARENT_THEME }
            ?: throw AssertionError("no existe el estilo $TRANSPARENT_THEME")

    private fun parse(file: File): Document =
        DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(file)

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
        const val ACTIVITY_NAME = ".widget.VpnPermissionActivity"
        const val TRANSPARENT_THEME = "Theme.Usipipo.Transparent"
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    }
}
