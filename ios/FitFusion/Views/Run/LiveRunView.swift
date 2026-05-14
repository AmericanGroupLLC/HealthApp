import SwiftUI
import MapKit
import CoreLocation
import HealthKit
import FitFusionCore

/// Live run tracker with real-time stats, GPS polyline, and HealthKit write on
/// completion. Uses `CLLocationManager` for GPS and `iOSHealthKitManager` for
/// heart-rate streaming and workout save.
struct LiveRunView: View {
    @StateObject private var vm = LiveRunViewModel()
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ZStack(alignment: .bottom) {
                mapLayer
                statsOverlay
                controlBar
            }
            .ignoresSafeArea(edges: .top)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                        .disabled(vm.state == .running)
                }
            }
            .alert("Run Saved!", isPresented: $vm.showSavedAlert) {
                Button("OK") { dismiss() }
            } message: {
                Text(vm.savedSummary)
            }
        }
    }

    // MARK: - Map

    private var mapLayer: some View {
        Map {
            if vm.routeCoordinates.count >= 2 {
                MapPolyline(coordinates: vm.routeCoordinates)
                    .stroke(CarePlusPalette.workoutPink, lineWidth: 4)
            }
            if let loc = vm.currentLocation {
                Annotation("", coordinate: loc) {
                    Circle()
                        .fill(CarePlusPalette.workoutPink)
                        .frame(width: 14, height: 14)
                        .overlay(Circle().stroke(.white, lineWidth: 2))
                }
            }
        }
        .mapStyle(.standard(elevation: .flat))
        .accessibilityLabel("Run route map")
        .accessibilityAddTraits(.isImage)
    }

    // MARK: - Stats overlay

    private var statsOverlay: some View {
        VStack(spacing: 0) {
            Spacer()
            VStack(spacing: CarePlusSpacing.md) {
                HStack(spacing: CarePlusSpacing.lg) {
                    statColumn(label: "Distance", value: vm.distanceFormatted, unit: "km")
                    statColumn(label: "Pace", value: vm.paceFormatted, unit: "/km")
                    statColumn(label: "Time", value: vm.elapsedFormatted, unit: "")
                    statColumn(label: "HR", value: vm.heartRateFormatted, unit: "bpm")
                }
                .padding(CarePlusSpacing.lg)
                .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: CarePlusRadius.lg))
            }
            .padding(.horizontal, CarePlusSpacing.lg)
            .padding(.bottom, 100)
        }
    }

    private func statColumn(label: String, value: String, unit: String) -> some View {
        VStack(spacing: CarePlusSpacing.xs) {
            Text(label)
                .font(CarePlusType.captionEm)
                .foregroundStyle(CarePlusPalette.onSurfaceMuted)
            Text(value)
                .font(.system(size: 28, weight: .heavy, design: .rounded))
                .foregroundStyle(CarePlusPalette.onSurface)
            if !unit.isEmpty {
                Text(unit)
                    .font(CarePlusType.caption)
                    .foregroundStyle(CarePlusPalette.onSurfaceMuted)
            }
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Controls

    private var controlBar: some View {
        HStack(spacing: CarePlusSpacing.xl) {
            switch vm.state {
            case .idle:
                Button { vm.start() } label: {
                    controlCircle(icon: "play.fill", color: CarePlusPalette.trainGreen)
                }
            case .running:
                Button { vm.pause() } label: {
                    controlCircle(icon: "pause.fill", color: .orange)
                }
            case .paused:
                Button { vm.resume() } label: {
                    controlCircle(icon: "play.fill", color: CarePlusPalette.trainGreen)
                }
                Button { vm.stop() } label: {
                    controlCircle(icon: "stop.fill", color: CarePlusPalette.danger)
                }
            case .finished:
                EmptyView()
            }
        }
        .padding(.bottom, CarePlusSpacing.xxl)
    }

    private func controlCircle(icon: String, color: Color) -> some View {
        Image(systemName: icon)
            .font(.title)
            .foregroundStyle(.white)
            .frame(width: 64, height: 64)
            .background(color, in: Circle())
            .shadow(radius: 4)
            .accessibilityLabel(controlLabel(for: icon))
    }

    private func controlLabel(for icon: String) -> String {
        switch icon {
        case "play.fill": return "Start run"
        case "pause.fill": return "Pause run"
        case "stop.fill": return "Stop run"
        default: return icon
        }
    }
}

// MARK: - ViewModel

@MainActor
final class LiveRunViewModel: NSObject, ObservableObject, CLLocationManagerDelegate {
    enum RunState { case idle, running, paused, finished }

    @Published var state: RunState = .idle
    @Published var routeCoordinates: [CLLocationCoordinate2D] = []
    @Published var currentLocation: CLLocationCoordinate2D?
    @Published var distanceMeters: Double = 0
    @Published var elapsedSeconds: TimeInterval = 0
    @Published var heartRate: Double = 0
    @Published var showSavedAlert = false
    @Published var savedSummary = ""

    private let locationManager = CLLocationManager()
    private let healthKit = iOSHealthKitManager.shared
    private let store = HKHealthStore()
    private var timer: Timer?
    private var startDate: Date?
    private var lastLocation: CLLocation?

    // Queries
    private var hrQuery: HKAnchoredObjectQuery?

    override init() {
        super.init()
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.activityType = .fitness
        locationManager.allowsBackgroundLocationUpdates = false
    }

    // MARK: - Formatted values

    var distanceFormatted: String {
        String(format: "%.2f", distanceMeters / 1000.0)
    }

    var paceFormatted: String {
        let km = distanceMeters / 1000.0
        guard km > 0.01 else { return "--:--" }
        let secPerKm = elapsedSeconds / km
        let mins = Int(secPerKm) / 60
        let secs = Int(secPerKm) % 60
        return String(format: "%d:%02d", mins, secs)
    }

    var elapsedFormatted: String {
        let mins = Int(elapsedSeconds) / 60
        let secs = Int(elapsedSeconds) % 60
        return String(format: "%02d:%02d", mins, secs)
    }

    var heartRateFormatted: String {
        heartRate > 0 ? "\(Int(heartRate))" : "--"
    }

    // MARK: - Controls

    func start() {
        locationManager.requestWhenInUseAuthorization()
        locationManager.startUpdatingLocation()
        startDate = Date()
        state = .running
        startTimer()
        startHeartRateStream()
    }

    func pause() {
        state = .paused
        timer?.invalidate()
        locationManager.stopUpdatingLocation()
    }

    func resume() {
        state = .running
        startTimer()
        locationManager.startUpdatingLocation()
    }

    func stop() {
        state = .finished
        timer?.invalidate()
        locationManager.stopUpdatingLocation()
        stopHeartRateStream()
        Task { await saveToHealthKit() }
    }

    // MARK: - Timer

    private func startTimer() {
        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [weak self] _ in
            Task { @MainActor in self?.elapsedSeconds += 1 }
        }
    }

    // MARK: - CLLocationManagerDelegate

    nonisolated func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        Task { @MainActor in
            for loc in locations {
                let coord = loc.coordinate
                if let prev = lastLocation {
                    distanceMeters += loc.distance(from: prev)
                }
                lastLocation = loc
                currentLocation = coord
                routeCoordinates.append(coord)
            }
        }
    }

    // MARK: - Heart rate stream

    private func startHeartRateStream() {
        let hrType = HKQuantityType(.heartRate)
        let predicate = HKQuery.predicateForSamples(withStart: Date(), end: nil)
        let query = HKAnchoredObjectQuery(
            type: hrType,
            predicate: predicate,
            anchor: nil,
            limit: HKObjectQueryNoLimit
        ) { [weak self] _, samples, _, _, _ in
            Task { @MainActor in self?.processHRSamples(samples) }
        }
        query.updateHandler = { [weak self] _, samples, _, _, _ in
            Task { @MainActor in self?.processHRSamples(samples) }
        }
        store.execute(query)
        hrQuery = query
    }

    private func processHRSamples(_ samples: [HKSample]?) {
        guard let samples = samples as? [HKQuantitySample], let last = samples.last else { return }
        let bpm = last.quantity.doubleValue(for: HKUnit.count().unitDivided(by: .minute()))
        Task { @MainActor in heartRate = bpm }
    }

    private func stopHeartRateStream() {
        if let q = hrQuery { store.stop(q) }
        hrQuery = nil
    }

    // MARK: - Save

    private func saveToHealthKit() async {
        guard let start = startDate else { return }
        let end = Date()
        let config = HKWorkoutConfiguration()
        config.activityType = .running
        config.locationType = .outdoor

        do {
            let builder = HKWorkoutBuilder(healthStore: store, configuration: config, device: .local())
            try await builder.beginCollection(at: start)

            let distanceSample = HKQuantitySample(
                type: HKQuantityType(.distanceWalkingRunning),
                quantity: HKQuantity(unit: .meter(), doubleValue: distanceMeters),
                start: start, end: end
            )
            try await builder.addSamples([distanceSample])
            try await builder.endCollection(at: end)
            try await builder.finishWorkout()

            let km = distanceMeters / 1000.0
            savedSummary = String(format: "%.2f km in %@", km, elapsedFormatted)
            showSavedAlert = true
        } catch {
            savedSummary = "Failed to save: \(error.localizedDescription)"
            showSavedAlert = true
        }
    }
}
