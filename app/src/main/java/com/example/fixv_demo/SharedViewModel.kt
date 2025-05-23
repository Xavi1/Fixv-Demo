package com.example.fixv_demo

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SharedViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Idle)
    val paymentState: StateFlow<PaymentState> = _paymentState

    private val _invoiceDetails = MutableStateFlow<InvoiceDetails?>(null)
    val invoiceDetails: StateFlow<InvoiceDetails?> = _invoiceDetails

    fun processPayment(
        appointmentDetails: AppointmentDetails,
        selectedPaymentMethod: String,
        currentUserId: String,
        serviceIdMap: Map<String, String>
    ) {
        _paymentState.value = PaymentState.Loading
        viewModelScope.launch {
            try {
                val userRef = firestore.collection("Users").document(currentUserId)
                val shopRef = firestore.collection("mechanic_shops").document(appointmentDetails.shopId)
                val vehicleRef = firestore.collection("Vehicles").document(appointmentDetails.vehicleId)

                val serviceRefs = appointmentDetails.services.mapNotNull { serviceName ->
                    val serviceId = serviceIdMap[serviceName]
                    if (serviceId != null) {
                        firestore.collection("services").document(serviceId)
                    } else {
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
                    val vehicleSnapshot = transaction.get(vehicleRef)

                    val userName = userSnapshot.getString("name") ?: "Unknown User"
                    val shopName = shopSnapshot.getString("name") ?: "Unknown Shop"

                    val paymentTransaction = hashMapOf(
                        "totalPrice" to appointmentDetails.totalCost,
                        "paymentMethod" to selectedPaymentMethod,
                        "serviceTypes" to serviceRefs,
                        "userId" to userRef,
                        "shopId" to shopRef,
                        "createdAt" to Timestamp.now(),
                        "payment_status" to "pending",
                        "invoiceId" to invoiceRef,
                        "transactionId" to paymentTransactionRef.id
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

                    InvoiceDetails(
                        invoiceId = invoiceRef.id,
                        totalAmount = appointmentDetails.totalCost,
                        userName = userName,
                        paymentStatus = "pending",
                        appointmentDate = appointmentDetails.date,
                        createdAt = SimpleDateFormat("EEE MMM dd", Locale.getDefault()).format(Date()),
                        shopName = shopName,
                        services = appointmentDetails.services,
                        vehicleDetails = appointmentDetails.vehicleDetails,
                        shopId = appointmentDetails.shopId,
                        servicePrices = appointmentDetails.servicePrices
                    )
                }.addOnSuccessListener { invoiceDetails ->
                    _invoiceDetails.value = invoiceDetails
                    _paymentState.value = PaymentState.Success(invoiceDetails)
                }.addOnFailureListener { e ->
                    _paymentState.value = PaymentState.Error(e.message ?: "Payment failed")
                }
            } catch (e: Exception) {
                _paymentState.value = PaymentState.Error(e.message ?: "Payment failed")
            }
        }
    }

    fun fetchInvoiceDetails(invoiceId: String) {
        _paymentState.value = PaymentState.Loading
        viewModelScope.launch {
            firestore.collection("invoices").document(invoiceId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val createdAtTimestamp = document.getTimestamp("createdAt") ?: Timestamp.now()
                        val createdAtFormatted = SimpleDateFormat("EEE MMM dd", Locale.getDefault()).format(createdAtTimestamp.toDate())
                        val amountDue = document.getDouble("amount_due") ?: 0.0
                        val serviceNames = document.get("serviceNames") as? List<String> ?: emptyList()
                        val appointmentId = document.getDocumentReference("appointmentId")?.id

                        val userRef = document.getDocumentReference("userId")
                        val shopRef = document.getDocumentReference("shopId")
                        val vehicleRef = document.getDocumentReference("vehicleId")
                        val serviceRefs = document.get("services") as? List<DocumentReference> ?: emptyList()

                        if (userRef != null && shopRef != null && vehicleRef != null && appointmentId != null) {
                            userRef.get().addOnSuccessListener { userDoc ->
                                shopRef.get().addOnSuccessListener { shopDoc ->
                                    vehicleRef.get().addOnSuccessListener { vehicleDoc ->
                                        firestore.collection("appointments").document(appointmentId).get()
                                            .addOnSuccessListener { appointmentDoc ->
                                                val appointmentDate = appointmentDoc.getString("date") ?: "Unknown"

                                                val userName = userDoc.getString("name") ?: "Unknown User"
                                                val shopName = shopDoc.getString("name") ?: "Unknown Shop"
                                                val vehicleDetails = "${vehicleDoc.getString("model") ?: "Unknown Vehicle"} ${vehicleDoc.getString("make") ?: "Unknown Make"} ${vehicleDoc.getString("licensePlate") ?: "Unknown License Plate"}"

                                                val servicePrices = mutableMapOf<String, Double>()
                                                val serviceFetchTasks = mutableListOf<com.google.android.gms.tasks.Task<*>>()

                                                // Fetch prices for each service
                                                serviceRefs.forEachIndexed { index, serviceRef ->
                                                    val task = serviceRef.get().continueWithTask { serviceTask ->
                                                        val serviceDoc = serviceTask.result
                                                        val serviceId = serviceDoc.id
                                                        val serviceName = serviceNames.getOrNull(index) ?: "Unknown Service"
                                                        Log.d("ServicePrice", "service: $serviceId, name: $serviceName")

                                                        // Get price from shop_services using references
                                                        firestore.collection("shop_services")
                                                            .whereEqualTo("serviceId", serviceRef)
                                                            .whereEqualTo("shopId", shopRef)
                                                            .get()
                                                            .continueWith { querySnapshotTask ->
                                                                val querySnapshot = querySnapshotTask.result
                                                                Log.d("ServicePrice", "querySnapshot size: ${querySnapshot.size()}")
                                                                if (!querySnapshot.isEmpty) {
                                                                    val price = querySnapshot.documents[0].getDouble("price") ?: 0.0
                                                                    Log.d("ServicePrice", "price: $price")
                                                                    servicePrices[serviceName] = price
                                                                } else {
                                                                    Log.d("ServicePrice", "querySnapshot is empty")
                                                                }
                                                            }
                                                    }
                                                    serviceFetchTasks.add(task)
                                                }

                                                Tasks.whenAllComplete(serviceFetchTasks).addOnCompleteListener {
                                                    // Ensure we have all prices before creating InvoiceDetails
                                                    val invoiceDetails = InvoiceDetails(
                                                        invoiceId = invoiceId,
                                                        totalAmount = amountDue,
                                                        userName = userName,
                                                        paymentStatus = document.getString("payment_status") ?: "Pending",
                                                        appointmentDate = appointmentDate,
                                                        createdAt = createdAtFormatted,
                                                        shopName = shopName,
                                                        services = serviceNames,
                                                        vehicleDetails = vehicleDetails,
                                                        shopId = shopRef.id,
                                                        servicePrices = servicePrices
                                                    )
                                                    _invoiceDetails.value = invoiceDetails
                                                    _paymentState.value = PaymentState.Success(invoiceDetails)
                                                }
                                            }
                                    }
                                }
                            }
                        } else {
                            _paymentState.value = PaymentState.Error("One or more required references are null")
                        }
                    } else {
                        _paymentState.value = PaymentState.Error("Invoice not found")
                    }
                }
                .addOnFailureListener { e ->
                    _paymentState.value = PaymentState.Error(e.message ?: "Error fetching invoice details")
                }
        }
    }

    fun resetPaymentState() {
        _paymentState.value = PaymentState.Idle
    }
}

sealed class PaymentState {
    object Idle : PaymentState()
    object Loading : PaymentState()
    data class Success(val invoiceDetails: InvoiceDetails) : PaymentState()
    data class Error(val message: String) : PaymentState()
}
