package com.smartcity.citizen.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
fun RegisterScreen(
    navController: NavHostController,
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("citizen") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var roleDropdownExpanded by remember { mutableStateOf(false) }
    val roles = listOf("citizen", "employee", "admin")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GreyBg)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Create Account",
            style = MaterialTheme.typography.headlineLarge,
            color = GreyTextDark
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name", color = GreyTextMid) },
            leadingIcon = { Icon(Icons.Default.Person, null, tint = GreyTextMid) },
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
                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        "Toggle password",
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
        Spacer(modifier = Modifier.height(8.dp))

        // Role selection (grey outline)
        Box {
            OutlinedButton(
                onClick = { roleDropdownExpanded = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GreyTextDark),
                border = BorderStroke(1.dp, GreyBorder)
            ) {
                Text("Role: ${role.replaceFirstChar { it.uppercase() }}", color = GreyTextDark)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ArrowDropDown, null, tint = GreyTextMid)
            }
            DropdownMenu(
                expanded = roleDropdownExpanded,
                onDismissRequest = { roleDropdownExpanded = false },
                containerColor = Color.White
            ) {
                roles.forEach { r ->
                    DropdownMenuItem(
                        text = { Text(r.replaceFirstChar { it.uppercase() }, color = GreyTextDark) },
                        onClick = {
                            role = r
                            roleDropdownExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Plain Button with grey background
        Button(
            onClick = {
                if (name.isBlank() || email.isBlank() || password.isBlank()) {
                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (isLoading) return@Button
                isLoading = true
                scope.launch {
                    try {
                        val result = auth.createUserWithEmailAndPassword(email, password).await()
                        val userId = result.user?.uid ?: throw Exception("User creation failed")
                        val userMap = hashMapOf(
                            "name" to name,
                            "email" to email,
                            "role" to role
                        )
                        firestore.collection("users").document(userId).set(userMap).await()
                        auth.signOut()
                        Toast.makeText(context, "Registration successful! Please log in.", Toast.LENGTH_LONG).show()
                        onRegisterSuccess()
                    } catch (e: Exception) {
                        Log.e("RegisterScreen", "Registration failed", e)
                        Toast.makeText(context, "Registration failed: ${e.message}", Toast.LENGTH_LONG).show()
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
                Text("Register")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onNavigateToLogin) {
            Text("Already have an account? Log In", color = GreyTextDark)
        }
    }
}