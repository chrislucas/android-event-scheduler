package com.br.androideventscheduler.feature.backup

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.MainActivity
import com.br.androideventscheduler.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.xmlpull.v1.XmlPullParser

@RunWith(RobolectricTestRunner::class)
class BackupManifestRobolectricTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val storagePermissions =
        setOf(
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.MANAGE_EXTERNAL_STORAGE",
            "android.permission.READ_MEDIA_IMAGES",
            "android.permission.READ_MEDIA_VIDEO",
            "android.permission.READ_MEDIA_AUDIO",
        )

    @Test
    fun `given merged manifest when reading providers then the file provider is private and grants uri permissions`() {
        val providers =
            context.packageManager
                .getPackageInfo(context.packageName, PackageManager.GET_PROVIDERS)
                .providers
                .orEmpty()

        val fileProvider = providers.single { it.name == FileProvider::class.java.name }
        assertEquals("${context.packageName}.fileprovider", fileProvider.authority)
        assertFalse(fileProvider.exported)
        assertTrue(fileProvider.grantUriPermissions)
    }

    @Test
    fun `given file paths resource when reading it then only the exports cache folder is exposed`() {
        val exposed = mutableListOf<Triple<String, String?, String?>>()
        context.resources.getXml(R.xml.file_paths).use { parser ->
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG && parser.name != "paths") {
                    exposed += Triple(parser.name, parser.getAttributeValue(null, "name"), parser.getAttributeValue(null, "path"))
                }
            }
        }

        assertEquals(listOf(Triple("cache-path", "exports", "exports/")), exposed)
    }

    @Test
    fun `given merged manifest when reading requested permissions then no storage permission is declared`() {
        val requested =
            context.packageManager
                .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
                .requestedPermissions
                .orEmpty()
                .toSet()

        assertEquals(emptySet<String>(), requested intersect storagePermissions)
    }

    private fun mainActivityHandles(intent: Intent): Boolean =
        context.packageManager
            .queryIntentActivities(intent.setPackage(context.packageName), 0)
            .any { it.activityInfo.name == MainActivity::class.java.name }

    private val jsonContentUri: Uri = Uri.parse("content://com.example.mail/attachments/agenda.json")

    @Test
    fun `given a view of a json content uri when resolving then the main activity handles it`() {
        val intent = Intent(Intent.ACTION_VIEW).setDataAndType(jsonContentUri, "application/json").addCategory(Intent.CATEGORY_DEFAULT)

        assertTrue(mainActivityHandles(intent))
    }

    @Test
    fun `given a send of json when resolving then the main activity handles it`() {
        val intent = Intent(Intent.ACTION_SEND).setType("application/json").addCategory(Intent.CATEGORY_DEFAULT)

        assertTrue(mainActivityHandles(intent))
    }

    @Test
    fun `given other mime types or schemes when resolving then the main activity does not handle the view`() {
        fun view(
            uri: String,
            type: String,
        ) = Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(uri), type).addCategory(Intent.CATEGORY_DEFAULT)

        assertFalse(mainActivityHandles(view("content://com.example.mail/a.txt", "text/plain")))
        assertFalse(mainActivityHandles(view("file:///sdcard/agenda.json", "application/json")))
        assertFalse(mainActivityHandles(view("https://example.com/agenda.json", "application/json")))
    }

    @Test
    fun `given a browsable view when resolving then the web cannot open the import`() {
        val intent =
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(jsonContentUri, "application/json")
                .addCategory(Intent.CATEGORY_DEFAULT)
                .addCategory(Intent.CATEGORY_BROWSABLE)

        assertFalse(mainActivityHandles(intent))
    }
}
