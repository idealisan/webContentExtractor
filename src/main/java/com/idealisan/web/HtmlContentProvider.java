package com.idealisan.web;

import org.jsoup.nodes.Element;

public interface HtmlContentProvider {
    public String getContent();
    public Element getContentElement();
}
