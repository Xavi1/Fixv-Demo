package com.example.fixv_demo

import com.example.fixv_demo.PaymentScreen
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import java.net.URLDecoder
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class Vehicle(
    val id: String,
    val make: String,
    val model: String,
    val mileage: Int,
    val year: Int,
    val plateNumber: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            com.example.fixv_demo.ui.theme.Fixv_demoTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "home"
                ) {
                    composable("home") {
                        HomeScreen(navController = navController)
                    }
                    composable("calendar") {
                        CalendarScreen(navController = navController)
                    }

                    composable("profile") {
                        ProfileScreen(
                            navController = navController
                        )
                    }

                    composable("add_vehicle") {
                        AddVehicleScreen(navController = navController)
                    }

                    composable("mechanicShops") {
                        MechanicShopsScreen(navController = navController)
                    }

                    composable("ScheduleAppointment") {
                        ScheduleAppointmentScreen(navController = navController)
                    }

                    composable(
                        route = "mechanic_shops/{documentId}",
                        arguments = listOf(navArgument("documentId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val shopId = backStackEntry.arguments?.getString("documentId")
                        Log.d("Navigation", "Navigating to details with shopId: $shopId")
                        if (shopId != null) {
                            MechanicShopDetailsScreen(
                                navController = navController,
                                shopId = shopId
                            )
                        } else {
                            // Handle the case where shopId is null
                            Text(text = "Invalid shop ID")
                        }
                    }


                    composable("editVehicleList") {
                        EditVehicleListScreen(navController = navController)
                    }

                    composable("addvehiclescreen") {
                        AddVehicleScreen(navController = navController)
                    }

                    composable(
                        route = "editVehicle/{vehicleJson}",
                        arguments = listOf(navArgument("vehicleJson") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val vehicleJson = backStackEntry.arguments?.getString("vehicleJson") ?: ""
                        val decodedJson = URLDecoder.decode(vehicleJson, "UTF-8")
                        val vehicle = Gson().fromJson(decodedJson, Vehicle::class.java)
                        EditVehicleScreen(navController = navController, vehicle = vehicle)
                    }

                    composable(
                        route = "EditVehicleScreen/{vehicleId}",
                        arguments = listOf(navArgument("vehicleId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val vehicleId = backStackEntry.arguments?.getString("vehicleId")

                        if (vehicleId != null) {
                            val db = FirebaseFirestore.getInstance()
                            var vehicle by remember { mutableStateOf<Vehicle?>(null) }

                            LaunchedEffect(vehicleId) {
                                db.collection("Vehicles").document(vehicleId)
                                    .get()
                                    .addOnSuccessListener { document ->
                                        document?.let {
                                            vehicle = Vehicle(
                                                id = it.id,
                                                make = it.getString("make") ?: "",
                                                model = it.getString("model") ?: "",
                                                mileage = it.getLong("mileage")?.toInt() ?: 0,
                                                year = it.getLong("year")?.toInt() ?: 0,
                                                plateNumber = it.getString("licensePlate") ?: ""
                                            )
                                        }
                                    }
                            }

                            vehicle?.let {
                                EditVehicleScreen(navController = navController, vehicle = it)
                            }
                        }
                    }

                    composable(
                        route = "appointmentConfirmation/{appointmentDetails}",
                        arguments = listOf(
                            navArgument("appointmentDetails") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val appointmentDetailsJson =
                            backStackEntry.arguments?.getString("appointmentDetails") ?: ""

                        AppointmentConfirmationScreen(
                            navController = navController,
                            appointmentDetailsJson = appointmentDetailsJson
                        )
                    }

                    composable(
                        route = "editappointmentscreen/{appointmentId}",
                        arguments = listOf(navArgument("appointmentId") {
                            type = NavType.StringType
                        })
                    ) { backStackEntry ->
                        val appointmentId = backStackEntry.arguments?.getString("appointmentId")
                        val db = FirebaseFirestore.getInstance()
                        var appointment by remember { mutableStateOf<Appointment?>(null) }

                        LaunchedEffect(appointmentId) {
                            if (appointmentId != null) {
                                db.collection("appointments").document(appointmentId)
                                    .get()
                                    .addOnSuccessListener { document ->
                                        document?.let { doc ->
                                            val status = doc.get("status") as? Map<String, Boolean>
                                                ?: emptyMap()
                                            val isConfirmed = status["confirmed"] ?: false

                                            // Get references
                                            val shopRef = doc.get("shopId") as? DocumentReference
                                            val vehicleRef =
                                                doc.get("vehicleId") as? DocumentReference
                                            val serviceRefs =
                                                (doc.get("services") as? List<*>)?.filterIsInstance<DocumentReference>()
                                                    ?: emptyList()

                                            // Create temporary appointment with IDs first
                                            val tempAppointment = Appointment(
                                                serviceType = emptyList(), // Will be populated later
                                                iconResource = R.drawable.ic_default_service,
                                                shopName = shopRef?.id ?: "Unknown Shop",
                                                date = doc.getString("date") ?: "No date",
                                                time = doc.getString("time") ?: "No time",
                                                vehicleDetails = vehicleRef?.id
                                                    ?: "Unknown Vehicle",
                                                licensePlate = "",
                                                invoiceId = (doc.get("transactionId") as? DocumentReference)?.id
                                                    ?: "",
                                                statusConfirmed = isConfirmed,
                                                appointmentId = doc.id
                                            )

                                            // Now fetch all the details
                                            CoroutineScope(Dispatchers.Main).launch {
                                                try {
                                                    // Fetch shop name
                                                    var shopName = "Unknown Shop"
                                                    if (shopRef != null) {
                                                        val shopDoc = shopRef.get().await()
                                                        if (shopDoc.exists()) {
                                                            shopName = shopDoc.getString("name")
                                                                ?: "Unknown Shop"
                                                        }
                                                    }

                                                    // Fetch vehicle details
                                                    var vehicleDetails = "Unknown Vehicle"
                                                    var licensePlate = ""
                                                    if (vehicleRef != null) {
                                                        val vehicleDoc = vehicleRef.get().await()
                                                        if (vehicleDoc.exists()) {
                                                            val make =
                                                                vehicleDoc.getString("make") ?: ""
                                                            val model =
                                                                vehicleDoc.getString("model") ?: ""
                                                            val year = vehicleDoc.getLong("year")
                                                                ?.toString() ?: ""
                                                            licensePlate =
                                                                vehicleDoc.getString("licensePlate")
                                                                    ?: ""
                                                            vehicleDetails = "$year $make $model"
                                                        }
                                                    }

                                                    // Fetch service names
                                                    val serviceNames = mutableListOf<String>()
                                                    for (serviceRef in serviceRefs) {
                                                        try {
                                                            val serviceDoc =
                                                                serviceRef.get().await()
                                                            if (serviceDoc.exists()) {
                                                                val serviceName =
                                                                    serviceDoc.getString("serviceName")
                                                                        ?: serviceRef.id
                                                                serviceNames.add(serviceName)
                                                            } else {
                                                                serviceNames.add(serviceRef.id)
                                                            }
                                                        } catch (e: Exception) {
                                                            serviceNames.add(serviceRef.id)
                                                        }
                                                    }

                                                    // Create final appointment with all details
                                                    appointment = tempAppointment.copy(
                                                        shopName = shopName,
                                                        vehicleDetails = vehicleDetails,
                                                        licensePlate = licensePlate,
                                                        serviceType = serviceNames
                                                    )
                                                } catch (e: Exception) {
                                                    Log.e(
                                                        "EditAppointment",
                                                        "Error fetching details",
                                                        e
                                                    )
                                                    // Use the basic appointment if details fail to load
                                                    appointment = tempAppointment
                                                }
                                            }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("EditAppointment", "Error loading appointment", e)
                                    }
                            }
                        }

                        appointment?.let {
                            EditAppointmentScreen(
                                navController = navController,
                                appointment = it
                            )
                        } ?: run {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }

                    composable(
                        route = "payment/{appointmentDetails}",
                        arguments = listOf(
                            navArgument("appointmentDetails") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val appointmentDetailsJson =
                            backStackEntry.arguments?.getString("appointmentDetails") ?: ""
                        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                        PaymentScreen(
                            navController = navController,
                            appointmentDetailsJson = appointmentDetailsJson,
                            currentUserId = currentUserId,
                            onBackClick = { navController.popBackStack() },
                            onPaymentComplete = { /* Handle payment completion */ }
                        )
                    }

                    composable(
                        route = "invoice/{invoiceJson}",
                        arguments = listOf(navArgument("invoiceJson") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val invoiceJson = backStackEntry.arguments?.getString("invoiceJson") ?: ""
                        InvoiceScreen(
                            invoiceJson = invoiceJson
                        )
                    }

                    composable("viewAppointments") {
                        AppointmentsScreen(
                            navController = navController,
                            onLogoutClick = { /* Handle logout */ })
                    }

                    composable("appointments") {
                        AppointmentsScreen(
                            navController,
                            onLogoutClick = { /* Handle logout */ })
                    }
                    composable(
                        route = "appointmentDetails/{shopName}/{date}/{time}/{vehicleDetails}/{serviceDetails}/{licensePlate}/{invoiceId}",
                        arguments = listOf(
                            navArgument("shopName") { type = NavType.StringType },
                            navArgument("date") { type = NavType.StringType },
                            navArgument("time") { type = NavType.StringType },
                            navArgument("vehicleDetails") { type = NavType.StringType },
                            navArgument("serviceDetails") { type = NavType.StringType },
                            navArgument("licensePlate") { type = NavType.StringType },
                            navArgument("invoiceId") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val shopName =
                            backStackEntry.arguments?.getString("shopName") ?: "Unknown Shop"
                        val date = backStackEntry.arguments?.getString("date") ?: "Unknown Date"
                        val time = backStackEntry.arguments?.getString("time") ?: "Unknown Time"
                        val vehicleDetails = backStackEntry.arguments?.getString("vehicleDetails")
                            ?: "Unknown Vehicle"
                        val serviceDetails = backStackEntry.arguments?.getString("serviceDetails")
                            ?: "Unknown Service"
                        val licensePlate =
                            backStackEntry.arguments?.getString("licensePlate") ?: "Unknown"
                        val invoiceId =
                            backStackEntry.arguments?.getString("invoiceId") ?: "Unknown"

                        AppointmentDetailsScreen(
                            shopName = shopName,
                            date = date,
                            time = time,
                            vehicleDetails = URLDecoder.decode(vehicleDetails, "UTF-8"),
                            serviceDetails = URLDecoder.decode(serviceDetails, "UTF-8"),
                            licensePlate = URLDecoder.decode(licensePlate, "UTF-8"),
                            invoiceId = invoiceId,
                            navController = navController
                        )
                    }
                    composable(
                        route = "invoice/{invoiceJson}",
                        arguments = listOf(navArgument("invoiceJson") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val invoiceJson = backStackEntry.arguments?.getString("invoiceJson")
                        InvoiceScreen(invoiceJson)
                    }

                    composable("manageAppointmentsScreen") {
                        ManageAppointmentsScreen(navController = navController)
                    }
                }
            }
        }
    }

    @Composable
    fun HomeScreen(navController: NavController) {
        val mainViewModel: MainViewModel = viewModel()
        val context = LocalContext.current

        var selectedItem by remember { mutableIntStateOf(0) }
        val isLoading by mainViewModel.isLoading.collectAsState()
        var showLogoutDialog by remember { mutableStateOf(false) }

        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 42.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Home text
                Text(
                    text = "Home",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 10.dp),
                    textAlign = TextAlign.Center
                )

                // Container for evenly spaced cards
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(48.dp)
                ) {
                    // Action Cards
                    ActionCard(
                        icon = {
                            Image(
                                painter = painterResource(id = R.drawable.mdi__mechanic),
                                contentDescription = "Car Repair Icon"
                            )
                        },
                        title = "Mechanic Shops",
                        subtitle = "View Shops available",
                        borderColor = Color.Gray,
                        borderWidth = 0.7.dp,
                        onClick = { navController.navigate("mechanicShops") }
                    )

                    ActionCard(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Schedule Icon",
                                modifier = Modifier.size(32.dp),
                                tint = Color.Black
                            )
                        },
                        title = "Schedule Appointment",
                        subtitle = "Make an appointment",
                        borderColor = Color.Gray,
                        borderWidth = 0.7.dp,
                        onClick = { navController.navigate("ScheduleAppointment") }
                    )

                    ActionCard(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Appointments Icon",
                                modifier = Modifier.size(32.dp),
                                tint = Color.Black
                            )
                        },
                        title = "View Appointments",
                        subtitle = "Edit or cancel appointments",
                        borderColor = Color.Gray,
                        borderWidth = 0.7.dp,
                        onClick = { navController.navigate("viewAppointments") }
                    )
                }
            }
        }
    }

    @Composable
    fun ActionCard(
        icon: (@Composable () -> Unit)? = null,
        title: String,
        subtitle: String,
        onClick: () -> Unit,
        borderColor: Color = Color.Gray,
        borderWidth: Dp = 1.dp
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            ),
            border = BorderStroke(borderWidth, borderColor),
            onClick = onClick
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Icon in center
                icon?.let {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        it()
                    }
                }

                // Title and subtitle in center
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}
