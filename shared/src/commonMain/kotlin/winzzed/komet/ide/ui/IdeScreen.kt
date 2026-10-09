package winzzed.komet.ide.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import winzzed.komet.ide.data.ProjectStorage
import winzzed.komet.ide.data.RecentProjects
import winzzed.komet.ide.platform.pickDirectory
import winzzed.komet.ide.platform.pickSaveFile
import winzzed.komet.ide.ui.theme.*
import winzzed.komet.ide.viewmodel.IdeViewModel

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

    var menuVisible by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var currentBasePath by remember { mutableStateOf(basePath) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .padding(6.dp)
    ) {
        Column(
            modifier = Modifier
                .width(260.dp)
                .fillMaxHeight()
                .background(SurfaceDark)
                .padding(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    IconButton(
                        onClick = { menuVisible = true },
                        modifier = Modifier
                            .size(44.dp)
                            .background(SurfaceHigh)
                    ) {
                        Icon(
                            Icons.Filled.Menu,
                            contentDescription = "Меню",
                            tint = TextPrimary
                        )
                    }

                    DropdownMenu(
                        expanded = menuVisible,
                        onDismissRequest = { menuVisible = false },
                        modifier = Modifier.background(SurfaceHigh)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Create new project", color = TextPrimary) },
                            onClick = {
                                menuVisible = false
                                showCreateDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Open project", color = TextPrimary) },
                            onClick = {
                                menuVisible = false
                                pickDirectory("Открыть проект", currentBasePath) { path ->
                                    if (path != null) {
                                        currentBasePath = path
                                        RecentProjects.add(path)
                                        vm.reload(path)
                                    }
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Project Settings", color = TextPrimary) },
                            onClick = {
                                menuVisible = false
                                showSettingsDialog = true
                            }
                        )
                        HorizontalDivider(color = OutlineGray)
                        DropdownMenuItem(
                            text = { Text("Close Project", color = TextPrimary) },
                            onClick = {
                                menuVisible = false
                                onCloseProject()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Close IDE", color = TextPrimary) },
                            onClick = {
                                menuVisible = false
                                onCloseIde()
                            }
                        )
                    }
                }

                IconButton(
                    onClick = {
                        val zipPath = vm.compileToCache() ?: return@IconButton
                        pickSaveFile("Сохранить плагин", "plugin.kinet") { target ->
                            if (target != null) {
                                ProjectStorage.copyFile(zipPath, target)
                            }
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(SurfaceHigh)
                ) {
                    Icon(
                        Icons.Filled.Build,
                        contentDescription = "Собрать",
                        tint = TextPrimary
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                "Файлы",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                files.forEach { f ->
                    val isSel = selected == f
                    val bg by animateColorAsState(
                        targetValue = if (isSel) SurfaceHigh else Color.Transparent,
                        animationSpec = tween(180),
                        label = "fileBg"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bg)
                            .clickable { vm.select(f) }
                            .padding(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Text(
                            "- /${f.name}",
                            color = if (isSel) TextPrimary else TextSecondary,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(Modifier.width(6.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(SurfaceMid)
        ) {
            val sel = selected
            if (sel == null) {
                Text(
                    "Выберите файл слева",
                    color = TextSecondary,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceDark)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            sel.name,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }

                    key(sel.path) {
                        Editor(
                            fileName = sel.name,
                            text = content,
                            onChange = vm::onEdit,
                            showHeader = false
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
                createProjectOnDisk(name, pkg, location)
                currentBasePath = location
                RecentProjects.add(location)
                vm.reload(location)
                showCreateDialog = false
            }
        )
    }

    if (showSettingsDialog) {
        ProjectSettingsWindow(
            basePath = currentBasePath,
            onClose = { showSettingsDialog = false }
        )
    }
}

private fun createProjectOnDisk(name: String, packageName: String, location: String) {
    val storage = ProjectStorage(location)
    storage.ensureSkeleton()
    storage.writeManifest(
        """
        {
          "name": "$name",
          "package": "$packageName",
          "version": "1.0.0"
        }
        """.trimIndent()
    )
}
