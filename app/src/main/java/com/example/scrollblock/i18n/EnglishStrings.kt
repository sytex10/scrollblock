package com.example.scrollblock.i18n

object EnglishStrings : AppStrings {
    // Top Bar & General Status
    override val brandShort = "SHORT"
    override val brandBan = "BAN"
    override val versionTag = "v1.0"
    override val statusActive = "Active"
    override val statusPaused = "Paused"
    override val statusBreakMode = "Break"
    override val statusServiceOff = "Service Off"
    override val statusNeedPermission = "Permission Needed"

    // Permission Setup Banner
    override val permBannerTitle = "Accessibility Service is Disabled"
    override val permBannerDesc = "Enable the Accessibility service so ShortBan can block short videos."
    override val permBannerRefresh = "Refresh"
    override val permBannerStepGuide = "Tap the button below to open Accessibility settings. Select ShortBan from the Installed Apps list and switch the toggle to ON."
    override val permBannerBtnStep1 = "Open Accessibility Settings"
    override val permBannerBtnStep2 = "App Info"

    // Stats Overview Card
    override val statsTitle = "Total Intercepted Feeds"
    override val statsResetButton = "Reset"
    override val statsNoBlocksYet = "No short video feeds blocked yet"
    override val statsLastBlockTemplate = "Last blocked: %s · %s"

    // Reset Stats Dialog
    override val resetDialogTitle = "Reset Counter"
    override val resetDialogMessage = "Are you sure you want to reset the blocked feeds counter to zero?"
    override val resetDialogCancel = "Cancel"
    override val resetDialogConfirm = "Reset"

    // Section Headers
    override val sectionHeaderDashboard = "DASHBOARD"
    override val sectionHeaderBreak = "BREAK & COOLDOWN"
    override val sectionHeaderPlatforms = "PROTECTION PLATFORMS"
    override val sectionHeaderSecurity = "SECURITY & SYSTEM"

    // Break / Cooldown Card
    override val breakTitle = "Temporary Break Mode"
    override val breakDesc = "Pause blocking temporarily for a controlled interval."
    override val breakLimitUnlimited = "Unlimited"
    override val breakLimitUsedTemplate = "%d/%d used"
    override val breakDurationLabel = "Select Break Duration"
    override val breakDailyLimitLabel = "Daily Break Allowance Limit"
    override val breakLimit1x = "1 time"
    override val breakLimit2x = "2 times"
    override val breakLimit3x = "3 times"
    override val breakStartButtonTemplate = "Start %d-Minute Break"
    override val breakLimitReached = "Daily Break Allowance Reached"
    override val breakRemainingHeader = "BREAK ACTIVE — TIME REMAINING"
    override val breakActiveDesc = "Protection is temporarily paused. It will automatically reactivate once the timer reaches zero."
    override val breakEndAndReturn = "End Break & Resume Protection"

    // Platforms Card
    override val platformInstagramTitle = "Instagram"
    override val platformInstagramDesc = "Redirects Reels tab to Home feed (won't exit the app)"
    override val platformInstagramDmTitle = "Allow first Reels video from DMs"
    override val platformInstagramDmDesc = "Only allows watching the video sent in chat; closes instantly if you swipe to the next reel."
    override val platformInstagramCommentsTitle = "Block comments (Comment Shield)"
    override val platformInstagramCommentsDesc = "Automatically closes comment sheets when opened in posts or Reels."

    override val platformTikTokTitle = "TikTok"
    override val platformTikTokDesc = "Redirects 'For You' feed to Inbox (won't exit the app)"
    override val platformTikTokDmTitle = "Allow first video from DMs"
    override val platformTikTokDmDesc = "Plays the shared video from messages; returns to chat when swiping to next video."
    override val platformTikTokCommentsTitle = "Block comments (Comment Shield)"
    override val platformTikTokCommentsDesc = "Automatically closes comment sheets when opened."

    override val platformYouTubeTitle = "YouTube Shorts"
    override val platformYouTubeDesc = "Closes full-screen Shorts player; regular long videos remain open"
    override val platformYouTubeCommentsTitle = "Block comments (Comment Shield)"
    override val platformYouTubeCommentsDesc = "Closes comments section across both long videos and Shorts."

    // Security & System Card
    override val securityPinTitle = "Parental / Security PIN Lock"
    override val securityPinDescOn = "Settings, toggles, and breaks are secured by 4-digit PIN."
    override val securityPinDescOff = "Protect app settings and break mode with a 4-digit PIN."
    override val securityChangePinBtn = "Change"

