package com.lavana.dapoer.ui.screens

import com.lavana.dapoer.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lavana.dapoer.data.*
import com.lavana.dapoer.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.viewinterop.AndroidView
import com.lavana.dapoer.ui.components.GoogleMapView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onLogout: () -> Unit
) {
    var currentTab by remember { mutableIntStateOf(0) } // 0: Orders, 1: Menu, 2: Banners, 3: Account
    
    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .shadow(10.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                containerColor = Color.White,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Pesanan") },
                    label = { Text("Pesanan", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OrangeJco,
                        selectedTextColor = OrangeJco,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = LightOrangeJco
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.RestaurantMenu, contentDescription = "Menu") },
                    label = { Text("Menu", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OrangeJco,
                        selectedTextColor = OrangeJco,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = LightOrangeJco
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = "Laporan") },
                    label = { Text("Laporan", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OrangeJco,
                        selectedTextColor = OrangeJco,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = LightOrangeJco
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = { Icon(Icons.Default.Bookmark, contentDescription = "Promo Banner") },
                    label = { Text("Banners", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OrangeJco,
                        selectedTextColor = OrangeJco,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = LightOrangeJco
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 4,
                    onClick = { currentTab = 4 },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Akun") },
                    label = { Text("Profil", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OrangeJco,
                        selectedTextColor = OrangeJco,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = LightOrangeJco
                    )
                )
            }
        },
        containerColor = LightGrayJco
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally(animationSpec = tween(300)) { width -> width } + fadeIn(animationSpec = tween(300))) togetherWith
                        (slideOutHorizontally(animationSpec = tween(300)) { width -> -width } + fadeOut(animationSpec = tween(300)))
                    } else {
                        (slideInHorizontally(animationSpec = tween(300)) { width -> -width } + fadeIn(animationSpec = tween(300))) togetherWith
                        (slideOutHorizontally(animationSpec = tween(300)) { width -> width } + fadeOut(animationSpec = tween(300)))
                    }
                },
                label = "AdminTabTransition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> TabAdminOrders()
                    1 -> TabAdminMenu()
                    2 -> TabAdminReports()
                    3 -> TabAdminBanners()
                    4 -> TabAdminProfile(onLogout = onLogout)
                }
            }
        }
    }
}

// ==========================================================
// SHARED: MODERN EMPTY STATE
// ==========================================================
@Composable
fun AdminEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(LightOrangeJco),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = OrangeJco, modifier = Modifier.size(48.dp))
        }
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkCharcoal)
        Text(subtitle, fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
    }
}

