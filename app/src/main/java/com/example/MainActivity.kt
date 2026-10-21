package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.ArmoryScreen
import com.example.ui.BaseManagementScreen
import com.example.ui.CombatScreen
import com.example.ui.MainMenuScreen
import com.example.ui.MissionsScreen
import com.example.ui.SettingsScreen
import com.example.ui.SquadScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val navController = rememberNavController()
        val viewModel: GameViewModel = viewModel()

        NavHost(navController = navController, startDestination = "menu") {
          composable("menu") {
            MainMenuScreen(navController, viewModel)
          }
          composable("mission_select") {
            CombatScreen(navController, viewModel, missionId = 1)
          }
          composable(
            route = "combat/{missionId}",
            arguments = listOf(navArgument("missionId") { type = NavType.IntType; defaultValue = 1 })
          ) { backStackEntry ->
            val missionId = backStackEntry.arguments?.getInt("missionId") ?: 1
            CombatScreen(navController, viewModel, missionId = missionId)
          }
          composable("base") {
            BaseManagementScreen(navController, viewModel)
          }
          composable("squad") {
            SquadScreen(navController, viewModel)
          }
          composable("armory") {
            ArmoryScreen(navController, viewModel)
          }
          composable("missions") {
            MissionsScreen(navController, viewModel)
          }
          composable("settings") {
            SettingsScreen(navController)
          }
        }
      }
    }
  }
}
