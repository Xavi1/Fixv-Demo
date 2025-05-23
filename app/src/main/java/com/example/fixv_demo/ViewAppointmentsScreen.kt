package com.example.fixv_demo

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


data class Appointment(
    val serviceType: List<String>,
    val iconResource: Int,
    val shopName: String,
    val date: String,
    val time: String,
    val vehicleDetails: String,
    val licensePlate: String,
    val invoiceId: String,
    val statusConfirmed: Boolean = false,
    val appointmentId: String = ""
)

@Composable
fun AppointmentsScreen(
    navController: NavController,
    onLogoutClick: () -> Unit
) {
    var selectedNavItem by remember { mutableStateOf(1) } // Calendar selected by default
    var showLogoutDialog by remember { mutableStateOf(false) }
    val appointments = remember { mutableStateListOf<Appointment>() }

    // Initialize Firestore
    val db: FirebaseFirestore = Firebase.firestore

    // Fetch appointments from Firestore
    LaunchedEffect(Unit) {
        db.collection("appointments")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    try {
                        val shopRef = document.get("shopId") as? DocumentReference
                        val vehicleRef = document.get("vehicleId") as? DocumentReference
                        val transactionRef = document.get("transactionId") as? DocumentReference

                        if (shopRef == null || vehicleRef == null || transactionRef == null) {
                            Log.e("AppointmentsScreen", "Missing shop, vehicle, or transaction reference in document: ${document.id}")
                            continue // Skip this document if references are missing
                        }

                        // Fetch shop and vehicle details
                        shopRef.get().addOnSuccessListener { shopDoc ->
                            val shopName = shopDoc.getString("name") ?: "Unknown Shop"

                            vehicleRef.get().addOnSuccessListener { vehicleDoc ->
                                val make = vehicleDoc.getString("make") ?: "Unknown"
                                val model = vehicleDoc.getString("model") ?: "Unknown"
                                val year = vehicleDoc.getLong("year")?.toString() ?: "Unknown"
                                val licensePlate = vehicleDoc.getString("licensePlate") ?: "Unknown"

                                val vehicleDetails = "$year $make $model $licensePlate"
                                Log.d("AppointmentsScreen", "vehicle details fetched: $vehicleDetails")

                                // Fetch service names (if any)
                                val serviceRefs = document.get("services") as? List<DocumentReference> ?: emptyList()
                                val serviceNames = mutableListOf<String>()

                                if (serviceRefs.isEmpty()) {
                                    // Fetch transaction details
                                    transactionRef.get().addOnSuccessListener { transactionDoc ->
                                        val invoiceRef = transactionDoc.get("invoiceId") as? DocumentReference
                                        if (invoiceRef != null) {
                                            invoiceRef.get().addOnSuccessListener { invoiceDoc ->
                                                val invoiceId = invoiceDoc.id // Assuming you need the document ID as the invoice ID

                                                val appointment = Appointment(
                                                    appointmentId = document.id,
                                                    serviceType = emptyList(),
                                                    iconResource = R.drawable.ic_default_service,
                                                    shopName = shopName,
                                                    date = document.getString("date") ?: "Unknown Date",
                                                    time = document.getString("time") ?: "Unknown Time",
                                                    vehicleDetails = vehicleDetails,
                                                    licensePlate = licensePlate,
                                                    invoiceId = invoiceId,
                                                    statusConfirmed = document.getBoolean("status.confirmed") ?: false,
                                                )
                                                appointments.add(appointment)
                                            }.addOnFailureListener { e ->
                                                Log.e("AppointmentsScreen", "Error fetching invoice", e)
                                            }
                                        } else {
                                            Log.e("AppointmentsScreen", "Missing invoice reference in transaction document: ${transactionDoc.id}")
                                        }
                                    }.addOnFailureListener { e ->
                                        Log.e("AppointmentsScreen", "Error fetching transaction", e)
                                    }
                                } else {
                                    for (serviceRef in serviceRefs) {
                                        serviceRef.get().addOnSuccessListener { serviceDoc ->
                                            val serviceName = serviceDoc.getString("serviceName")
                                            if (serviceName != null) {
                                                serviceNames.add(serviceName)
                                            }

                                            if (serviceNames.size == serviceRefs.size) {
                                                // Fetch transaction details
                                                transactionRef.get().addOnSuccessListener { transactionDoc ->
                                                    val invoiceRef = transactionDoc.get("invoiceId") as? DocumentReference
                                                    if (invoiceRef != null) {
                                                        invoiceRef.get().addOnSuccessListener { invoiceDoc ->
                                                            val invoiceId = invoiceDoc.id // Assuming you need the document ID as the invoice ID

                                                            val appointment = Appointment(
                                                                serviceType = serviceNames,
                                                                iconResource = getServiceIcon(serviceNames.firstOrNull() ?: ""),
                                                                shopName = shopName,
                                                                date = document.getString("date") ?: "Unknown Date",
                                                                time = document.getString("time") ?: "Unknown Time",
                                                                vehicleDetails = vehicleDetails,
                                                                licensePlate = licensePlate,
                                                                invoiceId = invoiceId
                                                            )
                                                            appointments.add(appointment)
                                                            Log.d("AppointmentsScreen", "Added appointment for $vehicleDetails ($licensePlate)")
                                                        }.addOnFailureListener { e ->
                                                            Log.e("AppointmentsScreen", "Error fetching invoice", e)
                                                        }
                                                    } else {
                                                        Log.e("AppointmentsScreen", "Missing invoice reference in transaction document: ${transactionDoc.id}")
                                                    }
                                                }.addOnFailureListener { e ->
                                                    Log.e("AppointmentsScreen", "Error fetching transaction", e)
                                                }
                                            }
                                        }.addOnFailureListener { e ->
                                            Log.e("AppointmentsScreen", "Error fetching service", e)
                                        }
                                    }
                                }
                            }.addOnFailureListener { e ->
                                Log.e("AppointmentsScreen", "Error fetching vehicle", e)
                            }
                        }.addOnFailureListener { e ->
                            Log.e("AppointmentsScreen", "Error fetching shop", e)
                        }
                    } catch (e: Exception) {
                        Log.e("AppointmentsScreen", "Error processing document", e)
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("AppointmentsScreen", "Error getting appointments", exception)
            }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top app bar with logo and logout button
            TopAppBar(
                onLogoutClick = { showLogoutDialog = true },
                showLogoutButton = true
            )

            // Title
            Text(
                text = "Upcoming",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )

            Text(
                text = "Appointments",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )

            // Appointments list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(appointments) { appointment ->
                    AppointmentCard(appointment, navController)
                }
            }

            // Bottom navigation
            BottomNavBar(
                selectedItem = selectedNavItem,
                onItemSelected = { selectedNavItem = it },
                navController = navController
            )
        }

        // Logout confirmation dialog
        if (showLogoutDialog) {
            LogoutDialog(
                onDismiss = { showLogoutDialog = false },
                onConfirm = {
                    showLogoutDialog = false
                    onLogoutClick()
                }
            )
        }
    }
}

