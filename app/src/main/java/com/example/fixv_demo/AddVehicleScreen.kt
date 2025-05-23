package com.example.fixv_demo

import android.util.Log
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@Composable
fun PlateNumberField(plateNumber: String, onValueChange: (String) -> Unit) {
    var isValid by remember { mutableStateOf(true) }

    OutlinedTextField(
        value = plateNumber.uppercase(),
        onValueChange = { input ->
            val cleanedInput = input.uppercase().replace(" ", "") // Remove existing spaces

            val lettersPart = cleanedInput.take(3).filter { it.isLetter() }  // First 3 chars must be letters
            val numbersPart = cleanedInput.drop(3).take(4).filter { it.isDigit() } // Last 4 must be numbers

            val formattedText = buildString {
                append(lettersPart)
                if (lettersPart.length == 3) append(" ") // Add space after third letter
                append(numbersPart)
            }

            if (formattedText.length <= 8) { // ABC 1234 format limit
                onValueChange(formattedText)
                isValid = formattedText.matches(Regex("^[A-Z]{3} \\d{0,4}$")) || formattedText.isEmpty()
            }
        },
        label = { Text("Enter Plate Number (ABC 1234)") },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text, // Still allows both but input is filtered
            imeAction = ImeAction.Done
        ),
        visualTransformation = PlateNumberTransformation(),
        isError = !isValid,
        supportingText = {
            if (!isValid) {
                Text("Invalid format! Use: ABC 1234", color = Color.Red)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color.LightGray,
            focusedBorderColor = Color.Black,
            cursorColor = Color.Black,
            focusedLabelColor = Color.Black,
            unfocusedLabelColor = Color.Gray
        ),
        shape = RoundedCornerShape(8.dp)
    )
}


// Formatting Transformation (Ensures space at the 4th character)
class PlateNumberTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text.uppercase() // Convert entire input to uppercase
        val transformedText = buildString {
            originalText.forEachIndexed { index, char ->
                if (index == 3) append(" ") // Insert space after third character
                append(char)
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return when {
                    offset >= originalText.length -> transformedText.length // Prevent out-of-bounds
                    offset > 2 -> offset + 1 // Add space shift
                    else -> offset
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                return when {
                    offset >= transformedText.length -> originalText.length // Prevent out-of-bounds
                    offset > 3 -> offset - 1
                    else -> offset
                }
            }
        }

        return TransformedText(AnnotatedString(transformedText), offsetMapping)
    }
}



