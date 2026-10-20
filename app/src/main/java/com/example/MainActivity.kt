package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.ui.theme.MyApplicationTheme

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.MainMenuScreen
import com.example.ui.PlaceholderScreen

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = "menu") {
          composable("menu") { MainMenuScreen(navController) }
          composable("mission_select") {
              val viewModel: com.example.viewmodel.GameViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
              com.example.ui.CombatScreen(navController, viewModel)
          }
          composable("base") {
              val viewModel: com.example.viewmodel.GameViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
              com.example.ui.BaseManagementScreen(viewModel)
          }
          composable("squad") {
              val viewModel: com.example.viewmodel.GameViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
              com.example.ui.SquadScreen(viewModel)
          }
          composable("armory") { PlaceholderScreen("Armory", navController) }
          composable("missions") { PlaceholderScreen("Missions", navController) }
          composable("settings") { PlaceholderScreen("Settings", navController) }
        }
      }
    }
  }
}
