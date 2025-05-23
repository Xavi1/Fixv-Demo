package com.example.fixv_demo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson

class MechanicShopDetails : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val shopId = intent.getStringExtra("shopId")

        setContent {
            val navController = rememberNavController()
            MechanicShopDetailsScreen(navController, shopId)
        }
    }
}

@Composable
fun MechanicShopDetailsScreen(
    navController: NavController,
    shopId: String?,
    viewModel: MechanicShopViewModel = viewModel() // ViewModel is already passed here
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    val selectedShop by viewModel.selectedShop.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(shopId) {
        shopId?.let {
            viewModel.fetchShopDetails(it)
        }
    }

    if (showLogoutDialog) {
        LogoutDialog(
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                showLogoutDialog = false
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(context, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                context.startActivity(intent)
                (context as? ComponentActivity)?.finish()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                onLogoutClick = { showLogoutDialog = true },
                showLogoutButton = true
            )
        },
        bottomBar = {
            BottomNavBar(
                navController = navController,
                selectedItem = 0,
                onItemSelected = {}
            )
        },
        containerColor = Color.White
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                errorMessage != null -> Text(
                    text = errorMessage ?: "Unknown error occurred",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                selectedShop != null -> ShopDetailsContent(selectedShop, viewModel, navController) // Pass viewModel here
                else -> Text(
                    text = "Shop not found",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun ShopDetailsContent(
    selectedShop: Map<String, Any>?,
    viewModel: MechanicShopViewModel,
    navController: NavController // Add navController as a parameter
) {
    val services by viewModel.services.collectAsState()
    val openingHours by viewModel.openingHours.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            ShopHeader(selectedShop)
            Spacer(modifier = Modifier.height(16.dp))
            ShopInfoCard(
                title = "Location",
                content = "${selectedShop?.get("address")}",
                icon = Icons.Default.LocationOn
            )
            ShopInfoCard(
                title = "Opening Hours",
                content = formatOpeningHours(openingHours),
                icon = Icons.Default.Schedule
            )
            ShopInfoCard(
                title = "Contact",
                content = "${selectedShop?.get("phoneNumber")}\n${selectedShop?.get("email")}",
                icon = Icons.Default.Phone
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Services", style = MaterialTheme.typography.titleMedium)
        }

        items(services) { service ->
            ServiceButton(service)
        }
        Log.d("MechanicShopDetails", "Final services list: $services")
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    navController.navigate("ScheduleAppointment")
                },
                modifier = Modifier.width(213.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Schedule", color = Color.White)
            }
        }
    }
}

@Composable
fun ShopHeader(selectedShop: Map<String, Any>?) {
    Text(
        text = selectedShop?.get("name") as? String ?: "",
        style = MaterialTheme.typography.headlineLarge,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun ShopInfoCard(title: String, content: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, Color(0xFFB7AEAE), shape = RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = content, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun ServiceButton(service: String) {
    Button(
        onClick = { /* Handle service click */ },
        modifier = Modifier
            .wrapContentWidth()
            .padding(vertical = 4.dp)
            .border(1.dp, Color.Gray, shape = RoundedCornerShape(16.dp)),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(text = service, color = Color.Black)
    }
}

fun formatOpeningHours(openingHours: List<Map<String, String>>): String {
    return openingHours.joinToString("\n") { "${it["day"]}: ${it["hours"]}" }
}