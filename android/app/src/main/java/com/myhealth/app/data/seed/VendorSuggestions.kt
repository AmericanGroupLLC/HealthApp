package com.myhealth.app.data.seed

data class VendorSuggestion(
    val name: String,
    val category: String,
    val tagline: String,
    val url: String,
)

object VendorSuggestions {
    val pharmacies = listOf(
        VendorSuggestion("CVS Pharmacy", "Pharmacy", "Prescriptions, vaccines & wellness", "https://www.cvs.com"),
        VendorSuggestion("Walgreens", "Pharmacy", "Pharmacy, health & wellness", "https://www.walgreens.com"),
        VendorSuggestion("Rite Aid", "Pharmacy", "Pharmacy & drugstore", "https://www.riteaid.com"),
    )

    val fitnessEquipment = listOf(
        VendorSuggestion("Rogue Fitness", "Equipment", "Barbells, racks & gym gear", "https://www.roguefitness.com"),
        VendorSuggestion("Peloton", "Membership", "Connected fitness classes", "https://www.onepeloton.com"),
        VendorSuggestion("Therabody", "Recovery", "Theragun & recovery tools", "https://www.therabody.com"),
    )

    val wearables = listOf(
        VendorSuggestion("Whoop", "Wearable", "HRV, strain & recovery band", "https://www.whoop.com"),
        VendorSuggestion("Oura Ring", "Wearable", "Sleep & readiness ring", "https://ouraring.com"),
        VendorSuggestion("Hyperice", "Recovery", "Percussion, compression & heat", "https://www.hyperice.com"),
    )

    val groceryDelivery = listOf(
        VendorSuggestion("Instacart", "Grocery", "Same-day grocery delivery", "https://www.instacart.com"),
        VendorSuggestion("Thrive Market", "Grocery", "Organic & healthy groceries", "https://thrivemarket.com"),
    )
}
