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

    fun productEdit(id: Long) = "product/$id"
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
    }
}
