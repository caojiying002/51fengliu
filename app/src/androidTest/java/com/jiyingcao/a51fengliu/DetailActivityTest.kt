package com.jiyingcao.a51fengliu

import android.content.Intent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jiyingcao.a51fengliu.ui.DetailActivity

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Rule

@RunWith(AndroidJUnit4::class)
class DetailActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(DetailActivity::class.java)

    @Test
    fun testItemDataDisplayed() {
        // Assuming that DetailActivity is expecting an ItemData object as an Intent extra
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, DetailActivity::class.java).apply {
            //putExtra("ITEM_DATA", ItemData("Title", "Description")) // Replace with actual keys and data
        }

        activityRule.scenario.onActivity { activity ->
            // Here you can verify that the UI elements are displaying the correct data
            //onView(withId(R.id.itemTitle)).check(matches(withText("Title")))
            //onView(withId(R.id.itemDescription)).check(matches(withText("Description")))
        }
    }
}
