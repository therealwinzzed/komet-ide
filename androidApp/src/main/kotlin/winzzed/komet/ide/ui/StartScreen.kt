package winzzed.komet.ide.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import winzzed.komet.ide.data.CURRENT_VERSION
import winzzed.komet.ide.data.RecentProjects
import winzzed.komet.ide.data.createProjectOnDisk
import winzzed.komet.ide.data.fileNameOf
import winzzed.komet.ide.data.parentPathOf
import winzzed.komet.ide.data.pathExists
import winzzed.komet.ide.ui.theme.*

/** Below this width the two welcome columns stack. */
private val COMPACT_BREAKPOINT = 640.dp

private data class WelcomeAction(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val isPrimary: Boolean = false
)

private val WELCOME_ACTIONS = listOf(
    WelcomeAction("Новый проект", "Создать проект для Komet", Icons.Filled.Add, isPrimary = true),
    WelcomeAction("Открыть", "Открыть проект с диска", Icons.Filled.FolderOpen)
)

/**
 * The IntelliJ "Welcome to Komet IDE" screen.
 *
 * Left column: primary actions. Right column: the projects list. Narrow windows
 * stack the columns and collapse the actions into two buttons.
 */
@Composable
fun StartScreen(onProjectSelected: (String) -> Unit) {
    var recentProjects by remember {
        mutableStateOf(RecentProjects.get().filter { pathExists(it) })
    }
    var showCreateDialog by remember { mutableStateOf(false) }

    val chooser = rememberDirectoryChooser(fallbackRoot = projectsRootDirectory()) { path ->
        if (path != null) {
            RecentProjects.add(path)
            onProjectSelected(path)
        }
    }

    val openProject = {
        chooser.show("Выберите папку проекта", projectsRootDirectory())
    }

    val openExisting: (String) -> Unit = { path ->
        RecentProjects.add(path)
        onProjectSelected(path)
    }

    Column(modifier = Modifier.fillMaxSize().background(IdeWindow)) {
        IdeTitleBar(projectName = null)

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val compact = maxWidth < COMPACT_BREAKPOINT

            if (compact) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    WelcomeBranding()
                    Spacer(Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        WelcomeQuickButton(
                            "Новый проект",
                            { showCreateDialog = true },
                            primary = true,
                            modifier = Modifier.weight(1f)
                        )
                        WelcomeQuickButton(
                            "Открыть",
                            openProject,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    ProjectsList(
                        projects = recentProjects,
                        onOpen = openExisting,
                        onRemove = { path ->
                            RecentProjects.remove(path)
                            recentProjects = recentProjects.filter { it != path }
                        }
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 28.dp, vertical = 24.dp)
                ) {
                    Column(modifier = Modifier.width(290.dp).fillMaxHeight()) {
                        WelcomeBranding()
                        Spacer(Modifier.height(26.dp))
                        WELCOME_ACTIONS.forEach { action ->
                            WelcomeRow(
                                action = action,
                                onClick = if (action.isPrimary) {
                                    { showCreateDialog = true }
                                } else {
                                    openProject
                                }
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        BasicText(
                            text = "Среда разработки плагинов Komet",
                            style = IdeUiTextStyle.copy(
                                color = IdeTextMuted,
                                fontSize = 11.sp
                            )
                        )
                    }

                    Spacer(Modifier.width(28.dp))
                    Box(Modifier.width(1.dp).fillMaxHeight().background(IdeBorder))
                    Spacer(Modifier.width(28.dp))

                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        ProjectsList(
                            projects = recentProjects,
                            onOpen = openExisting,
                            onRemove = { path ->
                                RecentProjects.remove(path)
                                recentProjects = recentProjects.filter { it != path }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateProjectDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, pkg, location ->
                val created = createProjectOnDisk(name, pkg, location)
                RecentProjects.add(created)
                showCreateDialog = false
                onProjectSelected(created)
            }
        )
    }
}

@Composable
private fun WelcomeBranding() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(IdeBlue),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "K",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                "Komet IDE",
                color = IdeText,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Версия $CURRENT_VERSION",
                color = IdeTextMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun WelcomeRow(
    action: WelcomeAction,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val background by animateColorAsState(
        targetValue = when {
            action.isPrimary && hovered -> IdeBlueHover
            action.isPrimary -> IdeBlue
            hovered -> IdeHover
            else -> Color.Transparent
        },
        animationSpec = tween(120),
        label = "welcomeRow"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            action.icon,
            contentDescription = null,
            tint = if (action.isPrimary) Color.White else IdeBlue,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                action.title,
                color = if (action.isPrimary) Color.White else IdeText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                action.subtitle,
                color = if (action.isPrimary) Color(0xFFD6E4FF) else IdeTextMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WelcomeQuickButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean = false,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                when {
                    primary && hovered -> IdeBlueHover
                    primary -> IdeBlue
                    hovered -> IdeHover
                    else -> IdeToolWindow
                }
            )
            .then(
                if (!primary) Modifier.border(1.dp, IdeBorder, RoundedCornerShape(6.dp))
                else Modifier
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (primary) Color.White else IdeText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ProjectsList(
    projects: List<String>,
    onOpen: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Проекты",
            color = IdeText,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.weight(1f))
        if (projects.isNotEmpty()) {
            Text("Недавние", color = IdeTextMuted, fontSize = 11.sp)
        }
    }
    Spacer(Modifier.height(10.dp))

    if (projects.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(IdeToolWindow)
                .border(1.dp, IdeBorder, RoundedCornerShape(8.dp))
                .padding(vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.FolderOpen,
                    contentDescription = null,
                    tint = IdeTextMuted,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.height(10.dp))
                Text("Нет недавних проектов", color = IdeTextDim, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Создайте новый проект или откройте существующий",
                    color = IdeTextMuted,
                    fontSize = 11.sp
                )
            }
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        projects.forEach { path ->
            ProjectListRow(
                path = path,
                onOpen = { onOpen(path) },
                onRemove = { onRemove(path) }
            )
        }
    }
}

@Composable
private fun ProjectListRow(
    path: String,
    onOpen: () -> Unit,
    onRemove: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (hovered) IdeHover else Color.Transparent)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onOpen
            )
            .padding(start = 10.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Folder,
            contentDescription = null,
            tint = IdeBlue,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                fileNameOf(path),
                color = IdeText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                parentPathOf(path) ?: path,
                color = IdeTextMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (hovered) {
            IconButton(onClick = onRemove, modifier = Modifier.size(26.dp)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Убрать из списка",
                    tint = IdeTextMuted,
                    modifier = Modifier.size(14.dp)
                )
            }
        } else {
            Spacer(Modifier.width(26.dp))
        }
    }
}
