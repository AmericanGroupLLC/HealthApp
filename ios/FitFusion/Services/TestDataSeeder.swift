import Foundation
import FitFusionCore
import CoreData

@MainActor
public final class TestDataSeeder {
    public static let shared = TestDataSeeder()
    private init() {}

    private var hasSeeded = false

    public func seedIfNeeded() {
        guard !hasSeeded,
              ProcessInfo.processInfo.arguments.contains("-seedTestData") else { return }
        hasSeeded = true
        seedAll()
    }

    private func seedAll() {
        let ctx = CloudStore.shared.viewContext

        seedProfile(ctx: ctx)
        seedMeals(ctx: ctx)
        seedMoodEntries(ctx: ctx)
        seedSymptomEntries(ctx: ctx)

        do {
            try ctx.save()
        } catch {
            print("[TestDataSeeder] Failed to save: \(error)")
        }
    }

    // MARK: - Profile

    private func seedProfile(ctx: NSManagedObjectContext) {
        let profile = NSEntityDescription.insertNewObject(forEntityName: "ProfileEntity", into: ctx)
        profile.setValue(UUID(), forKey: "id")
        profile.setValue("Test User Alpha", forKey: "name")
        profile.setValue("1996-01-15", forKey: "birthDateISO")
        profile.setValue("male", forKey: "sex")
        profile.setValue(175.0, forKey: "heightCm")
        profile.setValue(70.0, forKey: "weightKg")
        profile.setValue("maintain", forKey: "goal")
        profile.setValue("moderate", forKey: "activityLevel")
        profile.setValue(false, forKey: "unitsImperial")
        profile.setValue("system", forKey: "themeMode")
        profile.setValue("en", forKey: "language")
        profile.setValue(Date(), forKey: "updatedAt")
    }

    // MARK: - Meals

    private func seedMeals(ctx: NSManagedObjectContext) {
        let types = ["Breakfast", "Lunch", "Dinner"]
        let now = Date()
        let day: TimeInterval = 86_400

        for d in 0..<30 {
            for (idx, type) in types.enumerated() {
                let kcal = Double.random(in: 300...800)
                let meal = NSEntityDescription.insertNewObject(forEntityName: "MealEntity", into: ctx)
                meal.setValue(UUID(), forKey: "id")
                meal.setValue("Test \(type) \(d + 1)", forKey: "name")
                meal.setValue(kcal, forKey: "kcal")
                meal.setValue(kcal * 0.25 / 4, forKey: "protein")
                meal.setValue(kcal * 0.50 / 4, forKey: "carbs")
                meal.setValue(kcal * 0.25 / 9, forKey: "fat")
                meal.setValue(now.addingTimeInterval(-Double(d) * day + Double(idx) * 5 * 3600), forKey: "consumedAt")
            }
        }
    }

    // MARK: - Mood entries

    private func seedMoodEntries(ctx: NSManagedObjectContext) {
        let notes = [
            "Feeling great", "A bit tired", "Energized", "Stressed", "Calm and relaxed",
            "Good morning", "Post-workout high", "Need more sleep", "Productive day", "Winding down"
        ]
        let now = Date()
        let day: TimeInterval = 86_400

        for i in 0..<10 {
            let mood = NSEntityDescription.insertNewObject(forEntityName: "MoodEntryEntity", into: ctx)
            mood.setValue(UUID(), forKey: "id")
            mood.setValue(Int16.random(in: 1...5), forKey: "value")
            mood.setValue(notes[i], forKey: "note")
            mood.setValue(now.addingTimeInterval(-Double(i * 3) * day), forKey: "recordedAt")
        }
    }

    // MARK: - Symptom entries

    private func seedSymptomEntries(ctx: NSManagedObjectContext) {
        let symptoms: [(String, Int, String)] = [
            ("Head", 3, "Mild headache after screen time"),
            ("Lower back", 5, "Dull ache after sitting"),
            ("Right knee", 6, "Sharp pain during squats"),
            ("Neck", 4, "Stiffness in the morning"),
            ("Left shoulder", 3, "Soreness after workout"),
        ]
        let now = Date()
        let day: TimeInterval = 86_400

        for (i, entry) in symptoms.enumerated() {
            let s = NSEntityDescription.insertNewObject(forEntityName: "SymptomLogEntity", into: ctx)
            s.setValue(UUID(), forKey: "id")
            s.setValue(entry.0, forKey: "bodyLocation")
            s.setValue(Int16(entry.1), forKey: "painScale")
            s.setValue(Double.random(in: 0.5...4.0), forKey: "durationHours")
            s.setValue(entry.2, forKey: "notes")
            s.setValue(now.addingTimeInterval(-Double(i * 5) * day), forKey: "createdAt")
        }
    }
}
