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
import androidx.compose.animation.core.tween
import com.lavana.dapoer.ui.screens.*
import com.lavana.dapoer.ui.theme.DapoerLavanaTheme
import com.lavana.dapoer.data.CartManager
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize OpenStreetMap configuration
        org.osmdroid.config.Configuration.getInstance().userAgentValue = packageName
        
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
    
    var showUpdateDialog by remember { mutableStateOf(false) }
    var latestVersionName by remember { mutableStateOf("2.0") }
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
            val rows = com.lavana.dapoer.data.SupabaseClient.db["settings"].select {
                filter { eq("key", "latest_version_code") }
            }.decodeList<SettingsRow>()
            if (rows.isNotEmpty()) {
                val latestVer = rows.first().value
                if (latestVer.toInt() > currentVersionCode) {
                    latestVersionName = String.format("%.1f", latestVer)
                    showUpdateDialog = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    if (showUpdateDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { /* Non-dismissable */ },
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            title = {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Update",
                        tint = com.lavana.dapoer.ui.theme.OrangeJco,
                        modifier = Modifier.size(28.dp)
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                    androidx.compose.material3.Text(
                        text = "Update Aplikasi Tersedia",
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                androidx.compose.foundation.layout.Column {
                    androidx.compose.material3.Text(
                        text = if (isDownloading) {
                            "Sedang mengunduh pembaruan... ${(downloadProgress * 100).toInt()}%"
                        } else {
                            "Versi baru ($latestVersionName) telah dirilis! Segera perbarui aplikasi untuk mendapatkan fitur terbaru, optimalisasi peta OSM, dan peningkatan stabilitas sistem."
                        },
                        fontSize = 14.sp
                    )
                    
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
                            containerColor = com.lavana.dapoer.ui.theme.OrangeJco
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        androidx.compose.material3.Text("Perbarui Sekarang", color = androidx.compose.ui.graphics.Color.White)
                    }
                }
            }
        )
    }

    NavHost(
        navController = navController,
        startDestination = "splash",
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) },
        popEnterTransition = { fadeIn(animationSpec = tween(300)) },
        popExitTransition = { fadeOut(animationSpec = tween(300)) }
    ) {
        composable("splash") {
            SplashScreen(
                onAnimationFinished = {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
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
                    navController.navigate("login") {
                        popUpTo("admin_dashboard") { inclusive = true }
                    }
                }
            )
        }
        composable("driver_dashboard") {
            DriverScreen(
                onLogout = {
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
