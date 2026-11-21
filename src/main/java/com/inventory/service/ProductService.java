package com.inventory.service;

import com.inventory.dto.ProductRequest;
import com.inventory.dto.ProductResponse;
import com.inventory.entity.Category;
import com.inventory.entity.Product;
import com.inventory.entity.Supplier;
import com.inventory.exception.BusinessException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.repository.CategoryRepository;
import com.inventory.repository.OrderItemRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.PurchaseItemRepository;
import com.inventory.repository.SaleItemRepository;
import com.inventory.repository.SupplierRepository;
import com.inventory.util.Constants;
import com.inventory.util.Text;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final OrderItemRepository orderItemRepository;
    private final SaleItemRepository saleItemRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final StockService stockService;
    private final LogService logService;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository,
                          SupplierRepository supplierRepository, OrderItemRepository orderItemRepository,
                          SaleItemRepository saleItemRepository, PurchaseItemRepository purchaseItemRepository,
                          StockService stockService, LogService logService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
        this.orderItemRepository = orderItemRepository;
        this.saleItemRepository = saleItemRepository;
        this.purchaseItemRepository = purchaseItemRepository;
        this.stockService = stockService;
        this.logService = logService;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> search(String q, Long categoryId) {
        return productRepository.search(q == null ? "" : q.trim(), categoryId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> recent() {
        return productRepository.findTop5ByOrderByIdDesc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        String code = request.productCode().trim().toUpperCase();
        if (productRepository.existsByProductCodeIgnoreCase(code)) {
            throw new BusinessException("Product code '" + code + "' already exists");
        }
        Product product = new Product();
        apply(product, request, code);
        Product saved = productRepository.save(product);
        stockService.createStock(saved, request.quantity());
        logService.log("PRODUCT_ADDED", "Product added: " + saved.getProductCode() + " - " + saved.getName());
        return toResponse(saved);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = find(id);
        String code = request.productCode().trim().toUpperCase();
        if (productRepository.existsByProductCodeIgnoreCaseAndIdNot(code, id)) {
            throw new BusinessException("Product code '" + code + "' already exists");
        }
        apply(product, request, code);
        Product saved = productRepository.save(product);
        int current = stockService.quantityOf(id);
        int difference = request.quantity() - current;
        if (difference != 0) {
            stockService.change(saved, difference, Constants.MOVE_ADJUSTMENT, "PRODUCT", id, "Quantity edited on product form");
        }
        logService.log("PRODUCT_UPDATED", "Product updated: " + saved.getProductCode() + " - " + saved.getName());
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Product product = find(id);
        if (orderItemRepository.existsByProductId(id) || saleItemRepository.existsByProductId(id)
                || purchaseItemRepository.existsByProductId(id)) {
            throw new BusinessException("Product '" + product.getName() + "' has order, sale or purchase history and cannot be deleted. Set it to Inactive instead.");
        }
        stockService.deleteFor(id);
        productRepository.delete(product);
        productRepository.flush();
        logService.log("PRODUCT_DELETED", "Product deleted: " + product.getProductCode() + " - " + product.getName());
    }

    public Product find(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    public ProductResponse toResponse(Product p) {
        int quantity = stockService.quantityOf(p.getId());
        Supplier supplier = p.getSupplier();
        return new ProductResponse(p.getId(), p.getProductCode(), p.getName(), p.getCategory().getId(),
                p.getCategory().getName(), supplier == null ? null : supplier.getId(),
                supplier == null ? null : supplier.getName(), p.getImagePath(), p.getPurchasePrice(),
                p.getSellingPrice(), quantity, p.getBrand(), p.getProductDate(), p.getDescription(), p.getStatus(),
                stockService.statusOf(quantity));
    }

    private void apply(Product product, ProductRequest request, String code) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new BusinessException("Selected category does not exist"));
        Supplier supplier = null;
        if (request.supplierId() != null) {
            supplier = supplierRepository.findById(request.supplierId())
                    .orElseThrow(() -> new BusinessException("Selected supplier does not exist"));
        }
        product.setProductCode(code);
        product.setName(request.name().trim());
        product.setCategory(category);
        product.setSupplier(supplier);
        product.setImagePath(Text.clean(request.imagePath()));
        product.setPurchasePrice(request.purchasePrice());
        product.setSellingPrice(request.sellingPrice());
        product.setBrand(Text.clean(request.brand()));
        product.setProductDate(request.productDate() == null ? LocalDate.now() : request.productDate());
        product.setDescription(Text.clean(request.description()));
        product.setStatus(request.status() == null || request.status().isBlank()
                ? Constants.ACTIVE : CategoryService.normalizeStatus(request.status()));
    }
}
