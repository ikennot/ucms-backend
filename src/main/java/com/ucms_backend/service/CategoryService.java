package com.ucms_backend.service;

import com.ucms_backend.dto.CategoryResponse;
import com.ucms_backend.dto.CreateCategoryRequest;
import com.ucms_backend.dto.UpdateCategoryRequest;
import com.ucms_backend.exception.AppException;
import com.ucms_backend.model.entity.Category;
import com.ucms_backend.repository.CategoryRepository;
import com.ucms_backend.repository.TicketRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TicketRepository ticketRepository;

    public CategoryService(CategoryRepository categoryRepository, TicketRepository ticketRepository) {
        this.categoryRepository = categoryRepository;
        this.ticketRepository = ticketRepository;
    }

    public List<CategoryResponse> getCategories() {
        return categoryRepository.findAll().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public CategoryResponse createCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new AppException(409, "CATEGORY_ALREADY_EXISTS", "Category already exists");
        }

        Category category = new Category();
        category.setName(request.getName());

        Category saved = categoryRepository.save(category);
        return CategoryResponse.from(saved);
    }

    public CategoryResponse updateCategory(Long id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(404, "CATEGORY_NOT_FOUND", "Category not found"));

        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw new AppException(409, "CATEGORY_ALREADY_EXISTS", "Category already exists");
        }

        category.setName(request.getName());
        Category saved = categoryRepository.save(category);
        return CategoryResponse.from(saved);
    }

    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new AppException(404, "CATEGORY_NOT_FOUND", "Category not found");
        }

        if (ticketRepository.existsByCategoryId(id)) {
            throw new AppException(409, "CATEGORY_IN_USE", "Category is in use");
        }

        categoryRepository.deleteById(id);
    }
}
