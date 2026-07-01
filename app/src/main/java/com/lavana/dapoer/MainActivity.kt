package com.lavana.dapoer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import com.lavana.dapoer.ui.screens.*
import com.lavana.dapoer.ui.theme.DapoerLavanaTheme
import com.lavana.dapoer.data.CartManager
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.DialogProperties
import com.lavana.dapoer.R
import com.lavana.dapoer.ui.theme.ForestGreen
import com.lavana.dapoer.ui.theme.LightGrayJco
import com.lavana.dapoer.ui.theme.OnAccentDark
import com.lavana.dapoer.ui.theme.OrangeAccent
import com.lavana.dapoer.ui.theme.OrangeJco
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DapoerLavanaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DapoerLavanaAppNavigation()
                }
            }
        }
    }
}

@Composable
fun DapoerLavanaAppNavigation() {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPref = remember(context) { context.getSharedPreferences("lavana_prefs", android.content.Context.MODE_PRIVATE) }
    
    var showUpdateDialog by remember { mutableStateOf(false) }
    var latestVersionName by remember { mutableStateOf("") }
    var updateDescription by remember { mutableStateOf("") }
    var isMandatoryUpdate by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    
    val currentVersionCode = remember(context) {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (e: Exception) {
            1
        }
    }
    
    LaunchedEffect(Unit) {
        try {
            val savedUserId = sharedPref.getString("logged_in_user_id", null)
            val savedUserEmail = sharedPref.getString("logged_in_user_email", null)
            val savedUserRole = sharedPref.getString("logged_in_user_role", null)
            
            if (savedUserId != null && savedUserEmail != null) {
                com.lavana.dapoer.data.SupabaseClient.setMockUser(savedUserId, savedUserEmail)
                if (savedUserRole == "Admin" || savedUserRole == "Driver" || savedUserRole == "Kasir") {
                    val staffJson = sharedPref.getString("logged_in_staff_json", null)
                    if (staffJson != null) {
                        val staff = kotlinx.serialization.json.Json.decodeFromString<com.lavana.dapoer.data.StaffAccount>(staffJson)
                        com.lavana.dapoer.data.CartManager.currentStaff = staff
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val settings = com.lavana.dapoer.data.SupabaseClient.db["settings"].select()
                .decodeList<SettingsRow>()
            val latest = settings.find { it.key == "latest_version_code" }
            if (latest != null && latest.value.toInt() > currentVersionCode) {
                val code = latest.value.toInt()
                // Tampilkan build code apa adanya; nama versi opsional dari deskripsi.
                latestVersionName = "Build $code"
                updateDescription = latest.description
                isMandatoryUpdate = (settings.find { it.key == "force_update" }?.value ?: 0.0) >= 1.0
                showUpdateDialog = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { if (!isMandatoryUpdate && !isDownloading) showUpdateDialog = false },
            properties = DialogProperties(
                dismissOnBackPress = !isMandatoryUpdate,
                dismissOnClickOutside = false
            ),
            shape = RoundedCornerShape(20.dp),
            icon = {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.verticalGradient(listOf(OrangeJco, ForestGreen))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Update",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Pembaruan Tersedia",
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp
                )
            },
            text = {
                androidx.compose.foundation.layout.Column {
                    if (isDownloading) {
                        androidx.compose.material3.Text(
                            text = "Sedang mengunduh pembaruan... ${(downloadProgress * 100).toInt()}%",
                            fontSize = 14.sp
                        )
                    } else {
                        androidx.compose.material3.Text(
                            text = "Versi baru ($latestVersionName) telah dirilis! Segera perbarui aplikasi untuk mendapatkan fitur dan perbaikan terbaru.",
                            fontSize = 14.sp
                        )
                        if (updateDescription.isNotBlank()) {
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(10.dp))
                            androidx.compose.material3.Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = androidx.compose.material3.CardDefaults.cardColors(
                                    containerColor = androidx.compose.ui.graphics.Color.LightGray.copy(alpha = 0.2f)
                                )
                            ) {
                                androidx.compose.foundation.layout.Column(
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    androidx.compose.material3.Text(
                                        text = "Catatan Rilis:",
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = com.lavana.dapoer.ui.theme.OrangeJco
                                    )
                                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
                                    androidx.compose.material3.Text(
                                        text = updateDescription,
                                        fontSize = 11.sp,
                                        color = androidx.compose.ui.graphics.Color.DarkGray
                                    )
                                }
                            }
                        }
                    }
                    
                    if (isDownloading) {
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.fillMaxWidth(),
                            color = com.lavana.dapoer.ui.theme.OrangeJco
                        )
                    }
                    
                    if (downloadError != null) {
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.material3.Text(
                            text = downloadError ?: "",
                            color = androidx.compose.ui.graphics.Color.Red,
                            fontSize = 12.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                if (!isDownloading) {
                    androidx.compose.material3.Button(
                        onClick = {
                            isDownloading = true
                            downloadProgress = 0f
                            downloadError = null
                            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                try {
                                    var updateUrl = "https://raw.githubusercontent.com/dadang56/CAFE-ANDRO/main/apk/Dapoer_Lavana.apk" // Default fallback
                                    try {
                                        val bannerRows = com.lavana.dapoer.data.SupabaseClient.db["banners"].select {
                                            filter { eq("title", "app_apk_url") }
                                        }.decodeList<com.lavana.dapoer.data.BannerItem>()
                                        if (bannerRows.isNotEmpty() && bannerRows.first().imageUrl.isNotBlank()) {
                                            updateUrl = bannerRows.first().imageUrl
                                        }
                                    } catch (ex: Exception) {
                                        ex.printStackTrace()
                                    }
                                    
                                    val url = java.net.URL(updateUrl)
                                    val connection = url.openConnection() as java.net.HttpURLConnection
                                    connection.connect()
                                    if (connection.responseCode != java.net.HttpURLConnection.HTTP_OK) {
                                        throw java.lang.Exception("Server error: HTTP ${connection.responseCode}")
                                    }
                                    val fileLength = connection.contentLength
                                    val input = java.io.BufferedInputStream(connection.inputStream)
                                    
                                    val outputFile = java.io.File(context.cacheDir, "update.apk")
                                    if (outputFile.exists()) {
                                        outputFile.delete()
                                    }
                                    val output = java.io.FileOutputStream(outputFile)
                                    val data = ByteArray(4096)
                                    var total: Long = 0
                                    var count: Int
                                    while (input.read(data).also { count = it } != -1) {
                                        total += count
                                        if (fileLength > 0) {
                                            downloadProgress = total.toFloat() / fileLength.toFloat()
                                        }
                                        output.write(data, 0, count)
                                    }
                                    output.flush()
                                    output.close()
                                    input.close()
                                    
                                    // Launch package installer
                                    val apkUri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        outputFile
                                    )
                                    val installIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                        setDataAndType(apkUri, "application/vnd.android.package-archive")
                                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    }
                                    context.startActivity(installIntent)
                                    isDownloading = false
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    downloadError = "Gagal mengunduh: " + (e.localizedMessage ?: "Koneksi terputus")
                                    isDownloading = false
                                }
                            }
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = OrangeAccent,
                            contentColor = OnAccentDark
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        androidx.compose.material3.Text("Perbarui Sekarang", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (!isDownloading && !isMandatoryUpdate) {
                    TextButton(onClick = { showUpdateDialog = false }) {
                        Text("Nanti", color = Color.Gray)
                    }
                }
            }
        )
    }

    val clearSession = {
        sharedPref.edit()
            .remove("logged_in_user_id")
            .remove("logged_in_user_email")
            .remove("logged_in_user_role")
            .remove("logged_in_staff_json")
            .apply()
        com.lavana.dapoer.data.SupabaseClient.setMockUser(null, null)
        com.lavana.dapoer.data.CartManager.currentStaff = null
        coroutineScope.launch {
            try {
                com.lavana.dapoer.data.SupabaseClient.auth.signOut()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        Unit
    }

    NavHost(
        navController = navController,
        startDestination = "splash",
        enterTransition = { slideInHorizontally(animationSpec = tween(320)) { it } + fadeIn(animationSpec = tween(320)) },
        exitTransition = { slideOutHorizontally(animationSpec = tween(320)) { -it / 4 } + fadeOut(animationSpec = tween(320)) },
        popEnterTransition = { slideInHorizontally(animationSpec = tween(320)) { -it / 4 } + fadeIn(animationSpec = tween(320)) },
        popExitTransition = { slideOutHorizontally(animationSpec = tween(320)) { it } + fadeOut(animationSpec = tween(320)) }
    ) {
        composable("splash") {
            SplashScreen(
                onAnimationFinished = {
                    val savedUserId = sharedPref.getString("logged_in_user_id", null)
                    val savedUserRole = sharedPref.getString("logged_in_user_role", null)
                    if (savedUserId != null) {
                        if (savedUserRole == "Admin") {
                            navController.navigate("admin_dashboard") {
                                popUpTo("splash") { inclusive = true }
                            }
                        } else if (savedUserRole == "Driver") {
                            navController.navigate("driver_dashboard") {
                                popUpTo("splash") { inclusive = true }
                            }
                        } else {
                            navController.navigate("home") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                    } else {
                        navController.navigate("home") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
            )
        }
        composable("login") {
            LoginScreen(
                onNavigateToCustomer = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToAdmin = {
                    navController.navigate("admin_dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToDriver = {
                    navController.navigate("driver_dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable("admin_dashboard") {
            AdminScreen(
                onLogout = {
                    clearSession()
                    navController.navigate("login") {
                        popUpTo("admin_dashboard") { inclusive = true }
                    }
                }
            )
        }
        composable("driver_dashboard") {
            DriverScreen(
                onLogout = {
                    clearSession()
                    navController.navigate("login") {
                        popUpTo("driver_dashboard") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            HomeScreen(
                onOrderTypeSelected = { orderType ->
                    if (com.lavana.dapoer.data.SupabaseClient.currentUserId == null) {
                        navController.navigate("login")
                    } else {
                        navController.navigate("checkout/$orderType")
                    }
                },
                onNavigateToCart = {
                    if (com.lavana.dapoer.data.SupabaseClient.currentUserId == null) {
                        navController.navigate("login")
                    } else {
                        val orderType = CartManager.selectedOrderType ?: "Dine In"
                        navController.navigate("checkout/$orderType")
                    }
                },
                onLogout = {
                    clearSession()
                    navController.navigate("login") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onTrackOrder = { orderId ->
                    navController.navigate("tracking/$orderId")
                }
            )
        }
        composable(
            route = "checkout/{orderType}",
            arguments = listOf(navArgument("orderType") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderType = backStackEntry.arguments?.getString("orderType") ?: "Dine In"
            CheckoutScreen(
                orderType = orderType,
                onNavigateBack = { navController.popBackStack() },
                onOrderPlaced = { orderId ->
                    navController.navigate("tracking/$orderId")
                }
            )
        }
        composable(
            route = "tracking/{orderId}",
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            TrackingScreen(
                orderId = orderId,
                onNavigateHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
    }
}
