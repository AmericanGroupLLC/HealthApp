import SwiftUI
import FitFusionCore

/// Train tab home — adds a "Moderate workout for you" recommendation card
/// above the existing TrainView programs list. Per design-spec guidance,
/// the recommendation is derived from HealthKit-only signals (sleep + HRV
/// + yesterday's training load) — no clinical data.
struct TrainHomeView: View {
    @State private var showHeaderProfile = false
    @State private var showHeaderBell = false

    private let tint = CarePlusPalette.trainGreen

    // Recommendation — wired to ReadinessEngine.
    @State private var recommendation = TrainRecommendation(
        intensity: "—",
        title: "Loading…",
        rationale: "Checking your readiness",
        tags: []
    )
    @State private var readinessLoaded = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                AppHeader(tab: .train,
                          onProfile: { showHeaderProfile = true },
                          onBell: { showHeaderBell = true })

                ScrollView {
                    VStack(alignment: .leading, spacing: CarePlusSpacing.lg) {
                        recommendationCard

                        // ── Recommended for you (condition-based) ────
                        let conditions = HealthConditionsStore.shared.conditions
                        let recommended = ExerciseLibrary.recommended(for: conditions).prefix(6)
                        if !recommended.isEmpty {
                            Text("Recommended for you").font(CarePlusType.titleSM)
                            if conditions != [.none] && !conditions.isEmpty {
                                Text("Based on your declared conditions · safe exercises only")
                                    .font(.caption2).foregroundStyle(.secondary)
                            }
                            ForEach(Array(recommended), id: \.id) { exercise in
                                let benefits = ExerciseMedia.beneficialFor(exercise.id, conditions: conditions)
                                HStack(spacing: CarePlusSpacing.md) {
                                    AsyncImage(url: ExerciseMedia.thumbnailURL(for: exercise.id)) { phase in
                                        switch phase {
                                        case .success(let image):
                                            image.resizable().aspectRatio(contentMode: .fill)
                                        default:
                                            Image(systemName: "figure.strengthtraining.traditional")
                                                .font(.title2).foregroundStyle(tint)
                                        }
                                    }
                                    .frame(width: 56, height: 56)
                                    .clipShape(RoundedRectangle(cornerRadius: 8))
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(exercise.name).font(.headline)
                                        Text(exercise.primaryMuscles.map(\.label).joined(separator: ", "))
                                            .font(.caption).foregroundStyle(.secondary)
                                        if !benefits.isEmpty {
                                            Text("✓ Beneficial for: \(benefits.map(\.label).joined(separator: ", "))")
                                                .font(.caption2.weight(.semibold))
                                                .foregroundStyle(.green)
                                        }
                                    }
                                    Spacer()
                                }
                                .padding(CarePlusSpacing.md)
                                .background(
                                    benefits.isEmpty
                                        ? CarePlusPalette.surfaceElevated
                                        : Color.green.opacity(0.08),
                                    in: RoundedRectangle(cornerRadius: CarePlusRadius.md)
                                )
                            }
                        }

                        Text("Today's plan").font(CarePlusType.titleSM)
                        planRow(symbol: "figure.cooldown", title: "Warm-up",
                                subtitle: "5 min · 4 moves")
                        planRow(symbol: "dumbbell.fill", title: "Strength block",
                                subtitle: "20 min · 6 moves")
                        planRow(symbol: "wind", title: "Cooldown",
                                subtitle: "10 min · breathwork")

                        // Sedentary alert tile (links to standup timer).
                        NavigationLink {
                            StandupTimerView()
                        } label: {
                            HStack {
                                Image(systemName: "figure.stand").foregroundStyle(.orange)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("Sedentary 52 min").font(.headline)
                                    Text("Time to stand up").font(.caption)
                                        .foregroundStyle(.secondary)
                                }
                                Spacer()
                                Image(systemName: "chevron.right").foregroundStyle(.tertiary)
                            }
                            .padding(CarePlusSpacing.md)
                            .background(.orange.opacity(0.10),
                                        in: RoundedRectangle(cornerRadius: CarePlusRadius.md))
                        }.buttonStyle(.plain)

                        NavigationLink {
                            TrainView()
                        } label: {
                            HStack {
                                Text("Open program library").font(.headline)
                                Spacer()
                                Image(systemName: "chevron.right").foregroundStyle(.tertiary)
                            }
                            .padding(CarePlusSpacing.md)
                            .background(CarePlusPalette.surfaceElevated,
                                        in: RoundedRectangle(cornerRadius: CarePlusRadius.md))
                        }.buttonStyle(.plain)

