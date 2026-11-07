package com.reciteus2.beishuwa

object MarkdownRenderer {

    fun render(markdown: String, dark: Boolean): String {
        val body = renderBody(markdown.replace("\r\n", "\n"))
        val background = if (dark) "#12100C" else "#F7F3EA"
        val foreground = if (dark) "#EDE6D8" else "#221F19"
        val muted = if (dark) "#B9AF9B" else "#5C5647"
        val accent = if (dark) "#E0BC63" else "#8A6A1F"
        val accentSoft = if (dark) "rgba(224,188,99,0.14)" else "rgba(138,106,31,0.10)"
        val rule = if (dark) "rgba(255,255,255,0.10)" else "rgba(0,0,0,0.08)"

        return """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=3.0">
            <meta name="color-scheme" content="${if (dark) "dark" else "light"}">
            <style>
              :root {
                color-scheme: ${if (dark) "dark" else "light"};
                --bg: $background;
                --fg: $foreground;
                --muted: $muted;
                --accent: $accent;
                --accent-soft: $accentSoft;
                --rule: $rule;
              }
              html, body {
                margin: 0;
                padding: 0;
                background: var(--bg);
                color: var(--fg);
                -webkit-text-size-adjust: 100%;
              }
              body {
                font-family: -apple-system, "Noto Sans CJK SC", "Source Han Sans SC", "Microsoft YaHei", sans-serif;
                font-size: 16px;
                line-height: 1.85;
                padding: 14px 20px 48px;
                word-break: break-word;
              }
              h1 {
                font-size: 26px;
                line-height: 1.35;
                text-align: center;
                margin: 8px 0 26px;
                letter-spacing: 2px;
              }
              h2 {
                font-size: 19px;
                line-height: 1.5;
                margin: 30px 0 14px;
                padding-left: 11px;
                border-left: 4px solid var(--accent);
                font-weight: 600;
              }
              h3 {
                font-size: 17px;
                margin: 22px 0 10px;
              }
              p {
                margin: 0 0 14px;
              }
              .bio {
                color: var(--muted);
                background: var(--accent-soft);
                border-radius: 12px;
                padding: 14px 16px;
                margin: 0 0 8px;
              }
              ul {
                margin: 0 0 14px;
                padding-left: 22px;
              }
              li {
                margin: 6px 0;
              }
              strong {
                color: var(--accent);
              }
              hr {
                border: 0;
                border-top: 1px solid var(--rule);
                margin: 24px 0;
              }
              a {
                color: var(--accent);
                text-decoration: none;
              }
            </style>
            </head>
            <body>
            $body
            </body>
            </html>
        """.trimIndent()
    }

    private fun renderBody(markdown: String): String {
        val out = StringBuilder()
        var inList = false

        fun closeList() {
            if (inList) {
                out.append("</ul>\n")
                inList = false
            }
        }

        markdown.lines().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.isEmpty() -> closeList()

                line.startsWith("### ") -> {
                    closeList()
                    out.append("<h3>").append(inline(line.removePrefix("### "))).append("</h3>\n")
                }

                line.startsWith("## ") -> {
                    closeList()
                    out.append("<h2>").append(inline(line.removePrefix("## "))).append("</h2>\n")
                }

                line.startsWith("# ") -> {
                    closeList()
                    out.append("<h1>").append(inline(line.removePrefix("# "))).append("</h1>\n")
                }

                line.startsWith("- ") -> {
                    if (!inList) {
                        out.append("<ul>\n")
                        inList = true
                    }
                    out.append("<li>").append(inline(line.removePrefix("- "))).append("</li>\n")
                }

                line == "---" -> {
                    closeList()
                    out.append("<hr>\n")
                }

                line.startsWith("> ") -> {
                    closeList()
                    out.append("<p class=\"bio\">").append(inline(line.removePrefix("> "))).append("</p>\n")
                }

                else -> {
                    closeList()
                    out.append("<p>").append(inline(line)).append("</p>\n")
                }
            }
        }
        closeList()
        return out.toString()
    }

    private fun inline(text: String): String {
        val escaped = escapeHtml(text)
        return BOLD.replace(escaped) { match ->
            "<strong>${match.groupValues[1]}</strong>"
        }
    }

    private fun escapeHtml(text: String): String {
        val builder = StringBuilder(text.length)
        text.forEach { char ->
            when (char) {
                '&' -> builder.append("&amp;")
                '<' -> builder.append("&lt;")
                '>' -> builder.append("&gt;")
                '"' -> builder.append("&quot;")
                '\'' -> builder.append("&#39;")
                else -> builder.append(char)
            }
        }
        return builder.toString()
    }

    private val BOLD = Regex("\\*\\*(.+?)\\*\\*")
}
