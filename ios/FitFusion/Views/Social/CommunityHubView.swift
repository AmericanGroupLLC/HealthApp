import SwiftUI
import FitFusionCore

struct CommunityHubView: View {
    @EnvironmentObject var challenges: ChallengesStore
    @EnvironmentObject var friends: FriendsStore

    @State private var showAddFriend = false
    @State private var showCreateChallenge = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                // Active Challenges
                HStack {
                    Text("Active Challenges").font(.title3).bold()
                    Spacer()
                    Button { showCreateChallenge = true } label: {
                        Label("Create", systemImage: "plus")
                    }
                }

                ForEach(challenges.active) { challenge in
                    NavigationLink(destination: ChallengeDetailView(challenge: challenge)) {
                        GroupBox {
                            VStack(alignment: .leading) {
                                Text(challenge.title).bold()
                                Text("\(challenge.kind) • Target: \(Int(challenge.target))")
                                    .font(.caption).foregroundStyle(.secondary)
                            }
                        }
                    }
                    .buttonStyle(.plain)
                }

                // Friends
                HStack {
                    Text("Friends").font(.title3).bold()
                    Spacer()
                    Button { showAddFriend = true } label: {
                        Label("Add", systemImage: "person.badge.plus")
                    }
                }

                ForEach(friends.friends) { friend in
                    GroupBox {
                        HStack {
                            VStack(alignment: .leading) {
                                Text(friend.name).bold()
                                Text("@" + friend.handle).font(.caption).foregroundStyle(.secondary)
                            }
                            Spacer()
                        }
                    }
                }
            }
            .padding()
        }
        .navigationTitle("Community")
        .sheet(isPresented: $showAddFriend) {
            AddFriendSheet(friends: friends)
        }
    }
}

private struct AddFriendSheet: View {
    @ObservedObject var friends: FriendsStore
    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @State private var handle = ""

    var body: some View {
        NavigationStack {
            Form {
                TextField("Name", text: $name)
                TextField("Handle", text: $handle)
            }
            .navigationTitle("Add Friend")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Add") {
                        Task { await friends.addFriend(name: name, handle: handle) }
                        dismiss()
                    }
                    .disabled(name.isEmpty || handle.isEmpty)
                }
            }
        }
    }
}
