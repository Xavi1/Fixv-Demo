package com.example.fixv_demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@Composable
fun EditVehicleScreen(navController: NavController, vehicle: Vehicle) {
    val db = FirebaseFirestore.getInstance() // Firestore instance

    var name by remember { mutableStateOf<String>(vehicle.make) }
    var model by remember { mutableStateOf<String>(vehicle.model) }
    var mileage by remember { mutableStateOf<String>(vehicle.mileage.toString()) }
    var year by remember { mutableStateOf<String>(vehicle.year.toString()) }
    var plateNumber by remember { mutableStateOf<String>(vehicle.plateNumber) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { snackbarData: SnackbarData ->
                Snackbar(
                    snackbarData = snackbarData,
                    modifier = Modifier.padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    containerColor = Color.Black,
                    contentColor = Color.White,
                    actionColor = Color.White,
                    actionContentColor = Color.White,
                )
            }
        },
        containerColor = Color.White

    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Back Button and Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    text = "Edit Vehicle",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(90.dp))

            val textFieldColors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.LightGray,
                focusedBorderColor = Color.Black,
                cursorColor = Color.Black,
                focusedLabelColor = Color.Black,
                unfocusedLabelColor = Color.Gray
            )

            // Editable Fields
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Enter Vehicle Name:") },
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("Enter Vehicle Model:") },
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = mileage,
                onValueChange = { mileage = it },
                label = { Text("Enter Vehicle Mileage:") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = year,
                onValueChange = { year = it },
                label = { Text("Enter Vehicle Year:") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = plateNumber,
                onValueChange = { plateNumber = it },
                label = { Text("Enter Plate Number (ABC 1234):") },
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                // Discard Button
                Button(
                    onClick = {
                        navController.popBackStack() // Discard changes and navigate back
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) {
                    Text(text = "Discard", color = Color.White)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Submit Button
                Button(
                    onClick = {
                        isSaving = true
                        errorMessage = null

                        // Validation checks
                        if (name.isEmpty() || model.isEmpty() || mileage.isEmpty() || year.isEmpty() || plateNumber.isEmpty()) {
                            isSaving = false
                            errorMessage = "All fields must be filled in."
                            return@Button
                        } else if (plateNumber.length != 8 || !plateNumber.matches(Regex("^[A-Z]{3} \\d{4}\$"))) {
                            isSaving = false
                            errorMessage = "Plate number must be in the format ABC 1234."
                            return@Button
                        }

                        // Create update data map
                        val updatedVehicleData = mutableMapOf<String, Any?>(
                            "make" to name,
                            "model" to model,
                            "mileage" to mileage.toIntOrNull(),
                            "year" to year.toIntOrNull(),
                            "licensePlate" to plateNumber
                        )

                        // Firestore update
                        mileage.toIntOrNull()?.let { updatedVehicleData["mileage"] = it }
                        year.toIntOrNull()?.let { updatedVehicleData["year"] = it }

                        db.collection("Vehicles").document(vehicle.id)
                            .update(updatedVehicleData.filterValues { it != null })
                            .addOnSuccessListener {
                                isSaving = false
                                coroutineScope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Vehicle edited successfully!",
                                        duration = androidx.compose.material3.SnackbarDuration.Short,
                                        withDismissAction = true
                                    )
                                    if (result == SnackbarResult.Dismissed) {
                                        navController.navigateUp()
                                    }
                                }
                            }
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.Black
                        )
                    } else {
                        Text(text = "Save Changes", color = Color.White)
                    }
                }

                // Show error message
                errorMessage?.let { error ->
                    Text(text = error, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}