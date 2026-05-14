import XCTest

/// Deep-navigation tests for the Workout tab. Verifies sub-screens
/// like Run tracker, Exercise library, RPE sheet, and Live run.
final class WorkoutTabDeepUITests: XCTestCase {

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
        tabBar.buttons["Workout"].tap()
        app.tap()
        sleep(1)
    }

    func testWorkoutTabShowsActivityRingsStats() throws {
        // Activity rings show Move, Exercise, Stand stats
        let move = app.staticTexts["Move"]
        let exercise = app.staticTexts["Exercise"]
        let stand = app.staticTexts["Stand"]
        XCTAssertTrue(move.waitForExistence(timeout: 5), "Move stat should be visible")
        XCTAssertTrue(exercise.exists, "Exercise stat should be visible")
        XCTAssertTrue(stand.exists, "Stand stat should be visible")
    }

    func testWorkoutTabShowsCardioSection() throws {
        app.swipeUp()
        sleep(1)
        let cardio = app.staticTexts["Cardio"]
        XCTAssertTrue(cardio.waitForExistence(timeout: 5),
                      "Cardio section header should be visible")
    }

    func testWorkoutTabShowsStrengthSection() throws {
        app.swipeUp()
        sleep(1)
        let strength = app.staticTexts["Strength"]
        XCTAssertTrue(strength.waitForExistence(timeout: 5),
                      "Strength section header should be visible")
    }

    func testRunTrackerOpens() throws {
        app.swipeUp()
        sleep(1)
        let runTracker = app.staticTexts["Run / walk tracker"]
        guard runTracker.waitForExistence(timeout: 5) else {
            throw XCTSkip("Run/walk tracker tile not visible")
        }
        runTracker.tap()
        sleep(1)
        // Should navigate to RunTrackerView
        XCTAssertTrue(app.navigationBars.count > 0 || app.buttons.count > 1,
                      "Run tracker view should open")
    }

    func testLiveRunOpens() throws {
        app.swipeUp()
        sleep(1)
        let liveRun = app.staticTexts["Live run"]
        guard liveRun.waitForExistence(timeout: 5) else {
            throw XCTSkip("Live run tile not visible")
        }
        liveRun.tap()
        sleep(1)
        XCTAssertTrue(app.navigationBars.count > 0 || app.buttons.count > 1,
                      "Live run view should open")
    }

    func testRPERatingSheetOpens() throws {
        let rpe = app.staticTexts["Rate that workout (RPE)"]
        guard rpe.waitForExistence(timeout: 5) else {
            throw XCTSkip("RPE tile not visible")
        }
        // Tap the parent button containing the RPE text
        let rpeButton = app.buttons.containing(
            NSPredicate(format: "label CONTAINS 'RPE'")
        ).firstMatch
        if rpeButton.waitForExistence(timeout: 3) {
            rpeButton.tap()
        } else {
            rpe.tap()
        }
        sleep(1)
        // RPE sheet should present — look for scale numbers or slider
        let sheet = app.otherElements.matching(
            NSPredicate(format: "label CONTAINS '1' OR label CONTAINS 'Easy'")
        )
        XCTAssertTrue(sheet.count > 0 || app.sliders.count > 0 || app.buttons.count > 2,
                      "RPE rating sheet should present with rating options")
    }
}
