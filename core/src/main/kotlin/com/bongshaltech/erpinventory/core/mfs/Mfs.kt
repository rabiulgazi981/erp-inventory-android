package com.bongshaltech.erpinventory.core.mfs

import com.bongshaltech.erpinventory.core.Money

/** Mobile banking (bKash, Nagad, Rocket, Upay) or mobile operator load (GP, Robi/Airtel, Banglalink, Teletalk). */
enum class WalletGroup { MOBILE_BANKING, LOAD }

enum class AccountKind { AGENT, PERSONAL }

enum class MfsType(val group: WalletGroup?) {
    CASH_IN(WalletGroup.MOBILE_BANKING),
    CASH_OUT(WalletGroup.MOBILE_BANKING),
    PAY_BILL(WalletGroup.MOBILE_BANKING),
    SEND_MONEY(WalletGroup.MOBILE_BANKING),
    PAYMENT(WalletGroup.MOBILE_BANKING),
    RECHARGE(WalletGroup.LOAD),
    INTERNET_PACK(WalletGroup.LOAD),
    MINUTE_PACK(WalletGroup.LOAD),
    BUNDLE_PACK(WalletGroup.LOAD),
    SCRATCH_CARD(WalletGroup.LOAD),
    SIM_SALE(WalletGroup.LOAD),
    BALANCE_IN(null),
    BALANCE_OUT(null);

    fun allowedFor(group: WalletGroup) = this.group == null || this.group == group
}

/** Where e-money came from or went to on Balance in / Balance out. */
enum class BalanceCounterparty { DSO, CASH, BANK, OTHER_WALLET }

sealed interface MfsInput {
    /** Cash in/out, send money, payment, recharge and packs: one amount, commission from the shop's own rate. */
    data class Simple(val type: MfsType, val amount: Money, val commissionPercent: Double) : MfsInput

    /** Customer gives cash for bill + service charge; wallet pays bill + wallet fee. */
    data class PayBill(val bill: Money, val serviceCharge: Money, val walletFee: Money) : MfsInput

    /** E-money moved in or out. [settled] is what changed hands (less than [amount] when load is bought at a discount). */
    data class Balance(val incoming: Boolean, val amount: Money, val settled: Money, val via: BalanceCounterparty) : MfsInput

    /** Scratch card or SIM: physical stock sold for cash, wallet untouched. */
    data class StockSale(val unitSell: Money, val unitCost: Money, val qty: Int) : MfsInput
}

/** What one entry does to the wallet balance, the cash drawer and the shop's earnings. */
data class MfsEffect(val walletDelta: Money, val cashDelta: Money, val earning: Money, val stockDelta: Int = 0)

object MfsCalculator {

    fun effect(input: MfsInput): MfsEffect = when (input) {
        is MfsInput.Simple -> {
            val commission = input.amount.percent(input.commissionPercent)
            when (input.type) {
                // Agent receives e-money from the customer and pays out cash.
                MfsType.CASH_OUT -> MfsEffect(input.amount, -input.amount, commission)
                MfsType.CASH_IN, MfsType.SEND_MONEY, MfsType.PAYMENT,
                MfsType.RECHARGE, MfsType.INTERNET_PACK, MfsType.MINUTE_PACK, MfsType.BUNDLE_PACK ->
                    MfsEffect(-input.amount, input.amount, commission)
                else -> error("${input.type} needs its own input type")
            }
        }
        is MfsInput.PayBill -> MfsEffect(
            walletDelta = -(input.bill + input.walletFee),
            cashDelta = input.bill + input.serviceCharge,
            earning = input.serviceCharge - input.walletFee,
        )
        is MfsInput.Balance -> {
            val cashMoves = input.via == BalanceCounterparty.CASH || input.via == BalanceCounterparty.DSO
            if (input.incoming) MfsEffect(
                walletDelta = input.amount,
                cashDelta = if (cashMoves) -input.settled else Money.ZERO,
                // Buying load at a discount is the load commission.
                earning = input.amount - input.settled,
            ) else MfsEffect(
                walletDelta = -input.amount,
                cashDelta = if (cashMoves) input.settled else Money.ZERO,
                earning = input.settled - input.amount,
            )
        }
        is MfsInput.StockSale -> MfsEffect(
            walletDelta = Money.ZERO,
            cashDelta = input.unitSell * input.qty,
            earning = (input.unitSell - input.unitCost) * input.qty,
            stockDelta = -input.qty,
        )
    }
}
