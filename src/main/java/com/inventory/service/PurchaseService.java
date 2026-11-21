package com.inventory.service;

import com.inventory.dto.LineResponse;
import com.inventory.dto.PurchaseItemRequest;
import com.inventory.dto.PurchaseRequest;
import com.inventory.dto.PurchaseResponse;
import com.inventory.entity.Product;
import com.inventory.entity.Purchase;
import com.inventory.entity.PurchaseItem;
import com.inventory.entity.Supplier;
import com.inventory.exception.BusinessException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.PurchaseRepository;
import com.inventory.repository.SupplierRepository;
import com.inventory.util.Constants;
import com.inventory.util.CurrentUser;
import com.inventory.util.Money;
import com.inventory.util.Text;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final StockService stockService;
    private final LogService logService;

    public PurchaseService(PurchaseRepository purchaseRepository, SupplierRepository supplierRepository,
                           ProductRepository productRepository, StockService stockService, LogService logService) {
        this.purchaseRepository = purchaseRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.stockService = stockService;
        this.logService = logService;
    }

    @Transactional
    public PurchaseResponse create(PurchaseRequest request) {
        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new BusinessException("Selected supplier does not exist"));
        if (!Constants.ACTIVE.equals(supplier.getStatus())) {
            throw new BusinessException("Supplier '" + supplier.getName() + "' is inactive");
        }
        Purchase purchase = new Purchase();
        purchase.setPurchaseNumber("TMP-" + UUID.randomUUID().toString().substring(0, 20));
        purchase.setSupplier(supplier);
        purchase.setPurchaseDate(LocalDateTime.now());
        purchase.setStatus(Constants.COMPLETED);
        purchase.setCreatedBy(CurrentUser.email());

        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseItemRequest line : request.items()) {
            Product product = productRepository.findById(line.productId())
                    .orElseThrow(() -> new BusinessException("Product with id " + line.productId() + " does not exist"));
            PurchaseItem item = new PurchaseItem();
            item.setProduct(product);
            item.setQuantity(line.quantity());
            item.setPurchasePrice(Money.round(line.price()));
            item.setLineTotal(Money.multiply(line.price(), line.quantity()));
            purchase.addItem(item);
            total = total.add(item.getLineTotal());
        }
        purchase.setTotal(Money.round(total));

        Purchase saved = purchaseRepository.saveAndFlush(purchase);
        saved.setPurchaseNumber(String.format("PUR-%06d", saved.getId()));
        purchaseRepository.save(saved);
        for (PurchaseItem item : saved.getItems()) {
            stockService.change(item.getProduct(), item.getQuantity(), Constants.MOVE_PURCHASE, "PURCHASE",
                    saved.getId(), "Purchase " + saved.getPurchaseNumber());
            item.getProduct().setPurchasePrice(item.getPurchasePrice());
            productRepository.save(item.getProduct());
        }
        logService.log("PURCHASE_COMPLETED", "Purchase completed: " + saved.getPurchaseNumber() + " from "
                + supplier.getName() + ", total " + saved.getTotal());
        return toResponse(saved);
    }

    @Transactional
    public PurchaseResponse cancel(Long id) {
        Purchase purchase = find(id);
        if (Constants.CANCELLED.equals(purchase.getStatus())) {
            throw new BusinessException("This purchase is already cancelled");
        }
        for (PurchaseItem item : purchase.getItems()) {
            stockService.change(item.getProduct(), -item.getQuantity(), Constants.MOVE_REVERSAL, "PURCHASE",
                    purchase.getId(), "Purchase " + purchase.getPurchaseNumber() + " cancelled");
        }
        purchase.setStatus(Constants.CANCELLED);
        purchaseRepository.save(purchase);
        logService.log("PURCHASE_CANCELLED", "Purchase cancelled: " + purchase.getPurchaseNumber());
        return toResponse(purchase);
    }

    @Transactional(readOnly = true)
    public List<PurchaseResponse> search(String q, Long supplierId, LocalDate from, LocalDate to) {
        return purchaseRepository.findAllByOrderByIdDesc().stream()
                .filter(p -> supplierId == null || p.getSupplier().getId().equals(supplierId))
                .filter(p -> from == null || !p.getPurchaseDate().toLocalDate().isBefore(from))
                .filter(p -> to == null || !p.getPurchaseDate().toLocalDate().isAfter(to))
                .filter(p -> Text.contains(p.getPurchaseNumber(), q) || Text.contains(p.getSupplier().getName(), q))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PurchaseResponse> forSupplier(Long supplierId) {
        return purchaseRepository.findBySupplierIdOrderByIdDesc(supplierId).stream().map(this::toResponse).toList();
    }

    private Purchase find(Long id) {
        return purchaseRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Purchase not found"));
    }

    private PurchaseResponse toResponse(Purchase p) {
        List<LineResponse> lines = p.getItems().stream()
                .map(i -> new LineResponse(i.getProduct().getId(), i.getProduct().getProductCode(),
                        i.getProduct().getName(), i.getQuantity(), i.getPurchasePrice(), i.getLineTotal()))
                .toList();
        return new PurchaseResponse(p.getId(), p.getPurchaseNumber(), p.getSupplier().getId(), p.getSupplier().getName(),
                p.getPurchaseDate(), lines, p.getTotal(), p.getStatus(), p.getCreatedBy());
    }
}
