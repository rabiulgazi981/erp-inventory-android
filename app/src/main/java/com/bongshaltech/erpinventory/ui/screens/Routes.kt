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
    const val SALE_SCAN = "sale_scan"
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
    const val BACKUP = "backup"
    const val RECEIPT = "receipt/{id}"
    const val PRINTER = "printer"
    const val SMS_GATEWAY = "sms_gateway"
    const val COURIER = "courier"
    const val RETURNS = "returns"
    const val ORDERS = "orders"
    const val ORDER = "order/{kind}/{id}"
    const val SALE_RETURN = "sale_return/{id}"
    const val PURCHASE_RETURN = "purchase_return/{id}"
    const val AD_FREE = "ad_free"
    const val CASH_BANK = "cash_bank"
    const val LEDGER = "ledger/{m}"
    const val LOANS = "loans"
    const val LOAN = "loan/{id}"
    const val CASH_DAY = "cash_day"
    const val PROMOS = "promos"
    const val HR = "hr"
    const val BATCHES = "batches"
    const val REPACK = "repack"
    const val IMPORT = "import"
    const val LABELS = "labels"
    const val AUDIT = "audit"
    const val SERIALS = "serials"
    const val WAREHOUSES = "warehouses"
    const val WAREHOUSE = "warehouse/{id}"
    const val TABLES = "tables"
    const val FEATURES = "features"
    const val DELETED_SALES = "deleted_sales"
    const val STOCK_ADJUST = "stock_adjust"
    const val REORDER = "reorder"
    const val ACCOUNTS = "accounts"
    const val JOURNAL = "journal"
    const val RECURRING = "recurring"
    const val INVESTORS = "investors"
    const val RECIPES = "recipes"
    const val FACTORY = "factory"
    const val REPAIR = "repair"
    const val KITCHEN = "kitchen"
    const val REST_REPORT = "rest_report"
    const val PHARMA_REPORT = "pharma_report"
    const val REPORT_LIST = "reports_all"
    const val REPORT = "report/{k}"
    const val LOOK = "look"
    const val GUIDE = "guide"
    const val TABLE = "table/{id}"
    const val SALES = "sales"
    const val TAILOR = "tailor"
    const val TAILOR_NEW = "tailor_new"
    const val TAILOR_ORDER = "tailor_order/{id}"
    const val TAILOR_WORKERS = "tailor_workers"
    const val GARMENTS = "garments"
    const val GARMENT_STYLE = "garment_style/{id}"
    const val GARMENT_ORDER_NEW = "garment_order_new"
    const val GARMENT_ORDER = "garment_order/{id}"

    fun tailorOrder(id: Long) = "tailor_order/$id"
    fun garmentStyle(id: Long) = "garment_style/$id"
    fun garmentOrder(id: Long) = "garment_order/$id"

    fun productEdit(id: Long) = "product/$id"
    fun ledger(m: com.bongshaltech.erpinventory.data.db.PayMethod) = "ledger/${m.name}"
    fun loan(id: Long) = "loan/$id"
    fun report(kind: String) = "report/$kind"
    fun table(id: Long) = "table/$id"
    fun warehouse(id: Long) = "warehouse/$id"
    fun saleReturn(saleId: Long) = "sale_return/$saleId"
    fun order(kind: com.bongshaltech.erpinventory.data.db.OrderKind, id: Long) = "order/${kind.name}/$id"
    fun purchaseReturn(purchaseId: Long) = "purchase_return/$purchaseId"
    fun customer(id: Long) = "customer/$id"
    fun customerEdit(id: Long) = "customer_edit/$id"
    fun supplier(id: Long) = "supplier/$id"
    fun supplierEdit(id: Long) = "supplier_edit/$id"
    fun mfsEntry(walletId: Long) = "mfs_entry/$walletId"
    fun receipt(id: Long) = "receipt/$id"
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
        composable(Routes.SALE) { SaleScreen(go = { nav.navigate(it) }, onBack = back) }
        composable(Routes.SALE_SCAN) { SaleScreen(go = { nav.navigate(it) }, onBack = back, startScan = true) }
        composable(Routes.RECEIPT, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
            ReceiptScreen(e.arguments?.getLong("id") ?: 0L, go = { nav.navigate(it) }, onBack = back)
        }
        composable(Routes.PRINTER) { PrinterScreen(back) }
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
        composable(Routes.BACKUP) { BackupScreen(back) }
        composable(Routes.SMS_GATEWAY) { SmsGatewayScreen(back) }
        composable(Routes.COURIER) { CourierScreen(back) }
        composable(Routes.ORDERS) { OrdersScreen(go = { nav.navigate(it) }, onBack = back) }
        composable(
            Routes.ORDER,
            arguments = listOf(navArgument("kind") { type = NavType.StringType }, navArgument("id") { type = NavType.LongType }),
        ) { e ->
            val kind = runCatching { com.bongshaltech.erpinventory.data.db.OrderKind.valueOf(e.arguments?.getString("kind").orEmpty()) }
                .getOrDefault(com.bongshaltech.erpinventory.data.db.OrderKind.QUOTE)
            OrderScreen(kind, e.arguments?.getLong("id") ?: 0L, go = { nav.navigate(it) }, onBack = back)
        }
        composable(Routes.CASH_BANK) { CashBankScreen(go = { nav.navigate(it) }, onBack = back) }
        composable(Routes.LEDGER, arguments = listOf(navArgument("m") { type = NavType.StringType })) { e ->
            val m = runCatching { com.bongshaltech.erpinventory.data.db.PayMethod.valueOf(e.arguments?.getString("m").orEmpty()) }
                .getOrDefault(com.bongshaltech.erpinventory.data.db.PayMethod.CASH)
            LedgerScreen(m, back)
        }
        composable(Routes.LOANS) { LoansScreen(go = { nav.navigate(it) }, onBack = back) }
        composable(Routes.LOAN, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e -> LoanScreen(e.arguments?.getLong("id") ?: 0L, back) }
        composable(Routes.CASH_DAY) { CashDayScreen(back) }
        composable(Routes.PROMOS) { PromosScreen(back) }
        composable(Routes.HR) { HrScreen(back) }
        composable(Routes.BATCHES) { BatchesScreen(back) }
        composable(Routes.REPACK) { RepackScreen(back) }
        composable(Routes.IMPORT) { ImportScreen(back) }
        composable(Routes.LABELS) { LabelsScreen(back) }
        composable(Routes.AUDIT) { AuditScreen(back) }
        composable(Routes.FEATURES) { FeaturesScreen(back) }
        composable(Routes.STOCK_ADJUST) { StockAdjustScreen(back) }
        composable(Routes.REORDER) { ReorderScreen(back) }
        composable(Routes.ACCOUNTS) { AccountsScreen(go = { nav.navigate(it) }, onBack = back) }
        composable(Routes.JOURNAL) { JournalScreen(back) }
        composable(Routes.RECURRING) { RecurringScreen(back) }
        composable(Routes.INVESTORS) { InvestorsScreen(back) }
        composable(Routes.RECIPES) { RecipesScreen(back) }
        composable(Routes.SALES) { SalesListScreen(go = { nav.navigate(it) }, onBack = back) }
        composable(Routes.TAILOR) { TailorScreen(go = { nav.navigate(it) }, onBack = back) }
        composable(Routes.TAILOR_NEW) { TailorNewScreen(go = { nav.navigate(it) }, onBack = back) }
        composable(Routes.TAILOR_ORDER, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
            TailorOrderScreen(e.arguments?.getLong("id") ?: 0L, go = { nav.navigate(it) }, onBack = back)
        }
        composable(Routes.TAILOR_WORKERS) { TailorWorkersScreen(back) }
        composable(Routes.GARMENTS) { GarmentsScreen(go = { nav.navigate(it) }, onBack = back) }
        composable(Routes.GARMENT_STYLE, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
            GarmentStyleScreen(e.arguments?.getLong("id") ?: 0L, back)
        }
        composable(Routes.GARMENT_ORDER_NEW) { GarmentOrderNewScreen(go = { nav.navigate(it) }, onBack = back) }
        composable(Routes.GARMENT_ORDER, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
            GarmentOrderScreen(e.arguments?.getLong("id") ?: 0L, back)
        }
        composable(Routes.FACTORY) { WorkOrdersScreen(false, back) }
        composable(Routes.REPAIR) { WorkOrdersScreen(true, back) }
        composable(Routes.KITCHEN) { KitchenScreen(back) }
        composable(Routes.REST_REPORT) { RestaurantReportScreen(back) }
        composable(Routes.PHARMA_REPORT) { PharmacyReportScreen(back) }
        composable(Routes.LOOK) { ProfileLookScreen(back) }
        composable(Routes.GUIDE) { GuideScreen(back) }
        composable(Routes.REPORT_LIST) { ReportListScreen(go = { nav.navigate(it) }, onBack = back) }
        composable(Routes.REPORT) { e -> ReportViewScreen(e.arguments?.getString("k").orEmpty(), back) }
        composable(Routes.DELETED_SALES) { DeletedSalesScreen(go = { nav.navigate(it) }, onBack = back) }
        composable(Routes.SERIALS) { SerialsScreen(back) }
        composable(Routes.WAREHOUSES) { WarehousesScreen(go = { nav.navigate(it) }, onBack = back) }
        composable(Routes.WAREHOUSE, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e -> WarehouseScreen(e.arguments?.getLong("id") ?: 0L, back) }
        composable(Routes.TABLES) { TablesScreen(go = { nav.navigate(it) }, onBack = back) }
        composable(Routes.TABLE, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
            TableScreen(e.arguments?.getLong("id") ?: 0L, go = { nav.navigate(it) }, onBack = back)
        }
        composable(Routes.RETURNS) { ReturnsScreen(go = { nav.navigate(it) }, onBack = back) }
        composable(Routes.SALE_RETURN, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
            ReturnScreen(purchase = false, docId = e.arguments?.getLong("id") ?: 0L, onBack = back)
        }
        composable(Routes.PURCHASE_RETURN, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
            ReturnScreen(purchase = true, docId = e.arguments?.getLong("id") ?: 0L, onBack = back)
        }
        composable(Routes.AD_FREE) { AdFreeScreen(back) }
        composable(Routes.REPORTS) { ReportsScreen(tab = { nav.goTab(it) }, go = { nav.navigate(it) }) }
    }
}
