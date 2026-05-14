import Foundation

enum BuildConfig {
    static let apiBaseURL: String = {
        #if DEBUG
        return "http://localhost:4000"
        #else
        return "https://api.myhealth.app"
        #endif
    }()

    static let appVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
    static let buildNumber = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "1"
}
