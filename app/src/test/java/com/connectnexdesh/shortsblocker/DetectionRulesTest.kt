package com.connectnexdesh.shortsblocker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectionRulesTest {

    @Test
    fun supportedSocialAppsAreRecognized() {
        assertTrue(DetectionRules.isSupportedPackage("com.google.android.youtube"))
        assertTrue(DetectionRules.isSupportedPackage("com.instagram.android"))
        assertTrue(DetectionRules.isSupportedPackage("com.facebook.katana"))
        assertFalse(DetectionRules.isSupportedPackage("com.example.other"))
    }

    @Test
    fun youtubeShortsLabelsAreRecognized() {
        assertTrue(DetectionRules.isBlockedLabel("com.google.android.youtube", "Shorts"))
        assertTrue(DetectionRules.isBlockedLabel("com.google.android.youtube", "YouTube Shorts"))
        assertTrue(DetectionRules.isBlockedLabel("com.google.android.youtube", "Shorts, tab 2 of 5"))
        assertTrue(DetectionRules.isBlockedLabel("com.google.android.youtube", "Shorts shelf"))
        assertTrue(DetectionRules.isBlockedLabel("com.google.android.youtube", "com.google.android.youtube:id/shorts_tab"))
        assertTrue(DetectionRules.isBlockedLabel("com.google.android.youtube", "শর্টস"))
    }

    @Test
    fun reelsLabelsAreRecognizedForInstagramAndFacebook() {
        assertTrue(DetectionRules.isBlockedLabel("com.instagram.android", "Reels"))
        assertTrue(DetectionRules.isBlockedLabel("com.instagram.android", "Reels, tab 2 of 5"))
        assertTrue(DetectionRules.isBlockedLabel("com.instagram.android", "com.instagram.android:id/clips_tab"))
        assertTrue(DetectionRules.isBlockedLabel("com.facebook.katana", "Watch Reels"))
        assertTrue(DetectionRules.isBlockedLabel("com.facebook.katana", "রিলস"))
    }

    @Test
    fun ordinaryVideoLabelsAreNotMatched() {
        assertFalse(DetectionRules.isBlockedLabel("com.google.android.youtube", "Home"))
        assertFalse(DetectionRules.isBlockedLabel("com.google.android.youtube", "Subscriptions"))
        assertFalse(DetectionRules.isBlockedLabel("com.instagram.android", "Stories"))
        assertFalse(DetectionRules.isBlockedLabel("com.facebook.katana", "Videos"))
    }

    @Test
    fun labelsAreTrimmedAndCaseInsensitive() {
        assertTrue(DetectionRules.isBlockedLabel("com.google.android.youtube", "  SHORTS  "))
        assertTrue(DetectionRules.isBlockedLabel("com.instagram.android", "Reels Tab"))
    }
}
