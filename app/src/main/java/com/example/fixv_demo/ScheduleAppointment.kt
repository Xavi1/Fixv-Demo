package com.example.fixv_demo

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import java.util.Calendar
import java.util.Locale

class ScheduleAppointment : ComponentActivity() {
    // Empty class body
}

@Composable
fun ConfirmationDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Confirm Appointment") },
            text = { Text("Are you sure you want to submit this appointment?") },
            confirmButton = {
                Button(onClick = onConfirm) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                Button(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ScheduleAppointmentScreen(
    navController: NavController
) {
    // Existing state variables...
    var showLogoutDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableIntStateOf(0) }
    var selectedShop by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<Calendar?>(null) }
    var selectedTime by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var selectedService by remember { mutableStateOf("") }
    var selectedVehicle by remember { mutableStateOf("") }
    var serviceError by remember { mutableStateOf("") }
    var openingHours by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var selectedServices by remember { mutableStateOf(mutableStateListOf<String>()) }
    var closedDays by remember { mutableStateOf<List<String>>(emptyList()) }
    var totalCost by remember { mutableStateOf(0.0) }
    val context = LocalContext.current

    // Lists
    val shops = remember { mutableStateListOf<String>() }
    val services = remember { mutableStateListOf<String>() }
    val vehicles = remember { mutableStateListOf<String>() }
    val vehicleMap = remember { mutableStateMapOf<String, String>() }
    val servicePrices = remember { mutableStateMapOf<String, Double>() }

    // Fetch shops data
    LaunchedEffect(Unit) {
        val servicesCollection = FirebaseFirestore.getInstance().collection("services")
        val shopServicesCollection = FirebaseFirestore.getInstance().collection("shop_services")
        FirebaseFirestore.getInstance().collection("mechanic_shops")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val shopName = document.getString("name") ?: ""
                    shops.add(shopName)
                }
                Log.d("ScheduleAppointment", "Shops: $shops")
            }
            .addOnFailureListener { e ->
                Log.e("ScheduleAppointment", "Error fetching shops", e)
            }

        FirebaseFirestore.getInstance().collection("Vehicles")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val make = document.getString("make") ?: ""
                    val model = document.getString("model") ?: ""
                    val year = document.getLong("year")?.toString() ?: ""
                    val licensePlate = document.getString("licensePlate") ?: ""
                    val vehicleId = document.id

                    val displayName = "$make $model"
                    val vehicleDetails = "$make|$model|$year|$licensePlate|$vehicleId"
                    Log.d("VehicleData", "Retrieved: $displayName -> $vehicleDetails")

                    vehicles.add(displayName)
                    vehicleMap[displayName] = vehicleDetails
                    Log.d("VehicleMapping", "Added: $displayName -> $vehicleDetails")
                }
                Log.d("VehicleDebug", "All vehicle keys: ${vehicles.joinToString()}")
            }
            .addOnFailureListener { e ->
                Log.e("ScheduleAppointment", "Error fetching vehicles", e)
            }
    }

    LaunchedEffect(selectedShop) {
        if (selectedShop.isNotEmpty()) {
            servicePrices.clear()
            Log.d("ServicePrices", "Cleared servicePrices for new shop: $selectedShop")

            ScheduleAppointmentScreen.currentSelectedShop = selectedShop
            Log.d("SelectedShop", "Selected Shop: $selectedShop")

            // Fetch the shopId for the selected shop
            FirebaseFirestore.getInstance().collection("mechanic_shops")
                .whereEqualTo("name", selectedShop)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val shopDoc = documents.first()
                        val shopIdRef = shopDoc.reference
                        Log.d("SelectedShop", "Shop ID Reference: $shopIdRef")

                        fetchShopDetails(
                            selectedShop = selectedShop,
                            selectedDate = selectedDate,
                            services = services,
                            onOpeningHoursUpdated = { hours, closedDaysList ->
                                openingHours = hours
                                closedDays = closedDaysList
                                if (hours == null) {
                                    Toast.makeText(context, "Shop is closed on the selected day", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onError = { errorMessage ->
                                serviceError = errorMessage
                                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                            }
                        )

                        val servicesCollection = FirebaseFirestore.getInstance().collection("services")
                        val shopServicesCollection = FirebaseFirestore.getInstance().collection("shop_services")

                        servicesCollection.get()
                            .addOnSuccessListener { servicesDocuments ->
                                val serviceMap = mutableMapOf<String, String>()
                                for (serviceDoc in servicesDocuments) {
                                    val serviceId = serviceDoc.id
                                    val serviceName = serviceDoc.getString("serviceName") ?: "Unknown Service"
                                    serviceMap[serviceId] = serviceName
                                    Log.d("ServiceData", "Fetched Service: $serviceId -> $serviceName")
                                }

                                shopServicesCollection
                                    .whereEqualTo("shopId", shopIdRef)
                                    .get()
                                    .addOnSuccessListener { shopServicesDocuments ->
                                        Log.d("ShopServicesQuery", "Fetched ${shopServicesDocuments.size()} documents for shop $selectedShop")
                                        if (shopServicesDocuments.isEmpty) {
                                            Log.d("ShopServicesQuery", "No documents found for shopId: $shopIdRef")
                                        }
                                        for (shopServiceDoc in shopServicesDocuments) {
                                            Log.d("ShopServicesQuery", "Document: ${shopServiceDoc.data}")

                                            val serviceIdRef = shopServiceDoc.getDocumentReference("serviceId")
                                            val price = shopServiceDoc.getDouble("price") ?: 0.0
                                            val serviceId = serviceIdRef?.id

                                            if (serviceId != null && serviceMap.containsKey(serviceId)) {
                                                val serviceName = serviceMap[serviceId]!!
                                                if (serviceName == "brake repair") {
                                                    Log.d("ServicePrices", "Processing brake repair service: $serviceName -> $price")
                                                }
                                                if (selectedShop == ScheduleAppointmentScreen.currentSelectedShop) {
                                                    servicePrices[serviceName] = price
                                                    Log.d("ServicePrices", "Added: $serviceName -> $price for shop $selectedShop")
                                                }
                                            } else {
                                                Log.d("ServicePrices", "Service ID not found or not in serviceMap: $serviceId")
                                            }
                                        }
                                        Log.d("ServicePrices", "All Prices for $selectedShop: $servicePrices")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("ScheduleAppointment", "Error fetching shop service prices", e)
                                    }
                            }
                            .addOnFailureListener { e ->
                                Log.e("ScheduleAppointment", "Error fetching services", e)
                            }
                    } else {
                        Log.d("SelectedShop", "No shop found with name: $selectedShop")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ScheduleAppointment", "Error fetching shop ID", e)
                }
        } else {
            servicePrices.clear()
            Log.d("ServicePrices", "Cleared servicePrices because no shop is selected")
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
                selectedItem = selectedItem,
                onItemSelected = { selectedItem = it }
            )
        },
        containerColor = Color.White
    ) { padding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Schedule Appointment",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp),
                textAlign = TextAlign.Center
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.LightGray,
                        focusedBorderColor = Color.Black,
                        cursorColor = Color.Black,
                        focusedLabelColor = Color.Black,
                        unfocusedLabelColor = Color.Gray
                    )

                    DropdownMenuComponent(
                        label = "Shop",
                        options = shops,
                        selectedOptions = if (selectedShop.isEmpty()) emptyList() else listOf(selectedShop),
                        onOptionSelected = { selectedShop = it }
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    DatePickerField(
                        selectedDate = selectedDate?.let { formatDate(it) } ?: "",
                        onDateSelected = { calendar -> selectedDate = calendar },
                        enabled = selectedShop.isNotEmpty(),
                        closedDays = closedDays
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    TimePickerField(
                        selectedTime?.let { formatTime(it) } ?: "",
                        onTimeSelected = { hour, minute -> selectedTime = Pair(hour, minute) },
                        openingHours = openingHours, // Pass opening hours to TimePickerField
                        enabled = selectedShop.isNotEmpty() // Enable only if a shop is selected
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    DropdownMenuComponent(
                        label = "Service",
                        options = services,
                        selectedOptions = selectedServices,
                        onOptionSelected = { service ->
                            if (selectedServices.contains(service)) {
                                selectedServices.remove(service)
                            } else {
                                selectedServices.add(service)
                            }
                        },
                        onDropdownClick = {
                            if (selectedShop.isEmpty()) {
                                serviceError = "Please select a shop first"
                            }
                        },
                        showCheckIcon = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    DropdownMenuComponent(
                        label = "Vehicle",
                        options = vehicles,
                        selectedOptions = if (selectedVehicle.isEmpty()) emptyList() else listOf(selectedVehicle),
                        onOptionSelected = { selectedVehicle = it }
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            var isValid = true
                            if (selectedShop.isEmpty()) {
                                Toast.makeText(context, "Please select a shop", Toast.LENGTH_SHORT).show()
                                isValid = false
                            }
                            if (selectedDate == null) {
                                Toast.makeText(context, "Please select a date", Toast.LENGTH_SHORT).show()
                                isValid = false
                            } else {
                                val dayOfWeek = selectedDate!!.get(Calendar.DAY_OF_WEEK)
                                val closedDaysInt = closedDays.map { day ->
                                    when (day) {
                                        "Monday" -> Calendar.MONDAY
                                        "Tuesday" -> Calendar.TUESDAY
                                        "Wednesday" -> Calendar.WEDNESDAY
                                        "Thursday" -> Calendar.THURSDAY
                                        "Friday" -> Calendar.FRIDAY
                                        "Saturday" -> Calendar.SATURDAY
                                        else -> -1
                                    }
                                }

                                if (closedDaysInt.contains(dayOfWeek)) {
                                    Toast.makeText(context, "The shop is closed on the selected day", Toast.LENGTH_SHORT).show()
                                    isValid = false
                                }
                            }

                            if (selectedTime == null) {
                                Toast.makeText(context, "Please select a time", Toast.LENGTH_SHORT).show()
                                isValid = false
                            } else if (openingHours != null) {
                                val hour = selectedTime!!.first
                                val openHour = openingHours!!.first
                                val closeHour = openingHours!!.second

                                if (hour < openHour || hour >= closeHour) {
                                    Toast.makeText(
                                        context,
                                        "Selected time is outside shop hours (${openHour}:00-${closeHour}:00)",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    isValid = false
                                }
                            }

                            if (selectedServices.isEmpty()) {
                                Toast.makeText(context, "Please select at least one service", Toast.LENGTH_SHORT).show()
                                isValid = false
                            }
                            if (selectedVehicle.isEmpty()) {
                                Toast.makeText(context, "Please select a vehicle", Toast.LENGTH_SHORT).show()
                                isValid = false
                            }

                            if (isValid) {
                                val vehicleDetails = vehicleMap[selectedVehicle] ?: ""
                                val vehicleParts = vehicleDetails.split("|")
                                val vehicleMake = vehicleParts.getOrElse(0) { "" }
                                val vehicleModel = vehicleParts.getOrElse(1) { "" }
                                val vehicleYear = vehicleParts.getOrElse(2) { "" }
                                val licensePlate = vehicleParts.getOrElse(3) { "" }
                                val vehicleId = vehicleParts.getOrElse(4) { "" }

                                val calculatedTotalCost = selectedServices.sumOf { servicePrices[it] ?: 0.0 }
                                Log.d("ScheduleAppointment", "Calculated Total Cost on Button Click: $calculatedTotalCost")

                                // Fetch the shopId for the selected shop
                                FirebaseFirestore.getInstance().collection("mechanic_shops")
                                    .whereEqualTo("name", selectedShop)
                                    .get()
                                    .addOnSuccessListener { documents ->
                                        if (!documents.isEmpty) {
                                            val shopId = documents.first().id

                                            val appointmentDetails = AppointmentDetails(
                                                date = selectedDate?.let { formatDate(it) } ?: "",
                                                time = selectedTime?.let { formatTime(it) } ?: "",
                                                shopId = shopId,
                                                shop = selectedShop,
                                                services = selectedServices,
                                                totalCost = calculatedTotalCost,
                                                vehicleMake = vehicleMake,
                                                vehicleModel = vehicleModel,
                                                vehicleYear = vehicleYear,
                                                licensePlate = licensePlate,
                                                servicePrices = servicePrices.filterKeys { it in selectedServices },
                                                vehicleId = vehicleId
                                            )

                                            val appointmentDetailsJson = Uri.encode(Gson().toJson(appointmentDetails))
                                            Log.d("AppointmentData", "JSON: $appointmentDetailsJson")
                                            navController.navigate("appointmentConfirmation/$appointmentDetailsJson")
                                        } else {
                                            Toast.makeText(context, "Shop not found", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("ScheduleAppointment", "Error fetching shop ID", e)
                                        Toast.makeText(context, "Error fetching shop ID", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Submit", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun DatePickerField(
    selectedDate: String,
    onDateSelected: (Calendar) -> Unit,
    enabled: Boolean,
    closedDays: List<String>
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (enabled) Color.Black.copy(alpha = 0.8f) else Color.Gray,
                RoundedCornerShape(8.dp)
            )
            .clickable(enabled = enabled) {
                activity?.let {
                    val datePickerDialog = createThemedDatePickerDialog(
                        context = it,
                        closedDays = closedDays,
                        onDateSelected = { selectedCalendar ->
                            onDateSelected(selectedCalendar)
                        }
                    )

                    // Set minimum date to today
                    val calendar = Calendar.getInstance()
                    datePickerDialog.datePicker.minDate = calendar.timeInMillis
                    datePickerDialog.show()
                }
            }
            .padding(16.dp)
    ) {
        Text(
            text = if (selectedDate.isEmpty()) "Choose a date" else selectedDate,
            color = if (selectedDate.isEmpty()) Color.Gray else Color.Black,
            fontSize = 16.sp
        )
    }
}

// Add static field to track selected shop across composables
object ScheduleAppointmentScreen {
    var currentSelectedShop = ""
}

@Composable
fun TimePickerField(
    selectedTime: String,
    onTimeSelected: (Int, Int) -> Unit,
    openingHours: Pair<Int, Int>?,
    enabled: Boolean
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (enabled) Color.Black.copy(alpha = 0.8f) else Color.Gray,
                RoundedCornerShape(8.dp)
            )
            .clickable(enabled = enabled && openingHours != null) {
                if (openingHours == null) {
                    Toast.makeText(
                        context,
                        "Please select a date first to see available hours",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@clickable
                }

                val openHour = openingHours.first
                val closeHour = openingHours.second

                // Default to shop opening time for initial time selection
                val initialHour = openHour
                val initialMinute = 0

                val timePickerDialog = createThemedTimePickerDialog(
                    context = context,
                    openHour = openHour,
                    closeHour = closeHour,
                    initialHour = initialHour,
                    initialMinute = initialMinute,
                    onTimeSelected = onTimeSelected
                )

                // Custom dialog setup to inform user of available hours
                timePickerDialog.setTitle("Select Time (${openHour}:00 - ${closeHour}:00)")
                timePickerDialog.show()
            }
            .padding(16.dp)
    ) {
        Text(
            text = if (selectedTime.isEmpty()) {
                if (openingHours == null) {
                    "Choose a date first"
                } else {
                    "Choose a time (${openingHours.first}:00 - ${openingHours.second}:00)"
                }
            } else {
                selectedTime
            },
            color = if (selectedTime.isEmpty()) Color.Gray else Color.Black,
            fontSize = 16.sp
        )
    }
}
@Composable
fun DropdownMenuComponent(
    label: String,
    options: List<String>,
    selectedOptions: List<String>,
    onOptionSelected: (String) -> Unit,
    onDropdownClick: (() -> Unit)? = null,
    showCheckIcon: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(text = label, style = MaterialTheme.typography.titleMedium)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.LightGray, shape = RoundedCornerShape(16.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clickable {
                    expanded = true
                    onDropdownClick?.invoke()
                }
        ) {
            Text(
                text = if (selectedOptions.isEmpty()) "Select a $label" else selectedOptions.joinToString(", "),
                modifier = Modifier.align(Alignment.CenterStart)
            )
            IconButton(onClick = { expanded = true }, modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(20.dp)) {
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .width(350.dp)
                    .background(Color.White) // Set the background color to white
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        },
                        trailingIcon = {
                            if (showCheckIcon && selectedOptions.contains(option)) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        }
                    )
                }
            }
        }
    }
}

fun submitAppointment(shop: String, date: String, time: String, services: List<String>, vehicleDetails: String) {
    val appointment = hashMapOf(
        "shop" to shop,
        "date" to date,
        "time" to time,
        "services" to services,
        "vehicleDetails" to vehicleDetails
    )

    FirebaseFirestore.getInstance().collection("Appointments")
        .add(appointment)
        .addOnSuccessListener { /* Handle success */ }
        .addOnFailureListener { /* Handle failure */ }
}

fun formatDate(calendar: Calendar): String {
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val month = calendar.get(Calendar.MONTH) + 1 // Month is 0-indexed
    val year = calendar.get(Calendar.YEAR)
    return String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month, day)
}

fun formatTime(time: Pair<Int, Int>): String {
    val hour = time.first
    val minute = time.second
    return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
}

fun parseTime(time: String): Int {
    return try {
        val parts = time.split(":")
        if (parts.isNotEmpty()) {
            parts[0].toIntOrNull() ?: 9 // Default to 9 if parsing fails
        } else {
            9 // Default hour if format is unexpected
        }
    } catch (e: Exception) {
        9 // Default hour if any exception occurs
    }
}

fun fetchShopDetails(
    selectedShop: String,
    selectedDate: Calendar?,
    services: MutableList<String>,
    onOpeningHoursUpdated: (Pair<Int, Int>?, List<String>) -> Unit,
    onError: (String) -> Unit
) {
    FirebaseFirestore.getInstance().collection("mechanic_shops")
        .whereEqualTo("name", selectedShop)
        .get()
        .addOnSuccessListener { documents ->
            if (!documents.isEmpty) {
                val doc = documents.first()

                // Clear existing services list
                services.clear()

                // Get the services collection reference
                val servicesCollection = FirebaseFirestore.getInstance().collection("services")

                // Handle services offered
                val servicesList = doc.get("servicesOffered")

                when (servicesList) {
                    // Handle case where servicesOffered is a List
                    is List<*> -> {
                        if (servicesList.isEmpty()) {
                            processOpeningHours(doc, selectedDate, services, onOpeningHoursUpdated, onError)
                            return@addOnSuccessListener
                        }

                        var completedFetches = 0

                        // For debugging
                        Log.d("FetchShopDetails", "ServicesList size: ${servicesList.size}, type: ${servicesList.javaClass.name}")

                        servicesList.forEach { item ->
                            Log.d("FetchShopDetails", "Service item type: ${item?.javaClass?.name}")

                            when (item) {
                                // Handle DocumentReference
                                is com.google.firebase.firestore.DocumentReference -> {
                                    Log.d("FetchShopDetails", "Processing DocumentReference: ${item.path}")
                                    item.get()
                                        .addOnSuccessListener { serviceDoc ->
                                            val serviceName = serviceDoc.getString("serviceName")
                                            if (serviceName != null) {
                                                services.add(serviceName)
                                                Log.d("FetchShopDetails", "Added service name: $serviceName")
                                            } else {
                                                services.add("Unknown Service (No Name)")
                                                Log.d("FetchShopDetails", "Service document has no serviceName field")
                                            }

                                            completedFetches++
                                            if (completedFetches == servicesList.size) {
                                                processOpeningHours(doc, selectedDate, services, onOpeningHoursUpdated, onError)
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("FetchShopDetails", "Error fetching service", e)
                                            services.add("Service (ID: ${item.id.take(8)}...)")

                                            completedFetches++
                                            if (completedFetches == servicesList.size) {
                                                processOpeningHours(doc, selectedDate, services, onOpeningHoursUpdated, onError)
                                            }
                                        }
                                }
                                // Handle Map (could be a serialized reference)
                                is Map<*, *> -> {
                                    val serviceId = item["id"]?.toString()
                                    val serviceName = item["serviceName"]?.toString() ?: item["name"]?.toString()

                                    if (serviceName != null) {
                                        // If the map contains a name field, use it directly
                                        services.add(serviceName)
                                        Log.d("FetchShopDetails", "Using service name from map: $serviceName")
                                        completedFetches++
                                    } else if (serviceId != null) {
                                        // If there's an ID but no name, try to fetch the service
                                        servicesCollection.document(serviceId)
                                            .get()
                                            .addOnSuccessListener { serviceDoc ->
                                                val name = serviceDoc.getString("serviceName")
                                                if (name != null) {
                                                    services.add(name)
                                                    Log.d("FetchShopDetails", "Added service name from ID: $name")
                                                } else {
                                                    services.add("Service: $serviceId")
                                                    Log.d("FetchShopDetails", "Service has no serviceName field")
                                                }
                                                completedFetches++
                                                if (completedFetches == servicesList.size) {
                                                    processOpeningHours(doc, selectedDate, services, onOpeningHoursUpdated, onError)
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e("FetchShopDetails", "Error fetching service with ID", e)
                                                services.add("Unknown Service")
                                                completedFetches++
                                                if (completedFetches == servicesList.size) {
                                                    processOpeningHours(doc, selectedDate, services, onOpeningHoursUpdated, onError)
                                                }
                                            }
                                    } else {
                                        // Map doesn't have useful info
                                        services.add("Unknown Service (Invalid Map)")
                                        Log.d("FetchShopDetails", "Map has no id or name: $item")
                                        completedFetches++
                                    }

                                    if (completedFetches == servicesList.size) {
                                        processOpeningHours(doc, selectedDate, services, onOpeningHoursUpdated, onError)
                                    }
                                }
                                // Handle String (could be service name, ID, or path)
                                is String -> {
                                    val text = item.toString()
                                    if (text.startsWith("services/") || text.startsWith("/services/")) {
                                        // This looks like a reference path
                                        val serviceId = text.replace("^/?services/".toRegex(), "")
                                        Log.d("FetchShopDetails", "Fetching service with path: $text, ID: $serviceId")

                                        servicesCollection.document(serviceId)
                                            .get()
                                            .addOnSuccessListener { serviceDoc ->
                                                val serviceName = serviceDoc.getString("serviceName")
                                                if (serviceName != null) {
                                                    services.add(serviceName)
                                                    Log.d("FetchShopDetails", "Added service name from path: $serviceName")
                                                } else {
                                                    services.add("Service: $serviceId")
                                                    Log.d("FetchShopDetails", "Service from path has no serviceName field")
                                                }

                                                completedFetches++
                                                if (completedFetches == servicesList.size) {
                                                    processOpeningHours(doc, selectedDate, services, onOpeningHoursUpdated, onError)
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e("FetchShopDetails", "Error fetching service from path", e)
                                                services.add("Unknown Service")

                                                completedFetches++
                                                if (completedFetches == servicesList.size) {
                                                    processOpeningHours(doc, selectedDate, services, onOpeningHoursUpdated, onError)
                                                }
                                            }
                                    } else {
                                        // Try to determine if it's an ID or a name
                                        if (text.length < 20) { // Assume it's a direct service name
                                            services.add(text)
                                            Log.d("FetchShopDetails", "Added direct service name: $text")
                                        } else { // Might be a Firebase ID
                                            servicesCollection.document(text)
                                                .get()
                                                .addOnSuccessListener { serviceDoc ->
                                                    if (serviceDoc.exists()) {
                                                        val serviceName = serviceDoc.getString("serviceName")
                                                        if (serviceName != null) {
                                                            services.add(serviceName)
                                                            Log.d("FetchShopDetails", "Added service name from potential ID: $serviceName")
                                                        } else {
                                                            services.add(text) // Use original string as fallback
                                                            Log.d("FetchShopDetails", "Using original string as service: $text")
                                                        }
                                                    } else {
                                                        services.add(text) // Document doesn't exist, use original string
                                                        Log.d("FetchShopDetails", "Document doesn't exist, using as service name: $text")
                                                    }

                                                    completedFetches++
                                                    if (completedFetches == servicesList.size) {
                                                        processOpeningHours(doc, selectedDate, services, onOpeningHoursUpdated, onError)
                                                    }
                                                }
                                                .addOnFailureListener { e ->
                                                    // Query failed, use string as is
                                                    Log.e("FetchShopDetails", "Error testing if string is ID", e)
                                                    services.add(text)

                                                    completedFetches++
                                                    if (completedFetches == servicesList.size) {
                                                        processOpeningHours(doc, selectedDate, services, onOpeningHoursUpdated, onError)
                                                    }
                                                }
                                        }
                                    }
                                }
                                // Any other type or null
                                else -> {
                                    services.add("Unknown Service")
                                    Log.d("FetchShopDetails", "Unknown service item type: ${item?.javaClass?.name}")

                                    completedFetches++
                                    if (completedFetches == servicesList.size) {
                                        processOpeningHours(doc, selectedDate, services, onOpeningHoursUpdated, onError)
                                    }
                                }
                            }
                        }
                    }
                    // Handle case where servicesOffered is a Map (perhaps a single service)
                    is Map<*, *> -> {
                        Log.d("FetchShopDetails", "servicesOffered is a Map")
                        val serviceName = servicesList["serviceName"]?.toString() ?: servicesList["name"]?.toString()
                        if (serviceName != null) {
                            services.add(serviceName)
                        } else {
                            // Try to use ID if present
                            val serviceId = servicesList["id"]?.toString()
                            if (serviceId != null) {
                                servicesCollection.document(serviceId)
                                    .get()
                                    .addOnSuccessListener { serviceDoc ->
                                        val name = serviceDoc.getString("serviceName")
                                        if (name != null) {
                                            services.add(name)
                                        } else {
                                            services.add("Service: $serviceId")
                                        }
                                        processOpeningHours(doc, selectedDate, services, onOpeningHoursUpdated, onError)
                                    }
                                    .addOnFailureListener {
                                        services.add("Unknown Service")
                                        processOpeningHours(doc, selectedDate, services, onOpeningHoursUpdated, onError)
                                    }
                                return@addOnSuccessListener
                            } else {
                                services.add("Unknown Service (Map)")
                            }
                        }
                        processOpeningHours(doc, selectedDate, services, onOpeningHoursUpdated, onError)
                    }
                    // Handle case where servicesOffered is a String (perhaps a single service name or ID)
                    is String -> {
                        Log.d("FetchShopDetails", "servicesOffered is a String: $servicesList")
                        services.add(servicesList.toString())
                        processOpeningHours(doc, selectedDate, services, onOpeningHoursUpdated, onError)
                    }
                    // Handle case where servicesOffered is null or some other type
                    else -> {
                        Log.e("FetchShopDetails", "servicesOffered is not a recognized type: ${servicesList?.javaClass?.name}")
                        onError("Invalid service data format")
                        processOpeningHours(doc, selectedDate, services, onOpeningHoursUpdated, onError)
                    }
                }
            } else {
                onError("No shop details found.")
                onOpeningHoursUpdated(Pair(9, 17), emptyList())
            }
        }
        .addOnFailureListener { exception ->
            onError("Error fetching shop details: ${exception.message}")
            onOpeningHoursUpdated(Pair(9, 17), emptyList())
        }
}

private fun processOpeningHours(
    doc: com.google.firebase.firestore.DocumentSnapshot,
    selectedDate: Calendar?,
    services: MutableList<String>,
    onOpeningHoursUpdated: (Pair<Int, Int>?, List<String>) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val openingHoursMap = doc.get("openingHours") as? Map<*, *>
        val closedDays = mutableListOf<String>()

        // Process closed days
        openingHoursMap?.forEach { (day, data) ->
            val dayData = data as? Map<*, *>
            val isClosed = dayData?.get("closed") as? Boolean ?: false
            if (isClosed) {
                closedDays.add(day.toString())
            }
        }

        // Get the day for the selected date or current day
        val selectedDay = if (selectedDate != null) {
            getDayOfWeek(selectedDate)
        } else {
            getCurrentDay()
        }

        // Get hours for the selected day
        val dayData = openingHoursMap?.get(selectedDay) as? Map<*, *>
        val isClosed = dayData?.get("closed") as? Boolean ?: false

        if (isClosed) {
            // Shop is closed on the selected day
            onOpeningHoursUpdated(null, closedDays)
        } else {
            // Shop is open, get opening hours
            val openTime = dayData?.get("open") as? String
            val closeTime = dayData?.get("close") as? String

            if (openTime != null && closeTime != null) {
                val openHour = parseTime(openTime)
                val closeHour = parseTime(closeTime)
                onOpeningHoursUpdated(Pair(openHour, closeHour), closedDays)
            } else {
                // Default hours if not specified
                onOpeningHoursUpdated(Pair(9, 17), closedDays)
            }
        }
    } catch (e: Exception) {
        onError("Error parsing opening hours: ${e.message}")
        onOpeningHoursUpdated(Pair(9, 17), emptyList())
    }
}

fun getCurrentDay(): String {
    val calendar = Calendar.getInstance()
    val dayName = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()) ?: "Monday"
    // First letter capitalized, rest lowercase to match Firestore data
    return dayName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

fun getDayOfWeek(calendar: Calendar): String {
    val dayName = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()) ?: "Monday"
    // First letter capitalized, rest lowercase to match Firestore data
    return dayName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

class CustomDatePickerDialog(
    context: Context,
    private val closedDays: List<String>,
    private val onDateSelected: (Calendar) -> Unit
) : DatePickerDialog(
    context,
    { _, year, month, dayOfMonth ->
        // This is handled in our implementation below
    },
    Calendar.getInstance().get(Calendar.YEAR),
    Calendar.getInstance().get(Calendar.MONTH),
    Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
) {

    private val daysOfWeekMap = mapOf(
        "Monday" to Calendar.MONDAY,
        "Tuesday" to Calendar.TUESDAY,
        "Wednesday" to Calendar.WEDNESDAY,
        "Thursday" to Calendar.THURSDAY,
        "Friday" to Calendar.FRIDAY,
        "Saturday" to Calendar.SATURDAY,
        "Sunday" to Calendar.SUNDAY
    )

    // Convert string day names to Calendar day constants
    private val closedDaysInt = closedDays.mapNotNull { daysOfWeekMap[it] }

    init {
        // Override the date set listener to check for closed days
        setOnDateSetListener { _, year, month, dayOfMonth ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(year, month, dayOfMonth)

            // Check if selected day is closed
            val dayOfWeek = selectedCalendar.get(Calendar.DAY_OF_WEEK)
            if (closedDaysInt.contains(dayOfWeek)) {
                Toast.makeText(
                    context,
                    "This shop is closed on ${getDayName(dayOfWeek)}. Please select a different day.",
                    Toast.LENGTH_LONG
                ).show()
                // Don't dismiss the dialog, force user to select a valid day
            } else {
                onDateSelected(selectedCalendar)
                dismiss()
            }
        }

        // Set a custom title that shows closed days
        if (closedDays.isNotEmpty()) {
            setTitle("Select Date (Closed: ${closedDays.joinToString(", ")})")
        }

        // Apply theme programmatically
        try {
            // This will be executed when the dialog is created
            setOnShowListener {
                // Find the header text view (title) if present
                val titleView = this.findViewById<TextView>(android.R.id.title)
                titleView?.setTextColor(android.graphics.Color.BLACK)

                // Find the calendar header if present
                val headerView = this.findViewById<View>(
                    context.resources.getIdentifier("date_picker_header", "id", "android")
                )
                headerView?.setBackgroundColor(android.graphics.Color.BLACK)

                // Find the day header text views
                val dayHeaderContainer = this.findViewById<ViewGroup>(
                    context.resources.getIdentifier("day_picker_selector_layout", "id", "android")
                )
                if (dayHeaderContainer != null) {
                    for (i in 0 until dayHeaderContainer.childCount) {
                        val child = dayHeaderContainer.getChildAt(i)
                        if (child is TextView) {
                            child.setTextColor(android.graphics.Color.BLACK)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CustomDatePickerDialog", "Failed to style components: ${e.message}")
        }
    }

    private fun findNextAvailableDay(startDate: Calendar): Calendar? {
        val cal = Calendar.getInstance()
        cal.time = startDate.time

        // Try up to 7 days to find an available day
        for (i in 1..7) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            if (!closedDaysInt.contains(dayOfWeek)) {
                return cal
            }
        }
        return null // Couldn't find an available day
    }
}

class CustomDatePicker(context: Context, attrs: AttributeSet?) : DatePicker(context, attrs) {
    private val disabledDays = mutableListOf<Int>()

    fun setDisabledDays(days: List<String>) {
        disabledDays.clear()
        for (day in days) {
            val dayOfWeek = when (day) {
                "Monday" -> Calendar.MONDAY
                "Tuesday" -> Calendar.TUESDAY
                "Wednesday" -> Calendar.WEDNESDAY
                "Thursday" -> Calendar.THURSDAY
                "Friday" -> Calendar.FRIDAY
                "Saturday" -> Calendar.SATURDAY
                "Sunday" -> Calendar.SUNDAY
                else -> Calendar.MONDAY
            }
            disabledDays.add(dayOfWeek)
        }
        updateDisabledDays()
    }

    private fun updateDisabledDays() {
        val calendar = Calendar.getInstance()
        for (i in 0 until this.childCount) {
            val dayView = this.getChildAt(i)
            calendar.set(Calendar.DAY_OF_MONTH, i + 1)
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            dayView.isEnabled = !disabledDays.contains(dayOfWeek)
        }
    }
}

private fun createThemedDatePickerDialog(
    context: Context,
    closedDays: List<String>,
    onDateSelected: (Calendar) -> Unit
): CustomDatePickerDialog {
    val datePickerDialog = CustomDatePickerDialog(context, closedDays, onDateSelected)

    // Style the dialog with black theme
    try {
        // Apply black theme to the dialog
        datePickerDialog.window?.setBackgroundDrawableResource(android.R.color.white)

        // This will be executed after the dialog is shown to access internal views
        datePickerDialog.setOnShowListener {
            // Find the positive and negative buttons
            val positiveButton = datePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE)
            val negativeButton = datePickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE)

            // Style buttons
            positiveButton?.setTextColor(android.graphics.Color.BLACK)
            negativeButton?.setTextColor(android.graphics.Color.BLACK)
        }
    } catch (e: Exception) {
        Log.e("DatePicker", "Error styling date picker: ${e.message}")
    }

    return datePickerDialog
}

private fun createThemedTimePickerDialog(
    context: Context,
    openHour: Int,
    closeHour: Int,
    initialHour: Int,
    initialMinute: Int,
    onTimeSelected: (Int, Int) -> Unit
): TimePickerDialog {
    val timePickerDialog = TimePickerDialog(
        context,
        { _, selectedHour, selectedMinute ->
            // Validate if the selected time is within opening hours
            if (selectedHour < openHour || selectedHour >= closeHour) {
                Toast.makeText(
                    context,
                    "Please select a time between ${openHour}:00 and ${closeHour}:00",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                onTimeSelected(selectedHour, selectedMinute)
            }
        },
        initialHour,
        initialMinute,
        false
    )

    // Style the dialog with black theme
    try {
        // This will be executed after the dialog is shown to access internal views
        timePickerDialog.setOnShowListener {
            // Find the positive and negative buttons
            val positiveButton = timePickerDialog.getButton(TimePickerDialog.BUTTON_POSITIVE)
            val negativeButton = timePickerDialog.getButton(TimePickerDialog.BUTTON_NEGATIVE)

            // Style buttons
            positiveButton?.setTextColor(android.graphics.Color.BLACK)
            negativeButton?.setTextColor(android.graphics.Color.BLACK)
        }
    } catch (e: Exception) {
        Log.e("TimePicker", "Error styling time picker: ${e.message}")
    }

    return timePickerDialog
}

private fun getDayName(dayOfWeek: Int): String {
    return when (dayOfWeek) {
        Calendar.MONDAY -> "Monday"
        Calendar.TUESDAY -> "Tuesday"
        Calendar.WEDNESDAY -> "Wednesday"
        Calendar.THURSDAY -> "Thursday"
        Calendar.FRIDAY -> "Friday"
        Calendar.SATURDAY -> "Saturday"
        Calendar.SUNDAY -> "Sunday"
        else -> "Unknown"
    }
}

