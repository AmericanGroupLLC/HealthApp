import SwiftUI
import FitFusionCore

struct ProfileScreen: View {

    @StateObject private var hcStore = HealthConditionsStore.shared
    @EnvironmentObject var vitals: VitalsService
    @Environment(\.dismiss) private var dismiss

    @AppStorage("profile.name") private var name: String = ""
    @AppStorage("profile.heightCm") private var heightCm: Double = 170
    @AppStorage("profile.weightKg") private var weightKg: Double = 65
    @AppStorage("profile.unitsImperial") private var unitsImperial: Bool = false
    @AppStorage("profile.birthday") private var birthdayRaw: Double = 0
    @AppStorage("profile.birthLocation") private var birthLocation: String = ""
    @AppStorage("bioAge.sex") private var sexRaw: String = BiologicalAgeEngine.Sex.male.rawValue

    @State private var showBioAge = false

    private var birthday: Date? {
        birthdayRaw > 0 ? Date(timeIntervalSince1970: birthdayRaw) : nil
    }

    private var age: Int? {
        guard let bday = birthday else { return nil }
        return Calendar.current.dateComponents([.year], from: bday, to: Date()).year
    }

    private var zodiacSign: (name: String, symbol: String)? {
        guard let bday = birthday else { return nil }
        let comps = Calendar.current.dateComponents([.month, .day], from: bday)
        guard let month = comps.month, let day = comps.day else { return nil }
        return Self.zodiac(month: month, day: day)
    }

    private var bmi: Double {
        let h = heightCm / 100.0
        return h > 0 ? weightKg / (h * h) : 0
    }

    private var completionPercent: Int {
        var done = 0
        if !name.isEmpty { done += 1 }
        if heightCm > 0 { done += 1 }
        if weightKg > 0 { done += 1 }
        if birthday != nil { done += 1 }
        if !birthLocation.isEmpty { done += 1 }
        if hcStore.hasAnyCondition || hcStore.conditions == [.none] { done += 1 }
        return Int(Double(done) / 6.0 * 100.0)
    }

    var body: some View {
        Form {
            Section {
                completionCard
                    .listRowInsets(EdgeInsets())
                    .listRowBackground(Color.clear)
            }

            Section("Personal") {
                TextField("Name", text: $name)

                DatePicker(
                    "Birthday",
                    selection: Binding(
                        get: { birthday ?? Date() },
                        set: { birthdayRaw = $0.timeIntervalSince1970 }
                    ),
                    in: ...Date(),
                    displayedComponents: .date
                )

                if let z = zodiacSign, let a = age {
                    HStack {
                        Text("\(z.symbol) \(z.name)")
                            .foregroundStyle(CarePlusPalette.careBlue)
                        Spacer()
                        Text("Age: \(a)")
                            .foregroundStyle(.secondary)
                    }
                }

                TextField("Birthplace", text: $birthLocation)

                Picker("Sex", selection: $sexRaw) {
                    Text("Male").tag(BiologicalAgeEngine.Sex.male.rawValue)
                    Text("Female").tag(BiologicalAgeEngine.Sex.female.rawValue)
                    Text("Other").tag(BiologicalAgeEngine.Sex.other.rawValue)
                }

                Stepper(value: $heightCm, in: 120...220, step: 1) {
                    Text("Height: \(Int(heightCm)) cm")
                }
                Stepper(value: $weightKg, in: 30...200, step: 0.5) {
                    Text(String(format: "Weight: %.1f kg", weightKg))
                }
                Toggle("Imperial units (ft / lb)", isOn: $unitsImperial)
            }

            Section("Body composition") {
                HStack {
                    Text("BMI")
                    Spacer()
                    Text(String(format: "%.1f", bmi)).foregroundStyle(.secondary)
                    Text(bmiLabel(bmi))
                        .font(.caption.weight(.semibold))
                        .padding(.horizontal, 8).padding(.vertical, 2)
                        .background(bmiColor(bmi).opacity(0.18), in: Capsule())
                        .foregroundStyle(bmiColor(bmi))
                }
            }

            // ── Biological age summary card ─────────────────────
            if let a = age {
                Section("Biological Age") {
                    let bioResult = computeBioAge(chronological: Double(a))
                    Button {
                        showBioAge = true
                    } label: {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                HStack(spacing: 16) {
                                    VStack {
                                        Text("Chrono").font(.caption2).foregroundStyle(.secondary)
                                        Text("\(a)")
                                            .font(.system(size: 32, weight: .heavy, design: .rounded))
                                            .foregroundStyle(.secondary)
                                    }
                                    VStack {
                                        Text("Bio").font(.caption2).foregroundStyle(.secondary)
                                        Text("\(Int(bioResult.biologicalYears))")
                                            .font(.system(size: 32, weight: .heavy, design: .rounded))
                                            .foregroundStyle(bioResult.deltaYears < 0 ? .green : .orange)
                                    }
                                }
                                Text(bioResult.verdict).font(.caption.bold())
                                Text(String(format: "Confidence: %.0f%%", bioResult.confidence * 100))
                                    .font(.caption2).foregroundStyle(.secondary)
                                Text("This data stays on your device")
                                    .font(.caption2).foregroundStyle(.secondary)
                            }
                            Spacer()
                            Image(systemName: "chevron.right").foregroundStyle(.tertiary)
                        }
                    }
                    .buttonStyle(.plain)
                }
            }

