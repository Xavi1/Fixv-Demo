package com.example.fixv_demo

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun TopAppBar(
    onLogoutClick: () -> Unit,
    showLogoutButton: Boolean = true
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp) // Reduced horizontal padding
            .padding(top = 8.dp) // Reduced top padding
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo
            Image(
                painter = painterResource(id = R.drawable.fixv_logo),
                contentDescription = "FIXV Logo",
                modifier = Modifier.size(100.dp) // Optionally reduce logo size
            )

            if (showLogoutButton) {
                IconButton(
                    onClick = onLogoutClick,
                    modifier = Modifier.size(40.dp) // Optionally reduce button size
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Logout",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp) // Optionally reduce icon size
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavBar(
    selectedItem: Int,
    onItemSelected: (Int) -> Unit,
    navController: NavController
) {
    val items = listOf(
        NavItem("home", customIcon = R.drawable.ion__home),
        NavItem("calendar", customIcon = R.drawable.simple_line_icons__calender),
        NavItem("profile", customIcon = R.drawable.ix_user_profile_filled)
    )

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    NavigationBar(containerColor = Color.White, contentColor = Color.Black) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = {
                    item.customIcon?.let {
                        Image(
                            painter = painterResource(id = it),
                            contentDescription = item.title,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                label = {},
                selected = selectedItem == index,
                onClick = {
                    if (currentRoute != item.title) { // ✅ Prevents redundant navigation
                        onItemSelected(index)
                        navController.navigate(item.title) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true } // ✅ Clears back stack
                            launchSingleTop = true // ✅ Avoids re-adding the same screen
                            restoreState = true
                        }
                    }
                }
                ,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Black,
                    unselectedIconColor = Color.Gray,
                    indicatorColor = Color.White
                )
            )
        }
    }
}


@Composable
fun LogoutDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Logout",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center // Center the title text
            )
        },
        text = {
            Text(
                text = "Are you sure you want to logout?",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center // Center the dialog text
            )
        },
        confirmButton = {
            // Use a Row to center the buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center, // Center the buttons horizontally
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dismiss Button
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    )
                ) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.width(16.dp)) // Add spacing between buttons

                // Confirm Button
                TextButton(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    )
                ) {
                    Text("Logout", color = Color.White)
                }
            }
        },
        dismissButton = {} // Empty dismissButton since we're handling it in the confirmButton Row
    )
}

data class NavItem(
    val title: String,
    val icon: ImageVector? = null,
    val customIcon: Int? = null
)