// ==========================================================
// TABS 0: ORDERS MANAGEMENT
// ==========================================================
@Composable
fun TabAdminOrders() {
    val coroutineScope = rememberCoroutineScope()
    var ordersList by remember { mutableStateOf<List<Order>>(emptyList()) }
    var driversList by remember { mutableStateOf<List<StaffAccount>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedOrderForDriver by remember { mutableStateOf<Order?>(null) }
    var showAssignDialog by remember { mutableStateOf(false) }
    var previewReceiptUrl by remember { mutableStateOf<String?>(null) }
    var chatOrder by remember { mutableStateOf<Order?>(null) }

    var showKasirDialog by remember { mutableStateOf(false) }
    var selectedOrderForPrint by remember { mutableStateOf<Order?>(null) }
    var printOrderItems by remember { mutableStateOf<List<OrderItem>>(emptyList()) }
    var printMenuItemsMap by remember { mutableStateOf<Map<String, MenuItem>>(emptyMap()) }
    var isFetchingPrintItems by remember { mutableStateOf(false) }

    fun fetchPrintItems(order: Order) {
        selectedOrderForPrint = order
        isFetchingPrintItems = true
        coroutineScope.launch {
            try {
                val items = SupabaseClient.db["order_items"].select {
                    filter { eq("order_id", order.id ?: "") }
                }.decodeList<OrderItem>()
                
                val menus = SupabaseClient.db["menu_items"].select().decodeList<MenuItem>()
                printMenuItemsMap = menus.associateBy { it.id }
                printOrderItems = items
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isFetchingPrintItems = false
            }
        }
    }

    fun refreshData() {
        isLoading = true
        coroutineScope.launch {
            try {
                val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                ordersList = SupabaseClient.db["orders"].select().decodeList<Order>()
                    .filter { (it.createdAt?.take(10) ?: "") == todayStr }
                    .sortedByDescending { it.orderNumber ?: 0 }
                
                driversList = SupabaseClient.db["staff_accounts"].select {
                    filter { eq("role", "Driver") }
                }.decodeList<StaffAccount>()
            } catch (e: Exception) {
                // Ignore DB error
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Sales Summary Card
        val totalSales = ordersList.filter { it.status == "Selesai" }.sumOf { it.total }
        val completedCount = ordersList.filter { it.status == "Selesai" }.size

        // Floating teal gradient header with summary
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(Brush.verticalGradient(listOf(OrangeJco, ForestGreen)))
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Kelola Pesanan Delivery",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text("Pantau pesanan masuk hari ini", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                    IconButton(
                        onClick = { refreshData() },
                        modifier = Modifier.background(Color.White.copy(alpha = 0.18f), CircleShape)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Penjualan", fontSize = 12.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Rp ${String.format("%,.0f", totalSales)}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OrangeJco)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Pesanan Sukses", fontSize = 12.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("$completedCount Pesanan", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
                        }
                    }
                }
            }
        }

        // POS Kasir: buat pesanan langsung untuk pelanggan yang memesan di tempat (tanpa aplikasi)
        Button(
            onClick = { showKasirDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .height(46.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
        ) {
            Icon(Icons.Default.AddShoppingCart, contentDescription = null, tint = OnAccentDark, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Buat Pesanan (Kasir)", color = OnAccentDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        if (showKasirDialog) {
            KasirOrderDialog(
                onDismiss = { showKasirDialog = false },
                onCreated = { created ->
                    showKasirDialog = false
                    refreshData()
                    fetchPrintItems(created)
                }
            )
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = OrangeJco)
                    Text("Memuat pesanan...", fontSize = 13.sp, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (ordersList.isEmpty()) {
                    item {
                        AdminEmptyState(
                            icon = Icons.Default.ReceiptLong,
                            title = "Belum ada pesanan",
                            subtitle = "Belum ada pesanan masuk."
                        )
                    }
                } else {
                    items(ordersList) { order ->
                        AdminOrderCard(
                            order = order,
                            drivers = driversList,
                            onAccept = {
                                coroutineScope.launch {
                                    try {
                                        SupabaseClient.db["orders"].update({
                                            set("status", "Diproses")
                                            set("payment_status", "Terbayar")
                                        }) { filter { eq("id", order.id ?: "") } }
                                        refreshData()
                                    } catch (e: Exception) {}
                                }
                            },
                            onAssignDriverClick = {
                                selectedOrderForDriver = order
                                showAssignDialog = true
                            },
                            onShowReceipt = { url ->
                                previewReceiptUrl = url
                            },
                            onPrintReceipt = {
                                fetchPrintItems(order)
                            },
                            onOpenChat = {
                                chatOrder = order
                            },
                            onRejectPayment = { reason ->
                                coroutineScope.launch {
                                    try {
                                        // Set status jadi "Dibatalkan" (bukan tetap "Pending") agar pesanan
                                        // ini berhenti tampil dengan tombol Konfirmasi/Tolak -- sebelumnya
                                        // status tidak diubah sehingga pesanan yang sudah ditolak tetap
                                        // "menggantung" di daftar aksi admin seolah masih perlu diproses.
                                        SupabaseClient.db["orders"].update({
                                            set("status", "Dibatalkan")
                                            set("payment_status", "Ditolak")
                                            set("notes", "[Ditolak: $reason]")
                                        }) { filter { eq("id", order.id ?: "") } }
                                        refreshData()
                                    } catch (e: Exception) {}
                                }
                            },
                            onLocalPayment = {
                                coroutineScope.launch {
                                    try {
                                        // Pembayaran tunai di tempat: catat metode & tandai lunas, lalu proses.
                                        SupabaseClient.db["orders"].update({
                                            set("status", "Diproses")
                                            set("payment_status", "Terbayar")
                                            set("payment_method", "Tunai (Bayar di Tempat)")
                                        }) { filter { eq("id", order.id ?: "") } }
                                        refreshData()
                                    } catch (e: Exception) {}
                                }
                            },
                            onMarkReady = {
                                // Take Away / Dine In tidak butuh driver -- ditandai "Siap"
                                // saat pesanan sudah selesai dimasak (memicu notifikasi ke pelanggan).
                                coroutineScope.launch {
                                    try {
                                        SupabaseClient.db["orders"].update({
                                            set("status", "Siap")
                                        }) { filter { eq("id", order.id ?: "") } }
                                        refreshData()
                                    } catch (e: Exception) {}
                                }
                            },
                            onCompleteOrder = {
                                coroutineScope.launch {
                                    try {
                                        SupabaseClient.db["orders"].update({
                                            set("status", "Selesai")
                                        }) { filter { eq("id", order.id ?: "") } }
                                        refreshData()
                                    } catch (e: Exception) {}
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAssignDialog && selectedOrderForDriver != null) {
        AlertDialog(
            onDismissRequest = { showAssignDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White,
            title = { Text("Tugaskan Driver Pengiriman", fontWeight = FontWeight.Bold, color = DarkCharcoal) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pilih Driver yang bersedia untuk mengantar pesanan #${selectedOrderForDriver?.orderNumber}:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (driversList.isEmpty()) {
                        Text("Tidak ada driver terdaftar di database.", color = Color.Red, fontSize = 12.sp)
                    } else {
                        driversList.forEach { driver ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        coroutineScope.launch {
                                            try {
                                                SupabaseClient.db["orders"].update({
                                                    set("status", "Diantar")
                                                    set("driver_id", driver.id ?: "")
                                                }) { filter { eq("id", selectedOrderForDriver?.id ?: "") } }
                                                
                                                // Start simulated driver movement automatically
                                                CartManager.startDriverSimulation(selectedOrderForDriver?.id ?: "", selectedOrderForDriver?.coordinates)
                                                
                                                showAssignDialog = false
                                                refreshData()
                                            } catch (e: Exception) {}
                                        }
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = LightOrangeJco),
                                border = BorderStroke(1.dp, OrangeJco.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.DeliveryDining, contentDescription = "Driver", tint = OrangeJco)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(driver.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkCharcoal)
                                        Text("Telp: ${driver.contactNumber ?: "-"}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAssignDialog = false }) {
                    Text("Batal", color = Color.Gray)
                }
            }
        )
    }

    if (chatOrder != null) {
        ChatDialog(
            orderId = chatOrder?.id ?: "",
            orderNumber = chatOrder?.orderNumber,
            currentSenderRole = "Admin",
            currentSenderName = "Admin Dapoer Lavana",
            onDismiss = { chatOrder = null }
        )
    }

    if (previewReceiptUrl != null) {
        var isImageError by remember { mutableStateOf(false) }
        var isImageLoading by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { previewReceiptUrl = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White,
            title = { Text("Bukti Pembayaran Pelanggan", fontWeight = FontWeight.Bold, color = DarkCharcoal) },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth().height(320.dp).background(Color.White, RoundedCornerShape(8.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (previewReceiptUrl?.contains("qris_barcode.png") == true || previewReceiptUrl?.contains("mock") == true || isImageError) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Sukses", tint = ForestGreen, modifier = Modifier.size(50.dp))
                            Text("SINKRONISASI PEMBAYARAN", fontWeight = FontWeight.Bold, color = ForestGreen, fontSize = 14.sp)
                            Text("QRIS / Transfer Seluler Berhasil", fontSize = 11.sp, color = Color.Gray)
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Penerima:", fontSize = 11.sp, color = Color.Gray)
                                Text("Dapoer Lavana Cafe", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Status Transaksi:", fontSize = 11.sp, color = Color.Gray)
                                Text("TERVERIFIKASI OTOMATIS", fontWeight = FontWeight.Bold, color = ForestGreen, fontSize = 11.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Nilai Pembayaran:", fontSize = 11.sp, color = Color.Gray)
                                Text("LUNAS", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = OrangeJco)
                            }
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Catatan: Link gambar bukti transfer tersimpan aman di database Supabase (${previewReceiptUrl?.takeLast(30)}).",
                                fontSize = 9.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        AsyncImage(
                            model = previewReceiptUrl,
                            contentDescription = "Bukti Bayar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                            onLoading = { isImageLoading = true },
                            onSuccess = { isImageLoading = false; isImageError = false },
                            onError = { isImageLoading = false; isImageError = true }
                        )
                        if (isImageLoading) {
                            CircularProgressIndicator(color = OrangeJco)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { previewReceiptUrl = null }) {
                    Text("Tutup", color = OrangeJco)
                }
            }
        )
    }

    if (selectedOrderForPrint != null) {
        val context = LocalContext.current

        fun doPrint(elements: List<ReceiptElement>, successMsg: String, failMsg: String) {
            val savedPrinter = PrinterManager.getSavedPrinter(context)
            if (savedPrinter == null) {
                android.widget.Toast.makeText(context, "Pilih printer dulu di Pengaturan Printer", android.widget.Toast.LENGTH_LONG).show()
                return
            }
            coroutineScope.launch {
                val result = PrinterManager.printReceipt(context, elements)
                result.fold(
                    onSuccess = {
                        android.widget.Toast.makeText(context, successMsg, android.widget.Toast.LENGTH_LONG).show()
                    },
                    onFailure = { ex ->
                        android.widget.Toast.makeText(context, ex.message ?: failMsg, android.widget.Toast.LENGTH_LONG).show()
                    }
                )
            }
            selectedOrderForPrint = null
        }

        AlertDialog(
            onDismissRequest = { selectedOrderForPrint = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cetak Struk Thermal", fontWeight = FontWeight.Bold, color = DarkCharcoal)
                    if (isFetchingPrintItems) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                }
            },
            text = {
                if (isFetchingPrintItems) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("Memuat data item pesanan...")
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .background(Color.White)
                            .border(1.dp, Color.LightGray)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        // ATAS: rata kiri
                        Text(
                            text = "DAPOER LAVANA",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = Color.Black
                        )
                        Text(
                            text = "Warkop & Katering",
                            fontSize = 10.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = Color.Black
                        )
                        Text(
                            text = "Tlp/Wa : ${CartManager.currentStaff?.contactNumber ?: "-"}",
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = Color.Black
                        )
                        Text(
                            text = "Nama (Pemesan) : ${selectedOrderForPrint?.customerName ?: "-"}",
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = Color.Black
                        )
                        Text(
                            text = "Kasir : ${CartManager.currentStaff?.name ?: "-"}",
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = Color.Black
                        )
                        Text(
                            text = "--------------------------------",
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = Color.Black
                        )

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "No. Order : #${selectedOrderForPrint?.orderNumber}",
                                fontSize = 11.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = Color.Black
                            )
                            Text(
                                text = "Tanggal   : ${selectedOrderForPrint?.createdAt?.take(16)?.replace("T", " ") ?: ""}",
                                fontSize = 11.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = Color.Black
                            )
                            Text(
                                text = "Tipe      : ${selectedOrderForPrint?.orderType}",
                                fontSize = 11.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = Color.Black
                            )
                            if (!selectedOrderForPrint?.customerPhone.isNullOrBlank()) {
                                Text(
                                    text = "Telp      : ${selectedOrderForPrint?.customerPhone}",
                                    fontSize = 11.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = Color.Black
                                )
                            }
                        }
                        
                        Text(
                            text = "--------------------------------",
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = Color.Black
                        )
                        
                        printOrderItems.forEach { item ->
                            val menuName = printMenuItemsMap[item.menuItemId]?.name ?: "Item Menu"
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = "$menuName x${item.quantity}",
                                    fontSize = 11.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = Color.Black,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "Rp ${String.format("%,.0f", item.priceAtOrder * item.quantity)}",
                                    fontSize = 11.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = Color.Black
                                )
                            }
                            if (!item.notes.isNullOrBlank()) {
                                Text(
                                    text = "  * Notes: ${item.notes}",
                                    fontSize = 9.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = Color.Gray
                                )
                            }
                        }
                        
                        Text(
                            text = "--------------------------------",
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = Color.Black
                        )
                        
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = "Subtotal     :",
                                    fontSize = 11.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = Color.Black
                                )
                                Text(
                                    text = "Rp ${String.format("%,.0f", selectedOrderForPrint?.subtotal)}",
                                    fontSize = 11.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = Color.Black
                                )
                            }
                            if ((selectedOrderForPrint?.deliveryFee ?: 0.0) > 0.0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        text = "Ongkir       :",
                                        fontSize = 11.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = "Rp ${String.format("%,.0f", selectedOrderForPrint?.deliveryFee)}",
                                        fontSize = 11.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = Color.Black
                                    )
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = "TOTAL        :",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = Color.Black
                                )
                                Text(
                                    text = "Rp ${String.format("%,.0f", selectedOrderForPrint?.total)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = Color.Black
                                )
                            }
                        }
                        
                        Text(
                            text = "--------------------------------",
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = Color.Black
                        )
                        Text(
                            text = "Metode: ${selectedOrderForPrint?.paymentMethod} (${selectedOrderForPrint?.paymentStatus})",
                            fontSize = 10.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // BAWAH: rata tengah
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Bill ini bukan bukti Pembayaran",
                                fontSize = 9.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Image(
                                painter = painterResource(R.drawable.logo_lavana),
                                contentDescription = "Logo Dapoer Lavana",
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.Black),
                                modifier = Modifier.height(48.dp)
                            )
                            Text(
                                text = "Warkop . Rasa . Cerita",
                                fontSize = 8.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Terima Kasih Atas Kunjungan Anda",
                                fontSize = 10.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = Color.Black,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val order = selectedOrderForPrint
                                val kitchenElements = buildKitchenReceiptElements(order, printOrderItems, printMenuItemsMap)
                                doPrint(kitchenElements, "Struk dapur terkirim ke printer", "Gagal mencetak struk dapur")
                            },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestGreen),
                            border = BorderStroke(1.dp, ForestGreen)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cetak Dapur", color = ForestGreen, fontSize = 12.sp, maxLines = 1)
                        }
                        Button(
                            onClick = {
                                val order = selectedOrderForPrint
                                val receiptElements = buildOrderReceiptElements(context, order, printOrderItems, printMenuItemsMap)
                                doPrint(receiptElements, "Resi terkirim ke printer", "Gagal mencetak resi")
                            },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cetak Resi", color = Color.White, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { selectedOrderForPrint = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tutup", color = Color.Gray)
                    }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

// ==========================================================
// POS KASIR: buat pesanan langsung untuk pelanggan yang memesan di tempat
// ==========================================================
@Composable
fun KasirOrderDialog(
    onDismiss: () -> Unit,
    onCreated: (Order) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var menu by remember { mutableStateOf<List<MenuItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var orderType by remember { mutableStateOf("Dine In") }
    var payment by remember { mutableStateOf("Tunai") }
    var customerName by remember { mutableStateOf("") }
    val cart = remember { mutableStateMapOf<String, Int>() }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        menu = try {
            SupabaseClient.db["menu_items"].select().decodeList<MenuItem>().filter { it.isAvailable }
        } catch (e: Exception) { emptyList() }
        loading = false
    }

    val subtotal = menu.sumOf { (cart[it.id] ?: 0) * it.price }
    val totalQty = cart.values.sum()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = LightGrayJco) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(OrangeJco, ForestGreen)))
                        .padding(horizontal = 12.dp, vertical = 14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                        }
                        Spacer(Modifier.width(4.dp))
                        Column {
                            Text("Pesanan Baru (Kasir)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Pesan langsung di tempat, tanpa aplikasi", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                        }
                    }
                }

                // Tipe pesanan + nama
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(1.dp, LightOrangeJco, RoundedCornerShape(12.dp))
                            .padding(4.dp)
                    ) {
                        listOf("Dine In" to Icons.Default.Restaurant, "Take Away" to Icons.Default.TakeoutDining).forEach { (type, icon) ->
                            val sel = orderType == type
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (sel) OrangeJco else Color.Transparent)
                                    .clickable { orderType = type }
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(icon, contentDescription = null, tint = if (sel) Color.White else OrangeJco, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(type, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (sel) Color.White else DarkCharcoal)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        placeholder = { Text("Nama pemesan (opsional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeJco,
                            unfocusedBorderColor = Color.LightGray,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                }

                // Daftar menu + stepper qty
                if (loading) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = OrangeJco)
                    }
                } else if (menu.isEmpty()) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("Belum ada menu tersedia.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(menu) { item ->
                            val qty = cart[item.id] ?: 0
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkCharcoal, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("Rp ${String.format("%,.0f", item.price)}", fontSize = 12.sp, color = OrangeJco)
                                    }
                                    IconButton(
                                        onClick = { if (qty > 0) { if (qty - 1 == 0) cart.remove(item.id) else cart[item.id] = qty - 1 } },
                                        enabled = qty > 0
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Kurang", tint = if (qty > 0) OrangeJco else Color.LightGray)
                                    }
                                    Text("$qty", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DarkCharcoal, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 22.dp))
                                    IconButton(onClick = { cart[item.id] = qty + 1 }) {
                                        Icon(Icons.Default.Add, contentDescription = "Tambah", tint = OrangeJco)
                                    }
                                }
                            }
                        }
                        item { Spacer(Modifier.height(4.dp)) }
                    }
                }

                // Bar bawah: metode bayar + total + simpan
                Surface(color = Color.White, shadowElevation = 12.dp) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Bayar:", fontSize = 13.sp, color = DarkCharcoal, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(10.dp))
                            listOf("Tunai", "QRIS").forEach { m ->
                                val sel = payment == m
                                Box(
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (sel) OrangeJco else LightGrayJco)
                                        .clickable { payment = m }
                                        .padding(horizontal = 18.dp, vertical = 7.dp)
                                ) {
                                    Text(m, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (sel) Color.White else DarkCharcoal)
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Total ($totalQty item)", fontSize = 12.sp, color = Color.Gray)
                                Text("Rp ${String.format("%,.0f", subtotal)}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
                            }
                            Button(
                                onClick = {
                                    if (totalQty == 0 || saving) return@Button
                                    saving = true
                                    scope.launch {
                                        try {
                                            val order = Order(
                                                userId = null,
                                                orderType = orderType,
                                                status = "Selesai",
                                                subtotal = subtotal,
                                                total = subtotal,
                                                paymentMethod = payment,
                                                paymentStatus = "Terbayar",
                                                notes = "Pesanan langsung di kasir",
                                                customerName = customerName.trim().ifBlank { null },
                                                cashierUsername = CartManager.currentStaff?.username
                                            )
                                            val inserted = try {
                                                SupabaseClient.db["orders"].insert(order) { select() }.decodeSingle<Order>()
                                            } catch (e: Exception) {
                                                if (e.message?.contains("customer_name", ignoreCase = true) == true) {
                                                    SupabaseClient.db["orders"].insert(order.copy(customerName = null)) { select() }.decodeSingle<Order>()
                                                } else throw e
                                            }
                                            val items = menu.filter { (cart[it.id] ?: 0) > 0 }.map {
                                                OrderItem(orderId = inserted.id!!, menuItemId = it.id, quantity = cart[it.id]!!, priceAtOrder = it.price)
                                            }
                                            if (items.isNotEmpty()) SupabaseClient.db["order_items"].insert(items)
                                            android.widget.Toast.makeText(context, "Pesanan kasir tersimpan!", android.widget.Toast.LENGTH_SHORT).show()
                                            onCreated(inserted)
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "Gagal menyimpan: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                                        } finally {
                                            saving = false
                                        }
                                    }
                                },
                                enabled = totalQty > 0 && !saving,
                                modifier = Modifier.height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
                            ) {
                                if (saving) {
                                    CircularProgressIndicator(color = OnAccentDark, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                } else {
                                    Icon(Icons.Default.Print, contentDescription = null, tint = OnAccentDark, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Simpan & Cetak", color = OnAccentDark, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminOrderCard(
    order: Order,
    drivers: List<StaffAccount>,
    onAccept: () -> Unit,
    onAssignDriverClick: () -> Unit,
    onShowReceipt: (String) -> Unit,
    onPrintReceipt: () -> Unit,
    onOpenChat: () -> Unit,
    onRejectPayment: (String) -> Unit,
    onLocalPayment: () -> Unit,
    onMarkReady: () -> Unit,
    onCompleteOrder: () -> Unit
) {
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectReasonInput by remember { mutableStateOf("") }
    var showConfirmPaymentDialog by remember { mutableStateOf(false) }
    var showLocalPaymentDialog by remember { mutableStateOf(false) }
    val assignedDriverName = remember(order.driverId, drivers) {
        drivers.find { it.id == order.driverId }?.name ?: "Belum Ditugaskan"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pesanan #${order.orderNumber}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ForestGreen)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (order.status) {
                                "Pending" -> Color(0xFFFEF3C7)
                                "Diproses" -> Color(0xFFDBEAFE)
                                "Siap" -> Color(0xFFE0F2FE)
                                "Diantar" -> Color(0xFFE0F2FE)
                                "Selesai" -> Color(0xFFD1FAE5)
                                "Dibatalkan" -> Color(0xFFFEE2E2)
                                else -> Color.LightGray
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = order.status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (order.status) {
                            "Pending" -> Color(0xFFD97706)
                            "Diproses" -> Color(0xFF2563EB)
                            "Siap" -> Color(0xFF0284C7)
                            "Diantar" -> Color(0xFF0284C7)
                            "Selesai" -> Color(0xFF059669)
                            "Dibatalkan" -> RedPromo
                            else -> Color.DarkGray
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            val isDelivery = order.orderType == "Delivery"
            val isTakeAway = order.orderType == "Take Away"
            // Warna badge dibedakan jelas per tipe: Delivery = biru, Take Away = ungu, Dine In = oranye.
            val typeBg = when {
                isDelivery -> Color(0xFFDCEFFB)
                isTakeAway -> Color(0xFFEDE7F6)
                else -> LightOrangeJco
            }
            val typeColor = when {
                isDelivery -> Color(0xFF2563EB)
                isTakeAway -> Color(0xFF7E57C2)
                else -> OrangeJco
            }
            val typeIcon = when {
                isDelivery -> Icons.Default.DeliveryDining
                isTakeAway -> Icons.Default.TakeoutDining
                else -> Icons.Default.Restaurant
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(typeBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = null,
                            tint = typeColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = order.orderType,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = typeColor
                        )
                    }
                }
            }
            if (isDelivery) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Alamat: ${order.deliveryAddress ?: "-"}", fontSize = 12.sp, color = DarkCharcoal, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("Total: Rp ${String.format("%,.0f", order.total)} | Pembayaran: ${order.paymentMethod} (${order.paymentStatus})", fontSize = 12.sp, color = Color.Gray)
            
            if (order.status == "Diantar" || order.status == "Selesai") {
                Text("Kurir: $assignedDriverName", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OrangeJco)
            }
            if (order.deliveryWithinTolerance == false) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WarningAmber, contentDescription = null, tint = RedPromo, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Jarak Tidak Sesuai (~${order.deliveryDistanceMeters?.toInt() ?: "-"}m dari alamat) - mohon dicek",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = RedPromo
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            // Baris 1: aksi sekunder/info. Dipisah dari aksi status agar tidak berebut
            // ruang horizontal -- sebelumnya semua tombol digabung 1 Row tanpa wrap,
            // sehingga saat "Lihat Bukti Bayar" ikut tampil, tombol "Konfirmasi
            // Pembayaran" terdorong keluar layar (tidak terlihat & tidak bisa ditekan).
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onPrintReceipt,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestGreen),
                    border = BorderStroke(1.dp, ForestGreen),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = "Cetak", tint = ForestGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cetak Struk", color = ForestGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onOpenChat,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangeJco),
                    border = BorderStroke(1.dp, OrangeJco),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = "Chat", tint = OrangeJco, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chat", color = OrangeJco, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                if (!order.paymentReceiptUrl.isNullOrBlank()) {
                    Button(
                        onClick = { onShowReceipt(order.paymentReceiptUrl) },
                        colors = ButtonDefaults.buttonColors(containerColor = LightOrangeJco),
                        border = BorderStroke(1.dp, OrangeJco),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text("Lihat Bukti Bayar", color = OrangeJco, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Baris 2: aksi status pesanan -- SELALU baris sendiri, full width, terbagi
            // rata (weight) agar tidak pernah terpotong berapa pun banyaknya tombol.
            when (order.status) {
                "Pending" -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (order.paymentStatus == "Menunggu Verifikasi") {
                            Button(
                                onClick = { showRejectDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(36.dp).weight(1f)
                            ) {
                                Text("Tolak Pembayaran", fontSize = 12.sp, color = Color.White)
                            }
                        }
                        Button(
                            onClick = { showConfirmPaymentDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp).weight(1f)
                        ) {
                            Text("Konfirmasi Pembayaran", fontSize = 12.sp, color = OnAccentDark, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                    // Pembayaran Lokal: kasir menandai pesanan LUNAS dibayar tunai di tempat
                    // (untuk pelanggan Dine In/Take Away yang bayar langsung di kasir).
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showLocalPaymentDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestGreen),
                        border = BorderStroke(1.dp, ForestGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bayar di Tempat (Tunai)", fontSize = 12.sp, color = ForestGreen, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
                "Diproses" -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (order.orderType == "Delivery") {
                        // Delivery butuh driver untuk mengantar.
                        Button(
                            onClick = onAssignDriverClick,
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(36.dp)
                        ) {
                            Text("Tugaskan Driver", fontSize = 12.sp, color = OnAccentDark, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Take Away / Dine In tidak butuh driver -- cukup tandai siap.
                        Button(
                            onClick = onMarkReady,
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(36.dp)
                        ) {
                            Text(
                                if (order.orderType == "Take Away") "Tandai Siap Diambil" else "Tandai Siap Disajikan",
                                fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                "Siap" -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onCompleteOrder,
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        Text("Selesaikan Pesanan", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                else -> {}
            }
        }
    }

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White,
            title = { Text("Tolak Pembayaran", fontWeight = FontWeight.Bold, color = DarkCharcoal) },
            text = {
                Column {
                    Text("Masukkan alasan penolakan pembayaran:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectReasonInput,
                        onValueChange = { rejectReasonInput = it },
                        placeholder = { Text("Contoh: Bukti transfer tidak valid/belum masuk") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rejectReasonInput.isNotBlank()) {
                            onRejectPayment(rejectReasonInput)
                            showRejectDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Tolak", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Konfirmasi ulang sebelum menyetujui pembayaran -- mencegah tombol
    // "Konfirmasi Pembayaran" tertekan tidak sengaja langsung memproses pesanan.
    if (showConfirmPaymentDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmPaymentDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White,
            title = { Text("Konfirmasi Pembayaran", fontWeight = FontWeight.Bold, color = DarkCharcoal) },
            text = {
                Text("Yakin ingin mengonfirmasi pembayaran untuk Pesanan #${order.orderNumber}? Pesanan akan lanjut diproses untuk pengantaran.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmPaymentDialog = false
                        onAccept()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
                ) {
                    Text("Ya, Konfirmasi", color = OnAccentDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmPaymentDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Pembayaran Lokal (bayar tunai di tempat) -- menandai pesanan LUNAS via kasir.
    if (showLocalPaymentDialog) {
        AlertDialog(
            onDismissRequest = { showLocalPaymentDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White,
            title = { Text("Bayar di Tempat (Tunai)", fontWeight = FontWeight.Bold, color = DarkCharcoal) },
            text = {
                Text("Tandai Pesanan #${order.orderNumber} sebagai LUNAS dibayar tunai di tempat? Metode pembayaran akan tercatat \"Tunai (Bayar di Tempat)\" dan pesanan langsung diproses.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLocalPaymentDialog = false
                        onLocalPayment()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                ) {
                    Text("Ya, Sudah Bayar Tunai", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocalPaymentDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

// ==========================================================
// TABS 1: MENU CATALOG MANAGEMENT
// ==========================================================
@Composable
fun TabAdminMenu() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var menuList by remember { mutableStateOf<List<MenuItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showManageCategoriesDialog by remember { mutableStateOf(false) }
    var selectedMenuForEdit by remember { mutableStateOf<MenuItem?>(null) }
    var menuToDelete by remember { mutableStateOf<MenuItem?>(null) }

    fun loadMenu(showLoadingSpinner: Boolean = false) {
        if (showLoadingSpinner) {
            isLoading = true
        }
        coroutineScope.launch {
            menuList = SupabaseClient.initializeMenuIfEmpty()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadMenu(showLoadingSpinner = true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(Brush.verticalGradient(listOf(OrangeJco, ForestGreen)))
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Kelola Menu",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text("Atur katalog & ketersediaan produk", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showManageCategoriesDialog = true },
                        border = BorderStroke(1.dp, Color.White),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Kategori", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kategori", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah", tint = OnAccentDark, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Menu", color = OnAccentDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = OrangeJco)
                    Text("Memuat menu...", fontSize = 13.sp, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (menuList.isEmpty()) {
                    item {
                        AdminEmptyState(
                            icon = Icons.Default.RestaurantMenu,
                            title = "Belum ada menu",
                            subtitle = "Tekan tombol Menu untuk menambahkan produk pertama."
                        )
                    }
                }
                items(menuList) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)).background(LightOrangeJco)
                            ) {
                                if (item.imageUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = item.imageUrl,
                                        contentDescription = item.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Restaurant,
                                        contentDescription = null,
                                        tint = OrangeJco,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                val originalPrice = item.getOriginalPrice()
                                if (originalPrice != null && originalPrice > item.price) {
                                    Text(
                                        text = "Promo: Rp ${String.format("%,.0f", item.price)} (Asli: Rp ${String.format("%,.0f", originalPrice)}) | Kat: ${item.category}",
                                        fontSize = 11.sp,
                                        color = OrangeJco,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else {
                                    Text("Rp ${String.format("%,.0f", item.price)} | Kategori: ${item.category}", fontSize = 11.sp, color = Color.Gray)
                                }
                                Text(
                                    text = if (item.isAvailable) "Tersedia" else "Habis",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.isAvailable) ForestGreen else Color.Red
                                )
                            }
                            Switch(
                                checked = item.isAvailable,
                                onCheckedChange = { isChecked ->
                                    menuList = menuList.map {
                                        if (it.id == item.id) it.copy(isAvailable = isChecked) else it
                                    }
                                    coroutineScope.launch {
                                        try {
                                            SupabaseClient.db["menu_items"].update({
                                                set("is_available", isChecked)
                                            }) { filter { eq("id", item.id) } }
                                            loadMenu(showLoadingSpinner = false)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            menuList = menuList.map {
                                                if (it.id == item.id) it.copy(isAvailable = !isChecked) else it
                                            }
                                            android.widget.Toast.makeText(context, "Gagal mengubah stok: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = ForestGreen,
                                    checkedTrackColor = ForestGreen.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.scale(0.8f)
                            )
                            IconButton(onClick = {
                                selectedMenuForEdit = item
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray)
                            }
                            IconButton(onClick = {
                                menuToDelete = item
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Menu Dialog
    if (showAddDialog) {
        AddOrEditMenuDialog(
            item = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, category, price, desc, imageUrl, isAvailable ->
                coroutineScope.launch {
                    try {
                        val newItem = MenuItem(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            category = category,
                            price = price,
                            description = desc,
                            imageUrl = imageUrl,
                            isAvailable = isAvailable
                        )
                        SupabaseClient.db["menu_items"].insert(newItem)
                        showAddDialog = false
                        loadMenu(showLoadingSpinner = false)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        android.widget.Toast.makeText(context, "Gagal menambahkan menu: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    // Edit Menu Dialog
    if (selectedMenuForEdit != null) {
        AddOrEditMenuDialog(
            item = selectedMenuForEdit,
            onDismiss = { selectedMenuForEdit = null },
            onSave = { name, category, price, desc, imageUrl, isAvailable ->
                coroutineScope.launch {
                    try {
                        SupabaseClient.db["menu_items"].update({
                            set("name", name)
                            set("category", category)
                            set("price", price)
                            set("description", desc)
                            set("image_url", imageUrl)
                            set("is_available", isAvailable)
                        }) {
                            filter { eq("id", selectedMenuForEdit?.id ?: "") }
                        }
                        selectedMenuForEdit = null
                        loadMenu(showLoadingSpinner = false)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        android.widget.Toast.makeText(context, "Gagal mengubah menu: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    if (showManageCategoriesDialog) {
        ManageCategoriesDialog(
            onDismiss = { showManageCategoriesDialog = false },
            onCategoriesUpdated = { loadMenu() }
        )
    }

    // Konfirmasi Hapus Menu
    if (menuToDelete != null) {
        val itemToDelete = menuToDelete!!
        AlertDialog(
            onDismissRequest = { menuToDelete = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White,
            title = { Text("Hapus Menu", fontWeight = FontWeight.Bold, color = DarkCharcoal) },
            text = { Text("Hapus menu ini? Tindakan tidak dapat dibatalkan.") },
            confirmButton = {
                Button(
                    onClick = {
                        menuToDelete = null
                        coroutineScope.launch {
                            try {
                                // Hapus gambar dari bucket bila berasal dari "menu-images"
                                if (itemToDelete.imageUrl.contains("menu-images")) {
                                    try {
                                        val fileName = itemToDelete.imageUrl.substringAfterLast('/')
                                        SupabaseClient.storage["menu-images"].delete(listOf(fileName))
                                    } catch (ex: Exception) {
                                        ex.printStackTrace()
                                    }
                                }
                                SupabaseClient.db["menu_items"].delete {
                                    filter { eq("id", itemToDelete.id) }
                                }
                                loadMenu(showLoadingSpinner = false)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                android.widget.Toast.makeText(context, "Gagal menghapus menu: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Hapus", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { menuToDelete = null }) {
                    Text("Batal", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun AddOrEditMenuDialog(
    item: MenuItem?,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, String, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var category by remember { mutableStateOf(item?.category ?: "Donuts") }
    var price by remember { mutableStateOf(item?.price?.toInt()?.toString() ?: "") }

    val parsedOriginalPrice = item?.getOriginalPrice()
    var originalPriceInput by remember { mutableStateOf(parsedOriginalPrice?.toInt()?.toString() ?: "") }
    val cleanDesc = item?.getCleanDescription() ?: ""
    var desc by remember { mutableStateOf(cleanDesc) }

    var imageUrl by remember { mutableStateOf(item?.imageUrl ?: "") }
    var isAvailable by remember { mutableStateOf(item?.isAvailable ?: true) }

    val context = LocalContext.current
    val dialogScope = rememberCoroutineScope()
    var isUploadingPhoto by remember { mutableStateOf(false) }
    
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isUploadingPhoto = true
            dialogScope.launch {
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                    if (bytes != null) {
                        val fileName = "${UUID.randomUUID()}.jpg"
                        val bucket = SupabaseClient.storage["menu-images"]
                        bucket.upload(fileName, bytes, upsert = true)
                        val newUrl = bucket.publicUrl(fileName)
                        // Hapus gambar yang sedang ditampilkan (state hidup), bukan original yang
                        // ditangkap sekali — sehingga cleanup mengikuti foto di layar walau admin
                        // mengganti foto beberapa kali sebelum menyimpan (tidak ada file orphan).
                        val previousUrl = imageUrl
                        if (previousUrl.isNotBlank() && previousUrl.contains("menu-images") && previousUrl != newUrl) {
                            try {
                                val oldFileName = previousUrl.substringAfter("menu-images/").substringBefore('?')
                                bucket.delete(listOf(oldFileName))
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            }
                        }
                        imageUrl = newUrl
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    dialogScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Gagal mengunggah foto menu: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                    }
                } finally {
                    isUploadingPhoto = false
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = { Text(if (item == null) "Tambah Item Baru" else "Edit Detail Item", fontWeight = FontWeight.Bold, color = DarkCharcoal) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Produk") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Kategori Produk") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ketik kategori baru...") }
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text("Pilih Kategori Cepat:", fontSize = 11.sp, color = Color.Gray)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DYNAMIC_CATEGORIES.forEach { cat ->
                        val isCatSelected = category == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (isCatSelected) OrangeJco else Color.White)
                                .border(
                                    1.dp,
                                    if (isCatSelected) OrangeJco else LightOrangeJco,
                                    RoundedCornerShape(50)
                                )
                                .clickable { category = cat }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCatSelected) Color.White else OrangeJco
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Harga Promo / Jual (IDR)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = originalPriceInput,
                    onValueChange = { originalPriceInput = it },
                    label = { Text("Harga Asli / Sebelum Promo (IDR)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Kosongkan jika tidak ada promo") }
                )
                
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Deskripsi") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("URL Gambar") },
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = LightOrangeJco),
                        border = BorderStroke(1.dp, OrangeJco),
                        enabled = !isUploadingPhoto,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.height(56.dp).padding(top = 8.dp)
                    ) {
                        if (isUploadingPhoto) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = OrangeJco, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "Pilih Foto", tint = OrangeJco)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tersedia", fontSize = 14.sp, color = DarkCharcoal)
                    Switch(
                        checked = isAvailable,
                        onCheckedChange = { isAvailable = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ForestGreen,
                            checkedTrackColor = ForestGreen.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val priceVal = price.toDoubleOrNull() ?: 0.0
                    val originalPriceVal = originalPriceInput.toDoubleOrNull()
                    val finalDesc = createDescriptionWithOriginalPrice(desc, originalPriceVal)
                    onSave(name, category, priceVal, finalDesc, imageUrl, isAvailable)
                },
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Simpan", color = OnAccentDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = Color.Gray)
            }
        }
    )
}

// ==========================================================
// TABS 2: PROMOTION BANNERS
// ==========================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabAdminBanners() {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var banners by remember { mutableStateOf<List<BannerItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var imageUrlInput by remember { mutableStateOf("") }
    var titleInput by remember { mutableStateOf("") }
    var isUploadingPhoto by remember { mutableStateOf(false) }
    var menuItemsList by remember { mutableStateOf<List<MenuItem>>(emptyList()) }
    var linkedMenuForNewBanner by remember { mutableStateOf<MenuItem?>(null) }
    var showMenuPickerFor by remember { mutableStateOf<String?>(null) } // "new" atau id banner yang sedang diedit linknya
    var hotPromoTitleInput by remember { mutableStateOf("Hot Promo") }
    var hotPromoSubtitleInput by remember { mutableStateOf("Penawaran spesial untukmu") }
    var isSavingHotPromoText by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isUploadingPhoto = true
            coroutineScope.launch {
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                    if (bytes != null) {
                        val fileName = "banners/${UUID.randomUUID()}.jpg"
                        val bucket = SupabaseClient.storage["menu-images"]
                        bucket.upload(fileName, bytes, upsert = true)
                        val newUrl = bucket.publicUrl(fileName)
                        // Hapus gambar lama dari bucket setelah upload baru berhasil
                        val previousUrl = imageUrlInput
                        if (previousUrl.isNotBlank() && previousUrl.contains("menu-images") && previousUrl != newUrl) {
                            try {
                                val oldFileName = previousUrl.substringAfter("menu-images/").substringBefore('?')
                                bucket.delete(listOf(oldFileName))
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            }
                        }
                        imageUrlInput = newUrl
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    coroutineScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Gagal mengunggah gambar banner: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                    }
                } finally {
                    isUploadingPhoto = false
                }
            }
        }
    }

    fun loadBanners() {
        isLoading = true
        coroutineScope.launch {
            try {
                val list = SupabaseClient.db["banners"].select().decodeList<BannerItem>()
                // Tabel "banners" juga dipakai sebagai penampung sentinel non-banner: kategori
                // ("category|..."), QRIS/bank ("payment_...", "bank_..."), dan URL update APK
                // ("app_apk_url"). Semua itu HARUS disaring agar tidak muncul sebagai "banner hantu"
                // di daftar kelola banner admin (ini akar bug "banner masih muncul padahal sudah dihapus").
                banners = list.filter { b ->
                    val t = b.title ?: ""
                    !t.startsWith("payment_") && !t.startsWith("category|") &&
                        !t.startsWith("bank_") && t != "app_apk_url"
                }
            } catch (e: Exception) {}
            finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadBanners()
        try {
            menuItemsList = SupabaseClient.db["menu_items"].select().decodeList<MenuItem>()
        } catch (e: Exception) {}
        try {
            val settingsList = SupabaseClient.db["settings"].select().decodeList<SettingsRow>()
            settingsList.find { it.key == "hot_promo_title" }?.description?.let { if (it.isNotBlank()) hotPromoTitleInput = it }
            settingsList.find { it.key == "hot_promo_subtitle" }?.description?.let { if (it.isNotBlank()) hotPromoSubtitleInput = it }
        } catch (e: Exception) {}
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(Brush.verticalGradient(listOf(OrangeJco, ForestGreen)))
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Column {
                Text(
                    "Kelola Banner Promo",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.SansSerif
                )
                Text("Atur banner carousel di beranda pelanggan", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {

        // Judul & Subjudul "Hot Promo" di beranda pelanggan
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Judul Hot Promo", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkCharcoal)
                Text("Tampil di atas carousel banner beranda pelanggan", fontSize = 11.sp, color = Color.Gray)
                OutlinedTextField(
                    value = hotPromoTitleInput,
                    onValueChange = { hotPromoTitleInput = it },
                    label = { Text("Judul") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = hotPromoSubtitleInput,
                    onValueChange = { hotPromoSubtitleInput = it },
                    label = { Text("Subjudul") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Button(
                    onClick = {
                        isSavingHotPromoText = true
                        coroutineScope.launch {
                            try {
                                SupabaseClient.db["settings"].update({
                                    set("description", hotPromoTitleInput)
                                }) { filter { eq("key", "hot_promo_title") } }
                                SupabaseClient.db["settings"].update({
                                    set("description", hotPromoSubtitleInput)
                                }) { filter { eq("key", "hot_promo_subtitle") } }
                                android.widget.Toast.makeText(context, "Judul Hot Promo tersimpan", android.widget.Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Gagal menyimpan: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                            } finally {
                                isSavingHotPromoText = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSavingHotPromoText,
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                ) {
                    Text(if (isSavingHotPromoText) "Menyimpan..." else "Simpan Judul", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Form Tambah Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Tambah Banner Baru", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkCharcoal)
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Judul Promo") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (imageUrlInput.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, LightOrangeJco, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = imageUrlInput,
                            contentDescription = "Preview Gambar Banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { imageUrlInput = "" },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .background(Color.White.copy(alpha = 0.7f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Hapus Gambar", tint = Color.Red)
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangeJco),
                        border = BorderStroke(1.dp, OrangeJco),
                        enabled = !isUploadingPhoto
                    ) {
                        if (isUploadingPhoto) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = OrangeJco, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mengunggah...")
                        } else {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "Pilih Gambar")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pilih Gambar Banner (Galeri)")
                        }
                    }
                }

                // Koneksi banner ke menu: saat pelanggan tap banner, menu ini akan ditampilkan
                var menuDropdownExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = menuDropdownExpanded,
                    onExpandedChange = { menuDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = linkedMenuForNewBanner?.name ?: "Tidak terhubung ke menu",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Hubungkan ke Menu (opsional)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = menuDropdownExpanded,
                        onDismissRequest = { menuDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Tidak terhubung ke menu") },
                            onClick = {
                                linkedMenuForNewBanner = null
                                menuDropdownExpanded = false
                            }
                        )
                        menuItemsList.forEach { menu ->
                            DropdownMenuItem(
                                text = { Text(menu.name) },
                                onClick = {
                                    linkedMenuForNewBanner = menu
                                    menuDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        if (imageUrlInput.isNotBlank()) {
                            coroutineScope.launch {
                                try {
                                    val newBanner = BannerItem(
                                        id = UUID.randomUUID().toString(),
                                        imageUrl = imageUrlInput,
                                        title = titleInput,
                                        linkedMenuItemId = linkedMenuForNewBanner?.id
                                    )
                                    SupabaseClient.db["banners"].insert(newBanner)
                                    imageUrlInput = ""
                                    titleInput = ""
                                    linkedMenuForNewBanner = null
                                    loadBanners()
                                } catch (e: Exception) {}
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Tambahkan Banner", color = OnAccentDark, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("Daftar Banner Aktif", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkCharcoal)
        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = OrangeJco)
                    Text("Memuat banner...", fontSize = 13.sp, color = Color.Gray)
                }
            }
        } else {
            // Column biasa (bukan LazyColumn) -- daftar banner ini sekarang bersarang di dalam
            // Column induk yang sudah verticalScroll, dan LazyColumn tidak boleh dipasang di
            // dalam parent yang juga scrollable (akan crash: infinite height constraints).
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (banners.isEmpty()) {
                    AdminEmptyState(
                        icon = Icons.Default.Bookmark,
                        title = "Belum ada banner",
                        subtitle = "Tambahkan banner promo untuk tampil di beranda."
                    )
                }
                banners.forEach { banner ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)).background(LightOrangeJco)
                            ) {
                                if (banner.imageUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = banner.imageUrl,
                                        contentDescription = banner.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.Bookmark, contentDescription = null, tint = OrangeJco, modifier = Modifier.align(Alignment.Center))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(banner.title ?: "Promo Spesial", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkCharcoal)
                                Text(banner.imageUrl, fontSize = 10.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                val linkedName = menuItemsList.find { it.id == banner.linkedMenuItemId }?.name
                                Text(
                                    if (linkedName != null) "Terhubung: $linkedName" else "Belum terhubung ke menu",
                                    fontSize = 10.sp,
                                    color = if (linkedName != null) ForestGreen else Color.Gray,
                                    fontWeight = if (linkedName != null) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            IconButton(onClick = { showMenuPickerFor = banner.id }) {
                                Icon(Icons.Default.Link, contentDescription = "Hubungkan ke Menu", tint = OrangeJco)
                            }
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    try {
                                        SupabaseClient.db["banners"].delete {
                                            filter { eq("id", banner.id ?: "") }
                                        }
                                        loadBanners()
                                    } catch (e: Exception) {}
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
        }
    }

    if (showMenuPickerFor != null) {
        val editingBanner = banners.find { it.id == showMenuPickerFor }
        AlertDialog(
            onDismissRequest = { showMenuPickerFor = null },
            title = { Text("Hubungkan Banner ke Menu", fontWeight = FontWeight.Bold, color = DarkCharcoal) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Tidak terhubung ke menu",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                coroutineScope.launch {
                                    try {
                                        SupabaseClient.db["banners"].update({
                                            set("linked_menu_item_id", null as String?)
                                        }) { filter { eq("id", editingBanner?.id ?: "") } }
                                        loadBanners()
                                    } catch (e: Exception) {}
                                    showMenuPickerFor = null
                                }
                            }
                            .padding(12.dp),
                        fontSize = 13.sp,
                        color = if (editingBanner?.linkedMenuItemId == null) OrangeJco else DarkCharcoal,
                        fontWeight = if (editingBanner?.linkedMenuItemId == null) FontWeight.Bold else FontWeight.Normal
                    )
                    menuItemsList.forEach { menu ->
                        val selected = editingBanner?.linkedMenuItemId == menu.id
                        Text(
                            menu.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    coroutineScope.launch {
                                        try {
                                            SupabaseClient.db["banners"].update({
                                                set("linked_menu_item_id", menu.id)
                                            }) { filter { eq("id", editingBanner?.id ?: "") } }
                                            loadBanners()
                                        } catch (e: Exception) {}
                                        showMenuPickerFor = null
                                    }
                                }
                                .padding(12.dp),
                            fontSize = 13.sp,
                            color = if (selected) OrangeJco else DarkCharcoal,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMenuPickerFor = null }) {
                    Text("Tutup", color = Color.Gray)
                }
            }
        )
    }
}

// ==========================================================
// TABS 2B: FINANCIAL & TRANSACTION REPORTS
// ==========================================================
@Composable
fun TabAdminReports() {
    val coroutineScope = rememberCoroutineScope()
    var completedOrders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var menuItemsMap by remember { mutableStateOf<Map<String, MenuItem>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var reportType by remember { mutableStateOf("Harian") } // "Harian" or "Bulanan"
    val context = LocalContext.current
    var showPrintPreview by remember { mutableStateOf(false) }

    fun loadReportData() {
        isLoading = true
        coroutineScope.launch {
            try {
                // Fetch completed orders
                val allOrders = SupabaseClient.db["orders"].select {
                    filter { eq("status", "Selesai") }
                }.decodeList<Order>()
                completedOrders = allOrders.sortedByDescending { it.createdAt }

                // Fetch menu items for names
                val menus = SupabaseClient.db["menu_items"].select().decodeList<MenuItem>()
                menuItemsMap = menus.associateBy { it.id }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadReportData()
    }

    val todayStr = remember {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    }
    val thisMonthStr = remember {
        java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(java.util.Date())
    }

    var selectedDate by remember { mutableStateOf(todayStr) }
    var selectedMonth by remember { mutableStateOf(thisMonthStr) }

    val filteredOrders = remember(completedOrders, reportType, selectedDate, selectedMonth) {
        completedOrders.filter { order ->
            val datePart = order.createdAt?.take(10) ?: ""
            if (reportType == "Harian") {
                datePart == selectedDate
            } else {
                datePart.startsWith(selectedMonth)
            }
        }
    }

    val totalRevenue = filteredOrders.sumOf { it.total }
    val totalOrdersCount = filteredOrders.size
    val qrisRevenue = filteredOrders.filter { it.paymentMethod.contains("QRIS", ignoreCase = true) }.sumOf { it.total }
    val transferRevenue = filteredOrders.filter { it.paymentMethod.contains("Transfer", ignoreCase = true) }.sumOf { it.total }

    val reportText = remember(filteredOrders, reportType, totalRevenue, totalOrdersCount, qrisRevenue, transferRevenue, selectedDate, selectedMonth) {
        buildString {
            appendLine("==================================")
            appendLine("        LAPORAN ${reportType.uppercase()}")
            appendLine("         DAPOER LAVANA")
            appendLine("==================================")
            appendLine("Periode   : " + (if (reportType == "Harian") selectedDate else selectedMonth))
            appendLine("Dicetak   : " + java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date()))
            appendLine("----------------------------------")
            appendLine(String.format("Total Omset  : Rp %,.0f", totalRevenue))
            appendLine(String.format("Total Order  : %d Pesanan", totalOrdersCount))
            appendLine("----------------------------------")
            appendLine("Metode Pembayaran:")
            appendLine(String.format(" - QRIS      : Rp %,.0f", qrisRevenue))
            appendLine(String.format(" - Transfer  : Rp %,.0f", transferRevenue))
            appendLine("----------------------------------")
            appendLine("Daftar Transaksi:")
            if (filteredOrders.isEmpty()) {
                appendLine(" Tidak ada transaksi.")
            } else {
                filteredOrders.forEach { order ->
                    val time = order.createdAt?.takeLast(8)?.take(5) ?: ""
                    appendLine(String.format(" #%d [%s] Rp %,.0f (%s)", order.orderNumber, time, order.total, order.paymentMethod))
                }
            }
            appendLine("==================================")
            appendLine("       LAPORAN SELESAI DICETAK")
            appendLine("==================================")
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Floating teal gradient header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(Brush.verticalGradient(listOf(OrangeJco, ForestGreen)))
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Laporan Keuangan",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text("Rekap omset & transaksi selesai", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                }
                IconButton(
                    onClick = { loadReportData() },
                    modifier = Modifier.background(Color.White.copy(alpha = 0.18f), CircleShape)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                }
            }
        }

        Column(modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp)) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(LightOrangeJco)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Harian", "Bulanan").forEach { type ->
                val selected = reportType == type
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(11.dp))
                        .clickable { reportType = type }
                        .background(if (selected) OrangeJco else Color.Transparent)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Laporan $type",
                        color = if (selected) Color.White else OrangeJco,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Picker Tanggal/Periode Laporan
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(1.dp, LightOrangeJco, RoundedCornerShape(12.dp))
                .clickable {
                    val calendar = java.util.Calendar.getInstance()
                    val currentVal = if (reportType == "Harian") selectedDate else "$selectedMonth-01"
                    try {
                        val parts = currentVal.split("-")
                        calendar.set(java.util.Calendar.YEAR, parts[0].toInt())
                        calendar.set(java.util.Calendar.MONTH, parts[1].toInt() - 1)
                        calendar.set(java.util.Calendar.DAY_OF_MONTH, parts[2].toInt())
                    } catch (e: Exception) {}

                    android.app.DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            if (reportType == "Harian") {
                                selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                            } else {
                                selectedMonth = String.format("%04d-%02d", year, month + 1)
                            }
                        },
                        calendar.get(java.util.Calendar.YEAR),
                        calendar.get(java.util.Calendar.MONTH),
                        calendar.get(java.util.Calendar.DAY_OF_MONTH)
                    ).show()
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.DateRange, contentDescription = "Pilih Periode", tint = OrangeJco, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (reportType == "Harian") "Pilih Tanggal: " else "Pilih Bulan: ", fontSize = 13.sp, color = Color.Gray)
            Text(if (reportType == "Harian") selectedDate else selectedMonth, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
            Spacer(modifier = Modifier.weight(1f))
            Text("Ubah", fontSize = 11.sp, color = OrangeJco, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = OrangeJco)
                    Text("Memuat laporan...", fontSize = 13.sp, color = Color.Gray)
                }
            }
        } else {
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Total Omset", fontSize = 11.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Rp ${String.format("%,.0f", totalRevenue)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Total Transaksi", fontSize = 11.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("$totalOrdersCount Order", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OrangeJco)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Metode Pembayaran", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkCharcoal)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total QRIS:", fontSize = 12.sp, color = Color.Gray)
                            Text("Rp ${String.format("%,.0f", qrisRevenue)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Transfer:", fontSize = 12.sp, color = Color.Gray)
                            Text("Rp ${String.format("%,.0f", transferRevenue)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Daftar Transaksi Selesai", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkCharcoal)
                    
                    Button(
                        onClick = { showPrintPreview = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "Cetak", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cetak Laporan", color = Color.White, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    if (filteredOrders.isEmpty()) {
                        item {
                            AdminEmptyState(
                                icon = Icons.Default.Assessment,
                                title = "Belum ada transaksi",
                                subtitle = "Tidak ada transaksi untuk periode ini."
                            )
                        }
                    } else {
                        items(filteredOrders) { order ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Pesanan #${order.orderNumber}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("Waktu: ${order.createdAt?.take(16)?.replace("T", " ") ?: ""}", fontSize = 10.sp, color = Color.Gray)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Rp ${String.format("%,.0f", order.total)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ForestGreen)
                                            Text(order.paymentMethod, fontSize = 10.sp, color = OrangeJco)
                                        }
                                    }
                                    if (order.deliveryWithinTolerance == false) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.WarningAmber, contentDescription = null, tint = RedPromo, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "Jarak pengantaran tidak sesuai (~${order.deliveryDistanceMeters?.toInt() ?: "-"}m)",
                                                fontSize = 10.sp,
                                                color = RedPromo,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }

    if (showPrintPreview) {
        val toastContext = LocalContext.current
        AlertDialog(
            onDismissRequest = { showPrintPreview = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White,
            title = { Text("Preview Cetak Laporan", fontWeight = FontWeight.Bold, color = DarkCharcoal) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .verticalScroll(rememberScrollState())
                        .background(Color.White)
                        .border(1.dp, Color.LightGray)
                        .padding(16.dp)
                ) {
                    Text(
                        text = reportText,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color.Black
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val reportLines = reportText.split("\n")
                                val file = PdfHelper.generateTextPdf(context, "Laporan-Penjualan", reportLines)
                                if (file != null) {
                                    PdfHelper.sharePdf(context, file)
                                    android.widget.Toast.makeText(context, "PDF laporan dibuat", android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    android.widget.Toast.makeText(context, "Gagal membuat PDF laporan.", android.widget.Toast.LENGTH_LONG).show()
                                }
                            },
                            border = BorderStroke(1.dp, OrangeJco),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangeJco),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Simpan PDF", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Unduh PDF", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val savedPrinter = PrinterManager.getSavedPrinter(context)
                                if (savedPrinter == null) {
                                    android.widget.Toast.makeText(context, "Pilih printer dulu di Pengaturan Printer", android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    coroutineScope.launch {
                                        val result = PrinterManager.printText(context, reportText)
                                        result.fold(
                                            onSuccess = {
                                                android.widget.Toast.makeText(context, "Laporan terkirim ke printer", android.widget.Toast.LENGTH_LONG).show()
                                            },
                                            onFailure = { ex ->
                                                android.widget.Toast.makeText(context, ex.message ?: "Gagal mencetak laporan", android.widget.Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    }
                                    showPrintPreview = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = "Cetak", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cetak Thermal", color = Color.White, fontSize = 12.sp)
                        }
                    }
                    TextButton(
                        onClick = { showPrintPreview = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Tutup", color = Color.Gray)
                    }
                }
            }
        )
    }
}

// ==========================================================
// TABS 3: PROFILE & LOGOUT
// ==========================================================
@Composable
fun TabAdminProfile(onLogout: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var baseFee by remember { mutableStateOf("") }
    var per50mFee by remember { mutableStateOf("") }
    var cafeLat by remember { mutableStateOf("") }
    var cafeLon by remember { mutableStateOf("") }
    var isSavingSettings by remember { mutableStateOf(false) }
    var isSettingsLoading by remember { mutableStateOf(true) }
    var isRestaurantOpen by remember { mutableStateOf(true) }
    var isTogglingRestaurantOpen by remember { mutableStateOf(false) }
    var adminContactInput by remember { mutableStateOf(CartManager.currentStaff?.contactNumber ?: "") }
    var isSavingAdminContact by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<com.lavana.dapoer.data.SearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var shopAddress by remember { mutableStateOf("Memuat alamat...") }

    var qrisUrl by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var bankNo by remember { mutableStateOf("") }
    var bankOwner by remember { mutableStateOf("") }
    var isQrisUploading by remember { mutableStateOf(false) }

    // Staff Management States
    var staffList by remember { mutableStateOf<List<StaffAccount>>(emptyList()) }
    var isStaffLoading by remember { mutableStateOf(false) }
    var regUsername by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regName by remember { mutableStateOf("") }
    var regContact by remember { mutableStateOf("") }
    var regRole by remember { mutableStateOf("Driver") }
    var isRegisteringStaff by remember { mutableStateOf(false) }
    var regErrorMessage by remember { mutableStateOf<String?>(null) }

    var isSimulating by remember { mutableStateOf(false) }
    var simulationStep by remember { mutableStateOf<String?>(null) }

    var showPrinterSettings by remember { mutableStateOf(false) }

    fun loadStaff() {
        isStaffLoading = true
        coroutineScope.launch {
            try {
                staffList = SupabaseClient.db["staff_accounts"].select().decodeList<StaffAccount>()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isStaffLoading = false
            }
        }
    }

    fun startE2ESimulation() {
        isSimulating = true
        coroutineScope.launch {
            try {
                simulationStep = "1. Mendaftarkan Akun Driver ('driver_sim')..."
                val driverAcc = StaffAccount(
                    username = "driver_sim",
                    password = "driver123",
                    role = "Driver",
                    name = "Driver Simulator",
                    contactNumber = "08999999999"
                )
                try {
                    SupabaseClient.db["staff_accounts"].delete {
                        filter { eq("username", "driver_sim") }
                    }
                } catch (e: Exception) {}
                SupabaseClient.db["staff_accounts"].insert(driverAcc)
                kotlinx.coroutines.delay(2000)

                simulationStep = "2. Mendaftarkan Akun Kasir ('kasir_sim')..."
                val kasirAcc = StaffAccount(
                    username = "kasir_sim",
                    password = "kasir123",
                    role = "Kasir",
                    name = "Kasir Simulator",
                    contactNumber = "08777777777"
                )
                try {
                    SupabaseClient.db["staff_accounts"].delete {
                        filter { eq("username", "kasir_sim") }
                    }
                } catch (e: Exception) {}
                SupabaseClient.db["staff_accounts"].insert(kasirAcc)
                kotlinx.coroutines.delay(2000)

                simulationStep = "3. Mengisi Menu Baru ('Kopi Susu Gula Aren')..."
                val menuSim = MenuItem(
                    id = "00000000-0000-0000-0000-999900000001",
                    name = "Kopi Susu Gula Aren",
                    description = "Kopi espresso dengan gula aren dan susu segar.",
                    price = 18000.0,
                    category = "Coffee",
                    imageUrl = "https://images.unsplash.com/photo-1541167760496-1628856ab772?w=500",
                    isAvailable = true
                )
                try {
                    SupabaseClient.db["menu_items"].delete {
                        filter { eq("id", "00000000-0000-0000-0000-999900000001") }
                    }
                } catch (e: Exception) {}
                SupabaseClient.db["menu_items"].insert(menuSim)
                kotlinx.coroutines.delay(2000)

                simulationStep = "4. Simulasi Pelanggan membuat order baru..."
                val orderSim = Order(
                    userId = "7838c359-d924-468b-b5c4-398925c3194c",
                    customerPhone = "08123456789",
                    orderType = "Delivery",
                    status = "Pending",
                    paymentMethod = "Transfer",
                    paymentStatus = "Belum Bayar",
                    paymentReceiptUrl = "https://mtjyggxyjojcvcjxiblo.supabase.co/storage/v1/object/public/menu-images/qris_barcode.png",
                    deliveryAddress = "Jalan Merdeka No. 10, Jakarta",
                    deliveryDistanceKm = 3.5,
                    deliveryFee = 8000.0,
                    subtotal = 18000.0,
                    total = 26000.0,
                    coordinates = "-6.2297,106.8296"
                )
                val inserted = SupabaseClient.db["orders"].insert(orderSim) {
                    select()
                }.decodeSingle<Order>()
                val orderId = inserted.id ?: ""
                
                val itemSim = OrderItem(
                    orderId = orderId,
                    menuItemId = "00000000-0000-0000-0000-999900000001",
                    quantity = 1,
                    priceAtOrder = 18000.0,
                    notes = "Kurangi gula"
                )
                try {
                    SupabaseClient.db["order_items"].insert(itemSim)
                } catch (e: Exception) {}
                
                kotlinx.coroutines.delay(2000)

                simulationStep = "5. Simulasi Kasir memverifikasi & konfirmasi pembayaran..."
                SupabaseClient.db["orders"].update({
                    set("status", "Diproses")
                    set("payment_status", "Terbayar")
                    set("cashier_username", "kasir_sim")
                }) { filter { eq("id", orderId) } }
                kotlinx.coroutines.delay(2500)

                simulationStep = "6. Simulasi Admin menugaskan Driver..."
                val fetchedDriver = SupabaseClient.db["staff_accounts"].select {
                    filter { eq("username", "driver_sim") }
                }.decodeSingle<StaffAccount>()
                val dId = fetchedDriver.id ?: ""
                SupabaseClient.db["orders"].update({
                    set("status", "Diantar")
                    set("driver_id", dId)
                }) { filter { eq("id", orderId) } }
                CartManager.startDriverSimulation(orderId, "-6.2297,106.8296")
                kotlinx.coroutines.delay(3000)

                simulationStep = "7. Simulasi Driver menyelesaikan pengantaran..."
                SupabaseClient.db["orders"].update({
                    set("status", "Selesai")
                }) { filter { eq("id", orderId) } }
                kotlinx.coroutines.delay(2000)

                simulationStep = "Simulasi Selesai! Pesanan telah diproses dari Awal hingga Selesai."
                loadStaff()
            } catch (e: Exception) {
                simulationStep = "Error: ${e.message}"
            } finally {
                isSimulating = false
            }
        }
    }

    fun startPendingOrderSimulation() {
        isSimulating = true
        coroutineScope.launch {
            try {
                simulationStep = "1. Mendaftarkan Akun Driver ('driver_sim')..."
                val driverAcc = StaffAccount(
                    username = "driver_sim",
                    password = "driver123",
                    role = "Driver",
                    name = "Driver Simulator",
                    contactNumber = "08999999999"
                )
                try {
                    SupabaseClient.db["staff_accounts"].delete {
                        filter { eq("username", "driver_sim") }
                    }
                } catch (e: Exception) {}
                SupabaseClient.db["staff_accounts"].insert(driverAcc)
                kotlinx.coroutines.delay(1000)

                simulationStep = "2. Mendaftarkan Akun Kasir ('kasir_sim')..."
                val kasirAcc = StaffAccount(
                    username = "kasir_sim",
                    password = "kasir123",
                    role = "Kasir",
                    name = "Kasir Simulator",
                    contactNumber = "08777777777"
                )
                try {
                    SupabaseClient.db["staff_accounts"].delete {
                        filter { eq("username", "kasir_sim") }
                    }
                } catch (e: Exception) {}
                SupabaseClient.db["staff_accounts"].insert(kasirAcc)
                kotlinx.coroutines.delay(1000)

                simulationStep = "3. Mengisi Menu Baru ('Kopi Susu Gula Aren')..."
                val menuSim = MenuItem(
                    id = "00000000-0000-0000-0000-999900000001",
                    name = "Kopi Susu Gula Aren",
                    description = "Kopi espresso dengan gula aren dan susu segar.",
                    price = 18000.0,
                    category = "Coffee",
                    imageUrl = "https://images.unsplash.com/photo-1541167760496-1628856ab772?w=500",
                    isAvailable = true
                )
                try {
                    SupabaseClient.db["menu_items"].delete {
                        filter { eq("id", "00000000-0000-0000-0000-999900000001") }
                    }
                } catch (e: Exception) {}
                SupabaseClient.db["menu_items"].insert(menuSim)
                kotlinx.coroutines.delay(1000)

                simulationStep = "4. Membuat Order Baru (Status: Pending)..."
                val orderSim = Order(
                    userId = "7838c359-d924-468b-b5c4-398925c3194c",
                    customerPhone = "08123456789",
                    orderType = "Delivery",
                    status = "Pending",
                    paymentMethod = "Transfer",
                    paymentStatus = "Belum Bayar",
                    paymentReceiptUrl = "https://mtjyggxyjojcvcjxiblo.supabase.co/storage/v1/object/public/menu-images/qris_barcode.png",
                    deliveryAddress = "Jalan Merdeka No. 10, Jakarta",
                    deliveryDistanceKm = 3.5,
                    deliveryFee = 8000.0,
                    subtotal = 18000.0,
                    total = 26000.0,
                    coordinates = "-6.2297,106.8296"
                )
                val inserted = SupabaseClient.db["orders"].insert(orderSim) {
                    select()
                }.decodeSingle<Order>()
                val orderId = inserted.id ?: ""

                val itemSim = OrderItem(
                    orderId = orderId,
                    menuItemId = "00000000-0000-0000-0000-999900000001",
                    quantity = 1,
                    priceAtOrder = 18000.0,
                    notes = "Kurangi gula"
                )
                try {
                    SupabaseClient.db["order_items"].insert(itemSim)
                } catch (e: Exception) {}

                simulationStep = "Pesanan Simulasi Baru (Pending) Berhasil Dibuat!"
                loadStaff()
            } catch (e: Exception) {
                simulationStep = "Error: ${e.message}"
            } finally {
                isSimulating = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadStaff()
        
        try {
            val settingsList = SupabaseClient.db["settings"].select().decodeList<SettingsRow>()
            settingsList.forEach { row ->
                when (row.key) {
                    "delivery_base_fee" -> baseFee = row.value.toInt().toString()
                    "delivery_per_50m_fee" -> per50mFee = row.value.toInt().toString()
                    "cafe_latitude" -> cafeLat = row.value.toString()
                    "cafe_longitude" -> cafeLon = row.value.toString()
                    "restaurant_open" -> isRestaurantOpen = row.value >= 1.0
                }
            }
        } catch (e: Exception) {
            baseFee = "5000"
            per50mFee = "200"
            cafeLat = "-6.2297"
            cafeLon = "106.8296"
        }
        
        try {
            val bannerList = SupabaseClient.db["banners"].select().decodeList<BannerItem>()
            val qrisBanner = bannerList.firstOrNull { it.title == "payment_qris" }
            if (qrisBanner != null) {
                qrisUrl = qrisBanner.imageUrl
            }
            
            val bankBanner = bannerList.firstOrNull { it.title?.startsWith("payment_bank|") == true }
            if (bankBanner != null && bankBanner.title != null) {
                val parts = bankBanner.title.split("|")
                if (parts.size >= 4) {
                    bankName = parts[1]
                    bankNo = parts[2]
                    bankOwner = parts[3]
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isSettingsLoading = false
        }
    }

    LaunchedEffect(cafeLat, cafeLon) {
        val lat = cafeLat.toDoubleOrNull()
        val lon = cafeLon.toDoubleOrNull()
        if (lat != null && lon != null) {
            val addr = com.lavana.dapoer.data.GeocodingHelper.reverseGeocode(lat, lon)
            shopAddress = if (addr.isNotBlank()) addr else "$lat, $lon"
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length > 2) {
            kotlinx.coroutines.delay(500)
            isSearching = true
            suggestions = com.lavana.dapoer.data.GeocodingHelper.searchAddress(searchQuery)
            isSearching = false
        } else {
            suggestions = emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(100.dp).clip(CircleShape).background(LightOrangeJco).border(2.dp, OrangeJco, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.SupervisorAccount, contentDescription = "Admin", tint = OrangeJco, modifier = Modifier.size(60.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(CartManager.currentStaff?.name ?: "Admin Dapoer Lavana", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
        Text("Username: ${CartManager.currentStaff?.username ?: "admin@lavana.com"}", fontSize = 14.sp, color = Color.Gray)
        Text("Level Akses: ${CartManager.currentStaff?.role ?: "Owner / Administrator"}", fontSize = 12.sp, color = OrangeJco, fontWeight = FontWeight.Bold)

        if (!isSettingsLoading) {
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                val contactContext = LocalContext.current
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Nomor HP/WA Admin", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ForestGreen)
                    Text("Dicetak di resi sebagai kontak yang bisa dihubungi pelanggan", fontSize = 11.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = adminContactInput,
                        onValueChange = { adminContactInput = it },
                        placeholder = { Text("Contoh: 0812xxxxxxx") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            val staffId = CartManager.currentStaff?.id
                            if (staffId == null) {
                                android.widget.Toast.makeText(contactContext, "Akun staff tidak ditemukan, silakan login ulang", android.widget.Toast.LENGTH_LONG).show()
                            } else {
                                isSavingAdminContact = true
                                coroutineScope.launch {
                                    try {
                                        val newContact = adminContactInput.trim()
                                        SupabaseClient.db["staff_accounts"].update({
                                            set("contact_number", newContact)
                                        }) { filter { eq("id", staffId) } }
                                        CartManager.currentStaff = CartManager.currentStaff?.copy(contactNumber = newContact)
                                        CartManager.currentStaff?.let { staff ->
                                            val jsonStr = kotlinx.serialization.json.Json.encodeToString(StaffAccount.serializer(), staff)
                                            contactContext.getSharedPreferences("lavana_prefs", android.content.Context.MODE_PRIVATE)
                                                .edit().putString("logged_in_staff_json", jsonStr).apply()
                                        }
                                        android.widget.Toast.makeText(contactContext, "Nomor HP berhasil disimpan", android.widget.Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(contactContext, "Gagal menyimpan: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                                    } finally {
                                        isSavingAdminContact = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSavingAdminContact,
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                    ) {
                        Text(if (isSavingAdminContact) "Menyimpan..." else "Simpan Nomor HP", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                val toggleContext = LocalContext.current
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Status Warung", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ForestGreen)
                        Text(
                            if (isRestaurantOpen) "Buka - pelanggan bisa memesan" else "Tutup - pelanggan tidak bisa memesan",
                            fontSize = 12.sp,
                            color = if (isRestaurantOpen) ForestGreen else RedPromo
                        )
                    }
                    Switch(
                        checked = isRestaurantOpen,
                        onCheckedChange = { checked ->
                            val previous = isRestaurantOpen
                            isRestaurantOpen = checked
                            isTogglingRestaurantOpen = true
                            coroutineScope.launch {
                                try {
                                    SupabaseClient.db["settings"].update({
                                        set("value", if (checked) 1.0 else 0.0)
                                    }) { filter { eq("key", "restaurant_open") } }
                                } catch (e: Exception) {
                                    isRestaurantOpen = previous
                                    android.widget.Toast.makeText(toggleContext, "Gagal mengubah status warung: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                                } finally {
                                    isTogglingRestaurantOpen = false
                                }
                            }
                        },
                        enabled = !isTogglingRestaurantOpen,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ForestGreen)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Pengaturan Delivery & Toko", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ForestGreen)
                    
                    OutlinedTextField(
                        value = baseFee,
                        onValueChange = { baseFee = it },
                        label = { Text("Tarif Dasar Delivery (IDR)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = per50mFee,
                        onValueChange = { per50mFee = it },
                        label = { Text("Ongkos per 50 Meter (IDR)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Search Location
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Cari Lokasi Toko") },
                            placeholder = { Text("Ketik nama tempat/jalan...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = {
                                if (searchQuery.isNotBlank()) {
                                    isSearching = true
                                    coroutineScope.launch {
                                        suggestions = com.lavana.dapoer.data.GeocodingHelper.searchAddress(searchQuery)
                                        isSearching = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeJco),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("Cari", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    if (isSearching) {
                        CircularProgressIndicator(color = OrangeJco, modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally).padding(top = 8.dp))
                    }

                    if (suggestions.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .heightIn(max = 150.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color.LightGray)
                        ) {
                            LazyColumn(modifier = Modifier.padding(8.dp)) {
                                items(suggestions) { result ->
                                    Text(
                                        text = result.displayName,
                                        fontSize = 11.sp,
                                        color = DarkCharcoal,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                cafeLat = result.latitude.toString()
                                                cafeLon = result.longitude.toString()
                                                shopAddress = result.displayName
                                                suggestions = emptyList()
                                                searchQuery = ""
                                            }
                                            .padding(vertical = 8.dp, horizontal = 4.dp)
                                    )
                                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = shopAddress,
                        onValueChange = { shopAddress = it },
                        label = { Text("Alamat Toko Saat Ini") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Tentukan Lokasi Toko di Peta (Ketuk untuk pinpoint):", fontSize = 12.sp, color = Color.Gray)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                    ) {
                        val currentLat = cafeLat.toDoubleOrNull() ?: -6.2297
                        val currentLon = cafeLon.toDoubleOrNull() ?: 106.8296
                        GoogleMapView(
                            center = LatLng(currentLat, currentLon),
                            markers = listOf(LatLng(currentLat, currentLon) to "Lokasi Toko"),
                            onMapClick = { p ->
                                cafeLat = p.latitude.toString()
                                cafeLon = p.longitude.toString()
                                coroutineScope.launch {
                                    val addr = com.lavana.dapoer.data.GeocodingHelper.reverseGeocode(p.latitude, p.longitude)
                                    if (addr.isNotBlank()) {
                                        shopAddress = addr
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text("Pengaturan Metode Pembayaran", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkCharcoal)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = { Text("Nama Bank (Mobile Banking)") },
                        placeholder = { Text("Contoh: BCA, Mandiri") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = bankNo,
                        onValueChange = { bankNo = it },
                        label = { Text("Nomor Rekening") },
                        placeholder = { Text("Masukkan nomor rekening...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = bankOwner,
                        onValueChange = { bankOwner = it },
                        label = { Text("Nama Pemilik Rekening") },
                        placeholder = { Text("Nama sesuai buku tabungan...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("QRIS Code Pembayaran", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    val context = LocalContext.current
                    val qrisPickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri ->
                        if (uri != null) {
                            isQrisUploading = true
                            coroutineScope.launch {
                                try {
                                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                    if (bytes != null) {
                                        val fileName = "qris_${System.currentTimeMillis()}.jpg"
                                        val bucket = SupabaseClient.storage["menu-images"]
                                        bucket.upload(fileName, bytes)
                                        val publicUrl = bucket.publicUrl(fileName)
                                        // Hapus QRIS lama dari bucket setelah upload baru berhasil
                                        val previousQris = qrisUrl
                                        if (previousQris.isNotBlank() && previousQris.contains("menu-images") && previousQris != publicUrl) {
                                            try {
                                                val oldFileName = previousQris.substringAfter("menu-images/").substringBefore('?')
                                                bucket.delete(listOf(oldFileName))
                                            } catch (ex: Exception) {
                                                ex.printStackTrace()
                                            }
                                        }
                                        qrisUrl = publicUrl
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    coroutineScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                        android.widget.Toast.makeText(context, "Gagal mengunggah foto QRIS: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                } finally {
                                    isQrisUploading = false
                                }
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (qrisUrl.isNotBlank()) {
                            AsyncImage(
                                model = qrisUrl,
                                contentDescription = "QRIS Code",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Belum ada QRIS", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Button(
                                onClick = { qrisPickerLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isQrisUploading
                            ) {
                                Text(if (isQrisUploading) "Mengunggah..." else "Pilih Foto QRIS", color = Color.White, fontSize = 12.sp)
                            }
                            if (qrisUrl.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                TextButton(
                                    onClick = { qrisUrl = "" }
                                ) {
                                    Text("Hapus QRIS", color = Color.Red, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
                            isSavingSettings = true
                            coroutineScope.launch {
                                try {
                                    if (shopAddress.isNotBlank() && shopAddress != "Memuat alamat...") {
                                        val geoResults = com.lavana.dapoer.data.GeocodingHelper.searchAddress(shopAddress)
                                        if (geoResults.isNotEmpty()) {
                                            cafeLat = geoResults[0].latitude.toString()
                                            cafeLon = geoResults[0].longitude.toString()
                                        }
                                    }
                                    
                                    val baseVal = baseFee.toDoubleOrNull() ?: 5000.0
                                    val per50mVal = per50mFee.toDoubleOrNull() ?: 200.0
                                    val latVal = cafeLat.toDoubleOrNull() ?: -6.2297
                                    val lonVal = cafeLon.toDoubleOrNull() ?: 106.8296

                                    // Upsert (bukan update) -- baris cafe_latitude/cafe_longitude
                                    // ternyata belum pernah ada di database produksi, sehingga
                                    // update()+filter sebelumnya selalu no-op tanpa error (0 baris
                                    // cocok) dan lokasi toko tidak pernah benar-benar tersimpan.
                                    SupabaseClient.db["settings"].upsert(
                                        SettingsRow("delivery_base_fee", baseVal, "Tarif dasar pengiriman (jarak minimum)"),
                                        onConflict = "key"
                                    )
                                    SupabaseClient.db["settings"].upsert(
                                        SettingsRow("delivery_per_50m_fee", per50mVal, "Tarif tambahan per 50 meter setelah jarak dasar terlewati"),
                                        onConflict = "key"
                                    )
                                    SupabaseClient.db["settings"].upsert(
                                        SettingsRow("cafe_latitude", latVal, "Latitude lokasi Dapoer Lavana Cafe"),
                                        onConflict = "key"
                                    )
                                    SupabaseClient.db["settings"].upsert(
                                        SettingsRow("cafe_longitude", lonVal, "Longitude lokasi Dapoer Lavana Cafe"),
                                        onConflict = "key"
                                    )
                                    
                                    val bannerList = SupabaseClient.db["banners"].select().decodeList<BannerItem>()
                                    val oldBankBanner = bannerList.firstOrNull { it.title?.startsWith("payment_bank|") == true }
                                    val newTitle = "payment_bank|$bankName|$bankNo|$bankOwner"
                                    if (oldBankBanner != null) {
                                        SupabaseClient.db["banners"].update({
                                            set("title", newTitle)
                                        }) { filter { eq("id", oldBankBanner.id ?: "") } }
                                    } else {
                                        SupabaseClient.db["banners"].insert(BannerItem(
                                            id = UUID.randomUUID().toString(),
                                            title = newTitle,
                                            imageUrl = "https://images.unsplash.com/photo-1559526324-4b87b5e36e44?w=500"
                                        ))
                                    }
                                    
                                    val oldQrisBanner = bannerList.firstOrNull { it.title == "payment_qris" }
                                    if (oldQrisBanner != null) {
                                        SupabaseClient.db["banners"].update({
                                            set("image_url", qrisUrl)
                                        }) { filter { eq("id", oldQrisBanner.id ?: "") } }
                                    } else {
                                        SupabaseClient.db["banners"].insert(BannerItem(
                                            id = UUID.randomUUID().toString(),
                                            title = "payment_qris",
                                            imageUrl = qrisUrl
                                        ))
                                    }
                                    
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    coroutineScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                        android.widget.Toast.makeText(context, "Gagal menyimpan pengaturan: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                } finally {
                                    isSavingSettings = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = !isSavingSettings
                    ) {
                        Text(if (isSavingSettings) "Menyimpan..." else "Simpan Pengaturan", color = OnAccentDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        // Form Pendaftaran Akun Karyawan / Driver
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Daftarkan Akun Driver / Admin Baru", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ForestGreen)
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = regName,
                    onValueChange = { regName = it },
                    label = { Text("Nama Lengkap") },
                    placeholder = { Text("Masukkan nama lengkap...") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = regUsername,
                    onValueChange = { regUsername = it },
                    label = { Text("Username") },
                    placeholder = { Text("Masukkan username untuk login...") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = regPassword,
                    onValueChange = { regPassword = it },
                    label = { Text("Password") },
                    placeholder = { Text("Masukkan password...") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = regContact,
                    onValueChange = { regContact = it },
                    label = { Text("Nomor Kontak (WhatsApp)") },
                    placeholder = { Text("Contoh: 08123456789") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Text("Pilih Peran / Role Akun:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val roles = listOf("Driver", "Admin")
                    roles.forEach { role ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { regRole = role }
                        ) {
                            RadioButton(
                                selected = regRole == role,
                                onClick = { regRole = role },
                                colors = RadioButtonDefaults.colors(selectedColor = OrangeJco)
                            )
                            Text(role, fontSize = 13.sp, color = DarkCharcoal)
                        }
                    }
                }
                
                if (regErrorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(regErrorMessage ?: "", color = Color.Red, fontSize = 12.sp)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (regName.isBlank() || regUsername.isBlank() || regPassword.isBlank() || regContact.isBlank()) {
                            regErrorMessage = "Semua bidang wajib diisi!"
                            return@Button
                        }
                        regErrorMessage = null
                        isRegisteringStaff = true
                        coroutineScope.launch {
                            try {
                                val newStaff = StaffAccount(
                                    username = regUsername,
                                    password = regPassword,
                                    role = regRole,
                                    name = regName,
                                    contactNumber = regContact
                                )
                                SupabaseClient.db["staff_accounts"].insert(newStaff)
                                regName = ""
                                regUsername = ""
                                regPassword = ""
                                regContact = ""
                                regRole = "Driver"
                                loadStaff()
                            } catch (e: Exception) {
                                regErrorMessage = "Gagal mendaftarkan akun. Username mungkin sudah terpakai."
                            } finally {
                                isRegisteringStaff = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = !isRegisteringStaff
                ) {
                    Text(if (isRegisteringStaff) "Mendaftarkan..." else "Daftarkan Akun Karyawan", color = OnAccentDark, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // List Karyawan Terdaftar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Daftar Karyawan Terdaftar", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkCharcoal)
                Spacer(modifier = Modifier.height(12.dp))
                
                if (isStaffLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = OrangeJco)
                    }
                } else if (staffList.isEmpty()) {
                    Text("Belum ada karyawan terdaftar.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 12.dp))
                } else {
                    staffList.forEach { staff ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(staff.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkCharcoal)
                                Text("Username: ${staff.username} | Peran: ${staff.role}", fontSize = 11.sp, color = Color.Gray)
                                if (!staff.contactNumber.isNullOrBlank()) {
                                    Text("Telp/WA: ${staff.contactNumber}", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        try {
                                            SupabaseClient.db["staff_accounts"].delete {
                                                filter { eq("id", staff.id ?: "") }
                                            }
                                            loadStaff()
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.Red)
                            }
                        }
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Pengaturan Printer Thermal
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPrinterSettings = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Print, contentDescription = "Pengaturan Printer", tint = OrangeJco)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Pengaturan Printer", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkCharcoal)
                        Text("Pilih printer Bluetooth thermal", fontSize = 11.sp, color = Color.Gray)
                    }
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = RedPromo),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Keluar dari Aplikasi", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }

    if (showPrinterSettings) {
        PrinterSettingsDialog(onDismiss = { showPrinterSettings = false })
    }
}

@Composable
fun ManageCategoriesDialog(
    onDismiss: () -> Unit,
    onCategoriesUpdated: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    var selectedCategoryToRename by remember { mutableStateOf<String?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var newCategoryInput by remember { mutableStateOf("") }
    var categoryToDelete by remember { mutableStateOf<Pair<String, Int>?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Text("Kelola Kategori Produk", fontWeight = FontWeight.Bold, color = DarkCharcoal)
        },
        text = {
            if (isSaving) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = OrangeJco)
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Tambahkan kategori baru atau ubah nama kategori yang ada untuk memperbarui produk terkait.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Form Tambah Kategori Baru
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newCategoryInput,
                            onValueChange = { newCategoryInput = it },
                            placeholder = { Text("Nama kategori baru...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val trimmed = newCategoryInput.trim()
                                if (trimmed.isEmpty() || trimmed.contains('|')) {
                                    android.widget.Toast.makeText(context, "Nama kategori tidak boleh kosong atau mengandung karakter |", android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    val exists = DYNAMIC_CATEGORIES.any { it.equals(trimmed, ignoreCase = true) }
                                    if (!exists) {
                                        isSaving = true
                                        coroutineScope.launch {
                                            try {
                                                // WAJIB set id eksplisit: kolom banners.id UUID NOT NULL,
                                                // jika id null dikirim, Postgres menolak (default tidak dipakai).
                                                SupabaseClient.db["banners"].insert(BannerItem(
                                                    id = UUID.randomUUID().toString(),
                                                    title = "category|$trimmed",
                                                    imageUrl = ""
                                                ))
                                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                    if (!CUSTOM_CATEGORIES.contains(trimmed)) CUSTOM_CATEGORIES.add(trimmed)
                                                    if (!DYNAMIC_CATEGORIES.contains(trimmed)) DYNAMIC_CATEGORIES.add(trimmed)
                                                }
                                                newCategoryInput = ""
                                                onCategoriesUpdated()
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                android.widget.Toast.makeText(context, "Gagal menambah kategori: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                                            } finally {
                                                isSaving = false
                                            }
                                        }
                                    } else {
                                        android.widget.Toast.makeText(context, "Kategori sudah ada!", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Tambah", color = OnAccentDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Box(modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(DYNAMIC_CATEGORIES) { cat ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = LightGrayJco),
                                    border = BorderStroke(1.dp, LightOrangeJco)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(cat, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = DarkCharcoal)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    selectedCategoryToRename = cat
                                                    renameInput = cat
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Rename",
                                                    tint = OrangeJco,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    isSaving = true
                                                    coroutineScope.launch {
                                                        try {
                                                            val count = SupabaseClient.db["menu_items"].select {
                                                                filter { eq("category", cat) }
                                                            }.decodeList<MenuItem>().size
                                                            
                                                            if (count > 0) {
                                                                 // Kategori masih berisi produk — tawarkan pindahkan produk lalu hapus.
                                                                 kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                                     categoryToDelete = cat to count
                                                                 }
                                                            } else {
                                                                 // Kategori kosong (0 produk) — hapus banner (jika ada) lalu
                                                                 // buang dari daftar secara optimistis. Kategori bawaan tak punya
                                                                 // banner, jadi penghapusan dari DYNAMIC_CATEGORIES + reload yang
                                                                 // membuatnya hilang.
                                                                 SupabaseClient.db["banners"].delete {
                                                                     filter { eq("title", "category|$cat") }
                                                                 }
                                                                 kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                                     CUSTOM_CATEGORIES.remove(cat)
                                                                     DYNAMIC_CATEGORIES.remove(cat)
                                                                 }
                                                                 onCategoriesUpdated()
                                                            }
                                                        } catch (e: Exception) {
                                                             e.printStackTrace()
                                                             android.widget.Toast.makeText(context, "Gagal menghapus kategori: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                                                        } finally {
                                                             isSaving = false
                                                        }
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Hapus",
                                                    tint = Color.Red,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Tutup", color = OrangeJco)
            }
        }
    )

    if (selectedCategoryToRename != null) {
        AlertDialog(
            onDismissRequest = { selectedCategoryToRename = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White,
            title = { Text("Ubah Nama Kategori", fontWeight = FontWeight.Bold, color = DarkCharcoal) },
            text = {
                Column {
                    Text("Mengubah nama kategori \"$selectedCategoryToRename\" menjadi:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        singleLine = true,
                        label = { Text("Nama Baru Kategori") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val oldName = selectedCategoryToRename!!
                        val newName = renameInput.trim()
                        if (newName.isEmpty() || newName.contains('|')) {
                            android.widget.Toast.makeText(context, "Nama kategori tidak boleh kosong atau mengandung karakter |", android.widget.Toast.LENGTH_LONG).show()
                        } else if (!newName.equals(oldName, ignoreCase = true) && DYNAMIC_CATEGORIES.any { it.equals(newName, ignoreCase = true) }) {
                            // Cegah rename ke nama yang sudah ada — kalau diteruskan, semua produk
                            // kategori lama akan tergabung diam-diam ke kategori tujuan (tak bisa dibatalkan).
                            android.widget.Toast.makeText(context, "Kategori sudah ada!", android.widget.Toast.LENGTH_SHORT).show()
                        } else if (oldName != newName) {
                            isSaving = true
                            selectedCategoryToRename = null
                            coroutineScope.launch {
                                try {
                                    SupabaseClient.db["menu_items"].update({
                                        set("category", newName)
                                    }) {
                                        filter { eq("category", oldName) }
                                    }
                                    try {
                                        val oldBanner = SupabaseClient.db["banners"].select {
                                            filter { eq("title", "category|$oldName") }
                                        }.decodeList<BannerItem>().firstOrNull()
                                        if (oldBanner != null) {
                                            SupabaseClient.db["banners"].update({
                                                set("title", "category|$newName")
                                            }) { filter { eq("id", oldBanner.id ?: "") } }
                                        }
                                    } catch (ex: Exception) {
                                        ex.printStackTrace()
                                    }
                                    // Optimistic in-memory update on Main thread so rename persists in UI
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        val idx = DYNAMIC_CATEGORIES.indexOf(oldName)
                                        if (idx != -1) {
                                            DYNAMIC_CATEGORIES[idx] = newName
                                        }
                                        val customIdx = CUSTOM_CATEGORIES.indexOf(oldName)
                                        if (customIdx != -1) {
                                            CUSTOM_CATEGORIES[customIdx] = newName
                                        }
                                    }
                                    onCategoriesUpdated()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    android.widget.Toast.makeText(context, "Gagal mengubah nama kategori: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                                } finally {
                                    isSaving = false
                                }
                            }
                        } else {
                            selectedCategoryToRename = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Simpan", color = OnAccentDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedCategoryToRename = null }) {
                    Text("Batal", color = Color.Gray)
                }
            }
        )
    }

    // Konfirmasi hapus kategori yang masih berisi produk — pindahkan dulu ke kategori tujuan.
    if (categoryToDelete != null) {
        val delCat = categoryToDelete!!.first
        val delCount = categoryToDelete!!.second
        val targets = DYNAMIC_CATEGORIES.filter { it != delCat }
        AlertDialog(
            onDismissRequest = { if (!isSaving) categoryToDelete = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White,
            title = { Text("Hapus Kategori \"$delCat\"?", fontWeight = FontWeight.Bold, color = DarkCharcoal) },
            text = {
                val productNames = remember(delCat) { mutableStateOf<List<String>>(emptyList()) }
                LaunchedEffect(delCat) {
                    try {
                        productNames.value = SupabaseClient.db["menu_items"].select {
                            filter { eq("category", delCat) }
                        }.decodeList<MenuItem>().map { it.name }
                    } catch (e: Exception) { e.printStackTrace() }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Kategori ini masih punya $delCount produk:",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    if (productNames.value.isNotEmpty()) {
                        Text(
                            productNames.value.joinToString(", "),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkCharcoal
                        )
                    }
                    Text("Pilih tindakan:", fontSize = 12.sp, color = Color.Gray)

                    // Opsi merah: hapus kategori beserta seluruh produknya (selalu tersedia).
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isSaving) {
                                isSaving = true
                                coroutineScope.launch {
                                    try {
                                        SupabaseClient.db["menu_items"].delete { filter { eq("category", delCat) } }
                                        SupabaseClient.db["banners"].delete { filter { eq("title", "category|$delCat") } }
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            CUSTOM_CATEGORIES.remove(delCat)
                                            DYNAMIC_CATEGORIES.remove(delCat)
                                            categoryToDelete = null
                                        }
                                        onCategoriesUpdated()
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            android.widget.Toast.makeText(context, "Kategori \"$delCat\" beserta $delCount produk dihapus.", android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        android.widget.Toast.makeText(context, "Gagal menghapus: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                                    } finally {
                                        isSaving = false
                                    }
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = RedPromo.copy(alpha = 0.08f)),
                        border = BorderStroke(1.dp, RedPromo.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = RedPromo, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Hapus kategori & semua produknya", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = RedPromo)
                        }
                    }

                    if (targets.isNotEmpty()) {
                        Text("Atau pindahkan produk ke kategori lain lalu hapus:", fontSize = 12.sp, color = Color.Gray)
                        targets.forEach { target ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isSaving) {
                                        isSaving = true
                                        coroutineScope.launch {
                                            try {
                                                SupabaseClient.db["menu_items"].update({
                                                    set("category", target)
                                                }) { filter { eq("category", delCat) } }
                                                SupabaseClient.db["banners"].delete {
                                                    filter { eq("title", "category|$delCat") }
                                                }
                                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                    CUSTOM_CATEGORIES.remove(delCat)
                                                    DYNAMIC_CATEGORIES.remove(delCat)
                                                    categoryToDelete = null
                                                }
                                                onCategoriesUpdated()
                                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                    android.widget.Toast.makeText(context, "$delCount produk dipindah ke \"$target\". Kategori \"$delCat\" dihapus.", android.widget.Toast.LENGTH_LONG).show()
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                android.widget.Toast.makeText(context, "Gagal menghapus kategori: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                                            } finally {
                                                isSaving = false
                                            }
                                        }
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = LightGrayJco),
                                border = BorderStroke(1.dp, LightOrangeJco)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = OrangeJco, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Pindahkan ke \"$target\"", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = DarkCharcoal)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { if (!isSaving) categoryToDelete = null }) {
                    Text("Batal", color = Color.Gray)
                }
            }
        )
    }
}

// ==========================================================
// THERMAL RECEIPT BUILDER (58mm / 32 char width)
// ==========================================================
private const val RECEIPT_WIDTH = 32

private fun receiptLineLR(left: String, right: String): String {
    val space = RECEIPT_WIDTH - left.length - right.length
    return if (space > 0) {
        left + " ".repeat(space) + right
    } else {
        // Tidak muat dalam satu baris, pisah baris
        left + "\n" + right.padStart(RECEIPT_WIDTH)
    }
}

/**
 * Struk RESI (untuk pelanggan): identitas kafe + pemesan rata kiri di atas,
 * rincian pesanan & total di tengah, lalu disclaimer + logo + ucapan terima
 * kasih rata tengah di bawah. Dicetak via PrinterManager.printReceipt().
 */
fun buildOrderReceiptElements(
    context: android.content.Context,
    order: Order?,
    items: List<OrderItem>,
    menuMap: Map<String, MenuItem>
): List<ReceiptElement> {
    if (order == null) return emptyList()
    val elements = mutableListOf<ReceiptElement>()
    val staff = CartManager.currentStaff

    // ATAS: rata kiri
    elements.add(ReceiptElement.TextLine("DAPOER LAVANA", bold = true))
    elements.add(ReceiptElement.TextLine("Warkop & Katering"))
    elements.add(ReceiptElement.TextLine("Tlp/Wa : ${staff?.contactNumber ?: "-"}"))
    elements.add(ReceiptElement.TextLine("Nama (Pemesan) : ${order.customerName ?: "-"}"))
    elements.add(ReceiptElement.TextLine("Kasir : ${staff?.name ?: "-"}"))
    elements.add(ReceiptElement.Divider)

    elements.add(ReceiptElement.TextLine("No. Order : #${order.orderNumber}", bold = true))
    elements.add(ReceiptElement.TextLine("Tanggal   : ${order.createdAt?.take(16)?.replace("T", " ") ?: ""}"))
    elements.add(ReceiptElement.TextLine("Tipe      : ${order.orderType}"))
    if (!order.customerPhone.isNullOrBlank()) {
        elements.add(ReceiptElement.TextLine("Telp      : ${order.customerPhone}"))
    }
    elements.add(ReceiptElement.Divider)
    items.forEach { item ->
        val menuName = menuMap[item.menuItemId]?.name ?: "Item Menu"
        val priceStr = "Rp ${String.format("%,.0f", item.priceAtOrder * item.quantity)}"
        elements.add(ReceiptElement.TextLine(receiptLineLR("$menuName x${item.quantity}", priceStr)))
        if (!item.notes.isNullOrBlank()) {
            elements.add(ReceiptElement.TextLine("  * Catatan: ${item.notes}"))
        }
    }
    elements.add(ReceiptElement.Divider)
    elements.add(ReceiptElement.TextLine(receiptLineLR("Subtotal", "Rp ${String.format("%,.0f", order.subtotal)}")))
    if (order.deliveryFee > 0.0) {
        elements.add(ReceiptElement.TextLine(receiptLineLR("Ongkir", "Rp ${String.format("%,.0f", order.deliveryFee)}")))
    }
    elements.add(ReceiptElement.TextLine(receiptLineLR("TOTAL", "Rp ${String.format("%,.0f", order.total)}"), bold = true))
    elements.add(ReceiptElement.Divider)
    elements.add(ReceiptElement.TextLine("Metode: ${order.paymentMethod} (${order.paymentStatus})"))
    elements.add(ReceiptElement.Spacer)

    // BAWAH: rata tengah
    elements.add(ReceiptElement.TextLine("Bill ini bukan bukti Pembayaran", align = ReceiptAlign.CENTER))
    try {
        val logoBitmap = android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.logo_lavana)
        if (logoBitmap != null) {
            elements.add(ReceiptElement.Logo(logoBitmap))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    elements.add(ReceiptElement.TextLine("Warkop . Rasa . Cerita", align = ReceiptAlign.CENTER))
    elements.add(ReceiptElement.TextLine("Terima Kasih Atas Kunjungan Anda", align = ReceiptAlign.CENTER))

    return elements
}

/**
 * Struk DAPUR (untuk dapur): hanya daftar item + catatan yang perlu disiapkan,
 * tanpa harga/logo/metode pembayaran -- fokus ke apa yang harus dimasak.
 */
fun buildKitchenReceiptElements(
    order: Order?,
    items: List<OrderItem>,
    menuMap: Map<String, MenuItem>
): List<ReceiptElement> {
    if (order == null) return emptyList()
    val elements = mutableListOf<ReceiptElement>()

    elements.add(ReceiptElement.TextLine("STRUK DAPUR", bold = true, align = ReceiptAlign.CENTER, doubleHeight = true))
    elements.add(ReceiptElement.Divider)
    elements.add(ReceiptElement.TextLine("No. Order : #${order.orderNumber}", bold = true))
    elements.add(ReceiptElement.TextLine("Waktu     : ${order.createdAt?.take(16)?.replace("T", " ") ?: ""}"))
    elements.add(ReceiptElement.TextLine("Tipe      : ${order.orderType}"))
    if (order.orderType == "Dine In" && !order.tableNumber.isNullOrBlank()) {
        elements.add(ReceiptElement.TextLine("No. Meja  : ${order.tableNumber}"))
    }
    if (!order.customerName.isNullOrBlank()) {
        elements.add(ReceiptElement.TextLine("Pemesan   : ${order.customerName}"))
    }
    elements.add(ReceiptElement.Divider)
    items.forEach { item ->
        val menuName = menuMap[item.menuItemId]?.name ?: "Item Menu"
        elements.add(ReceiptElement.TextLine("${item.quantity}x $menuName", bold = true, doubleHeight = true))
        if (!item.notes.isNullOrBlank()) {
            elements.add(ReceiptElement.TextLine("  * Catatan: ${item.notes}"))
        }
    }
    elements.add(ReceiptElement.Divider)

    return elements
}

// ==========================================================
// FILE UTILITIES: SAVE REPORT TO DOWNLOADS
// ==========================================================
fun saveReportToDownloads(context: android.content.Context, filename: String, content: String): Boolean {
    return try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(content.toByteArray())
                }
                true
            } else {
                false
            }
        } else {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            val file = java.io.File(downloadsDir, filename)
            file.writeText(content)
            true
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
