package com.bongshaltech.erpinventory.data

import com.bongshaltech.erpinventory.core.Money
import com.bongshaltech.erpinventory.core.mfs.BalanceCounterparty
import com.bongshaltech.erpinventory.core.mfs.MfsCalculator
import com.bongshaltech.erpinventory.core.mfs.MfsEffect
import com.bongshaltech.erpinventory.core.mfs.MfsInput
import com.bongshaltech.erpinventory.core.mfs.MfsType
import com.bongshaltech.erpinventory.core.mfs.WalletGroup
import com.bongshaltech.erpinventory.data.db.MfsWalletEntity

/** Wallets and load balances a Bangladeshi shop can run. Colours are only for the small badge. */
enum class MfsProvider(val label: String, val group: WalletGroup, val color: Long) {
    BKASH("bKash", WalletGroup.MOBILE_BANKING, 0xFFE2136E),
    NAGAD("Nagad", WalletGroup.MOBILE_BANKING, 0xFFF7941D),
    ROCKET("Rocket", WalletGroup.MOBILE_BANKING, 0xFF8C3494),
    UPAY("Upay", WalletGroup.MOBILE_BANKING, 0xFF1C4E9D),
    GP("Grameenphone", WalletGroup.LOAD, 0xFF19AAF8),
    ROBI("Robi / Airtel", WalletGroup.LOAD, 0xFFE2231A),
    BANGLALINK("Banglalink", WalletGroup.LOAD, 0xFFF37021),
    TELETALK("Teletalk", WalletGroup.LOAD, 0xFF00A651),
}

fun MfsWalletEntity.providerOrNull() = MfsProvider.entries.firstOrNull { it.name == provider }
fun MfsWalletEntity.group() = providerOrNull()?.group ?: WalletGroup.MOBILE_BANKING
fun MfsWalletEntity.label() = providerOrNull()?.label ?: provider

/** Entry types shown for each kind of wallet, in the order of the design. */
val walletTypes = listOf(
    MfsType.CASH_IN, MfsType.CASH_OUT, MfsType.SEND_MONEY, MfsType.PAYMENT, MfsType.PAY_BILL, MfsType.BALANCE_IN, MfsType.BALANCE_OUT,
)
val loadTypes = listOf(
    MfsType.RECHARGE, MfsType.INTERNET_PACK, MfsType.MINUTE_PACK, MfsType.BUNDLE_PACK, MfsType.BALANCE_IN, MfsType.BALANCE_OUT,
)

fun typesFor(w: MfsWalletEntity) = if (w.group() == WalletGroup.LOAD) loadTypes else walletTypes

/** Send money and pay bill take a charge from the customer and pay a wallet fee. */
val MfsType.hasCharge get() = this == MfsType.SEND_MONEY || this == MfsType.PAY_BILL
val MfsType.isBalance get() = this == MfsType.BALANCE_IN || this == MfsType.BALANCE_OUT

data class NewMfs(
    val walletId: Long,
    val type: MfsType,
    val customerPhone: String,
    val amountPaisa: Long,
    /** Charge from the customer (send money, pay bill). */
    val chargePaisa: Long = 0,
    /** Wallet fee (send money, pay bill). */
    val feePaisa: Long = 0,
    /** What changed hands on balance in/out; defaults to the amount. */
    val settledPaisa: Long? = null,
    val via: BalanceCounterparty = BalanceCounterparty.DSO,
    val trxId: String = "",
    val note: String = "",
    val staffId: Long? = null,
)

/** What an entry will do, using the owner's commission rate for that wallet. */
fun mfsEffect(w: MfsWalletEntity, n: NewMfs): MfsEffect {
    val a = Money(n.amountPaisa)
    val input = when (n.type) {
        MfsType.CASH_IN -> MfsInput.Simple(n.type, a, w.cashInPct)
        MfsType.CASH_OUT -> MfsInput.Simple(n.type, a, w.cashOutPct)
        MfsType.PAYMENT -> MfsInput.Simple(n.type, a, w.paymentPct)
        MfsType.RECHARGE, MfsType.INTERNET_PACK, MfsType.MINUTE_PACK, MfsType.BUNDLE_PACK -> MfsInput.Simple(n.type, a, w.loadPct)
        MfsType.SEND_MONEY, MfsType.PAY_BILL -> MfsInput.PayBill(a, Money(n.chargePaisa), Money(n.feePaisa))
        MfsType.BALANCE_IN -> MfsInput.Balance(true, a, Money(n.settledPaisa ?: n.amountPaisa), n.via)
        MfsType.BALANCE_OUT -> MfsInput.Balance(false, a, Money(n.settledPaisa ?: n.amountPaisa), n.via)
        MfsType.SCRATCH_CARD, MfsType.SIM_SALE -> error("Scratch cards and SIMs are sold as products")
    }
    return MfsCalculator.effect(input)
}

sealed interface MfsResult {
    data class Saved(val number: String) : MfsResult
    data class LowBalance(val balancePaisa: Long) : MfsResult
    data object NoWallet : MfsResult
}

/** Holds a bKash / Nagad / operator SMS shared to the app until the entry screen reads it. */
object SharedSms {
    var text: String? = null
}
