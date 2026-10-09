package winzzed.komet.ide.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import winzzed.komet.ide.data.RecentProjects
import winzzed.komet.ide.data.fileNameOf
import winzzed.komet.ide.data.parentPathOf
import winzzed.komet.ide.data.pathExists
import winzzed.komet.ide.platform.pickDirectory
import winzzed.komet.ide.ui.theme.*

@Composable
fun StartScreen(onProjectSelected: (String) -> Unit) {
    var recentProjects by remember {
        mutableStateOf(RecentProjects.get().filter { pathExists(it) })
    }
    val isWelcomeMode = recentProjects.isEmpty()

    Surface(color = OledBlack, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                text = "Komet IDE",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(24.dp))

            if (isWelcomeMode) {
                WelcomeActions(
                    onNewProject = { chooseFolderAndOpen(onProjectSelected) },
                    onOpenProject = { chooseFolderAndOpen(onProjectSelected) }
                )
            } else {
                RecentProjectsList(
                    projects = recentProjects,
                    onOpen = { path ->
                        RecentProjects.add(path)
                        onProjectSelected(path)
                    },
                    onRemove = { path ->
                        RecentProjects.remove(path)
                        recentProjects = recentProjects.filter { it != path }
                    },
                    onNewProject = { chooseFolderAndOpen(onProjectSelected) },
                    onOpenOther = { chooseFolderAndOpen(onProjectSelected) }
                )
            }
        }
    }
}

@Composable
private fun WelcomeActions(
    onNewProject: () -> Unit,
    onOpenProject: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Добро пожаловать!",
            color = TextPrimary,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Создайте новый проект или откройте существующий",
            color = TextSecondary,
            fontSize = 16.sp
        )
        Spacer(Modifier.height(40.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            BigActionCard(
                icon = Icons.Filled.Add,
                title = "Новый проект",
                subtitle = "Создать папку с manifest.json и main.js",
                onClick = onNewProject
            )
            BigActionCard(
                icon = Icons.Filled.FolderOpen,
                title = "Открыть проект",
                subtitle = "Выбрать существующую папку",
                onClick = onOpenProject
            )
        }
    }
}

@Composable
private fun RecentProjectsList(
    projects: List<String>,
    onOpen: (String) -> Unit,
    onRemove: (String) -> Unit,
    onNewProject: () -> Unit,
    onOpenOther: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onNewProject,
            colors = ButtonDefaults.buttonColors(
                containerColor = SurfaceHigh,
                contentColor = TextPrimary
            )
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Новый проект")
        }
        Button(
            onClick = onOpenOther,
            colors = ButtonDefaults.buttonColors(
                containerColor = SurfaceHigh,
                contentColor = TextPrimary
            )
        ) {
            Icon(Icons.Filled.FolderOpen, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Открыть")
        }
    }
    Spacer(Modifier.height(24.dp))

    Text(
        text = "Недавние проекты",
        color = TextSecondary,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(12.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        projects.forEach { path ->
            RecentProjectRow(
                path = path,
                onOpen = { onOpen(path) },
                onRemove = { onRemove(path) }
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun RecentProjectRow(
    path: String,
    onOpen: () -> Unit,
    onRemove: () -> Unit
) {
    val fileName = fileNameOf(path)
    val parentPath = parentPathOf(path) ?: path

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .clickable { onOpen() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.FolderOpen,
            contentDescription = null,
            tint = TextSecondary
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileName,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = parentPath,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Убрать из недавних",
                tint = TextSecondary
            )
        }
    }
}

@Composable
private fun BigActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(200.dp)
            .background(SurfaceDark)
            .clickable { onClick() }
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = TextPrimary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = subtitle,
            color = TextSecondary,
            fontSize = 12.sp
        )
    }
}

private fun chooseFolderAndOpen(onSelected: (String) -> Unit) {
    pickDirectory("Выберите папку проекта", null) { path ->
        if (path != null) {
            RecentProjects.add(path)
            onSelected(path)
        }
    }
}
