import Foundation
import SwiftUI

class ErrorHandler: ObservableObject {
    @Published var currentError: AppError?
    @Published var showAlert = false

    func handle(_ error: Error, context: String = "") {
        let appError = AppError(message: error.localizedDescription, context: context)
        currentError = appError
        showAlert = true
    }
}

struct AppError: Identifiable {
    let id = UUID()
    let message: String
    let context: String
}
