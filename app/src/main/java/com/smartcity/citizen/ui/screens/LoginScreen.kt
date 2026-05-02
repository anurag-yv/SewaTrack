package com.smartcity.citizen.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Grey colour palette (no blue)
private val GreyBg = Color(0xFFF5F5F5)
private val GreyBorder = Color(0xFFE0E0E0)
private val GreyTextDark = Color(0xFF424242)
private val GreyTextMid = Color(0xFF757575)
private val GreyButton = Color(0xFF616161)

@Composable
fun LoginScreen(
    navController: NavHostController,
    onLoginSuccess: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GreyBg)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Login",
            style = MaterialTheme.typography.headlineLarge,
            color = GreyTextDark
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email", color = GreyTextMid) },
            leadingIcon = { Icon(Icons.Default.Email, null, tint = GreyTextMid) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GreyBorder,
                unfocusedBorderColor = GreyBorder,
                focusedLabelColor = GreyTextMid,
                cursorColor = GreyTextDark,
                focusedTextColor = GreyTextDark,
                unfocusedTextColor = GreyTextDark
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password", color = GreyTextMid) },
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = GreyTextMid) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle password",
                        tint = GreyTextMid
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GreyBorder,
                unfocusedBorderColor = GreyBorder,
                focusedLabelColor = GreyTextMid,
                cursorColor = GreyTextDark,
                focusedTextColor = GreyTextDark,
                unfocusedTextColor = GreyTextDark
            )
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Plain Button with grey background (no gradient, no blue)
        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    Toast.makeText(context, "Please enter email and password", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                scope.launch {
                    isLoading = true
                    try {
                        val result = auth.signInWithEmailAndPassword(email, password).await()
                        val user = result.user
                        if (user != null) {
                            val doc = firestore.collection("users").document(user.uid).get().await()
                            if (doc.exists()) {
                                onLoginSuccess()
                            } else {
                                auth.signOut()
                                Toast.makeText(context, "Account not found. Please register.", Toast.LENGTH_LONG).show()
                                navController.navigate("register")
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = GreyButton,
                contentColor = Color.White
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
            } else {
                Text("Log In")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { navController.navigate("register") }) {
            Text("Don't have an account? Register", color = GreyTextDark)
        }
    }
}