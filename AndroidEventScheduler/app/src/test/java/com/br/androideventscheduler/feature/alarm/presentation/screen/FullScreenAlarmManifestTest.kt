package com.br.androideventscheduler.feature.alarm.presentation.screen

import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Lê o `AndroidManifest.xml` do módulo: o Robolectric não expõe `showWhenLocked`/`turnScreenOn` (API 27+) em
 * `ActivityInfo`, então a declaração é conferida no próprio arquivo. Rodar sobre a tela de bloqueio de verdade
 * só é verificável em aparelho/emulador.
 */
class FullScreenAlarmManifestTest {
    private val androidNamespace = "http://schemas.android.com/apk/res/android"

    private fun activityElement(): Element {
        val manifest = File("src/main/AndroidManifest.xml")
        val document =
            DocumentBuilderFactory
                .newInstance()
                .apply { isNamespaceAware = true }
                .newDocumentBuilder()
                .parse(manifest)
        val activities = document.getElementsByTagName("activity")
        return (0 until activities.length)
            .map { index -> activities.item(index) as Element }
            .single { it.getAttributeNS(androidNamespace, "name").endsWith("FullScreenAlarmActivity") }
    }

    private fun Element.androidAttribute(name: String): String? =
        getAttributeNS(androidNamespace, name).takeIf { hasAttributeNS(androidNamespace, name) }

    @Test
    fun `given the manifest when reading the full screen activity then it shows over the lock screen and turns the screen on`() {
        val activity = activityElement()

        assertEquals("true", activity.androidAttribute("showWhenLocked"))
        assertEquals("true", activity.androidAttribute("turnScreenOn"))
    }

    @Test
    fun `given the manifest when reading the full screen activity then it is private single top in its own task and out of recents`() {
        val activity = activityElement()

        assertEquals("false", activity.androidAttribute("exported"))
        assertEquals("singleTop", activity.androidAttribute("launchMode"))
        assertEquals("", activity.androidAttribute("taskAffinity"))
        assertEquals("true", activity.androidAttribute("excludeFromRecents"))
    }

    @Test
    fun `given the manifest when reading the full screen activity filter then it has the action and default category and no data`() {
        val filter = activityElement().getElementsByTagName("intent-filter").item(0) as Element

        val action = filter.getElementsByTagName("action").item(0) as Element
        val category = filter.getElementsByTagName("category").item(0) as Element
        assertEquals("com.br.androideventscheduler.action.SHOW_FULL_SCREEN_ALARM", action.getAttributeNS(androidNamespace, "name"))
        assertEquals("android.intent.category.DEFAULT", category.getAttributeNS(androidNamespace, "name"))
        assertEquals(0, filter.getElementsByTagName("data").length)
    }

    @Test
    fun `given the manifest when listing permissions then no wake lock or keyguard permission is declared`() {
        val manifestText = File("src/main/AndroidManifest.xml").readText()

        assertEquals(false, manifestText.contains("WAKE_LOCK"))
        assertEquals(false, manifestText.contains("DISABLE_KEYGUARD"))
    }
}
