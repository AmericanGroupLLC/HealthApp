import SwiftUI
import FitFusionCore

typealias Challenge = ChallengesStore.Challenge
typealias LeaderboardEntry = LeaderboardClient.Entry

extension ChallengesStore.Challenge {
    static let placeholder = ChallengesStore.Challenge(
        id: UUID(),
        title: "Sample Challenge",
        kind: "steps",
        startsAt: Date(),
        endsAt: Date().addingTimeInterval(7 * 86400),
        target: 10000,
        joinedAt: Date()
    )
}

struct ChallengeDetailView: View {
    let challenge: Challenge
    @EnvironmentObject var leaderboard: LeaderboardClient
    @State private var scoreInput = ""
    @State private var entries: [LeaderboardEntry] = []

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                GroupBox {
                    VStack(alignment: .leading) {
                        Text(challenge.title).font(.title2).bold()
                        Text("Kind: \(challenge.kind)")
                        Text("Target: \(Int(challenge.target))")
                    }
                }

                GroupBox("Submit Score") {
                    HStack {
                        TextField("Score", text: $scoreInput)
                            .textFieldStyle(.roundedBorder)
                            .keyboardType(.numberPad)
                        Button("Submit") {
                            if let score = Double(scoreInput) {
                                Task { await leaderboard.submit(score: score, challengeId: challenge.id.hashValue) }
                                scoreInput = ""
                                loadLeaderboard()
                            }
                        }
                        .buttonStyle(.borderedProminent)
                        .disabled(Double(scoreInput) == nil)
                    }
                }

                Text("Leaderboard").font(.title3).bold()

                ForEach(Array(entries.enumerated()), id: \.element.id) { index, entry in
                    GroupBox {
                        HStack {
                            let badge: String = {
                                switch index {
                                case 0: return "🥇"
                                case 1: return "🥈"
                                case 2: return "🥉"
                                default: return "#\(index + 1)"
                                }
                            }()
                            Text(badge).frame(width: 30)
                            VStack(alignment: .leading) {
                                Text(entry.name).bold()
                            }
                            Spacer()
                            Text("\(Int(entry.score))").bold()
                        }
                        if challenge.target > 0 {
                            ProgressView(value: min(entry.score / challenge.target, 1.0))
                        }
                    }
                }
            }
            .padding()
        }
        .navigationTitle(challenge.title)
        .task { loadLeaderboard() }
    }

    private func loadLeaderboard() {
        Task {
            entries = (try? await leaderboard.entries(for: challenge.id.hashValue)) ?? []
        }
    }
}
