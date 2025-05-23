package com.example.fixv_demo

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.google.gson.Gson

// Data class to hold appointment information
data class AppointmentDetails(
    val date: String,
    val time: String,
    val shopId: String,
    val shop: String,
    val services: List<String>,
    val totalCost: Double,
    val vehicleMake: String,
    val vehicleModel: String,
    val vehicleYear: String,
    val licensePlate: String,
    val servicePrices: Map<String, Double>,
    val vehicleId: String
){
    val vehicleDetails: String
        get() = "$vehicleMake $vehicleModel $vehicleYear $licensePlate $vehicleId"
}

@OptIn(UnstableApi::class)
@Composable
fun AppointmentConfirmationScreen(
    navController: NavController,
    appointmentDetailsJson: String
) {
    val appointmentDetails = remember {
        Gson().fromJson(appointmentDetailsJson, AppointmentDetails::class.java)
    }
    val selectedNavItem = remember { mutableStateOf(0) }
    Log.d("AppointmentDetails", "Appointment Details: $appointmentDetails")
    Log.d("TotalCost", "Total Cost: ${appointmentDetails.totalCost}")

    Scaffold(
        topBar = {
            TopAppBar(
                onLogoutClick = { },
                showLogoutButton = false
            )
        },
        bottomBar = {
            BottomNavBar(
                selectedItem = selectedNavItem.value,
                onItemSelected = { selectedNavItem.value = it },
                navController = navController
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
        ) {
            item {
                // Back button and title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        text = "Appointment\nConfirmation",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            item {
                // Summary section
                Text(
                    text = "Summary",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 16.dp)
                )
            }

            item {
                // Appointment details card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp, horizontal = 16.dp)
                    ) {
                        // Appointment details
                        Text(
                            text = "Date: ${appointmentDetails.date}",
                            fontSize = 18.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        Text(
                            text = "Time: ${appointmentDetails.time}",
                            fontSize = 18.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        // Vehicle details section
                        Text(
                            text = "Vehicle Details",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )

                        Text(
                            text = "Make: ${appointmentDetails.vehicleMake}",
                            fontSize = 18.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Text(
                            text = "Model: ${appointmentDetails.vehicleModel}",
                            fontSize = 18.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Text(
                            text = "Year: ${appointmentDetails.vehicleYear}",
                            fontSize = 18.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Text(
                            text = "License Plate: ${appointmentDetails.licensePlate}",
                            fontSize = 18.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        // Services section
                        Text(
                            text = "Services",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )

                        appointmentDetails.services.forEach { service ->
                            Text(
                                text = "• $service",
                                fontSize = 18.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        // Cost section
                        Text(
                            text = "Total Cost",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )

                        Text(
                            text = "$ ${String.format("%.2f", appointmentDetails.totalCost)} TTD",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            item {
                // Continue to Payment button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            val appointmentDetailsJson = Uri.encode(Gson().toJson(appointmentDetails))
                            navController.navigate("payment/$appointmentDetailsJson")
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black
                        ),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Continue to Payment",
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}


// Example of how to navigate to this screen from another screen
// In your appointment scheduling screen:
/*
navController.navigate(
    "appointmentConfirmation/${Uri.encode(Gson().toJson(
        AppointmentDetails(
            date = selectedDate,
            time = selectedTime,
            services = selectedServices,
            vehicleMake = vehicle.make,
            vehicleModel = vehicle.model,
            vehicleYear = vehicle.year,
            licensePlate = vehicle.licensePlate,
            totalCost = calculatedTotalCost
        )
    ))}"
)
*/

// And in your NavGraph:
/*
composable(
    route = "appointmentConfirmation/{appointmentDetails}",
    arguments = listOf(navArgument("appointmentDetails") { type = NavType.StringType })
) { backStackEntry ->
    val appointmentDetailsJson = backStackEntry.arguments?.getString("appointmentDetails") ?: ""
    val appointmentDetails = Gson().fromJson(appointmentDetailsJson, AppointmentDetails::class.java)

    AppointmentConfirmationScreen(
        navController = navController,
        appointmentDetails = appointmentDetails
    )
}
*/