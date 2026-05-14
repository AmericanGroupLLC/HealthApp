import SwiftUI
import HealthKit
import FitFusionCore

/// Care tab home — top of the four-tab Care+ shell. Tile grid:
///  • Connect MyChart — CTA when not yet linked
///  • Insurance card  — CTA when not yet uploaded
///  • Care plan       — per-condition cards from HealthConditionsStore
///  • Doctors         — favorites + finder entry
///  • Annual reports  — coming soon
///  • Symptoms log    — coming soon
struct CareHomeView: View {
    @StateObject private var conditions = HealthConditionsStore.shared
    @State private var showHeaderProfile = false
    @State private var showHeaderBell = false
    @State private var myChartConnected = false  // wired by MyChartConnectView
    @State private var insuranceUploaded = false // wired by InsuranceCardSheet

    @State private var readings: [HealthCondition: (String, Bool)] = [:]
    private let hk = iOSHealthKitManager.shared

    private let tint = CarePlusPalette.careBlue

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                AppHeader(tab: .care,
                          onProfile: { showHeaderProfile = true },
                          onBell: { showHeaderBell = true })

                ScrollView {
                    VStack(alignment: .leading, spacing: CarePlusSpacing.lg) {

                        // ─── Quick connect tiles ───────────────────────
                        VStack(spacing: CarePlusSpacing.sm) {
                            NavigationLink {
                                MyChartConnectView(onConnected: { myChartConnected = true })
                            } label: {
                                ctaTile(symbol: "cross.case.fill",
                                        title: myChartConnected ? "MyChart connected"
                                                                  : "Connect MyChart",
                                        subtitle: myChartConnected
                                            ? "Tap to view your records."
                                            : "Read-only access via SMART-on-FHIR.",
                                        ctaText: myChartConnected ? nil : "Connect")
                            }
                            .buttonStyle(.plain)

                            NavigationLink {
                                InsuranceCardSheet(onSaved: { insuranceUploaded = true })
                            } label: {
                                ctaTile(symbol: "creditcard.fill",
                                        title: insuranceUploaded
                                            ? "Insurance card on file"
                                            : "Add insurance card",
                                        subtitle: insuranceUploaded
                                            ? "Tap to update."
                                            : "Snap a photo — OCR runs on-device.",
                                        ctaText: insuranceUploaded ? nil : "Add")
                            }
                            .buttonStyle(.plain)

                            // ── New: snap a printed lab report ───────
                            NavigationLink {
                                LabReportSheet()
                            } label: {
                                ctaTile(symbol: "doc.text.viewfinder",
                                        title: "Snap lab report",
                                        subtitle: "On-device OCR pulls A1C, BP, lipids.",
                                        ctaText: "Snap")
                            }
                            .buttonStyle(.plain)
                        }

                        // ─── Care plan (per-condition) ─────────────────
                        sectionHeader("Care plan")
                        if conditions.hasAnyCondition {
                            ForEach(Array(conditions.conditions).sorted(by: { $0.label < $1.label }),
                                    id: \.self) { c in
                                if c != .none {
                                    let r = readings[c]
                                    CarePlanCard(condition: c,
                                                 reading: r?.0,
                                                 readingHealthy: r?.1 ?? true)
                                }
                            }
                        } else {
                            emptyRow(
                                symbol: "stethoscope",
                                text: "Declare any conditions in Profile → Health profile to see a tailored care plan."
                            )
                        }

                        // ─── Quick links ───────────────────────────────
                        sectionHeader("Find help")
                        NavigationLink {
                            DoctorFinderView()
                        } label: {
                            tile(symbol: "stethoscope", title: "Doctors",
                                 subtitle: "Search by ZIP and specialty. Save favorites.")
                        }.buttonStyle(.plain)

                        // ── Suggested pharmacies ─────────────────────
                        sectionHeader("Suggested pharmacies")
                        vendorTile(symbol: "cross.vial.fill", name: "CVS Pharmacy",
                                   tagline: "Prescriptions, vaccines & wellness")
                        vendorTile(symbol: "cross.vial.fill", name: "Walgreens",
                                   tagline: "Pharmacy, health & wellness")
                        vendorTile(symbol: "cross.vial.fill", name: "Rite Aid",
                                   tagline: "Pharmacy & drugstore")

                        NavigationLink {
                            AnnualReportsView()
                        } label: {
                            tile(symbol: "doc.text.magnifyingglass", title: "Annual reports",
                                 subtitle: "Yearly summary you can share with your doctor.")
                        }.buttonStyle(.plain)

                        NavigationLink {
                            SymptomsLogView()
                        } label: {
                            tile(symbol: "thermometer.medium", title: "Symptoms log",
                                 subtitle: "Track how you feel day-to-day.")
                        }.buttonStyle(.plain)
                    }
                    .padding(CarePlusSpacing.lg)
                }
            }
            .background(CarePlusPalette.surface.ignoresSafeArea())
            .navigationBarHidden(true)
            .sheet(isPresented: $showHeaderProfile) {
                NavigationStack { ProfileScreen() }
            }
            .sheet(isPresented: $showHeaderBell) {
                NewsDrawerSheet().presentationDetents([.medium, .large])
            }
        }
        .tint(tint)
        .task { await loadVitals() }
    }

    private func loadVitals() async {
        var r: [HealthCondition: (String, Bool)] = [:]
        let sys = await hk.latestQuantity(type: HKQuantityType(.bloodPressureSystolic),
                                          unit: .millimeterOfMercury())
        let dia = await hk.latestQuantity(type: HKQuantityType(.bloodPressureDiastolic),
                                          unit: .millimeterOfMercury())
        if let s = sys, let d = dia {
            let healthy = s < 130 && d < 80
            r[.hypertension] = ("\(Int(s))/\(Int(d))", healthy)
            r[.lowBloodPressure] = ("\(Int(s))/\(Int(d))", s >= 90)
        }

        let glucose = await hk.latestQuantity(type: HKQuantityType(.bloodGlucose),
                                              unit: HKUnit.gramUnit(with: .milli).unitDivided(by: .literUnit(with: .deci)))
        if let g = glucose {
            let healthy = g < 140
            r[.diabetesT1] = ("\(Int(g)) mg/dL", healthy)
            r[.diabetesT2] = ("\(Int(g)) mg/dL", healthy)
        }

        let rhr = await hk.averageRestingHR(daysBack: 7)
        if let hr = rhr {
            r[.heartCondition] = ("\(Int(hr)) bpm", hr < 100)
        }

        let weight = await hk.latestQuantity(type: HKQuantityType(.bodyMass),
                                             unit: .gramUnit(with: .kilo))
        let height = await hk.latestQuantity(type: HKQuantityType(.height),
                                             unit: .meter())
        if let w = weight, let h = height, h > 0 {
            let bmi = w / (h * h)
            r[.obesity] = (String(format: "BMI %.1f", bmi), bmi < 30)
        }

        readings = r
    }

    // MARK: - Small components

    private func sectionHeader(_ s: String) -> some View {
        Text(s).font(CarePlusType.titleSM)
            .padding(.top, CarePlusSpacing.sm)
    }

    private func ctaTile(symbol: String, title: String, subtitle: String,
                         ctaText: String?) -> some View {
        HStack(spacing: CarePlusSpacing.md) {
            Image(systemName: symbol)
                .font(.title2.weight(.semibold))
                .frame(width: 40, height: 40)
                .background(tint.opacity(0.15), in: RoundedRectangle(cornerRadius: 10))
                .foregroundStyle(tint)
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.headline)
                Text(subtitle).font(.caption).foregroundStyle(.secondary)
            }
            Spacer()
            if let cta = ctaText {
                Text(cta).font(.caption.weight(.semibold))
                    .padding(.horizontal, 12).padding(.vertical, 6)
                    .background(tint, in: Capsule())
                    .foregroundStyle(.white)
            } else {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundStyle(CarePlusPalette.success)
            }
        }
        .padding(CarePlusSpacing.md)
        .background(CarePlusPalette.surfaceElevated, in: RoundedRectangle(cornerRadius: CarePlusRadius.md))
    }

    private func tile(symbol: String, title: String, subtitle: String) -> some View {
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
            Image(systemName: "chevron.right").foregroundStyle(.tertiary)
        }
        .padding(CarePlusSpacing.md)
        .background(CarePlusPalette.surfaceElevated, in: RoundedRectangle(cornerRadius: CarePlusRadius.md))
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
        .background(CarePlusPalette.surfaceElevated, in: RoundedRectangle(cornerRadius: CarePlusRadius.md))
    }

    private func conditionRow(_ c: HealthCondition) -> some View {
        HStack(spacing: CarePlusSpacing.md) {
            Image(systemName: c.symbol)
                .font(.title3)
                .frame(width: 36, height: 36)
                .background(tint.opacity(0.12), in: RoundedRectangle(cornerRadius: 9))
                .foregroundStyle(tint)
            VStack(alignment: .leading, spacing: 2) {
                Text(c.label).font(.headline)
                Text(careTipFor(c)).font(.caption).foregroundStyle(.secondary)
            }
            Spacer()
            Text(latestReadingFor(c))
                .font(.caption.weight(.semibold))
                .foregroundStyle(tint)
        }
        .padding(CarePlusSpacing.md)
        .background(CarePlusPalette.surfaceElevated, in: RoundedRectangle(cornerRadius: CarePlusRadius.md))
    }

    private func emptyRow(symbol: String, text: String) -> some View {
        HStack(alignment: .top, spacing: CarePlusSpacing.md) {
            Image(systemName: symbol).foregroundStyle(.secondary)
            Text(text).font(.caption).foregroundStyle(.secondary)
        }
        .padding(CarePlusSpacing.md)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(CarePlusPalette.surfaceElevated, in: RoundedRectangle(cornerRadius: CarePlusRadius.md))
    }

    /// Tiny stub that maps a declared condition to a one-liner. Real care
    /// plan content (action items, articles, escalation paths) ships in
    /// week 3 alongside the symptoms log.
    private func careTipFor(_ c: HealthCondition) -> String {
        switch c {
        case .hypertension:     return "Aim for < 130/80 mmHg. Limit sodium."
        case .lowBloodPressure: return "Stay hydrated. Avoid sudden standing."
        case .heartCondition:   return "Keep workouts under your prescribed HR cap."
        case .diabetesT1, .diabetesT2:
            return "Pre/post-meal glucose check. Carb count."
        case .asthma:           return "Carry inhaler. Watch AQI."
        case .obesity:          return "Steady cardio + 0.5 kg/week deficit."
        default:                return "Tap to see condition-specific guidance."
        }
    }

    /// Placeholder for the per-condition latest reading. Uses pre-loaded
    /// HealthKit data from the `readings` state dictionary.
    private func latestReadingFor(_ c: HealthCondition) -> String {
        readings[c]?.0 ?? "—"
    }
}
