package com.inventory.controller;

import com.inventory.dto.ReportResult;
import com.inventory.exception.BusinessException;
import com.inventory.service.ExportService;
import com.inventory.service.LogService;
import com.inventory.service.ReportService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final ExportService exportService;
    private final LogService logService;

    public ReportController(ReportService reportService, ExportService exportService, LogService logService) {
        this.reportService = reportService;
        this.exportService = exportService;
        this.logService = logService;
    }

    @GetMapping("/{type}")
    public ReportResult report(@PathVariable String type,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                               @RequestParam(required = false) String q) {
        return reportService.generate(type, from, to, q);
    }

    @GetMapping("/{type}/export")
    public ResponseEntity<byte[]> export(@PathVariable String type,
                                         @RequestParam String format,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                         @RequestParam(required = false) String q) {
        ReportResult report = reportService.generate(type, from, to, q);
        String base = "report-" + type.toLowerCase() + "-" + LocalDate.now();
        logService.log("REPORT_EXPORTED", report.title() + " exported as " + format.toUpperCase());
        if ("pdf".equalsIgnoreCase(format)) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + base + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(exportService.toPdf(report));
        }
        if ("xlsx".equalsIgnoreCase(format) || "excel".equalsIgnoreCase(format)) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + base + ".xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(exportService.toExcel(report));
        }
        throw new BusinessException("Export format must be pdf or xlsx");
    }
}
