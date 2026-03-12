/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses Word documents (.docx) by extracting tables as {@link ParsedSheet} objects.
 *
 * <p>Each table found in the document becomes a separate sheet.
 * The first row of each table is treated as the header row.</p>
 */
@Component
public class WordParser {

    public static final String ERROR_CANNOT_READ = "Cannot read Word file";
    public static final String ERROR_NO_TABLES = "Word document contains no tables";
    public static final String TABLE_NAME_PREFIX = "Table ";

    /**
     * Parses a Word document from the given input stream.
     *
     * @param inputStream the file content
     * @param fileName    the original file name (for error context)
     * @return list of parsed sheets (one per table)
     * @throws IllegalArgumentException if the file cannot be read or contains no tables
     */
    public List<ParsedSheet> parse(InputStream inputStream, String fileName) {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            List<XWPFTable> tables = document.getTables();
            if (tables.isEmpty()) {
                throw new IllegalArgumentException(ERROR_NO_TABLES);
            }

            List<ParsedSheet> sheets = new ArrayList<>();
            for (int i = 0; i < tables.size(); i++) {
                ParsedSheet parsed = parseTable(tables.get(i), TABLE_NAME_PREFIX + (i + 1));
                if (parsed != null) {
                    sheets.add(parsed);
                }
            }
            return sheets;
        } catch (IOException e) {
            throw new IllegalArgumentException(ERROR_CANNOT_READ + ": " + fileName, e);
        }
    }

    private ParsedSheet parseTable(XWPFTable table, String tableName) {
        List<XWPFTableRow> tableRows = table.getRows();
        if (tableRows.isEmpty()) {
            return null;
        }

        List<String> headers = extractHeaders(tableRows.getFirst());
        if (headers.isEmpty()) {
            return null;
        }

        List<Map<String, String>> rows = new ArrayList<>();
        for (int i = 1; i < tableRows.size(); i++) {
            rows.add(extractRowData(tableRows.get(i), headers));
        }

        return new ParsedSheet(tableName, headers, rows);
    }

    private List<String> extractHeaders(XWPFTableRow headerRow) {
        List<String> headers = new ArrayList<>();
        headerRow.getTableCells().forEach(cell -> {
            String text = cell.getText().trim();
            if (!text.isEmpty()) {
                headers.add(text);
            }
        });
        return headers;
    }

    private Map<String, String> extractRowData(XWPFTableRow row, List<String> headers) {
        Map<String, String> data = new LinkedHashMap<>();
        var cells = row.getTableCells();
        for (int j = 0; j < headers.size(); j++) {
            String value = (j < cells.size()) ? cells.get(j).getText().trim() : "";
            data.put(headers.get(j), value);
        }
        return data;
    }
}
