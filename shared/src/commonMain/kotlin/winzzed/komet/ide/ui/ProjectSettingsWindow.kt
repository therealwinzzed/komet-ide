package winzzed.komet.ide.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import winzzed.komet.ide.data.readRaw
import winzzed.komet.ide.data.writeRaw
import winzzed.komet.ide.ui.theme.*

private enum class SettingsSection(val title: String) {
    Project("Project"),
    Modules("Permissions")
}

private data class ManifestField(
    val key: String,
    val originalValue: String,
    var editValue: String
)

private val ALL_PERMISSIONS = listOf(
    "chat.write",
    "chat.edit",
    "chat.photo",
    "chat.file",
    "ui.notify",
    "contact.read",
    "message.readReply",
    "network",
    "storage"
)

private fun parseManifestFields(raw: String): List<ManifestField> {
    val fields = mutableListOf<ManifestField>()
    val flatRegex = """"([^"]+)"\s*:\s*"([^"]*)"""".toRegex()
    flatRegex.findAll(raw).forEach { m ->
        val key = m.groupValues[1]
        if (key == "permissions" || key == "commands") return@forEach
        fields += ManifestField(
            key = key,
            originalValue = m.groupValues[2],
            editValue = m.groupValues[2]
        )
    }
    return fields
}

private fun parsePermissions(raw: String): Set<String> {
    val regex = """"permissions"\s*:\s*\[([^\]]*)]""".toRegex()
    val match = regex.find(raw) ?: return emptySet()
    val itemRegex = """"([^"]+)"""".toRegex()
    return itemRegex.findAll(match.groupValues[1]).map { it.groupValues[1] }.toSet()
}

private fun replacePermissions(raw: String, permissions: Set<String>): String {
    val newArray = permissions.sorted().joinToString(", ") { "\"$it\"" }
    val regex = """"permissions"\s*:\s*\[([^\]]*)]""".toRegex()
    return regex.replace(raw) { """"permissions": [$newArray]""" }
}

private fun applyEditsToRaw(raw: String, fields: List<ManifestField>): String {
    var result = raw
    for (field in fields) {
        if (field.editValue == field.originalValue) continue

        val escapedNew = field.editValue
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")

        val pattern = """("${Regex.escape(field.key)}"\s*:\s*")([^"]*)(")""".toRegex()
        result = pattern.replaceFirst(result, "$1$escapedNew$3")
    }
    return result
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
    var selectedSection by remember { mutableStateOf(SettingsSection.Project) }

    LaunchedEffect(basePath) {
        withContext(Dispatchers.Default) {
            try {
                val t = readRaw(manifestPath)
                rawText = t
                fields = parseManifestFields(t)
                permissions = parsePermissions(t)
            } catch (_: Throwable) {
                rawText = null
            }
        }
    }

    Surface(color = SurfaceDark, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceHigh)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Project Structure",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                HorizontalDivider(color = OutlineGray)

                Row(modifier = Modifier.fillMaxSize().weight(1f)) {

                    Column(
                        modifier = Modifier
                            .width(240.dp)
                            .fillMaxHeight()
                            .background(SurfaceDark)
                            .padding(vertical = 12.dp)
                    ) {
                        SettingsSection.entries.forEach { section ->
                            val isSelected = selectedSection == section
                            val bg = if (isSelected) SurfaceHigh else Color.Transparent
                            Text(
                                text = section.title,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold
                                else FontWeight.Normal,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(bg)
                                    .clickable { selectedSection = section }
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                            )
                        }
                    }

                    VerticalDivider(color = OutlineGray)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(SurfaceMid)
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp)
                    ) {
                        when (selectedSection) {
                            SettingsSection.Project -> {
                                SectionTitle("Project")
                                Spacer(Modifier.height(16.dp))

                                if (rawText == null) {
                                    Text(
                                        "manifest.json не найден",
                                        color = Color(0xFFFF6B6B),
                                        fontSize = 13.sp
                                    )
                                } else {
                                    fields.forEachIndexed { index, field ->
                                        EditableField(
                                            label = field.key,
                                            value = field.editValue,
                                            onChange = { newValue ->
                                                val updated = fields.toMutableList()
                                                updated[index] = field.copy(editValue = newValue)
                                                fields = updated
                                            }
                                        )
                                    }
                                }
                            }
                            SettingsSection.Modules -> {
                                SectionTitle("Permissions")
                                Spacer(Modifier.height(16.dp))

                                if (rawText == null) {
                                    Text(
                                        "manifest.json не найден",
                                        color = Color(0xFFFF6B6B),
                                        fontSize = 13.sp
                                    )
                                } else {
                                    ALL_PERMISSIONS.forEach { perm ->
                                        PermissionRow(
                                            name = perm,
                                            checked = perm in permissions,
                                            onCheckedChange = { isChecked ->
                                                permissions = if (isChecked) {
                                                    permissions + perm
                                                } else {
                                                    permissions - perm
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
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
                    TextButton(onClick = onClose) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val current = rawText
                            if (current != null) {
                                var updated = applyEditsToRaw(current, fields)
                                updated = replacePermissions(updated, permissions)
                                writeRaw(manifestPath, updated)
                            }
                            onClose()
                        },
                        enabled = rawText != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = OledBlack
                        )
                    ) { Text("OK") }
                }
            }
        }
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
            textStyle = androidx.compose.ui.text.TextStyle(
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
            .padding(vertical = 4.dp),
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
            fontFamily = FontFamily.Monospace
        )
    }
}