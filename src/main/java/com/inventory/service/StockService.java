package com.inventory.service;

import com.inventory.dto.ChartPoint;
import com.inventory.dto.StockMovementResponse;
import com.inventory.dto.StockResponse;
import com.inventory.entity.Product;
import com.inventory.entity.Stock;
import com.inventory.entity.StockMovement;
import com.inventory.exception.BusinessException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.repository.StockMovementRepository;
import com.inventory.repository.StockRepository;
import com.inventory.util.Constants;
import com.inventory.util.CurrentUser;
import com.inventory.util.Text;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {

    private final StockRepository stockRepository;
    private final StockMovementRepository movementRepository;
    private final SettingService settingService;

    public StockService(StockRepository stockRepository, StockMovementRepository movementRepository,
                        SettingService settingService) {
        this.stockRepository = stockRepository;
        this.movementRepository = movementRepository;
        this.settingService = settingService;
    }

    @Transactional
    public void createStock(Product product, int openingQuantity) {
        Stock stock = new Stock();
        stock.setProduct(product);
        stock.setQuantity(openingQuantity);
        stockRepository.save(stock);
        if (openingQuantity > 0) {
            recordMovement(product, Constants.MOVE_OPENING, openingQuantity, openingQuantity,
                    "PRODUCT", product.getId(), "Opening stock");
        }
    }

    @Transactional
    public int change(Product product, int delta, String movementType, String referenceType, Long referenceId, String note) {
        Stock stock = stockRepository.findForUpdate(product.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Stock record not found for " + product.getName()));
        int newQuantity = stock.getQuantity() + delta;
        if (newQuantity < 0) {
            throw new BusinessException("Insufficient stock for " + product.getName() + ". Available: "
                    + stock.getQuantity() + ", required: " + Math.abs(delta));
        }
        stock.setQuantity(newQuantity);
        stockRepository.save(stock);
        recordMovement(product, movementType, delta, newQuantity, referenceType, referenceId, note);
        return newQuantity;
    }

    @Transactional(readOnly = true)
    public int quantityOf(Long productId) {
        return stockRepository.findByProductId(productId).map(Stock::getQuantity).orElse(0);
    }

    @Transactional
    public void deleteFor(Long productId) {
        movementRepository.deleteByProductId(productId);
        stockRepository.findByProductId(productId).ifPresent(stockRepository::delete);
    }

    public String statusOf(int quantity) {
        if (quantity <= 0) {
            return Constants.STOCK_OUT;
        }
        if (quantity <= settingService.lowStockThreshold()) {
            return Constants.STOCK_LOW;
        }
        return Constants.STOCK_AVAILABLE;
    }

    @Transactional(readOnly = true)
    public List<StockResponse> list(String q, String status) {
        return stockRepository.findAll().stream()
                .map(s -> new StockResponse(s.getProduct().getId(), s.getProduct().getProductCode(),
                        s.getProduct().getName(), s.getProduct().getCategory().getName(), s.getQuantity(),
                        s.getProduct().getPurchasePrice(), s.getProduct().getSellingPrice(), statusOf(s.getQuantity())))
                .filter(r -> status == null || status.isBlank() || r.stockStatus().equalsIgnoreCase(status))
                .filter(r -> Text.contains(r.productName(), q) || Text.contains(r.productCode(), q)
                        || Text.contains(r.categoryName(), q))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> movements(Long productId) {
        List<StockMovement> rows = productId == null
                ? movementRepository.findTop200ByOrderByIdDesc()
                : movementRepository.findTop200ByProductIdOrderByIdDesc(productId);
        return rows.stream().map(m -> new StockMovementResponse(m.getId(), m.getProduct().getName(),
                m.getMovementType(), m.getQuantityChange(), m.getBalanceAfter(), m.getReferenceType(),
                m.getReferenceId(), m.getNote(), m.getCreatedBy(), m.getCreatedAt())).toList();
    }

    @Transactional(readOnly = true)
    public long lowStockCount() {
        return stockRepository.countByQuantityLessThanEqual(settingService.lowStockThreshold());
    }

    @Transactional(readOnly = true)
    public List<ChartPoint> quantityByCategory() {
        return stockRepository.quantityByCategory().stream()
                .map(row -> new ChartPoint(String.valueOf(row[0]), (Number) row[1])).toList();
    }

    private void recordMovement(Product product, String type, int change, int balance, String refType, Long refId, String note) {
        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setMovementType(type);
        movement.setQuantityChange(change);
        movement.setBalanceAfter(balance);
        movement.setReferenceType(refType);
        movement.setReferenceId(refId);
        movement.setNote(note);
        movement.setCreatedBy(CurrentUser.email());
        movementRepository.save(movement);
    }
}