                        NavigationLink {
                            ProgressReportView()
                        } label: {
                            HStack(spacing: CarePlusSpacing.md) {
                                Image(systemName: "chart.bar.fill")
                                    .font(.title3)
                                    .frame(width: 36, height: 36)
                                    .background(tint.opacity(0.12), in: RoundedRectangle(cornerRadius: 9))
                                    .foregroundStyle(tint)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("Progress report").font(.headline)
                                    Text("Weekly volume & strength trends.").font(.caption).foregroundStyle(.secondary)
                                }
                                Spacer()
                                Image(systemName: "chevron.right").foregroundStyle(.tertiary)
                            }
                            .padding(CarePlusSpacing.md)
                            .background(CarePlusPalette.surfaceElevated,
                                        in: RoundedRectangle(cornerRadius: CarePlusRadius.md))
                        }.buttonStyle(.plain)

                        NavigationLink {
                            RecoveryDayView()
                        } label: {
                            HStack(spacing: CarePlusSpacing.md) {
                                Image(systemName: "leaf.fill")
                                    .font(.title3)
                                    .frame(width: 36, height: 36)
                                    .background(tint.opacity(0.12), in: RoundedRectangle(cornerRadius: 9))
                                    .foregroundStyle(tint)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("Recovery day").font(.headline)
                                    Text("Stretching, foam rolling & breathwork.").font(.caption).foregroundStyle(.secondary)
                                }
                                Spacer()
                                Image(systemName: "chevron.right").foregroundStyle(.tertiary)
                            }
                            .padding(CarePlusSpacing.md)
                            .background(CarePlusPalette.surfaceElevated,
                                        in: RoundedRectangle(cornerRadius: CarePlusRadius.md))
                        }.buttonStyle(.plain)

                        // ── Suggested fitness vendors ────────────────
                        Text("Suggested fitness vendors").font(CarePlusType.titleSM)
                            .padding(.top, CarePlusSpacing.sm)
                        vendorTile(symbol: "dumbbell.fill", name: "Rogue Fitness",
                                   tagline: "Barbells, racks & gym gear")
                        vendorTile(symbol: "figure.indoor.cycle", name: "Peloton",
                                   tagline: "Connected fitness classes")
                        vendorTile(symbol: "hand.raised.fill", name: "Therabody",
                                   tagline: "Theragun & recovery tools")
                    }
                    .padding(CarePlusSpacing.lg)
                }
            }
            .background(CarePlusPalette.surface.ignoresSafeArea())
            .navigationBarHidden(true)
            .sheet(isPresented: $showHeaderProfile) { NavigationStack { ProfileScreen() } }
            .sheet(isPresented: $showHeaderBell) {
                NewsDrawerSheet().presentationDetents([.medium, .large])
            }
        }
        .tint(tint)
        .task {
            guard !readinessLoaded else { return }
            readinessLoaded = true
            let hk = iOSHealthKitManager.shared
            let hrvAvg = await hk.averageHRV(daysBack: 7)
            let sleep = await hk.averageSleepHours(daysBack: 3)
            let workoutMin = await hk.totalWorkoutMinutes(daysBack: 2)
            let score = await ReadinessEngine.shared.compute(
                hrvAvg: hrvAvg, sleepHrs: sleep, workoutMinutes: workoutMin
            )
            let intensity: String
            let title: String
            var tags: [String] = []
            switch score.value {
            case 80...:
                intensity = "High"
                title = "45 min strength session"
                tags = ["strength", "push", "energy"]
            case 60...:
                intensity = "Moderate"
                title = "35 min mixed cardio + strength"
                tags = ["cardio", "strength", "balanced"]
            case 40...:
                intensity = "Light"
                title = "25 min easy cardio or yoga"
                tags = ["yoga", "walk", "gentle"]
            default:
                intensity = "Recovery"
                title = "20 min stretch & breathwork"
                tags = ["stretch", "breathwork", "rest"]
            }
            let sleepStr = sleep.map { String(format: "Sleep %.1fh", $0) } ?? "Sleep —"
            let hrvStr = hrvAvg.map { String(format: "HRV %.0f ms", $0) } ?? "HRV —"
            recommendation = TrainRecommendation(
                intensity: intensity,
                title: title,
                rationale: "\(sleepStr) · \(hrvStr)",
                tags: tags
            )
        }
    }

    private var recommendationCard: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("\(recommendation.intensity) workout for you")
                .font(.subheadline.weight(.semibold)).foregroundStyle(tint)
            Text(recommendation.title).font(CarePlusType.title)
            Text(recommendation.rationale).font(.caption).foregroundStyle(.secondary)
            HStack(spacing: 6) {
                ForEach(recommendation.tags, id: \.self) { tag in
                    Text(tag).font(.caption2.weight(.semibold))
                        .padding(.horizontal, 8).padding(.vertical, 3)
                        .background(.regularMaterial, in: Capsule())
                }
            }
        }
        .padding(CarePlusSpacing.md)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(tint.opacity(0.10),
                    in: RoundedRectangle(cornerRadius: CarePlusRadius.md))
    }

    private func planRow(symbol: String, title: String, subtitle: String) -> some View {
        HStack(spacing: CarePlusSpacing.md) {
            Image(systemName: symbol)
                .font(.title3)
                .frame(width: 36, height: 36)
                .background(tint.opacity(0.12), in: RoundedRectangle(cornerRadius: 9))
                .foregroundStyle(tint)
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.headline)
                Text(subtitle).font(.caption).foregroundStyle(.secondary)
            }
            Spacer()
        }
        .padding(CarePlusSpacing.md)
        .background(CarePlusPalette.surfaceElevated,
                    in: RoundedRectangle(cornerRadius: CarePlusRadius.md))
    }

    private func vendorTile(symbol: String, name: String, tagline: String) -> some View {
        HStack(spacing: CarePlusSpacing.md) {
            Image(systemName: symbol)
                .font(.title3)
                .frame(width: 36, height: 36)
                .background(tint.opacity(0.12), in: RoundedRectangle(cornerRadius: 9))
                .foregroundStyle(tint)
            VStack(alignment: .leading, spacing: 2) {
                Text(name).font(.headline)
                Text(tagline).font(.caption).foregroundStyle(.secondary)
            }
            Spacer()
            Text("Visit").font(.caption.weight(.semibold))
                .padding(.horizontal, 10).padding(.vertical, 4)
                .background(tint.opacity(0.15), in: Capsule())
                .foregroundStyle(tint)
        }
        .padding(CarePlusSpacing.md)
        .background(CarePlusPalette.surfaceElevated,
                    in: RoundedRectangle(cornerRadius: CarePlusRadius.md))
    }
}

private struct TrainRecommendation {
    let intensity: String
    let title: String
    let rationale: String
    let tags: [String]
}
