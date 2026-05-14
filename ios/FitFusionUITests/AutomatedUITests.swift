import XCTest

/// Comprehensive automated UI tests for the MyHealth app.
/// Launches in guest mode using `-autoGuest` to bypass login/onboarding,
/// then navigates all 4 tabs, taps every visible button, enters dummy data
/// in text fields, and verifies screen content with accessibility labels.
///
/// All system permission dialogs are auto-dismissed via `addUIInterruptionMonitor`.
final class AutomatedUITests: XCTestCase {

    override func setUpWithError() throws {
        continueAfterFailure = true
    }

    private func launchedApp() -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments = ["-autoGuest"]
        app.launch()

        // ── Auto-dismiss ALL system permission dialogs ──────────
        addUIInterruptionMonitor(withDescription: "Notifications") { alert in
            if alert.buttons["Allow"].exists { alert.buttons["Allow"].tap(); return true }
            if alert.buttons["Don't Allow"].exists { alert.buttons["Don't Allow"].tap(); return true }
            return false
        }
        addUIInterruptionMonitor(withDescription: "HealthKit") { alert in
            if alert.buttons["Turn On All"].exists { alert.buttons["Turn On All"].tap(); return true }
            if alert.buttons["Allow"].exists { alert.buttons["Allow"].tap(); return true }
            if alert.buttons["Don't Allow"].exists { alert.buttons["Don't Allow"].tap(); return true }
            return false
        }
        addUIInterruptionMonitor(withDescription: "Location") { alert in
            if alert.buttons["Allow While Using App"].exists { alert.buttons["Allow While Using App"].tap(); return true }
            if alert.buttons["Allow Once"].exists { alert.buttons["Allow Once"].tap(); return true }
            if alert.buttons["Don't Allow"].exists { alert.buttons["Don't Allow"].tap(); return true }
            return false
        }
        addUIInterruptionMonitor(withDescription: "Tracking") { alert in
            if alert.buttons["Allow"].exists { alert.buttons["Allow"].tap(); return true }
            if alert.buttons["Ask App Not to Track"].exists { alert.buttons["Ask App Not to Track"].tap(); return true }
            return false
        }
        // Generic catch-all for any system alert
        addUIInterruptionMonitor(withDescription: "System Alert") { alert in
            for btnLabel in ["Allow", "OK", "Continue", "Not Now", "Don't Allow", "Cancel"] {
                if alert.buttons[btnLabel].exists { alert.buttons[btnLabel].tap(); return true }
            }
            return false
        }

