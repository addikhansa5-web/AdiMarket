package com.example.adimarket;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Phase 13: AI Integration Testing
 * Menguji integrasi antar fitur AI di AdiMarket
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AIIntegrationTest {

    @Rule
    public ActivityScenarioRule<DashboardActivity> activityRule =
            new ActivityScenarioRule<>(DashboardActivity.class);

    @Test
    public void testNavigationToAIModules() {
        // 1. Uji Navigasi ke Menu Jual (Input untuk AI Fraud & Image Recog)
        onView(withId(R.id.btnJual)).perform(click());
        onView(withId(R.id.etNamaKendaraan)).check(matches(isDisplayed()));
    }

    @Test
    public void testAIComponentInitialization() {
        // Simulasi pengujian komponen AI secara programatik
        // Di sini kita memastikan class AI bisa diinstansiasi tanpa error
        SentimentAnalyzer analyzer = new SentimentAnalyzer(null);
        analyzer.analyze("Mobil ini sangat bagus dan terawat");
        
        FraudDetection fraud = new FraudDetection();
        fraud.isSuspicious("Jual cepat mobil murah bangat", "1000000");
    }
}
