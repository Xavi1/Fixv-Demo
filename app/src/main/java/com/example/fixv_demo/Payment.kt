package com.example.fixv_demo

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    navController: NavController,
    appointmentDetailsJson: String,
    currentUserId: String,
    onBackClick: () -> Unit,
    onPaymentComplete: (String) -> Unit // Add this parameter
) {
    val sharedViewModel: SharedViewModel = viewModel()
    val paymentState by sharedViewModel.paymentState.collectAsState()
    val appointmentDetails = remember {
        Gson().fromJson(appointmentDetailsJson, AppointmentDetails::class.java)
    }

    var isProcessing by remember { mutableStateOf(false) }
    var serviceIdMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var selectedPaymentMethod by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    val firestore = remember { FirebaseFirestore.getInstance() }

    // Handle payment state changes
    LaunchedEffect(paymentState) {
        when (val state = paymentState) {
            is PaymentState.Success -> {
                val invoiceJson = Gson().toJson(state.invoiceDetails)
                navController.navigate("invoice/$invoiceJson")
            }
            is PaymentState.Error -> {
                // Show error dialog
                loadError = state.message
            }
            PaymentState.Loading, PaymentState.Idle -> {
                // Loading state handled elsewhere
            }
        }
    }

    // Fetch service IDs (same as before)
    LaunchedEffect(appointmentDetails) {
        try {
            val serviceCollection = FirebaseFirestore.getInstance().collection("services")
            val fetchedServiceMap = mutableMapOf<String, String>()

            serviceCollection.get()
                .addOnSuccessListener { querySnapshot ->
                    val allServices = querySnapshot.documents.associate {
                        val name = it.getString("serviceName") ?: ""
                        name.lowercase() to it.id
                    }

                    appointmentDetails.services.forEach { serviceName ->
                        val serviceId = allServices[serviceName.lowercase()]
                        if (serviceId != null) {
                            fetchedServiceMap[serviceName] = serviceId
                        } else {
                            val matchingService = allServices.entries.firstOrNull {
                                it.key.contains(serviceName.lowercase()) ||
                                        serviceName.lowercase().contains(it.key)
                            }
                            if (matchingService != null) {
                                fetchedServiceMap[serviceName] = matchingService.value
                            } else {
                                loadError = "Service not found: $serviceName"
                            }
                        }
                    }
                    serviceIdMap = fetchedServiceMap
                    isLoading = false
                }
                .addOnFailureListener { e ->
                    loadError = "Error loading services: ${e.message}"
                    isLoading = false
                }
        } catch (e: Exception) {
            loadError = "Error: ${e.message}"
            isLoading = false
        }
    }

    var showDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var invoiceDetailsJson by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                onLogoutClick = { /* No logout needed here */ },
                showLogoutButton = false
            )
        },
        bottomBar = {
            BottomNavBar(
                navController = navController,
                selectedItem = 0, // Home is selected by default
                onItemSelected = { selectedItem ->
                    when (selectedItem) {
                        0 -> navController.navigate("home")
                        1 -> navController.navigate("calendar")
                        2 -> navController.navigate("profile")
                    }
                }
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Payment Details",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Loading indicator
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            // Error message for service loading
            if (loadError != null) {
                item {
                    Text(
                        text = loadError ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            // Only show content when services are loaded
            if (!isLoading && loadError == null) {
                // Order Summary Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = "Order Summary",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            appointmentDetails.services.forEach { service ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(service)
                                    Text("$${appointmentDetails.servicePrices[service]}")
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total", fontWeight = FontWeight.Bold)
                                Text("$${String.format("%.2f", appointmentDetails.totalCost)}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Payment Method Selection
                item {
                    Text(
                        text = "Payment Method",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            PaymentMethodItem(
                                title = "Cash on Delivery",
                                iconResId = R.drawable.ic_cash_on_delivery,
                                selected = selectedPaymentMethod == "cod",
                                onClick = {
                                    selectedPaymentMethod = "cod"
                                    showError = false
                                }
                            )
                            PaymentMethodItem(
                                title = "Credit Card",
                                iconResId = R.drawable.ic_credit_card, // Replace with your credit card icon
                                selected = false,
                                onClick = {},
                                enabled = false, // Disabled
                                subtext = "Coming Soon"
                            )
                            PaymentMethodItem(
                                title = "Google Pay",
                                iconResId = R.drawable.ic_google_pay, // Replace with your Google Pay icon
                                selected = false,
                                onClick = {},
                                enabled = false, // Disabled
                                subtext = "Coming Soon"
                            )
                        }
                    }
                }
                // Error Message
                if (showError) {
                    item {
                        Text(
                            text = "Please select a payment method.",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                // Pay Button
                item {
                    Button(
                        onClick = {
                            val currentTimestamp = System.currentTimeMillis()
                            Log.d("PaymentScreen", "Initiating payment transaction at: $currentTimestamp")
                            Log.d("PaymentScreen", "Selected payment method: $selectedPaymentMethod")
                            if (selectedPaymentMethod.isEmpty()) {
                                Log.e("PaymentScreen", "No payment method selected")
                                showError = true
                            } else {
                                showDialog = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black
                        )
                    ) {
                        Text(
                            "Pay $${String.format("%.2f", appointmentDetails.totalCost)}",
                            modifier = Modifier.padding(vertical = 8.dp),
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    // Confirmation Dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { if (!isProcessing) showDialog = false },
            title = { Text("Confirm Payment", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to proceed with the payment?") },
            containerColor = Color.White,
            confirmButton = {
                Button(
                    onClick = {
                        isProcessing = true
                        try {
                            println("Confirm button clicked")
                            firestore.collection("test_connectivity").document("test").set(hashMapOf("test" to System.currentTimeMillis()))
                                .addOnSuccessListener {
                                    processPaymentTransaction(
                                        firestore,
                                        appointmentDetails,
                                        selectedPaymentMethod,
                                        currentUserId,
                                        serviceIdMap,
                                        { value -> showDialog = value },
                                        { value -> showSuccessDialog = value },
                                        { value -> showErrorDialog = value },
                                        { value -> errorMessage = value },
                                        { invoiceJson ->
                                            invoiceDetailsJson = invoiceJson
                                            onPaymentComplete(invoiceJson)
                                        }
                                    )
                                }
                                .addOnFailureListener { e ->
                                    isProcessing = false
                                    showErrorDialog = true
                                    errorMessage = "Cannot connect to database: ${e.message}"
                                }
                        } catch (e: Exception) {
                            isProcessing = false
                            showErrorDialog = true
                            errorMessage = "Error: ${e.message}"
                        }
                    },
                    enabled = !isProcessing, // Disable button while processing
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) {
                    Text("Confirm", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { if (!isProcessing) showDialog = false },
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(width = 1.dp)
                ) {
                    Text("Cancel", color = Color.Black)
                }
            }
        )
    }

    // Error Dialog
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("Error", fontWeight = FontWeight.Bold, color = Color.Red) },
            text = { Text(errorMessage) },
            containerColor = Color.White,
            confirmButton = {
                Button(
                    onClick = { showErrorDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) {
                    Text("OK", color = Color.White)
                }
            }
        )
    }

    // Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                navController.navigate("invoice/$invoiceDetailsJson")
            },
            title = { Text("Payment Successful", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50)) },
            text = { Text("Your payment has been processed successfully.") },
            containerColor = Color.White,
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        navController.navigate("invoice/$invoiceDetailsJson")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) {
                    Text("View Invoice", color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun PaymentMethodItem(
    title: String,
    iconResId: Int,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true, // Add enabled parameter
    subtext: String? = null // Add subtext parameter
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = iconResId),
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                colorFilter = if (enabled) null else ColorFilter.tint(Color.Gray) // Grey out if disabled
            )
            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = if (selected && enabled) FontWeight.Bold else FontWeight.Normal,
                    color = if (enabled) Color.Black else Color.Gray
                )
                subtext?.let {
                    Text(
                        text = it,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
        RadioButton(
            selected = selected,
            onClick = { if (enabled) onClick() },
            enabled = enabled, // Disable RadioButton if not enabled
            colors = RadioButtonDefaults.colors(
                selectedColor = Color.Black, // Color when selected
                unselectedColor = Color.Gray, // Color when unselected
                disabledSelectedColor = Color.Gray, // Color when selected but disabled
                disabledUnselectedColor = Color.LightGray // Color when unselected and disabled
            )
        )
    }
}

data class PaymentTransaction(
    val totalPrice: Double,
    val paymentMethod: String,
    val serviceTypes: List<DocumentReference>,
    val userId: DocumentReference,
    val shopId: DocumentReference,
    val createdAt: Timestamp = Timestamp.now(),
    val payment_status: String = "pending",
    val invoiceId: DocumentReference? = null,
    val transactionId: String? = null,
    val vehicleId: DocumentReference? = null
)

private fun processPaymentTransaction(
    firestore: FirebaseFirestore,
    appointmentDetails: AppointmentDetails,
    selectedPaymentMethod: String,
    currentUserId: String,
    serviceIdMap: Map<String, String>,
    setShowDialog: (Boolean) -> Unit,
    setShowSuccessDialog: (Boolean) -> Unit,
    setShowErrorDialog: (Boolean) -> Unit,
    setErrorMessage: (String) -> Unit,
    onPaymentComplete: (String) -> Unit
) {
    try {
        Log.d("PaymentScreen", "Starting payment transaction process")

        val userRef = firestore.collection("Users").document(currentUserId)
        val shopRef = firestore.collection("mechanic_shops").document(appointmentDetails.shopId)
        val vehicleRef = firestore.collection("Vehicles").document(appointmentDetails.vehicleId)

        val serviceRefs = appointmentDetails.services.mapNotNull { serviceName ->
            val serviceId = serviceIdMap[serviceName]
            if (serviceId != null) {
                firestore.collection("services").document(serviceId)
            } else {
                Log.e("PaymentScreen", "No service ID found for service: $serviceName")
                null
            }
        }

        if (serviceRefs.size != appointmentDetails.services.size) {
            throw Exception("Could not find IDs for all services")
        }

        val paymentTransactionRef = firestore.collection("payment_transactions").document()
        val invoiceRef = firestore.collection("invoices").document()
        val appointmentRef = firestore.collection("appointments").document()

        firestore.runTransaction { transaction ->
            val userSnapshot = transaction.get(userRef)
            val shopSnapshot = transaction.get(shopRef)

            val userName = userSnapshot.getString("name") ?: "Unknown User"
            val shopName = shopSnapshot.getString("name") ?: "Unknown Shop"

            val paymentTransaction = PaymentTransaction(
                totalPrice = appointmentDetails.totalCost,
                paymentMethod = selectedPaymentMethod,
                serviceTypes = serviceRefs,
                userId = userRef,
                shopId = shopRef,
                createdAt = Timestamp.now(),
                payment_status = "pending",
                invoiceId = invoiceRef,
                transactionId = paymentTransactionRef.id
            )

            val appointment = hashMapOf(
                "userId" to userRef,
                "shopId" to shopRef,
                "vehicleId" to vehicleRef,
                "services" to serviceRefs,
                "date" to appointmentDetails.date,
                "time" to appointmentDetails.time,
                "status" to mapOf("confirmed" to true),
                "transactionId" to paymentTransactionRef,
                "appointmentId" to appointmentRef.id,
                "createdAt" to Timestamp.now()
            )

            val invoice = hashMapOf(
                "userId" to userRef,
                "shopId" to shopRef,
                "vehicleId" to vehicleRef,
                "amount_due" to appointmentDetails.totalCost,
                "services" to serviceRefs,
                "serviceNames" to appointmentDetails.services,
                "transactionId" to paymentTransactionRef,
                "appointmentId" to appointmentRef,
                "createdAt" to Timestamp.now(),
                "payment_status" to "pending",
                "status" to "payment pending"
            )

            transaction.set(paymentTransactionRef, paymentTransaction)
            transaction.set(appointmentRef, appointment)
            transaction.set(invoiceRef, invoice)

            val invoiceDetailsJson = Gson().toJson(
                InvoiceDetails(
                    invoiceId = invoiceRef.id,
                    totalAmount = appointmentDetails.totalCost,
                    userName = userName,
                    paymentStatus = paymentTransaction.payment_status,
                    appointmentDate = appointmentDetails.date,
                    createdAt = Timestamp.now().toDate().toString(),
                    shopName = shopName,
                    services = appointmentDetails.services,
                    vehicleDetails = appointmentDetails.vehicleDetails,
                    shopId = appointmentDetails.shopId,
                    servicePrices = appointmentDetails.servicePrices
                )
            )

            onPaymentComplete(invoiceDetailsJson)
        }.addOnSuccessListener {
            setShowDialog(false)
            setShowSuccessDialog(true)
        }.addOnFailureListener { e ->
            setShowDialog(false)
            setShowErrorDialog(true)
            setErrorMessage("Failed to save transaction data: ${e.message}")
        }

    } catch (e: Exception) {
        setShowDialog(false)
        setShowErrorDialog(true)
        setErrorMessage("Error processing payment: ${e.message}")
    }
}

