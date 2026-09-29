package com.example.study_mentor.ui


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.study_mentor.ui.theme.PrimaryBlue

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate("ask_ai") {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Ask AI",
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") { HomeScreen() }
            composable("practice") { PracticeScreen() }
            composable("ask_ai") { 
                CameraScannerScreen(onBackClick = { navController.popBackStack() }) 
            }
            composable("library") { PlaceholderScreen("Library Screen") }
            composable("profile") { PlaceholderScreen("Profile Screen") }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = currentRoute == "home",
            onClick = {
                if (currentRoute != "home") {
                    navController.navigate("home") {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryBlue,
                selectedTextColor = PrimaryBlue,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Create, contentDescription = "Practice") },
            label = { Text("Practice") },
            selected = currentRoute == "practice",
            onClick = {
                if (currentRoute != "practice") {
                    navController.navigate("practice") {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryBlue,
                selectedTextColor = PrimaryBlue,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = Color.Transparent
            )
        )
        // Center empty space for FAB
        NavigationBarItem(
            icon = { },
            label = { },
            selected = false,
            onClick = { },
            enabled = false,
            colors = NavigationBarItemDefaults.colors(
                disabledIconColor = Color.Transparent,
                disabledTextColor = Color.Transparent,
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Library") },
            label = { Text("Library") },
            selected = currentRoute == "library",
            onClick = {
                if (currentRoute != "library") {
                    navController.navigate("library") {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryBlue,
                selectedTextColor = PrimaryBlue,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile") },
            selected = currentRoute == "profile",
            onClick = {
                if (currentRoute != "profile") {
                    navController.navigate("profile") {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryBlue,
                selectedTextColor = PrimaryBlue,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = Color.Transparent
            )
        )
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium
        )
    }
}
