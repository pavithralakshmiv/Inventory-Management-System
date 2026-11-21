package com.inventory.service;

import com.inventory.dto.LineItemRequest;
import com.inventory.dto.LineResponse;
import com.inventory.dto.OrderRequest;
import com.inventory.dto.OrderResponse;
import com.inventory.entity.Customer;
import com.inventory.entity.CustomerOrder;
import com.inventory.entity.OrderItem;
import com.inventory.entity.Product;
import com.inventory.exception.BusinessException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.repository.CustomerRepository;
import com.inventory.repository.OrderRepository;
import com.inventory.repository.ProductRepository;
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
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final StockService stockService;
    private final LogService logService;

    public OrderService(OrderRepository orderRepository, CustomerRepository customerRepository,
                        ProductRepository productRepository, StockService stockService, LogService logService) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.stockService = stockService;
        this.logService = logService;
    }

    @Transactional
    public OrderResponse create(OrderRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new BusinessException("Selected customer does not exist"));
        if (!Constants.ACTIVE.equals(customer.getStatus())) {
            throw new BusinessException("Customer '" + customer.getName() + "' is inactive");
        }
        String paymentStatus = request.paymentStatus() == null || request.paymentStatus().isBlank()
                ? "Unpaid" : request.paymentStatus();
        if (!Constants.PAYMENT_STATUSES.contains(paymentStatus)) {
            throw new BusinessException("Invalid payment status");
        }
        BigDecimal discountPercent = discount(request.discountPercent());

        CustomerOrder order = new CustomerOrder();
        order.setOrderNumber("TMP-" + UUID.randomUUID().toString().substring(0, 20));
        order.setCustomer(customer);
        order.setOrderDate(LocalDateTime.now());
        order.setPaymentStatus(paymentStatus);
        order.setStatus(Constants.PENDING);
        order.setCreatedBy(CurrentUser.email());

        BigDecimal subtotal = BigDecimal.ZERO;
        Map<Product, Integer> quantities = new LinkedHashMap<>();
        for (Map.Entry<Long, Integer> line : merge(request.items()).entrySet()) {
            Product product = productRepository.findById(line.getKey())
                    .orElseThrow(() -> new BusinessException("Product with id " + line.getKey() + " does not exist"));
            if (!Constants.ACTIVE.equals(product.getStatus())) {
                throw new BusinessException("Product '" + product.getName() + "' is inactive");
            }
            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(line.getValue());
            item.setUnitPrice(product.getSellingPrice());
            item.setLineTotal(Money.multiply(product.getSellingPrice(), line.getValue()));
            order.addItem(item);
            subtotal = subtotal.add(item.getLineTotal());
            quantities.put(product, line.getValue());
        }
        BigDecimal discountAmount = Money.percentOf(subtotal, discountPercent);
        order.setSubtotal(Money.round(subtotal));
        order.setDiscountPercent(discountPercent);
        order.setDiscountAmount(discountAmount);
        order.setTotal(Money.round(subtotal.subtract(discountAmount)));

        CustomerOrder saved = orderRepository.saveAndFlush(order);
        saved.setOrderNumber(String.format("ORD-%06d", saved.getId()));
        orderRepository.save(saved);
        for (Map.Entry<Product, Integer> line : quantities.entrySet()) {
            stockService.change(line.getKey(), -line.getValue(), Constants.MOVE_ORDER, "ORDER", saved.getId(),
                    "Order " + saved.getOrderNumber());
        }
        logService.log("ORDER_CREATED", "Order created: " + saved.getOrderNumber() + " for " + customer.getName()
                + ", total " + saved.getTotal());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> search(String q, String status, LocalDate from, LocalDate to) {
        return orderRepository.findAllByOrderByIdDesc().stream()
                .filter(o -> status == null || status.isBlank() || o.getStatus().equalsIgnoreCase(status))
                .filter(o -> from == null || !o.getOrderDate().toLocalDate().isBefore(from))
                .filter(o -> to == null || !o.getOrderDate().toLocalDate().isAfter(to))
                .filter(o -> Text.contains(o.getOrderNumber(), q) || Text.contains(o.getCustomer().getName(), q)
                        || o.getItems().stream().anyMatch(i -> Text.contains(i.getProduct().getName(), q)))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> recent() {
        return orderRepository.findTop5ByOrderByIdDesc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public OrderResponse updateStatus(Long id, String status) {
        if (!Constants.ORDER_STATUSES.contains(status)) {
            throw new BusinessException("Invalid order status");
        }
        CustomerOrder order = find(id);
        if (Constants.CANCELLED.equals(order.getStatus())) {
            throw new BusinessException("A cancelled order cannot be changed");
        }
        if (Constants.CANCELLED.equals(status)) {
            for (OrderItem item : order.getItems()) {
                stockService.change(item.getProduct(), item.getQuantity(), Constants.MOVE_REVERSAL, "ORDER",
                        order.getId(), "Order " + order.getOrderNumber() + " cancelled");
            }
        }
        order.setStatus(status);
        orderRepository.save(order);
        logService.log("ORDER_STATUS_UPDATED", "Order " + order.getOrderNumber() + " status changed to " + status);
        return toResponse(order);
    }

    @Transactional
    public OrderResponse updatePayment(Long id, String paymentStatus) {
        if (!Constants.PAYMENT_STATUSES.contains(paymentStatus)) {
            throw new BusinessException("Invalid payment status");
        }
        CustomerOrder order = find(id);
        if (Constants.CANCELLED.equals(order.getStatus())) {
            throw new BusinessException("A cancelled order cannot be changed");
        }
        order.setPaymentStatus(paymentStatus);
        orderRepository.save(order);
        logService.log("ORDER_PAYMENT_UPDATED", "Order " + order.getOrderNumber() + " payment status changed to " + paymentStatus);
        return toResponse(order);
    }

    private CustomerOrder find(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    static Map<Long, Integer> merge(List<LineItemRequest> items) {
        Map<Long, Integer> merged = new LinkedHashMap<>();
        for (LineItemRequest item : items) {
            merged.merge(item.productId(), item.quantity(), Integer::sum);
        }
        return merged;
    }

    static BigDecimal discount(BigDecimal percent) {
        if (percent == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        if (percent.compareTo(BigDecimal.ZERO) < 0 || percent.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessException("Discount must be between 0 and 100 percent");
        }
        return Money.round(percent);
    }

    private OrderResponse toResponse(CustomerOrder o) {
        List<LineResponse> lines = o.getItems().stream()
                .map(i -> new LineResponse(i.getProduct().getId(), i.getProduct().getProductCode(),
                        i.getProduct().getName(), i.getQuantity(), i.getUnitPrice(), i.getLineTotal()))
                .toList();
        int quantity = lines.stream().mapToInt(LineResponse::quantity).sum();
        return new OrderResponse(o.getId(), o.getOrderNumber(), o.getCustomer().getId(), o.getCustomer().getName(),
                o.getOrderDate(), lines, quantity, o.getSubtotal(), o.getDiscountPercent(), o.getDiscountAmount(),
                o.getTotal(), o.getPaymentStatus(), o.getStatus(), o.getCreatedBy());
    }
}
