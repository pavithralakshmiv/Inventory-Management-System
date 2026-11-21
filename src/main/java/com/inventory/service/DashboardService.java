package com.inventory.service;

import com.inventory.dto.DashboardResponse;
import com.inventory.repository.CustomerRepository;
import com.inventory.repository.OrderRepository;
import com.inventory.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final StockService stockService;
    private final ProductService productService;
    private final OrderService orderService;
    private final LogService logService;

    public DashboardService(ProductRepository productRepository, CustomerRepository customerRepository,
                            OrderRepository orderRepository, StockService stockService,
                            ProductService productService, OrderService orderService, LogService logService) {
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.stockService = stockService;
        this.productService = productService;
        this.orderService = orderService;
        this.logService = logService;
    }

    @Transactional(readOnly = true)
    public DashboardResponse load() {
        return new DashboardResponse(productRepository.count(), customerRepository.count(), orderRepository.count(),
                stockService.lowStockCount(), stockService.quantityByCategory(), logService.recent(),
                productService.recent(), orderService.recent());
    }
}