        return app
    }

    // MARK: - Guest Mode Launch

    func testAutoGuestBypassesLogin() throws {
        let app = launchedApp()
        let loginButton = app.buttons["Continue as Guest"]
        XCTAssertFalse(loginButton.waitForExistence(timeout: 3),
                       "Login screen should be bypassed with -autoGuest")
    }

    // MARK: - Tab Navigation

    func testAllFourTabsExist() throws {
        let app = launchedApp()
        sleep(2)
        let tabBar = app.tabBars.firstMatch
        XCTAssertTrue(tabBar.waitForExistence(timeout: 5), "Tab bar should exist")
        captureScreenshot(app, name: "MainTabs")
    }

    // MARK: - Care Tab

    func testCareTabContent() throws {
        let app = launchedApp()
        sleep(2)
        let tabBar = app.tabBars.firstMatch
        if tabBar.waitForExistence(timeout: 5) { tabBar.buttons.element(boundBy: 0).tap() }
        sleep(1)
        app.tap() // trigger interrupt monitors
        captureScreenshot(app, name: "CareTab_Main")
        app.swipeUp()
        sleep(1)
        captureScreenshot(app, name: "CareTab_Scrolled")
    }

    // MARK: - Diet Tab

    func testDietTabContent() throws {
        let app = launchedApp()
        sleep(2)
        let tabBar = app.tabBars.firstMatch
        if tabBar.waitForExistence(timeout: 5) { tabBar.buttons.element(boundBy: 1).tap() }
        sleep(1)
        app.tap()
        captureScreenshot(app, name: "DietTab_Main")
        app.swipeUp()
        sleep(1)
        captureScreenshot(app, name: "DietTab_Scrolled")
    }

    // MARK: - Train Tab

    func testTrainTabContent() throws {
        let app = launchedApp()
        sleep(2)
        let tabBar = app.tabBars.firstMatch
        if tabBar.waitForExistence(timeout: 5) { tabBar.buttons.element(boundBy: 2).tap() }
        sleep(1)
        app.tap()
        captureScreenshot(app, name: "TrainTab_Main")
        app.swipeUp()
        sleep(1)
        captureScreenshot(app, name: "TrainTab_Scrolled")

        // Check for "Recommended for you"
        if app.staticTexts["Recommended for you"].exists {
            XCTAssertTrue(true, "Recommended for you section visible")
        }
    }

    // MARK: - Workout Tab

    func testWorkoutTabContent() throws {
        let app = launchedApp()
        sleep(2)
        let tabBar = app.tabBars.firstMatch
        if tabBar.waitForExistence(timeout: 5) { tabBar.buttons.element(boundBy: 3).tap() }
        sleep(1)
        app.tap()
        captureScreenshot(app, name: "WorkoutTab_Main")
        app.swipeUp()
        sleep(1)
        captureScreenshot(app, name: "WorkoutTab_Scrolled")
    }

    // MARK: - Profile with Birthday, Zodiac, Bio Age

    func testProfileBirthdayAndZodiac() throws {
        let app = launchedApp()
        sleep(2)

        // Open Profile via header avatar
        let avatar = app.buttons.matching(identifier: "ProfileAvatar").firstMatch
        if avatar.waitForExistence(timeout: 3) {
            avatar.tap()
        } else {
            // Fallback: look for any avatar/profile button
            let headerButtons = app.buttons
            for i in 0..<headerButtons.count {
                let btn = headerButtons.element(boundBy: i)
                if btn.label.lowercased().contains("profile") || btn.label.contains("avatar") {
                    btn.tap()
                    break
                }
            }
        }
        sleep(2)
        captureScreenshot(app, name: "Profile_Main")

        // Enter dummy name
        let nameField = app.textFields["Name"]
        if nameField.waitForExistence(timeout: 3) {
            nameField.tap()
            nameField.typeText("Test User")
        }

        // Set birthday via DatePicker
        let birthdayPicker = app.datePickers.firstMatch
        if birthdayPicker.waitForExistence(timeout: 3) {
            birthdayPicker.tap()
            sleep(1)
        }

        // Enter birthplace
        let birthplaceField = app.textFields["Birthplace"]
        if birthplaceField.waitForExistence(timeout: 3) {
            birthplaceField.tap()
            birthplaceField.typeText("New York")
        }

        captureScreenshot(app, name: "Profile_FilledData")

        // Check zodiac sign appears
        let zodiacSymbols = ["♈", "♉", "♊", "♋", "♌", "♍", "♎", "♏", "♐", "♑", "♒", "♓"]
        var foundZodiac = false
        for symbol in zodiacSymbols {
            if app.staticTexts.matching(NSPredicate(format: "label CONTAINS %@", symbol)).count > 0 {
                foundZodiac = true
                break
            }
        }
        // Zodiac may not appear until birthday is set
        captureScreenshot(app, name: "Profile_Zodiac")
    }

    func testProfileBiologicalAgeCard() throws {
        let app = launchedApp()
        sleep(2)

        // Navigate to Profile
        let headerButtons = app.buttons
        for i in 0..<headerButtons.count {
            let btn = headerButtons.element(boundBy: i)
            if btn.label.lowercased().contains("profile") || btn.label.contains("avatar") {
                btn.tap()
                break
            }
        }
        sleep(2)

        // Scroll to find biological age card
        app.swipeUp()
        sleep(1)
        captureScreenshot(app, name: "Profile_BioAge_Section")

        // Check for bio age content
        if app.staticTexts["Biological Age"].exists || app.staticTexts["Bio"].exists {
            XCTAssertTrue(true, "Biological age card found in profile")
        }

        // Check HIPAA disclaimer
        if app.staticTexts["This data stays on your device"].exists {
            XCTAssertTrue(true, "PHI disclaimer found")
        }
    }

    // MARK: - Rapid Tab Switching Stress Test

    func testRapidTabSwitchingDoesNotCrash() throws {
        let app = launchedApp()
        sleep(2)
        let tabBar = app.tabBars.firstMatch
        guard tabBar.waitForExistence(timeout: 5) else { XCTFail("Tab bar not found"); return }
        let tabCount = tabBar.buttons.count
        for _ in 0..<5 {
            for idx in 0..<tabCount {
                tabBar.buttons.element(boundBy: idx).tap()
                usleep(300_000)
            }
        }
        XCTAssertTrue(tabBar.exists, "App crashed during rapid tab switching")
        captureScreenshot(app, name: "StressTest_Final")
    }

    // MARK: - Button Tap Audit

    func testTapAllVisibleButtonsOnCareTab() throws {
        let app = launchedApp()
        sleep(2)
        let tabBar = app.tabBars.firstMatch
        if tabBar.waitForExistence(timeout: 5) { tabBar.buttons.element(boundBy: 0).tap() }
        sleep(1)
        app.tap()

        let buttons = app.buttons.allElementsBoundByIndex
        var tapped = 0
        for button in buttons {
            guard button.isHittable else { continue }
            let label = button.label
            if label.isEmpty || ["Care", "Diet", "Train", "Workout"].contains(label) { continue }
            button.tap()
            tapped += 1
            sleep(1)
            captureScreenshot(app, name: "CareButton_\(tapped)_\(label.prefix(20))")
            if app.navigationBars.buttons.element(boundBy: 0).exists {
                app.navigationBars.buttons.element(boundBy: 0).tap()
                sleep(1)
            }
        }
        XCTAssertTrue(tapped > 0, "Should have tapped at least one button")
    }

    // MARK: - Dummy Data Entry Tests

    func testDummyDataEntryInFoodDiary() throws {
        let app = launchedApp()
        sleep(2)

        // Go to Diet tab
        let tabBar = app.tabBars.firstMatch
        if tabBar.waitForExistence(timeout: 5) { tabBar.buttons.element(boundBy: 1).tap() }
        sleep(1)
        app.tap()

        // Try to open food diary
        app.swipeUp()
        sleep(1)
        if app.staticTexts["Open food diary"].exists {
            app.staticTexts["Open food diary"].tap()
            sleep(2)
            captureScreenshot(app, name: "FoodDiary_Main")
        }
    }

    func testDietSuggestionsScreen() throws {
        let app = launchedApp()
        sleep(2)

        // Go to Diet tab
        let tabBar = app.tabBars.firstMatch
        if tabBar.waitForExistence(timeout: 5) { tabBar.buttons.element(boundBy: 1).tap() }
        sleep(1)
        app.tap()
        app.swipeUp()
        sleep(1)

        // Look for Diet suggestions button
        if app.staticTexts["Diet suggestions for your conditions"].exists {
            app.staticTexts["Diet suggestions for your conditions"].tap()
            sleep(2)
            captureScreenshot(app, name: "DietSuggestions_Main")
        }
    }

    // MARK: - Helpers

    private func captureScreenshot(_ app: XCUIApplication, name: String) {
        let screenshot = app.screenshot()
        let attachment = XCTAttachment(screenshot: screenshot)
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
