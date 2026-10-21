package com.lk.biometricrd.core.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val DefaultAutoDismiss = 2.seconds
private const val ExitAnimationMillis = 250L

private val SuccessGreen = Color(0xFF2E7D32)

enum class BottomAlertType {
    Error,
    Success,
}

@Composable
fun BottomErrorAlert(
    title: String,
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    type: BottomAlertType = BottomAlertType.Error,
) {
    var isVisible by remember { mutableStateOf(false) }
    var displayedMessage by remember { mutableStateOf<String?>(null) }
    var displayedType by remember { mutableStateOf(BottomAlertType.Error) }
    val scope = rememberCoroutineScope()

    suspend fun hideAndNotify() {
        if (!isVisible) return
        isVisible = false
        delay(ExitAnimationMillis)
        displayedMessage = null
        onDismiss()
    }

    LaunchedEffect(message, type) {
        if (!message.isNullOrBlank()) {
            displayedMessage = message
            displayedType = type
            isVisible = true
            delay(DefaultAutoDismiss)
            hideAndNotify()
        }
    }

    val isSuccess = displayedType == BottomAlertType.Success
    val containerColor = if (isSuccess) SuccessGreen else MaterialTheme.colorScheme.error
    val contentColor = if (isSuccess) Color.White else MaterialTheme.colorScheme.onError
    val density = LocalDensity.current
    val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0

    AnimatedVisibility(
        visible = isVisible,
        modifier = modifier,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(durationMillis = 550),
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(durationMillis = ExitAnimationMillis.toInt()),
        ) + fadeOut(animationSpec = tween(200)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .then(if (isKeyboardOpen) Modifier.imePadding() else Modifier)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
                .wrapContentHeight(align = Alignment.Bottom),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = containerColor),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            ) {
                Row(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isSuccess) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    } else {
                        IconButton(onClick = { scope.launch { hideAndNotify() } }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(30.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = contentColor,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = displayedMessage.orEmpty(),
                            color = contentColor,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    if (isSuccess) {
                        IconButton(onClick = { scope.launch { hideAndNotify() } }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
