package com.inventory.service;

import com.inventory.dto.PurchaseResponse;
import com.inventory.dto.SupplierRequest;
import com.inventory.entity.Supplier;
import com.inventory.exception.BusinessException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.PurchaseRepository;
import com.inventory.repository.SupplierRepository;
import com.inventory.util.Text;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final PurchaseRepository purchaseRepository;
    private final PurchaseService purchaseService;
    private final LogService logService;

    public SupplierService(SupplierRepository supplierRepository, ProductRepository productRepository,
                           PurchaseRepository purchaseRepository, PurchaseService purchaseService, LogService logService) {
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.purchaseRepository = purchaseRepository;
        this.purchaseService = purchaseService;
        this.logService = logService;
    }

    @Transactional(readOnly = true)
    public List<Supplier> search(String q) {
        return supplierRepository.search(q == null ? "" : q.trim());
    }

    @Transactional(readOnly = true)
    public Supplier get(Long id) {
        return supplierRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
    }

    @Transactional
    public Supplier create(SupplierRequest request) {
        String name = request.name().trim();
        if (supplierRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException("A supplier named '" + name + "' already exists");
        }
        Supplier supplier = new Supplier();
        apply(supplier, request);
        Supplier saved = supplierRepository.save(supplier);
        logService.log("SUPPLIER_ADDED", "Supplier added: " + saved.getName());
        return saved;
    }

    @Transactional
    public Supplier update(Long id, SupplierRequest request) {
        Supplier supplier = get(id);
        String name = request.name().trim();
        if (supplierRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BusinessException("A supplier named '" + name + "' already exists");
        }
        apply(supplier, request);
        Supplier saved = supplierRepository.save(supplier);
        logService.log("SUPPLIER_UPDATED", "Supplier updated: " + saved.getName());
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        Supplier supplier = get(id);
        if (productRepository.existsBySupplierId(id) || purchaseRepository.existsBySupplierId(id)) {
            throw new BusinessException("Supplier '" + supplier.getName() + "' is linked to products or purchases and cannot be deleted. Set it to Inactive instead.");
        }
        supplierRepository.delete(supplier);
        supplierRepository.flush();
        logService.log("SUPPLIER_DELETED", "Supplier deleted: " + supplier.getName());
    }

    @Transactional(readOnly = true)
    public List<PurchaseResponse> purchaseHistory(Long id) {
        get(id);
        return purchaseService.forSupplier(id);
    }

    private void apply(Supplier supplier, SupplierRequest request) {
        supplier.setName(request.name().trim());
        supplier.setContactPerson(Text.clean(request.contactPerson()));
        supplier.setMobile(Text.clean(request.mobile()));
        supplier.setEmail(Text.email(request.email()));
        supplier.setAddress(Text.clean(request.address()));
        supplier.setStatus(CategoryService.normalizeStatus(request.status()));
    }
}
