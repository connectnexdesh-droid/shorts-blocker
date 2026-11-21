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
        assertTrue(DetectionRules.isBlockedLabel("com.google.android.youtube", "শর্টস"))
        assertTrue(DetectionRules.isBlockedLabel("com.google.android.youtube", "ইউটিউব শর্টস"))
    }

    @Test
    fun reelsLabelsAreRecognizedForInstagramAndFacebook() {
        assertTrue(DetectionRules.isBlockedLabel("com.instagram.android", "Reels"))
        assertTrue(DetectionRules.isBlockedLabel("com.instagram.android", "রিলস"))
        assertTrue(DetectionRules.isBlockedLabel("com.facebook.katana", "Watch Reels"))
        assertTrue(DetectionRules.isBlockedLabel("com.facebook.katana", "रील्स"))
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
