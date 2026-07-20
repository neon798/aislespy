package app.aislespy.ui.ingredient

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.aislespy.domain.knowledge.IngredientHit
import app.aislespy.ui.components.AisleListRowCard
import app.aislespy.ui.components.InfoChip
import app.aislespy.ui.components.SeverityBar
import app.aislespy.ui.theme.AisleColors
import app.aislespy.ui.theme.AisleSpyShapes
import app.aislespy.ui.theme.BricolageGrotesque
import app.aislespy.ui.theme.PublicSans

private val ExampleQueries = listOf(
    "aspartame",
    "parfum",
    "E250",
    "titanium dioxide",
)

private const val HELPER_COPY =
    "Search additives and cosmetic ingredients by name or E-number"

private const val NO_RESULTS_COPY =
    "No match in our packs — try an E-number or a common name."

@Composable
fun IngredientLookupScreen(
    onOpenIngredient: (concernId: String) -> Unit,
    modifier: Modifier = Modifier,
    lookupViewModel: IngredientLookupViewModel? = null,
) {
    val app = LocalContext.current.applicationContext as Application
    val resolvedVm = lookupViewModel
        ?: viewModel(factory = IngredientLookupViewModel.Factory(app))
    val uiState by resolvedVm.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = modifier,
        containerColor = AisleColors.current.surface,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(top = 18.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Ingredients",
                    style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                    color = AisleColors.current.ink,
                    modifier = Modifier.semantics { heading() },
                )
                SearchField(
                    query = uiState.query,
                    onQueryChange = resolvedVm::onQueryChange,
                    onClear = {
                        resolvedVm.onQueryChange("")
                        focusManager.clearFocus()
                    },
                    onSearch = { focusManager.clearFocus() },
                )
            }

            when {
                uiState.isPrompt -> {
                    PromptContent(
                        onExampleClick = { example ->
                            resolvedVm.onQueryChange(example)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 22.dp),
                    )
                }
                uiState.isNoResults -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 38.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = NO_RESULTS_COPY,
                            fontFamily = PublicSans,
                            fontSize = 13.sp,
                            lineHeight = 20.8.sp,
                            color = AisleColors.current.muted55,
                            modifier = Modifier.semantics {
                                contentDescription = NO_RESULTS_COPY
                            },
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = 22.dp,
                            vertical = 4.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(
                            items = uiState.results,
                            key = { "${it.domain}:${it.entry.id}" },
                        ) { hit ->
                            IngredientHitRow(
                                hit = hit,
                                onClick = {
                                    val id = resolvedVm.selectHit(hit)
                                    onOpenIngredient(id)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val borderColor = if (focused) {
        AisleColors.current.olive
    } else {
        AisleColors.current.outlineChipBorder
    }

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        cursorBrush = SolidColor(AisleColors.current.olive),
        textStyle = TextStyle(
            fontFamily = PublicSans,
            fontSize = 15.sp,
            color = AisleColors.current.ink,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .semantics { contentDescription = "Search ingredients" },
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AisleColors.current.card, AisleSpyShapes.input)
                    .border(1.5.dp, borderColor, AisleSpyShapes.input)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = if (focused) {
                        AisleColors.current.olive
                    } else {
                        AisleColors.current.muted45
                    },
                    modifier = Modifier.size(20.dp),
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Name or E-number",
                            fontFamily = PublicSans,
                            fontSize = 15.sp,
                            color = AisleColors.current.muted45,
                        )
                    }
                    inner()
                }
                if (query.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Clear search",
                        tint = AisleColors.current.muted55,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable(onClick = onClear)
                            .semantics { contentDescription = "Clear search" },
                    )
                }
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PromptContent(
    onExampleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = HELPER_COPY,
            fontFamily = PublicSans,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = AisleColors.current.muted60,
        )
        Text(
            text = "Try one of these",
            fontFamily = PublicSans,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = AisleColors.current.muted55,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ExampleQueries.forEach { example ->
                Text(
                    text = example,
                    fontFamily = PublicSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = AisleColors.current.olive,
                    modifier = Modifier
                        .background(
                            AisleColors.current.oliveContainer,
                            AisleSpyShapes.pill,
                        )
                        .border(
                            1.dp,
                            AisleColors.current.olive.copy(alpha = 0.25f),
                            AisleSpyShapes.pill,
                        )
                        .clickable { onExampleClick(example) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .semantics {
                            contentDescription = "Example search $example"
                        },
                )
            }
        }
        Text(
            text = "Informational only — not medical advice.",
            fontFamily = PublicSans,
            fontSize = 11.sp,
            color = AisleColors.current.muted45,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun IngredientHitRow(
    hit: IngredientHit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val entry = hit.entry
    val domainLabel = when (hit.domain.lowercase()) {
        "food" -> "Food"
        "beauty" -> "Beauty"
        else -> hit.domain.replaceFirstChar { it.uppercase() }
    }
    val category = entry.categories.firstOrNull().orEmpty()
    val whyOneLine = entry.why.replace('\n', ' ').trim()
    val talkBack = buildString {
        append(entry.title)
        append(", ")
        append(domainLabel)
        if (category.isNotEmpty()) {
            append(", ")
            append(category)
        }
        append(", severity ${entry.severity} of 5")
    }

    AisleListRowCard(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = talkBack
        },
        onClick = onClick,
        contentPadding = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = entry.title,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    color = AisleColors.current.ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = AisleColors.current.muted45,
                    modifier = Modifier.size(18.dp),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                InfoChip(
                    label = domainLabel,
                    contentDescription = "Domain $domainLabel",
                )
                if (category.isNotEmpty()) {
                    Text(
                        text = category,
                        fontFamily = PublicSans,
                        fontSize = 11.sp,
                        color = AisleColors.current.muted55,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            SeverityBar(severity = entry.severity)
            Text(
                text = whyOneLine,
                fontFamily = PublicSans,
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                color = AisleColors.current.muted70,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
