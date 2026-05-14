import SwiftUI
import CoreData
import FitFusionCore

struct SymptomsLogView: View {
    @State private var bodyLocation = ""
    @State private var painScale: Double = 5
    @State private var durationHours: Double = 1
    @State private var notes = ""
    @State private var history: [NSManagedObject] = []

    private let locations = ["Head", "Chest", "Abdomen", "Back", "Left Arm", "Right Arm", "Left Leg", "Right Leg"]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                GroupBox("Log Symptom") {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Body Location")
                        LazyVGrid(columns: [GridItem(.adaptive(minimum: 80))], spacing: 8) {
                            ForEach(locations, id: \.self) { loc in
                                Button {
                                    bodyLocation = loc
                                } label: {
                                    Text(loc)
                                        .font(.caption)
                                        .padding(.horizontal, 8)
                                        .padding(.vertical, 4)
                                        .background(bodyLocation == loc ? Color.blue : Color.gray.opacity(0.2))
                                        .foregroundColor(bodyLocation == loc ? .white : .primary)
                                        .cornerRadius(8)
                                }
                            }
                        }

                        Text("Pain Scale: \(Int(painScale)) / 10")
                        Slider(value: $painScale, in: 1...10, step: 1)

                        Text("Duration: \(String(format: "%.1f", durationHours)) hours")
                        Slider(value: $durationHours, in: 0.5...72)

                        TextField("Notes", text: $notes, axis: .vertical)
                            .textFieldStyle(.roundedBorder)
                            .lineLimit(2...4)

                        Button {
                            guard !bodyLocation.isEmpty else { return }
                            CloudStore.shared.addSymptom(
                                bodyLocation: bodyLocation,
                                painScale: Int(painScale),
                                durationHours: durationHours,
                                notes: notes.isEmpty ? nil : notes
                            )
                            bodyLocation = ""
                            painScale = 5
                            notes = ""
                            loadHistory()
                        } label: {
                            Label("Log Symptom", systemImage: "plus")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.borderedProminent)
                        .disabled(bodyLocation.isEmpty)
                    }
                }

                if !history.isEmpty {
                    Text("History").font(.title3).bold()
                    ForEach(history, id: \.objectID) { entry in
                        GroupBox {
                            VStack(alignment: .leading) {
                                HStack {
                                    Text(entry.value(forKey: "bodyLocation") as? String ?? "")
                                        .bold()
                                    Spacer()
                                    Text("Pain: \(entry.value(forKey: "painScale") as? Int16 ?? 0)/10")
                                        .foregroundStyle(.secondary)
                                }
                                if let n = entry.value(forKey: "notes") as? String, !n.isEmpty {
                                    Text(n).font(.caption).foregroundStyle(.secondary)
                                }
                                if let d = entry.value(forKey: "recordedAt") as? Date {
                                    Text(d, style: .relative)
                                        .font(.caption2).foregroundStyle(.tertiary)
                                }
                            }
                        }
                        .contextMenu {
                            Button(role: .destructive) {
                                CloudStore.shared.deleteSymptom(entry)
                                loadHistory()
                            } label: {
                                Label("Delete", systemImage: "trash")
                            }
                        }
                    }
                }
            }
            .padding()
        }
        .navigationTitle("Symptoms Log")
        .onAppear { loadHistory() }
    }

    private func loadHistory() {
        history = CloudStore.shared.fetchSymptoms()
    }
}
