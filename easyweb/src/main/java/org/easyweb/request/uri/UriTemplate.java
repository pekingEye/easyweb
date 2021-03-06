package org.easyweb.request.uri;

import org.easyweb.request.PageMethod;
import org.easyweb.request.PageMethod;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: jimmey/shantong
 * Date: 13-7-4
 * Time: 下午2:46
 */
public class UriTemplate implements Serializable {

    /**
     * Captures URI template variable names.
     */
    private static final Pattern NAMES_PATTERN = Pattern.compile("\\{([^/]+?)\\}");

    /**
     * Replaces template variables in the URI template.
     */
    private static final String DEFAULT_VARIABLE_PATTERN = "(.*)";

    private final List<String> variableNames;

    private final boolean restful;

    private final Pattern matchPattern;

    private final String uriTemplate;

    private final PageMethod pageMethod;

    /**
     * Construct a new {@code UriTemplate} with the given URI String.
     *
     * @param uriTemplate the URI template string
     */
    public UriTemplate(String uriTemplate, PageMethod pageMethod) {
        Parser parser = new Parser(uriTemplate);
        this.uriTemplate = uriTemplate;
        this.variableNames = parser.getVariableNames();
        this.restful = !this.variableNames.isEmpty();
        this.pageMethod = pageMethod;
        if (restful) {
            this.matchPattern = parser.getMatchPattern();
        } else {
            this.matchPattern = null;
        }
    }

    /**
     * Return the names of the variables in the template, in order.
     *
     * @return the template variable names
     */
    public List<String> getVariableNames() {
        return this.variableNames;
    }

    public PageMethod getPageMethod() {
        return pageMethod;
    }

    public String getUriTemplate() {
        return uriTemplate;
    }

    public boolean isRestful() {
        return restful;
    }

    /**
     * Indicate whether the given URI matches this template.
     *
     * @param uri the URI to match to
     * @return <code>true</code> if it matches; <code>false</code> otherwise
     */
    public boolean matches(String uri) {
        if (uri == null) {
            return false;
        }
        if (!restful) {
            return uri.equals(uriTemplate);
        }
        Matcher matcher = this.matchPattern.matcher(uri);
        return matcher.matches();
    }

    /**
     * Match the given URI to a map of variable values. Keys in the returned map are variable names,
     * values are variable values, as occurred in the given URI.
     * <p>Example:
     * <pre class="code">
     * UriTemplate template = new UriTemplate("http://example.com/hotels/{hotel}/bookings/{booking}");
     * System.out.println(template.match("http://example.com/hotels/1/bookings/42"));
     * </pre>
     * will print: <blockquote><code>{hotel=1, booking=42}</code></blockquote>
     *
     * @param uri the URI to match to
     * @return a map of variable values
     */
    public Map<String, String> match(String uri) {
        if (!restful) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new LinkedHashMap<String, String>(this.variableNames.size());
        Matcher matcher = this.matchPattern.matcher(uri);
        if (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String name = this.variableNames.get(i - 1);
                String value = matcher.group(i);
                result.put(name, value);
//                try {
//                    result.put(name, URLDecoder.decode(value, "utf-8"));
//                } catch (Exception e) {
//
//                }
            }
        }
        return result;
    }


    @Override
    public String toString() {
        return this.uriTemplate;
    }


    /**
     * Static inner class to parse URI template strings into a matching regular expression.
     */
    private static class Parser {

        private final List<String> variableNames = new LinkedList<String>();

        private final StringBuilder patternBuilder = new StringBuilder();

        private Parser(String uriTemplate) {
            Matcher m = NAMES_PATTERN.matcher(uriTemplate);
            int end = 0;
            while (m.find()) {
                this.patternBuilder.append(quote(uriTemplate, end, m.start()));
                String match = m.group(1);
                int colonIdx = match.indexOf(':');
                if (colonIdx == -1) {
                    this.patternBuilder.append(DEFAULT_VARIABLE_PATTERN);
                    this.variableNames.add(match);
                } else {
                    if (colonIdx + 1 == match.length()) {
                        throw new IllegalArgumentException("No custom regular expression specified after ':' in \"" + match + "\"");
                    }
                    String variablePattern = match.substring(colonIdx + 1, match.length());
                    this.patternBuilder.append('(');
                    this.patternBuilder.append(variablePattern);
                    this.patternBuilder.append(')');
                    String variableName = match.substring(0, colonIdx);
                    this.variableNames.add(variableName);
                }
                end = m.end();
            }
            this.patternBuilder.append(quote(uriTemplate, end, uriTemplate.length()));
            int lastIdx = this.patternBuilder.length() - 1;
            if (lastIdx >= 0 && this.patternBuilder.charAt(lastIdx) == '/') {
                this.patternBuilder.deleteCharAt(lastIdx);
            }
        }

        private String quote(String fullPath, int start, int end) {
            if (start == end) {
                return "";
            }
            return Pattern.quote(fullPath.substring(start, end));
        }

        private List<String> getVariableNames() {
            return Collections.unmodifiableList(this.variableNames);
        }

        private Pattern getMatchPattern() {
            return Pattern.compile(this.patternBuilder.toString());
        }
    }

}
