package com.hyperlocal.tantra.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Sort;

import java.util.Map;

/**
 * Shared helpers for listing query params: attribute filter extraction and sort normalization.
 *
 * Attribute filtering contract:
 *   Frontend sends attr_<attributeKey>=<value> for each selected attribute.
 *   e.g. attr_cropType=PULSE&attr_variety=BASMATI
 *   We build a JSONB object {"cropType":"PULSE","variety":"BASMATI"} and match via
 *   PostgreSQL's @> (contains) operator against the listing's attributes column.
 *   Multiple attrs = AND (all must be present with the given value).
 *
 * Sort contract:
 *   Supported sort fields: offeredPrice, createdAt (also accepted: offered_price, created_at).
 *   Supported directions: asc, desc (default: desc).
 *   Accepts both explicit ?sortBy=offeredPrice&sortDir=asc and Spring-style ?sort=offeredPrice,asc.
 */
public final class ListingQueryUtil {

    /** Allowed sort fields — whitelist prevents SQL injection via sort param. */
    private static final String SORT_OFFERED_PRICE = "offeredPrice";
    private static final String SORT_CREATED_AT    = "createdAt";

    private ListingQueryUtil() {}

    // ─── Attribute filter ─────────────────────────────────────────────────────

    /**
     * Extracts attr_* query params and builds a PostgreSQL JSONB filter string.
     *
     * attr_cropType=PULSE  +  attr_variety=BASMATI
     *   → {"cropType":"PULSE","variety":"BASMATI"}
     *
     * Returns null when no valid attr_* params are present — null means no attribute
     * filtering and the SQL (:attrFilter IS NULL OR ...) condition is a no-op.
     */
    public static String buildAttrFilter(HttpServletRequest request) {
        Map<String, String[]> params = request.getParameterMap();
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            String paramName = entry.getKey();
            if (!paramName.startsWith("attr_") || paramName.length() <= 5) continue;
            String[] values = entry.getValue();
            if (values == null || values.length == 0
                    || values[0] == null || values[0].isBlank()) continue;
            String attrKey   = escapeJson(paramName.substring(5).trim());
            String attrValue = escapeJson(values[0].trim());
            if (!first) json.append(",");
            json.append("\"").append(attrKey).append("\":\"").append(attrValue).append("\"");
            first = false;
        }
        json.append("}");
        return first ? null : json.toString();
    }

    // ─── Sort normalization ───────────────────────────────────────────────────

    /**
     * Normalises a sort field name to one of the two DB-safe values,
     * or null if the input is unrecognised (= default subscription sort applies).
     * Both camelCase and snake_case variants are accepted.
     */
    public static String normalizeSortBy(String sortBy) {
        if (sortBy == null) return null;
        if ("offeredPrice".equalsIgnoreCase(sortBy)
                || "offered_price".equalsIgnoreCase(sortBy)) return SORT_OFFERED_PRICE;
        if ("createdAt".equalsIgnoreCase(sortBy)
                || "created_at".equalsIgnoreCase(sortBy))    return SORT_CREATED_AT;
        return null; // unknown field → ignore, use default
    }

    /** Normalises sort direction to "asc" or "desc" (default "desc"). */
    public static String normalizeSortDir(String sortDir) {
        return "asc".equalsIgnoreCase(sortDir) ? "asc" : "desc";
    }

    /**
     * Resolves the effective sortBy/sortDir pair. Explicit params win;
     * Spring-style Pageable sort (from ?sort=field,dir) is the fallback.
     * Returns a two-element array: [sortBy, sortDir], either of which may be null.
     */
    public static String[] resolveSort(String explicitSortBy, String explicitSortDir, Sort pageableSort) {
        String sortBy  = normalizeSortBy(explicitSortBy);
        String sortDir = normalizeSortDir(explicitSortDir);

        // If explicit sortBy was unrecognised or absent, try Spring-style Pageable sort
        if (sortBy == null && pageableSort != null && pageableSort.isSorted()) {
            Sort.Order firstOrder = pageableSort.iterator().next();
            sortBy  = normalizeSortBy(firstOrder.getProperty());
            sortDir = firstOrder.isAscending() ? "asc" : "desc";
        }
        return new String[]{sortBy, sortDir};
    }

    // ─── JSON escaping ────────────────────────────────────────────────────────

    /** Minimal JSON string escaping — prevents injection in attr keys/values. */
    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
