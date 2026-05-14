import SwiftUI
import Charts
import HealthKit
import FitFusionCore

/// Water intake tracker with animated circular progress, quick-add buttons,
/// and a 7-day history chart. Reads and writes `HKQuantityType.dietaryWater`
/// via `iOSHealthKitManager`.
struct WaterTrackerView: View {
    @EnvironmentObject private var healthKit: iOSHealthKitManager

    @State private var todayML: Double = 0
    @State private var goalML: Double = 2500
    @State private var history: [DayEntry] = []
    @State private var isLoading = false

    private let store = HKHealthStore()

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: CarePlusSpacing.lg) {
                    progressRing
                    quickAddButtons
                    historyChart
                }
                .padding(CarePlusSpacing.lg)
            }
            .background(CarePlusPalette.surface.ignoresSafeArea())
            .navigationTitle("Water Tracker")
            .navigationBarTitleDisplayMode(.inline)
            .task { await reload() }
        }
    }

    // MARK: - Progress ring

    private var progressRing: some View {
        let progress = min(todayML / goalML, 1.0)

        return VStack(spacing: CarePlusSpacing.md) {
            ZStack {
                Circle()
                    .stroke(CarePlusPalette.divider, lineWidth: 14)
                Circle()
                    .trim(from: 0, to: progress)
                    .stroke(
                        LinearGradient(
                            colors: [.cyan, CarePlusPalette.careBlue],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        style: StrokeStyle(lineWidth: 14, lineCap: .round)
                    )
                    .rotationEffect(.degrees(-90))
                    .animation(.easeInOut(duration: 0.6), value: todayML)

                VStack(spacing: CarePlusSpacing.xs) {
                    Image(systemName: "drop.fill")
                        .font(.title)
                        .foregroundStyle(.cyan)
                    Text("\(Int(todayML)) ml")
                        .font(.system(size: 32, weight: .heavy, design: .rounded))
                        .foregroundStyle(CarePlusPalette.onSurface)
                    Text("of \(Int(goalML)) ml")
                        .font(CarePlusType.caption)
                        .foregroundStyle(CarePlusPalette.onSurfaceMuted)
                }
            }
            .frame(width: 200, height: 200)
            .frame(maxWidth: .infinity)
            .accessibilityElement(children: .combine)
            .accessibilityLabel("Water intake progress: \(Int(todayML)) of \(Int(goalML)) milliliters")
            .accessibilityValue("\(Int(min(todayML / goalML, 1.0) * 100)) percent")

            Text(statusMessage)
                .font(CarePlusType.bodyEm)
                .foregroundStyle(todayML >= goalML ? CarePlusPalette.success : CarePlusPalette.onSurface)
                .frame(maxWidth: .infinity)
        }
        .padding(CarePlusSpacing.lg)
        .background(CarePlusPalette.surfaceElevated, in: RoundedRectangle(cornerRadius: CarePlusRadius.lg))
    }

    private var statusMessage: String {
        if todayML >= goalML { return "Goal reached! Great job staying hydrated." }
        let remaining = Int(goalML - todayML)
        return "\(remaining) ml to go"
    }

    // MARK: - Quick-add

    private var quickAddButtons: some View {
        VStack(alignment: .leading, spacing: CarePlusSpacing.sm) {
            Text("Quick Add").font(CarePlusType.titleSM)
            HStack(spacing: CarePlusSpacing.md) {
                addButton(label: "+250 ml", amount: 250)
                addButton(label: "+500 ml", amount: 500)
                addButton(label: "+1 L", amount: 1000)
            }
        }
    }

    private func addButton(label: String, amount: Double) -> some View {
        Button {
            Task { await addWater(ml: amount) }
        } label: {
            Text(label)
                .font(CarePlusType.bodyEm)
                .frame(maxWidth: .infinity)
                .padding(CarePlusSpacing.md)
                .background(CarePlusPalette.careBlue, in: RoundedRectangle(cornerRadius: CarePlusRadius.md))
                .foregroundStyle(.white)
        }
    }

    // MARK: - History chart

    private var historyChart: some View {
        VStack(alignment: .leading, spacing: CarePlusSpacing.sm) {
            Text("Last 7 Days").font(CarePlusType.titleSM)
            if history.isEmpty {
                Text("No history yet")
                    .font(CarePlusType.caption)
                    .foregroundStyle(CarePlusPalette.onSurfaceMuted)
                    .frame(maxWidth: .infinity, minHeight: 120)
            } else {
                Chart(history) { entry in
                    BarMark(
                        x: .value("Day", entry.date, unit: .day),
                        y: .value("ml", entry.ml)
                    )
                    .foregroundStyle(
                        entry.ml >= goalML
                            ? CarePlusPalette.success
                            : CarePlusPalette.careBlue
                    )
                    .cornerRadius(4)

                    RuleMark(y: .value("Goal", goalML))
                        .lineStyle(StrokeStyle(lineWidth: 1, dash: [4]))
                        .foregroundStyle(CarePlusPalette.onSurfaceMuted)
                }
                .chartXAxis {
                    AxisMarks(values: .stride(by: .day)) { _ in
                        AxisGridLine()
                        AxisValueLabel(format: .dateTime.weekday(.abbreviated))
                    }
                }
                .frame(height: 180)
            }
        }
        .padding(CarePlusSpacing.md)
        .background(CarePlusPalette.surfaceElevated, in: RoundedRectangle(cornerRadius: CarePlusRadius.md))
    }

    // MARK: - HealthKit

    private func addWater(ml: Double) async {
        let type = HKQuantityType(.dietaryWater)
        let quantity = HKQuantity(unit: .literUnit(with: .milli), doubleValue: ml)
        let sample = HKQuantitySample(type: type, quantity: quantity, start: Date(), end: Date())
        do {
            try await store.save(sample)
            await reload()
        } catch {
            // Handled by iOSHealthKitManager.lastError pattern
        }
    }

    private func reload() async {
        isLoading = true
        defer { isLoading = false }

        let type = HKQuantityType(.dietaryWater)
        let cal = Calendar.current
        let now = Date()
        guard let weekAgo = cal.date(byAdding: .day, value: -7, to: now) else { return }

        // Fetch 7-day stats
        let predicate = HKQuery.predicateForSamples(withStart: weekAgo, end: now)
        let interval = DateComponents(day: 1)

        let results: [DayEntry] = await withCheckedContinuation { cont in
            let query = HKStatisticsCollectionQuery(
                quantityType: type,
                quantitySamplePredicate: predicate,
                options: .cumulativeSum,
                anchorDate: cal.startOfDay(for: weekAgo),
                intervalComponents: interval
            )
            query.initialResultsHandler = { _, collection, _ in
                var entries: [DayEntry] = []
                collection?.enumerateStatistics(from: weekAgo, to: now) { stats, _ in
                    let ml = stats.sumQuantity()?.doubleValue(for: .literUnit(with: .milli)) ?? 0
                    entries.append(DayEntry(date: stats.startDate, ml: ml))
                }
                cont.resume(returning: entries)
            }
            store.execute(query)
        }

        history = results
        todayML = results.last?.ml ?? 0
    }
}

// MARK: - Model

extension WaterTrackerView {
    struct DayEntry: Identifiable {
        let id = UUID()
        let date: Date
        let ml: Double
    }
}
