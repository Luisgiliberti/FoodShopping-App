package com.example.foodshopping

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun RegisterScreenView(
    onRegister: (String, String, String) -> Unit,
    onNavigateToLogin: () -> Unit,
    errorMessage: String?
) {
    val username = remember { mutableStateOf("") }
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val confirmPassword = remember { mutableStateOf("") }
    val localErrorMessage = remember { mutableStateOf<String?>(null) }
    val emailPattern = "^[a-z0-9+_.-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$".toRegex()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFC8E6C9),
                        Color(0xFFBBDEFB),
                        Color(0xFFFFF176)
                    )
                )
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Register",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = username.value,
                onValueChange = { username.value = it },
                label = { Text("Username", color = Color.Black) },
                textStyle = TextStyle(color = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = email.value,
                onValueChange = { email.value = it },
                label = { Text("Email", color = Color.Black) },
                textStyle = TextStyle(color = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password.value,
                onValueChange = { password.value = it },
                label = { Text("Password (min 6 chars)", color = Color.Black) },
                textStyle = TextStyle(color = Color.Black),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = confirmPassword.value,
                onValueChange = { confirmPassword.value = it },
                label = { Text("Confirm Password", color = Color.Black) },
                textStyle = TextStyle(color = Color.Black),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    when {
                        username.value.isEmpty() -> localErrorMessage.value = "Username is required."
                        email.value.isEmpty() -> localErrorMessage.value = "Email is required."
                        !email.value.matches(emailPattern) -> localErrorMessage.value = "Please enter a valid email address."
                        password.value.length < 6 -> localErrorMessage.value = "Password must be at least 6 characters long."
                        password.value != confirmPassword.value -> localErrorMessage.value = "Passwords do not match."
                        else -> {
                            localErrorMessage.value = null
                            onRegister(username.value, email.value, password.value)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF445E91)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Text("Register", color = Color.White)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Already have an account? Click here",
                color = Color.Black,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clickable {
                        onNavigateToLogin()
                    }
            )

            localErrorMessage.value?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = error, color = Color.Red)
            }

            errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = error, color = Color.Red)
            }
        }
    }
}
