import SwiftUI
import HealthKit
import FitFusionCore

struct WellnessInsightsView: View {
    private let hk = iOSHealthKitManager.shared

    @State private var readiness: ReadinessEngine.Score?
    @State private var hrvAvg: Double?
    @State private var sleepHrs: Double?
    @State private var workoutMin: Double = 0
    @State private var rhr: Double?
    @State private var loaded = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                if let score = readiness {
                    GroupBox {
                        HStack {
                            Image(systemName: scoreIcon(score.value))
                                .foregroundColor(scoreColor(score.value))
                            VStack(alignment: .leading) {
                                Text("Readiness: \(score.value)/100").bold()
                                Text(score.suggestion).foregroundStyle(.secondary)
                            }
                        }
                    }
                }

                ForEach(dynamicInsights, id: \.0) { title, desc, color, icon in
                    GroupBox {
                        HStack(alignment: .top) {
                            Image(systemName: icon)
                                .foregroundColor(color)
                                .frame(width: 24)
                            VStack(alignment: .leading, spacing: 4) {
                                Text(title).bold()
                                Text(desc)
                                    .font(.subheadline)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                }
            }
            .padding()
        }
        .navigationTitle("Wellness Insights")
        .task {
            guard !loaded else { return }
            loaded = true
            async let h = hk.averageHRV(daysBack: 14)
            async let s = hk.averageSleepHours(daysBack: 7)
            async let w = hk.totalWorkoutMinutes(daysBack: 7)
            async let r = hk.averageRestingHR(daysBack: 7)
            hrvAvg = await h
            sleepHrs = await s
            workoutMin = await w
            rhr = await r
            readiness = await ReadinessEngine.shared.compute(
                hrvAvg: hrvAvg, sleepHrs: sleepHrs, workoutMinutes: workoutMin
            )
        }
    }

    private var dynamicInsights: [(String, String, Color, String)] {
        var items: [(String, String, Color, String)] = []

        if let s = sleepHrs {
            let status = s >= 7 ? "on track" : "below your 8h goal"
            items.append(("Sleep Quality",
                          String(format: "Averaging %.1fh this week — %@.", s, status),
                          .purple, "moon.fill"))
        }

        if let h = hrvAvg {
            let trend = h > 50 ? "strong recovery signals" : "recovery could improve"
            items.append(("HRV Trend",
                          String(format: "7-day avg %.0f ms — %@.", h, trend),
                          .green, "waveform.path.ecg"))
        }

        let workoutCount = Int(workoutMin / 30)
        let loadAdvice = workoutMin > 120 ? "Consider a recovery day." : "Room to add a session."
        items.append(("Workout Load",
                      "\(workoutCount) sessions (~\(Int(workoutMin)) min) this week. \(loadAdvice)",
                      .orange, "figure.strengthtraining.traditional"))

        if let r = rhr {
            let rhrNote = r < 60 ? "Excellent cardiovascular fitness." : "Normal range."
            items.append(("Resting Heart Rate",
                          String(format: "%.0f bpm average. %@", r, rhrNote),
                          .red, "heart.fill"))
        }

        return items
    }

    private func scoreIcon(_ v: Int) -> String {
        switch v {
        case 80...: return "checkmark.circle.fill"
        case 60...: return "chart.line.uptrend.xyaxis"
        case 40...: return "exclamationmark.triangle"
        default:    return "bed.double.fill"
        }
    }

    private func scoreColor(_ v: Int) -> Color {
        switch v {
        case 80...: return .green
        case 60...: return .blue
        case 40...: return .orange
        default:    return .red
        }
    }
}
