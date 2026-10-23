package com.example.domain.legal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test suite verifying Requirement #22:
 * LifeOS First-Time Terms & Conditions and Privacy Consent.
 */
class LegalConsentUnitTest {

    @Test
    fun defaultState_freshInstall_consentIsNotComplete() {
        // Upon fresh install, DataStore holds null for accepted versions
        val isComplete = LegalConsentManager.isConsentComplete(
            acceptedTermsVersion = null,
            acceptedPrivacyVersion = null
        )
        assertFalse("Fresh install with null versions must NOT be considered complete", isComplete)
    }

    @Test
    fun partialAcceptance_onlyTerms_consentIsNotComplete() {
        val isComplete = LegalConsentManager.isConsentComplete(
            acceptedTermsVersion = LegalDocuments.CURRENT_TERMS_VERSION,
            acceptedPrivacyVersion = null
        )
        assertFalse("Accepting only Terms without Privacy acknowledgment must NOT be complete", isComplete)
    }

    @Test
    fun partialAcceptance_onlyPrivacy_consentIsNotComplete() {
        val isComplete = LegalConsentManager.isConsentComplete(
            acceptedTermsVersion = null,
            acceptedPrivacyVersion = LegalDocuments.CURRENT_PRIVACY_VERSION
        )
        assertFalse("Accepting only Privacy without Terms agreement must NOT be complete", isComplete)
    }

    @Test
    fun blankOrEmptyVersions_consentIsNotComplete() {
        assertFalse("Empty strings must not be considered valid acceptance",
            LegalConsentManager.isConsentComplete("", "")
        )
        assertFalse("Blank strings must not be considered valid acceptance",
            LegalConsentManager.isConsentComplete("   ", "   ")
        )
    }

    @Test
    fun bothCurrentVersionsAccepted_consentIsComplete() {
        val isComplete = LegalConsentManager.isConsentComplete(
            acceptedTermsVersion = LegalDocuments.CURRENT_TERMS_VERSION,
            acceptedPrivacyVersion = LegalDocuments.CURRENT_PRIVACY_VERSION
        )
        assertTrue("When both active versions match current documents, consent must be complete", isComplete)
    }

    @Test
    fun legalTermsVersionBump_requiresReconsent() {
        // User accepted v1.0.0
        val acceptedTerms = "1.0.0"
        val acceptedPrivacy = "1.0.0"

        // New legal terms released: v1.1.0
        val newTerms = "1.1.0"
        val newPrivacy = "1.0.0"

        val isComplete = LegalConsentManager.isConsentComplete(
            acceptedTermsVersion = acceptedTerms,
            acceptedPrivacyVersion = acceptedPrivacy,
            targetTermsVersion = newTerms,
            targetPrivacyVersion = newPrivacy
        )
        assertFalse("Outdated Terms version must require re-consent", isComplete)

        val reconsentNeeded = LegalConsentManager.requiresReconsent(
            acceptedTermsVersion = acceptedTerms,
            acceptedPrivacyVersion = acceptedPrivacy,
            newTermsVersion = newTerms,
            newPrivacyVersion = newPrivacy
        )
        assertTrue("requiresReconsent must return true when Terms version increments", reconsentNeeded)
    }

    @Test
    fun privacyPolicyVersionBump_requiresReconsent() {
        val acceptedTerms = "1.0.0"
        val acceptedPrivacy = "1.0.0"

        val newTerms = "1.0.0"
        val newPrivacy = "1.2.0"

        val isComplete = LegalConsentManager.isConsentComplete(
            acceptedTermsVersion = acceptedTerms,
            acceptedPrivacyVersion = acceptedPrivacy,
            targetTermsVersion = newTerms,
            targetPrivacyVersion = newPrivacy
        )
        assertFalse("Outdated Privacy version must require re-consent", isComplete)

        val reconsentNeeded = LegalConsentManager.requiresReconsent(
            acceptedTermsVersion = acceptedTerms,
            acceptedPrivacyVersion = acceptedPrivacy,
            newTermsVersion = newTerms,
            newPrivacyVersion = newPrivacy
        )
        assertTrue("requiresReconsent must return true when Privacy version increments", reconsentNeeded)
    }

    @Test
    fun appUpdateWithoutLegalBump_doesNotRequireReconsent() {
        // App code updated (e.g. bugfixes / new widgets), but legal documents unchanged
        val acceptedTerms = LegalDocuments.CURRENT_TERMS_VERSION
        val acceptedPrivacy = LegalDocuments.CURRENT_PRIVACY_VERSION

        val reconsentNeeded = LegalConsentManager.requiresReconsent(
            acceptedTermsVersion = acceptedTerms,
            acceptedPrivacyVersion = acceptedPrivacy
        )
        assertFalse("App updates with unchanged legal versions must not re-prompt existing users", reconsentNeeded)
    }

    @Test
    fun termsDocument_containsMandatorySections() {
        val terms = LegalDocuments.TERMS_OF_SERVICE_CONTENT

        assertTrue("Terms must contain draft disclaimer for owner review",
            terms.contains("DISCLAIMER / DRAFT REVIEW NOTICE")
        )
        assertTrue("Terms must state version number",
            terms.contains(LegalDocuments.CURRENT_TERMS_VERSION)
        )
        assertTrue("Terms must define offline-first nature",
            terms.contains("OFFLINE-FIRST")
        )
        assertTrue("Terms must clarify health and medical disclaimer",
            terms.contains("NOT a medical device")
        )
        assertTrue("Terms must contain warranty disclaimer",
            terms.contains("AS IS")
        )
        assertTrue("Terms must contain limitation of liability",
            terms.contains("LIMITATION OF LIABILITY")
        )
        assertTrue("Terms must specify user responsibility for data and backups",
            terms.contains("USER RESPONSIBILITY FOR DATA & BACKUPS")
        )
    }

    @Test
    fun privacyDocument_containsMandatorySections() {
        val privacy = LegalDocuments.PRIVACY_POLICY_CONTENT

        assertTrue("Privacy policy must contain draft disclaimer for owner review",
            privacy.contains("DISCLAIMER / DRAFT REVIEW NOTICE")
        )
        assertTrue("Privacy policy must state version number",
            privacy.contains(LegalDocuments.CURRENT_PRIVACY_VERSION)
        )
        assertTrue("Privacy policy must state zero remote telemetry",
            privacy.contains("Zero Remote Telemetry")
        )
        assertTrue("Privacy policy must state zero ad networks",
            privacy.contains("Zero Ad Networks")
        )
        assertTrue("Privacy policy must state on-device local storage",
            privacy.contains("100% On-Device")
        )
        assertTrue("Privacy policy must explain permission usage",
            privacy.contains("POST_NOTIFICATIONS") && privacy.contains("SCHEDULE_EXACT_ALARM")
        )
        assertTrue("Privacy policy must clarify no network access required",
            privacy.contains("INTERNET")
        )
    }

    @Test
    fun versionConstants_areValidSemanticVersions() {
        val semVerRegex = Regex("""^\d+\.\d+\.\d+(-[a-zA-Z0-9.]+)?$""")
        assertTrue("Terms version must follow semantic versioning",
            LegalDocuments.CURRENT_TERMS_VERSION.matches(semVerRegex)
        )
        assertTrue("Privacy version must follow semantic versioning",
            LegalDocuments.CURRENT_PRIVACY_VERSION.matches(semVerRegex)
        )
    }
}
