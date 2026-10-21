package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PlayerProfile

@Composable
fun TopResourceBar(
    profile: PlayerProfile?,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF14171A),
        tonalElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF2B333B), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Level badge
            Box(
                modifier = Modifier
                    .background(Color(0xFF2E7D32), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "LVL ${profile?.level ?: 1}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Coins
            ResourceChip(
                label = "COINS",
                value = "${profile?.coins ?: 0}",
                color = Color(0xFFFFB300),
                symbol = "🪙"
            )

            // Food
            ResourceChip(
                label = "FOOD",
                value = "${profile?.food ?: 0}",
                color = Color(0xFF81C784),
                symbol = "🍞"
            )

            // Metal
            ResourceChip(
                label = "METAL",
                value = "${profile?.metal ?: 0}",
                color = Color(0xFF90A4AE),
                symbol = "🔩"
            )

            // Energy
            ResourceChip(
                label = "NRG",
                value = "${profile?.energy ?: 50}",
                color = Color(0xFF4DD0E1),
                symbol = "⚡"
            )
        }
    }
}

@Composable
private fun ResourceChip(
    label: String,
    value: String,
    color: Color,
    symbol: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .background(Color(0xFF1C2228), RoundedCornerShape(4.dp))
            .border(0.5.dp, Color(0xFF333D47), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(symbol, fontSize = 11.sp)
        Column {
            Text(
                text = value,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}
