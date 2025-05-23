package com.example.fixv_demo.ui

import com.example.fixv_demo.R

import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Log

data class LoginFormState(
    val username: String = "",
    val password: String = "",
    val usernameError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val loginError: String? = null
)

@Composable
fun LoginScreen(auth: FirebaseAuth, navController: NavController) {
    var formState by remember { mutableStateOf(LoginFormState()) }
    var passwordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun handleSignInAttempt() {
        // Validate form and set immediate errors
        var hasErrors = false

        // Username validation
        val usernameError = when {
            formState.username.isEmpty() -> {
                hasErrors = true
                "Email is required"
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(formState.username).matches() -> {
                hasErrors = true
                "Please enter a valid email"
            }
            else -> null
        }

        // Password validation
        val passwordError = when {
            formState.password.isEmpty() -> {
                hasErrors = true
                "Password is required"
            }
            formState.password.length < 6 -> {
                hasErrors = true
                "Password must be at least 6 characters"
            }
            else -> null
        }

        // Update form state with validation errors
        formState = formState.copy(
            usernameError = usernameError,
            passwordError = passwordError
        )

        // If validation passes, attempt sign in
        if (!hasErrors) {
            scope.launch {
                formState = formState.copy(isLoading = true, loginError = null)
                try {
                    auth.signInWithEmailAndPassword(formState.username, formState.password)
                        .await()
                    navController.navigate("home")
                    (context as? ComponentActivity)?.finish()
                } catch (e: Exception) {
                    val errorMessage = when (e) {
                        is FirebaseAuthInvalidUserException -> "No account found with this email"
                        is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password"
                        else -> "Failed to sign in. Please check your internet connection and try again."
                    }
                    formState = formState.copy(loginError = errorMessage)
                    Log.e("LoginScreen", "Error logging in: $e")
                } finally {
                    formState = formState.copy(isLoading = false)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Logo
        Image(
            painter = painterResource(id = R.drawable.fixv_logo),
            contentDescription = "FIXV Logo",
            modifier = Modifier.size(112.dp)
        )

        Text(
            text = "Login",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier
                .padding(bottom = 85.dp, top = 35.dp)
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
                    loginError = null
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
                cursorColor = Color.Black,
                errorBorderColor = MaterialTheme.colorScheme.error
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
                    loginError = null
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
                cursorColor = Color.Black,
                errorBorderColor = MaterialTheme.colorScheme.error
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { handleSignInAttempt() },
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
