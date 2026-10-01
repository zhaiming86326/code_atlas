package com.codeatlas.service;

import org.jsoup.Jsoup;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.security.MessageDigest;
import java.util.*;

public final class ContentParser {
    public static final int MAX_BYTES = 2 * 1024 * 1024;
    public static final String VERSION = "text-html-v1";
    public record Parsed(String title, String content) {}
    public record Chunk(String section, String content) {}

    public static Parsed parse(byte[] raw, String name, boolean html) {
        if (raw.length == 0 || raw.length > MAX_BYTES) throw new IllegalArgumentException("内容为空或超过 2 MiB");
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(raw)).toString().replace("\uFEFF", "");
        } catch (CharacterCodingException e) { throw new IllegalArgumentException("请使用 UTF-8 编码文件或网页"); }
        String title = name;
        if (html) {
            var doc = Jsoup.parse(text);
            if (!doc.title().isBlank()) title = doc.title();
            doc.select("script,style,nav,header,footer,aside,noscript,form").remove();
            var body = doc.selectFirst("main,article,[role=main]");
            if (body == null) body = doc.body();
            body.select("pre").forEach(pre -> pre.replaceWith(new org.jsoup.nodes.TextNode("\n```\n" + pre.wholeText() + "\n```\n")));
            body.select("h1,h2,h3,h4,h5,h6").forEach(h -> h.before(new org.jsoup.nodes.TextNode("\n" + "#".repeat(Integer.parseInt(h.tagName().substring(1))) + " ")));
            body.select("p,div,li,blockquote,h1,h2,h3,h4,h5,h6,tr").forEach(block -> block.appendChild(new org.jsoup.nodes.TextNode("\n")));
            body.select("br").forEach(br -> br.replaceWith(new org.jsoup.nodes.TextNode("\n")));
            text = body.wholeText();
        }
        text = text.replace("\r\n", "\n").replace('\r', '\n').strip();
        if (text.isBlank() || text.indexOf('\0') >= 0) throw new IllegalArgumentException("未找到有效正文");
        if (!html) title = Arrays.stream(text.split("\n")).filter(s -> s.startsWith("# ")).findFirst().map(s -> s.substring(2)).orElse(name);
        return new Parsed(title, text);
    }

    public static String hash(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    public static List<Chunk> chunks(String content) {
        var result = new ArrayList<Chunk>();
        var section = new ArrayList<String>();
        var block = new StringBuilder();
        boolean fenced = false;
        String fence = "";
        for (String line : content.split("\n", -1)) {
            String trim = line.stripLeading();
            boolean heading = !fenced && trim.matches("#{1,6} .*" );
            if (heading || (!fenced && block.length() >= 1600 && line.isBlank())) {
                flush(result, section, block);
            }
            if (heading) {
                int level = trim.indexOf(' ');
                while (section.size() >= level) section.removeLast();
                while (section.size() < level - 1) section.add("未命名章节");
                section.add(trim.substring(level + 1));
            }
            block.append(line).append('\n');
            if (!fenced && (trim.startsWith("```") || trim.startsWith("~~~"))) {
                fenced = true; fence = trim.substring(0, 3);
            } else if (fenced && trim.startsWith(fence) && trim.substring(3).isBlank()) fenced = false;
        }
        flush(result, section, block);
        return result;
    }
    private static void flush(List<Chunk> out, List<String> section, StringBuilder block) {
        // ponytail: oversized paragraphs/code stay intact (up to import limit); split by syntax when embeddings require it.
        if (!block.toString().isBlank()) out.add(new Chunk(String.join(" / ", section), block.toString().strip()));
        block.setLength(0);
    }
}
