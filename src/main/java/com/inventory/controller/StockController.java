package com.inventory.controller;

import com.inventory.dto.StockMovementResponse;
import com.inventory.dto.StockResponse;
import com.inventory.service.StockService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stock")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping
    public List<StockResponse> list(@RequestParam(required = false) String q,
                                    @RequestParam(required = false) String status) {
        return stockService.list(q, status);
    }

    @GetMapping("/movements")
    public List<StockMovementResponse> movements(@RequestParam(required = false) Long productId) {
        return stockService.movements(productId);
    }
}
