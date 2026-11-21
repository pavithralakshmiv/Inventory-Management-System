package com.inventory.service;

import com.inventory.dto.ReportResult;
import com.inventory.entity.Customer;
import com.inventory.entity.CustomerOrder;
import com.inventory.entity.Product;
import com.inventory.entity.Purchase;
import com.inventory.entity.Sale;
import com.inventory.entity.Supplier;
import com.inventory.exception.BusinessException;
import com.inventory.repository.CategoryRepository;
import com.inventory.repository.CustomerRepository;
import com.inventory.repository.OrderRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.PurchaseRepository;
import com.inventory.repository.SaleRepository;
import com.inventory.repository.StockRepository;
import com.inventory.repository.SupplierRepository;
import com.inventory.util.Constants;
import com.inventory.util.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

    private final SaleRepository saleRepository;
    private final PurchaseRepository purchaseRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final StockRepository stockRepository;
    private final CategoryRepository categoryRepository;
    private final StockService stockService;

    public ReportService(SaleRepository saleRepository, PurchaseRepository purchaseRepository,
                         OrderRepository orderRepository, ProductRepository productRepository,
                         CustomerRepository customerRepository, SupplierRepository supplierRepository,
                         StockRepository stockRepository, CategoryRepository categoryRepository,
                         StockService stockService) {
        this.saleRepository = saleRepository;
        this.purchaseRepository = purchaseRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.supplierRepository = supplierRepository;
        this.stockRepository = stockRepository;
        this.categoryRepository = categoryRepository;
        this.stockService = stockService;
    }

    @Transactional(readOnly = true)
    public ReportResult generate(String type, LocalDate from, LocalDate to, String q) {
        ReportResult raw = switch (type.toLowerCase()) {
            case "sales" -> sales(from, to);
            case "purchases" -> purchases(from, to);
            case "stock" -> stock();
            case "products" -> products();
            case "customers" -> customers();
            case "suppliers" -> suppliers();
            case "orders" -> orders(from, to);
            case "summary" -> summary(from, to);
            default -> throw new BusinessException("Unknown report type: " + type);
        };
        if (q == null || q.isBlank()) {
            return raw;
        }
        String needle = q.trim().toLowerCase();
        List<List<Object>> filtered = raw.rows().stream()
                .filter(row -> row.stream().anyMatch(cell -> String.valueOf(cell).toLowerCase().contains(needle)))
                .toList();
        return new ReportResult(raw.type(), raw.title(), raw.columns(), filtered, raw.chartTitle(), raw.chartLabels(), raw.chartValues());
    }

    private boolean inRange(LocalDateTime date, LocalDate from, LocalDate to) {
        LocalDate day = date.toLocalDate();
        return (from == null || !day.isBefore(from)) && (to == null || !day.isAfter(to));
    }

    private ReportResult sales(LocalDate from, LocalDate to) {
        List<List<Object>> rows = new ArrayList<>();
        Map<String, BigDecimal> perDay = new LinkedHashMap<>();
        List<Sale> sales = saleRepository.findAllByOrderByIdDesc().stream().filter(s -> inRange(s.getSaleDate(), from, to)).toList();
        for (Sale s : sales) {
            rows.add(List.of(s.getInvoiceNumber(), s.getSaleDate().toLocalDate().toString(),
                    s.getCustomer() == null ? "Walk-in Customer" : s.getCustomer().getName(), s.getPaymentMethod(),
                    s.getSubtotal(), s.getDiscountAmount(), s.getTotal(), s.getProfit(), s.getStatus()));
            if (!Constants.CANCELLED.equals(s.getStatus())) {
                perDay.merge(s.getSaleDate().toLocalDate().toString(), s.getTotal(), BigDecimal::add);
            }
        }
        return build("sales", "Sales Report",
                List.of("Invoice", "Date", "Customer", "Payment", "Subtotal", "Discount", "Total", "Profit", "Status"),
                rows, "Sales total by day", perDay);
    }

    private ReportResult purchases(LocalDate from, LocalDate to) {
        List<List<Object>> rows = new ArrayList<>();
        Map<String, BigDecimal> perDay = new LinkedHashMap<>();
        List<Purchase> purchases = purchaseRepository.findAllByOrderByIdDesc().stream().filter(p -> inRange(p.getPurchaseDate(), from, to)).toList();
        for (Purchase p : purchases) {
            rows.add(List.of(p.getPurchaseNumber(), p.getPurchaseDate().toLocalDate().toString(), p.getSupplier().getName(),
                    p.getItems().stream().mapToInt(i -> i.getQuantity()).sum(), p.getTotal(), p.getStatus()));
            if (!Constants.CANCELLED.equals(p.getStatus())) {
                perDay.merge(p.getPurchaseDate().toLocalDate().toString(), p.getTotal(), BigDecimal::add);
            }
        }
        return build("purchases", "Purchase Report",
                List.of("Purchase No", "Date", "Supplier", "Items Qty", "Total", "Status"),
                rows, "Purchase total by day", perDay);
    }

    private ReportResult stock() {
        List<List<Object>> rows = new ArrayList<>();
        Map<String, BigDecimal> perProduct = new LinkedHashMap<>();
        stockRepository.findAll().stream().sorted((a, b) -> a.getProduct().getName().compareToIgnoreCase(b.getProduct().getName()))
                .forEach(s -> {
                    Product p = s.getProduct();
                    rows.add(List.of(p.getProductCode(), p.getName(), p.getCategory().getName(), s.getQuantity(),
                            p.getPurchasePrice(), p.getSellingPrice(), Money.multiply(p.getPurchasePrice(), s.getQuantity()),
                            stockService.statusOf(s.getQuantity())));
                    perProduct.put(p.getName(), BigDecimal.valueOf(s.getQuantity()));
                });
        return build("stock", "Stock Report",
                List.of("Code", "Product", "Category", "Quantity", "Purchase Price", "Selling Price", "Stock Value", "Status"),
                rows, "Quantity in stock by product", perProduct);
    }

    private ReportResult products() {
        List<List<Object>> rows = new ArrayList<>();
        for (Product p : productRepository.findAll()) {
            rows.add(List.of(p.getProductCode(), p.getName(), p.getCategory().getName(),
                    p.getSupplier() == null ? "-" : p.getSupplier().getName(), p.getBrand() == null ? "-" : p.getBrand(),
                    p.getPurchasePrice(), p.getSellingPrice(), Money.round(p.getSellingPrice().subtract(p.getPurchasePrice())),
                    p.getStatus()));
        }
        return build("products", "Product Report",
                List.of("Code", "Product", "Category", "Supplier", "Brand", "Purchase Price", "Selling Price", "Margin", "Status"),
                rows, null, null);
    }

    private ReportResult customers() {
        List<CustomerOrder> orders = orderRepository.findAll();
        List<List<Object>> rows = new ArrayList<>();
        for (Customer c : customerRepository.findAll()) {
            List<CustomerOrder> mine = orders.stream().filter(o -> o.getCustomer().getId().equals(c.getId())
                    && !Constants.CANCELLED.equals(o.getStatus())).toList();
            BigDecimal spent = mine.stream().map(CustomerOrder::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            rows.add(List.of(c.getId(), c.getName(), c.getMobile(), c.getEmail() == null ? "-" : c.getEmail(),
                    c.getAddress() == null ? "-" : c.getAddress(), c.getStatus(), mine.size(), Money.round(spent)));
        }
        return build("customers", "Customer Report",
                List.of("ID", "Name", "Mobile", "Email", "Address", "Status", "Orders", "Order Value"), rows, null, null);
    }

    private ReportResult suppliers() {
        List<Purchase> purchases = purchaseRepository.findAll();
        List<List<Object>> rows = new ArrayList<>();
        for (Supplier s : supplierRepository.findAll()) {
            List<Purchase> mine = purchases.stream().filter(p -> p.getSupplier().getId().equals(s.getId())
                    && !Constants.CANCELLED.equals(p.getStatus())).toList();
            BigDecimal total = mine.stream().map(Purchase::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            rows.add(List.of(s.getId(), s.getName(), s.getContactPerson() == null ? "-" : s.getContactPerson(),
                    s.getMobile() == null ? "-" : s.getMobile(), s.getEmail() == null ? "-" : s.getEmail(),
                    s.getStatus(), mine.size(), Money.round(total)));
        }
        return build("suppliers", "Supplier Report",
                List.of("ID", "Name", "Contact Person", "Mobile", "Email", "Status", "Purchases", "Purchase Value"), rows, null, null);
    }

    private ReportResult orders(LocalDate from, LocalDate to) {
        List<List<Object>> rows = new ArrayList<>();
        Map<String, BigDecimal> perStatus = new LinkedHashMap<>();
        List<CustomerOrder> orders = orderRepository.findAllByOrderByIdDesc().stream().filter(o -> inRange(o.getOrderDate(), from, to)).toList();
        for (CustomerOrder o : orders) {
            rows.add(List.of(o.getOrderNumber(), o.getOrderDate().toLocalDate().toString(), o.getCustomer().getName(),
                    o.getItems().stream().mapToInt(i -> i.getQuantity()).sum(), o.getTotal(), o.getPaymentStatus(), o.getStatus()));
            perStatus.merge(o.getStatus(), BigDecimal.ONE, BigDecimal::add);
        }
        return build("orders", "Order Report",
                List.of("Order No", "Date", "Customer", "Items Qty", "Total", "Payment", "Status"),
                rows, "Number of orders by status", perStatus);
    }

    private ReportResult summary(LocalDate from, LocalDate to) {
        List<Sale> sales = saleRepository.findAll().stream()
                .filter(s -> inRange(s.getSaleDate(), from, to) && !Constants.CANCELLED.equals(s.getStatus())).toList();
        List<Purchase> purchases = purchaseRepository.findAll().stream()
                .filter(p -> inRange(p.getPurchaseDate(), from, to) && !Constants.CANCELLED.equals(p.getStatus())).toList();
        List<CustomerOrder> orders = orderRepository.findAll().stream()
                .filter(o -> inRange(o.getOrderDate(), from, to) && !Constants.CANCELLED.equals(o.getStatus())).toList();
        BigDecimal salesTotal = sales.stream().map(Sale::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal profit = sales.stream().map(Sale::getProfit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal purchaseTotal = purchases.stream().map(Purchase::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal orderTotal = orders.stream().map(CustomerOrder::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal stockValue = stockRepository.findAll().stream()
                .map(s -> Money.multiply(s.getProduct().getPurchasePrice(), s.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<List<Object>> rows = new ArrayList<>();
        rows.add(List.of("Sales Count", sales.size()));
        rows.add(List.of("Total Sales", Money.round(salesTotal)));
        rows.add(List.of("Total Profit", Money.round(profit)));
        rows.add(List.of("Purchase Count", purchases.size()));
        rows.add(List.of("Total Purchases", Money.round(purchaseTotal)));
        rows.add(List.of("Order Count", orders.size()));
        rows.add(List.of("Total Order Value", Money.round(orderTotal)));
        rows.add(List.of("Total Products", productRepository.count()));
        rows.add(List.of("Total Categories", categoryRepository.count()));
        rows.add(List.of("Total Customers", customerRepository.count()));
        rows.add(List.of("Total Suppliers", supplierRepository.count()));
        rows.add(List.of("Stock Value (at purchase price)", Money.round(stockValue)));
        rows.add(List.of("Low / Out of Stock Items", stockService.lowStockCount()));
        Map<String, BigDecimal> chart = new LinkedHashMap<>();
        chart.put("Sales", Money.round(salesTotal));
        chart.put("Purchases", Money.round(purchaseTotal));
        chart.put("Profit", Money.round(profit));
        return build("summary", "Summary Report", List.of("Metric", "Value"), rows, "Sales vs Purchases vs Profit", chart);
    }

    private ReportResult build(String type, String title, List<String> columns, List<List<Object>> rows,
                               String chartTitle, Map<String, BigDecimal> chart) {
        if (chart == null || chart.isEmpty()) {
            return new ReportResult(type, title, columns, rows, null, List.of(), List.of());
        }
        return new ReportResult(type, title, columns, rows, chartTitle, new ArrayList<>(chart.keySet()),
                new ArrayList<>(chart.values()));
    }
}
