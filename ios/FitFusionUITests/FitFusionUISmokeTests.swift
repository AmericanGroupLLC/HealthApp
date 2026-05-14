import XCTest

/// Top-level UI smoke flows. Each test launches a fresh app instance with the
/// `-uiTesting` argument so the app can short-circuit any live network calls
/// and start in a clean Guest Mode state (see `FitFusionApp.swift` for the
/// `ProcessInfo.arguments` hook).
final class FitFusionUISmokeTests: XCTestCase {

    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    private func launchedApp() -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments = ["-uiTesting", "-resetState"]
        app.launch()
        return app
    }

    /// Verifies the very first thing a user sees \u{2014} the Login screen with the
    /// new "Continue as Guest" button.
    func testLoginShowsContinueAsGuest() throws {
        let app = launchedApp()
        let guestButton = app.buttons["Continue as Guest"]
        XCTAssertTrue(guestButton.waitForExistence(timeout: 5),
                      "Continue as Guest button missing on Login")
    }

    /// Tap Continue as Guest → verify Onboarding or Dashboard renders.
    func testGuestPathLandsOnOnboarding() throws {
        let app = launchedApp()
        let guestButton = app.buttons["Continue as Guest"]
        XCTAssertTrue(guestButton.waitForExistence(timeout: 10))
        guestButton.tap()

        // After tapping, the app shows either Onboarding or the dashboard.
        // Look for common onboarding elements or the tab bar.
        let welcome = app.staticTexts["Welcome to MyHealth"]
        let getStarted = app.buttons["Get started"]
        let homeTab = app.tabBars.buttons["Care"]
        let foundOne = welcome.waitForExistence(timeout: 10)
            || getStarted.waitForExistence(timeout: 3)
            || homeTab.waitForExistence(timeout: 3)
        XCTAssertTrue(foundOne, "Neither Welcome/Onboarding nor Home tab appeared")
    }

    /// Once on the dashboard, the bottom tab bar should expose every primary tab.
    func testBottomTabBarHasAllPrimaryTabs() throws {
        let app = XCUIApplication()
        app.launchArguments = ["-autoGuest"]
        app.launch()

        let tabs = ["Care", "Diet", "Train", "Workout"]
        for label in tabs {
            let tab = app.tabBars.buttons[label]
            XCTAssertTrue(tab.exists || tab.waitForExistence(timeout: 5),
                          "Tab '\(label)' missing")
        }
    }
}
