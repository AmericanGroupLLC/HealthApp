import SwiftUI
import Charts
import HealthKit
import FitFusionCore

/// Multi-metric progress report with Swift Charts. Pulls weight, steps, sleep,
/// and resting heart rate from `iOSHealthKitManager` and displays trend lines
/// over a selectable 7 / 30 / 90-day window.
struct ProgressReportView: View {
    @EnvironmentObject private var healthKit: iOSHealthKitManager

    @State private var range: DateRange = .week
    @State private var weightData: [DataPoint] = []
    @State private var stepsData: [DataPoint] = []
    @State private var sleepData: [DataPoint] = []
    @State private var rhrData: [DataPoint] = []
    @State private var isLoading = false

    enum DateRange: String, CaseIterable, Identifiable {
        case week = "7 Days"
        case month = "30 Days"
        case quarter = "90 Days"
        var id: String { rawValue }
        var days: Int {
            switch self {
            case .week: return 7
            case .month: return 30
            case .quarter: return 90
            }
        }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: CarePlusSpacing.lg) {
                    rangePicker
                    if isLoading {
                        ProgressView()
                            .frame(maxWidth: .infinity, minHeight: 200)
                    } else {
                        chartCard(title: "Weight", data: weightData, unit: "kg", color: CarePlusPalette.careBlue)
                        chartCard(title: "Steps", data: stepsData, unit: "steps", color: CarePlusPalette.trainGreen)
                        chartCard(title: "Sleep", data: sleepData, unit: "hrs", color: .indigo)
                        chartCard(title: "Resting HR", data: rhrData, unit: "bpm", color: CarePlusPalette.workoutPink)
                    }
                }
                .padding(CarePlusSpacing.lg)
            }
            .background(CarePlusPalette.surface.ignoresSafeArea())
            .navigationTitle("Progress Report")
            .navigationBarTitleDisplayMode(.inline)
            .task { await loadData() }
            .onChange(of: range) { Task { await loadData() } }
        }
    }

    // MARK: - Range picker

    private var rangePicker: some View {
        Picker("Range", selection: $range) {
            ForEach(DateRange.allCases) { r in
                Text(r.rawValue).tag(r)
            }
        }
        .pickerStyle(.segmented)
    }

    // MARK: - Chart card

    private func chartCard(title: String, data: [DataPoint], unit: String, color: Color) -> some View {
        VStack(alignment: .leading, spacing: CarePlusSpacing.sm) {
            HStack {
                Text(title).font(CarePlusType.titleSM)
                Spacer()
                if let last = data.last {
                    Text(String(format: "%.1f %@", last.value, unit))
                        .font(CarePlusType.captionEm)
                        .foregroundStyle(color)
                }
            }
            if data.isEmpty {
                Text("No data for this period")
                    .font(CarePlusType.caption)
                    .foregroundStyle(CarePlusPalette.onSurfaceMuted)
                    .frame(maxWidth: .infinity, minHeight: 120)
            } else {
                Chart(data) { point in
                    LineMark(
                        x: .value("Date", point.date),
                        y: .value(title, point.value)
                    )
                    .foregroundStyle(color)
                    .interpolationMethod(.catmullRom)

                    AreaMark(
                        x: .value("Date", point.date),
                        y: .value(title, point.value)
                    )
                    .foregroundStyle(color.opacity(0.1))
                    .interpolationMethod(.catmullRom)
                }
                .chartXAxis {
                    AxisMarks(values: .stride(by: range == .week ? .day : .weekOfYear)) { _ in
                        AxisGridLine()
                        AxisValueLabel(format: .dateTime.month(.abbreviated).day())
                    }
                }
                .frame(height: 180)
            }
        }
        .padding(CarePlusSpacing.md)
        .background(CarePlusPalette.surfaceElevated, in: RoundedRectangle(cornerRadius: CarePlusRadius.md))
    }

    // MARK: - Data loading

    private func loadData() async {
        isLoading = true
        defer { isLoading = false }

        let store = HKHealthStore()
        let cal = Calendar.current
        let end = Date()
        guard let start = cal.date(byAdding: .day, value: -range.days, to: end) else { return }

        async let w = fetchDailyStats(store: store, type: .init(.bodyMass), unit: .gramUnit(with: .kilo), start: start, end: end)
        async let s = fetchDailyStats(store: store, type: .init(.stepCount), unit: .count(), start: start, end: end, options: .cumulativeSum)
        async let sl = fetchDailyStats(store: store, type: .init(.appleExerciseTime), unit: .minute(), start: start, end: end, options: .cumulativeSum)
        async let r = fetchDailyStats(store: store, type: .init(.restingHeartRate), unit: HKUnit.count().unitDivided(by: .minute()), start: start, end: end)

        weightData = await w
        stepsData = await s
        sleepData = await sl
        rhrData = await r
    }

    private func fetchDailyStats(
        store: HKHealthStore,
        type: HKQuantityType,
        unit: HKUnit,
        start: Date,
        end: Date,
        options: HKStatisticsOptions = .discreteAverage
    ) async -> [DataPoint] {
        let predicate = HKQuery.predicateForSamples(withStart: start, end: end)
        let interval = DateComponents(day: 1)

        return await withCheckedContinuation { cont in
            let query = HKStatisticsCollectionQuery(
                quantityType: type,
                quantitySamplePredicate: predicate,
                options: options,
                anchorDate: Calendar.current.startOfDay(for: start),
                intervalComponents: interval
            )
            query.initialResultsHandler = { _, collection, _ in
                var points: [DataPoint] = []
                collection?.enumerateStatistics(from: start, to: end) { stats, _ in
                    let qty = options == .cumulativeSum
                        ? stats.sumQuantity()
                        : stats.averageQuantity()
                    if let v = qty?.doubleValue(for: unit) {
                        points.append(DataPoint(date: stats.startDate, value: v))
                    }
                }
                cont.resume(returning: points)
            }
            store.execute(query)
        }
    }
}

// MARK: - Model

extension ProgressReportView {
    struct DataPoint: Identifiable {
        let id = UUID()
        let date: Date
        let value: Double
    }
}
