package com.lavana.dapoer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lavana.dapoer.data.*
import com.lavana.dapoer.ui.theme.*


@Composable
fun ReceiptDialog(
    order: Order,
    orderItems: List<OrderItem>,
    menuItemsMap: Map<String, String>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Terracotta)
            ) {
                Text("Tutup", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("DAPOER LAVANA", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = ForestGreen, fontFamily = FontFamily.SansSerif)
                Text("Struk Pembayaran Digital", fontSize = 12.sp, color = ForestGreen.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = ForestGreen.copy(alpha = 0.3f), thickness = 1.dp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Info Struk
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("No. Order:", fontSize = 11.sp, color = ForestGreen, fontWeight = FontWeight.Bold)
                    Text("#${order.orderNumber ?: "N/A"}", fontSize = 11.sp, color = ForestGreen, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tipe Order:", fontSize = 11.sp, color = ForestGreen)
                    Text(order.orderType, fontSize = 11.sp, color = ForestGreen, fontWeight = FontWeight.Bold)
                }
                
                // Cek nama pelanggan dari notes
                val customerName = remember(order.notes) {
                    if (order.notes?.startsWith("Atas Nama:") == true) {
                        order.notes.substringAfter("Atas Nama:").substringBefore(" (Pemesanan").trim()
                    } else {
                        null
                    }
                }
                if (customerName != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Pelanggan:", fontSize = 11.sp, color = ForestGreen)
                        Text(customerName, fontSize = 11.sp, color = ForestGreen, fontWeight = FontWeight.Bold)
                    }
                }

                if (order.orderType == "Dine In" && !order.tableNumber.isNullOrEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("No. Meja:", fontSize = 11.sp, color = ForestGreen)
                        Text(order.tableNumber, fontSize = 11.sp, color = ForestGreen, fontWeight = FontWeight.Bold)
                    }
                } else if (order.orderType == "Delivery" && !order.deliveryAddress.isNullOrEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Alamat Pengiriman:", fontSize = 11.sp, color = ForestGreen)
                        Text(order.deliveryAddress, fontSize = 11.sp, color = ForestGreen.copy(alpha = 0.8f))
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Metode Bayar:", fontSize = 11.sp, color = ForestGreen)
                    Text(order.paymentMethod, fontSize = 11.sp, color = ForestGreen, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Status Bayar:", fontSize = 11.sp, color = ForestGreen)
                    Text(
                        text = order.paymentStatus,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (order.paymentStatus == "Terbayar") ForestGreen else Terracotta
                    )
                }
                
                if (!order.createdAt.isNullOrEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Waktu:", fontSize = 11.sp, color = ForestGreen)
                        val cleanTime = order.createdAt.substringBefore(".").replace("T", " ")
                        Text(cleanTime, fontSize = 11.sp, color = ForestGreen)
                    }
                }

                HorizontalDivider(color = ForestGreen.copy(alpha = 0.3f), thickness = 1.dp)
                
                // Rincian Menu
                Text("Daftar Pesanan:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ForestGreen)
                if (orderItems.isEmpty()) {
                    Text("- Tidak ada detail item -", fontSize = 11.sp, color = Color.Gray)
                } else {
                    orderItems.forEach { item ->
                        val menuName = menuItemsMap[item.menuItemId] ?: "Menu Tidak Dikenal"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(menuName, fontSize = 12.sp, color = ForestGreen)
                                Text("${item.quantity} x Rp ${String.format("%,.0f", item.priceAtOrder)}", fontSize = 10.sp, color = ForestGreen.copy(alpha = 0.6f))
                            }
                            Text("Rp ${String.format("%,.0f", item.priceAtOrder * item.quantity)}", fontSize = 12.sp, color = ForestGreen)
                        }
                    }
                }

                HorizontalDivider(color = ForestGreen.copy(alpha = 0.3f), thickness = 1.dp)

                // Rincian Biaya
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subtotal:", fontSize = 11.sp, color = ForestGreen)
                    Text("Rp ${String.format("%,.0f", order.subtotal)}", fontSize = 11.sp, color = ForestGreen)
                }
                if (order.deliveryFee > 0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Ongkos Kirim:", fontSize = 11.sp, color = ForestGreen)
                        Text("Rp ${String.format("%,.0f", order.deliveryFee)}", fontSize = 11.sp, color = ForestGreen)
                    }
                }
                if (order.discountAmount > 0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Diskon:", fontSize = 11.sp, color = Terracotta)
                        Text("-Rp ${String.format("%,.0f", order.discountAmount)}", fontSize = 11.sp, color = Terracotta)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ForestGreen)
                    Text("Rp ${String.format("%,.0f", order.total)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Terracotta)
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Terima kasih telah memesan di Dapoer Lavana!",
                    fontSize = 10.sp,
                    color = ForestGreen.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}