import SwiftUI
import FitFusionCore

/// Diet → Vendor browse. Fetches the sample vendor list from the backend
/// filtered by the user's declared HealthConditionsStore set.
struct VendorBrowseView: View {

    @StateObject private var conditions = HealthConditionsStore.shared
    @State private var vendors: [VendorClient.Vendor] = []
    @State private var loading = true
    @State private var error: String?
    @State private var selectedVendor: VendorClient.Vendor?

    private let tint = CarePlusPalette.dietCoral

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: CarePlusSpacing.md) {
                Text("Meal vendors").font(CarePlusType.title)
                Text(filterSummary).font(.caption).foregroundStyle(.secondary)

                if loading { ProgressView("Loading…").padding() }
                if let err = error {
                    Text(err).font(.caption).foregroundStyle(CarePlusPalette.danger)
                }

                ForEach(vendors) { v in
                    Button {
                        selectedVendor = v
                    } label: {
                        vendorRow(v)
                    }.buttonStyle(.plain)
                }
            }
            .padding(CarePlusSpacing.lg)
        }
        .navigationTitle("Vendors")
        .navigationBarTitleDisplayMode(.inline)
        .task { await load() }
        .sheet(item: $selectedVendor) { vendor in
            VendorDetailSheet(vendor: vendor, tint: tint)
        }
    }

    private var filterSummary: String {
        let cond = conditions.conditions.filter { $0 != .none }.map(\.label)
        return cond.isEmpty
            ? "Showing all vendors. Declare conditions in Profile to filter."
            : "Filtered for: " + cond.joined(separator: ", ")
    }

    private func vendorRow(_ v: VendorClient.Vendor) -> some View {
        HStack(spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 10).fill(tint.opacity(0.14)).frame(width: 48, height: 48)
                Image(systemName: "fork.knife").foregroundStyle(tint)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(v.name).font(.headline)
                Text(v.cuisine ?? "—").font(.caption).foregroundStyle(.secondary)
                if let c = v.calories_per_meal_avg {
                    Text("\(c) kcal avg").font(.caption2).foregroundStyle(.secondary)
                }
            }
            Spacer()
            Image(systemName: "chevron.right").foregroundStyle(.tertiary)
        }
        .padding(12)
        .background(CarePlusPalette.surfaceElevated, in: RoundedRectangle(cornerRadius: 12))
    }

    private func load() async {
        loading = true; defer { loading = false }
        let names = conditions.conditions.filter { $0 != .none }.map(\.rawValue)
        do {
            self.vendors = try await VendorClient.shared.menu(conditions: names)
            self.error = nil
        } catch {
            self.error = error.localizedDescription
        }
    }
}

private struct VendorDetailSheet: View {
    let vendor: VendorClient.Vendor
    let tint: Color
    @Environment(\.dismiss) private var dismiss

    private struct MenuItem: Identifiable {
        let id = UUID()
        let name: String
        let kcal: Int
        let protein: Int
        let carbs: Int
        let fat: Int
    }

    private var sampleMenu: [MenuItem] {
        let base = vendor.calories_per_meal_avg ?? 500
        return [
            MenuItem(name: "Signature Bowl", kcal: base, protein: base / 15, carbs: base / 8, fat: base / 20),
            MenuItem(name: "Grilled Plate", kcal: Int(Double(base) * 1.1), protein: Int(Double(base) * 1.1) / 12, carbs: Int(Double(base) * 1.1) / 9, fat: Int(Double(base) * 1.1) / 22),
            MenuItem(name: "Light Salad", kcal: Int(Double(base) * 0.65), protein: Int(Double(base) * 0.65) / 14, carbs: Int(Double(base) * 0.65) / 10, fat: Int(Double(base) * 0.65) / 25),
            MenuItem(name: "Wrap Combo", kcal: Int(Double(base) * 0.9), protein: Int(Double(base) * 0.9) / 13, carbs: Int(Double(base) * 0.9) / 7, fat: Int(Double(base) * 0.9) / 18),
        ]
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: CarePlusSpacing.md) {
                    HStack(spacing: 12) {
                        ZStack {
                            RoundedRectangle(cornerRadius: 12).fill(tint.opacity(0.14))
                                .frame(width: 56, height: 56)
                            Image(systemName: "fork.knife").font(.title2).foregroundStyle(tint)
                        }
                        VStack(alignment: .leading, spacing: 2) {
                            Text(vendor.name).font(.title3.bold())
                            if let c = vendor.cuisine {
                                Text(c).font(.subheadline).foregroundStyle(.secondary)
                            }
                        }
                    }

                    if let blurb = vendor.blurb {
                        Text(blurb).font(.caption).foregroundStyle(.secondary)
                    }

                    Text("Menu").font(.headline).padding(.top, 4)

                    ForEach(sampleMenu) { item in
                        VStack(alignment: .leading, spacing: 6) {
                            HStack {
                                Text(item.name).font(.subheadline.weight(.medium))
                                Spacer()
                                Text("\(item.kcal) kcal").font(.subheadline.weight(.semibold))
                                    .foregroundStyle(tint)
                            }
                            HStack(spacing: 16) {
                                macroLabel("P", "\(item.protein)g")
                                macroLabel("C", "\(item.carbs)g")
                                macroLabel("F", "\(item.fat)g")
                            }
                        }
                        .padding(12)
                        .background(CarePlusPalette.surfaceElevated,
                                    in: RoundedRectangle(cornerRadius: 10))
                    }
                }
                .padding(CarePlusSpacing.lg)
            }
            .navigationTitle(vendor.name)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }

    private func macroLabel(_ label: String, _ value: String) -> some View {
        HStack(spacing: 2) {
            Text(label).font(.caption2.weight(.semibold)).foregroundStyle(.secondary)
            Text(value).font(.caption2)
        }
    }
}
