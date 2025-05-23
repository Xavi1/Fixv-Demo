package com.example.fixv_demo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Log

class MechanicShopViewModel : ViewModel() {
    private val _selectedShop = MutableStateFlow<Map<String, Any>?>(null)
    val selectedShop = _selectedShop.asStateFlow()

    private val _services = MutableStateFlow<List<String>>(emptyList())
    val services = _services.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _openingHours = MutableStateFlow<List<Map<String, String>>>(emptyList())
    val openingHours = _openingHours.asStateFlow()

    fun fetchShopDetails(shopId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val db = FirebaseFirestore.getInstance()
                val shopDoc = db.collection("mechanic_shops").document(shopId)
                val shopSnapshot = shopDoc.get().await()
                Log.d("MechanicShopViewModel", "Shop document data: ${shopSnapshot.data}")

                _selectedShop.value = shopSnapshot.data

                // Fetch services from shop_services collection
                val shopServicesQuery = db.collection("shop_services")
                    .whereEqualTo("shopId", shopId) // Ensure shopId format is correct
                    .get()
                    .await()

                Log.d("MechanicShopViewModel", "Fetched shop_services count: ${shopServicesQuery.documents.size}")
                for (document in shopServicesQuery.documents) {
                    Log.d("MechanicShopViewModel", "shop_services document: ${document.data}")
                }


                val servicesList = mutableListOf<String>()
                val servicesOffered = shopSnapshot.get("servicesOffered") as? List<DocumentReference>

                if (servicesOffered != null) {
                    for (serviceRef in servicesOffered) {
                        try {
                            val serviceDoc = serviceRef.get().await()
                            val serviceName = serviceDoc.getString("serviceName")
                            if (serviceName != null) {
                                servicesList.add(serviceName)
                                Log.d("MechanicShopViewModel", "Added service: $serviceName")
                            }
                        } catch (e: Exception) {
                            Log.e("MechanicShopViewModel", "Error fetching service", e)
                        }
                    }
                }

                _services.value = servicesList
                Log.d("MechanicShopViewModel", "Final services list after fetch: $servicesList")

                _services.value = servicesList
                Log.d("MechanicShopViewModel", "Final services list: $servicesList")

                // Get opening hours from shop document
                val openingHoursData = shopSnapshot.get("openingHours")
                _openingHours.value = convertOpeningHours(openingHoursData)

            } catch (e: Exception) {
                Log.e("MechanicShopViewModel", "Error loading services", e)
                _errorMessage.value = "Error loading services: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Define the convertOpeningHours function here or move it to a common utility file
    private fun convertOpeningHours(openingHoursData: Any?): List<Map<String, String>> {
        val result = mutableListOf<Map<String, String>>()

        when (openingHoursData) {
            is Map<*, *> -> {
                openingHoursData.forEach { (day, data) ->
                    when (data) {
                        is Map<*, *> -> {
                            val dayName = day.toString()
                            val closed = data["closed"] as? Boolean ?: false

                            if (closed) {
                                result.add(mapOf(
                                    "day" to dayName,
                                    "hours" to "Closed"
                                ))
                            } else {
                                val open = data["open"] as? String ?: ""
                                val close = data["close"] as? String ?: ""
                                result.add(mapOf(
                                    "day" to dayName,
                                    "hours" to "$open - $close"
                                ))
                            }
                        }
                        is String -> {
                            result.add(mapOf(
                                "day" to day.toString(),
                                "hours" to data
                            ))
                        }
                    }
                }
            }
            is List<*> -> {
                openingHoursData.forEach { item ->
                    if (item is Map<*, *>) {
                        val dayMap = mutableMapOf<String, String>()
                        item.forEach { (key, value) ->
                            if (key is String && value is String) {
                                dayMap[key] = value
                            }
                        }
                        if (dayMap.isNotEmpty()) {
                            result.add(dayMap)
                        }
                    }
                }
            }
        }

        return result
    }
}