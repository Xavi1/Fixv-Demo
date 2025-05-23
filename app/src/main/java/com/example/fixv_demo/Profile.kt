package com.example.fixv_demo

import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.DocumentReference
import java.util.Date

@Composable
fun ProfileScreen(
    navController: NavController
) {
    // Existing state variables
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var selectedItem by remember { mutableIntStateOf(0) }
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    var name by remember { mutableStateOf("John Doe") }
    var email by remember { mutableStateOf("john.doe@email.com") }
    var vehicles by remember { mutableStateOf<List<Vehicle>>(emptyList()) }
    var appointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    val db = Firebase.firestore

    // Fetch user data from Firestore
    LaunchedEffect(userId) {
        if (userId != null) {
            // Fetch user profile data
            db.collection("Users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        name = document.getString("name") ?: "Unknown User"
                        email = document.getString("email") ?: "No Email"
                    } else {
                        Log.e("ProfileScreen", "User document does not exist")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("ProfileScreen", "Failed to fetch user data: ${exception.message}")
                    name = "John Doe"
                    email = "john.doe@email.com"
                }

            // Fetch vehicles data from Vehicles collection
            db.collection("Vehicles")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener { documents ->
                    val vehicleList = mutableListOf<Vehicle>()
                    for (document in documents) {
                        val vehicle = Vehicle(
                            id = document.id,
                            model = document.getString("model") ?: "",
                            plateNumber = document.getString("licensePlate") ?: "",
                            make = document.getString("make") ?: "",
                            year = document.getLong("year")?.toInt() ?: 0,
                            mileage = document.getLong("mileage")?.toInt() ?: 0
                        )
                        vehicleList.add(vehicle)
                    }
                    vehicles = vehicleList
                }
                .addOnFailureListener { exception ->
                    Log.e("ProfileScreen", "Failed to fetch vehicles: ${exception.message}")
                }

            // Fetch appointments data from Appointments collection
            db.collection("appointments")
                .whereEqualTo("userId", db.document("Users/$userId"))
                .get()
                .addOnSuccessListener { documents ->
                    val appointmentList = mutableListOf<Appointment>()
                    for (document in documents) {
                        // Get service references and convert to list of service IDs
                        val serviceRefs =
                            document.get("services") as? List<DocumentReference> ?: emptyList()
                        val serviceNames = mutableListOf<String>()

                        for (serviceRef in serviceRefs) {
                            serviceRef.get().addOnSuccessListener { serviceDoc ->
                                val serviceName =
                                    serviceDoc.getString("serviceName") ?: "Unknown Service"
                                serviceNames.add(serviceName)

                                if (serviceNames.size == serviceRefs.size) {
                                    val appointment = Appointment(
                                        appointmentId = document.id,
                                        serviceType = serviceNames,
                                        date = document.getString("date") ?: "",
                                        time = document.getString("time") ?: "",
                                        statusConfirmed = document.getBoolean("status.confirmed")
                                            ?: false,
                                        shopName = (document.get("shopId") as? DocumentReference)?.id
                                            ?: "",
                                        vehicleDetails = (document.get("vehicleId") as? DocumentReference)?.id
                                            ?: "",
                                        iconResource = R.drawable.clock_icon, // Assuming a default icon resource
                                        licensePlate = document.getString("licensePlate") ?: "",
                                        invoiceId = document.getString("invoiceId") ?: ""
                                    )
                                    appointmentList.add(appointment)
                                    appointments = appointmentList
                                }
                            }.addOnFailureListener { exception ->
                                Log.e(
                                    "ProfileScreen",
                                    "Failed to fetch service: ${exception.message}"
                                )
                            }
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("ProfileScreen", "Failed to fetch appointments: ${exception.message}")
                }
        }
    }

    if (showEditDialog) {
        EditProfileDialog(
            userId = userId ?: "",
            currentName = name,
            currentEmail = email,
            onDismiss = { showEditDialog = false },
            onSave = { newName, newEmail ->
                name = newName
                email = newEmail
                showEditDialog = false
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

    var expanded by remember { mutableStateOf(false) }
    var appointmentsExpanded by remember { mutableStateOf(false) }

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
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Profile",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // User Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.size(24.dp),
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "User",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Profile Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = Color.LightGray,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Edit Button (Top Right)
                        IconButton(
                            onClick = { showEditDialog = true },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile",
                                tint = Color.Black
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                // Name
                                Text(
                                    text = "Name: $name",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                // Email
                                Text(
                                    text = "Email: $email",
                                    fontSize = 16.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            // My Vehicles Section
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.car_icon),
                            contentDescription = "Vehicle Icon",
                            modifier = Modifier.size(24.dp),
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "My Vehicles",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        // Edit Button (aligned to end)
                        Row(
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = {
                                navController.navigate("editVehicleList")
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Vehicles",
                                    tint = Color.Black
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (vehicles.isEmpty()) {
                        Text(
                            text = "No vehicles found",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                    } else {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = Color.LightGray,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { expanded = !expanded },
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${vehicles[0].year} ${vehicles[0].make} ${vehicles[0].model}",
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = "Plate Number: ${vehicles[0].plateNumber}",
                                            fontSize = 14.sp,
                                            color = Color.Gray
                                        )
                                    }

                                    Icon(
                                        imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Expand/Collapse"
                                    )
                                }

                                if (expanded) {
                                    vehicles.drop(1).forEach { extraVehicle ->
                                        HorizontalDivider()
                                        Column(modifier = Modifier.padding(top = 8.dp)) {
                                            Text(
                                                text = "${extraVehicle.year} ${extraVehicle.make} ${extraVehicle.model}",
                                                fontSize = 16.sp
                                            )
                                            Text(
                                                text = "Plate Number: ${extraVehicle.plateNumber}",
                                                fontSize = 14.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Appointments Section
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.clock_icon),
                            contentDescription = "Appointments Icon",
                            modifier = Modifier.size(24.dp),
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Appointments",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        // Edit Button (aligned to end)
                        Row(
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = {
                                navController.navigate("manageAppointmentsScreen")
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Appointments",
                                    tint = Color.Black
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    if (appointments.isEmpty()) {
                        Text(
                            text = "No appointments found",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                    } else {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = Color.LightGray,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { appointmentsExpanded = !appointmentsExpanded },
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        val firstAppointment = appointments[0]
                                        val vehicle = vehicles.find { it.id == firstAppointment.vehicleDetails }
                                        Text(
                                            text = vehicle?.let { "${it.year} ${it.make} ${it.model}" }
                                                ?: "Vehicle details not available",
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = firstAppointment.serviceType.joinToString(", "),
                                            fontSize = 14.sp,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = if (firstAppointment.statusConfirmed) "Confirmed" else "Pending",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (firstAppointment.statusConfirmed) Color(0xFF4CAF50) else Color(0xFFFF9800)
                                        )
                                    }

                                    Icon(
                                        imageVector = if (appointmentsExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Expand/Collapse"
                                    )
                                }

                                if (appointmentsExpanded) {
                                    appointments.drop(1).forEach { appointment ->
                                        HorizontalDivider()
                                        Column(modifier = Modifier.padding(top = 8.dp)) {
                                            val vehicle = vehicles.find { it.id == appointment.vehicleDetails }
                                            Text(
                                                text = vehicle?.let { "${it.year} ${it.make} ${it.model}" }
                                                    ?: "Vehicle details not available",
                                                fontSize = 16.sp
                                            )
                                            Text(
                                                text = appointment.serviceType.joinToString(", "),
                                                fontSize = 14.sp,
                                                color = Color.Gray
                                            )
                                            Text(
                                                text = "${appointment.date} ${appointment.time}",
                                                fontSize = 14.sp,
                                                color = Color.Gray
                                            )
                                            Text(
                                                text = if (appointment.statusConfirmed) "Confirmed" else "Pending",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (appointment.statusConfirmed) Color(0xFF4CAF50) else Color(0xFFFF9800)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class Profile(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val createdAt: Date = Date()
)

@Composable
fun EditProfileDialog(
    userId: String,
    currentName: String,
    currentEmail: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    val db = Firebase.firestore
    var newName by remember { mutableStateOf(currentName) }
    var newEmail by remember { mutableStateOf(currentEmail) }
    var isSaving by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }

    val colors = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        focusedIndicatorColor = Color.Black,
        unfocusedIndicatorColor = Color.Gray,
        cursorColor = Color.Black
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Edit Profile", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
        containerColor = Color.White,
        text = {
            Column {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Name", color = Color.Black) },
                    singleLine = true,
                    colors = colors
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = {
                        newEmail = it
                        emailError = if (isValidEmail(it)) null else "Invalid email format"
                    },
                    label = { Text("Email", color = Color.Black) },
                    singleLine = true,
                    colors = colors,
                    isError = emailError != null
                )

                if (emailError != null) {
                    Text(
                        text = emailError!!,
                        color = Color.Red,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!isValidEmail(newEmail)) {
                        emailError = "Invalid email format"
                        return@TextButton
                    }

                    if (userId.isNotEmpty()) {
                        isSaving = true
                        val updatedData = mapOf(
                            "name" to newName,
                            "email" to newEmail
                        )
                        db.collection("Users").document(userId).update(updatedData)
                            .addOnSuccessListener {
                                Log.d("EditProfileDialog", "Profile updated successfully")
                                onSave(newName, newEmail)
                                isSaving = false
                                onDismiss()
                            }
                            .addOnFailureListener { e ->
                                Log.e(
                                    "EditProfileDialog",
                                    "Error updating profile: ${e.message}"
                                )
                                isSaving = false
                            }
                    }
                }
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.Black
                    )
                } else {
                    Text("Save", color = Color.Black)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Black)
            }
        }
    )
}

// Email Validation Function
fun isValidEmail(email: String): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}