package winzzed.komet.ide.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import winzzed.komet.ide.data.defaultProjectsDirectory
import winzzed.komet.ide.platform.pickDirectory
import winzzed.komet.ide.ui.theme.*

@Composable
fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, packageName: String, location: String) -> Unit
) {
    var name by remember { mutableStateOf("My Application") }
    var packageName by remember { mutableStateOf("winzzed.komet.myapplication") }
    var location by remember { mutableStateOf(defaultProjectsDirectory()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Создать новый проект", color = TextPrimary) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя проекта") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text("Имя пакета") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Расположение") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        TextButton(onClick = {
                            pickDirectory("Выберите папку для проекта", location) { picked ->
                                if (picked != null) location = picked
                            }
                        }) { Text("...") }
                    }
                )

                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = Color.Red)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    name.isBlank() -> error = "Имя проекта не может быть пустым"
                    packageName.isBlank() -> error = "Имя пакета не может быть пустым"
                    location.isBlank() -> error = "Укажите расположение"
                    else -> onCreate(name, packageName, location)
                }
            }) { Text("Создать") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}