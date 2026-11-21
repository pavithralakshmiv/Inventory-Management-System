package com.inventory.service;

import com.inventory.dto.LineResponse;
import com.inventory.dto.SaleRequest;
import com.inventory.dto.SaleResponse;
import com.inventory.entity.Customer;
import com.inventory.entity.Product;
import com.inventory.entity.Sale;
import com.inventory.entity.SaleItem;
import com.inventory.exception.BusinessException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.repository.CustomerRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.SaleRepository;
import com.inventory.util.Constants;
import com.inventory.util.CurrentUser;
import com.inventory.util.Money;
import com.inventory.util.Text;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final StockService stockService;
    private final LogService logService;

    public SaleService(SaleRepository saleRepository, CustomerRepository customerRepository,
                       ProductRepository productRepository, StockService stockService, LogService logService) {
        this.saleRepository = saleRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.stockService = stockService;
        this.logService = logService;
    }

    @Transactional
    public SaleResponse create(SaleRequest request) {
        if (!Constants.PAYMENT_METHODS.contains(request.paymentMethod())) {
            throw new BusinessException("Invalid payment method");
        }
        Customer customer = null;
        if (request.customerId() != null) {
            customer = customerRepository.findById(request.customerId())
                    .orElseThrow(() -> new BusinessException("Selected customer does not exist"));
        }
        BigDecimal discountPercent = OrderService.discount(request.discountPercent());

        Sale sale = new Sale();
        sale.setInvoiceNumber("TMP-" + UUID.randomUUID().toString().substring(0, 20));
        sale.setCustomer(customer);
        sale.setSaleDate(LocalDateTime.now());
        sale.setPaymentMethod(request.paymentMethod());
        sale.setStatus(Constants.COMPLETED);
        sale.setCreatedBy(CurrentUser.email());

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal cost = BigDecimal.ZERO;
        Map<Product, Integer> quantities = new LinkedHashMap<>();
        for (Map.Entry<Long, Integer> line : OrderService.merge(request.items()).entrySet()) {
            Product product = productRepository.findById(line.getKey())
                    .orElseThrow(() -> new BusinessException("Product with id " + line.getKey() + " does not exist"));
            if (!Constants.ACTIVE.equals(product.getStatus())) {
                throw new BusinessException("Product '" + product.getName() + "' is inactive");
            }
            SaleItem item = new SaleItem();
            item.setProduct(product);
            item.setQuantity(line.getValue());
            item.setUnitPrice(product.getSellingPrice());
            item.setCostPrice(product.getPurchasePrice());
            item.setLineTotal(Money.multiply(product.getSellingPrice(), line.getValue()));
            sale.addItem(item);
            subtotal = subtotal.add(item.getLineTotal());
            cost = cost.add(Money.multiply(product.getPurchasePrice(), line.getValue()));
            quantities.put(product, line.getValue());
        }
        BigDecimal discountAmount = Money.percentOf(subtotal, discountPercent);
        BigDecimal total = Money.round(subtotal.subtract(discountAmount));
        sale.setSubtotal(Money.round(subtotal));
        sale.setDiscountPercent(discountPercent);
        sale.setDiscountAmount(discountAmount);
        sale.setTotal(total);
        sale.setProfit(Money.round(total.subtract(cost)));

        Sale saved = saleRepository.saveAndFlush(sale);
        saved.setInvoiceNumber(String.format("INV-%06d", saved.getId()));
        saleRepository.save(saved);
        for (Map.Entry<Product, Integer> line : quantities.entrySet()) {
            stockService.change(line.getKey(), -line.getValue(), Constants.MOVE_SALE, "SALE", saved.getId(),
                    "Sale " + saved.getInvoiceNumber());
        }
        logService.log("SALE_COMPLETED", "Sale completed: " + saved.getInvoiceNumber() + ", total " + saved.getTotal());
        return toResponse(saved);
    }

    @Transactional
    public SaleResponse cancel(Long id) {
        Sale sale = find(id);
        if (Constants.CANCELLED.equals(sale.getStatus())) {
            throw new BusinessException("This sale is already cancelled");
        }
        for (SaleItem item : sale.getItems()) {
            stockService.change(item.getProduct(), item.getQuantity(), Constants.MOVE_REVERSAL, "SALE", sale.getId(),
                    "Sale " + sale.getInvoiceNumber() + " cancelled");
        }
        sale.setStatus(Constants.CANCELLED);
        saleRepository.save(sale);
        logService.log("SALE_CANCELLED", "Sale cancelled: " + sale.getInvoiceNumber());
        return toResponse(sale);
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> search(String q, LocalDate from, LocalDate to) {
        return saleRepository.findAllByOrderByIdDesc().stream()
                .filter(s -> from == null || !s.getSaleDate().toLocalDate().isBefore(from))
                .filter(s -> to == null || !s.getSaleDate().toLocalDate().isAfter(to))
                .filter(s -> Text.contains(s.getInvoiceNumber(), q)
                        || Text.contains(s.getCustomer() == null ? "Walk-in" : s.getCustomer().getName(), q))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SaleResponse get(Long id) {
        return toResponse(find(id));
    }

    private Sale find(Long id) {
        return saleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Sale not found"));
    }

    private SaleResponse toResponse(Sale s) {
        List<LineResponse> lines = s.getItems().stream()
                .map(i -> new LineResponse(i.getProduct().getId(), i.getProduct().getProductCode(),
                        i.getProduct().getName(), i.getQuantity(), i.getUnitPrice(), i.getLineTotal()))
                .toList();
        Customer c = s.getCustomer();
        return new SaleResponse(s.getId(), s.getInvoiceNumber(), c == null ? null : c.getId(),
                c == null ? "Walk-in Customer" : c.getName(), s.getSaleDate(), lines, s.getSubtotal(),
                s.getDiscountPercent(), s.getDiscountAmount(), s.getTotal(), s.getProfit(), s.getPaymentMethod(),
                s.getStatus(), s.getCreatedBy());
    }
}
