package winzzed.komet.ide.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import winzzed.komet.ide.data.CURRENT_VERSION
import winzzed.komet.ide.data.ProjectFile
import winzzed.komet.ide.data.RecentProjects
import winzzed.komet.ide.data.createProjectOnDisk
import winzzed.komet.ide.data.fileNameOf
import winzzed.komet.ide.platform.DownloadResult
import winzzed.komet.ide.platform.PlatformBackHandler
import winzzed.komet.ide.platform.downloadsFolderLabel
import winzzed.komet.ide.ui.theme.*
import winzzed.komet.ide.viewmodel.IdeViewModel

/** Width below which the project tool window becomes a slide-over pane. */
private val COMPACT_BREAKPOINT = 700.dp
private val TOOL_WINDOW_WIDTH = 260.dp

@Composable
fun IdeScreen(
    basePath: String,
    onCloseProject: () -> Unit,
    onCloseIde: () -> Unit
) {
    val vm = remember { IdeViewModel(basePath) }
    DisposableEffect(Unit) { onDispose { vm.close() } }

    val files by vm.files.collectAsState()
    val selected by vm.selected.collectAsState()
    val content by vm.content.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var currentBasePath by remember { mutableStateOf(basePath) }
    var message by remember { mutableStateOf<String?>(null) }
    var building by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val projectName = fileNameOf(currentBasePath)

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(IdeWindow)) {
        val compact = maxWidth < COMPACT_BREAKPOINT
        // On narrow windows the tool window is a slide-over, so it starts hidden.
        var toolWindowOpen by remember(compact, currentBasePath) { mutableStateOf(!compact) }

        PlatformBackHandler(enabled = true) {
            when {
                showSettings -> showSettings = false
                showCreateDialog -> showCreateDialog = false
                compact && toolWindowOpen -> toolWindowOpen = false
                else -> onCloseProject()
            }
        }

        val chooser = rememberDirectoryChooser(fallbackRoot = projectsRootDirectory()) { path ->
            if (path != null) {
                currentBasePath = path
                RecentProjects.add(path)
                vm.reload(path)
            }
        }

        val build: () -> Unit = {
            if (!building) {
                building = true
                scope.launch {
                    val result = vm.compilePlugin()
                    building = false
                    message = buildReport(result)
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            IdeTitleBar(
                projectName = projectName,
                onOpenSettings = { showSettings = true }
            )

            IdeMainToolbar(
                onNewProject = { showCreateDialog = true },
                onOpenProject = { chooser.show("Открыть проект", currentBasePath) },
                onBuild = build,
                onRefresh = { vm.refresh() },
                statusLabel = if (building) "Сборка…" else null
            ) {
                if (compact) {
                    ToolbarIconButton(
                        icon = Icons.Filled.FolderOpen,
                        contentDescription = "Файлы проекта",
                        onClick = { toolWindowOpen = !toolWindowOpen },
                        tint = if (toolWindowOpen) IdeBlue else IdeText
                    )
                }
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(IdeBorder))

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (!compact) {
                        ProjectToolWindow(
                            projectName = projectName,
                            files = files,
                            selectedPath = selected?.path,
                            onSelect = { vm.select(it) },
                            modifier = Modifier.width(TOOL_WINDOW_WIDTH).fillMaxHeight()
                        )
                        Box(Modifier.width(1.dp).fillMaxHeight().background(IdeBorder))
                    }

                    EditorArea(
                        files = files,
                        selected = selected,
                        content = content,
                        onSelect = { vm.select(it) },
                        onChange = vm::onEdit,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }

                if (compact && toolWindowOpen) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x66000000))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { toolWindowOpen = false }
                            )
                    )
                    ProjectToolWindow(
                        projectName = projectName,
                        files = files,
                        selectedPath = selected?.path,
                        onSelect = {
                            vm.select(it)
                            toolWindowOpen = false
                        },
                        onClose = { toolWindowOpen = false },
                        modifier = Modifier.width(TOOL_WINDOW_WIDTH).fillMaxHeight()
                    )
                }
            }

            IdeStatusBar(
                left = {
                    StatusBarItem(projectName, Icons.Filled.Folder)
                    StatusBarItem(
                        if (files.isEmpty()) "0 files" else "${files.size} files",
                        Icons.Filled.Description
                    )
                },
                right = {
                    StatusBarItem("UTF-8")
                    selected?.let { StatusBarItem(it.name, Icons.Filled.Code) }
                    StatusBarItem("Komet IDE $CURRENT_VERSION")
                }
            )
        }
    }

    if (showCreateDialog) {
        CreateProjectDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, pkg, location ->
                val created = createProjectOnDisk(name, pkg, location)
                currentBasePath = created
                RecentProjects.add(created)
                vm.reload(created)
                showCreateDialog = false
            }
        )
    }

    if (showSettings) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0x99000000))) {
            ProjectSettingsWindow(
                basePath = currentBasePath,
                onClose = { showSettings = false }
            )
        }
    }

    message?.let { text ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(IdePopup)
                    .border(1.dp, IdeBorderStrong, RoundedCornerShape(10.dp))
                    .padding(20.dp)
            ) {
                Text(
                    "Сборка завершена",
                    color = IdeText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text,
                    color = IdeTextDim,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IdeDialogButton("OK", primary = true, onClick = { message = null })
                }
            }
        }
    }
}

