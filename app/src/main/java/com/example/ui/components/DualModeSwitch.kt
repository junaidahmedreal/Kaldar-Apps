package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.locale.AppStrings

@Composable
fun DualModeSwitch(
    currentMode: String,
    onModeChanged: (String) -> Unit,
    language: String = "en",
    modifier: Modifier = Modifier
) {
    val isPersonal = currentMode == "PERSONAL"

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("dual_mode_switch"),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Personal Mode Button
            val personalBg by animateColorAsState(
                targetValue = if (isPersonal) MaterialTheme.colorScheme.primary else Color.Transparent,
                animationSpec = tween(200),
                label = "personalBg"
            )
            val personalFg by animateColorAsState(
                targetValue = if (isPersonal) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(200),
                label = "personalFg"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(personalBg)
                    .clickable { onModeChanged("PERSONAL") }
                    .testTag("mode_personal_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = personalFg,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = " " + AppStrings.get("personal", language),
                        color = personalFg,
                        fontWeight = if (isPersonal) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }

            // Business Mode Button
            val businessBg by animateColorAsState(
                targetValue = if (!isPersonal) MaterialTheme.colorScheme.secondary else Color.Transparent,
                animationSpec = tween(200),
                label = "businessBg"
            )
            val businessFg by animateColorAsState(
                targetValue = if (!isPersonal) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(200),
                label = "businessFg"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(businessBg)
                    .clickable { onModeChanged("BUSINESS") }
                    .testTag("mode_business_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = null,
                        tint = businessFg,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = " " + AppStrings.get("business", language),
                        color = businessFg,
                        fontWeight = if (!isPersonal) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
