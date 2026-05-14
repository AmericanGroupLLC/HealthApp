import SwiftUI
import FitFusionCore

/// Simple cart checkout screen — displays line items with quantity and price,
/// calculates a total, and places an order via API or local confirmation
/// with a generated order ID.
struct OrderCheckoutView: View {
    @Environment(\.dismiss) private var dismiss

    @State private var items: [CartItem] = CartItem.sampleCart
    @State private var isPlacingOrder = false
    @State private var orderPlaced = false
    @State private var orderID = ""

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: CarePlusSpacing.lg) {
                    itemsList
                    totalsCard
                    placeOrderButton
                }
                .padding(CarePlusSpacing.lg)
            }
            .background(CarePlusPalette.surface.ignoresSafeArea())
            .navigationTitle("Checkout")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
            .alert("Order Placed", isPresented: $orderPlaced) {
                Button("Done") { dismiss() }
            } message: {
                Text("Your order #\(orderID) has been confirmed.")
            }
        }
    }

    // MARK: - Items

    private var itemsList: some View {
        VStack(alignment: .leading, spacing: CarePlusSpacing.sm) {
            Text("Your Cart").font(CarePlusType.titleSM)
            if items.isEmpty {
                ContentUnavailableView(
                    "Cart is empty",
                    systemImage: "cart",
                    description: Text("Add items from the menu to get started.")
                )
            } else {
                ForEach($items) { $item in
                    HStack(spacing: CarePlusSpacing.md) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(item.name).font(CarePlusType.bodyEm)
                            Text(String(format: "$%.2f each", item.unitPrice))
                                .font(CarePlusType.caption)
                                .foregroundStyle(CarePlusPalette.onSurfaceMuted)
                        }
                        Spacer()
                        quantityStepper(item: $item)
                        Text(String(format: "$%.2f", item.lineTotal))
                            .font(CarePlusType.bodyEm)
                            .frame(width: 60, alignment: .trailing)
                    }
                    .padding(CarePlusSpacing.md)
                    .background(CarePlusPalette.surfaceElevated, in: RoundedRectangle(cornerRadius: CarePlusRadius.sm))
                }
            }
        }
    }

    private func quantityStepper(item: Binding<CartItem>) -> some View {
        HStack(spacing: CarePlusSpacing.sm) {
            Button {
                if item.wrappedValue.quantity > 1 { item.wrappedValue.quantity -= 1 }
                else { items.removeAll { $0.id == item.wrappedValue.id } }
            } label: {
                Image(systemName: "minus.circle.fill")
                    .foregroundStyle(CarePlusPalette.danger)
            }
            Text("\(item.wrappedValue.quantity)")
                .font(CarePlusType.bodyEm)
                .frame(minWidth: 20)
            Button { item.wrappedValue.quantity += 1 } label: {
                Image(systemName: "plus.circle.fill")
                    .foregroundStyle(CarePlusPalette.trainGreen)
            }
        }
    }

    // MARK: - Totals

    private var totalsCard: some View {
        let subtotal = items.reduce(0) { $0 + $1.lineTotal }
        let tax = subtotal * 0.08
        let total = subtotal + tax

        return VStack(spacing: CarePlusSpacing.sm) {
            totalRow(label: "Subtotal", amount: subtotal)
            totalRow(label: "Tax (8%)", amount: tax)
            Divider()
            totalRow(label: "Total", amount: total, bold: true)
        }
        .padding(CarePlusSpacing.md)
        .background(CarePlusPalette.surfaceElevated, in: RoundedRectangle(cornerRadius: CarePlusRadius.md))
    }

    private func totalRow(label: String, amount: Double, bold: Bool = false) -> some View {
        HStack {
            Text(label).font(bold ? CarePlusType.bodyEm : CarePlusType.body)
            Spacer()
            Text(String(format: "$%.2f", amount))
                .font(bold ? CarePlusType.bodyEm : CarePlusType.body)
                .foregroundStyle(bold ? CarePlusPalette.onSurface : CarePlusPalette.onSurfaceMuted)
        }
    }

    // MARK: - Place order

    private var placeOrderButton: some View {
        Button {
            Task { await placeOrder() }
        } label: {
            HStack {
                if isPlacingOrder {
                    ProgressView().tint(.white)
                }
                Text("Place Order")
                    .font(CarePlusType.bodyEm)
            }
            .frame(maxWidth: .infinity)
            .padding(CarePlusSpacing.md)
            .background(items.isEmpty ? Color.gray : CarePlusPalette.trainGreen,
                        in: RoundedRectangle(cornerRadius: CarePlusRadius.md))
            .foregroundStyle(.white)
        }
        .disabled(items.isEmpty || isPlacingOrder)
    }

    private func placeOrder() async {
        isPlacingOrder = true
        defer { isPlacingOrder = false }

        // Simulate network delay
        try? await Task.sleep(for: .seconds(1))

        orderID = "FF-\(Int.random(in: 100000...999999))"
        orderPlaced = true
    }
}

// MARK: - Cart model

extension OrderCheckoutView {
    struct CartItem: Identifiable {
        let id = UUID()
        let name: String
        let unitPrice: Double
        var quantity: Int

        var lineTotal: Double { unitPrice * Double(quantity) }

        static let sampleCart: [CartItem] = [
            CartItem(name: "Grilled Chicken Bowl", unitPrice: 12.99, quantity: 1),
            CartItem(name: "Green Smoothie", unitPrice: 6.49, quantity: 2),
            CartItem(name: "Protein Bar", unitPrice: 3.99, quantity: 3),
        ]
    }
}
