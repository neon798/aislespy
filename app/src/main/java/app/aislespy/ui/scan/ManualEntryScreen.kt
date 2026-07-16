package app.aislespy.ui.scan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryScreen(
    onLookup: (barcode: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var barcode by rememberSaveable { mutableStateOf("") }
    val valid = BarcodeValidation.isValid(barcode)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Enter barcode") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = barcode,
                onValueChange = { barcode = BarcodeValidation.filterDigits(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Barcode") },
                supportingText = {
                    Text("Enter the number under the barcode (8 or 12–14 digits)")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Go,
                ),
                keyboardActions = KeyboardActions(
                    onGo = {
                        if (valid) onLookup(barcode)
                    },
                ),
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onLookup(barcode) },
                enabled = valid,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Look up")
            }
            if (barcode.isNotEmpty() && !valid) {
                Text(
                    text = "Use 8 digits (EAN-8) or 12–14 digits (UPC/EAN-13)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
