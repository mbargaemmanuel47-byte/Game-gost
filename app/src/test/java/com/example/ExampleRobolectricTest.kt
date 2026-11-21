package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.isFirebaseAvailable
import com.example.ui.resolveDefaultWebClientId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
    val dbId = context.getString(R.string.firestore_database_id)
    assertEquals("Ghost Line", appName)
    assertTrue(dbId.isNotBlank())
  }

  @Test
  fun `firebase availability and web client lookup never throw`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val available = isFirebaseAvailable(context)
    val webClientId = resolveDefaultWebClientId(context)
    assertNotNull(available)
    if (webClientId != null) {
      assertTrue(webClientId.isNotBlank())
    }
  }
}