    override val securityAccessibilityTitle = "Accessibility Service Settings"
    override val securityAccessibilityDescRunning = "Service is active in background · Open settings"
    override val securityAccessibilityDescStopped = "Service is stopped · Tap to grant permission"
    override val securityOpenBtn = "Open"

    override val securityGuideTitle = "User Guide & Onboarding"
    override val securityGuideDesc = "Review how ShortBan operates and how permissions work"
    override val securityGuideBtn = "Guide"

    override val securityPrivacyTitle = "Privacy Policy & Accessibility Disclosure"
    override val securityPrivacyDesc = "Zero data collection guarantee and permission usage details"
    override val securityReadBtn = "Read"

    override val securityLanguageTitle = "App Language"
    override val securityLanguageDesc = "Switch between Turkish and English"

    override val securityVersionTitle = "Version"
    override val securityVersionSub = "ShortBan Mobile"

    // PIN Dialogs & Prompts
    override val pinVerifyDefaultTitle = "PIN Verification"
    override val pinVerifyDefaultSub = "Enter your 4-digit PIN to continue."
    override val pinFieldPlaceholder = "4-digit PIN"
    override val pinConfirmFieldPlaceholder = "Re-enter PIN"
    override val pinDialogCancelBtn = "Cancel"
    override val pinDialogConfirmBtn = "Confirm"

    override val pinSetupNewHeader = "Set New PIN"
    override val pinSetupSubHeader = "Choose a 4-digit PIN to safeguard settings and cooldowns."
    override val pinSetupMismatch = "PIN codes do not match."
    override val pinSetupMustBe4 = "PIN must be exactly 4 digits."
    override val pinSetupSaveBtn = "Save"

    // Specific PIN Protection Subtitles
    override val dialogPinResetStatsSub = "Enter PIN to reset the counter."
    override val dialogPinDurationTitle = "Break Duration"
    override val dialogPinDurationSub = "Enter PIN to change the break duration."
    override val dialogPinDailyLimitTitle = "Daily Limit"
    override val dialogPinDailyLimitSub = "Enter PIN to change daily break allowance."
    override val dialogPinStartBreakTitle = "Start Break"
    override val dialogPinStartBreakSub = "Enter PIN to start temporary break."
    override val dialogPinRemoveTitle = "Remove PIN Lock"
    override val dialogPinRemoveSub = "Enter current PIN to remove security lock."
    override val dialogPinChangeTitle = "Change PIN"
    override val dialogPinChangeSub = "First verify your current PIN code."
    override val dialogPinIgDisableSub = "Enter PIN to disable Instagram blocking."
    override val dialogPinIgDmSub = "Enter PIN to change Instagram DM allowance."
    override val dialogPinIgCommentsSub = "Enter PIN to disable Instagram comment shield."
    override val dialogPinTtDisableSub = "Enter PIN to disable TikTok blocking."
    override val dialogPinTtDmSub = "Enter PIN to change TikTok DM allowance."
    override val dialogPinTtCommentsSub = "Enter PIN to disable TikTok comment shield."
    override val dialogPinYtDisableSub = "Enter PIN to disable YouTube Shorts blocking."
    override val dialogPinYtCommentsSub = "Enter PIN to disable YouTube comment shield."

    // Toasts
    override val toastMolaBitirildi = "Break ended. Protection is now active."
    override val toastMolaHakkiDoldu = "Today's break allowance has been exhausted."
    override val toastPinRemoved = "PIN lock removed."
    override val toastPinSaved = "PIN saved successfully."
    override val toastPinLength = "PIN must be 4 digits."
    override val toastWrongPinLeftTemplate = "Incorrect PIN (%d attempts remaining)"
    override val toastLockedOutTemplate = "Too many failed attempts. Please wait %d seconds."

    // Privacy & Disclosure Dialog
    override val privacyDialogTitle = "Prominent Disclosure & Privacy Policy"
    override val privacyDialogIntro = "ShortBan runs strictly on your device to intercept addictive short video streams (Instagram Reels, TikTok, YouTube Shorts) and toxic comment sections, guiding you back to safety."
    override val privacySection1 = "1. Accessibility Service Disclosure:\nShortBan uses the Android AccessibilityService API solely to detect when short-video reels or comment windows are opened in supported apps. Personal messages (DMs), keyboard entries, banking apps, and sensitive screens are never read or touched."
    override val privacySection2 = "2. Zero Data Collection & Transmission:\nNo personal data, keystrokes, messages, passwords, screenshots, or browsing activity are recorded, collected, or uploaded to any remote server or third party. Everything runs 100% locally on your phone."
    override val privacySection3 = "3. On-Device Storage Only:\nProtection rules, cooldown counters, and your optional 4-digit PIN lock are saved exclusively in private local storage on your device."
    override val privacySection4 = "4. No Ads & No Trackers:\nShortBan contains zero advertising SDKs, tracking pixels, or third-party behavioral analytics tools."
    override val privacyDialogDismiss = "Cancel"
    override val privacyDialogAcknowledge = "Got it & Close"
    override val privacyDialogAcceptSettings = "Accept & Open Settings"

