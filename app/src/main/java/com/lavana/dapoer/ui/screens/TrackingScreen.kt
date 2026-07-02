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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Chat
import androidx.compose.ui.graphics.Brush
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
    var showChatDialog by remember { mutableStateOf(false) }

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

            // Hubungkan realtime DULU, baru subscribe ke channel.
            SupabaseClient.realtime.connect()
            channel.subscribe()
        } catch (e: Exception) {
            // websocket error fallback
            e.printStackTrace()
        }
    }

    // Polling fallback: setiap 3 detik ambil ulang baris order agar lokasi
    // driver (notes) ikut diperbarui meski realtime gagal/terlambat.
    // Coroutine ini otomatis dibatalkan saat composable keluar.
    LaunchedEffect(orderId) {
        while (true) {
            kotlinx.coroutines.delay(3000)
            try {
                val fresh = SupabaseClient.db["orders"].select {
                    filter { eq("id", orderId) }
                }.decodeList<Order>().firstOrNull()
                if (fresh != null) {
                    order = fresh
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
                actions = {
                    if (order?.id != null) {
                        IconButton(onClick = { showChatDialog = true }) {
                            Icon(Icons.Default.Chat, contentDescription = "Chat dengan Admin", tint = OrangeJco)
                        }
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    // Gradient top strip with order number + type
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(OrangeJco, ForestGreen)))
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Pesanan", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                                Text("#${order?.orderNumber ?: "..."}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                            }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = orderType,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Status: ${order?.status ?: "Pending"}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DarkCharcoal)
                        Spacer(modifier = Modifier.height(2.dp))
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
                            color = Color.Gray
                        )

                        // Lihat Resi Button
                        HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = Color.LightGray.copy(alpha = 0.4f))
                        OutlinedButton(
                            onClick = { showReceiptDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangeJco),
                            border = BorderStroke(1.5.dp, OrangeJco),
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Lihat Resi / Struk", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
            
            if (order != null && (order?.paymentStatus == "Belum Bayar" || order?.paymentStatus == "Ditolak")) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, OrangeJco.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(18.dp)
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
                                        .clip(RoundedCornerShape(50))
                                        .background(if (isSelected) OrangeJco else Color.White)
                                        .border(1.dp, if (isSelected) OrangeJco else LightOrangeJco, RoundedCornerShape(50))
                                        .clickable {
                                            selectedPayment = method
                                        }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = method,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) Color.White else DarkCharcoal
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
                                    Spacer(modifier = Modifier.height(8.dp))
                                    var isDownloadingQris by remember { mutableStateOf(false) }
                                    val qrisDownloadScope = rememberCoroutineScope()
                                    OutlinedButton(
                                        onClick = {
                                            if (!isDownloadingQris) {
                                                isDownloadingQris = true
                                                qrisDownloadScope.launch {
                                                    val ok = downloadImageToGallery(context, displayQrisUrl, "QRIS_DapoerLavana_${System.currentTimeMillis()}.jpg")
                                                    Toast.makeText(
                                                        context,
                                                        if (ok) "QRIS tersimpan ke Galeri. Buka aplikasi m-banking lalu pilih gambar ini untuk scan." else "Gagal mengunduh QRIS.",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                    isDownloadingQris = false
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangeJco),
                                        border = BorderStroke(1.dp, OrangeJco),
                                        shape = RoundedCornerShape(8.dp),
                                        enabled = !isDownloadingQris,
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        if (isDownloadingQris) {
                                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = OrangeJco)
                                        } else {
                                            Icon(Icons.Default.Download, contentDescription = null, tint = OrangeJco, modifier = Modifier.size(16.dp))
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Unduh Gambar QRIS", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                } else {
                                    Text("Transfer Ke Rekening Resmi", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkCharcoal)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Bank:", fontSize = 11.sp, color = Color.Gray)
                                            Text(bankName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DarkCharcoal)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Nomor Rekening:", fontSize = 11.sp, color = Color.Gray)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(bankNo, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DarkCharcoal)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    Icons.Default.ContentCopy,
                                                    contentDescription = "Salin Nomor Rekening",
                                                    tint = OrangeJco,
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clickable {
                                                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(bankNo))
                                                            Toast.makeText(context, "Nomor rekening disalin!", Toast.LENGTH_SHORT).show()
                                                        }
                                                )
                                            }
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
                                                val bucket = SupabaseClient.storage["menu-images"]

                                                // Hapus bukti lama jika ada agar storage tidak menumpuk file yatim.
                                                try {
                                                    val oldUrl = order?.paymentReceiptUrl
                                                    val bucketPath = "/storage/v1/object/public/menu-images/"
                                                    if (oldUrl != null && oldUrl.contains(bucketPath)) {
                                                        val oldFile = oldUrl.substringAfter(bucketPath).substringBefore("?")
                                                        if (oldFile.isNotBlank() && oldFile != "qris_barcode.png") {
                                                            bucket.delete(oldFile)
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }

                                                bucket.upload(fileName, bytes)
                                                publicUrl = bucket.publicUrl(fileName)
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
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
                        ) {
                            Text(
                                text = if (isUploadingPayment) "Mengirim..." else "Konfirmasi Pembayaran",
                                color = OnAccentDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Linimasa Progress Status Pesanan (Diterima -> Disiapkan -> Diantar -> Selesai)
            Text(
                text = "Status Pesanan",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = DarkCharcoal,
                modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TrackingStepItem(stepNumber = 1, title = "Pesanan Diterima", time = "10:15 WIB", isActive = activeStep >= 1)
                    TrackingStepItem(stepNumber = 2, title = "Sedang Disiapkan di Dapur", time = "10:25 WIB", isActive = activeStep >= 2)
                    TrackingStepItem(stepNumber = 3, title = "Sedang Dalam Pengiriman", time = "10:40 WIB", isActive = activeStep >= 3)
                    TrackingStepItem(stepNumber = 4, title = "Sampai di Lokasi Anda", time = "Menunggu", isActive = activeStep >= 4, isLast = true)
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // JIKA DELIVERY: Tampilkan Info Driver & Peta Live Lokasi Driver
            if (orderType == "Delivery") {
                Column(modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)) {
                    Text(
                        text = "Posisi Live Kurir",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = DarkCharcoal
                    )
                    Text(
                        text = "Pantau lokasi kurir secara langsung",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(18.dp))
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
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                // Info Profil Kurir
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    if (driverAccount != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar Driver
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(LightOrangeJco),
                                contentAlignment = Alignment.Center
                            ) {
                                val initial = driverAccount?.name?.firstOrNull()?.toString() ?: "D"
                                Text(initial, fontWeight = FontWeight.Bold, color = OrangeJco, fontSize = 22.sp)
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(driverAccount?.name ?: "Driver Cafe Andro", fontWeight = FontWeight.Bold, color = DarkCharcoal, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = OrangeJco, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sedang mengantar pesanan Anda", fontSize = 11.sp, color = Color.Gray)
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(LightOrangeJco)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = OrangeAccent, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("4.9", fontWeight = FontWeight.Bold, color = ForestGreen, fontSize = 12.sp)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(LightOrangeJco),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = OrangeJco, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Mencari kurir",
                                    color = DarkCharcoal,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Menunggu Driver mengambil pesanan...",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Tombol "Kembali Ke Beranda"
            Button(
                onClick = onNavigateHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangeJco)
            ) {
                Icon(Icons.Default.Home, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
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
            if (showChatDialog && order?.id != null) {
                val customerName = remember { SupabaseClient.currentUserEmail?.substringBefore("@") ?: "Pelanggan" }
                ChatDialog(
                    orderId = order!!.id!!,
                    orderNumber = order?.orderNumber,
                    currentSenderRole = "Customer",
                    currentSenderName = customerName,
                    onDismiss = { showChatDialog = false }
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
    val lineCol = if (isActive) OrangeJco else Color.LightGray.copy(alpha = 0.5f)
    val textCol = if (isActive) DarkCharcoal else Color.Gray

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp)
        ) {
            // Circle Step Indicator: filled teal when active/completed
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isActive) OrangeJco else LightOrangeJco)
                    .border(1.dp, if (isActive) OrangeJco else Color.LightGray, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isActive) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(
                        text = stepNumber.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangeJco.copy(alpha = 0.6f)
                    )
                }
            }

            // Vertical connector line
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(lineCol)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.padding(top = 2.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textCol)
            Text(time, fontSize = 11.sp, color = textCol.copy(alpha = 0.7f))
        }
    }
}

// Unduh gambar dari URL (mis. QRIS) dan simpan ke Galeri (MediaStore.Images) agar
// pelanggan bisa membukanya lewat aplikasi m-banking (fitur scan dari galeri).
private suspend fun downloadImageToGallery(context: android.content.Context, imageUrl: String, filename: String): Boolean {
    return try {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val bytes = java.net.URL(imageUrl).openStream().use { it.readBytes() }
            val resolver = context.contentResolver
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES)
                }
            }
            val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { it.write(bytes) }
                true
            } else {
                false
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
