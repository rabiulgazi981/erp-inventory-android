package com.bongshaltech.erpinventory.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bongshaltech.erpinventory.ui.components.Tab

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val STAFF_LOGIN = "staff_login"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val PRODUCTS = "products"
    const val PRODUCTS_LOW = "products_low"
    const val PRODUCT_EDIT = "product/{id}"
    const val SALE = "sale"
    const val MORE = "more"
    const val BIZ = "biz"
    const val STAFF_MANAGE = "staff_manage"
    const val SOON = "soon"
    const val CUSTOMERS = "customers"
    const val CUSTOMERS_DUE = "customers_due"
    const val CUSTOMER = "customer/{id}"
    const val CUSTOMER_EDIT = "customer_edit/{id}"
    const val PURCHASES = "purchases"
    const val PURCHASE_NEW = "purchase_new"
    const val SUPPLIERS = "suppliers"
    const val SUPPLIER = "supplier/{id}"
    const val SUPPLIER_EDIT = "supplier_edit/{id}"
    const val EXPENSES = "expenses"
    const val EXPENSE_NEW = "expense_new"
    const val REPORTS = "reports"
    const val MFS = "mfs"
    const val MFS_ENTRY = "mfs_entry/{id}"
    const val MFS_WALLETS = "mfs_wallets"
    const val SMS_ACCOUNT = "sms_account"

    fun productEdit(id: Long) = "product/$id"
    fun customer(id: Long) = "customer/$id"
    fun customerEdit(id: Long) = "customer_edit/$id"
    fun supplier(id: Long) = "supplier/$id"
    fun supplierEdit(id: Long) = "supplier_edit/$id"
    fun mfsEntry(walletId: Long) = "mfs_entry/$walletId"
}

/** Top-level moves (after login, sign out) clear the back stack so Back never returns to a signed-out screen. */
private fun NavHostController.goRoot(route: String) = navigate(route) {
    popUpTo(0) { inclusive = true }
    launchSingleTop = true
}

/** Bottom tabs: Home stays at the root, other tabs sit on top of it so Back returns Home. */
private fun NavHostController.goTab(t: Tab) {
    val route = tabRoute(t)
    if (route == Routes.HOME) {
        popBackStack(Routes.HOME, inclusive = false)
    } else navigate(route) {
        popUpTo(Routes.HOME) { inclusive = false }
        launchSingleTop = true
    }
}

@Composable
fun ErpNavHost() {
    val nav = rememberNavController()
    val back: () -> Unit = { nav.popBackStack() }
    NavHost(nav, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) { SplashScreen { nav.goRoot(it) } }
        composable(Routes.LOGIN) { LoginScreen { if (it == Routes.STAFF_LOGIN) nav.navigate(it) else nav.goRoot(it) } }
        composable(Routes.STAFF_LOGIN) { StaffLoginScreen(onBack = back) { nav.goRoot(it) } }
        composable(Routes.ONBOARDING) { OnboardingScreen { nav.goRoot(it) } }
        composable(Routes.HOME) { HomeScreen(go = { nav.navigate(it) }, tab = { nav.goTab(it) }) }
        composable(Routes.PRODUCTS) { ProductsScreen(false, go = { nav.navigate(it) }, tab = { nav.goTab(it) }) }
        composable(Routes.PRODUCTS_LOW) { ProductsScreen(true, go = { nav.navigate(it) }, tab = { nav.goTab(it) }) }
        composable(Routes.PRODUCT_EDIT, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
            ProductEditScreen(e.arguments?.getLong("id") ?: 0L, back)
        }
        composable(Routes.SALE) { SaleScreen(back) }
        composable(Routes.MORE) {
            MoreScreen(go = { nav.navigate(it) }, tab = { nav.goTab(it) }, signedOut = { nav.goRoot(Routes.LOGIN) })
        }
        composable(Routes.BIZ) { BizProfileScreen(back) }
        composable(Routes.STAFF_MANAGE) { StaffManageScreen(back) }
        composable(Routes.SOON) { SoonScreen(back) }
        composable(Routes.CUSTOMERS) { CustomersScreen(false, go = { nav.navigate(it) }, onBack = back) }
        composable(Routes.CUSTOMERS_DUE) { CustomersScreen(true, go = { nav.navigate(it) }, onBack = back) }
        composable(Routes.CUSTOMER, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
            CustomerDetailScreen(e.arguments?.getLong("id") ?: 0L, go = { nav.navigate(it) }, onBack = back)
        }
        composable(Routes.CUSTOMER_EDIT, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
            CustomerEditScreen(e.arguments?.getLong("id") ?: 0L, back)
        }
        composable(Routes.PURCHASES) { PurchasesScreen(go = { nav.navigate(it) }, onBack = back) }
        composable(Routes.PURCHASE_NEW) { PurchaseNewScreen(go = { nav.navigate(it) }, onBack = back) }
        composable(Routes.SUPPLIERS) { SuppliersScreen(go = { nav.navigate(it) }, onBack = back) }
        composable(Routes.SUPPLIER, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
            SupplierDetailScreen(e.arguments?.getLong("id") ?: 0L, go = { nav.navigate(it) }, onBack = back)
        }
        composable(Routes.SUPPLIER_EDIT, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
            SupplierEditScreen(e.arguments?.getLong("id") ?: 0L, back)
        }
        composable(Routes.EXPENSES) { ExpensesScreen(go = { nav.navigate(it) }, onBack = back) }
        composable(Routes.EXPENSE_NEW) { ExpenseNewScreen(back) }
        composable(Routes.MFS) { MfsScreen(go = { nav.navigate(it) }, onBack = back) }
        composable(Routes.MFS_ENTRY, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
            MfsEntryScreen(e.arguments?.getLong("id") ?: 0L, back)
        }
        composable(Routes.MFS_WALLETS) { MfsWalletsScreen(back) }
        composable(Routes.SMS_ACCOUNT) { SmsAccountScreen(back) }
        composable(Routes.REPORTS) { ReportsScreen(tab = { nav.goTab(it) }) }
    }
}