    // Onboarding Screen General
    override val onboardingBadgeGuide = "GUIDE"
    override val onboardingBtnSkip = "Skip Guide"
    override val onboardingBtnClose = "Close"
    override val onboardingBtnBack = "Back"
    override val onboardingBtnAcceptContinue = "Accept & Continue"
    override val onboardingBtnNext = "Next Step"
    override val onboardingBtnFinish = "Finish Setup & Start"
    override val onboardingServiceActive = "Accessibility Service is ON (Active)"
    override val onboardingServiceInactive = "Accessibility Service is OFF (Permission Required)"

    // Onboarding Step 1
    override val onboardingStep1LabelTemplate = "STEP 01 / %s · HOW IT WORKS"
    override val onboardingStep1Title = "Smartly Block Short Videos & Toxic Feeds"
    override val onboardingStep1Subtitle = "ShortBan doesn't close your favorite apps; it safely intercepts endless doomscrolling and toxic feeds, routing you back to calm screens:"
    override val onboardingStep1IgTitle = "Instagram Reels -> Home Feed"
    override val onboardingStep1IgBody = "Hides the Reels tab. Tapping it won't crash or close the app; it smoothly redirects you to your Home feed. Stories, posts, profiles, and DMs remain fully functional."
    override val onboardingStep1TtTitle = "TikTok -> Direct Inbox"
    override val onboardingStep1TtBody = "Blocks the addictive 'For You' video feed and automatically routes you to your Inbox. You can message friends and manage your profile without endless scrolling."
    override val onboardingStep1YtTitle = "YouTube Shorts -> Standard Videos"
    override val onboardingStep1YtBody = "Disables the vertical Shorts player and hides quick entry points. Enjoy regular educational long-form videos and subscriptions distraction-free."
    override val onboardingStep1Footer = "Even if you spam tap the reels button, built-in anti-spam protection intercepts and prevents short video playback."

    // Onboarding Step 2
    override val onboardingStep2LabelTemplate = "STEP 02 / %s · FEATURES"
    override val onboardingStep2Title = "Feed Masking, Comment Shield, Cooldown & PIN"
    override val onboardingStep2Subtitle = "Carefully engineered tools to protect your focus and digital wellbeing without disrupting daily life:"
    override val onboardingStep2HideTag = "HIDE"
    override val onboardingStep2HideTitle = "Mask Short Video Buttons (New)"
    override val onboardingStep2HideBody = "Overlays and blends short video entrance buttons with the app's current dark/light theme, preventing accidental taps."
    override val onboardingStep2DmTag = "DM"
    override val onboardingStep2DmTitle = "Allow First DM Video Share"
    override val onboardingStep2DmBody = "Watch reels shared by your friends in direct messages. The moment you swipe down to browse the next video, ShortBan instantly closes the feed and returns to chat."
    override val onboardingStep2PinTag = "PIN"
    override val onboardingStep2PinTitle = "Adjustable Cooldown & 4-Digit PIN Lock"
    override val onboardingStep2PinBody = "Take intentional breaks when needed, or lock rules behind a 4-digit PIN to prevent impulsive relapses or safeguard kids."
    override val onboardingStep2Footer = "Toggle any of these rules with a single tap anytime from the main dashboard."

    // Onboarding Step 3 (Privacy)
    override val onboardingStep3LabelTemplate = "STEP 03 / %s · PRIVACY & CONSENT"
    override val onboardingStep3Title = "Prominent Privacy & Accessibility Consent"
    override val onboardingStep3Subtitle = "Our strict privacy commitment and Android AccessibilityService usage details:"
    override val onboardingStep3Item1Title = "Accessibility Service Disclosure"
    override val onboardingStep3Item1Body = "ShortBan strictly uses the Android AccessibilityService API to detect when short video reels or comment panels are actively opened in Instagram, TikTok, or YouTube, and safely navigates back. DMs, typed text, keyboard entries, and banking apps are never monitored or touched."
    override val onboardingStep3Item2Title = "Zero Data Collection & Transmission"
    override val onboardingStep3Item2Body = "No personal data, messages, passwords, screenshots, or browsing history are ever recorded, collected, or transmitted to any server or third party. All detection happens 100% locally on your device."
    override val onboardingStep3Item3Title = "100% On-Device Preferences & No Ads"
    override val onboardingStep3Item3Body = "Your protection toggles, cooldown limits, blocked counters, and PIN code remain stored strictly on your device. ShortBan contains zero advertising SDKs or third-party trackers."
    override val onboardingStep3Footer = "By proceeding and enabling the Accessibility Service, you acknowledge and agree to this Prominent Disclosure and Privacy Policy."

