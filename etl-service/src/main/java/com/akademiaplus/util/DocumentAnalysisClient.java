/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util;

import com.akademiaplus.domain.ColumnMapping;
import com.akademiaplus.domain.MigrationEntityType;
import com.akademiaplus.domain.SheetAnalysis;
import com.akademiaplus.domain.TransformType;
import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Calls Claude API to analyze uploaded document structure and suggest column mappings.
 *
 * <p>Sends document headers and sample rows to Claude for entity type detection
 * and column-to-field mapping suggestions. Uses {@code claude-haiku-4-5} by default
 * for cost efficiency.</p>
 */
@Component
public class DocumentAnalysisClient {

    private static final Logger log = LoggerFactory.getLogger(DocumentAnalysisClient.class);

    public static final int MAX_SAMPLE_ROWS = 15;
    public static final String ERROR_API_CALL = "Claude API call failed";

    @Value("${anthropic.api-key:}")
    private String apiKey;

    @Value("${anthropic.model:claude-haiku-4-5}")
    private String model;

    @Value("classpath:prompts/analyze-system.txt")
    private Resource systemPromptResource;

    @Value("classpath:prompts/analyze-user.txt")
    private Resource userPromptResource;

    private final SchemaContextBuilder schemaContextBuilder;
    private final ObjectMapper objectMapper;
    private AnthropicClient client;
    private String systemPrompt;
    private String userPromptTemplate;

    /**
     * Creates a new DocumentAnalysisClient.
     *
     * @param schemaContextBuilder the schema context builder for prompt construction
     */
    public DocumentAnalysisClient(SchemaContextBuilder schemaContextBuilder) {
        this.schemaContextBuilder = schemaContextBuilder;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Initializes the Anthropic client and loads prompt templates after properties are injected.
     */
    @PostConstruct
    public void init() {
        if (apiKey != null && !apiKey.isBlank()) {
            this.client = AnthropicOkHttpClient.builder()
                    .apiKey(apiKey)
                    .build();
        }
        this.systemPrompt = loadResource(systemPromptResource);
        this.userPromptTemplate = loadResource(userPromptResource);
    }

    private String loadResource(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load prompt resource: " + resource.getFilename(), e);
        }
    }

    /**
     * Analyzes document structure using Claude API.
     *
     * @param fileName the source file name
     * @param sheets   parsed sheets with headers and rows
     * @param hint     optional entity type hint (null for auto-detection)
     * @return list of sheet analyses with entity type detection and column mappings
     */
    public List<SheetAnalysis> analyzeDocument(String fileName, List<ParsedSheet> sheets,
                                                MigrationEntityType hint) {
        if (client == null) {
            log.warn("Anthropic API key not configured — returning empty analysis");
            return createEmptyAnalyses(sheets);
        }

        String userPrompt = buildUserPrompt(fileName, sheets, hint);

        try {
            MessageCreateParams params = MessageCreateParams.builder()
                    .model(model)
                    .maxTokens(4096L)
                    .system(systemPrompt)
                    .addUserMessage(userPrompt)
                    .build();

            Message response = client.messages().create(params);

            String responseText = response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(textBlock -> textBlock.text())
                    .collect(Collectors.joining());

            return parseAnalysisResponse(responseText);
        } catch (RuntimeException e) {
            log.error("Claude API analysis failed: {}", e.getMessage(), e);
            return createEmptyAnalyses(sheets);
        }
    }

    private String buildUserPrompt(String fileName, List<ParsedSheet> sheets,
                                    MigrationEntityType hint) {
        String schemas = schemaContextBuilder.buildContext(hint);

        StringBuilder sheetsBlock = new StringBuilder();
        for (ParsedSheet sheet : sheets) {
            sheetsBlock.append("- Sheet \"").append(sheet.name()).append("\": ");
            sheetsBlock.append("headers=").append(sheet.headers()).append(", ");
            sheetsBlock.append("sample rows=");

            List<Map<String, String>> sampleRows = sheet.rows().size() > MAX_SAMPLE_ROWS
                    ? sheet.rows().subList(0, MAX_SAMPLE_ROWS)
                    : sheet.rows();
            sheetsBlock.append(sampleRows).append("\n");
        }

        String hintText = hint != null ? "\nEntity type hint: " + hint.name() : "";

        return userPromptTemplate
                .replace("{{schemas}}", schemas)
                .replace("{{fileName}}", fileName)
                .replace("{{sheets}}", sheetsBlock.toString())
                .replace("{{hint}}", hintText);
    }

    private List<SheetAnalysis> parseAnalysisResponse(String responseText) {
        try {
            String jsonText = extractJsonArray(responseText);
            ArrayNode jsonArray = (ArrayNode) objectMapper.readTree(jsonText);
            List<SheetAnalysis> analyses = new ArrayList<>();

            for (JsonNode node : jsonArray) {
                analyses.add(parseSheetAnalysis(node));
            }
            return analyses;
        } catch (Exception e) {
            log.error("Failed to parse Claude analysis response: {}", e.getMessage(), e);
            return List.of();
        }
    }

    private SheetAnalysis parseSheetAnalysis(JsonNode node) {
        String sheetName = node.path("sheetName").asText("");
        String entityType = node.path("detectedEntityType").asText(null);
        double confidence = node.path("confidence").asDouble(0.0);

        List<ColumnMapping> mappings = new ArrayList<>();
        JsonNode mappingsNode = node.path("columnMappings");
        if (mappingsNode.isArray()) {
            for (JsonNode m : mappingsNode) {
                String source = m.path("source").asText("");
                String target = m.path("target").asText("");
                String transform = m.path("transform").asText("NONE");
                TransformType transformType = parseTransformType(transform);
                mappings.add(new ColumnMapping(source, target, transformType));
            }
        }

        List<String> warnings = jsonArrayToStringList(node.path("warnings"));
        List<String> unmapped = jsonArrayToStringList(node.path("unmappedSourceColumns"));
        List<String> missing = jsonArrayToStringList(node.path("missingRequiredFields"));

        return new SheetAnalysis(sheetName, entityType, confidence, mappings,
                warnings, unmapped, missing);
    }

    private TransformType parseTransformType(String value) {
        try {
            return TransformType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return TransformType.NONE;
        }
    }

    private List<String> jsonArrayToStringList(JsonNode arrayNode) {
        List<String> result = new ArrayList<>();
        if (arrayNode.isArray()) {
            for (JsonNode item : arrayNode) {
                result.add(item.asText());
            }
        }
        return result;
    }

    private String extractJsonArray(String text) {
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private List<SheetAnalysis> createEmptyAnalyses(List<ParsedSheet> sheets) {
        return sheets.stream()
                .map(sheet -> new SheetAnalysis(
                        sheet.name(), null, 0.0,
                        List.of(), List.of("Analysis unavailable — configure API key"),
                        List.of(), List.of()))
                .toList();
    }
}
