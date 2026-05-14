import SwiftUI
import FitFusionCore

/// 7-day meal plan detail view with per-day tabs and macro targets for each meal.
/// Driven by a static plan model — a real implementation would fetch from an API
/// or Core Data.
struct MealPlanDetailView: View {
    let planName: String

    @State private var selectedDay: Int = 0

    private let weekdays = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: CarePlusSpacing.lg) {
                    dayPicker
                    macroSummary
                    mealsForDay
                }
                .padding(CarePlusSpacing.lg)
            }
            .background(CarePlusPalette.surface.ignoresSafeArea())
            .navigationTitle(planName)
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    // MARK: - Day picker

    private var dayPicker: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: CarePlusSpacing.sm) {
                ForEach(0..<7, id: \.self) { index in
                    Button {
                        withAnimation(.easeInOut(duration: 0.2)) { selectedDay = index }
                    } label: {
                        Text(weekdays[index])
                            .font(CarePlusType.captionEm)
                            .padding(.horizontal, CarePlusSpacing.md)
                            .padding(.vertical, CarePlusSpacing.sm)
                            .background(
                                selectedDay == index
                                    ? CarePlusPalette.dietCoral
                                    : CarePlusPalette.surfaceElevated,
                                in: RoundedRectangle(cornerRadius: CarePlusRadius.pill)
                            )
                            .foregroundStyle(
                                selectedDay == index ? .white : CarePlusPalette.onSurface
                            )
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    // MARK: - Macro summary

    private var macroSummary: some View {
        let day = mealPlan[selectedDay]
        let totalCal = day.meals.reduce(0) { $0 + $1.calories }
        let totalP = day.meals.reduce(0) { $0 + $1.protein }
        let totalC = day.meals.reduce(0) { $0 + $1.carbs }
        let totalF = day.meals.reduce(0) { $0 + $1.fat }

        return HStack(spacing: CarePlusSpacing.md) {
            macroColumn(label: "Calories", value: "\(totalCal)", unit: "kcal", color: .orange)
            macroColumn(label: "Protein", value: "\(totalP)", unit: "g", color: CarePlusPalette.workoutPink)
            macroColumn(label: "Carbs", value: "\(totalC)", unit: "g", color: CarePlusPalette.careBlue)
            macroColumn(label: "Fat", value: "\(totalF)", unit: "g", color: CarePlusPalette.warning)
        }
        .padding(CarePlusSpacing.md)
        .background(CarePlusPalette.surfaceElevated, in: RoundedRectangle(cornerRadius: CarePlusRadius.md))
    }

    private func macroColumn(label: String, value: String, unit: String, color: Color) -> some View {
        VStack(spacing: CarePlusSpacing.xs) {
            Text(value)
                .font(.system(.title3, design: .rounded, weight: .bold))
                .foregroundStyle(color)
            Text(unit)
                .font(CarePlusType.caption)
                .foregroundStyle(CarePlusPalette.onSurfaceMuted)
            Text(label)
                .font(CarePlusType.captionEm)
                .foregroundStyle(CarePlusPalette.onSurfaceMuted)
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Meals list

    private var mealsForDay: some View {
        let day = mealPlan[selectedDay]
        return VStack(alignment: .leading, spacing: CarePlusSpacing.md) {
            ForEach(day.meals) { meal in
                mealCard(meal)
            }
        }
    }

    private func mealCard(_ meal: PlannedMeal) -> some View {
        VStack(alignment: .leading, spacing: CarePlusSpacing.sm) {
            HStack {
                Image(systemName: meal.icon)
                    .font(.title3)
                    .foregroundStyle(CarePlusPalette.dietCoral)
                    .frame(width: 28)
                Text(meal.type).font(CarePlusType.titleSM)
                Spacer()
                Text("\(meal.calories) kcal")
                    .font(CarePlusType.captionEm)
                    .foregroundStyle(.orange)
            }

            Text(meal.name)
                .font(CarePlusType.bodyEm)

            if !meal.description.isEmpty {
                Text(meal.description)
                    .font(CarePlusType.caption)
                    .foregroundStyle(CarePlusPalette.onSurfaceMuted)
            }

            HStack(spacing: CarePlusSpacing.lg) {
                macroTag("P", value: meal.protein, color: CarePlusPalette.workoutPink)
                macroTag("C", value: meal.carbs, color: CarePlusPalette.careBlue)
                macroTag("F", value: meal.fat, color: CarePlusPalette.warning)
            }
        }
        .padding(CarePlusSpacing.md)
        .background(CarePlusPalette.surfaceElevated, in: RoundedRectangle(cornerRadius: CarePlusRadius.md))
    }

    private func macroTag(_ letter: String, value: Int, color: Color) -> some View {
        HStack(spacing: 4) {
            Text(letter)
                .font(CarePlusType.captionEm)
                .foregroundStyle(color)
            Text("\(value)g")
                .font(CarePlusType.caption)
                .foregroundStyle(CarePlusPalette.onSurfaceMuted)
        }
    }
}

// MARK: - Data model

extension MealPlanDetailView {
    struct PlannedMeal: Identifiable {
        let id = UUID()
        let type: String
        let icon: String
        let name: String
        let description: String
        let calories: Int
        let protein: Int
        let carbs: Int
        let fat: Int
    }

    struct PlanDay {
        let meals: [PlannedMeal]
    }

    private var mealPlan: [PlanDay] {
        (0..<7).map { day in
            PlanDay(meals: [
                PlannedMeal(
                    type: "Breakfast", icon: "sunrise.fill",
                    name: day % 2 == 0 ? "Oatmeal & Berries" : "Greek Yogurt Parfait",
                    description: day % 2 == 0 ? "Rolled oats, blueberries, honey, chia seeds" : "Greek yogurt, granola, mixed berries",
                    calories: day % 2 == 0 ? 350 : 310,
                    protein: day % 2 == 0 ? 12 : 22,
                    carbs: day % 2 == 0 ? 55 : 38,
                    fat: day % 2 == 0 ? 8 : 9
                ),
                PlannedMeal(
                    type: "Lunch", icon: "sun.max.fill",
                    name: day % 3 == 0 ? "Grilled Chicken Salad" : (day % 3 == 1 ? "Quinoa Bowl" : "Turkey Wrap"),
                    description: "Mixed greens, lean protein, whole grains",
                    calories: 520 + day * 10,
                    protein: 38 + day * 2,
                    carbs: 42 + day * 3,
                    fat: 16 + day
                ),
                PlannedMeal(
                    type: "Snack", icon: "leaf.fill",
                    name: "Protein Smoothie",
                    description: "Banana, whey protein, almond milk",
                    calories: 220,
                    protein: 25,
                    carbs: 22,
                    fat: 5
                ),
                PlannedMeal(
                    type: "Dinner", icon: "moon.stars.fill",
                    name: day % 2 == 0 ? "Salmon & Veggies" : "Lean Beef Stir-Fry",
                    description: day % 2 == 0 ? "Baked salmon, roasted broccoli, sweet potato" : "Sirloin strips, bell peppers, brown rice",
                    calories: day % 2 == 0 ? 580 : 620,
                    protein: day % 2 == 0 ? 42 : 45,
                    carbs: day % 2 == 0 ? 38 : 52,
                    fat: day % 2 == 0 ? 22 : 18
                ),
            ])
        }
    }
}
