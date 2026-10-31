package winzzed.komet.ide.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import winzzed.komet.ide.data.ALL_PERMISSIONS
import winzzed.komet.ide.data.ManifestField
import winzzed.komet.ide.data.applyEditsToRaw
import winzzed.komet.ide.data.parseManifestFields
import winzzed.komet.ide.data.parsePermissions
import winzzed.komet.ide.data.readRaw
import winzzed.komet.ide.data.replacePermissions
import winzzed.komet.ide.data.writeRaw
import winzzed.komet.ide.ui.theme.*

/** Width below which the sidebar is replaced by an inline section switcher. */
private val COMPACT_BREAKPOINT = 600.dp

private enum class SettingsSection(val title: String) {
    Project("Project"),
    Permissions("Permissions")
}

@Composable
fun ProjectSettingsWindow(
    basePath: String,
    onClose: () -> Unit
) {
    val manifestPath = "$basePath/manifest.json"
    var rawText by remember { mutableStateOf<String?>(null) }
    var fields by remember { mutableStateOf<List<ManifestField>>(emptyList()) }
    var permissions by remember { mutableStateOf<Set<String>>(emptySet()) }
    // Permissions present in the manifest but not in the built-in list; kept on save.
    var extraPermissions by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedSection by remember { mutableStateOf(SettingsSection.Project) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(basePath) {
        // manifest.json is a few hundred bytes, so read it directly rather than
        // hopping to a background dispatcher. The hop made the first frame depend on
        // a cross-thread resume, which is slower to settle and does not complete
        // under a controlled Compose clock.
        try {
            val t = readRaw(manifestPath)
            rawText = t
            fields = parseManifestFields(t)
            val parsed = parsePermissions(t)
            permissions = parsed
            extraPermissions = parsed.filterNot { it in ALL_PERMISSIONS }.sorted()
        } catch (_: Throwable) {
            rawText = null
        }
    }

    val save = {
        val current = rawText
        if (current == null || saving) {
            onClose()
        } else {
            saving = true
            scope.launch {
                val failure = try {
                    var updated = applyEditsToRaw(current, fields)
                    updated = replacePermissions(updated, permissions)
                    writeRaw(manifestPath, updated)
                    null
                } catch (t: Throwable) {
                    t.message ?: "не удалось записать manifest.json"
                }
                saving = false
                if (failure == null) onClose() else error = failure
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(SurfaceDark)) {
        val compact = maxWidth < COMPACT_BREAKPOINT

        Column(modifier = Modifier.fillMaxSize()) {
            // ---- Header -------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceHigh)
                    .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Project Structure",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose, enabled = !saving) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Закрыть",
                        tint = TextSecondary
                    )
                }
            }
            HorizontalDivider(color = OutlineGray)

            if (compact) {
                // ---- Phone layout: sections as an inline switcher -------------
                SectionSwitcher(
                    selected = selectedSection,
                    onSelect = { selectedSection = it }
                )
                HorizontalDivider(color = OutlineGray)

                SettingsContent(
                    section = selectedSection,
                    rawText = rawText,
                    fields = fields,
                    permissions = permissions,
                    extraPermissions = extraPermissions,
                    onFieldChange = { index, value ->
                        fields = fields.toMutableList().also { it[index] = it[index].copy(editValue = value) }
                    },
                    onPermissionChange = { perm, checked ->
                        permissions = if (checked) permissions + perm else permissions - perm
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                // ---- Wide layout: sidebar + content ---------------------------
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .width(240.dp)
                            .fillMaxHeight()
                            .background(SurfaceDark)
                            .padding(vertical = 12.dp)
                    ) {
                        SettingsSection.entries.forEach { section ->
                            val isSelected = selectedSection == section
                            Text(
                                text = section.title,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold
                                else FontWeight.Normal,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSelected) SurfaceHigh else Color.Transparent)
                                    .clickable { selectedSection = section }
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                            )
                        }
                    }

                    VerticalDivider(color = OutlineGray)

                    // weight(1f), not fillMaxSize(): a fixed 240.dp sibling plus a
                    // full-width child overflows and pushes the content off-screen.
                    // weight(1f) states the intent: share the row with the sidebar.
                    SettingsContent(
                        section = selectedSection,
                        rawText = rawText,
                        fields = fields,
                        permissions = permissions,
                        extraPermissions = extraPermissions,
                        onFieldChange = { index, value ->
                            fields = fields.toMutableList().also { it[index] = it[index].copy(editValue = value) }
                        },
                        onPermissionChange = { perm, checked ->
                            permissions = if (checked) permissions + perm else permissions - perm
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ---- Actions ------------------------------------------------------
            error?.let { message ->
                HorizontalDivider(color = OutlineGray)
                Text(
                    text = "Ошибка: $message",
                    color = Color(0xFFFF6B6B),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            HorizontalDivider(color = OutlineGray)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onClose, enabled = !saving) {
                    Text("Cancel", color = TextSecondary)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { save() },
                    enabled = rawText != null && !saving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = OledBlack
                    )
                ) { Text(if (saving) "..." else "OK") }
            }
        }
    }
}

@Composable
private fun SectionSwitcher(
    selected: SettingsSection,
    onSelect: (SettingsSection) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SettingsSection.entries.forEach { section ->
            val isSelected = selected == section
            Text(
                text = section.title,
                color = if (isSelected) TextPrimary else TextSecondary,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier
                    .weight(1f)
                    .background(if (isSelected) SurfaceHigh else Color.Transparent)
                    .clickable { onSelect(section) }
                    .padding(vertical = 10.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun SettingsContent(
    section: SettingsSection,
    rawText: String?,
    fields: List<ManifestField>,
    permissions: Set<String>,
    extraPermissions: List<String>,
    onFieldChange: (Int, String) -> Unit,
    onPermissionChange: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceMid)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        when (section) {
            SettingsSection.Project -> {
                SectionTitle("Project")
                Spacer(Modifier.height(16.dp))

                if (rawText == null) {
                    MissingManifest()
                } else if (fields.isEmpty()) {
                    Text(
                        "В manifest.json нет редактируемых строковых полей",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                } else {
                    fields.forEachIndexed { index, field ->
                        EditableField(
                            label = field.key,
                            value = field.editValue,
                            onChange = { onFieldChange(index, it) }
                        )
                    }
                }
            }

            SettingsSection.Permissions -> {
                SectionTitle("Permissions")
                Spacer(Modifier.height(16.dp))

                if (rawText == null) {
                    MissingManifest()
                } else {
                    ALL_PERMISSIONS.forEach { perm ->
                        PermissionRow(
                            name = perm,
                            checked = perm in permissions,
                            onCheckedChange = { onPermissionChange(perm, it) }
                        )
                    }
                    // Permissions the manifest already had beyond the presets.
                    // Listed from a stable snapshot so unchecking does not make the
                    // row vanish, and saving never silently drops a custom entry.
                    if (extraPermissions.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Свои разрешения",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        extraPermissions.forEach { perm ->
                            PermissionRow(
                                name = perm,
                                checked = perm in permissions,
                                onCheckedChange = { onPermissionChange(perm, it) }
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MissingManifest() {
    Text(
        "manifest.json не найден",
        color = Color(0xFFFF6B6B),
        fontSize = 13.sp
    )
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        color = TextPrimary,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold
    )
    HorizontalDivider(color = OutlineGray, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun EditableField(
    label: String,
    value: String,
    onChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(
                color = TextPrimary,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OutlineGray,
                unfocusedBorderColor = OutlineGray,
                focusedContainerColor = SurfaceHigh,
                unfocusedContainerColor = SurfaceHigh,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = TextPrimary
            )
        )
    }
}

@Composable
private fun PermissionRow(
    name: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = TextSecondary,
                checkmarkColor = OledBlack
            )
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = name,
            color = TextPrimary,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
    }
}
