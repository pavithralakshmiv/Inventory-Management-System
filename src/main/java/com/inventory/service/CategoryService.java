package com.inventory.service;

import com.inventory.dto.CategoryRequest;
import com.inventory.entity.Category;
import com.inventory.exception.BusinessException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.repository.CategoryRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.util.Constants;
import com.inventory.util.Text;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final LogService logService;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository, LogService logService) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.logService = logService;
    }

    @Transactional(readOnly = true)
    public List<Category> search(String q) {
        return categoryRepository.search(q == null ? "" : q.trim());
    }

    @Transactional(readOnly = true)
    public Category get(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    @Transactional
    public Category create(CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException("A category named '" + name + "' already exists");
        }
        Category category = new Category();
        apply(category, request);
        Category saved = categoryRepository.save(category);
        logService.log("CATEGORY_ADDED", "Category added: " + saved.getName());
        return saved;
    }

    @Transactional
    public Category update(Long id, CategoryRequest request) {
        Category category = get(id);
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BusinessException("A category named '" + name + "' already exists");
        }
        apply(category, request);
        Category saved = categoryRepository.save(category);
        logService.log("CATEGORY_UPDATED", "Category updated: " + saved.getName());
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        Category category = get(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new BusinessException("Category '" + category.getName() + "' has products and cannot be deleted. Set it to Inactive instead.");
        }
        categoryRepository.delete(category);
        categoryRepository.flush();
        logService.log("CATEGORY_DELETED", "Category deleted: " + category.getName());
    }

    private void apply(Category category, CategoryRequest request) {
        category.setName(request.name().trim());
        category.setDescription(Text.clean(request.description()));
        category.setStatus(normalizeStatus(request.status()));
    }

    static String normalizeStatus(String status) {
        if (Constants.INACTIVE.equalsIgnoreCase(status)) {
            return Constants.INACTIVE;
        }
        if (Constants.ACTIVE.equalsIgnoreCase(status)) {
            return Constants.ACTIVE;
        }
        throw new BusinessException("Status must be Active or Inactive");
    }
}
