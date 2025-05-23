package com.example.fixv_demo

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAppointmentScreen(navController: NavController, appointment: Appointment) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    // State for date picker
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("MM/dd/yyyy", Locale.US) }
    var selectedDate by remember { mutableStateOf(parseDate(appointment.date, dateFormatter)) }

    // State for time picker
    var showTimePicker by remember { mutableStateOf(false) }
    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.US) }
    var selectedTime by remember { mutableStateOf(parseTime(appointment.time, timeFormatter)) }

    var status by remember { mutableStateOf(appointment.statusConfirmed) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Date picker
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate?.time)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedDate = Date(it)
                        }
                        showDatePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) {
                    Text("OK", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false }
                ) {
                    Text("Cancel", color = Color.Black)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time picker
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedTime?.hours ?: 12,
            initialMinute = selectedTime?.minutes ?: 0,
            is24Hour = false
        )
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                selectedTime = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }.time
                showTimePicker = false
            },
            timePickerState.hour,
            timePickerState.minute,
            false
        ).show()
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { snackbarData ->
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
        }
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
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    text = "Edit Appointment",
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

            // Date Picker Field
            OutlinedTextField(
                value = selectedDate?.let { dateFormatter.format(it) } ?: "",
                onValueChange = {},
                label = { Text("Appointment Date") },
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Select Date"
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Time Picker Field
            OutlinedTextField(
                value = selectedTime?.let { timeFormatter.format(it) } ?: "",
                onValueChange = {},
                label = { Text("Appointment Time") },
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showTimePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Select Time"
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Status Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Status:",
                    modifier = Modifier.weight(1f),
                    fontSize = 16.sp
                )
                Button(
                    onClick = { status = !status },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (status) Color.Green else Color.LightGray
                    )
                ) {
                    Text(
                        text = if (status) "Confirmed" else "Pending",
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Display read-only information
            Text(
                text = "Shop: ${appointment.shopName}",
                modifier = Modifier.fillMaxWidth(),
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Vehicle: ${appointment.vehicleDetails}",
                modifier = Modifier.fillMaxWidth(),
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Services: ${appointment.serviceType.joinToString(", ")}",
                modifier = Modifier.fillMaxWidth(),
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Discard Button
                Button(
                    onClick = { navController.popBackStack() },
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

                        if (selectedDate == null || selectedTime == null) {
                            isSaving = false
                            errorMessage = "Please select both date and time"
                            return@Button
                        }

                        val updatedAppointmentData = mapOf(
                            "date" to selectedDate?.let { dateFormatter.format(it) },
                            "time" to selectedTime?.let { timeFormatter.format(it) },
                            "status" to mapOf("confirmed" to status)
                        )

                        db.collection("appointments").document(appointment.appointmentId)
                            .update(updatedAppointmentData)
                            .addOnSuccessListener {
                                isSaving = false
                                coroutineScope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Appointment updated successfully!",
                                        withDismissAction = true
                                    )
                                    if (result == SnackbarResult.Dismissed) {
                                        navController.navigateUp()
                                    }
                                }
                            }
                            .addOnFailureListener { e ->
                                isSaving = false
                                errorMessage = "Failed to update appointment: ${e.message}"
                            }
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    } else {
                        Text(text = "Save Changes", color = Color.White)
                    }
                }
            }

            errorMessage?.let { error ->
                Text(
                    text = error,
                    color = Color.Red,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

private fun parseDate(dateString: String, formatter: SimpleDateFormat): Date? {
    return try {
        formatter.parse(dateString)
    } catch (e: Exception) {
        null
    }
}

private fun parseTime(timeString: String, formatter: SimpleDateFormat): Date? {
    return try {
        formatter.parse(timeString)
    } catch (e: Exception) {
        null
    }
}