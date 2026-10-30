package com.bongshaltech.erpinventory.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val STAFF_LOGIN = "staff_login"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val PRODUCTS = "products"
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

@Composable
fun ErpNavHost() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) { SplashScreen { nav.goRoot(it) } }
        composable(Routes.LOGIN) { LoginScreen { nav.goRoot(it) } }
        composable(Routes.STAFF_LOGIN) { StaffLoginScreen(onBack = { nav.popBackStack() }) { nav.goRoot(it) } }
        composable(Routes.ONBOARDING) { OnboardingScreen { nav.goRoot(it) } }
        composable(Routes.HOME) { HomeScreen { nav.navigate(it) } }
        composable(Routes.PRODUCTS) {
            ProductsScreen(onBack = { nav.popBackStack() }, onEdit = { nav.navigate(Routes.productEdit(it)) })
        }
        composable(Routes.PRODUCT_EDIT, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
            ProductEditScreen(e.arguments?.getLong("id") ?: 0L) { nav.popBackStack() }
        }
        composable(Routes.SALE) { SaleScreen { nav.popBackStack() } }
        composable(Routes.MORE) {
            MoreScreen(onBack = { nav.popBackStack() }, go = { nav.navigate(it) }, signedOut = { nav.goRoot(Routes.LOGIN) })
        }
        composable(Routes.BIZ) { BizProfileScreen { nav.popBackStack() } }
        composable(Routes.STAFF_MANAGE) { StaffManageScreen { nav.popBackStack() } }
        composable(Routes.SOON) { SoonScreen { nav.popBackStack() } }
    }
}
