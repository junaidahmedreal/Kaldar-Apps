package com.example.data.ai

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

data class ParsedReceiptData(
    val merchant: String = "Unknown Merchant",
    val date: String = "",
    val time: String = "",
    val totalAmount: Double = 0.0,
    val currency: String = "$",
    val category: String = "Other",
    val paymentMethod: String = "Cash",
    val tax: Double = 0.0,
    val discount: Double = 0.0,
    val invoiceNumber: String = "",
    val items: List<com.example.data.local.entity.ReceiptItem> = emptyList()
)

object RuleBasedCategorizer {

    private val CATEGORY_RULES: Map<String, List<String>> = mapOf(
        "Food" to listOf(
            "pizza", "burger", "restaurant", "cafe", "coffee", "dining", "kfc",
            "mcdonald", "bakery", "food", "starbucks", "dinner", "lunch", "breakfast",
            "bistro", "sandwich", "tea", "bar", "grill", "takeaway", "subway", "domino"
        ),
        "Fuel" to listOf(
            "petrol", "fuel", "diesel", "gasoline", "shell", "bp", "cng", "station",
            "oil", "chevron", "exxon", "total", "gas station", "petroleum"
        ),
        "Medical" to listOf(
            "medicine", "pharmacy", "hospital", "clinic", "doctor", "pharma",
            "health", "drug", "tablet", "syrup", "prescription", "lab", "dental", "chemist"
        ),
        "Groceries" to listOf(
            "supermarket", "mart", "walmart", "grocery", "store", "provisions",
            "vegetables", "fruits", "target", "costco", "aldi", "carrefour", "kroger", "milk", "bread"
        ),
        "Travel" to listOf(
            "uber", "taxi", "cab", "flight", "airline", "train", "metro", "bus",
            "ticket", "hotel", "booking", "airbnb", "fare", "toll", "transit", "parking"
        ),
        "Utilities" to listOf(
            "electric", "power", "water", "bill", "recharge", "wifi", "internet",
            "broadband", "gas bill", "utility", "sewer", "telecom", "cellular"
        ),
        "Shopping" to listOf(
            "amazon", "fashion", "clothes", "apparel", "electronics", "shoes",
            "mall", "boutique", "zara", "h&m", "retail", "clothing"
        ),
        "Entertainment" to listOf(
            "cinema", "movie", "netflix", "game", "theatre", "imax", "amusement",
            "concert", "spotify", "bowling", "event", "ticketmaster"
        ),
        "Office" to listOf(
            "office", "stationery", "paper", "printer", "ink", "staples", "desk",
            "hardware", "software", "supplies"
        )
    )

    fun categorize(rawText: String): String {
        val lower = rawText.lowercase(Locale.ROOT)
        for ((category, keywords) in CATEGORY_RULES) {
            for (kw in keywords) {
                if (lower.contains(kw)) {
                    return category
                }
            }
        }
        return "Other"
    }

    fun detectCurrency(rawText: String, defaultCurrency: String = "$"): String {
        val lower = rawText.lowercase(Locale.ROOT)
        return when {
            rawText.contains("$") || lower.contains("usd") -> "$"
            rawText.contains("€") || lower.contains("eur") -> "€"
            rawText.contains("£") || lower.contains("gbp") -> "£"
            rawText.contains("₹") || lower.contains("inr") -> "₹"
            rawText.contains("₨") || lower.contains("pkr") || lower.contains("rs.") || lower.contains("rs ") -> "Rs"
            lower.contains("aed") -> "AED"
            lower.contains("cad") -> "C$"
            lower.contains("aud") -> "A$"
            lower.contains("sar") -> "SAR"
            else -> defaultCurrency
        }
    }

    fun detectPaymentMethod(rawText: String): String {
        val lower = rawText.lowercase(Locale.ROOT)
        return when {
            lower.contains("cash") -> "Cash"
            lower.contains("visa") || lower.contains("mastercard") || lower.contains("credit card") || lower.contains("debit card") || lower.contains("card") -> "Card"
            lower.contains("upi") || lower.contains("gpay") || lower.contains("phonepe") || lower.contains("paytm") -> "UPI"
            lower.contains("online") || lower.contains("paypal") || lower.contains("apple pay") || lower.contains("google pay") -> "Online"
            else -> "Cash"
        }
    }

    fun parseOfflineFallback(rawText: String, defaultCurrency: String = "$"): ParsedReceiptData {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val merchant = if (lines.isNotEmpty()) {
            // First line that is not a numeric date or symbol
            lines.firstOrNull { line ->
                line.length >= 3 && !line.matches(Regex("^[0-9\\W]+$"))
            } ?: lines.first()
        } else {
            "Unknown Merchant"
        }

        // Amount extraction
        var extractedAmount = 0.0
        val amountRegex = Regex("(?i)(?:total|balance due|amount|net|subtotal|sum)[^0-9]*([0-9]+[.,][0-9]{2})")
        val match = amountRegex.find(rawText)
        if (match != null) {
            val numStr = match.groupValues[1].replace(",", ".")
            extractedAmount = numStr.toDoubleOrNull() ?: 0.0
        }

        if (extractedAmount == 0.0) {
            // Find highest number with 2 decimal places in lines
            val decimalRegex = Regex("([0-9]+\\.[0-9]{2})")
            val allAmounts = decimalRegex.findAll(rawText).mapNotNull {
                it.value.toDoubleOrNull()
            }.toList()
            if (allAmounts.isNotEmpty()) {
                extractedAmount = allAmounts.maxOrNull() ?: 0.0
            }
        }

        // Date extraction
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val dateRegex = Regex("([0-9]{4}[-/][0-9]{1,2}[-/][0-9]{1,2}|[0-9]{1,2}[-/][0-9]{1,2}[-/][0-9]{2,4})")
        val dateMatch = dateRegex.find(rawText)
        val extractedDate = dateMatch?.value ?: today

        // Time extraction
        val nowTime = SimpleDateFormat("HH:mm", Locale.US).format(Date())
        val timeRegex = Regex("([0-1]?[0-9]|2[0-3]):[0-5][0-9](?::[0-5][0-9])?\\s*(?:AM|PM|am|pm)?")
        val timeMatch = timeRegex.find(rawText)
        val extractedTime = timeMatch?.value ?: nowTime

        // Invoice number extraction
        val invoiceRegex = Regex("(?i)(?:invoice|inv|bill|receipt|order)[^0-9a-zA-Z#]*#?\\s*([0-9a-zA-Z\\-]+)")
        val invoiceMatch = invoiceRegex.find(rawText)
        val invoice = invoiceMatch?.groupValues?.getOrNull(1) ?: ""

        val currency = detectCurrency(rawText, defaultCurrency)
        val category = categorize(rawText)
        val paymentMethod = detectPaymentMethod(rawText)

        return ParsedReceiptData(
            merchant = merchant,
            date = extractedDate,
            time = extractedTime,
            totalAmount = extractedAmount,
            currency = currency,
            category = category,
            paymentMethod = paymentMethod,
            tax = 0.0,
            discount = 0.0,
            invoiceNumber = invoice,
            items = emptyList()
        )
    }
}
