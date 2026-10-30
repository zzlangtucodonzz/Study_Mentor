package com.example.study_mentor.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.study_mentor.ui.theme.BackgroundOffWhite
import com.example.study_mentor.ui.theme.PrimaryBlue

@Composable
fun LoginScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onAuthSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    val state by viewModel.authState.collectAsState()
    val context = LocalContext.current
    val emailError = if (submitted) AuthValidation.emailError(email) else null
    val passwordError = if (submitted) AuthValidation.loginPasswordError(password) else null

    LaunchedEffect(state.currentUser) {
        if (state.currentUser != null) onAuthSuccess()
    }
    LaunchedEffect(state.infoMessage) {
        state.infoMessage?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(BackgroundOffWhite)
            .imePadding().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 32.dp)
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.width(8.dp))
            Text("Welcome Back", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Text("Sign in to continue learning", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        state.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
        }
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; viewModel.clearError() },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            isError = emailError != null,
            supportingText = { emailError?.let { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; viewModel.clearError() },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            isError = passwordError != null,
            supportingText = { passwordError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        TextButton(
            onClick = { viewModel.clearError(); showResetDialog = true },
            modifier = Modifier.align(Alignment.End)
        ) { Text("Forgot password?") }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                submitted = true
                if (AuthValidation.emailError(email) == null &&
                    AuthValidation.loginPasswordError(password) == null) {
                    viewModel.login(email, password)
                }
            },
            enabled = !state.isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (state.isLoading) CircularProgressIndicator(
                modifier = Modifier.size(22.dp), strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            ) else Text("Log In", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("Don't have an account? ")
            Text("Sign up", color = PrimaryBlue, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(enabled = !state.isLoading) { onNavigateToRegister() })
        }
    }

    if (showResetDialog) {
        var resetEmail by remember { mutableStateOf(email) }
        var resetSubmitted by remember { mutableStateOf(false) }
        val resetError = if (resetSubmitted) AuthValidation.emailError(resetEmail) else null
        AlertDialog(
            onDismissRequest = { if (!state.isLoading) showResetDialog = false },
            title = { Text("Reset password") },
            text = {
                Column {
                    Text("Enter your email to receive a password reset link.")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = resetEmail, onValueChange = { resetEmail = it },
                        label = { Text("Email") }, singleLine = true,
                        isError = resetError != null,
                        supportingText = { resetError?.let { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                }
            },
            confirmButton = {
                TextButton(enabled = !state.isLoading, onClick = {
                    resetSubmitted = true
                    if (AuthValidation.emailError(resetEmail) == null) {
                        viewModel.resetPassword(resetEmail)
                        showResetDialog = false
                    }
                }) { Text("Send link") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }
}
