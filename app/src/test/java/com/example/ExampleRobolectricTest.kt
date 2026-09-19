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
    assertEquals("EnExpense", appName)
  }

  @Test
  fun `rule based categorizer classifies food receipts`() {
    val receiptText = "DOMINO'S PIZZA\n1x Pepperoni Pizza $14.99\nTotal: $14.99\nCash"
    val category = com.example.data.ai.RuleBasedCategorizer.categorize(receiptText)
    assertEquals("Food", category)

    val currency = com.example.data.ai.RuleBasedCategorizer.detectCurrency(receiptText)
    assertEquals("$", currency)
  }
}
