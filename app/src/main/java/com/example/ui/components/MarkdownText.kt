package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CodeBlockBackgroundDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.util.LruCache

private val markdownBlockCache = LruCache<String, List<MarkdownBlock>>(200)

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val blocks = remember(text) { 
        markdownBlockCache.get(text) ?: parseMarkdownBlocks(text).also { markdownBlockCache.put(text, it) }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.CodeBlock -> {
                    CodeBlockView(
                        language = block.language,
                        code = block.code
                    )
                }
                is MarkdownBlock.MathBlock -> {
                    MathFormulaCard(formula = block.formula)
                }
                is MarkdownBlock.Header -> {
                    Text(
                        text = parseInlineMarkdown(block.content, textColor),
                        style = when (block.level) {
                            1 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            2 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            else -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        },
                        color = textColor,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }
                is MarkdownBlock.BulletPoint -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "• ",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(
                            text = parseInlineMarkdown(block.content, textColor),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                    }
                }
                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = parseInlineMarkdown(block.content, textColor),
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MathFormulaCard(formula: String) {
    val formattedFormula = remember(formula) { formatMathSymbols(formula) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
            modifier = Modifier.padding(vertical = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = formattedFormula,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 24.sp
                )
            }
        }
    }
}

@Composable
fun CodeBlockView(
    language: String,
    code: String
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CodeBlockBackgroundDark,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("code_block_container")
    ) {
        Column {
            // Header bar with language & copy button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.ifBlank { "code" }.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = Color(0xFF94A3B8)
                )

                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Copied Code", code)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                        copied = true
                        scope.launch {
                            delay(2000)
                            copied = false
                        }
                    },
                    modifier = Modifier.size(28.dp).testTag("copy_code_button")
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = if (copied) Color(0xFF10B981) else Color(0xFFCBD5E1),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Scrollable code content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(14.dp)
            ) {
                Text(
                    text = code,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = Color(0xFFE2E8F0)
                )
            }
        }
    }
}

sealed class MarkdownBlock {
    data class Paragraph(val content: String) : MarkdownBlock()
    data class Header(val level: Int, val content: String) : MarkdownBlock()
    data class BulletPoint(val content: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class MathBlock(val formula: String) : MarkdownBlock()
}

fun parseMarkdownBlocks(rawText: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = rawText.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        // Math Block detection ($$ formula $$)
        if (line.trim().startsWith("$$") && line.trim().endsWith("$$") && line.trim().length > 4) {
            val formula = line.trim().removePrefix("$$").removeSuffix("$$").trim()
            blocks.add(MarkdownBlock.MathBlock(formula))
            i++
            continue
        }

        if (line.trim() == "$$") {
            val mathLines = mutableListOf<String>()
            i++
            while (i < lines.size && lines[i].trim() != "$$") {
                mathLines.add(lines[i])
                i++
            }
            blocks.add(MarkdownBlock.MathBlock(mathLines.joinToString(" ")))
            i++
            continue
        }

        // Code Block detection
        if (line.trim().startsWith("```")) {
            val language = line.trim().removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            blocks.add(MarkdownBlock.CodeBlock(language, codeLines.joinToString("\n")))
            i++
            continue
        }

        if (line.startsWith("#")) {
            val count = line.takeWhile { it == '#' }.length
            val content = line.drop(count).trim()
            blocks.add(MarkdownBlock.Header(count.coerceIn(1, 3), content))
            i++
            continue
        }

        if (line.trim().startsWith("* ") || line.trim().startsWith("- ")) {
            val content = line.trim().substring(2).trim()
            blocks.add(MarkdownBlock.BulletPoint(content))
            i++
            continue
        }

        if (line.isNotBlank()) {
            blocks.add(MarkdownBlock.Paragraph(line.trim()))
        }
        i++
    }

    return blocks
}

