package com.example.fixv_demo

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.gson.Gson
import java.net.URLEncoder

@Composable
fun EditVehicleListScreen(navController: NavController) {
    var vehicles by remember { mutableStateOf<List<Vehicle>>(emptyList()) }
    val db = Firebase.firestore
    val userId = Firebase.auth.currentUser?.uid

    // Fetch vehicles data when the component is first rendered
    LaunchedEffect(userId) {
        if (userId != null) {
            db.collection("Vehicles")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener { documents ->
                    val vehicleList = documents.mapNotNull { document ->
                        Vehicle(
                            id = document.id,
                            model = document.getString("model") ?: "",
                            plateNumber = document.getString("licensePlate") ?: "",
                            make = document.getString("make") ?: "",
                            year = document.getLong("year")?.toInt() ?: 0,
                            mileage = document.getLong("mileage")?.toInt() ?: 0
                        )
                    }
                    vehicles = vehicleList
                }
                .addOnFailureListener { exception ->
                    Log.e("EditVehicleListScreen", "Error fetching vehicles", exception)
                }
        }
    }

    Scaffold(
        topBar = { TopAppBar(onLogoutClick = {}, showLogoutButton = false) },
        bottomBar = { BottomNavBar(selectedItem = 0, onItemSelected = {}, navController = navController) },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                IconButton(
                    onClick = { navController.popBackStack()  },
                    modifier = Modifier
                        .size(56.dp)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(32.dp))
                }

                // Title
                Box(modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                    contentAlignment = Alignment.Center) {
                    Text(
                        text = "Vehicles",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.size(56.dp))
            }
            Spacer(modifier = Modifier.height(90.dp))

            // List of vehicles
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                vehicles.forEach { vehicle ->
                    VehicleItem(
                        vehicle = "${vehicle.year} ${vehicle.make} ${vehicle.model}",
                        plateNumber = vehicle.plateNumber,
                        onRemoveClick = {
                            // Remove vehicle from Firestore
                            db.collection("Vehicles").document(vehicle.id)
                                .delete()
                                .addOnSuccessListener {
                                    // Update local state after successful deletion
                                    vehicles = vehicles.filter { it.id != vehicle.id }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("EditVehicleScreen", "Error removing vehicle", e)
                                }
                        },
                        onEditClick = {
                            // Navigate to EditVehicleScreen with the selected vehicle's ID
                            val vehicleJson = URLEncoder.encode(Gson().toJson(vehicle), "UTF-8")
                            navController.navigate("editVehicle/${vehicleJson}")
                        }
                    )
                }
            }


            Spacer(modifier = Modifier.weight(1f))

            // Add vehicle button
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Button(
                    onClick = { navController.navigate("addvehiclescreen") },
                    modifier = Modifier
                        .width(200.dp)
                        .padding(vertical = 16.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) {
                    Text(text = "Add Vehicle", color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun VehicleItem(vehicle: String, plateNumber: String, onRemoveClick: () -> Unit, onEditClick: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) } // State to control dialog visibility
    var isLoading by remember { mutableStateOf(false) } // Track loading state

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.Gray)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ){
            Column {
                Text(
                    text = vehicle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Plate Number: $plateNumber",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            //Edit and Remove buttons container
            Row(modifier = Modifier.padding(end = 16.dp)) {
                // Edit button
                TextButton(onClick = {
                    isLoading = true
                    Log.d("EditVehicle", "Navigating to edit with vehicle: ${Gson().toJson(vehicle)}")
                    onEditClick()
                     },enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(text = "Edit", color = Color.Black)
                    }
                }

                // Remove Button
                IconButton(onClick = { showDialog = true }) {
                    Icon(
                        imageVector = Icons.Filled.RemoveCircle,
                        contentDescription = "Remove Vehicle",
                        tint = Color.Red
                    )
                }
            }
        }
    }

    // Confirmation Dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false }, // Close dialog when dismissed
            title = { Text(text = "Remove Vehicle") },
            text = { Text("Are you sure you want to remove this vehicle?") },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveClick()
                        showDialog = false
                    }
                , colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false } // Close the dialog without removing
                ) {
                    Text("Cancel", color = Color.Black)
                }
            }
        )
    }
}