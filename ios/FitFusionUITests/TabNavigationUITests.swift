import XCTest

/// Tests tab navigation and verifies content on each tab using `-autoGuest`
/// to bypass login/onboarding and land directly on the dashboard.
final class TabNavigationUITests: XCTestCase {

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
        app.tap() // trigger any pending interruption monitors

        let tabBar = app.tabBars.firstMatch
        if !tabBar.waitForExistence(timeout: 10) {
            // Might still be on onboarding — try tapping through
            app.tap()
            sleep(2)
        }
        XCTAssertTrue(app.tabBars.firstMatch.waitForExistence(timeout: 5),
                      "Tab bar should appear after autoGuest")
    }

    // MARK: - Tab Existence & Selection

    func testAllFourTabsExist() throws {
        let tabBar = app.tabBars.firstMatch
        for label in ["Care", "Diet", "Train", "Workout"] {
            XCTAssertTrue(tabBar.buttons[label].exists, "Tab '\(label)' missing")
        }
    }

    func testCareTabIsSelectedByDefault() throws {
        let careTab = app.tabBars.firstMatch.buttons["Care"]
        XCTAssertTrue(careTab.isSelected, "Care should be the default selected tab")
    }

    // MARK: - Care Tab Content

    func testCareTabShowsConnectMyChart() throws {
        app.tabBars.firstMatch.buttons["Care"].tap()
        sleep(1)
        app.tap()
        let connectMyChart = app.staticTexts["Connect MyChart"]
        XCTAssertTrue(connectMyChart.waitForExistence(timeout: 5),
                      "Connect MyChart tile should be visible on Care tab")
    }

    func testCareTabShowsInsuranceCard() throws {
        app.tabBars.firstMatch.buttons["Care"].tap()
        sleep(1)
        app.tap()
        let insurance = app.staticTexts["Add insurance card"]
        XCTAssertTrue(insurance.waitForExistence(timeout: 5),
                      "Insurance card tile should be visible on Care tab")
    }

    func testCareTabShowsLabReport() throws {
        app.tabBars.firstMatch.buttons["Care"].tap()
        sleep(1)
        app.tap()
        let labReport = app.staticTexts["Snap lab report"]
        XCTAssertTrue(labReport.waitForExistence(timeout: 5),
                      "Lab report tile should be visible on Care tab")
    }

    func testCareTabShowsCarePlanSection() throws {
        app.tabBars.firstMatch.buttons["Care"].tap()
        sleep(1)
        app.tap()
        app.swipeUp()
        sleep(1)
        let carePlan = app.staticTexts["Care plan"]
        XCTAssertTrue(carePlan.waitForExistence(timeout: 5),
                      "Care plan section header should be visible")
    }

    // MARK: - Diet Tab Content

    func testDietTabShowsFoodDiaryLink() throws {
        app.tabBars.firstMatch.buttons["Diet"].tap()
        sleep(1)
        app.tap()
        app.swipeUp()
        sleep(1)
        let diary = app.staticTexts["Open food diary"]
        XCTAssertTrue(diary.waitForExistence(timeout: 5),
                      "Food diary link should be visible on Diet tab")
    }

    // MARK: - Train Tab Content

    func testTrainTabShowsTodaysPlan() throws {
        app.tabBars.firstMatch.buttons["Train"].tap()
        sleep(1)
        app.tap()
        app.swipeUp()
        sleep(1)
        let todaysPlan = app.staticTexts["Today's plan"]
        XCTAssertTrue(todaysPlan.waitForExistence(timeout: 5),
                      "Today's plan section should be visible on Train tab")
    }

    // MARK: - Workout Tab Content

    func testWorkoutTabShowsRPERating() throws {
        app.tabBars.firstMatch.buttons["Workout"].tap()
        sleep(1)
        app.tap()
        let rpe = app.staticTexts["Rate that workout (RPE)"]
        XCTAssertTrue(rpe.waitForExistence(timeout: 5),
                      "RPE rating tile should be visible on Workout tab")
    }

    func testWorkoutTabShowsRunTracker() throws {
        app.tabBars.firstMatch.buttons["Workout"].tap()
        sleep(1)
        app.tap()
        app.swipeUp()
        sleep(1)
        let run = app.staticTexts["Run / walk tracker"]
        XCTAssertTrue(run.waitForExistence(timeout: 5),
                      "Run/walk tracker should be visible on Workout tab")
    }

    // MARK: - Tab Cycling

    func testTabCyclingReturnsToCare() throws {
        let tabBar = app.tabBars.firstMatch

        tabBar.buttons["Diet"].tap()
        sleep(1)
        tabBar.buttons["Train"].tap()
        sleep(1)
        tabBar.buttons["Workout"].tap()
        sleep(1)
        tabBar.buttons["Care"].tap()
        sleep(1)
        app.tap()

        let connectMyChart = app.staticTexts["Connect MyChart"]
        XCTAssertTrue(connectMyChart.waitForExistence(timeout: 5),
                      "Care tab content should restore after cycling")
    }
}
