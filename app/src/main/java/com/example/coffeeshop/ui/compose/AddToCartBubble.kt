package com.example.coffeeshop.ui.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.coffeeshop.R
import kotlinx.coroutines.delay

// ─── Colors ─────────────────────────────────────────────────────────────────
private val BubbleGradientStart = Color(0xFF6B3A2A)
private val BubbleGradientEnd = Color(0xFFD4845A)
private val BubbleTextPrimary = Color.White
private val BubbleTextSecondary = Color(0xFFFFE0B2)
private val BubbleCartAccent = Color(0xFFFFD54F)

/**
 * Data class chứa thông tin đồ uống vừa thêm vào giỏ.
 */
data class CartBubbleData(
    val drinkName: String,
    val thumbnailUrl: String
)

/**
 * Singleton quản lý trạng thái bong bóng giữa các Activity.
 * DetailActivity set pending → finish() → MainActivity đọc và hiện bubble.
 */
object CartBubbleState {
    var pendingBubble: CartBubbleData? = null
        private set

    fun setPending(data: CartBubbleData) {
        pendingBubble = data
    }

    fun consume(): CartBubbleData? {
        val data = pendingBubble
        pendingBubble = null
        return data
    }
}

/**
 * Hiệu ứng bong bóng "Đã thêm vào giỏ".
 *
 * @param data Thông tin đồ uống. null = ẩn bong bóng.
 * @param onCartClicked Callback khi nhấn vào bong bóng → điều hướng giỏ hàng.
 * @param onDismiss Callback khi bong bóng tự ẩn (sau timeout hoặc khi nhấn).
 * @param autoHideMillis Thời gian tự ẩn (ms). Mặc định 5000 (5 giây).
 */
@Composable
fun AddToCartBubble(
    data: CartBubbleData?,
    onCartClicked: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    autoHideMillis: Long = 5_000L
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(data) {
        if (data != null) {
            isVisible = true
            delay(autoHideMillis)
            isVisible = false
            delay(300) // chờ animation exit
            onDismiss()
        } else {
            isVisible = false
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = isVisible && data != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            data?.let { bubbleData ->
                BubbleContent(
                    data = bubbleData,
                    onClick = {
                        isVisible = false
                        onCartClicked()
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun BubbleContent(
    data: CartBubbleData,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 84.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.3f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(BubbleGradientStart, BubbleGradientEnd)
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(color = Color.White.copy(alpha = 0.3f)),
                onClick = onClick
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail đồ uống
        AsyncImage(
            model = data.thumbnailUrl,
            contentDescription = data.drinkName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.15f))
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Text
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Đã thêm vào giỏ ✓",
                color = BubbleTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = data.drinkName,
                color = BubbleTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // "Xem giỏ" CTA
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.15f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.btn_2),
                contentDescription = "Giỏ hàng",
                tint = BubbleCartAccent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Xem giỏ",
                color = BubbleCartAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
