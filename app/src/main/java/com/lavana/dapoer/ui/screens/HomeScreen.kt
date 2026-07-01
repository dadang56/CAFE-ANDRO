@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.lavana.dapoer.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lavana.dapoer.R
import com.lavana.dapoer.data.*
import com.lavana.dapoer.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.viewinterop.AndroidView
import com.lavana.dapoer.ui.components.GoogleMapView
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.platform.LocalContext
import android.location.LocationManager
import android.location.LocationListener
import android.location.Location
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onOrderTypeSelected: (String) -> Unit,
    onNavigateToCart: () -> Unit,
    onLogout: () -> Unit,
    onTrackOrder: (String) -> Unit
) {
    var currentTab by remember { mutableIntStateOf(0) } // 0: Beranda, 1: Order, 2: Favorit, 3: Akun
    val context = LocalContext.current
    val sharedPref = remember(context) { context.getSharedPreferences("lavana_prefs", Context.MODE_PRIVATE) }
    var currentPhone by remember { mutableStateOf(sharedPref.getString("user_phone", "0812-9876-5432") ?: "0812-9876-5432") }
    val coroutineScope = rememberCoroutineScope()
    val isGuest = remember { SupabaseClient.currentUserId == null }

    LaunchedEffect(SupabaseClient.currentUserEmail) {
        val userEmail = SupabaseClient.currentUserEmail
        if (userEmail != null) {
            val localUsername = userEmail.substringBefore("@")
            if (localUsername.all { it.isDigit() } && localUsername.length >= 8) {
                currentPhone = localUsername
                sharedPref.edit().putString("user_phone", localUsername).apply()
            }
        }
    }

    // Wishlist/Favorit local database simulation state
    val favoriteItemIds = remember { mutableStateListOf<String>() }

    // Dynamic Menu Data
    var menuList by remember { mutableStateOf<List<MenuItem>>(emptyList()) }
    var isLoadingMenu by remember { mutableStateOf(true) }
    var bannerList by remember { mutableStateOf<List<BannerItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        CartManager.selectedOrderType = "Delivery"
        isLoadingMenu = true
        try {
            menuList = SupabaseClient.initializeMenuIfEmpty()
        } catch (e: Exception) {}

        try {
            val fetchedBanners = SupabaseClient.db["banners"].select().decodeList<BannerItem>()
            val filtered = fetchedBanners.filter { it.title?.startsWith("payment_") != true }
            if (filtered.isNotEmpty()) {
                bannerList = filtered
            }
        } catch (e: Exception) {}
        isLoadingMenu = false
    }

    val displayMenu = menuList

    Scaffold(
        bottomBar = {
            JcoBottomNavigation(
                currentTab = currentTab,
                onTabSelected = { tabIndex ->
                    currentTab = tabIndex
                }
            )
        },
        containerColor = Color.White,
        floatingActionButton = {
            if (CartManager.getItemsCount() > 0) {
                FloatingActionButton(
                    onClick = onNavigateToCart,
                    containerColor = OrangeJco,
                    contentColor = Color.White
                ) {
                    BadgedBox(
                        badge = {
                            Badge(containerColor = Color.Red) {
                                Text(CartManager.getItemsCount().toString(), color = Color.White)
                            }
                        }
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Keranjang")
                    }
                }
            }
        }
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
                label = "TabTransition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> TabBeranda(
                        displayMenu = displayMenu,
                        isLoadingMenu = isLoadingMenu,
                        bannerList = bannerList,
                        currentPhone = currentPhone,
                        favoriteItemIds = favoriteItemIds,
                        onNavigateToLogin = onLogout
                    )
                    1 -> TabOrderHistory(
                        onLogout = onLogout,
                        onTrackOrder = onTrackOrder
                    )
                    2 -> TabWishlist(
                        displayMenu = displayMenu,
                        favoriteItemIds = favoriteItemIds,
                        onNavigateToLogin = onLogout
                    )
                    3 -> TabAkunDetail(
                        currentPhone = currentPhone,
                        onPhoneChanged = { currentPhone = it },
                        onLogout = onLogout
                    )
                }
            }
        }
    }
}

