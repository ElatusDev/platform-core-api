/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util;

import java.util.List;
import java.util.Map;

/**
 * Represents a parsed sheet or table extracted from an uploaded document.
 *
 * @param name    the sheet name (for Excel) or "Table N" (for Word tables)
 * @param headers column header names in order
 * @param rows    list of rows, each mapping header name to cell value
 */
public record ParsedSheet(
        String name,
        List<String> headers,
        List<Map<String, String>> rows
) {
}
