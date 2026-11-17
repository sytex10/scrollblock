package com.example.scrollblock.i18n

interface AppStrings {
    // Top Bar & General Status
    val brandShort: String
    val brandBan: String
    val versionTag: String
    val statusActive: String
    val statusPaused: String
    val statusBreakMode: String
    val statusServiceOff: String
    val statusNeedPermission: String

    // Permission Setup Banner
    val permBannerTitle: String
    val permBannerDesc: String
    val permBannerRefresh: String
    val permBannerStepGuide: String
    val permBannerBtnStep1: String
    val permBannerBtnStep2: String

    // Stats Overview Card
    val statsTitle: String
    val statsResetButton: String
    val statsNoBlocksYet: String
    val statsLastBlockTemplate: String

    // Reset Stats Dialog
    val resetDialogTitle: String
    val resetDialogMessage: String
    val resetDialogCancel: String
    val resetDialogConfirm: String

    // Section Headers
    val sectionHeaderDashboard: String
    val sectionHeaderBreak: String
    val sectionHeaderPlatforms: String
    val sectionHeaderSecurity: String

    // Break / Cooldown Card
    val breakTitle: String
    val breakDesc: String
    val breakLimitUnlimited: String
    val breakLimitUsedTemplate: String
    val breakDurationLabel: String
    val breakDailyLimitLabel: String
    val breakLimit1x: String
    val breakLimit2x: String
    val breakLimit3x: String
    val breakStartButtonTemplate: String
    val breakLimitReached: String
    val breakRemainingHeader: String
    val breakActiveDesc: String
    val breakEndAndReturn: String

    // Platforms Card
    val platformInstagramTitle: String
    val platformInstagramDesc: String
    val platformInstagramDmTitle: String
    val platformInstagramDmDesc: String
    val platformInstagramCommentsTitle: String
    val platformInstagramCommentsDesc: String

    val platformTikTokTitle: String
    val platformTikTokDesc: String
    val platformTikTokDmTitle: String
    val platformTikTokDmDesc: String
    val platformTikTokCommentsTitle: String
    val platformTikTokCommentsDesc: String

    val platformYouTubeTitle: String
    val platformYouTubeDesc: String
    val platformYouTubeCommentsTitle: String
    val platformYouTubeCommentsDesc: String

    // Security & System Card
    val securityPinTitle: String
    val securityPinDescOn: String
    val securityPinDescOff: String
    val securityChangePinBtn: String

    val securityAccessibilityTitle: String
    val securityAccessibilityDescRunning: String
    val securityAccessibilityDescStopped: String
    val securityOpenBtn: String

    val securityGuideTitle: String
    val securityGuideDesc: String
    val securityGuideBtn: String

    val securityPrivacyTitle: String
    val securityPrivacyDesc: String
    val securityReadBtn: String

    val securityLanguageTitle: String
    val securityLanguageDesc: String

    val securityVersionTitle: String
    val securityVersionSub: String

    // PIN Dialogs & Prompts
    val pinVerifyDefaultTitle: String
    val pinVerifyDefaultSub: String
    val pinFieldPlaceholder: String
    val pinConfirmFieldPlaceholder: String
    val pinDialogCancelBtn: String
    val pinDialogConfirmBtn: String

    val pinSetupNewHeader: String
    val pinSetupSubHeader: String
    val pinSetupMismatch: String
    val pinSetupMustBe4: String
    val pinSetupSaveBtn: String

    // Specific PIN Protection Subtitles
    val dialogPinResetStatsSub: String
    val dialogPinDurationTitle: String
    val dialogPinDurationSub: String
    val dialogPinDailyLimitTitle: String
    val dialogPinDailyLimitSub: String
    val dialogPinStartBreakTitle: String
    val dialogPinStartBreakSub: String
    val dialogPinRemoveTitle: String
    val dialogPinRemoveSub: String
    val dialogPinChangeTitle: String
    val dialogPinChangeSub: String
    val dialogPinIgDisableSub: String
    val dialogPinIgDmSub: String
    val dialogPinIgCommentsSub: String
    val dialogPinTtDisableSub: String
    val dialogPinTtDmSub: String
    val dialogPinTtCommentsSub: String
    val dialogPinYtDisableSub: String
    val dialogPinYtCommentsSub: String

