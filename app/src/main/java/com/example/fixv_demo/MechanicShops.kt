package com.example.fixv_demo
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class MechanicShopsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            MechanicShopsScreen(navController = navController)
        }
    }
}

fun fetchMechanicShops(onSuccess: (List<Map<String, Any>>) -> Unit, onFailure: (Exception) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    db.collection("mechanic_shops")
        .get()
        .addOnSuccessListener { result ->
            val shops = result.documents.map { document ->
                val data = document.data?.toMutableMap() ?: mutableMapOf()
                data["id"] = document.id // ✅ Store the Firestore document ID
                data
            }
            onSuccess(shops)
        }
        .addOnFailureListener { e ->
            onFailure(e)
        }
}

@Composable
fun MechanicShopsScreen(navController: NavController) {
    var mechanicShops by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedItem by remember { mutableIntStateOf(0) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Fetch mechanic shops from Firestore
    LaunchedEffect(Unit) {
        fetchMechanicShops(
            onSuccess = { shops ->
                mechanicShops = shops
                isLoading = false
            },
            onFailure = { e ->
                println("Error fetching mechanic shops: $e")
                isLoading = false
            }
        )
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
                selectedItem = selectedItem,
                onItemSelected = { selectedItem = it }
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = "Mechanic Shops",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp),
                textAlign = TextAlign.Center
            )

            // Loading Indicator
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                // Mechanic Shops List
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    mechanicShops.forEach { shop ->
                        MechanicShopItem(
                            name = shop["name"] as? String ?: "",
                            address = shop["address"] as? String ?: "",
                            phone = shop["phoneNumber"] as? String ?: "",
                            email = shop["email"] as? String ?: "",
                            openingHours = convertOpeningHours(shop["openingHours"]),
                            documentId = shop["id"] as? String ?: "",
                            navController = navController
                        )
                    }
                }
                }
        }
    }
}

@Composable
// Helper function to convert opening hours from Firestore format to our display format
fun convertOpeningHours(openingHoursData: Any?): List<Map<String, String>> {
    val result = mutableListOf<Map<String, String>>()

    when (openingHoursData) {
        // Handle case where openingHours is a Map (as in the Firestore structure)
        is Map<*, *> -> {
            openingHoursData.forEach { (day, data) ->
                when (data) {
                    is Map<*, *> -> {
                        val dayName = day.toString()
                        val closed = data["closed"] as? Boolean ?: false

                        if (closed) {
                            result.add(mapOf(
                                "day" to dayName,
                                "hours" to "Closed"
                            ))
                        } else {
                            val open = data["open"] as? String ?: ""
                            val close = data["close"] as? String ?: ""
                            result.add(mapOf(
                                "day" to dayName,
                                "hours" to "$open - $close"
                            ))
                        }
                    }
                    // Handle case where the value is just a string
                    is String -> {
                        result.add(mapOf(
                            "day" to day.toString(),
                            "hours" to data
                        ))
                    }
                }
            }
        }
        // Handle case where openingHours is already a List<Map<String, String>>
        is List<*> -> {
            openingHoursData.forEach { item ->
                if (item is Map<*, *>) {
                    val dayMap = mutableMapOf<String, String>()
                    item.forEach { (key, value) ->
                        if (key is String && value is String) {
                            dayMap[key] = value
                        }
                    }
                    if (dayMap.isNotEmpty()) {
                        result.add(dayMap)
                    }
                }
            }
        }
    }

    return result
}

@Composable
fun MechanicShopItem(
    name: String,
    address: String,
    phone: String,
    email: String,
    openingHours: List<Map<String, String>>,
    documentId: String,
    navController: NavController
) {
    var showOpeningHours by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                Log.d("Navigation", "Attempting to navigate with ID in mechanic shops:$documentId")
                navController.navigate("mechanic_shops/$documentId")
            },
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = cardColors(containerColor = Color.White),
        border = BorderStroke(0.7.dp, Color.Gray),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Shop Name
            Text(
                text = name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Address
            Text(
                text = address,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 4.dp),
                color = Color(0xFF9A9494)
            )

            // Phone
            Text(
                text = phone,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 4.dp),
                color = Color(0xFF9A9494)
            )

            // Email
            Text(
                text = email,
                fontSize = 14.sp,
                color = Color(0xFF9A9494)
            )

            // Opening Hours Section
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Opening Hours",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                IconButton(
                    onClick = { showOpeningHours = !showOpeningHours },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (showOpeningHours) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle Opening Hours"
                    )
                }
            }

            // Display Opening Hours
            if (showOpeningHours) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    openingHours.forEach { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = entry["day"] ?: "", fontSize = 14.sp)
                            Text(text = entry["hours"] ?: "", fontSize = 14.sp, color = Color(0xFF9A9494))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}