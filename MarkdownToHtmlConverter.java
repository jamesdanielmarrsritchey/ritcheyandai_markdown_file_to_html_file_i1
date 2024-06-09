import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownToHtmlConverter {
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[(.*?)\\]\\((.*?)\\)");
    private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[(.*?)\\]\\((.*?)\\)");
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.*?)\\*\\*");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("\\*(.*?)\\*");
    private static final Pattern ORDERED_LIST_PATTERN = Pattern.compile("^\\d+\\.\\s+(.*)");
    private static final Pattern UNORDERED_LIST_PATTERN = Pattern.compile("^\\*\\s+(.*)");
    private static final Pattern BLOCKQUOTE_PATTERN = Pattern.compile("^>\\s+(.*)");
    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.*)");
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("^```(.*)");

    public static String convertMarkdownToHtml(String markdownFilePath) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<title>Markdown to HTML</title>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(markdownFilePath), StandardCharsets.UTF_8)) {
            String line;
            boolean inOrderedList = false;
            boolean inUnorderedList = false;
            boolean inBlockquote = false;
            boolean inCodeBlock = false;
            while ((line = reader.readLine()) != null) {
                Matcher orderedListMatcher = ORDERED_LIST_PATTERN.matcher(line);
                Matcher unorderedListMatcher = UNORDERED_LIST_PATTERN.matcher(line);
                Matcher blockquoteMatcher = BLOCKQUOTE_PATTERN.matcher(line);
                Matcher headingMatcher = HEADING_PATTERN.matcher(line);
                Matcher codeBlockMatcher = CODE_BLOCK_PATTERN.matcher(line);
                if (orderedListMatcher.matches()) {
                    if (!inOrderedList) {
                        html.append("<ol>\n");
                        inOrderedList = true;
                    }
                    html.append("<li>").append(convertAll(orderedListMatcher.group(1))).append("</li>\n");
                } else if (unorderedListMatcher.matches()) {
                    if (!inUnorderedList) {
                        html.append("<ul>\n");
                        inUnorderedList = true;
                    }
                    html.append("<li>").append(convertAll(unorderedListMatcher.group(1))).append("</li>\n");
                } else if (blockquoteMatcher.matches()) {
                    if (!inBlockquote) {
                        html.append("<blockquote>\n");
                        inBlockquote = true;
                    }
                    html.append(convertAll(blockquoteMatcher.group(1))).append("<br>\n");
                } else if (headingMatcher.matches()) {
                    int level = headingMatcher.group(1).length();
                    html.append("<h").append(level).append(">")
                        .append(convertAll(headingMatcher.group(2)))
                        .append("</h").append(level).append(">\n");
                } else if (codeBlockMatcher.matches()) {
                    if (inCodeBlock) {
                        html.append("</code></pre>\n");
                        inCodeBlock = false;
                    } else {
                        html.append("<pre><code>\n");
                        inCodeBlock = true;
                    }
                } else {
                    if (inOrderedList) {
                        html.append("</ol>\n");
                        inOrderedList = false;
                    }
                    if (inUnorderedList) {
                        html.append("</ul>\n");
                        inUnorderedList = false;
                    }
                    if (inBlockquote) {
                        html.append("</blockquote>\n");
                        inBlockquote = false;
                    }
                    if (inCodeBlock) {
                        html.append(line).append("\n");
                    } else {
                        html.append("<p>").append(convertAll(line)).append("</p>\n");
                    }
                }
            }

            if (inOrderedList) {
                html.append("</ol>\n");
            }
            if (inUnorderedList) {
                html.append("</ul>\n");
            }
            if (inBlockquote) {
                html.append("</blockquote>\n");
            }
            if (inCodeBlock) {
                html.append("</code></pre>\n");
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading markdown file", e);
        }

        html.append("</body>\n");
        html.append("</html>\n");
        return html.toString();
    }

    private static String convertAll(String text) {
        String result = convertLinks(text);
        result = convertImages(result);
        result = convertBold(result);
        return convertItalic(result);
    }

    private static String convertLinks(String text) {
        Matcher matcher = LINK_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "<a href=\"" + matcher.group(2) + "\">" + matcher.group(1) + "</a>");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String convertImages(String text) {
        Matcher matcher = IMAGE_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "<img src=\"" + matcher.group(2) + "\" alt=\"" + matcher.group(1) + "\">");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String convertBold(String text) {
        Matcher matcher = BOLD_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "<b>" + matcher.group(1) + "</b>");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String convertItalic(String text) {
        Matcher matcher = ITALIC_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "<i>" + matcher.group(1) + "</i>");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static void main(String[] args) {
        String sourceFilePath = null;
        String destinationFilePath = null;
        for (int i = 0; i < args.length; i++) {
            if ("--source_file".equals(args[i]) && i + 1 < args.length) {
                sourceFilePath = args[i + 1];
            } else if ("--destination_file".equals(args[i]) && i + 1 < args.length) {
                destinationFilePath = args[i + 1];
            }
        }
        if (sourceFilePath == null) {
            System.err.println("Please specify the source file with --source_file");
            System.exit(1);
        }
        if (destinationFilePath == null) {
            System.err.println("Please specify the destination file with --destination_file");
            System.exit(1);
        }
        try {
            String html = convertMarkdownToHtml(sourceFilePath);
            Files.write(Paths.get(destinationFilePath), html.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("Error reading source file or writing to destination file: " + e.getMessage());
            System.exit(1);
        }
    }
}