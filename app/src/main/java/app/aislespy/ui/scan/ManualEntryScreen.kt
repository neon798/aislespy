package app.aislespy.ui.scan

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.aislespy.ui.components.AislePrimaryButton
import app.aislespy.ui.components.BackLink
import app.aislespy.ui.components.SectionHeader
import app.aislespy.ui.theme.AisleSpyShapes
import app.aislespy.ui.theme.CardBorder
import app.aislespy.ui.theme.CardWhite
import app.aislespy.ui.theme.CreamSurface
import app.aislespy.ui.theme.ErrorRed
import app.aislespy.ui.theme.IbmPlexMono
import app.aislespy.ui.theme.Ink
import app.aislespy.ui.theme.MutedText45
import app.aislespy.ui.theme.MutedText55
import app.aislespy.ui.theme.MutedText60
import app.aislespy.ui.theme.Olive
import app.aislespy.ui.theme.PublicSans
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box

private data class SampleCode(val code: String, val hint: String)

private val SampleCodes = listOf(
    SampleCode("7612345001234", "granola bar"),
    SampleCode("8712100849084", "instant ramen"),
    SampleCode("5010029220117", "ambiguous match"),
    SampleCode("0481516234200", "unknown code"),
)

private const val LENGTH_ERROR = "Barcodes are 8–13 digits — keep typing."

@Composable
fun ManualEntryScreen(
    onLookup: (barcode: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var barcode by rememberSaveable { mutableStateOf("") }
    var focused by rememberSaveable { mutableStateOf(false) }
    var submittedEmpty by rememberSaveable { mutableStateOf(false) }
    val valid = BarcodeValidation.isValid(barcode)
    val showError = BarcodeValidation.showLengthError(barcode) ||
        (submittedEmpty && barcode.length < BarcodeValidation.MIN_LENGTH)

    Scaffold(
        modifier = modifier,
        containerColor = CreamSurface,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            BackLink(
                onClick = onBack,
                modifier = Modifier.padding(start = 8.dp, top = 8.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "Type the barcode",
                    style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                    color = Ink,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = "For when the camera can't read it — enter the EAN or UPC digits from under the bars.",
                    fontFamily = PublicSans,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = MutedText60,
                )

                val borderColor = when {
                    focused -> Olive
                    else -> CardBorder.copy(alpha = 0.5f).let {
                        // ~ rgba(80,60,30,.3)
                        androidx.compose.ui.graphics.Color(0x4D503C1E)
                    }
                }
                BasicTextField(
                    value = barcode,
                    onValueChange = {
                        barcode = BarcodeValidation.filterDigits(it)
                        submittedEmpty = false
                    },
                    singleLine = true,
                    cursorBrush = SolidColor(Olive),
                    textStyle = TextStyle(
                        fontFamily = IbmPlexMono,
                        fontSize = 16.sp,
                        letterSpacing = 0.1.em,
                        color = Ink,
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Go,
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            if (valid) {
                                onLookup(barcode)
                            } else {
                                submittedEmpty = true
                            }
                        },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focused = it.isFocused }
                        .semantics { contentDescription = "Barcode input" },
                    decorationBox = { inner ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardWhite, AisleSpyShapes.input)
                                .border(1.5.dp, borderColor, AisleSpyShapes.input)
                                .padding(horizontal = 16.dp, vertical = 15.dp),
                        ) {
                            if (barcode.isEmpty()) {
                                Text(
                                    text = "e.g. 7612345001234",
                                    fontFamily = IbmPlexMono,
                                    fontSize = 16.sp,
                                    letterSpacing = 0.1.em,
                                    color = MutedText45,
                                )
                            }
                            inner()
                        }
                    },
                )

                if (showError) {
                    Text(
                        text = LENGTH_ERROR,
                        fontFamily = PublicSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = ErrorRed,
                    )
                }

                AislePrimaryButton(
                    text = "Look it up",
                    onClick = {
                        if (valid) {
                            onLookup(barcode)
                        } else {
                            submittedEmpty = true
                        }
                    },
                    enabled = true,
                    modifier = Modifier.semantics {
                        contentDescription = "Look it up"
                    },
                )

                Spacer(Modifier.height(6.dp))
                SectionHeader(text = "Try one of these")

                SampleCodes.forEach { sample ->
                    SampleCodeRow(
                        code = sample.code,
                        hint = sample.hint,
                        onClick = {
                            barcode = sample.code
                            submittedEmpty = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SampleCodeRow(
    code: String,
    hint: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardWhite, AisleSpyShapes.smallTile)
            .border(1.dp, CardBorder, AisleSpyShapes.smallTile)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp)
            .semantics { contentDescription = "Sample barcode $code, $hint" },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = code,
            fontFamily = IbmPlexMono,
            fontSize = 12.5.sp,
            color = Ink,
        )
        Text(
            text = hint,
            fontFamily = PublicSans,
            fontSize = 11.sp,
            color = MutedText55,
        )
    }
}
