package com.jobos.desktop.core.ui;

import javafx.scene.web.WebView;
import java.util.regex.Pattern;

/**
 * Utility class for rendering Markdown text as HTML for display in WebView.
 */
public class RichTextRenderer {
    
    private static final String HTML_TEMPLATE = """
        <!DOCTYPE html>
        <html>
        <head>
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
                    font-size: 14px;
                    color: #374151;
                    line-height: 1.6;
                    margin: 0;
                    padding: 0;
                    background-color: transparent;
                }
                h1, h2, h3, h4, h5, h6 {
                    color: #111827;
                    margin-top: 16px;
                    margin-bottom: 8px;
                }
                h1 { font-size: 24px; }
                h2 { font-size: 20px; }
                h3 { font-size: 16px; }
                p {
                    margin: 0 0 12px 0;
                }
                ul, ol {
                    margin: 0 0 12px 0;
                    padding-left: 24px;
                }
                li {
                    margin-bottom: 4px;
                }
                strong, b {
                    font-weight: 600;
                    color: #111827;
                }
                em, i {
                    font-style: italic;
                }
                code {
                    background-color: #F3F4F6;
                    padding: 2px 6px;
                    border-radius: 4px;
                    font-family: 'SF Mono', Monaco, Consolas, monospace;
                    font-size: 13px;
                }
                pre {
                    background-color: #F3F4F6;
                    padding: 12px;
                    border-radius: 8px;
                    overflow-x: auto;
                }
                pre code {
                    background: none;
                    padding: 0;
                }
                a {
                    color: #0F766E;
                    text-decoration: none;
                }
                a:hover {
                    text-decoration: underline;
                }
                blockquote {
                    border-left: 4px solid #0F766E;
                    margin: 12px 0;
                    padding: 8px 16px;
                    background-color: #F0FDFA;
                    color: #374151;
                }
                hr {
                    border: none;
                    border-top: 1px solid #E5E7EB;
                    margin: 16px 0;
                }
                table {
                    border-collapse: collapse;
                    width: 100%;
                    margin: 12px 0;
                }
                th, td {
                    border: 1px solid #E5E7EB;
                    padding: 8px 12px;
                    text-align: left;
                }
                th {
                    background-color: #F9FAFB;
                    font-weight: 600;
                }
            </style>
        </head>
        <body>
            %s
        </body>
        </html>
        """;
    
    /**
     * Configure a WebView for rich text rendering
     */
    public static void configureWebView(WebView webView) {
        webView.setContextMenuEnabled(false);
        webView.getEngine().setJavaScriptEnabled(false);
        
        // Make the WebView transparent
        webView.setStyle("-fx-background-color: transparent;");
        
        // Disable scrollbars and make content fit
        webView.getEngine().loadContent("""
            <html><body></body></html>
            """);
    }
    
    /**
     * Render markdown text to HTML and load into WebView
     */
    public static void renderMarkdown(WebView webView, String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            webView.getEngine().loadContent(String.format(HTML_TEMPLATE, "<p>No content</p>"));
            return;
        }
        
