package com.example.fixv_demo

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun logout(context: Context, onLogoutComplete: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {

                // Sign out from Firebase
                FirebaseAuth.getInstance().signOut()

                // Clear SharedPreferences
                val sharedPrefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                sharedPrefs.edit().clear().apply()

                // Clear any stored tokens or user data
                clearUserData(context)

                // Notify logout complete
                onLogoutComplete()
            } catch (e: Exception) {
                // Handle any errors during logout
                Log.e("Logout", "Error during logout", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun clearUserData(context: Context) {
        // Clear any cached files
        context.cacheDir.deleteRecursively()

        // Clear any other stored data (e.g., database)
        // Add any other data clearing logic specific to your app here
    }
}