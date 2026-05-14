import XCTest

/// Tests the login screen before entering the app.
/// Uses `-uiTesting -resetState` so the app always starts on login.
final class LoginFlowUITests: XCTestCase {

    private var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["-uiTesting", "-resetState"]
        app.launch()
    }

    func testLoginScreenShowsEmailAndPasswordFields() throws {
        let email = app.textFields["Email"]
        XCTAssertTrue(email.waitForExistence(timeout: 5), "Email field should be visible")
        let password = app.secureTextFields["Password"]
        XCTAssertTrue(password.waitForExistence(timeout: 3), "Password field should be visible")
    }

    func testLoginScreenShowsSignInButton() throws {
        let signIn = app.buttons["Sign In"]
        XCTAssertTrue(signIn.waitForExistence(timeout: 5), "Sign In button should be visible")
    }

    func testLoginScreenShowsContinueAsGuest() throws {
        let guest = app.buttons["Continue as Guest"]
        XCTAssertTrue(guest.waitForExistence(timeout: 5),
                      "Continue as Guest button should be visible")
    }

    func testLoginScreenShowsCreateAccount() throws {
        let create = app.buttons["Create an account"]
        XCTAssertTrue(create.waitForExistence(timeout: 5),
                      "Create an account button should be visible")
    }

    func testGuestLoginNavigatesToApp() throws {
        let guest = app.buttons["Continue as Guest"]
        XCTAssertTrue(guest.waitForExistence(timeout: 5))
        guest.tap()

        // Should see either onboarding or the tab bar
        let tabBar = app.tabBars.firstMatch
        let welcome = app.staticTexts["Welcome to MyHealth"]
        let found = tabBar.waitForExistence(timeout: 10)
            || welcome.waitForExistence(timeout: 3)
        XCTAssertTrue(found, "App should navigate past login after guest tap")
    }
}
