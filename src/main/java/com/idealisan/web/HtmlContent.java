package com.idealisan.web;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class HtmlContent implements HtmlContentProvider {

    private final Document htmlDoc;
    private final HashMap<Node, Score> scores = new HashMap<Node, Score>();

    private static final Map<String, Double> weight = new HashMap<String, Double>();
    private static final List<String> massTags = new ArrayList<String>();

    static {
        massTags.add("li");
        massTags.add("i");
        massTags.add("td");
//        massTags.add("a");
        massTags.add("tr");
        massTags.add("th");
//        massTags.add("item");
        massTags.add("it");
        massTags.add("breadcrumb");
        massTags.add("bread");
        massTags.add("crumb");

        weight.put("p", 1.0);
        weight.put("br", 0.01);
        weight.put("img", 1.0);
        weight.put("picture", 1.0);
        weight.put("a", -2.0);
        weight.put("video", 1.0);
//        weight.put("cont", 3.0);
//        weight.put("con", 3.0);
//        weight.put("container", 2.0);
        weight.put("content",3.0);
        weight.put("article", 3.0);
//        weight.put("a", -3.0);
        weight.put("main", 2.0);
        weight.put("text", 2.0);
//        weight.put("input", -100.0);
//        weight.put("button", -100.0);
//        weight.put("form", -100.0);
        weight.put("code", 3.0);
        weight.put("pre", 3.0);
        weight.put("comment", -5.0);
    }


    public HtmlContent(String htmlText) {
        this(Jsoup.parse(htmlText));
    }

    public HtmlContent(Document htmlDoc) {
        this.htmlDoc = htmlDoc;
    }

    private void clean() {
        htmlDoc.select("script,noscript,style,iframe,footer,foot,head,header,nav").remove();
    }

    private boolean isMassive(Node node) {
        //by tag
        if (massTags.contains(node.nodeName())) {
            return true;
        }

        //by class
        String styleClass = node.attr("class").toLowerCase();
        String[] styleClasses = styleClass.split("[\\-\\s]");
        for (String str : styleClasses) {
            if (massTags.contains(str)) {
                return true;
            }
        }

        return false;
    }

    private void calculateScore(Node node) {
        ensureHasScore(node);
        if (isMassive(node)) {
            return;
        }
        //by class
        String styleClass = node.attr("class").toLowerCase();
        String[] styleClasses = styleClass.split("[\\-\\s]");
        for (String str : styleClasses) {
            scores.get(node).value += getWeightOrZore(str);
        }

        //by id
        scores.get(node).value+=getWeightOrZore(node.attr("id"));

        //by tag
        scores.get(node).value += getWeightOrZore(node.nodeName());

        //by leaf
        if (node.childNodeSize() == 0) {
            if (node instanceof TextNode) {
                scores.get(node).value += weight.get("text");
                //text length
                scores.get(node).value += Math.floor(((TextNode) node).text().length()/50.0) + 1;
            }
            //big image
            if (node.nodeName().equals("img")) {
                try {
                    String widthStr = node.attr("width");
                    widthStr = widthStr.replaceAll("[^\\d]+", "");
                    if (Integer.valueOf(widthStr) > 50) {
                        scores.get(node).value += 3.0;
                    }

                    widthStr = node.attr("height");
                    widthStr = widthStr.replaceAll("[^\\d]+", "");
                    if (Integer.valueOf(widthStr) > 50) {
                        scores.get(node).value += 3.0;
                    }
                } catch (NumberFormatException e) {

                }
            }

        }

        //for parent
        if (node.hasParent() && !massTags.contains(node.nodeName())) {
            Node parent = node.parent();
            Score score = ensureHasScore(parent);
            double selfValue = scores.get(node).value;
            score.value += selfValue*1.4;
            //for grandparent
            if (parent.hasParent()) {
                Node grantParent = parent.parent();
                ensureHasScore(grantParent).value += 0.7*selfValue;
            }
        }
    }

    private Score ensureHasScore(Node node) {
        if (scores.get(node) == null) {
            scores.put(node, new Score(0));
        }
        return scores.get(node);
    }

    private double getWeightOrZore(String text) {
        Double ret = weight.get(text);
        return ret == null ? 0 : ret;
    }

    private void vote() {
        clean();
        Elements allElements = htmlDoc.getAllElements();
        for (Element el : allElements) {
            calculateScore(el);
        }
    }

    @Override
    public String getContent() {
        Element contentElement = getContentElement();
        if (contentElement==null){
            return null;
        }
        return getContentElement().wholeText();
    }

    @Override
    public Element getContentElement() {
        vote();
        Element ret = null;
        double maxScore = 0;

        Set<Map.Entry<Node, Score>> entries = scores.entrySet();
        if (entries.isEmpty()) {
            return ret;
        } else {
            for (Map.Entry<Node, Score> en : entries) {
                if (en.getValue().value > maxScore) {
                    ret = (Element) en.getKey();
                    maxScore = en.getValue().value;
                }
            }
        }

        if (maxScore != 0) {
            return ret;
        }
        return null;
    }

    private static class Score {
        public double value = 0;

        public Score(double score) {
            this.value = score;
        }

        @Override
        public String toString() {
            return "Score{" +
                    "value=" + value +
                    '}';
        }
    }


    @Override
    public String toString() {
        return getContent();
    }

    String getVoteDetail() {
        return scores.toString();
    }

    public static void main(String[] args) throws IOException {
        while (true) {
            System.out.println("URL:");
            Document document = Jsoup.parse(new URL(new Scanner(System.in).nextLine().trim()), 30000);
            HtmlContent htmlContent = new HtmlContent(document);
            Element contentElement = htmlContent.getContentElement();
            System.out.println(contentElement);
            Files.write(Paths.get("temp.html"), contentElement.html().getBytes());
//        System.out.println(htmlContent.getContentElement());
//        System.out.println(htmlContent.getVoteDetail());
        }
    }
}