        String html = convertMarkdownToHtml(markdown);
        webView.getEngine().loadContent(String.format(HTML_TEMPLATE, html));
    }
    
    /**
     * Simple markdown to HTML converter
     */
    public static String convertMarkdownToHtml(String markdown) {
        if (markdown == null) return "";
        
        String html = markdown;
        
        // Escape HTML special characters first
        html = html.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;");
        
        // Headers (must be at start of line)
        html = Pattern.compile("^######\\s+(.+)$", Pattern.MULTILINE).matcher(html).replaceAll("<h6>$1</h6>");
        html = Pattern.compile("^#####\\s+(.+)$", Pattern.MULTILINE).matcher(html).replaceAll("<h5>$1</h5>");
        html = Pattern.compile("^####\\s+(.+)$", Pattern.MULTILINE).matcher(html).replaceAll("<h4>$1</h4>");
        html = Pattern.compile("^###\\s+(.+)$", Pattern.MULTILINE).matcher(html).replaceAll("<h3>$1</h3>");
        html = Pattern.compile("^##\\s+(.+)$", Pattern.MULTILINE).matcher(html).replaceAll("<h2>$1</h2>");
        html = Pattern.compile("^#\\s+(.+)$", Pattern.MULTILINE).matcher(html).replaceAll("<h1>$1</h1>");
        
        // Bold and italic
        html = Pattern.compile("\\*\\*\\*(.+?)\\*\\*\\*").matcher(html).replaceAll("<strong><em>$1</em></strong>");
        html = Pattern.compile("___(.+?)___").matcher(html).replaceAll("<strong><em>$1</em></strong>");
        html = Pattern.compile("\\*\\*(.+?)\\*\\*").matcher(html).replaceAll("<strong>$1</strong>");
        html = Pattern.compile("__(.+?)__").matcher(html).replaceAll("<strong>$1</strong>");
        html = Pattern.compile("\\*(.+?)\\*").matcher(html).replaceAll("<em>$1</em>");
        html = Pattern.compile("_(.+?)_").matcher(html).replaceAll("<em>$1</em>");
        
        // Inline code
        html = Pattern.compile("`([^`]+)`").matcher(html).replaceAll("<code>$1</code>");
        
        // Links
        html = Pattern.compile("\\[([^\\]]+)\\]\\(([^)]+)\\)").matcher(html).replaceAll("<a href=\"$2\" target=\"_blank\">$1</a>");
        
        // Horizontal rule
        html = Pattern.compile("^---$", Pattern.MULTILINE).matcher(html).replaceAll("<hr>");
        html = Pattern.compile("^\\*\\*\\*$", Pattern.MULTILINE).matcher(html).replaceAll("<hr>");
        
        // Unordered lists (simple approach - handle bullet points at start of lines)
        html = processLists(html);
        
        // Paragraphs - wrap non-tagged lines
        html = processParagraphs(html);
        
        // Line breaks
        html = html.replace("\n\n", "</p><p>");
        
        return html;
    }
    
    private static String processLists(String html) {
        StringBuilder result = new StringBuilder();
        String[] lines = html.split("\n");
        boolean inList = false;
        boolean isOrdered = false;
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            // Check for unordered list item
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ")) {
                if (!inList) {
                    result.append("<ul>\n");
                    inList = true;
                    isOrdered = false;
                } else if (isOrdered) {
                    result.append("</ol>\n<ul>\n");
                    isOrdered = false;
                }
                result.append("<li>").append(trimmed.substring(2)).append("</li>\n");
            }
            // Check for ordered list item
            else if (trimmed.matches("^\\d+\\.\\s+.+$")) {
                if (!inList) {
                    result.append("<ol>\n");
                    inList = true;
                    isOrdered = true;
                } else if (!isOrdered) {
                    result.append("</ul>\n<ol>\n");
                    isOrdered = true;
                }
                String content = trimmed.replaceFirst("^\\d+\\.\\s+", "");
                result.append("<li>").append(content).append("</li>\n");
            }
            else {
                if (inList) {
                    result.append(isOrdered ? "</ol>\n" : "</ul>\n");
                    inList = false;
                }
                result.append(line).append("\n");
            }
        }
        
        if (inList) {
            result.append(isOrdered ? "</ol>\n" : "</ul>\n");
        }
        
        return result.toString();
    }
    
    private static String processParagraphs(String html) {
        StringBuilder result = new StringBuilder();
        String[] lines = html.split("\n");
        StringBuilder paragraph = new StringBuilder();
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            // Skip if already a block element
            if (trimmed.startsWith("<h") || trimmed.startsWith("<ul") || 
                trimmed.startsWith("<ol") || trimmed.startsWith("<li") ||
                trimmed.startsWith("<hr") || trimmed.startsWith("<blockquote") ||
                trimmed.startsWith("</ul") || trimmed.startsWith("</ol") ||
                trimmed.startsWith("<pre") || trimmed.isEmpty()) {
                
                if (paragraph.length() > 0) {
                    result.append("<p>").append(paragraph.toString().trim()).append("</p>\n");
                    paragraph = new StringBuilder();
                }
                result.append(line).append("\n");
            } else {
                if (paragraph.length() > 0) paragraph.append(" ");
                paragraph.append(trimmed);
            }
        }
        
        if (paragraph.length() > 0) {
            result.append("<p>").append(paragraph.toString().trim()).append("</p>\n");
        }
        
        return result.toString();
    }
    
    /**
     * Calculate the height needed for content
     */
    public static void autoResizeWebView(WebView webView) {
        webView.getEngine().executeScript(
            "document.body.scrollHeight"
        );
    }
    
    /**
     * Render plain text as HTML (preserves line breaks)
     */
    public static void renderPlainText(WebView webView, String text) {
        if (text == null || text.isEmpty()) {
            webView.getEngine().loadContent(String.format(HTML_TEMPLATE, "<p>No content</p>"));
            return;
        }
        
        String html = text.replace("&", "&amp;")
                         .replace("<", "&lt;")
                         .replace(">", "&gt;")
                         .replace("\n\n", "</p><p>")
                         .replace("\n", "<br>");
        
        webView.getEngine().loadContent(String.format(HTML_TEMPLATE, "<p>" + html + "</p>"));
    }
}
