package com.ocean.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

public class XssFilterUtil {

    private static final Safelist SAFELIST = Safelist.relaxed()
            .addTags("h1", "h2", "h3", "h4", "h5", "h6", "p", "img", "a", "ul", "ol", "li",
                    "table", "tr", "td", "th", "thead", "tbody", "strong", "em", "br", "span", "div",
                    "blockquote", "pre", "code", "hr", "sub", "sup")
            .addAttributes("img", "src", "alt", "width", "height", "style")
            .addAttributes("a", "href", "target", "title")
            .addAttributes("td", "colspan", "rowspan")
            .addAttributes("th", "colspan", "rowspan")
            .addAttributes(":all", "style", "class");

    /**
     * 过滤 HTML 中的 XSS 攻击内容（用于富文本）
     */
    public static String filterRichText(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        return Jsoup.clean(html, SAFELIST);
    }

    /**
     * 去除所有 HTML 标签（用于纯文本字段）
     */
    public static String stripXss(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        // 转义危险字符
        value = value.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
        value = value.replaceAll("\\(", "&#40;").replaceAll("\\)", "&#41;");
        value = value.replaceAll("'", "&#39;");
        value = value.replaceAll("eval\\((.*)\\)", "");
        value = value.replaceAll("[\"'][\\s]*javascript:(.*)[\"']", "\"\"");
        value = value.replaceAll("script", "");
        return value;
    }
}
