package com.inventory.dto;

import java.util.List;

public record ReportResult(String type, String title, List<String> columns, List<List<Object>> rows,
                           String chartTitle, List<String> chartLabels, List<Number> chartValues) {
}