            Section("Connected sources") {
                connectedRow(symbol: "heart.text.square.fill",
                             label: "Apple Health",
                             status: .connected)
                connectedRow(symbol: "cross.case.fill",
                             label: "Epic MyChart",
                             status: KeychainStore.shared.fhirAccessToken(
                                issuer: EpicSandboxConfig.issuer
                             ) != nil ? .connected : .notConnected)
                connectedRow(symbol: "creditcard.fill",
                             label: "Insurance card",
                             status: PHIStore.shared.latestInsuranceCard() != nil
                                ? .connected : .notConnected)
                connectedRow(symbol: "pills.fill",
                             label: "Pharmacy",
                             status: .add)
            }

            Section("Health profile") {
                NavigationLink("Conditions & medical history") { HealthProfileView() }
            }

            Section("Account") {
                NavigationLink("Settings") { SettingsView() }
            }

            Section {
                Button(role: .destructive) {
                } label: {
                    Label("Sign out", systemImage: "rectangle.portrait.and.arrow.right")
                }
            }
        }
        .navigationTitle("Profile")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button("Done") { dismiss() }
            }
        }
        .sheet(isPresented: $showBioAge) {
            BiologicalAgeView()
        }
    }

    private func computeBioAge(chronological: Double) -> BiologicalAgeEngine.Result {
        let sex = BiologicalAgeEngine.Sex(rawValue: sexRaw) ?? .male
        let s = vitals.snapshot
        let inputs = BiologicalAgeEngine.Inputs(
            chronologicalYears: chronological,
            sex: sex,
            restingHR: s.restingHR,
            hrv: s.hrv,
            vo2Max: s.vo2Max,
            avgSleepHours: s.lastNightSleepHrs,
            bmi: bmi > 0 ? bmi : nil,
            bodyFatPct: s.bodyFatPct,
            systolicBP: s.systolicBP,
            diastolicBP: s.diastolicBP,
            weeklyExerciseMin: s.exerciseMinToday.map { $0 * 7 },
            stepsPerDay: Double(s.todaySteps),
            smoker: false,
            heavyAlcohol: false
        )
        return BiologicalAgeEngine.shared.estimate(inputs)
    }

    private var completionCard: some View {
        VStack(alignment: .leading, spacing: CarePlusSpacing.sm) {
            HStack {
                Text("Profile completion").font(.subheadline)
                Spacer()
                Text("\(completionPercent)%").font(.headline)
                    .foregroundStyle(CarePlusPalette.careBlue)
            }
            ProgressView(value: Double(completionPercent), total: 100)
                .tint(CarePlusPalette.careBlue)
            Text("Complete your profile so MyChart imports merge cleanly.")
                .font(.caption2).foregroundStyle(.secondary)
        }
        .padding(CarePlusSpacing.md)
        .background(CarePlusPalette.surfaceElevated, in: RoundedRectangle(cornerRadius: CarePlusRadius.md))
        .padding(.horizontal, CarePlusSpacing.lg)
        .padding(.vertical, CarePlusSpacing.sm)
    }

    private func bmiLabel(_ b: Double) -> String {
        switch b {
        case ..<18.5:  return "Under"
        case ..<25:    return "Normal"
        case ..<30:    return "Over"
        default:       return "Obese"
        }
    }

    private func bmiColor(_ b: Double) -> Color {
        switch b {
        case ..<18.5:  return CarePlusPalette.info
        case ..<25:    return CarePlusPalette.success
        case ..<30:    return CarePlusPalette.warning
        default:       return CarePlusPalette.danger
        }
    }

    // MARK: - Connected sources row

    private enum SourceStatus { case connected, notConnected, add }

    private func connectedRow(symbol: String, label: String,
                              status: SourceStatus) -> some View {
        HStack {
            Image(systemName: symbol).foregroundStyle(CarePlusPalette.careBlue).frame(width: 24)
            Text(label)
            Spacer()
            switch status {
            case .connected:
                Image(systemName: "checkmark.circle.fill")
                    .foregroundStyle(CarePlusPalette.success)
            case .notConnected:
                Text("Connect").font(.caption.weight(.semibold))
                    .foregroundStyle(CarePlusPalette.careBlue)
            case .add:
                Text("Add").font(.caption.weight(.semibold))
                    .foregroundStyle(CarePlusPalette.careBlue)
            }
        }
    }

    // MARK: - Zodiac computation

    static func zodiac(month: Int, day: Int) -> (name: String, symbol: String) {
        let signs: [(String, String, Int, Int, Int, Int)] = [
            ("Aries", "♈", 3, 21, 4, 19),
            ("Taurus", "♉", 4, 20, 5, 20),
            ("Gemini", "♊", 5, 21, 6, 20),
            ("Cancer", "♋", 6, 21, 7, 22),
            ("Leo", "♌", 7, 23, 8, 22),
            ("Virgo", "♍", 8, 23, 9, 22),
            ("Libra", "♎", 9, 23, 10, 22),
            ("Scorpio", "♏", 10, 23, 11, 21),
            ("Sagittarius", "♐", 11, 22, 12, 21),
            ("Capricorn", "♑", 12, 22, 1, 19),
            ("Aquarius", "♒", 1, 20, 2, 18),
            ("Pisces", "♓", 2, 19, 3, 20),
        ]
        for s in signs {
            if s.0 == "Capricorn" {
                if (month == s.2 && day >= s.3) || (month == s.4 && day <= s.5) {
                    return (s.0, s.1)
                }
            } else if (month == s.2 && day >= s.3) || (month == s.4 && day <= s.5) {
                return (s.0, s.1)
            }
        }
        return ("Pisces", "♓")
    }
}