@Composable
fun AddVehicleScreen(
    navController: NavController
) {
    var vehicleName by remember { mutableStateOf("") }
    var vehicleModel by remember { mutableStateOf("") }
    var vehicleMileage by remember { mutableStateOf("") }
    var plateNumber by remember { mutableStateOf("") }
    var vehicleYear by remember { mutableStateOf("") }
    var selectedItem by remember { mutableIntStateOf(0) }

    val db = FirebaseFirestore.getInstance()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                onLogoutClick = { /* No logout button needed here */ },
                showLogoutButton = false
            )
        },
        bottomBar = {
            BottomNavBar(
                navController = navController,
                selectedItem = selectedItem,
                onItemSelected = { selectedItem = it }
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { snackbarData: SnackbarData ->
                Snackbar(
                    snackbarData = snackbarData,
                    modifier = Modifier.padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    containerColor = Color.Gray,
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
                .padding(horizontal = 16.dp)
        ) {
            // Back button and title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp,bottom = 16.dp,end = 16.dp)
                    ,horizontalArrangement = Arrangement.Center
            ) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Image(
                        painter = painterResource(id = R.drawable.back_arrow),
                        contentDescription = "Back",
                        modifier = Modifier
                            .size(32.dp),

                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Add Vehicle",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(5f)
                )
            }

            // Form Card
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                    ,horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val textFieldColors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.LightGray,
                        focusedBorderColor = Color.Black,
                        cursorColor = Color.Black,
                        focusedLabelColor = Color.Black,
                        unfocusedLabelColor = Color.Gray
                    )

                    // Vehicle Name Field
                    OutlinedTextField(
                        value = vehicleName,
                        onValueChange = { vehicleName = it },
                        label = { Text("Enter Vehicle Name:") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors,
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Vehicle Model Field
                    OutlinedTextField(
                        value = vehicleModel,
                        onValueChange = { vehicleModel = it },
                        label = { Text("Enter Vehicle Model:") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors,
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Vehicle Mileage Field
                    OutlinedTextField(
                        value = vehicleMileage,
                        onValueChange = { input ->
                                // Filter out non-numeric characters
                                val filteredInput = input.filter { it.isDigit() }

                                // Optionally limit the length of the input
                                if (filteredInput.length <= 10) { // Example: Limit to 10 digits
                                    vehicleMileage = filteredInput
                                }
                            },
                        label = { Text("Enter Vehicle Mileage:") },
                        modifier = Modifier
                            .fillMaxWidth(),
                        colors = textFieldColors,
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Vehicle Year Field
                    OutlinedTextField(
                        value = vehicleYear,
                        onValueChange = { input ->
                            // Filter out non-numeric characters
                           val filteredInput = input.filter { it.isDigit() }

                            // Limit to 4 characters
                            if(filteredInput.length <= 4){

                                vehicleYear = filteredInput
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true,
                        label = { Text("Enter Vehicle Year:") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors,
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Plate Number Field
                    PlateNumberField(plateNumber, onValueChange = { plateNumber = it })

                    // Submit Button
                    Button(
                        onClick = {
                            if (vehicleName.isEmpty() || vehicleModel.isEmpty() || vehicleMileage.isEmpty() || vehicleYear.isEmpty() || plateNumber.isEmpty()) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Please fill in all fields", duration = androidx.compose.material3.SnackbarDuration.Short, actionLabel = "OK")
                                }
                            } else if (plateNumber.length != 8 || !plateNumber.matches(Regex("^[A-Z]{3} \\d{4}\$"))) {
                                coroutineScope.launch {snackbarHostState.showSnackbar("Plate number must be in the format ABC 1234", duration = androidx.compose.material3.SnackbarDuration.Short, actionLabel = "OK")
                                }
                            }
                            else {
                                val auth = Firebase.auth

                                auth.currentUser?.let { user ->  // Using safe call with let

                                    val vehicleData = mapOf(
                                        "make" to vehicleName,
                                        "model" to vehicleModel,
                                        "licensePlate" to plateNumber,
                                        "createdAt" to com.google.firebase.Timestamp.now(),
                                        "mileage" to (vehicleMileage.toIntOrNull() ?: 0),
                                        "year" to (vehicleYear.toIntOrNull() ?: 0),
                                        "userId" to user.uid  // Now we safely use user.uid
                                    )

                                    db.collection("Vehicles")
                                        .add(vehicleData)
                                        .addOnSuccessListener { documentReference ->
                                            val vehicleId = documentReference.id
                                            db.collection("Vehicles").document(vehicleId)
                                                .update("vehicleId", vehicleId)
                                                .addOnSuccessListener {
                                                    coroutineScope.launch {
                                                        val result = snackbarHostState.showSnackbar(
                                                            message = "Vehicle added successfully!",
                                                            duration = androidx.compose.material3.SnackbarDuration.Short,
                                                            actionLabel = "OK",
                                                        )
                                                        if (result == SnackbarResult.ActionPerformed) {
                                                            navController.navigateUp()
                                                        }
                                                    }
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.w("Firestore", "Error adding document", e)
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("Error: ${e.message}")
                                                    }
                                                }
                                        }
                                } ?: run {
                                    // Handle case where user is null
                                    Log.w("Auth", "User not logged in")
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("User not logged in")
                                            }
                                    }
                            }

                        },
                        modifier = Modifier
                            .width(150.dp)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Submit", color = Color.White)
                    }
                }
            }
        }
    }
}