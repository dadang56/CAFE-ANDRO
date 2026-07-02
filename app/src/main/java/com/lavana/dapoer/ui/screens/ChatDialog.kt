package com.lavana.dapoer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lavana.dapoer.data.ChatMessage
import com.lavana.dapoer.data.SupabaseClient
import com.lavana.dapoer.ui.theme.*
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Chat per-pesanan antara Admin dan Pelanggan. Dipakai dari AdminScreen (sender = "Admin")
 * dan TrackingScreen (sender = "Customer") sehingga keduanya melihat thread yang sama.
 */
@Composable
fun ChatDialog(
    orderId: String,
    orderNumber: Int?,
    currentSenderRole: String,
    currentSenderName: String,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSending by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    fun loadMessages(showLoading: Boolean = false) {
        if (showLoading) isLoading = true
        coroutineScope.launch {
            try {
                val fetched = SupabaseClient.db["chat_messages"].select {
                    filter { eq("order_id", orderId) }
                }.decodeList<ChatMessage>().sortedBy { it.createdAt ?: "" }
                messages = fetched
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(orderId) {
        loadMessages(showLoading = true)
        try {
            val channel = SupabaseClient.realtime.channel("chat_$orderId")
            val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "chat_messages"
                filter = "order_id=eq.$orderId"
            }
            coroutineScope.launch {
                changeFlow.collectLatest { change ->
                    if (change is PostgresAction.Insert) {
                        val newMsg = change.decodeRecord<ChatMessage>()
                        if (messages.none { it.id != null && it.id == newMsg.id }) {
                            messages = (messages + newMsg).sortedBy { it.createdAt ?: "" }
                        }
                    }
                }
            }
            SupabaseClient.realtime.connect()
            channel.subscribe()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Polling cadangan bila realtime gagal tersambung (mis. jaringan tidak stabil).
    LaunchedEffect(orderId) {
        while (true) {
            delay(4000)
            loadMessages(showLoading = false)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun sendMessage() {
        val trimmed = inputText.trim()
        if (trimmed.isEmpty() || isSending) return
        isSending = true
        coroutineScope.launch {
            try {
                SupabaseClient.db["chat_messages"].insert(
                    ChatMessage(
                        orderId = orderId,
                        senderRole = currentSenderRole,
                        senderName = currentSenderName,
                        message = trimmed
                    )
                )
                inputText = ""
                loadMessages()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isSending = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = LightGrayJco
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(OrangeJco, ForestGreen)))
                        .padding(horizontal = 12.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Tutup", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            "Chat Pesanan #${orderNumber ?: "-"}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            if (currentSenderRole == "Admin") "Anda sebagai Admin" else "Anda sebagai Pelanggan",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp
                        )
                    }
                }

                // Message list
                Box(modifier = Modifier.weight(1f)) {
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = OrangeJco)
                        }
                    } else if (messages.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(LightOrangeJco),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = OrangeJco, modifier = Modifier.size(36.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Belum ada pesan", fontWeight = FontWeight.Bold, color = DarkCharcoal)
                            Text("Mulai percakapan di bawah ini", fontSize = 12.sp, color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(messages) { msg ->
                                val isMine = msg.senderRole == currentSenderRole
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .widthIn(max = 280.dp)
                                            .clip(
                                                RoundedCornerShape(
                                                    topStart = 14.dp, topEnd = 14.dp,
                                                    bottomStart = if (isMine) 14.dp else 2.dp,
                                                    bottomEnd = if (isMine) 2.dp else 14.dp
                                                )
                                            )
                                            .background(if (isMine) OrangeJco else Color.White)
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        if (!isMine) {
                                            Text(
                                                msg.senderName ?: msg.senderRole,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = OrangeJco
                                            )
                                        }
                                        Text(
                                            msg.message,
                                            fontSize = 13.sp,
                                            color = if (isMine) Color.White else DarkCharcoal
                                        )
                                        Text(
                                            msg.createdAt?.take(16)?.replace("T", " ") ?: "",
                                            fontSize = 9.sp,
                                            color = if (isMine) Color.White.copy(alpha = 0.7f) else Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Input row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ketik pesan...", fontSize = 13.sp) },
                        shape = RoundedCornerShape(20.dp),
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { sendMessage() },
                        enabled = !isSending && inputText.isNotBlank(),
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank()) OrangeAccent else Color.LightGray)
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = OnAccentDark)
                        } else {
                            Icon(Icons.Default.Send, contentDescription = "Kirim", tint = OnAccentDark)
                        }
                    }
                }
            }
        }
    }
}
