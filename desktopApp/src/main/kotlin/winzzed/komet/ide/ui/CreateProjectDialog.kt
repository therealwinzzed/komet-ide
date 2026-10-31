package winzzed.komet.ide.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import winzzed.komet.ide.data.defaultProjectsDirectory
import winzzed.komet.ide.data.sanitizeFolderName
import winzzed.komet.ide.ui.theme.*

/**
 * IntelliJ's "New Project" dialog: a left rail of project generators, a Name
 * field and a Location field with a browse suffix.
 *
 * It renders as an in-window overlay rather than an OS dialog so it looks the
 * same on every platform, including Android where there are no OS dialogs.
 */
@Composable
fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, packageName: String, location: String) -> Unit
) {
    var name by remember { mutableStateOf("My Application") }
    var packageName by remember { mutableStateOf("winzzed.komet.myapplication") }
    var parentLocation by remember { mutableStateOf(defaultProjectsDirectory()) }
    var error by remember { mutableStateOf<String?>(null) }

    // The project becomes a sub-folder of the chosen location, so show the result.
    val projectPath = "${parentLocation.trimEnd('/', '\\')}/${sanitizeFolderName(name)}"

    val chooser = rememberDirectoryChooser(fallbackRoot = defaultProjectsDirectory()) { picked ->
        if (picked != null) parentLocation = picked
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(modifier = Modifier.padding(24.dp)) {
            val compact = maxWidth < 560.dp
            val dialogWidth = if (compact) maxWidth else 660.dp
            val dialogHeight = if (compact) (maxHeight - 32.dp).coerceAtMost(540.dp) else 430.dp

            Column(
                modifier = Modifier
                    .width(dialogWidth)
                    .height(dialogHeight)
                    .clip(RoundedCornerShape(10.dp))
                    .background(IdePopup)
                    .border(1.dp, IdeBorderStrong, RoundedCornerShape(10.dp))
                    // Swallow clicks so they do not reach the scrim behind.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .background(IdeWindow)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "New Project",
                        color = IdeText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(IdeBorder))

                if (compact) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp)
                    ) {
                        NewProjectForm(
                            name = name,
                            onNameChange = {
                                name = it
                                error = null
                            },
                            packageName = packageName,
                            onPackageChange = {
                                packageName = it
                                error = null
                            },
                            parentLocation = parentLocation,
                            onParentChange = {
                                parentLocation = it
                                error = null
                            },
                            projectPath = projectPath,
                            error = error,
                            onBrowse = {
                                chooser.show("Выберите папку для проекта", parentLocation)
                            }
                        )
                    }
                } else {
                    Row(modifier = Modifier.weight(1f)) {
                        GeneratorRail()
                        Box(Modifier.width(1.dp).fillMaxHeight().background(IdeBorder))
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            NewProjectForm(
                                name = name,
                                onNameChange = {
                                    name = it
                                    error = null
                                },
                                packageName = packageName,
                                onPackageChange = {
                                    packageName = it
                                    error = null
                                },
                                parentLocation = parentLocation,
                                onParentChange = {
                                    parentLocation = it
                                    error = null
                                },
                                projectPath = projectPath,
                                error = error,
                                onBrowse = {
                                    chooser.show("Выберите папку для проекта", parentLocation)
                                }
                            )
                        }
                    }
                }

                Box(Modifier.fillMaxWidth().height(1.dp).background(IdeBorder))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = IdeTextDim, fontSize = 13.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    IdeDialogButton(
                        text = "Create",
                        primary = true,
                        onClick = {
                            when {
                                name.isBlank() -> error = "Имя проекта не может быть пустым"
                                packageName.isBlank() -> error = "Имя пакета не может быть пустым"
                                parentLocation.isBlank() -> error = "Укажите расположение"
                                else -> onCreate(name, packageName, parentLocation)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GeneratorRail() {
    val items = listOf("Komet Plugin", "Empty Project", "Import Project")
    Column(
        modifier = Modifier
            .width(190.dp)
            .fillMaxHeight()
            .background(IdeWindow)
            .padding(vertical = 12.dp)
    ) {
        items.forEachIndexed { index, title ->
            val selected = index == 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (selected) IdeHover else Color.Transparent)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    color = if (selected) IdeText else IdeTextDim,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun NewProjectForm(
    name: String,
    onNameChange: (String) -> Unit,
    packageName: String,
    onPackageChange: (String) -> Unit,
    parentLocation: String,
    onParentChange: (String) -> Unit,
    projectPath: String,
    error: String?,
    onBrowse: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IdeFieldLabel("Name", Modifier.width(110.dp))
            IdeTextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            IdeFieldLabel("Package", Modifier.width(110.dp))
            IdeTextField(
                value = packageName,
                onValueChange = onPackageChange,
                modifier = Modifier.weight(1f),
                monospace = true
            )
        }
        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            IdeFieldLabel("Location", Modifier.width(110.dp))
            IdeTextField(
                value = parentLocation,
                onValueChange = onParentChange,
                modifier = Modifier.weight(1f),
                monospace = true,
                trailing = {
                    Box(
                        modifier = Modifier
                            .padding(end = 2.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(IdeHover)
                            .clickable(onClick = onBrowse)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("…", color = IdeText, fontSize = 13.sp)
                    }
                }
            )
        }

        Spacer(Modifier.height(14.dp))
        Text("Проект будет создан в", color = IdeTextMuted, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            projectPath,
            color = IdeTextDim,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        error?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = IdeRed, fontSize = 11.sp)
        }
    }
}

@Composable
private fun IdeFieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = IdeTextDim,
        fontSize = 12.sp,
        modifier = modifier
    )
}

@Composable
fun IdeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    monospace: Boolean = false,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .height(30.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(IdeWindow)
            .border(1.dp, IdeBorder, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = IdeText,
                    fontSize = 12.sp,
                    fontFamily = if (monospace) FontFamily.Monospace else FontFamily.SansSerif
                ),
                cursorBrush = SolidColor(IdeText),
                modifier = Modifier.fillMaxWidth()
            )
        }
        trailing?.invoke()
    }
}

/** IntelliJ-styled button: flat for secondary, filled blue for primary. */
@Composable
fun IdeDialogButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    val background by animateColorAsState(
        targetValue = when {
            !enabled -> if (primary) IdeBluePressed.copy(alpha = 0.4f) else IdeHover
            primary && hovered -> IdeBlueHover
            primary -> IdeBlue
            hovered -> IdeBorderStrong
            else -> IdeHover
        },
        animationSpec = tween(120),
        label = "dialogButton"
    )

    Box(
        modifier = modifier
            .height(30.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(background)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (enabled) IdeText else IdeTextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
