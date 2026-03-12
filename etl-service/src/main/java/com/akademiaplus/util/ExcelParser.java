/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses Excel files (.xlsx, .xls) into a list of {@link ParsedSheet} objects.
 *
 * <p>Uses Apache POI to read workbook contents. Handles merged cells,
 * empty rows, and numeric/date cell types via {@link DataFormatter}.</p>
 */
@Component
public class ExcelParser {

    public static final String ERROR_CANNOT_READ = "Cannot read Excel file";
    public static final String ERROR_NO_SHEETS = "Excel file contains no sheets";
    public static final String ERROR_NO_HEADERS = "Sheet '%s' has no header row";

    private final DataFormatter dataFormatter = new DataFormatter();

    /**
     * Parses an Excel file from the given input stream.
     *
     * @param inputStream the file content
     * @param fileName    the original file name (for error context)
     * @return list of parsed sheets with headers and rows
     * @throws IllegalArgumentException if the file cannot be read or has no sheets
     */
    public List<ParsedSheet> parse(InputStream inputStream, String fileName) {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException(ERROR_NO_SHEETS);
            }

            List<ParsedSheet> sheets = new ArrayList<>();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                ParsedSheet parsed = parseSheet(sheet);
                if (parsed != null) {
                    sheets.add(parsed);
                }
            }
            return sheets;
        } catch (IOException e) {
            throw new IllegalArgumentException(ERROR_CANNOT_READ + ": " + fileName, e);
        }
    }

    private ParsedSheet parseSheet(Sheet sheet) {
        Row headerRow = sheet.getRow(sheet.getFirstRowNum());
        if (headerRow == null) {
            return null;
        }

        List<String> headers = extractHeaders(headerRow);
        if (headers.isEmpty()) {
            return null;
        }

        List<Map<String, String>> rows = new ArrayList<>();
        for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isEmptyRow(row)) {
                continue;
            }
            rows.add(extractRowData(row, headers));
        }

        return new ParsedSheet(sheet.getSheetName(), headers, rows);
    }

    private List<String> extractHeaders(Row headerRow) {
        List<String> headers = new ArrayList<>();
        for (int j = headerRow.getFirstCellNum(); j < headerRow.getLastCellNum(); j++) {
            Cell cell = headerRow.getCell(j);
            String value = (cell != null) ? dataFormatter.formatCellValue(cell).trim() : "";
            if (!value.isEmpty()) {
                headers.add(value);
            }
        }
        return headers;
    }

    private Map<String, String> extractRowData(Row row, List<String> headers) {
        Map<String, String> data = new LinkedHashMap<>();
        for (int j = 0; j < headers.size(); j++) {
            Cell cell = row.getCell(row.getFirstCellNum() + j);
            String value = (cell != null) ? dataFormatter.formatCellValue(cell).trim() : "";
            data.put(headers.get(j), value);
        }
        return data;
    }

    private boolean isEmptyRow(Row row) {
        for (int j = row.getFirstCellNum(); j < row.getLastCellNum(); j++) {
            Cell cell = row.getCell(j);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = dataFormatter.formatCellValue(cell).trim();
                if (!value.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
}
