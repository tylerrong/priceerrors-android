package app.priceerrors.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import app.priceerrors.core.model.Deal
import app.priceerrors.core.sharing.DealSharing

/**
 * Long-press share/copy menu for feed cards — mirrors iOS `dealShareContextMenu`.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DealShareClickable(
    deal: Deal,
    onClick: () -> Unit,
    onShare: (Deal) -> Unit,
    onCopyLink: (Deal) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var menuOpen by remember(deal.id) { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val canShare = DealSharing.canShare(deal)

    Box(modifier = modifier) {
        Box(
            modifier = Modifier.combinedClickable(
                onClick = onClick,
                onLongClick = {
                    if (canShare) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuOpen = true
                    }
                },
            ),
        ) {
            content()
        }
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
        ) {
            DropdownMenuItem(
                text = { Text("Share deal") },
                onClick = {
                    menuOpen = false
                    onShare(deal)
                },
            )
            DropdownMenuItem(
                text = { Text("Copy link") },
                onClick = {
                    menuOpen = false
                    onCopyLink(deal)
                },
            )
        }
    }
}
