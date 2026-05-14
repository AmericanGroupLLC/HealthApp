import SwiftUI
import FitFusionCore

/// Recovery day screen showing readiness score, guided stretches, recovery tips,
/// and a quick link to the Sleep Recovery view. Uses `ReadinessEngine` for
/// score computation and `iOSHealthKitManager` for HRV / sleep data.
struct RecoveryDayView: View {
    @EnvironmentObject private var healthKit: iOSHealthKitManager

    @State private var score: ReadinessEngine.Score?
    @State private var showSleepRecovery = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: CarePlusSpacing.lg) {
                    scoreCard
                    tipsSection
                    stretchesSection
                    sleepLink
                }
                .padding(CarePlusSpacing.lg)
            }
            .background(CarePlusPalette.surface.ignoresSafeArea())
            .navigationTitle("Recovery Day")
            .navigationBarTitleDisplayMode(.inline)
            .task { await computeScore() }
            .sheet(isPresented: $showSleepRecovery) {
                SleepRecoveryView()
            }
        }
    }

    // MARK: - Score card

    private var scoreCard: some View {
        VStack(spacing: CarePlusSpacing.md) {
            ZStack {
                Circle()
                    .stroke(CarePlusPalette.divider, lineWidth: 10)
                Circle()
                    .trim(from: 0, to: CGFloat(score?.value ?? 0) / 100.0)
                    .stroke(scoreColor, style: StrokeStyle(lineWidth: 10, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                    .animation(.easeInOut(duration: 0.8), value: score?.value)
                VStack(spacing: CarePlusSpacing.xs) {
                    Text("\(score?.value ?? 0)")
                        .font(.system(size: 48, weight: .heavy, design: .rounded))
                        .foregroundStyle(scoreColor)
                    Text("Readiness")
                        .font(CarePlusType.captionEm)
                        .foregroundStyle(CarePlusPalette.onSurfaceMuted)
                }
            }
            .frame(width: 160, height: 160)
            .frame(maxWidth: .infinity)

            if let suggestion = score?.suggestion {
                Text(suggestion)
                    .font(CarePlusType.bodyEm)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
            }
        }
        .padding(CarePlusSpacing.lg)
        .background(CarePlusPalette.surfaceElevated, in: RoundedRectangle(cornerRadius: CarePlusRadius.lg))
    }

    private var scoreColor: Color {
        guard let s = score?.value else { return .gray }
        switch s {
        case 80...:  return CarePlusPalette.success
        case 60...:  return CarePlusPalette.trainGreen
        case 40...:  return CarePlusPalette.warning
        default:     return CarePlusPalette.danger
        }
    }

    // MARK: - Tips

    private var tipsSection: some View {
        VStack(alignment: .leading, spacing: CarePlusSpacing.sm) {
            Text("Recovery Tips").font(CarePlusType.titleSM)
            ForEach(recoveryTips, id: \.self) { tip in
                HStack(alignment: .top, spacing: CarePlusSpacing.sm) {
                    Image(systemName: "leaf.fill")
                        .foregroundStyle(CarePlusPalette.trainGreen)
                        .frame(width: 20)
                    Text(tip)
                        .font(CarePlusType.body)
                        .foregroundStyle(CarePlusPalette.onSurface)
                }
                .padding(.vertical, CarePlusSpacing.xs)
            }
        }
        .padding(CarePlusSpacing.md)
        .background(CarePlusPalette.surfaceElevated, in: RoundedRectangle(cornerRadius: CarePlusRadius.md))
    }

    private var recoveryTips: [String] {
        guard let s = score?.value else {
            return ["Load your health data to get personalized tips."]
        }
        if s < 40 {
            return [
                "Take a full rest day — your body needs it.",
                "Focus on hydration: aim for 2–3 liters of water.",
                "Prioritize 8+ hours of sleep tonight.",
                "Consider a warm bath or foam rolling session.",
            ]
        } else if s < 60 {
            return [
                "Keep activity light — a walk or gentle yoga.",
                "Eat nutrient-dense meals with adequate protein.",
                "Limit caffeine after 2 PM for better sleep.",
            ]
        } else {
            return [
                "You're recovering well — gentle movement is fine.",
                "Light stretching will help maintain mobility.",
                "Stay consistent with your sleep schedule.",
            ]
        }
    }

    // MARK: - Stretches

    private var stretchesSection: some View {
        VStack(alignment: .leading, spacing: CarePlusSpacing.sm) {
            Text("Guided Stretches").font(CarePlusType.titleSM)
            ForEach(stretches) { stretch in
                HStack(spacing: CarePlusSpacing.md) {
                    Image(systemName: stretch.icon)
                        .font(.title2)
                        .foregroundStyle(CarePlusPalette.careBlue)
                        .frame(width: 36)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(stretch.name).font(CarePlusType.bodyEm)
                        Text(stretch.duration)
                            .font(CarePlusType.caption)
                            .foregroundStyle(CarePlusPalette.onSurfaceMuted)
                    }
                    Spacer()
                    Image(systemName: "chevron.right")
                        .foregroundStyle(CarePlusPalette.onSurfaceMuted)
                }
                .padding(CarePlusSpacing.md)
                .background(CarePlusPalette.surface, in: RoundedRectangle(cornerRadius: CarePlusRadius.sm))
            }
        }
        .padding(CarePlusSpacing.md)
        .background(CarePlusPalette.surfaceElevated, in: RoundedRectangle(cornerRadius: CarePlusRadius.md))
    }

    // MARK: - Sleep link

    private var sleepLink: some View {
        Button { showSleepRecovery = true } label: {
            HStack(spacing: CarePlusSpacing.md) {
                Image(systemName: "moon.zzz.fill")
                    .font(.title2)
                    .foregroundStyle(.indigo)
                VStack(alignment: .leading, spacing: 2) {
                    Text("Sleep Recovery").font(CarePlusType.bodyEm)
                    Text("Review last night's sleep stages")
                        .font(CarePlusType.caption)
                        .foregroundStyle(CarePlusPalette.onSurfaceMuted)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .foregroundStyle(CarePlusPalette.onSurfaceMuted)
            }
            .padding(CarePlusSpacing.md)
            .background(CarePlusPalette.surfaceElevated, in: RoundedRectangle(cornerRadius: CarePlusRadius.md))
        }
        .buttonStyle(.plain)
    }

    // MARK: - Compute

    private func computeScore() async {
        let hrv = await healthKit.averageHRV(daysBack: 7)
        let sleep = try? await healthKit.fetchLastNightSleep()
        score = ReadinessEngine.shared.compute(
            hrvAvg: hrv,
            sleepHrs: sleep?.totalHours,
            workoutMinutes: nil
        )
    }
}

// MARK: - Stretch model

extension RecoveryDayView {
    struct Stretch: Identifiable {
        let id = UUID()
        let name: String
        let duration: String
        let icon: String
    }

    private var stretches: [Stretch] {
        [
            Stretch(name: "Neck & Shoulders", duration: "3 min", icon: "figure.cooldown"),
            Stretch(name: "Hip Flexor Opener", duration: "4 min", icon: "figure.flexibility"),
            Stretch(name: "Hamstring Stretch", duration: "3 min", icon: "figure.walk"),
            Stretch(name: "Spinal Twist", duration: "3 min", icon: "figure.mind.and.body"),
            Stretch(name: "Quad Stretch", duration: "2 min", icon: "figure.strengthtraining.traditional"),
            Stretch(name: "Full Body Cool-Down", duration: "5 min", icon: "figure.yoga"),
        ]
    }
}