// ==========================================================
// 1. TAB BERANDA (J.CO Style Dashboard)
// ==========================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TabBeranda(
    displayMenu: List<MenuItem>,
    isLoadingMenu: Boolean,
    bannerList: List<BannerItem>,
    currentPhone: String,
    favoriteItemIds: MutableList<String>,
    onNavigateToLogin: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPref = remember(context) { context.getSharedPreferences("lavana_prefs", android.content.Context.MODE_PRIVATE) }
    var latestVersionCode by remember { mutableStateOf(2.0) }
    var selectedCategory by remember { mutableStateOf("All") }
    var showOrderTypeSelectionDialog by remember { mutableStateOf(false) }
    var selectedItemForCart by remember { mutableStateOf<MenuItem?>(null) }
    val coroutineScope = rememberCoroutineScope()

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
            val rows = SupabaseClient.db["settings"].select {
                filter { eq("key", "latest_version_code") }
            }.decodeList<SettingsRow>()
            if (rows.isNotEmpty()) {
                latestVersionCode = rows.first().value
            }
        } catch (e: Exception) {}
    }

    val userEmail = remember { SupabaseClient.currentUserEmail }
    val userName = remember(userEmail) {
        if (userEmail != null) {
            val fullName = sharedPref.getString("name_$userEmail", null)
            if (!fullName.isNullOrBlank()) {
                fullName.trim().split(" ").firstOrNull() ?: fullName
            } else {
                userEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
            }
        } else {
            "Guest"
        }
    }

    val filteredItems = remember(displayMenu, selectedCategory) {
        if (selectedCategory == "All") {
            displayMenu
        } else {
            displayMenu.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(LightGrayJco)
    ) {
        // Modern floating teal-gradient welcome header with rounded bottom corners
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(Brush.verticalGradient(listOf(OrangeJco, ForestGreen)))
                .padding(top = 36.dp, bottom = 32.dp, start = 20.dp, end = 20.dp)
        ) {
            // Simple and minimalist background shapes
            Canvas(
                modifier = Modifier
                    .matchParentSize()
            ) {
                // Large soft circle on the top right
                drawCircle(
                    color = Color.White,
                    radius = size.minDimension * 0.65f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.95f, -size.height * 0.15f),
                    alpha = 0.08f
                )
                // Smaller soft circle on the bottom left
                drawCircle(
                    color = Color.White,
                    radius = size.minDimension * 0.45f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.05f, size.height * 1.15f),
                    alpha = 0.06f
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Selamat Datang,",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Text(
                        text = userName,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                        color = Color.White
                    )
                    if (userEmail == null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.18f))
                                .clickable { onNavigateToLogin() }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Silahkan Login terlebih dahulu",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowForwardIos,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }



        // Hot Promo Banner Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Hot Promo",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkCharcoal
                )
                Text(
                    text = "Penawaran spesial untukmu",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Text(
                text = "Lihat Semua",
                fontSize = 13.sp,
                color = OrangeJco,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { /* action */ }
            )
        }

        val fallbackBanners = remember {
            listOf(
                BannerItem("1", "https://images.unsplash.com/photo-1612240498936-65f5101365d2?w=800", "Your Favorite Picks Bundle"),
                BannerItem("2", "https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?w=800", "Dapoer Lavana Coffee Combo Deal"),
                BannerItem("3", "https://images.unsplash.com/photo-1551024601-bec78aea704b?w=800", "Sweet Assorted Box Promo")
            )
        }
        val activeBanners = if (bannerList.isNotEmpty()) bannerList else fallbackBanners

        if (activeBanners.isNotEmpty()) {
            val pagerState = rememberPagerState(pageCount = { activeBanners.size })
            
            LaunchedEffect(pagerState) {
                while (true) {
                    delay(4000)
                    if (activeBanners.isNotEmpty()) {
                        val nextPage = (pagerState.currentPage + 1) % activeBanners.size
                        try {
                            pagerState.animateScrollToPage(nextPage)
                        } catch (e: Exception) {}
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 16.dp),
                pageSpacing = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            ) { page ->
                val banner = activeBanners[page]
                Card(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    AsyncImage(
                        model = banner.imageUrl,
                        contentDescription = banner.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Pager Indicator Dots
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                activeBanners.forEachIndexed { index, _ ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (selected) 22.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(if (selected) OrangeJco else OrangeJco.copy(alpha = 0.22f))
                    )
                }
            }
        }



        // Apa yang ingin kamu cari? Section
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp)
        ) {
            Text(
                text = "Apa yang ingin kamu cari ?",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = DarkCharcoal
            )
            Text(
                text = "Pilih kategori menu favoritmu",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        // Categories Pills List
        val categories = listOf("All") + MENU_CATEGORIES
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) OrangeJco else Color.White)
                        .then(
                            if (isSelected) Modifier
                            else Modifier.border(
                                1.dp,
                                LightOrangeJco,
                                RoundedCornerShape(20.dp)
                            )
                        )
                        .clickable { selectedCategory = category }
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = category,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else DarkCharcoal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Product Catalog Grid (2-Column J.CO Style)
        if (isLoadingMenu) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = OrangeJco)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Memuat menu...",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            // LazyVerticalGrid doesn't work inside nested verticalScroll naturally without setting height,
            // so we calculate chunks or render inside FlowRow/custom grid manually to allow smooth scrolling of the whole page.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                val chunks = filteredItems.chunked(3)
                chunks.forEach { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { item ->
                            Box(modifier = Modifier.weight(1f)) {
                                JcoProductCard(
                                    item = item,
                                    isFavorite = favoriteItemIds.contains(item.id),
                                    onFavoriteClick = {
                                        if (favoriteItemIds.contains(item.id)) {
                                            favoriteItemIds.remove(item.id)
                                        } else {
                                            favoriteItemIds.add(item.id)
                                        }
                                    },
                                    onAddToCart = {
                                        CartManager.addItem(item)
                                        android.widget.Toast.makeText(context, "${item.name} berhasil ditambahkan ke keranjang", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                        repeat(3 - rowItems.size) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

// 3-Column J.CO Product Card
@Composable
fun JcoProductCard(
    item: MenuItem,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onAddToCart: () -> Unit
) {
    val context = LocalContext.current
    val quantity = CartManager.getItemQuantity(item)
    val originalPrice = item.getOriginalPrice()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column {
                // Food Image Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                ) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(108.dp)
                            .graphicsLayer { this.alpha = if (item.isAvailable) 1f else 0.5f }
                    )
                    if (!item.isAvailable) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Black.copy(alpha = 0.3f))
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("HABIS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                        .graphicsLayer { this.alpha = if (item.isAvailable) 1f else 0.6f }
                ) {
                    // Item Title
                    Text(
                        text = item.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = DarkCharcoal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    if (originalPrice != null && originalPrice > item.price) {
                        // Original Price (Strikethrough)
                        Text(
                            text = "Rp ${String.format("%,.0f", originalPrice)}",
                            fontSize = 9.sp,
                            color = Color.Gray,
                            textDecoration = TextDecoration.LineThrough
                        )
                    } else {
                        // Invisible placeholder to keep text heights aligned
                        Text(
                            text = "",
                            fontSize = 8.sp
                        )
                    }

                    // Current Promo Price
                    Text(
                        text = "Rp ${String.format("%,.0f", item.price)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = OrangeJco
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Add quantity / Cart action
                    if (!item.isAvailable) {
                        Button(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp),
                            shape = RoundedCornerShape(11.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.LightGray,
                                disabledContainerColor = Color.LightGray
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Habis", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else if (quantity == 0) {
                        Button(
                            onClick = onAddToCart,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp),
                            shape = RoundedCornerShape(11.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Beli", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OnAccentDark)
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(LightOrangeJco)
                                .border(1.dp, OrangeJco, RoundedCornerShape(11.dp)),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = {
                                    CartManager.removeItem(item)
                                    android.widget.Toast.makeText(context, "Jumlah ${item.name} dikurangi", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text("-", fontWeight = FontWeight.Bold, color = OrangeJco, fontSize = 12.sp)
                            }
                            Text(
                                text = quantity.toString(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangeJco
                            )
                            IconButton(
                                onClick = {
                                    CartManager.addItem(item)
                                    android.widget.Toast.makeText(context, "Jumlah ${item.name} ditambah", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text("+", fontWeight = FontWeight.Bold, color = OrangeJco, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Top-left promo badge (Only if originalPrice exists and > current price)
            if (originalPrice != null && originalPrice > item.price) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 18.dp, bottomEnd = 10.dp))
                        .background(RedPromo)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Promo",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Top-right Wishlist heart
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(26.dp)
                    .background(Color.White.copy(alpha = 0.92f), CircleShape)
                    .clickable(onClick = onFavoriteClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) OrangeAccent else Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// ==========================================================
// 2. TAB ORDER (Pesanan & History)
// ==========================================================
@Composable
fun TabOrderHistory(
    onLogout: () -> Unit,
    onTrackOrder: (String) -> Unit
) {
    val isGuest = remember { SupabaseClient.currentUserId == null }
    val coroutineScope = rememberCoroutineScope()
    var orderHistory by remember { mutableStateOf<List<Order>>(emptyList()) }
    var isLoadingHistory by remember { mutableStateOf(false) }
    var activeSubTab by remember { mutableIntStateOf(0) } // 0: Dalam Proses, 1: Selesai
    
    val orderDetails = remember { mutableStateMapOf<String, List<OrderItem>>() }
    val menuItemsMap = remember { mutableStateMapOf<String, String>() }

    var activeReceiptOrder by remember { mutableStateOf<Order?>(null) }
    var activeReceiptItems by remember { mutableStateOf<List<OrderItem>>(emptyList()) }

    fun loadOrderItems(orderId: String) {
        if (orderDetails.containsKey(orderId)) return
        coroutineScope.launch {
            try {
                val items = SupabaseClient.db["order_items"].select {
                    filter { eq("order_id", orderId) }
                }.decodeList<OrderItem>()
                if (items.isEmpty()) {
                    orderDetails[orderId] = listOf(OrderItem(orderId + "_item1", orderId, "jco-1", 2, 12000.0))
                } else {
                    orderDetails[orderId] = items
                }
            } catch (e: Exception) {
                orderDetails[orderId] = listOf(OrderItem(orderId + "_item1", orderId, "jco-1", 2, 12000.0))
            }
        }
    }

    LaunchedEffect(Unit) {
        val userId = SupabaseClient.currentUserId
        if (userId != null) {
            isLoadingHistory = true
            try {
                val fetched = SupabaseClient.db["orders"].select {
                    filter { eq("user_id", userId) }
                }.decodeList<Order>()
                orderHistory = fetched.sortedByDescending { it.createdAt ?: "" }
                orderHistory.forEach { order ->
                    order.id?.let { loadOrderItems(it) }
                }
            } catch (e: Exception) {
                // local dummy history
                orderHistory = listOf(
                    Order("1", 101, "2026-06-18T10:15:00Z", userId, "Dine In", "Selesai", "05", null, null, 0.0, 24000.0, 0.0, 24000.0, "QRIS", "Terbayar", null, null, null, null, null, null),
                    Order("2", 102, "2026-06-18T10:45:00Z", userId, "Delivery", "Diantar", null, "Jl. Melati No. 12", 2.5, 5000.0, 38000.0, 0.0, 43000.0, "Tunai", "Belum Bayar", null, null, "0812-3456-7890", "-6.2297,106.8296", null, null)
                )
                orderHistory.forEach { order ->
                    order.id?.let { loadOrderItems(it) }
                }
            } finally {
                isLoadingHistory = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Orange top bar
        JcoOrangeTopBar(title = "Pesanan")

        if (isGuest) {
            // Guest State
            JcoGuestStateUI(
                imageVector = Icons.Default.ReceiptLong,
                title = "Kamu belum login",
                description = "Login sekarang untuk melihat riwayat pesanan lezatmu!",
                buttonText = "Login Sekarang",
                onClick = onLogout
            )
        } else {
            // Logged in user: Tab Layout
            TabRow(
                selectedTabIndex = activeSubTab,
                containerColor = Color.White,
                contentColor = OrangeJco,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[activeSubTab]),
                        color = OrangeJco
                    )
                }
            ) {
                Tab(
                    selected = activeSubTab == 0,
                    onClick = { activeSubTab = 0 },
                    text = { Text("Dalam Proses", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeSubTab == 1,
                    onClick = { activeSubTab = 1 },
                    text = { Text("Selesai", fontWeight = FontWeight.Bold) }
                )
            }

            val inProcessStatuses = listOf("Pending", "Diproses", "Diantar", "Menunggu Driver", "Driver Sampai")
            val filteredOrders = remember(orderHistory, activeSubTab) {
                if (activeSubTab == 0) {
                    orderHistory.filter { it.status in inProcessStatuses }
                } else {
                    orderHistory.filter { it.status !in inProcessStatuses }
                }
            }

            if (isLoadingHistory) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LightGrayJco),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = OrangeJco)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Memuat pesanan...", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            } else if (filteredOrders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LightGrayJco)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .background(LightOrangeJco, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = OrangeJco
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Belum ada pesanan",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkCharcoal,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Pesananmu akan muncul di sini",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LightGrayJco)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredOrders) { order ->
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
                                    Column {
                                        Text(
                                            text = "Order #${order.orderNumber ?: "N/A"}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = DarkCharcoal
                                        )
                                        Text(
                                            text = "Tipe: ${order.orderType} | Metode: ${order.paymentMethod}",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = "Total: Rp ${String.format("%,.0f", order.total)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = OrangeJco
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                when (order.status) {
                                                    "Selesai" -> Color(0xFFE8F5E9)
                                                    "Pending" -> Color(0xFFFFF3E0)
                                                    else -> LightOrangeJco
                                                }
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = order.status,
                                            color = when (order.status) {
                                                "Selesai" -> Color(0xFF2E7D32)
                                                "Pending" -> Color(0xFFE65100)
                                                else -> OrangeJco
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color.LightGray.copy(alpha = 0.3f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (order.status != "Selesai" && order.orderType == "Delivery") {
                                        Text(
                                            text = "Lacak Driver",
                                            color = OrangeJco,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .clickable { order.id?.let { onTrackOrder(it) } }
                                                .padding(horizontal = 8.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                    }
                                    Text(
                                        text = "Lihat Resi",
                                        color = DarkCharcoal,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clickable {
                                                activeReceiptOrder = order
                                                activeReceiptItems = orderDetails[order.id ?: ""] ?: emptyList()
                                            }
                                            .padding(horizontal = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    activeReceiptOrder?.let { order ->
        ReceiptDialog(
            order = order,
            orderItems = activeReceiptItems,
            menuItemsMap = menuItemsMap,
            onDismiss = { activeReceiptOrder = null }
        )
    }
}

// ==========================================================
// 3. TAB FAVORIT (Wishlist)
// ==========================================================
@Composable
fun TabWishlist(
    displayMenu: List<MenuItem>,
    favoriteItemIds: List<String>,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val isGuest = remember { SupabaseClient.currentUserId == null }

    val wishlistItems = remember(displayMenu, favoriteItemIds) {
        displayMenu.filter { favoriteItemIds.contains(it.id) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        JcoOrangeTopBar(title = "Favorit")

        if (isGuest) {
            JcoGuestStateUI(
                imageVector = Icons.Default.FavoriteBorder,
                title = "Kamu belum login",
                description = "Masuk untuk menyimpan menu Dapoer Lavana favoritmu ke wishlist.",
                buttonText = "Login Sekarang",
                onClick = onNavigateToLogin
            )
        } else {
            if (wishlistItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LightGrayJco)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .background(LightOrangeJco, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FavoriteBorder,
                                contentDescription = "Empty Wishlist",
                                modifier = Modifier.size(64.dp),
                                tint = OrangeJco
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Belum ada menu favorit",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkCharcoal,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Ketuk ikon hati pada menu untuk menambahkan",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LightGrayJco)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(wishlistItems) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = item.imageUrl,
                                    contentDescription = item.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = DarkCharcoal,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Rp ${String.format("%,.0f", item.price)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = OrangeJco
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))

                                // Beli button
                                Button(
                                    onClick = {
                                        if (CartManager.selectedOrderType == null) {
                                            CartManager.selectedOrderType = "Dine In" // Default for quick buy
                                        }
                                        CartManager.addItem(item)
                                        android.widget.Toast.makeText(context, "${item.name} berhasil ditambahkan ke keranjang", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(38.dp),
                                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp)
                                ) {
                                    Text("Beli", fontSize = 12.sp, color = OnAccentDark, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================================
// 4. TAB AKUN (Profile & settings)
// ==========================================================
@Composable
fun TabAkunDetail(
    currentPhone: String,
    onPhoneChanged: (String) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val sharedPref = remember(context) { context.getSharedPreferences("lavana_prefs", Context.MODE_PRIVATE) }
    val userEmail = remember { SupabaseClient.currentUserEmail }
    val userName = remember(userEmail) {
        if (userEmail != null) {
            val fullName = sharedPref.getString("name_$userEmail", null)
            if (!fullName.isNullOrBlank()) {
                fullName.trim().split(" ").firstOrNull() ?: fullName
            } else {
                userEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
            }
        } else {
            "Guest"
        }
    }
    var currentAddress by remember { mutableStateOf(sharedPref.getString("default_address", "Jl. Melati No. 12, Kebayoran Baru, Jakarta Selatan") ?: "Jl. Melati No. 12, Kebayoran Baru, Jakarta Selatan") }
    var currentCoords by remember { mutableStateOf(sharedPref.getString("default_coordinates", "-6.2410,106.8350") ?: "-6.2410,106.8350") }

    var showAddressDialog by remember { mutableStateOf(false) }
    var showPhoneDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    // Dialog Phone Number
    if (showPhoneDialog) {
        var tempPhone by remember { mutableStateOf(currentPhone) }
        AlertDialog(
            onDismissRequest = { showPhoneDialog = false },
            title = { Text("Ubah Nomor HP", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tempPhone,
                    onValueChange = { tempPhone = it },
                    label = { Text("Nomor HP") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        sharedPref.edit().putString("user_phone", tempPhone).apply()
                        onPhoneChanged(tempPhone)
                        showPhoneDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeJco)
                ) {
                    Text("Simpan", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPhoneDialog = false }) {
                    Text("Batal", color = Color.Gray)
                }
            }
        )
    }

    // Dialog Address Pinpoint
    if (showAddressDialog) {
        val scope = rememberCoroutineScope()
        var tempAddress by remember { mutableStateOf(currentAddress) }
        var tempCoords by remember { mutableStateOf(currentCoords) }
        val parsedTempCoords = remember(tempCoords) {
            val parts = tempCoords.split(",")
            if (parts.size == 2) {
                LatLng(parts[0].toDoubleOrNull() ?: -6.2410, parts[1].toDoubleOrNull() ?: 106.8350)
            } else {
                LatLng(-6.2410, 106.8350)
            }
        }
        var currentMapLocation by remember { mutableStateOf(parsedTempCoords) }
        var searchQuery by remember { mutableStateOf("") }
        var isSearching by remember { mutableStateOf(false) }
        var suggestions by remember { mutableStateOf<List<com.lavana.dapoer.data.SearchResult>>(emptyList()) }

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

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                          permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) {
                getUserLocation(context) { loc ->
                    tempCoords = "${loc.latitude},${loc.longitude}"
                    currentMapLocation = loc
                    // Auto reverse-geocode street name
                    scope.launch {
                        val street = com.lavana.dapoer.data.GeocodingHelper.reverseGeocode(loc.latitude, loc.longitude)
                        if (street.isNotBlank()) {
                            tempAddress = street
                        }
                    }
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showAddressDialog = false },
            title = { Text("Ubah Alamat Pengiriman", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Search Address Field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Cari Lokasi / Alamat") },
                            placeholder = { Text("Ketik alamat...") },
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
                                    scope.launch {
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

                    // Search suggestions list
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
                                                tempAddress = result.displayName
                                                tempCoords = "${result.latitude},${result.longitude}"
                                                currentMapLocation = LatLng(result.latitude, result.longitude)
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

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = tempAddress,
                        onValueChange = { tempAddress = it },
                        label = { Text("Alamat Hasil Pinpoint") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // GPS Detector Button
                    Button(
                        onClick = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "GPS", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Deteksi Lokasi GPS Otomatis", color = Color.White, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Ketuk peta untuk pinpoint koordinat (alamat terupdate otomatis):",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color.LightGray)
                    ) {
                        GoogleMapView(
                            center = currentMapLocation,
                            markers = listOf(currentMapLocation to "Alamat Pengiriman"),
                            onMapClick = { p ->
                                currentMapLocation = p
                                tempCoords = "${p.latitude},${p.longitude}"
                                scope.launch {
                                    val street = com.lavana.dapoer.data.GeocodingHelper.reverseGeocode(p.latitude, p.longitude)
                                    if (street.isNotBlank()) {
                                        tempAddress = street
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Lokasi Terpilih: $tempAddress",
                        fontSize = 11.sp,
                        color = OrangeJco
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        sharedPref.edit().apply {
                            putString("default_address", tempAddress)
                            putString("default_coordinates", tempCoords)
                            apply()
                        }
                        currentAddress = tempAddress
                        currentCoords = tempCoords
                        showAddressDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeJco)
                ) {
                    Text("Simpan", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddressDialog = false }) {
                    Text("Batal", color = Color.Gray)
                }
            }
        )
    }

    if (showTermsDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showTermsDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Syarat dan Ketentuan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = DarkCharcoal
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Selamat datang di Dapoer Lavana Delivery. Dengan menggunakan aplikasi kami, Anda menyetujui syarat & ketentuan berikut:",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        BulletPoint("1", "Layanan Kami adalah khusus Delivery. Tidak ada pilihan Dine In (makan di tempat) atau Take Away.")
                        BulletPoint("2", "Ongkos kirim dihitung berdasarkan jarak per kilometer dari lokasi toko Dapoer Lavana terdekat ke titik pengiriman Anda.")
                        BulletPoint("3", "Pembayaran dilakukan menggunakan QRIS dan Transfer.")
                        BulletPoint("4", "Pembatalan pesanan hanya dapat dilakukan sebelum admin menyetujui pesanan Anda.")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showTermsDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeJco),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Tutup", color = Color.White)
                    }
                }
            }
        }
    }

    if (showPrivacyDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showPrivacyDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Kebijakan & Privasi",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = DarkCharcoal
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Kebijakan Privasi Dapoer Lavana:",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        BulletPoint("1", "Informasi Akun: Kami menyimpan email dan data profil Anda secara aman pada database Supabase terenkripsi.")
                        BulletPoint("2", "Data Alamat & Lokasi: Alamat utama dan koordinat GPS disimpan secara lokal di perangkat Anda melalui SharedPreferences untuk mempermudah alur checkout.")
                        BulletPoint("3", "Data Pesanan: Histori transaksi Anda disimpan untuk melacak pengiriman dan riwayat pembelian Anda.")
                        BulletPoint("4", "Izin Lokasi: Aplikasi memerlukan izin lokasi perangkat untuk menentukan posisi akurat tujuan pengiriman Anda pada peta.")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showPrivacyDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeJco),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Tutup", color = Color.White)
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightGrayJco)
    ) {
        JcoOrangeTopBar(title = "Akun")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Profile Card (Cartoon Avatar + Guest/User Details)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (userEmail == null) {
                            onLogout() // redirect to login
                        }
                    },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.profile_picture),
                            contentDescription = "Avatar Profile",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = userName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = DarkCharcoal
                            )
                            if (userEmail != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { showPhoneDialog = true }
                                ) {
                                    Text(
                                        text = "No. HP: $currentPhone",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Phone",
                                        tint = OrangeJco,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Text(
                                    text = "Dapoer Lavana Club",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OrangeJco,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            } else {
                                Text(
                                    text = "Silakan login terlebih dahulu",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowForwardIos,
                        contentDescription = "Detail",
                        tint = Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Alamat Pengiriman Utama
            if (userEmail != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Alamat Pengiriman Utama",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(LightOrangeJco, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = OrangeJco,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Lokasi Pengantaran Utama",
                                    fontWeight = FontWeight.Bold,
                                    color = DarkCharcoal,
                                    fontSize = 14.sp
                                )
                            }
                            
                            TextButton(onClick = { showAddressDialog = true }) {
                                Text("Ubah", color = OrangeJco, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentAddress,
                            fontSize = 13.sp,
                            color = DarkCharcoal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Koordinat: $currentCoords",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // General Settings Card
            Text(
                text = "General",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    SettingsRowItem(
                        icon = Icons.Default.Phone,
                        title = "Hubungi Kami",
                        description = "Hubungi Admin Dapoer Lavana via WhatsApp",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://wa.me/6281328884312")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:081328884312")
                                }
                                try {
                                    context.startActivity(dialIntent)
                                } catch (ex: Exception) {}
                            }
                        }
                    )
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f), thickness = 0.8.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRowItem(
                        icon = Icons.Default.Description,
                        title = "Syarat dan Ketentuan",
                        description = "Lihat syarat dan ketentuan kami",
                        onClick = { showTermsDialog = true }
                    )
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f), thickness = 0.8.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRowItem(
                        icon = Icons.Default.Lock,
                        title = "Kebijakan & Privasi",
                        description = "Lihat privasi dan kebijakan kami",
                        onClick = { showPrivacyDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Logout/Login Button Card
            Button(
                onClick = {
                    SupabaseClient.setMockUser(null, null)
                    onLogout()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (userEmail != null) Color.White else OrangeAccent,
                    contentColor = if (userEmail != null) Color.Red else OnAccentDark
                ),
                shape = RoundedCornerShape(14.dp),
                border = if (userEmail != null) BorderStroke(1.dp, Color.Red) else null
            ) {
                Text(
                    text = if (userEmail != null) "Keluar Akun" else "Login Sekarang",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // App version at bottom
            Text(
                text = "2.0.1 (43)",
                fontSize = 11.sp,
                color = Color.LightGray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun SettingsRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(LightOrangeJco, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = OrangeJco
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, color = DarkCharcoal, fontSize = 13.sp)
                Text(text = description, color = Color.Gray, fontSize = 11.sp)
            }
        }
        Icon(
            imageVector = Icons.Default.ArrowForwardIos,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(12.dp)
        )
    }
}

@Composable
fun BulletPoint(number: String, text: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "$number. ",
            fontWeight = FontWeight.Bold,
            color = OrangeJco,
            fontSize = 13.sp
        )
        Text(
            text = text,
            color = DarkCharcoal,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

// ==========================================================
// SHARED UI HELPER COMPONENTS
// ==========================================================

@Composable
fun JcoOrangeTopBar(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(OrangeJco, ForestGreen)))
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun JcoGuestStateUI(
    iconResId: Int? = null,
    imageVector: ImageVector? = null,
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (imageVector != null) {
                Icon(
                    imageVector = imageVector,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    tint = OrangeJco.copy(alpha = 0.4f)
                )
            } else if (iconResId != null) {
                Image(
                    painter = painterResource(id = iconResId),
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DarkCharcoal,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = buttonText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnAccentDark
                )
            }
        }
    }
}

@Composable
fun JcoBottomNavigation(
    currentTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            JcoBottomTabItem(
                label = "Beranda",
                icon = Icons.Default.Home,
                isSelected = currentTab == 0,
                onClick = { onTabSelected(0) }
            )
            JcoBottomTabItem(
                label = "Order",
                icon = Icons.Default.ReceiptLong,
                isSelected = currentTab == 1,
                onClick = { onTabSelected(1) }
            )
            JcoBottomTabItem(
                label = "Favorit",
                icon = if (currentTab == 2) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                isSelected = currentTab == 2,
                selectedColor = OrangeAccent,
                onClick = { onTabSelected(2) }
            )
            JcoBottomTabItem(
                label = "Akun",
                icon = Icons.Default.Person,
                isSelected = currentTab == 3,
                onClick = { onTabSelected(3) }
            )
        }
    }
}

@Composable
fun JcoBottomTabItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    selectedColor: Color = OrangeJco,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = if (isSelected) selectedColor else Color.Gray
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (isSelected) selectedColor else Color.Gray,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private fun getUserLocation(context: Context, onLocationReceived: (LatLng) -> Unit) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    try {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            
            var loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (loc == null) {
                loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }
            if (loc == null) {
                loc = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            }
            
            if (loc != null) {
                onLocationReceived(LatLng(loc.latitude, loc.longitude))
            }
            
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    onLocationReceived(LatLng(location.latitude, location.longitude))
                    locationManager.removeUpdates(this)
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, context.mainLooper)
            } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, context.mainLooper)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
