package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SecurityPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Private Folder", appName)
    }

    @Test
    fun `security preferences pin and recovery email test`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val security = SecurityPreferences(context)

        security.setPin("1234")
        assertTrue(security.verifyPin("1234"))
        assertFalse(security.verifyPin("9999"))

        assertTrue(security.verifyRecoveryEmail("Tdurgesh2009@gmail.com"))
        assertTrue(security.verifyRecoveryEmail("tdurgesh2009@gmail.com "))
        assertTrue(security.isAntiClearDataEnabled)
        assertTrue(security.isAutoDeleteFromGalleryEnabled)

        security.isAutoDeleteFromGalleryEnabled = false
        assertFalse(security.isAutoDeleteFromGalleryEnabled)
    }

    @Test
    fun `manageSpaceActivity is configured on application`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            android.content.pm.PackageManager.GET_ACTIVITIES
        )
        val manageSpaceActivityName = packageInfo.applicationInfo?.manageSpaceActivityName
        assertEquals("com.example.ManageSpaceActivity", manageSpaceActivityName)
    }

    @Test
    fun `media repository delete source uris handles empty list`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = com.example.data.MediaRepository(context)
        val result = repo.deleteSourceUris(emptyList())
        assertTrue(result is com.example.data.DeleteSourceResult.Done)
    }
}