    // Onboarding Step 4 (Direct Setup)
    override val onboardingStep4A12Label = "STEP 04 / 04 · QUICK SETUP"
    override val onboardingStep4A12Title = "Enable Accessibility Service"
    override val onboardingStep4A12Subtitle = "Enable the Accessibility service so ShortBan can automatically detect and block short videos:"
    override val onboardingStep4A12Button = "Open Accessibility Settings"
    override val onboardingStep4A12Footer = "After toggling the service on, tap 'Finish Setup & Start' below to enter the app."

    // Onboarding Step 4 (Android 13+ Guide)
    override val onboardingStep4A13Label = "STEP 04 / 05 · SETUP (PHASE 1)"
    override val onboardingStep4A13Title = "Accessibility Service Setup"
    override val onboardingStep4A13Subtitle = "To complete permissions, follow these steps:"
    override val onboardingStep4A13Item1Title = "Select ShortBan in Accessibility"
    override val onboardingStep4A13Item1Body = "Tap button 1 below and select ShortBan from the list."
    override val onboardingStep4A13Item2Title = "App Settings Menu"
    override val onboardingStep4A13Item2Body = "Tap button 2 below to view application details."
    override val onboardingStep4A13Item3Title = "Confirm Permissions"
    override val onboardingStep4A13Item3Body = "Review and confirm settings on your device."
    override val onboardingStep4A13BtnStep1 = "Step 1: Tap ShortBan in Accessibility"
    override val onboardingStep4A13BtnStep2 = "Step 2: App Info"
    override val onboardingStep4A13Footer = "Pro Tip: In the Accessibility menu, look under Installed Apps > ShortBan and turn the service ON."

    // Onboarding Step 5 (Android 13+)
    override val onboardingStep5A13Label = "STEP 05 / 05 · SETUP (PHASE 2)"
    override val onboardingStep5A13Title = "Activate Accessibility Protection"
    override val onboardingStep5A13Subtitle = "Now that restricted settings are allowed, you can activate ShortBan's background shield:"
    override val onboardingStep5A13Item1Title = "Open Accessibility Settings Again"
    override val onboardingStep5A13Item1Body = "Tap button 3 below to return to the Accessibility menu."
    override val onboardingStep5A13Item2Title = "Turn ShortBan Switch ON"
    override val onboardingStep5A13Item2Body = "Find ShortBan under 'Downloaded Apps' (or Installed Services) and switch it ON."
    override val onboardingStep5A13Item3Title = "100% Privacy & Local Processing"
    override val onboardingStep5A13Item3Body = "ShortBan operates completely offline. No screen content, keystrokes, or media are ever logged or sent over the internet."
    override val onboardingStep5A13BtnStep3 = "Step 3: Open Accessibility & Turn On"
    override val onboardingStep5A13Footer = "After toggling the service on, tap 'Finish Setup & Start' below to enter the dashboard."

    // Onboarding Privacy Modal
    override val onboardingModalTitle = "Privacy Policy & Accessibility Consent"
    override val onboardingModalDesc = "ShortBan strictly uses the Android AccessibilityService API to detect when short video reels or comment panels are actively opened in Instagram, TikTok, or YouTube, and safely navigates back."
    override val onboardingModalBullet1 = "• Zero Data Collection: No personal data, messages, passwords, or screenshots are ever stored or transmitted over the internet."
    override val onboardingModalBullet2 = "• 100% On-Device: All detection runs in real-time on your phone; private DMs, keyboards, and other apps are never touched."
    override val onboardingModalCancel = "Cancel"
    override val onboardingModalAccept = "Accept & Open Settings"

    // Quick Settings Tile
    override val tileLabel = "ShortBan"
    override val tileStateActive = "Protection Active"
    override val tileStatePaused = "Paused"
    override val tileStateBreak = "Break Active"
    override val tileStateServiceOff = "Service Off"
}
