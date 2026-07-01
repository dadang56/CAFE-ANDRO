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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
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
            .background(LightGrayJco)
    ) {
        // Watermark halus agar area kosong tidak terlalu polos
        Image(
            painter = painterResource(id = R.drawable.wm_wheat),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 10.dp, bottom = 28.dp)
                .size(150.dp)
                .rotate(-18f),
            alpha = 0.06f
        )
        Image(
            painter = painterResource(id = R.drawable.wm_rice),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 96.dp)
                .size(120.dp)
                .rotate(18f),
            alpha = 0.06f
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Animasi masuk logo yang bersih (fade + scale overshoot) — tanpa ring berputar.
            val logoAlpha = remember { Animatable(0f) }
            val logoScale = remember { Animatable(0.85f) }
            LaunchedEffect(Unit) {
                logoAlpha.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 600))
            }
            LaunchedEffect(Unit) {
                logoScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }

            // ==================== TEAL GRADIENT TOP REGION (shared) ====================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(Brush.verticalGradient(listOf(OrangeJco, ForestGreen)))
            ) {
                // Back button overlaid on the gradient (soft translucent chip for contrast)
                IconButton(
                    onClick = {
                        if (isRegisterScreen) {
                            isRegisterScreen = false
                            errorMessage = null
                        } else {
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 12.dp, top = 12.dp)
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 36.dp, bottom = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .alpha(logoAlpha.value)
                            .graphicsLayer(scaleX = logoScale.value, scaleY = logoScale.value),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_lavana),
                            contentDescription = "Logo Dapoer Lavana",
                            modifier = Modifier
                                .height(132.dp)
                                .fillMaxWidth(0.82f),
                            contentScale = ContentScale.Fit,
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White)
                        )
                    }
                }
            }

            if (!isRegisterScreen) {
                // ==================== LOGIN SCREEN ====================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-24).dp)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                  Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Welcome Header text
                    Text(
                        text = "Selamat Datang!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = DarkCharcoal,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = "Masuk ke akun Anda untuk melanjutkan",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        textAlign = TextAlign.Start
                    )

                    Spacer(modifier = Modifier.height(24.dp))

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
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(RedPromo.copy(alpha = 0.10f))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = RedPromo,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

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
                                
                                val saveSession = { userId: String, emailVal: String, roleVal: String, staff: StaffAccount? ->
                                    val editor = sharedPref.edit()
                                        .putString("logged_in_user_id", userId)
                                        .putString("logged_in_user_email", emailVal)
                                        .putString("logged_in_user_role", roleVal)
                                    if (staff != null) {
                                        try {
                                            val jsonStr = Json.encodeToString(staff)
                                            editor.putString("logged_in_staff_json", jsonStr)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    } else {
                                        editor.remove("logged_in_staff_json")
                                    }
                                    editor.apply()
                                }

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
                                            saveSession(staff.id ?: "staff_id", staff.username, staff.role, staff)
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
                                            val currentAuthUser = SupabaseClient.auth.currentUserOrNull()
                                            val uId = currentAuthUser?.id ?: java.util.UUID.randomUUID().toString()
                                            saveSession(uId, inputClean, "Customer", null)
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
                                            val uId = java.util.UUID.randomUUID().toString()
                                            SupabaseClient.setMockUser(uId, inputClean)
                                            CartManager.currentStaff = null
                                            saveSession(uId, inputClean, "Customer", null)
                                            loginSuccess = true
                                            onNavigateToCustomer()
                                        }
                                    }

                                    // 3. Local Mock Backups (Offline fallback)
                                    if (!loginSuccess) {
                                        if (inputClean == "pelanggan@lavana.com" && password == "pelanggan123") {
                                            val uId = "7838c359-d924-468b-b5c4-398925c3194c"
                                            SupabaseClient.setMockUser(uId, "pelanggan@lavana.com")
                                            CartManager.currentStaff = null
                                            saveSession(uId, "pelanggan@lavana.com", "Customer", null)
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
                                            saveSession(mockAdmin.id ?: "", mockAdmin.username, "Admin", mockAdmin)
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
                                            saveSession(mockDriver.id ?: "", mockDriver.username, "Driver", mockDriver)
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
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = OnAccentDark,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Memproses...",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnAccentDark
                            )
                        } else {
                            Text(
                                text = "Masuk",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnAccentDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    HorizontalDivider(color = LightOrangeJco, thickness = 1.dp)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bottom register prompt
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
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
                }
            } else {
                // ==================== REGISTER/SIGNUP SCREEN ====================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-24).dp)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                  Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Buat Akun",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = DarkCharcoal,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = "Silahkan lengkapi data diri kamu untuk menyelesaikan proses registrasi akun",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        textAlign = TextAlign.Start
                    )

                    Spacer(modifier = Modifier.height(22.dp))

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

                    Spacer(modifier = Modifier.height(12.dp))

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

                    Spacer(modifier = Modifier.height(12.dp))

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

                    Spacer(modifier = Modifier.height(12.dp))

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

                    Spacer(modifier = Modifier.height(12.dp))

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
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(RedPromo.copy(alpha = 0.10f))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = RedPromo,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

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
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = OnAccentDark,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Memproses...",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnAccentDark
                            )
                        } else {
                            Text(
                                text = "Buat Akun",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnAccentDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    HorizontalDivider(color = LightOrangeJco, thickness = 1.dp)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bottom login prompt
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Sudah punya akun ? ", fontSize = 13.sp, color = DarkCharcoal)
                        Text(
                            text = "Masuk",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangeJco,
                            modifier = Modifier.clickable {
                                isRegisterScreen = false
                                errorMessage = null
                            }
                        )
                    }
                  }
                }
            }
        }
    }
}
