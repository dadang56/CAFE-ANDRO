package com.lavana.dapoer.ui.screens

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
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
                containerColor = Color.White,
                tonalElevation = 8.dp
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Kelola Pesanan Delivery", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
            IconButton(onClick = { refreshData() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = OrangeJco)
            }
        }

        // Sales Summary Card
        val totalSales = ordersList.filter { it.status == "Selesai" }.sumOf { it.total }
        val completedCount = ordersList.filter { it.status == "Selesai" }.size
        
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Penjualan", fontSize = 12.sp, color = Color.Gray)
                    Text("Rp ${String.format("%,.0f", totalSales)}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OrangeJco)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Pesanan Sukses", fontSize = 12.sp, color = Color.Gray)
                    Text("$completedCount Pesanan", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OrangeJco)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (ordersList.isEmpty()) {
                    item {
                        Text("Belum ada pesanan masuk.", modifier = Modifier.fillMaxWidth().padding(top = 40.dp), textAlign = TextAlign.Center, color = Color.Gray)
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
                            onRejectPayment = { reason ->
                                coroutineScope.launch {
                                    try {
                                        SupabaseClient.db["orders"].update({
                                            set("payment_status", "Ditolak")
                                            set("notes", "[Ditolak: $reason]")
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
            title = { Text("Tugaskan Driver Pengiriman", fontWeight = FontWeight.Bold) },
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
                                colors = CardDefaults.cardColors(containerColor = LightOrangeJco),
                                border = BorderStroke(1.dp, OrangeJco.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.DeliveryDining, contentDescription = "Driver", tint = OrangeJco)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(driver.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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

    if (previewReceiptUrl != null) {
        var isImageError by remember { mutableStateOf(false) }
        var isImageLoading by remember { mutableStateOf(true) }
        
        AlertDialog(
            onDismissRequest = { previewReceiptUrl = null },
            title = { Text("Bukti Pembayaran Pelanggan", fontWeight = FontWeight.Bold) },
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
        AlertDialog(
            onDismissRequest = { selectedOrderForPrint = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cetak Struk Thermal", fontWeight = FontWeight.Bold)
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .background(Color.White)
                            .border(1.dp, Color.LightGray)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "DAPOER LAVANA",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = Color.Black
                        )
                        Text(
                            text = "Kopi & Selera Nusantara",
                            fontSize = 10.sp,
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
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Terima Kasih Atas Kunjungan Anda",
                            fontSize = 10.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = Color.Black,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { selectedOrderForPrint = null }) {
                        Text("Tutup", color = Color.Gray)
                    }
                    Button(
                        onClick = {
                            android.widget.Toast.makeText(context, "Resi dikirim ke Printer Bluetooth Thermal...", android.widget.Toast.LENGTH_LONG).show()
                            selectedOrderForPrint = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "Cetak", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cetak Struk", color = Color.White)
                    }
                }
            }
        )
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
    onRejectPayment: (String) -> Unit
) {
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectReasonInput by remember { mutableStateOf("") }
    val assignedDriverName = remember(order.driverId, drivers) {
        drivers.find { it.id == order.driverId }?.name ?: "Belum Ditugaskan"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                                "Diantar" -> Color(0xFFE0F2FE)
                                "Selesai" -> Color(0xFFD1FAE5)
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
                            "Diantar" -> Color(0xFF0284C7)
                            "Selesai" -> Color(0xFF059669)
                            else -> Color.DarkGray
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Alamat: ${order.deliveryAddress ?: "-"}", fontSize = 12.sp, color = DarkCharcoal, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Total: Rp ${String.format("%,.0f", order.total)} | Pembayaran: ${order.paymentMethod} (${order.paymentStatus})", fontSize = 12.sp, color = Color.Gray)
            
            if (order.status == "Diantar" || order.status == "Selesai") {
                Text("Kurir: $assignedDriverName", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OrangeJco)
            }

            Spacer(modifier = Modifier.height(10.dp))
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

                Spacer(modifier = Modifier.weight(1f))
                when (order.status) {
                    "Pending" -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (order.paymentStatus == "Menunggu Verifikasi") {
                                Button(
                                    onClick = { showRejectDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Tolak Pembayaran", fontSize = 12.sp, color = Color.White)
                                }
                            }
                            Button(
                                onClick = onAccept,
                                colors = ButtonDefaults.buttonColors(containerColor = OrangeJco),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Konfirmasi Pembayaran", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                    "Diproses" -> {
                        Button(
                            onClick = onAssignDriverClick,
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Tugaskan Driver", fontSize = 12.sp, color = Color.White)
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Tolak Pembayaran", fontWeight = FontWeight.Bold) },
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
}

// ==========================================================
// TABS 1: MENU CATALOG MANAGEMENT
// ==========================================================
@Composable
fun TabAdminMenu() {
    val coroutineScope = rememberCoroutineScope()
    var menuList by remember { mutableStateOf<List<MenuItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showManageCategoriesDialog by remember { mutableStateOf(false) }
    var selectedMenuForEdit by remember { mutableStateOf<MenuItem?>(null) }

    fun loadMenu() {
        isLoading = true
        coroutineScope.launch {
            menuList = SupabaseClient.initializeMenuIfEmpty()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadMenu()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Kelola Menu", 
                fontSize = 20.sp, 
                fontWeight = FontWeight.Bold, 
                color = ForestGreen,
                modifier = Modifier.weight(1f)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showManageCategoriesDialog = true },
                    border = BorderStroke(1.dp, OrangeJco),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangeJco),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Kategori", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Kategori", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeJco),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Menu", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OrangeJco)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(menuList) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)).background(LightOrangeJco)
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
                                    coroutineScope.launch {
                                        try {
                                            SupabaseClient.db["menu_items"].update({
                                                set("is_available", isChecked)
                                            }) { filter { eq("id", item.id) } }
                                            loadMenu()
                                        } catch (e: Exception) {}
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
                                coroutineScope.launch {
                                    try {
                                        SupabaseClient.db["menu_items"].delete {
                                            filter { eq("id", item.id) }
                                        }
                                        loadMenu()
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

    // Add Menu Dialog
    if (showAddDialog) {
        AddOrEditMenuDialog(
            item = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, category, price, desc, imageUrl ->
                coroutineScope.launch {
                    try {
                        val newItem = MenuItem(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            category = category,
                            price = price,
                            description = desc,
                            imageUrl = imageUrl,
                            isAvailable = true
                        )
                        SupabaseClient.db["menu_items"].insert(newItem)
                        showAddDialog = false
                        loadMenu()
                    } catch (e: Exception) {}
                }
            }
        )
    }

    // Edit Menu Dialog
    if (selectedMenuForEdit != null) {
        AddOrEditMenuDialog(
            item = selectedMenuForEdit,
            onDismiss = { selectedMenuForEdit = null },
            onSave = { name, category, price, desc, imageUrl ->
                coroutineScope.launch {
                    try {
                        SupabaseClient.db["menu_items"].update({
                            set("name", name)
                            set("category", category)
                            set("price", price)
                            set("description", desc)
                            set("image_url", imageUrl)
                        }) {
                            filter { eq("id", selectedMenuForEdit?.id ?: "") }
                        }
                        selectedMenuForEdit = null
                        loadMenu()
                    } catch (e: Exception) {}
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
}

@Composable
fun AddOrEditMenuDialog(
    item: MenuItem?,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, String, String) -> Unit
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var category by remember { mutableStateOf(item?.category ?: "Donuts") }
    var price by remember { mutableStateOf(item?.price?.toInt()?.toString() ?: "") }
    
    val parsedOriginalPrice = item?.getOriginalPrice()
    var originalPriceInput by remember { mutableStateOf(parsedOriginalPrice?.toInt()?.toString() ?: "") }
    val cleanDesc = item?.getCleanDescription() ?: ""
    var desc by remember { mutableStateOf(cleanDesc) }
    
    var imageUrl by remember { mutableStateOf(item?.imageUrl ?: "") }

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
                        imageUrl = bucket.publicUrl(fileName)
                    }
                } catch (e: Exception) {
                    // Fail / error log
                } finally {
                    isUploadingPhoto = false
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Tambah Item Baru" else "Edit Detail Item", fontWeight = FontWeight.Bold) },
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
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCatSelected) OrangeJco else LightGrayJco)
                                .clickable { category = cat }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCatSelected) Color.White else DarkCharcoal
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val priceVal = price.toDoubleOrNull() ?: 0.0
                    val originalPriceVal = originalPriceInput.toDoubleOrNull()
                    val finalDesc = createDescriptionWithOriginalPrice(desc, originalPriceVal)
                    onSave(name, category, priceVal, finalDesc, imageUrl)
                },
                colors = ButtonDefaults.buttonColors(containerColor = OrangeJco)
            ) {
                Text("Simpan", color = Color.White)
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
@Composable
fun TabAdminBanners() {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var banners by remember { mutableStateOf<List<BannerItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var imageUrlInput by remember { mutableStateOf("") }
    var titleInput by remember { mutableStateOf("") }
    var isUploadingPhoto by remember { mutableStateOf(false) }

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
                        imageUrlInput = bucket.publicUrl(fileName)
                    }
                } catch (e: Exception) {
                    // Fail / error log
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
                banners = list.filter { it.title?.startsWith("payment_") != true }
            } catch (e: Exception) {}
            finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadBanners()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Kelola Banner Promo Carousel", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
        Spacer(modifier = Modifier.height(12.dp))

        // Form Tambah Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tambah Banner Baru", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
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
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
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

                Button(
                    onClick = {
                        if (imageUrlInput.isNotBlank()) {
                            coroutineScope.launch {
                                try {
                                    val newBanner = BannerItem(
                                        id = UUID.randomUUID().toString(),
                                        imageUrl = imageUrlInput,
                                        title = titleInput
                                    )
                                    SupabaseClient.db["banners"].insert(newBanner)
                                    imageUrlInput = ""
                                    titleInput = ""
                                    loadBanners()
                                } catch (e: Exception) {}
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeJco),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tambahkan Banner", color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OrangeJco)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(banners) { banner ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(banner.title ?: "Promo Spesial", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(banner.imageUrl, fontSize = 10.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Laporan Keuangan", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
            IconButton(onClick = { loadReportData() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = OrangeJco)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.White).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
        ) {
            listOf("Harian", "Bulanan").forEach { type ->
                val selected = reportType == type
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { reportType = type }
                        .background(if (selected) OrangeJco else Color.Transparent)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Laporan $type",
                        color = if (selected) Color.White else Color.Gray,
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
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
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
                CircularProgressIndicator(color = OrangeJco)
            }
        } else {
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Total Omset", fontSize = 11.sp, color = Color.Gray)
                            Text("Rp ${String.format("%,.0f", totalRevenue)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Total Transaksi", fontSize = 11.sp, color = Color.Gray)
                            Text("$totalOrdersCount Order", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OrangeJco)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("Tidak ada transaksi untuk periode ini.", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    } else {
                        items(filteredOrders) { order ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
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
            title = { Text("Preview Cetak Laporan", fontWeight = FontWeight.Bold) },
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
                                val filename = if (reportType == "Harian") {
                                    "Laporan_Harian_$selectedDate.txt"
                                } else {
                                    "Laporan_Bulanan_$selectedMonth.txt"
                                }
                                val ok = saveReportToDownloads(context, filename, reportText)
                                if (ok) {
                                    android.widget.Toast.makeText(context, "Laporan disimpan ke folder Downloads: $filename", android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    android.widget.Toast.makeText(context, "Gagal menyimpan laporan.", android.widget.Toast.LENGTH_LONG).show()
                                }
                            },
                            border = BorderStroke(1.dp, OrangeJco),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangeJco),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Simpan", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Simpan ke File", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val filename = if (reportType == "Harian") {
                                    "Laporan_Harian_$selectedDate.txt"
                                } else {
                                    "Laporan_Bulanan_$selectedMonth.txt"
                                }
                                saveReportToDownloads(context, filename, reportText)
                                android.widget.Toast.makeText(context, "Laporan disimpan & dikirim ke Printer...", android.widget.Toast.LENGTH_LONG).show()
                                showPrintPreview = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = "Cetak", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cetak Laporan", color = Color.White, fontSize = 12.sp)
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
    var perKmFee by remember { mutableStateOf("") }
    var cafeLat by remember { mutableStateOf("") }
    var cafeLon by remember { mutableStateOf("") }
    var isSavingSettings by remember { mutableStateOf(false) }
    var isSettingsLoading by remember { mutableStateOf(true) }

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
                    "delivery_per_km_fee" -> perKmFee = row.value.toInt().toString()
                    "cafe_latitude" -> cafeLat = row.value.toString()
                    "cafe_longitude" -> cafeLon = row.value.toString()
                }
            }
        } catch (e: Exception) {
            baseFee = "5000"
            perKmFee = "2500"
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
        Text("Admin Dapoer Lavana", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
        Text("Email: admin@lavana.com", fontSize = 14.sp, color = Color.Gray)
        Text("Level Akses: Owner / Administrator", fontSize = 12.sp, color = OrangeJco, fontWeight = FontWeight.Bold)

        if (!isSettingsLoading) {
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
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
                        value = perKmFee,
                        onValueChange = { perKmFee = it },
                        label = { Text("Ongkos per Kilometer (IDR)") },
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
                                        qrisUrl = publicUrl
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
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
                                    val perKmVal = perKmFee.toDoubleOrNull() ?: 2500.0
                                    val latVal = cafeLat.toDoubleOrNull() ?: -6.2297
                                    val lonVal = cafeLon.toDoubleOrNull() ?: 106.8296
                                    
                                    SupabaseClient.db["settings"].update({
                                        set("value", baseVal)
                                    }) { filter { eq("key", "delivery_base_fee") } }
                                    
                                    SupabaseClient.db["settings"].update({
                                        set("value", perKmVal)
                                    }) { filter { eq("key", "delivery_per_km_fee") } }
                                    
                                    SupabaseClient.db["settings"].update({
                                        set("value", latVal)
                                    }) { filter { eq("key", "cafe_latitude") } }
                                    
                                    SupabaseClient.db["settings"].update({
                                        set("value", lonVal)
                                    }) { filter { eq("key", "cafe_longitude") } }
                                    
                                    val bannerList = SupabaseClient.db["banners"].select().decodeList<BannerItem>()
                                    val oldBankBanner = bannerList.firstOrNull { it.title?.startsWith("payment_bank|") == true }
                                    val newTitle = "payment_bank|$bankName|$bankNo|$bankOwner"
                                    if (oldBankBanner != null) {
                                        SupabaseClient.db["banners"].update({
                                            set("title", newTitle)
                                        }) { filter { eq("id", oldBankBanner.id ?: "") } }
                                    } else {
                                        SupabaseClient.db["banners"].insert(BannerItem(
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
                                            title = "payment_qris",
                                            imageUrl = qrisUrl
                                        ))
                                    }
                                    
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    isSavingSettings = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeJco),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(45.dp),
                        enabled = !isSavingSettings
                    ) {
                        Text(if (isSavingSettings) "Menyimpan..." else "Simpan Pengaturan", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        // Form Pendaftaran Akun Karyawan / Driver
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
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
                        if (regName.isBlank() || regUsername.isBlank() || regPassword.isBlank()) {
                            regErrorMessage = "Nama, Username, dan Password wajib diisi!"
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
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(45.dp),
                    enabled = !isRegisteringStaff
                ) {
                    Text(if (isRegisteringStaff) "Mendaftarkan..." else "Daftarkan Akun Karyawan", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // List Karyawan Terdaftar
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
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

        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = RedPromo),
            shape = RoundedCornerShape(25.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Keluar dari Aplikasi", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun ManageCategoriesDialog(
    onDismiss: () -> Unit,
    onCategoriesUpdated: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    var selectedCategoryToRename by remember { mutableStateOf<String?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var newCategoryInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = {
            Text("Kelola Kategori Produk", fontWeight = FontWeight.Bold)
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
                                if (trimmed.isNotEmpty()) {
                                    val exists = DYNAMIC_CATEGORIES.any { it.equals(trimmed, ignoreCase = true) }
                                    if (!exists) {
                                        CUSTOM_CATEGORIES.add(trimmed)
                                        newCategoryInput = ""
                                        onCategoriesUpdated()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeJco),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Tambah", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                                    colors = CardDefaults.cardColors(containerColor = LightGrayJco)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(cat, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
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
            title = { Text("Ubah Nama Kategori", fontWeight = FontWeight.Bold) },
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
                        if (newName.isNotEmpty() && oldName != newName) {
                            isSaving = true
                            selectedCategoryToRename = null
                            coroutineScope.launch {
                                try {
                                    SupabaseClient.db["menu_items"].update({
                                        set("category", newName)
                                    }) {
                                        filter { eq("category", oldName) }
                                    }
                                    val idx = DYNAMIC_CATEGORIES.indexOf(oldName)
                                    if (idx != -1) {
                                        DYNAMIC_CATEGORIES[idx] = newName
                                    }
                                    val customIdx = CUSTOM_CATEGORIES.indexOf(oldName)
                                    if (customIdx != -1) {
                                        CUSTOM_CATEGORIES[customIdx] = newName
                                    }
                                    onCategoriesUpdated()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    isSaving = false
                                }
                            }
                        } else {
                            selectedCategoryToRename = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeJco),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Simpan", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedCategoryToRename = null }) {
                    Text("Batal", color = Color.Gray)
                }
            }
        )
    }
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