    // Toasts
    val toastMolaBitirildi: String
    val toastMolaHakkiDoldu: String
    val toastPinRemoved: String
    val toastPinSaved: String
    val toastPinLength: String
    val toastWrongPinLeftTemplate: String
    val toastLockedOutTemplate: String

    // Privacy & Disclosure Dialog
    val privacyDialogTitle: String
    val privacyDialogIntro: String
    val privacySection1: String
    val privacySection2: String
    val privacySection3: String
    val privacySection4: String
    val privacyDialogDismiss: String
    val privacyDialogAcknowledge: String
    val privacyDialogAcceptSettings: String

    // Onboarding Screen General
    val onboardingBadgeGuide: String
    val onboardingBtnSkip: String
    val onboardingBtnClose: String
    val onboardingBtnBack: String
    val onboardingBtnAcceptContinue: String
    val onboardingBtnNext: String
    val onboardingBtnFinish: String
    val onboardingServiceActive: String
    val onboardingServiceInactive: String

    // Onboarding Step 1
    val onboardingStep1LabelTemplate: String
    val onboardingStep1Title: String
    val onboardingStep1Subtitle: String
    val onboardingStep1IgTitle: String
    val onboardingStep1IgBody: String
    val onboardingStep1TtTitle: String
    val onboardingStep1TtBody: String
    val onboardingStep1YtTitle: String
    val onboardingStep1YtBody: String
    val onboardingStep1Footer: String

    // Onboarding Step 2
    val onboardingStep2LabelTemplate: String
    val onboardingStep2Title: String
    val onboardingStep2Subtitle: String
    val onboardingStep2HideTag: String
    val onboardingStep2HideTitle: String
    val onboardingStep2HideBody: String
    val onboardingStep2DmTag: String
    val onboardingStep2DmTitle: String
    val onboardingStep2DmBody: String
    val onboardingStep2PinTag: String
    val onboardingStep2PinTitle: String
    val onboardingStep2PinBody: String
    val onboardingStep2Footer: String

    // Onboarding Step 3 (Privacy)
    val onboardingStep3LabelTemplate: String
    val onboardingStep3Title: String
    val onboardingStep3Subtitle: String
    val onboardingStep3Item1Title: String
    val onboardingStep3Item1Body: String
    val onboardingStep3Item2Title: String
    val onboardingStep3Item2Body: String
    val onboardingStep3Item3Title: String
    val onboardingStep3Item3Body: String
    val onboardingStep3Footer: String

    // Onboarding Step 4 (Android 12)
    val onboardingStep4A12Label: String
    val onboardingStep4A12Title: String
    val onboardingStep4A12Subtitle: String
    val onboardingStep4A12Button: String
    val onboardingStep4A12Footer: String

    // Onboarding Step 4 (Android 13+)
    val onboardingStep4A13Label: String
    val onboardingStep4A13Title: String
    val onboardingStep4A13Subtitle: String
    val onboardingStep4A13Item1Title: String
    val onboardingStep4A13Item1Body: String
    val onboardingStep4A13Item2Title: String
    val onboardingStep4A13Item2Body: String
    val onboardingStep4A13Item3Title: String
    val onboardingStep4A13Item3Body: String
    val onboardingStep4A13BtnStep1: String
    val onboardingStep4A13BtnStep2: String
    val onboardingStep4A13Footer: String

    // Onboarding Step 5 (Android 13+)
    val onboardingStep5A13Label: String
    val onboardingStep5A13Title: String
    val onboardingStep5A13Subtitle: String
    val onboardingStep5A13Item1Title: String
    val onboardingStep5A13Item1Body: String
    val onboardingStep5A13Item2Title: String
    val onboardingStep5A13Item2Body: String
    val onboardingStep5A13Item3Title: String
    val onboardingStep5A13Item3Body: String
    val onboardingStep5A13BtnStep3: String
    val onboardingStep5A13Footer: String

    // Onboarding Privacy Modal
    val onboardingModalTitle: String
    val onboardingModalDesc: String
    val onboardingModalBullet1: String
    val onboardingModalBullet2: String
    val onboardingModalCancel: String
    val onboardingModalAccept: String

    // Quick Settings Tile
    val tileLabel: String
    val tileStateActive: String
    val tileStatePaused: String
    val tileStateBreak: String
    val tileStateServiceOff: String
}