fun formatMathSymbols(raw: String): String {
    var res = raw.trim()

    // 1. Clean LaTeX wrappers like \text{...}, \mathrm{...}, \mathbf{...}, \operatorname{...}
    var prev = ""
    var iter1 = 0
    while (prev != res && iter1 < 10) {
        prev = res
        iter1++
        res = res.replace(Regex("""\\text\{([^{}]*)\}""")) { it.groupValues[1] }
        res = res.replace(Regex("""\\mathrm\{([^{}]*)\}""")) { it.groupValues[1] }
        res = res.replace(Regex("""\\mathbf\{([^{}]*)\}""")) { it.groupValues[1] }
        res = res.replace(Regex("""\\operatorname\{([^{}]*)\}""")) { it.groupValues[1] }
        res = res.replace(Regex("""\\displaystyle"""), "")
        res = res.replace(Regex("""\\limits"""), "")
    }

    // 2. Clean nested fractions \frac{a}{b} -> (a)/(b) or a/b
    prev = ""
    var iter2 = 0
    while (prev != res && iter2 < 10) {
        prev = res
        iter2++
        res = res.replace(Regex("""\\frac\{([^{}]*)\}\{([^{}]*)\}""")) { match ->
            val num = match.groupValues[1].trim()
            val den = match.groupValues[2].trim()
            if (num.length <= 3 && den.length <= 3 && !num.contains(" ") && !den.contains(" ")) {
                "$num/$den"
            } else {
                "($num)/($den)"
            }
        }
    }

    // 3. Clean subscripts with curly braces like h_{sph} -> h_sph, I_{cyl} -> I_cyl
    res = res.replace(Regex("""([a-zA-Z0-9]+)_\{([^{}]*)\}""")) { "${it.groupValues[1]}_${it.groupValues[2]}" }

    // 4. Clean brackets & parens
    res = res.replace("\\left(", "(").replace("\\right)", ")")
    res = res.replace("\\left[", "[").replace("\\right]", "]")
    res = res.replace("\\left\\{", "{").replace("\\right\\}", "}")
    res = res.replace("\\left.", "").replace("\\right.", "")
    res = res.replace("\\left", "").replace("\\right", "")

    // 5. Greek letters & symbols
    res = res
        .replace("\\omega", "ω")
        .replace("\\Omega", "Ω")
        .replace("\\alpha", "α")
        .replace("\\beta", "β")
        .replace("\\theta", "θ")
        .replace("\\Theta", "Θ")
        .replace("\\pi", "π")
        .replace("\\mu", "μ")
        .replace("\\lambda", "λ")
        .replace("\\rho", "ρ")
        .replace("\\sigma", "σ")
        .replace("\\tau", "τ")
        .replace("\\phi", "φ")
        .replace("\\Phi", "Φ")
        .replace("\\Delta", "Δ")
        .replace("\\delta", "δ")
        .replace("\\infty", "∞")
        .replace("\\propto", " ∝ ")
        .replace("\\approx", " ≈ ")
        .replace("\\neq", " ≠ ")
        .replace("\\leq", " ≤ ")
        .replace("\\geq", " ≥ ")
        .replace("\\le", " ≤ ")
        .replace("\\ge", " ≥ ")
        .replace("\\times", " × ")
        .replace("\\cdot", " · ")
        .replace("\\div", " ÷ ")
        .replace("\\pm", " ± ")
        .replace("\\int", "∫")
        .replace("\\sum", "∑")
        .replace("\\sqrt{", "√(")
        .replace("\\sqrt", "√")
        .replace("\\cos", "cos")
        .replace("\\sin", "sin")
        .replace("\\tan", "tan")
        .replace("\\log", "log")
        .replace("\\ln", "ln")

    // 6. Common powers
    res = res
        .replace("^2", "²")
        .replace("^3", "³")
        .replace("^{2}", "²")
        .replace("^{3}", "³")
        .replace("^{4}", "⁴")
        .replace("^{n}", "ⁿ")
        .replace("^{-1}", "⁻¹")
        .replace("^{-2}", "⁻²")
        .replace("^⁻¹", "⁻¹")

    // Clean remaining backslashes before plain words
    res = res.replace(Regex("""\\([a-zA-Z]+)""")) { it.groupValues[1] }
    // Clean residual double spaces
    res = res.replace(Regex("""\s+"""), " ")

    return res
}

private val BOLD_REGEX = Regex("\\*\\*(.*?)\\*\\*")
private val CODE_REGEX = Regex("`(.*?)`")
private val INLINE_MATH_REGEX = Regex("\\$(.*?)\\$")

@Composable
fun parseInlineMarkdown(text: String, defaultColor: Color): androidx.compose.ui.text.AnnotatedString {
    val primaryColor = MaterialTheme.colorScheme.primary
    return remember(text, defaultColor, primaryColor) {
        try {
            buildAnnotatedString {
                var remaining = text
                var loopLimit = 0
                while (remaining.isNotEmpty() && loopLimit < 300) {
                    loopLimit++
                    val boldMatch = BOLD_REGEX.find(remaining)
                    val codeMatch = CODE_REGEX.find(remaining)
                    val mathMatch = INLINE_MATH_REGEX.find(remaining)

                    val nextMatch = listOfNotNull(boldMatch, codeMatch, mathMatch).minByOrNull { it.range.first }

                    if (nextMatch == null) {
                        append(remaining)
                        break
                    }

                    val start = nextMatch.range.first
                    val end = nextMatch.range.last + 1

                    if (start > 0) {
                        append(remaining.substring(0, start))
                    }

                    if (nextMatch == boldMatch && boldMatch.groupValues.size > 1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = defaultColor)) {
                            append(boldMatch.groupValues[1])
                        }
                    } else if (nextMatch == codeMatch && codeMatch.groupValues.size > 1) {
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = Color(0x226366F1),
                                fontWeight = FontWeight.SemiBold,
                                color = primaryColor
                            )
                        ) {
                            append(" ${codeMatch.groupValues[1]} ")
                        }
                    } else if (nextMatch == mathMatch && mathMatch.groupValues.size > 1) {
                        val formula = formatMathSymbols(mathMatch.groupValues[1])
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.SemiBold,
                                color = defaultColor
                            )
                        ) {
                            append(" $formula ")
                        }
                    }

                    val advance = if (end > start) end else (start + 1).coerceAtMost(remaining.length)
                    remaining = remaining.substring(advance)
                }
            }
        } catch (_: Exception) {
            androidx.compose.ui.text.AnnotatedString(text)
        }
    }
}
