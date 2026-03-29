package com.pesu.pesudash.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pesu.pesudash.data.model.UserProfile
import com.pesu.pesudash.ui.components.ButtonVariant
import com.pesu.pesudash.ui.components.ShadcnButton
import com.pesu.pesudash.ui.components.ShadcnInput
import com.pesu.pesudash.ui.components.ShadcnSeparator
import com.pesu.pesudash.ui.theme.AppTheme
import com.pesu.pesudash.ui.theme.Inter

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: (UserProfile) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val c = AppTheme.colors

    var username       by remember { mutableStateOf("") }
    var password       by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        if (state is LoginUiState.Success) {
            onLoginSuccess((state as LoginUiState.Success).profile)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(c.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text          = "PESU",
                fontSize      = 36.sp,
                fontFamily    = Inter,
                fontWeight    = FontWeight.Black,
                color         = c.foreground,
                letterSpacing = (-1).sp
            )
            Text(
                text       = "Pesu Dash",
                fontSize   = 16.sp,
                fontFamily = Inter,
                fontWeight = FontWeight.Normal,
                color      = c.mutedFg,
                modifier   = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(12.dp))
            ShadcnSeparator()
            Spacer(Modifier.height(32.dp))

            Column(
                modifier            = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    "SRN",
                    fontSize   = 13.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Medium,
                    color      = c.foreground,
                    modifier   = Modifier.padding(bottom = 6.dp)
                )
                ShadcnInput(
                    value           = username,
                    onValueChange   = { username = it },
                    placeholder     = "PES1UG22CS001",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    "Password",
                    fontSize   = 13.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Medium,
                    color      = c.foreground,
                    modifier   = Modifier.padding(bottom = 6.dp)
                )
                ShadcnInput(
                    value                = password,
                    onValueChange        = { password = it },
                    placeholder          = "Enter your password",
                    visualTransformation = if (passwordVisible)
                        VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        TextButton(onClick = { passwordVisible = !passwordVisible }) {
                            Text(
                                if (passwordVisible) "Hide" else "Show",
                                color      = c.dimFg,
                                fontSize   = 12.sp,
                                fontFamily = Inter
                            )
                        }
                    }
                )
            }

            if (state is LoginUiState.Error) {
                Text(
                    text       = (state as LoginUiState.Error).message,
                    color      = c.red,
                    fontSize   = 13.sp,
                    fontFamily = Inter,
                    modifier   = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            ShadcnButton(
                text      = "Sign In",
                onClick   = { viewModel.login(username, password) },
                modifier  = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                isLoading = state is LoginUiState.Loading,
                enabled   = state !is LoginUiState.Loading,
                variant   = ButtonVariant.Default
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text       = "Login with your PESU Academy credentials",
                fontSize   = 12.sp,
                fontFamily = Inter,
                color      = c.dimFg
            )
        }
    }
}