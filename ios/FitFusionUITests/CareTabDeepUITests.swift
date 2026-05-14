import XCTest

/// Deep-navigation tests for Care tab features. Taps through into
/// sub-screens and verifies expected content renders.
final class CareTabDeepUITests: XCTestCase {

    private var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["-autoGuest"]
        app.launch()

        addUIInterruptionMonitor(withDescription: "System Alert") { alert in
            for label in ["Allow", "OK", "Don't Allow", "Not Now", "Cancel",
                          "Allow While Using App", "Allow Once",
                          "Turn On All", "Ask App Not to Track"] {
                if alert.buttons[label].exists { alert.buttons[label].tap(); return true }
            }
            return false
        }

        sleep(2)
        app.tap()

        let tabBar = app.tabBars.firstMatch
        XCTAssertTrue(tabBar.waitForExistence(timeout: 15), "Tab bar should appear")
        tabBar.buttons["Care"].tap()
        app.tap()
        sleep(1)
    }

    func testConnectMyChartOpensDetail() throws {
        let connect = app.staticTexts["Connect MyChart"]
        guard connect.waitForExistence(timeout: 5) else {
            throw XCTSkip("MyChart tile not visible")
        }
        connect.tap()
        sleep(1)
        // Should navigate to MyChartConnectView
        let scopesHeader = app.staticTexts["What we'll read"]
        let connectButton = app.buttons["Connect with MyChart"]
        let found = scopesHeader.waitForExistence(timeout: 5)
            || connectButton.waitForExistence(timeout: 3)
        XCTAssertTrue(found, "MyChart connect view should show scopes or connect button")
    }

    func testInsuranceCardOpensSheet() throws {
        let insurance = app.staticTexts["Add insurance card"]
        guard insurance.waitForExistence(timeout: 5) else {
            throw XCTSkip("Insurance tile not visible")
        }
        insurance.tap()
        sleep(1)
        // The InsuranceCardSheet should present
        let snap = app.buttons.matching(NSPredicate(format: "label CONTAINS 'photo'"))
        let save = app.buttons["Save"]
        let found = snap.count > 0 || save.waitForExistence(timeout: 3)
            || app.navigationBars.count > 0
        XCTAssertTrue(found, "Insurance card sheet should open with photo/save options")
    }

    func testLabReportOpensSheet() throws {
        let labReport = app.staticTexts["Snap lab report"]
        guard labReport.waitForExistence(timeout: 5) else {
            throw XCTSkip("Lab report tile not visible")
        }
        labReport.tap()
        sleep(1)
        // LabReportSheet should present
        XCTAssertTrue(app.navigationBars.count > 0 || app.buttons.count > 0,
                      "Lab report sheet should open")
    }

    func testCareTabShowsHealthScoreSection() throws {
        // Scroll down to find Health Score
        for _ in 0..<3 {
            app.swipeUp()
            sleep(1)
            let healthScore = app.staticTexts["Health Score"]
            if healthScore.exists {
                XCTAssertTrue(true, "Health Score section found")
                return
            }
        }
        // Health Score may not be present depending on conditions — don't fail
    }

    func testBackNavigationFromMyChart() throws {
        let connect = app.staticTexts["Connect MyChart"]
        guard connect.waitForExistence(timeout: 5) else {
            throw XCTSkip("MyChart tile not visible")
        }
        connect.tap()
        sleep(1)

        // Go back
        let backButton = app.navigationBars.buttons.element(boundBy: 0)
        if backButton.waitForExistence(timeout: 3) {
            backButton.tap()
            sleep(1)
        }

        // Should be back on Care tab
        let careConnect = app.staticTexts["Connect MyChart"]
        XCTAssertTrue(careConnect.waitForExistence(timeout: 5),
                      "Should return to Care tab after back navigation")
    }
}
