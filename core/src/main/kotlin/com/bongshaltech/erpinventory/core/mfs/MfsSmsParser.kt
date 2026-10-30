package com.bongshaltech.erpinventory.core.mfs

import com.bongshaltech.erpinventory.core.Money

/**
 * Pulls amount, fee, TrxID and the customer's number out of a bKash / Nagad / operator confirmation SMS the shopkeeper shares to the app.
 *
 * Wallet SMS formats change and differ per provider, so this only looks for the common pieces and never guesses:
 * a field it cannot find stays null and the form asks the shopkeeper.
 */
data class ParsedMfsSms(
    val amount: Money?,
    val fee: Money?,
    val trxId: String?,
    /** Full number when the SMS shows it, e.g. "01812445566". */
    val number: String?,
    /** Masked number when the SMS hides the middle, e.g. "01711***567". */
    val maskedNumber: String?,
) {
    /** Last digits visible in a masked number, used to suggest saved customers. */
    val visibleTail: String? get() = maskedNumber?.takeLastWhile { it.isDigit() }?.takeIf { it.length >= 3 }
}

object MfsSmsParser {
    private val amountRe = Regex("""(?:Tk|BDT|৳)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
    private val feeRe = Regex("""(?:Fee|Charge)\s*(?:Tk|BDT|৳)?\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
    private val trxRe = Regex("""(?:TrxID|TxnID|TxnId|Trx ID|Transaction ID)\s*[:#]?\s*([A-Z0-9]{6,})""", RegexOption.IGNORE_CASE)
    private val fullNumberRe = Regex("""(?<![0-9])(?:\+?88)?(01[3-9][0-9]{8})(?![0-9])""")
    private val maskedNumberRe = Regex("""(?<![0-9])(01[0-9]{1,4}[*Xx•]{2,}[0-9]{2,5})(?![0-9])""")

    fun parse(sms: String): ParsedMfsSms {
        // Remove the fee before reading the main amount, so "Fee Tk 10" is not taken as the amount.
        val feeMatch = feeRe.find(sms)
        val withoutFee = feeMatch?.let { sms.removeRange(it.range) } ?: sms
        return ParsedMfsSms(
            amount = amountRe.find(withoutFee)?.groupValues?.get(1)?.let(Money::parse),
            fee = feeMatch?.groupValues?.get(1)?.let(Money::parse),
            trxId = trxRe.find(sms)?.groupValues?.get(1)?.uppercase(),
            number = fullNumberRe.find(sms)?.groupValues?.get(1),
            maskedNumber = maskedNumberRe.find(sms)?.groupValues?.get(1),
        )
    }

    /** Saved customer numbers that match what the SMS shows (same start and same visible tail). */
    fun matchMasked(masked: String, savedNumbers: List<String>): List<String> {
        val head = masked.takeWhile { it.isDigit() }
        val tail = masked.takeLastWhile { it.isDigit() }
        return savedNumbers.filter { n ->
            val digits = n.filter(Char::isDigit).removePrefix("88")
            digits.length == 11 && digits.startsWith(head) && digits.endsWith(tail)
        }
    }
}
