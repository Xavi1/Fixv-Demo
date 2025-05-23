package com.example.fixv_demo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class LoginFormState(
    val username: String = "",
    val password: String = "",
    val usernameError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val loginError: String? = null  // New field for login-specific errors
)

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.White
            ) {
                LoginScreen(auth = auth)
            }
        }
    }
}

val auth = FirebaseAuth.getInstance()



fun linkAuthUserToFirestore(userId: String) {
    val db = Firebase.firestore
    val user = auth.currentUser  // ✅ Get currently logged-in user
    if (user != null) {
        val userRef = db.collection("Users").document(userId)

        userRef.get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {  // ✅ If user doesn't exist in Firestore, create a new document
                    val userData = mapOf(
                        "uid" to user.uid,
                        "name" to (user.displayName ?: user.email?.substringBefore("@") ?: "New User"),
                        "email" to (user.email ?: "No Email")
                    )
                    userRef.set(userData)
                        .addOnSuccessListener { Log.d("Firestore", "User added to Firestore.") }
                        .addOnFailureListener { Log.e("Firestore", "Failed to add user", it) }
                }
            }
            .addOnFailureListener { Log.e("Firestore", "Error checking user", it) }
    }
}

@Composable
fun LoginScreen(auth: FirebaseAuth) {
    var formState by remember { mutableStateOf(LoginFormState()) }
    var passwordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun validateForm(): Boolean {
        val usernameError = when {
            formState.username.isEmpty() -> "Email is required"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(formState.username).matches() ->
                "Please enter a valid email"
            else -> null
        }

        val passwordError = when {
            formState.password.isEmpty() -> "Password is required"
            formState.password.length < 6 -> "Password must be at least 6 characters"
            else -> null
        }

        formState = formState.copy(
            usernameError = usernameError,
            passwordError = passwordError,
            loginError = null  // Clear any previous login errors
        )

        return usernameError == null && passwordError == null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 35.dp),
        horizontalAlignment = Alignment.Start
    ) {
        //Spacer(modifier = Modifier.height(50.dp))

        // Logo
        Image(
            painter = painterResource(id = R.drawable.fixv_logo),
            contentDescription = "FIXV Logo",
            modifier = Modifier.size(120.dp)
        )

        Text(
            text = "Login",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier
                .padding(bottom = 32.dp)
                .padding(top = 55.dp)
                .align(Alignment.CenterHorizontally)
        )

        // Show login error if present
        formState.loginError?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }

        OutlinedTextField(
            value = formState.username,
            onValueChange = {
                formState = formState.copy(
                    username = it,
                    usernameError = null,
                    loginError = null  // Clear login error when user types
                )
            },
            placeholder = { Text("Enter your email", color = Color.Gray) },
            isError = formState.usernameError != null || formState.loginError != null,
            supportingText = {
                formState.usernameError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.LightGray,
                focusedBorderColor = Color.Black,
                cursorColor = Color.Black
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = formState.password,
            onValueChange = {
                formState = formState.copy(
                    password = it,
                    passwordError = null,
                    loginError = null  // Clear login error when user types
                )
            },
            placeholder = { Text("Enter your password", color = Color.Gray) },
            isError = formState.passwordError != null || formState.loginError != null,
            supportingText = {
                formState.passwordError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.LightGray,
                focusedBorderColor = Color.Black,
                cursorColor = Color.Black
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                Log.d("LoginActivity", "Attempting login for ${formState.username}")
                if (validateForm()) {
                    scope.launch {
                        formState = formState.copy(isLoading = true)
                        try {
                            val result = auth.signInWithEmailAndPassword(formState.username, formState.password).await()
                            val user = result.user
                            if (user != null) {
                                linkAuthUserToFirestore(user.uid)  // ✅ Ensure user is linked to Firestore
                                context.startActivity(Intent(context, MainActivity::class.java))
                                (context as? ComponentActivity)?.finish()
                            }
                        } catch (e: Exception) {
                            Log.e("LoginScreen", "Error logging in: $e")
                        } finally {
                            formState = formState.copy(isLoading = false)
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White
            ),
            enabled = !formState.isLoading
        ) {
            if (formState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White
                )
            } else {
                Text("Sign in", fontSize = 16.sp)
            }
        }
    }
}