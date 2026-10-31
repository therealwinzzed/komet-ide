package winzzed.komet.ide.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import winzzed.komet.ide.ui.theme.*

/**
 * The top chrome: a thin caption strip. The window title itself is drawn by the
 * OS window on desktop, so the strip repeats the product name and the project.
 */
@Composable
fun IdeTitleBar(
    projectName: String?,
    onOpenSettings: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .background(IdeWindow)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(IdeBlue),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "K",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (projectName != null) "$projectName — Komet IDE" else "Komet IDE",
                color = IdeText,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (onOpenSettings != null) {
                ToolbarIconButton(
                    icon = Icons.Filled.Settings,
                    contentDescription = "Настройки",
                    onClick = onOpenSettings
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(IdeBorder)
        )
    }
}

/** The main toolbar: square icon buttons separated by thin vertical rules. */
@Composable
fun IdeMainToolbar(
    onNewProject: () -> Unit,
    onOpenProject: () -> Unit,
    onBuild: (() -> Unit)?,
    onRefresh: (() -> Unit)?,
    statusLabel: String?,
    extraActions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(IdeToolWindow)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolbarIconButton(
            icon = Icons.Filled.Add,
            contentDescription = "Новый проект",
            onClick = onNewProject
        )
        ToolbarIconButton(
            icon = Icons.Filled.FolderOpen,
            contentDescription = "Открыть проект",
            onClick = onOpenProject
        )

        ToolbarSeparator()

        ToolbarIconButton(
            icon = Icons.Filled.Build,
            contentDescription = "Собрать plugin.kinet",
            onClick = { onBuild?.invoke() },
            enabled = onBuild != null
        )
        ToolbarIconButton(
            icon = Icons.Filled.PlayArrow,
            contentDescription = "Запустить",
            onClick = {},
            enabled = false,
            tint = IdeTextMuted
        )

        ToolbarSeparator()

        ToolbarIconButton(
            icon = Icons.Filled.Refresh,
            contentDescription = "Обновить",
            onClick = { onRefresh?.invoke() },
            enabled = onRefresh != null
        )

        extraActions()

        Spacer(Modifier.weight(1f))

        if (statusLabel != null) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(IdeHover)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(statusLabel, color = IdeTextDim, fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}

@Composable
fun ToolbarSeparator() {
    Box(
        modifier = Modifier
            .padding(horizontal = 6.dp)
            .width(1.dp)
            .height(18.dp)
            .background(IdeToolbarSeparator)
    )
}

@Composable
fun ToolbarIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color = IdeText
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val background by animateColorAsState(
        targetValue = when {
            !enabled -> Color.Transparent
            hovered -> IdeHover
            else -> Color.Transparent
        },
        animationSpec = tween(120),
        label = "toolbarHover"
    )

    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(background)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else IdeTextMuted,
            modifier = Modifier.size(16.dp)
        )
    }
}

/** A status-bar segment with an optional icon. */
@Composable
fun StatusBarItem(
    text: String,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    tint: Color = IdeTextDim
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(if (hovered && onClick != null) IdeHover else Color.Transparent)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(12.dp)
            )
            Spacer(Modifier.width(4.dp))
        }
        BasicText(
            text = text,
            style = IdeUiTextStyle.copy(color = tint)
        )
    }
}

/** The bottom strip: project, file count, encoding and version. */
@Composable
fun IdeStatusBar(
    left: @Composable RowScope.() -> Unit = {},
    right: @Composable RowScope.() -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(IdeBorder)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(IdeToolWindow)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            left()
            Spacer(Modifier.weight(1f))
            right()
        }
    }
}