/** Turns a download outcome into the text shown in the result dialog. */
private fun buildReport(result: DownloadResult): String = when {
    !result.ok -> result.message ?: "Не удалось сохранить архив"
    result.location == null -> "Файл сохранён в $downloadsFolderLabel"
    else -> buildString {
        append("Файл сохранён:\n")
        append(result.location)
        result.message?.let { append("\n\n").append(it) }
    }
}

/**
 * The Project tool window: a title strip, then the project root node with its
 * files indented beneath it, as in the real IDE.
 */
@Composable
private fun ProjectToolWindow(
    projectName: String,
    files: List<ProjectFile>,
    selectedPath: String?,
    onSelect: (ProjectFile) -> Unit,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null
) {
    Column(modifier = modifier.background(IdeToolWindow)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .padding(start = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Project",
                color = IdeText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (onClose != null) {
                ToolbarIconButton(
                    icon = Icons.Filled.Close,
                    contentDescription = "Закрыть",
                    onClick = onClose
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = IdeTextDim,
                    modifier = Modifier.size(16.dp)
                )
                Icon(
                    Icons.Filled.Folder,
                    contentDescription = null,
                    tint = IdeBlue,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    projectName,
                    color = IdeText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            files.forEach { file ->
                ProjectFileRow(
                    file = file,
                    selected = selectedPath == file.path,
                    onClick = { onSelect(file) }
                )
            }

            if (files.isEmpty()) {
                Text(
                    "Пустой проект",
                    color = IdeTextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 34.dp, top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ProjectFileRow(
    file: ProjectFile,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    val background by animateColorAsState(
        targetValue = when {
            selected -> IdeSelection
            hovered -> IdeHover
            else -> Color.Transparent
        },
        animationSpec = tween(120),
        label = "fileRow"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(background)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(start = 26.dp, end = 8.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            fileTypeIcon(file.name),
            contentDescription = null,
            tint = fileTypeColor(file.name),
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            file.name,
            color = IdeText,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun fileTypeIcon(name: String) = when {
    name.endsWith(".json") -> Icons.Filled.DataObject
    name.endsWith(".js") || name.endsWith(".ts") -> Icons.Filled.Code
    name.endsWith(".md") -> Icons.Filled.Description
    else -> Icons.Filled.InsertDriveFile
}

private fun fileTypeColor(name: String): Color = when {
    name.endsWith(".json") -> IdeYellow
    name.endsWith(".js") || name.endsWith(".ts") -> IdeOrange
    else -> IdeTextDim
}

/** The editor area: a tab strip on top, the active editor below. */
@Composable
private fun EditorArea(
    files: List<ProjectFile>,
    selected: ProjectFile?,
    content: String,
    onSelect: (ProjectFile) -> Unit,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.background(IdeEditor)) {
        EditorTabStrip(files = files, selected = selected, onSelect = onSelect)
        Box(Modifier.fillMaxWidth().height(1.dp).background(IdeBorder))

        if (selected == null) {
            EmptyEditor()
        } else {
            key(selected.path) {
                Editor(
                    fileName = selected.name,
                    text = content,
                    onChange = onChange,
                    showHeader = false
                )
            }
        }
    }
}

@Composable
private fun EditorTabStrip(
    files: List<ProjectFile>,
    selected: ProjectFile?,
    onSelect: (ProjectFile) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(IdeWindow)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (files.isEmpty()) {
            Spacer(Modifier.width(8.dp))
            Text("No files open", color = IdeTextMuted, fontSize = 11.sp)
        }

        files.forEach { file ->
            EditorTab(
                file = file,
                selected = selected?.path == file.path,
                onClick = { onSelect(file) }
            )
        }
    }
}

@Composable
private fun EditorTab(
    file: ProjectFile,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Row(
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(min = 90.dp, max = 200.dp)
            .background(if (selected) IdeEditor else if (hovered) IdeHover else Color.Transparent)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            fileTypeIcon(file.name),
            contentDescription = null,
            tint = fileTypeColor(file.name),
            modifier = Modifier.size(13.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            file.name,
            color = if (selected) IdeText else IdeTextDim,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }

    if (selected) {
        Box(Modifier.width(1.dp).fillMaxHeight().background(IdeBorder))
    }
}

@Composable
private fun EmptyEditor() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IdeEditor),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.Code,
                contentDescription = null,
                tint = IdeTextMuted,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text("Выберите файл в проекте", color = IdeTextDim, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text("или создайте новый проект", color = IdeTextMuted, fontSize = 11.sp)
        }
    }
}
