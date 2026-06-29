package com.lavana.dapoer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import io.github.jan.supabase.storage.storage
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import android.widget.Toast
import androidx.compose.ui.text.font.FontFamily
import com.lavana.dapoer.data.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.decodeRecord
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.lavana.dapoer.ui.components.GoogleMapView
import com.lavana.dapoer.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(
    orderId: String,
    onNavigateHome: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    
    var order by remember { mutableStateOf<Order?>(null) }
    var orderItems by remember { mutableStateOf<List<OrderItem>>(emptyList()) }
    var menuList by remember { mutableStateOf<List<MenuItem>>(emptyList()) }
    val menuItemsMap = remember { mutableStateMapOf<String, String>() }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showReceiptDialog by remember { mutableStateOf(false) }

    var qrisUrl by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("BCA") }
    var bankNo by remember { mutableStateOf("1234567890") }
    var bankOwner by remember { mutableStateOf("Dapoer Lavana") }

    var selectedPayment by remember { mutableStateOf("QRIS") }
    var uploadedFileName by remember { mutableStateOf<String?>(null) }
    var uploadedFileUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isUploadingPayment by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var showThankYouDialog by remember { mutableStateOf(false) }

    fun loadOrder() {
        coroutineScope.launch {
            try {
                val fetched = SupabaseClient.db["orders"].select {
                    filter {
                        eq("id", orderId)
                    }
                }.decodeSingle<Order>()
                order = fetched

                // Fetch payment settings from banners table
                try {
                    val bannerList = SupabaseClient.db["banners"].select().decodeList<BannerItem>()
                    val qrisBanner = bannerList.firstOrNull { it.title == "payment_qris" }
                    if (qrisBanner != null && qrisBanner.imageUrl.isNotBlank()) {
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
                }

                // Fetch menu list to map names
                menuList = SupabaseClient.initializeMenuIfEmpty()
                menuList.forEach { menuItemsMap[it.id] = it.name }

                // Fetch order items
                try {
                    val items = SupabaseClient.db["order_items"].select {
                        filter {
                            eq("order_id", orderId)
                        }
                    }.decodeList<OrderItem>()
                    orderItems = items
                } catch (e: Exception) {
                    // fallback local items
                    if (orderId == "1") {
                        orderItems = listOf(OrderItem("11", "1", "00000000-0000-0000-0000-000000000001", 2, 28000.0))
                    } else if (orderId == "2") {
                        orderItems = listOf(OrderItem("22", "2", "00000000-0000-0000-0000-000000000002", 1, 25000.0))
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Gagal memuat status: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(orderId) {
        loadOrder()
        
        // Setup listener realtime
        try {
            val channel = SupabaseClient.realtime.channel("order_track_$orderId")
            val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "orders"
                filter = "id=eq.$orderId"
            }
            
            coroutineScope.launch {
                changeFlow.collectLatest { change ->
                    when (change) {
                        is PostgresAction.Update -> {
                            val updated = change.decodeRecord<Order>()
                            if (updated.status != order?.status) {
                                Toast.makeText(context, "Status pesanan berubah menjadi: ${updated.status}", Toast.LENGTH_LONG).show()
                            }
                            order = updated
                        }
                        is PostgresAction.Insert -> {
                            order = change.decodeRecord<Order>()
                        }
                        else -> {}
                    }
                }
            }
            
            SupabaseClient.realtime.connect()
            channel.subscribe()
        } catch (e: Exception) {
            // websocket error fallback
        }
    }

    val activeStep = when (order?.status) {
        "Pending" -> 1
        "Diproses" -> 2
        "Siap" -> 3
        "Diantar" -> 3
        "Selesai" -> 4
        else -> 1
    }
    
    val orderType = order?.orderType ?: "Delivery"
    
    val rejectionReason = remember(order?.notes) {
        val notes = order?.notes ?: ""
        if (notes.contains("[Ditolak: ")) {
            notes.substringAfter("[Ditolak: ").substringBefore("]")
        } else {
            "Alasan tidak ditentukan oleh admin."
        }
    }
    
    var driverAccount by remember { mutableStateOf<StaffAccount?>(null) }
    LaunchedEffect(order?.driverId) {
        val driverId = order?.driverId
        if (!driverId.isNullOrEmpty()) {
            if (driverId.startsWith("sim_") || driverId == "00000000-0000-0000-0000-0000000000d1") {
                driverAccount = StaffAccount(id = "00000000-0000-0000-0000-0000000000d1", username = "driver@lavana.com", password = "", role = "Driver", name = "Driver Budi")
            } else if (driverId == "00000000-0000-0000-0000-0000000000d2") {
                driverAccount = StaffAccount(id = "00000000-0000-0000-0000-0000000000d2", username = "driver2@lavana.com", password = "", role = "Driver", name = "Driver Andi")
            } else {
                try {
                    val staff = SupabaseClient.db["staff_accounts"].select {
                        filter {
                            eq("id", driverId)
                        }
                    }.decodeSingle<StaffAccount>()
                    driverAccount = staff
                } catch (e: Exception) {
                    // ignore
                }
            }
        } else {
            driverAccount = null
        }
    }
    
    // Titik Lokasi Driver & Rumah Pelanggan
    val customerHome = remember(order?.coordinates) {
        val parts = order?.coordinates?.split(",")
        val lat = parts?.getOrNull(0)?.toDoubleOrNull() ?: -6.2410
        val lon = parts?.getOrNull(1)?.toDoubleOrNull() ?: 106.8350
        LatLng(lat, lon)
    }

    val driverLocation = remember(order?.notes, order?.id, CartManager.simulatedDriverLocations[orderId]) {
        val loc = parseDriverLocation(order?.notes)
        if (loc != null) return@remember loc
        val simLoc = CartManager.simulatedDriverLocations[orderId]
        if (simLoc != null) return@remember simLoc
        LatLng(-6.2297, 106.8296) // Cafe location fallback
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lacak Pesanan", fontWeight = FontWeight.Bold, color = DarkCharcoal) },
                navigationIcon = {
                    IconButton(onClick = onNavigateHome) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali ke Beranda", tint = DarkCharcoal)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = LightGrayJco
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // Header Info Pesanan
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Pesanan #${order?.orderNumber ?: "..."}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkCharcoal)
                        Text(
                            text = orderType,
                            color = OrangeJco,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Status: ${order?.status ?: "Pending"}", fontSize = 12.sp, color = DarkCharcoal.copy(alpha = 0.8f))
                    Text(
                        text = when (order?.status) {
                            "Pending" -> "Menunggu konfirmasi admin."
                            "Diproses" -> "Sedang disiapkan di Dapur."
                            "Siap" -> "Pesanan siap! Silakan diambil/ditunggu."
                            "Diantar" -> {
                                val name = driverAccount?.name
                                if (name != null) "Sedang dalam pengiriman oleh $name." else "Sedang dalam pengiriman."
                            }
                            "Selesai" -> "Selesai! Terima kasih atas pesanan Anda."
                            else -> "Memuat..."
                        },
                        fontSize = 12.sp,
                        color = DarkCharcoal.copy(alpha = 0.8f)
                    )

                    // Lihat Resi Button
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray)
                    Button(
                        onClick = { showReceiptDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = LightOrangeJco),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, OrangeJco)
                    ) {
                        Text("Lihat Resi / Struk", color = OrangeJco, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
            
            if (order != null && (order?.paymentStatus == "Belum Bayar" || order?.paymentStatus == "Ditolak")) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, OrangeJco),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (order?.paymentStatus == "Ditolak") "Pembayaran Ditolak Admin" else "Penyelesaian Pembayaran",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (order?.paymentStatus == "Ditolak") Color.Red else OrangeJco
                        )
                        
                        if (order?.paymentStatus == "Ditolak") {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Alasan: $rejectionReason",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Red
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Silakan pilih metode pembayaran dan unggah bukti transfer:", fontSize = 12.sp, color = DarkCharcoal)
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("QRIS", "Transfer").forEach { method ->
                                val isSelected = selectedPayment == method
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (isSelected) LightOrangeJco else Color.White, RoundedCornerShape(8.dp))
                                        .border(1.dp, if (isSelected) OrangeJco else Color.LightGray, RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedPayment = method
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = method,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) OrangeJco else DarkCharcoal
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LightGrayJco, RoundedCornerShape(8.dp))
                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (selectedPayment == "QRIS") {
                                    Text("Scan QRIS Resmi Dapoer Lavana", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkCharcoal)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val displayQrisUrl = if (qrisUrl.isNotBlank()) qrisUrl else "https://mtjyggxyjojcvcjxiblo.supabase.co/storage/v1/object/public/menu-images/qris_barcode.png"
                                    AsyncImage(
                                        model = displayQrisUrl,
                                        contentDescription = "QRIS Barcode",
                                        modifier = Modifier
                                            .size(150.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                            .background(Color.White),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Text("Transfer Ke Rekening Resmi", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkCharcoal)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Bank:", fontSize = 11.sp, color = Color.Gray)
                                            Text(bankName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DarkCharcoal)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Nomor Rekening:", fontSize = 11.sp, color = Color.Gray)
                                            Text(bankNo, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DarkCharcoal)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Nama Penerima:", fontSize = 11.sp, color = Color.Gray)
                                            Text(bankOwner, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DarkCharcoal)
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Unggah Bukti Pembayaran", fontWeight = FontWeight.Bold, color = DarkCharcoal, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        val imagePickerLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.GetContent()
                        ) { uri ->
                            if (uri != null) {
                                uploadedFileUri = uri
                                uploadedFileName = "bukti_bayar_${orderId}_${System.currentTimeMillis()}.jpg"
                            }
                        }
                        
                        Button(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LightOrangeJco),
                            border = BorderStroke(1.dp, OrangeJco)
                        ) {
                            Text(
                                text = uploadedFileName ?: "Pilih Gambar Bukti Bayar",
                                color = OrangeJco,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                        
                        if (uploadError != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(uploadError!!, color = Color.Red, fontSize = 11.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = {
                                if (uploadedFileUri == null) {
                                    uploadError = "Silakan pilih gambar bukti bayar terlebih dahulu!"
                                    return@Button
                                }
                                isUploadingPayment = true
                                uploadError = null
                                coroutineScope.launch {
                                    try {
                                        var publicUrl = "https://mtjyggxyjojcvcjxiblo.supabase.co/storage/v1/object/public/menu-images/qris_barcode.png"
                                        try {
                                            val inputStream = context.contentResolver.openInputStream(uploadedFileUri!!)
                                            val bytes = inputStream?.readBytes()
                                            inputStream?.close()
                                            if (bytes != null) {
                                                val fileName = uploadedFileName ?: "bukti_bayar_${orderId}.jpg"
                                                val bucket = SupabaseClient.storage.from("menu-images")
                                                bucket.upload(fileName, bytes)
                                                publicUrl = "${SupabaseClient.SUPABASE_URL}/storage/v1/object/public/menu-images/$fileName"
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                        
                                        SupabaseClient.db["orders"].update({
                                            set("payment_status", "Menunggu Verifikasi")
                                            set("payment_method", selectedPayment)
                                            set("payment_receipt_url", publicUrl)
                                        }) { filter { eq("id", orderId) } }
                                        
                                        isUploadingPayment = false
                                        showThankYouDialog = true
                                        loadOrder()
                                    } catch (e: Exception) {
                                        uploadError = "Gagal memproses pembayaran: ${e.localizedMessage}"
                                        isUploadingPayment = false
                                    }
                                }
                            },
                            enabled = !isUploadingPayment,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeJco)
                        ) {
                            Text(
                                text = if (isUploadingPayment) "Mengirim..." else "Konfirmasi Pembayaran",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Linimasa Progress Status Pesanan (Diterima -> Disiapkan -> Diantar -> Selesai)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    TrackingStepItem(stepNumber = 1, title = "Pesanan Diterima", time = "10:15 WIB", isActive = activeStep >= 1)
                    TrackingStepItem(stepNumber = 2, title = "Sedang Disiapkan di Dapur", time = "10:25 WIB", isActive = activeStep >= 2)
                    TrackingStepItem(stepNumber = 3, title = "Sedang Dalam Pengiriman", time = "10:40 WIB", isActive = activeStep >= 3)
                    TrackingStepItem(stepNumber = 4, title = "Sampai di Lokasi Anda", time = "Menunggu", isActive = activeStep >= 4, isLast = true)
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // JIKA DELIVERY: Tampilkan Info Driver & Peta Live Lokasi Driver
            if (orderType == "Delivery") {
                Text(
                    text = "Posisi Live Kurir",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = DarkCharcoal,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                ) {
                    GoogleMapView(
                        center = driverLocation,
                        markers = listOf(
                            driverLocation to "${driverAccount?.name ?: "Kurir"} (Dalam Perjalanan)",
                            customerHome to "Rumah Anda"
                        ),
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Info Profil Kurir J.CO
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LightOrangeJco, RoundedCornerShape(12.dp))
                        .border(1.dp, OrangeJco.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    if (driverAccount != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar Driver
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(LightOrangeJco)
                                    .border(1.dp, OrangeJco, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                val initial = driverAccount?.name?.firstOrNull()?.toString() ?: "D"
                                Text(initial, fontWeight = FontWeight.Bold, color = OrangeJco, fontSize = 20.sp)
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(driverAccount?.name ?: "Driver Cafe Andro", fontWeight = FontWeight.Bold, color = DarkCharcoal, fontSize = 16.sp)
                                Text("Status: Sedang mengantar pesanan Anda", fontSize = 11.sp, color = DarkCharcoal.copy(alpha = 0.8f))
                                Text("Rating Kurir: 4.9 ★", fontSize = 11.sp, color = DarkCharcoal.copy(alpha = 0.8f))
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Menunggu Driver mengambil pesanan...",
                                color = OrangeJco,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Tombol "Kembali Ke Beranda" (Modern J.CO style)
            Button(
                onClick = onNavigateHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangeJco)
            ) {
                Text(
                    text = "Kembali ke Beranda",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            if (showReceiptDialog && order != null) {
                ReceiptDialog(
                    order = order!!,
                    orderItems = orderItems,
                    menuItemsMap = menuItemsMap,
                    onDismiss = { showReceiptDialog = false }
                )
            }
            if (showThankYouDialog) {
                AlertDialog(
                    onDismissRequest = { showThankYouDialog = false },
                    title = { Text("Terima Kasih", fontWeight = FontWeight.Bold, color = DarkCharcoal) },
                    text = { Text("Pesanan Anda akan segera kami proses.", color = DarkCharcoal) },
                    confirmButton = {
                        Button(
                            onClick = { showThankYouDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeJco)
                        ) {
                            Text("OK", color = Color.White)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun TrackingStepItem(
    stepNumber: Int,
    title: String,
    time: String,
    isActive: Boolean,
    isLast: Boolean = false
) {
    val indicatorColor = if (isActive) LightOrangeJco else Color.White
    val lineCol = if (isActive) OrangeJco else Color.LightGray
    val textCol = if (isActive) DarkCharcoal else Color.Gray

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp)
        ) {
            // Circle Step Number
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(indicatorColor, CircleShape)
                    .border(1.dp, if (isActive) OrangeJco else Color.LightGray, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stepNumber.toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) OrangeJco else Color.Gray
                )
            }
            
            // Vertical connector line
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(24.dp)
                        .background(lineCol)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textCol)
            Text(time, fontSize = 11.sp, color = textCol.copy(alpha = 0.7f))
        }
    }
}