// Mapping of service types to icons
val serviceIcons = mapOf(
    "Brake Repair" to R.drawable.ic_brake_repair,
    "Engine Check" to R.drawable.ic_engine_check,
    "Oil Change" to R.drawable.ic_oil_change,
    "Tire Rotation" to R.drawable.ic_tire_rotation
)

// Helper function to get the appropriate icon resource for a service
fun getServiceIcon(serviceType: String): Int {
    return serviceIcons[serviceType] ?: R.drawable.ic_default_service
}

@Composable
fun AppointmentCard(appointment: Appointment, navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .border(
                width = 1.dp,
                color = Color.Black,
                shape = RoundedCornerShape(32.dp)
            )
            .clickable {
                val serviceDetails = appointment.serviceType.joinToString(",")
                // URL encode the parameters to handle special characters
                val encodedVehicleDetails = java.net.URLEncoder.encode(appointment.vehicleDetails, "UTF-8")
                val encodedServiceDetails = java.net.URLEncoder.encode(serviceDetails, "UTF-8")
                val encodedLicensePlate = java.net.URLEncoder.encode(appointment.licensePlate, "UTF-8")
                val encodedInvoiceId = java.net.URLEncoder.encode(appointment.invoiceId, "UTF-8")

                navController.navigate("appointmentDetails/${appointment.shopName}/${appointment.date}/${appointment.time}/${encodedVehicleDetails}/${encodedServiceDetails}/${encodedLicensePlate}/${encodedInvoiceId}")
                Log.d("Navigation", "Navigated to appointmentDetails with invoice: ${appointment.invoiceId}")
            }
    ) {
        // Leave the UI display as-is - we're just adding the invoice ID to the navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Service icons
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                appointment.serviceType.forEach { serviceType ->
                    val iconResource = serviceIcons[serviceType] ?: R.drawable.ic_default_service
                    Image(
                        painter = painterResource(id = iconResource),
                        contentDescription = serviceType,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Service details
            Column(
                modifier = Modifier.weight(5f) // Adjusted weight to take more space
            ) {
                Text(
                    text = appointment.serviceType.joinToString(", "),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Text(
                    text = appointment.shopName,
                    fontSize = 16.sp,
                    color = Color.Gray
                )

                Text(
                    text = "${appointment.date} | ${appointment.time}",
                    fontSize = 16.sp,
                    color = Color.Gray
                )

                Text(
                    text = "${appointment.vehicleDetails} (${appointment.licensePlate})",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }

            // Arrow icon
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "View Details",
                tint = Color.Black
            )
        }
    }
}