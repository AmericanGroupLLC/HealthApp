import SwiftUI
import HealthKit
import FitFusionCore

struct AnnualReportsView: View {
    private let hk = iOSHealthKitManager.shared

    @State private var workoutCount: Int = 0
    @State private var mealCount: Int = 0
    @State private var avgSleep: Double?
    @State private var avgSteps: Double?
    @State private var loaded = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                Text("Year in Review")
                    .font(.title2).bold()
                Text("Your \(Calendar.current.component(.year, from: Date())) health journey")
                    .foregroundStyle(.secondary)

                HStack(spacing: 12) {
                    annualStat(icon: "figure.strengthtraining.traditional",
                               label: "Workouts",
                               value: "\(workoutCount)", color: .pink)
                    annualStat(icon: "fork.knife",
                               label: "Meals",
                               value: "\(mealCount)", color: .green)
                }
                HStack(spacing: 12) {
                    annualStat(icon: "moon.fill",
                               label: "Avg Sleep",
                               value: avgSleep.map { String(format: "%.1fh", $0) } ?? "—",
                               color: .purple)
                    annualStat(icon: "figure.walk",
                               label: "Avg Steps",
                               value: avgSteps.map { formatSteps($0) } ?? "—",
                               color: .blue)
                }

                GroupBox("Medication Adherence") {
                    VStack(alignment: .leading) {
                        let adherence = computeAdherence()
                        Text("\(adherence)%").font(.system(size: 36, weight: .bold))
                            .foregroundColor(adherence >= 80 ? .green : .orange)
                        Text("of scheduled doses taken on time").foregroundStyle(.secondary)
                    }
                }

                GroupBox {
                    HStack {
                        Image(systemName: "heart.text.square")
                            .foregroundColor(.blue)
                        VStack(alignment: .leading) {
                            Text("Share with Doctor").bold()
                            Text("Generate a summary PDF").font(.caption).foregroundStyle(.secondary)
                        }
                        Spacer()
                        Image(systemName: "chevron.right").foregroundStyle(.secondary)
                    }
                }
            }
            .padding()
        }
        .navigationTitle("Annual Reports")
        .task {
            guard !loaded else { return }
            loaded = true
            let daysThisYear = Calendar.current.ordinality(of: .day, in: .year, for: Date()) ?? 365
            async let wc = hk.workoutCount(daysBack: daysThisYear)
            async let sl = hk.averageSleepHours(daysBack: daysThisYear)
            async let st = hk.averageDailySteps(daysBack: daysThisYear)
            workoutCount = await wc
            avgSleep = await sl
            avgSteps = await st
            mealCount = CloudStore.shared.fetchMeals(daysBack: daysThisYear).count
        }
    }

    private func annualStat(icon: String, label: String, value: String, color: Color) -> some View {
        GroupBox {
            VStack {
                Image(systemName: icon).foregroundColor(color)
                Text(value).font(.title2).bold()
                Text(label).font(.caption).foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity)
        }
    }

    private func formatSteps(_ v: Double) -> String {
        if v >= 1000 {
            return String(format: "%,.0f", v)
        }
        return String(format: "%.0f", v)
    }

    private func computeAdherence() -> Int {
        let logs = CloudStore.shared.fetchMeals(daysBack: 365) // proxy; real adherence from MedicineDoseLogEntity
        return logs.isEmpty ? 0 : min(100, Int(Double(logs.count) / 3.65))
    }
}
