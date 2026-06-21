package com.webscraper.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webscraper.ui.theme.*

@Composable
fun StatusBadge(status: String) {
    val (color, label) = when (status) {
        "pending" -> StatusPending to "待处理"
        "running" -> StatusRunning to "采集中"
        "completed" -> StatusCompleted to "已完成"
        "failed" -> StatusFailed to "失败"
        else -> Color.Gray to status
    }
    Text(text = label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier.background(color, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 2.dp))
}
