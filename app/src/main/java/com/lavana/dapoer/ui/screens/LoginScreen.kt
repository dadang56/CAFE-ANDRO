package com.lavana.dapoer.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lavana.dapoer.R
import com.lavana.dapoer.data.CartManager
import com.lavana.dapoer.data.StaffAccount
import com.lavana.dapoer.data.SupabaseClient
import com.lavana.dapoer.ui.theme.*
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.launch
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import android.content.Context
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateToCustomer: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToDriver: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Registration States
    var isRegisterScreen by remember { mutableStateOf(false) }
    var regPhone by remember { mutableStateOf("") }
    var regFullName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regConfirmPassword by remember { mutableStateOf("") }
    var regPasswordVisible by remember { mutableStateOf(false) }
    var regConfirmVisible by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val sharedPref = remember(context) { context.getSharedPreferences("lavana_prefs", Context.MODE_PRIVATE) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Top Navigation Back Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (isRegisterScreen) {
                        isRegisterScreen = false
                        errorMessage = null
                    } else {
                        onNavigateBack()
                    }
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = DarkCharcoal
                    )
                }
            }

            val infiniteTransition = rememberInfiniteTransition(label = "LogoAnim")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.95f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 2000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "LogoScale"
            )
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 15000, easing = androidx.compose.animation.core.LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "LogoRotate"
            )

            if (!isRegisterScreen) {
                // ==================== LOGIN SCREEN ====================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .graphicsLayer(scaleX = scale, scaleY = scale),
                        contentAlignment = Alignment.Center
                    ) {
                        // Minimalist decorative spinning ring
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .border(BorderStroke(1.5.dp, OrangeJco.copy(alpha = 0.25f)), shape = CircleShape)
                                .graphicsLayer(rotationZ = rotation)
                        ) {
                            // Small decorative orange dot on the ring
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(OrangeJco, CircleShape)
                                    .align(Alignment.TopCenter)
                            )
                        }
                        
                        // Dapoer Lavana Logo (Enlarged inside the ring)
                        Image(
                            painter = painterResource(id = R.drawable.logo_lavana),
                            contentDescription = "Logo Dapoer Lavana",
                            modifier = Modifier
                                .height(150.dp)
                                .fillMaxWidth(0.85f),
                            contentScale = ContentScale.Fit,
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(OrangeJco)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    // Welcome Header text
                    Text(
                        text = "Selamat Datang!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkCharcoal,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = "Masuk ke akun Anda untuk melanjutkan",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        textAlign = TextAlign.Start
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Email/Username/Phone Input
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text("Nomor HP atau Email", color = Color.LightGray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeJco,
                            unfocusedBorderColor = Color.LightGray,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password Input
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("Kata Sandi", color = Color.LightGray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = "Toggle Password Visibility",
                                    tint = Color.Gray
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeJco,
                            unfocusedBorderColor = Color.LightGray,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        singleLine = true
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMessage!!,
                            color = Color.Red,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Button Masuk
                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                errorMessage = "Nomor HP/Email dan sandi wajib diisi!"
                                return@Button
                            }
                            isLoading = true
                            errorMessage = null
                            coroutineScope.launch {
                                val rawInput = email.trim()
                                val inputClean = if (rawInput.contains("@")) rawInput else "$rawInput@lavana.com"
                                var loginSuccess = false
                                
                                try {
                                    // 1. Check if user is staff (Admin or Driver)
                                    try {
                                        val staffAccounts = SupabaseClient.db["staff_accounts"].select {
                                            filter {
                                                eq("username", inputClean)
                                                eq("password", password)
                                            }
                                        }.decodeList<StaffAccount>()
                                        
                                        if (staffAccounts.isNotEmpty()) {
                                            val staff = staffAccounts.first()
                                            CartManager.currentStaff = staff
                                            SupabaseClient.setMockUser(staff.id ?: "staff_id", staff.username)
                                            loginSuccess = true
                                            
                                            if (staff.role == "Admin") {
                                                onNavigateToAdmin()
                                            } else if (staff.role == "Driver") {
                                                onNavigateToDriver()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        // Ignore DB error, proceed to auth check
                                    }

                                    // 2. Check Supabase Auth for Customer (Pelanggan)
                                    if (!loginSuccess) {
                                        try {
                                            SupabaseClient.auth.signInWith(Email) {
                                                this.email = inputClean
                                                this.password = password
                                            }
                                            CartManager.currentStaff = null
                                            loginSuccess = true
                                            onNavigateToCustomer()
                                        } catch (authException: Exception) {
                                            // Proceed to check local mock credentials
                                        }
                                    }

                                    // 2.5 Check locally registered users backup in SharedPreferences
                                    if (!loginSuccess) {
                                        val registeredUsers = sharedPref.getStringSet("registered_users", emptySet()) ?: emptySet()
                                        val match = registeredUsers.firstOrNull { it.startsWith("$inputClean:") }
                                        if (match != null && match.substringAfter(":") == password) {
                                            SupabaseClient.setMockUser(java.util.UUID.randomUUID().toString(), inputClean)
                                            CartManager.currentStaff = null
                                            loginSuccess = true
                                            onNavigateToCustomer()
                                        }
                                    }

                                    // 3. Local Mock Backups (Offline fallback)
                                    if (!loginSuccess) {
                                        if (inputClean == "pelanggan@lavana.com" && password == "pelanggan123") {
                                            SupabaseClient.setMockUser("7838c359-d924-468b-b5c4-398925c3194c", "pelanggan@lavana.com")
                                            CartManager.currentStaff = null
                                            loginSuccess = true
                                            onNavigateToCustomer()
                                        } else if (inputClean == "admin@lavana.com" && password == "admin123") {
                                            val mockAdmin = StaffAccount(
                                                id = "00000000-0000-0000-0000-0000000000a1",
                                                username = "admin@lavana.com",
                                                password = "admin123",
                                                role = "Admin",
                                                name = "Admin Demo (Dapoer Lavana)"
                                            )
                                            CartManager.currentStaff = mockAdmin
                                            SupabaseClient.setMockUser(mockAdmin.id, mockAdmin.username)
                                            loginSuccess = true
                                            onNavigateToAdmin()
                                        } else if (inputClean == "driver@lavana.com" && password == "driver123") {
                                            val mockDriver = StaffAccount(
                                                id = "00000000-0000-0000-0000-0000000000d1",
                                                username = "driver@lavana.com",
                                                password = "driver123",
                                                role = "Driver",
                                                name = "Driver Demo (Dapoer Lavana)"
                                            )
                                            CartManager.currentStaff = mockDriver
                                            SupabaseClient.setMockUser(mockDriver.id, mockDriver.username)
                                            loginSuccess = true
                                            onNavigateToDriver()
                                        }
                                    }

                                    if (!loginSuccess) {
                                        errorMessage = "Gagal Masuk: Kredensial tidak valid!"
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Gagal Masuk: Masalah jaringan atau akun tidak terdaftar!"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeJco),
                        enabled = !isLoading
                    ) {
                        Text(
                            text = if (isLoading) "Memproses..." else "Masuk",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Bottom register prompt
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = "Belum punya akun ? ", fontSize = 13.sp, color = DarkCharcoal)
                        Text(
                            text = "Buat Akun",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangeJco,
                            modifier = Modifier.clickable {
                                isRegisterScreen = true
                                errorMessage = null
                            }
                        )
                    }
                }
            } else {
                // ==================== REGISTER/SIGNUP SCREEN ====================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .graphicsLayer(scaleX = scale, scaleY = scale),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .border(BorderStroke(1.5.dp, OrangeJco.copy(alpha = 0.25f)), shape = CircleShape)
                                .graphicsLayer(rotationZ = rotation)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(OrangeJco, CircleShape)
                                    .align(Alignment.TopCenter)
                            )
                        }
                        
                        Image(
                            painter = painterResource(id = R.drawable.logo_lavana),
                            contentDescription = "Logo Dapoer Lavana",
                            modifier = Modifier
                                .height(110.dp)
                                .fillMaxWidth(0.75f),
                            contentScale = ContentScale.Fit,
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(OrangeJco)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Buat Akun",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkCharcoal,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = "Silahkan lengkapi data diri kamu untuk menyelesaikan proses registrasi akun",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        textAlign = TextAlign.Start
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Nomor Telepon Input
                    OutlinedTextField(
                        value = regPhone,
                        onValueChange = { regPhone = it },
                        placeholder = { Text("Nomor Telepon", color = Color.LightGray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeJco,
                            unfocusedBorderColor = Color.LightGray,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Nama Lengkap Input
                    OutlinedTextField(
                        value = regFullName,
                        onValueChange = { regFullName = it },
                        placeholder = { Text("Nama Lengkap", color = Color.LightGray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeJco,
                            unfocusedBorderColor = Color.LightGray,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Email Input (Opsional)
                    OutlinedTextField(
                        value = regEmail,
                        onValueChange = { regEmail = it },
                        placeholder = { Text("Email (Opsional)", color = Color.LightGray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeJco,
                            unfocusedBorderColor = Color.LightGray,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Kata Sandi Input
                    OutlinedTextField(
                        value = regPassword,
                        onValueChange = { regPassword = it },
                        placeholder = { Text("Kata Sandi", color = Color.LightGray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = if (regPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { regPasswordVisible = !regPasswordVisible }) {
                                Icon(
                                    imageVector = if (regPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = "Toggle Password Visibility",
                                    tint = Color.Gray
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeJco,
                            unfocusedBorderColor = Color.LightGray,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Konfirmasi Password Input
                    OutlinedTextField(
                        value = regConfirmPassword,
                        onValueChange = { regConfirmPassword = it },
                        placeholder = { Text("Konfirmasi Password", color = Color.LightGray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = if (regConfirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { regConfirmVisible = !regConfirmVisible }) {
                                Icon(
                                    imageVector = if (regConfirmVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = "Toggle Password Visibility",
                                    tint = Color.Gray
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeJco,
                            unfocusedBorderColor = Color.LightGray,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        singleLine = true
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMessage!!,
                            color = Color.Red,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Button Buat Akun
                    Button(
                        onClick = {
                            if (regPhone.isBlank() || regFullName.isBlank() || regPassword.isBlank() || regConfirmPassword.isBlank()) {
                                errorMessage = "Semua bidang wajib diisi kecuali Email!"
                                return@Button
                            }
                            if (regPassword != regConfirmPassword) {
                                errorMessage = "Konfirmasi password tidak cocok!"
                                return@Button
                            }
                            isLoading = true
                            errorMessage = null
                            coroutineScope.launch {
                                try {
                                    val phoneClean = regPhone.trim()
                                    val primaryEmail = "${phoneClean}@lavana.com"
                                    
                                    // Step 1. Supabase Auth Sign Up using Phone format
                                    try {
                                        SupabaseClient.auth.signUpWith(Email) {
                                            this.email = primaryEmail
                                            this.password = regPassword
                                        }
                                    } catch (signUpEx: Exception) {
                                        // Ignore Auth error (e.g. offline/mock)
                                    }

                                    // Save primary account details in SharedPreferences Backup
                                    val currentSet = sharedPref.getStringSet("registered_users", emptySet()) ?: emptySet()
                                    val newUsers = HashSet(currentSet)
                                    newUsers.add("$primaryEmail:$regPassword")
                                    sharedPref.edit().putString("name_$primaryEmail", regFullName.trim()).apply()

                                    // Step 2. If optional email is provided, register that as well!
                                    val optionalEmail = regEmail.trim()
                                    if (optionalEmail.isNotBlank()) {
                                        try {
                                            SupabaseClient.auth.signUpWith(Email) {
                                                this.email = optionalEmail
                                                this.password = regPassword
                                            }
                                        } catch (e: Exception) {}
                                        newUsers.add("$optionalEmail:$regPassword")
                                        sharedPref.edit().putString("name_$optionalEmail", regFullName.trim()).apply()
                                    }

                                    sharedPref.edit().putStringSet("registered_users", newUsers).apply()

                                    // Register success, go back to login screen pre-filled
                                    email = regPhone
                                    isRegisterScreen = false
                                    errorMessage = "Registrasi Berhasil! Silahkan Masuk."
                                    
                                    // Reset register states
                                    regPhone = ""
                                    regFullName = ""
                                    regEmail = ""
                                    regPassword = ""
                                    regConfirmPassword = ""
                                } catch (e: Exception) {
                                    errorMessage = "Registrasi Gagal: ${e.localizedMessage ?: "Coba lagi"}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeJco),
                        enabled = !isLoading
                    ) {
                        Text(
                            text = if (isLoading) "Memproses..." else "Buat Akun",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}
