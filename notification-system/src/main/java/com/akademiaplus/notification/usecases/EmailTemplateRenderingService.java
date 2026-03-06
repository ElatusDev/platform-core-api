/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple template rendering service that replaces {@code {{variableName}}}
 * placeholders with provided variable values.
 * <p>
 * Uses regex-based replacement. Variables not found in the provided map
 * are left as-is in the output. Future versions may support conditionals,
 * loops, or delegate to a full template engine like Thymeleaf.
 */
@Service
public class EmailTemplateRenderingService {

    /** Regex pattern for matching {{variableName}} placeholders. */
    public static final String VARIABLE_PATTERN = "\\{\\{(\\w+)\\}\\}";

    /** Compiled pattern instance for variable matching. */
    public static final Pattern COMPILED_PATTERN = Pattern.compile(VARIABLE_PATTERN);

    /**
     * Renders a template string by replacing {@code {{variableName}}} placeholders
     * with values from the provided variable map.
     * <p>
     * Variables not present in the map are left unchanged in the output.
     * All variable values are converted to strings via {@link Object#toString()}.
     *
     * @param template  the template string containing {{variable}} placeholders
     * @param variables the variable name-to-value map for substitution
     * @return the rendered string with placeholders replaced
     */
    public String render(String template, Map<String, Object> variables) {
        if (template == null || variables == null || variables.isEmpty()) {
            return template;
        }

        Matcher matcher = COMPILED_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object value = variables.get(variableName);
            String replacement = value != null ? Matcher.quoteReplacement(value.toString()) : matcher.group(0);
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);

        return result.toString();
    }
}
