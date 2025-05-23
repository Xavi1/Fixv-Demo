package com.example.fixv_demo

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.util.concurrent.atomic.AtomicInteger

@Composable
fun ManageAppointmentsScreen(navController: NavController) {
    var appointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var debugInfo by remember { mutableStateOf("") }
    val db = Firebase.firestore
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    LaunchedEffect(userId) {
        try {
            if (userId.isNotEmpty()) {
                Log.d(
                    "ManageAppointmentsScreen",
                    "DEBUG: First trying to fetch ALL appointments to check structure"
                )

                db.collection("appointments")
                    .limit(10)
                    .get()
                    .addOnSuccessListener { allDocuments: QuerySnapshot ->
                        val debugMsg = StringBuilder()
                        debugMsg.append("Found ${allDocuments.size()} total appointments in database\n")

                        if (allDocuments.size() > 0) {
                            val firstDoc = allDocuments.documents[0]
                            debugMsg.append("First document ID: ${firstDoc.id}\n")

                            val userIdValue = firstDoc.get("userId")
                            debugMsg.append("userId field type: ${userIdValue?.javaClass?.simpleName}\n")
                            debugMsg.append("userId value: $userIdValue\n")

                            val alternativeUserIdFields =
                                listOf("user_id", "user", "userRef", "userReference")
                            alternativeUserIdFields.forEach { field ->
                                val value = firstDoc.get(field)
                                if (value != null) {
                                    debugMsg.append("Alternative field '$field' found with value: $value\n")
                                }
                            }

                            debugMsg.append("All fields in document:\n")
                            firstDoc.data?.forEach { (key, value) ->
                                debugMsg.append("  $key: $value (${value?.javaClass?.simpleName})\n")
                            }
                        }

                        debugInfo = debugMsg.toString()
                        Log.d("ManageAppointmentsScreen", debugMsg.toString())

                        fetchWithApproach1(db, userId, debugInfo) { result ->
                            if (result.isNotEmpty()) {
                                appointments = result
                                isLoading = false
                            } else {
                                fetchWithApproach2(db, userId, debugInfo) { result2 ->
                                    if (result2.isNotEmpty()) {
                                        appointments = result2
                                        isLoading = false
                                    } else {
                                        fetchWithApproach3(db, userId, debugInfo) { result3 ->
                                            appointments = result3
                                            isLoading = false
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("ManageAppointmentsScreen", "Error fetching all appointments", exception)
                        errorMessage = "Failed initial debug query: ${exception.message}"
                        isLoading = false
                    }
            } else {
                errorMessage = "User not logged in"
                isLoading = false
            }
        } catch (e: Exception) {
            errorMessage = "Failed to load appointments: ${e.localizedMessage}"
            isLoading = false
        }
    }

    Scaffold(
        topBar = { TopAppBar(onLogoutClick = {}, showLogoutButton = false) },
        bottomBar = { BottomNavBar(selectedItem = 1, onItemSelected = {}, navController = navController) },
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

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Manage Appointments",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.size(56.dp))
            }
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.Black)
                    }
                }
                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = errorMessage ?: "Unknown error",
                            color = Color.Red,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                appointments.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No appointments found",
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        appointments.forEach { appointment ->
                            AppointmentItem(
                                appointment = appointment,
                                onRemoveClick = {
                                    removeAppointmentAndUpdateTransaction(
                                        db = db,
                                        appointment = appointment,
                                        onSuccess = {
                                            appointments = appointments.filter { it.appointmentId != appointment.appointmentId }
                                        },
                                        onFailure = { e ->
                                            errorMessage = "Failed to remove appointment: ${e.message}"
                                        }
                                    )
                                },
                                onEditClick = {
                                    navController.navigate("editappointmentscreen/${appointment.appointmentId}")
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { navController.navigate("ScheduleAppointment") },
                    modifier = Modifier
                        .width(200.dp)
                        .padding(vertical = 16.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) {
                    Text(text = "Add Appointment", color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }
}

private fun fetchWithApproach1(db: FirebaseFirestore, userId: String, debugInfo: String, callback: (List<Appointment>) -> Unit) {
    Log.d("ManageAppointmentsScreen", "Approach 1: Using just the userId")

    db.collection("appointments")
        .whereEqualTo("userId", userId)
        .get()
        .addOnSuccessListener { documents: QuerySnapshot ->
            Log.d("ManageAppointmentsScreen", "Approach 1: Found ${documents.size()} documents")
            if (documents.isEmpty) {
                callback(emptyList())
            } else {
                val appointmentList = mapDocumentsToAppointments(documents)
                callback(appointmentList)
            }
        }
        .addOnFailureListener { exception ->
            Log.e("ManageAppointmentsScreen", "Approach 1 error", exception)
            callback(emptyList())
        }
}

private fun fetchWithApproach2(db: FirebaseFirestore, userId: String, debugInfo: String, callback: (List<Appointment>) -> Unit) {
    Log.d("ManageAppointmentsScreen", "Approach 2: Using /Users/$userId")

    db.collection("appointments")
        .whereEqualTo("userId", "/Users/$userId")
        .get()
        .addOnSuccessListener { documents: QuerySnapshot ->
            Log.d("ManageAppointmentsScreen", "Approach 2: Found ${documents.size()} documents")
            if (documents.isEmpty) {
                callback(emptyList())
            } else {
                val appointmentList = mapDocumentsToAppointments(documents)
                callback(appointmentList)
            }
        }
        .addOnFailureListener { exception ->
            Log.e("ManageAppointmentsScreen", "Approach 2 error", exception)
            callback(emptyList())
        }
}

private fun fetchWithApproach3(db: FirebaseFirestore, userId: String, debugInfo: String, callback: (List<Appointment>) -> Unit) {
    Log.d("ManageAppointmentsScreen", "Approach 3: Using document reference approach")

    val userRef = db.document("Users/$userId")

    db.collection("appointments")
        .whereEqualTo("userId", userRef)
        .get()
        .addOnSuccessListener { documents: QuerySnapshot ->
            Log.d("ManageAppointmentsScreen", "Approach 3: Found ${documents.size()} documents")
            if (documents.isEmpty) {
                callback(emptyList())
            } else {
                // Launch a coroutine to handle the async operations
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val appointmentList = mapDocumentsToAppointmentsSuspend(documents)
                        callback(appointmentList)
                    } catch (e: Exception) {
                        Log.e("ManageAppointmentsScreen", "Error mapping appointments", e)
                        callback(emptyList())
                    }
                }
            }
        }
        .addOnFailureListener { exception ->
            Log.e("ManageAppointmentsScreen", "Approach 3 error", exception)
            callback(emptyList())
        }
}

private fun mapDocumentsToAppointments(documents: QuerySnapshot): List<Appointment> {
    val appointmentsList = mutableListOf<Appointment>()

    documents.forEach { document ->
        try {
            val status = document.get("status") as? Map<String, Boolean> ?: emptyMap()
            val isConfirmed = status["confirmed"] ?: false

            // Get references
            val shopRef = document.get("shopId") as? DocumentReference
            val vehicleRef = document.get("vehicleId") as? DocumentReference
            val serviceRefs = (document.get("services") as? List<*>)?.filterIsInstance<DocumentReference>() ?: emptyList()

            // Fetch service names
            val serviceNames = mutableListOf<String>()
            serviceRefs.forEach { serviceRef ->
                serviceRef.get().addOnSuccessListener { serviceDoc ->
                    if (serviceDoc.exists()) {
                        val serviceName = serviceDoc.getString("serviceName") ?: serviceRef.id
                        serviceNames.add(serviceName)
                    } else {
                        serviceNames.add(serviceRef.id)
                    }
                }.addOnFailureListener {
                    serviceNames.add(serviceRef.id) // Fallback to ID on error
                }
            }

            // Create an appointment with IDs first
            val appointment = Appointment(
                serviceType = serviceNames,
                iconResource = R.drawable.ic_default_service,
                shopName = shopRef?.id ?: "Unknown Shop",
                date = document.getString("date") ?: "No date",
                time = document.getString("time") ?: "No time",
                vehicleDetails = vehicleRef?.id ?: "Unknown Vehicle",
                licensePlate = "",
                invoiceId = (document.get("transactionId") as? DocumentReference)?.id ?: "",
                statusConfirmed = isConfirmed,
                appointmentId = document.id
            )

            appointmentsList.add(appointment)
        } catch (e: Exception) {
            Log.e("ManageAppointmentsScreen", "Error mapping document ${document.id}", e)
        }
    }

    return appointmentsList
}

// New suspending function to fetch appointment details
private suspend fun mapDocumentsToAppointmentsSuspend(documents: QuerySnapshot): List<Appointment> {
    val appointmentsList = mutableListOf<Appointment>()

    documents.forEach { document ->
        try {
            val status = document.get("status") as? Map<String, Boolean> ?: emptyMap()
            val isConfirmed = status["confirmed"] ?: false

            // Get references
            val shopRef = document.get("shopId") as? DocumentReference
            val vehicleRef = document.get("vehicleId") as? DocumentReference
            val serviceRefs = (document.get("services") as? List<*>)?.filterIsInstance<DocumentReference>() ?: emptyList()

            // Fetch shop details
            var shopName = "Unknown Shop"
            if (shopRef != null) {
                try {
                    val shopDoc = shopRef.get().await()
                    if (shopDoc.exists()) {
                        shopName = shopDoc.getString("name") ?: "Unknown Shop"
                    }
                } catch (e: Exception) {
                    Log.e("ManageAppointmentsScreen", "Error fetching shop: ${shopRef.id}", e)
                }
            }

            // Fetch vehicle details
            var vehicleDetails = "Unknown Vehicle"
            var licensePlate = ""
            if (vehicleRef != null) {
                try {
                    val vehicleDoc = vehicleRef.get().await()
                    if (vehicleDoc.exists()) {
                        val make = vehicleDoc.getString("make") ?: ""
                        val model = vehicleDoc.getString("model") ?: ""
                        val year = vehicleDoc.getLong("year")?.toString() ?: ""
                        licensePlate = vehicleDoc.getString("licensePlate") ?: ""
                        vehicleDetails = "$year $make $model"
                    }
                } catch (e: Exception) {
                    Log.e("ManageAppointmentsScreen", "Error fetching vehicle: ${vehicleRef.id}", e)
                }
            }

            // Fetch service names
            val serviceNames = mutableListOf<String>()
            for (serviceRef in serviceRefs) {
                try {
                    val serviceDoc = serviceRef.get().await()
                    if (serviceDoc.exists()) {
                        val serviceName = serviceDoc.getString("serviceName") ?: serviceRef.id
                        serviceNames.add(serviceName)
                    } else {
                        serviceNames.add(serviceRef.id)
                    }
                } catch (e: Exception) {
                    Log.e("ManageAppointmentsScreen", "Error fetching service: ${serviceRef.id}", e)
                    serviceNames.add(serviceRef.id) // Fallback to ID on error
                }
            }

            // Create the appointment with all details
            val appointment = Appointment(
                serviceType = serviceNames,
                iconResource = R.drawable.ic_default_service,
                shopName = shopName,
                date = document.getString("date") ?: "No date",
                time = document.getString("time") ?: "No time",
                vehicleDetails = vehicleDetails,
                licensePlate = licensePlate,
                invoiceId = (document.get("transactionId") as? DocumentReference)?.id ?: "",
                statusConfirmed = isConfirmed,
                appointmentId = document.id
            )

            appointmentsList.add(appointment)
        } catch (e: Exception) {
            Log.e("ManageAppointmentsScreen", "Error mapping document ${document.id}", e)
        }
    }

    return appointmentsList
}

@Composable
fun AppointmentItem(
    appointment: Appointment,
    onRemoveClick: () -> Unit,
    onEditClick: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth()
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
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${appointment.date} ${appointment.time}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Shop: ${appointment.shopName}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                if (appointment.serviceType.isNotEmpty()) {
                    Text(
                        text = "Services: ${appointment.serviceType.take(2).joinToString(", ")}",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    appointment.serviceType.drop(2).chunked(2).forEach { services ->
                        Text(
                            text = services.joinToString(", "),
                            fontSize = 14.sp,
                            color = Color.Gray,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = "Vehicle: ${appointment.vehicleDetails}${if (appointment.licensePlate.isNotEmpty()) " (${appointment.licensePlate})" else ""}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = "Status: ${if (appointment.statusConfirmed) "Confirmed" else "Pending"}",
                    fontSize = 14.sp,
                    color = if (appointment.statusConfirmed) Color.Green else Color.Gray
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        onEditClick()
                    },
                    enabled = !isLoading
                ) {
                    Text(text = "Edit", color = Color.Black)
                }

                IconButton(
                    onClick = { showDialog = true },
                    modifier = Modifier.size(48.dp) // Set a fixed size for the IconButton
                ) {
                    Icon(
                        imageVector = Icons.Filled.RemoveCircle,
                        contentDescription = "Remove Appointment",
                        tint = Color.Red,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = "Remove Appointment") },
            text = { Text("Are you sure you want to remove this appointment?") },
            confirmButton = {
                Button(
                    onClick = {
                        isLoading = true
                        CoroutineScope(Dispatchers.IO).launch {
                            onRemoveClick()
                            isLoading = false
                            showDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false }
                ) {
                    Text("Cancel", color = Color.Black)
                }
            }
        )
    }
}

private fun removeAppointmentAndUpdateTransaction(
    db: FirebaseFirestore,
    appointment: Appointment,
    onSuccess: () -> Unit,
    onFailure: (Exception) -> Unit
) {
    val transactionRef = db.collection("payment_transactions").document(appointment.invoiceId)

    db.collection("appointments").document(appointment.appointmentId)
        .delete()
        .addOnSuccessListener {
            transactionRef.update("payment_status", "Void")
                .addOnSuccessListener {
                    Log.d("ManageAppointmentsScreen", "Appointment removed and transaction voided successfully")
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    Log.e("ManageAppointmentsScreen", "Failed to update transaction status", e)
                    onFailure(e)
                }
        }
        .addOnFailureListener { e ->
            Log.e("ManageAppointmentsScreen", "Failed to remove appointment", e)
            onFailure(e)
        }
}