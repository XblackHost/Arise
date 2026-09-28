package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ARISE", appName)
  }

  @Test
  fun `test api key encryption and storage`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val storage = com.example.security.ApiKeyStorage(context)
    val testKey = "AIzaSyTestApiKey123456789"
    storage.saveApiKey(testKey)
    assertEquals(testKey, storage.getApiKey())
    assert(storage.hasValidApiKey())
    assert(storage.hasCompletedFirstLaunch())
  }
}
