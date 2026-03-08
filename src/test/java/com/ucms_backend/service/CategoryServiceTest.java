package com.ucms_backend.service;

import com.ucms_backend.dto.CategoryResponse;
import com.ucms_backend.dto.CreateCategoryRequest;
import com.ucms_backend.dto.UpdateCategoryRequest;
import com.ucms_backend.exception.AppException;
import com.ucms_backend.model.entity.Category;
import com.ucms_backend.repository.CategoryRepository;
import com.ucms_backend.repository.TicketRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void createCategory_success_returnsResponse() {
        CreateCategoryRequest request = new CreateCategoryRequest("Facilities");
        Category saved = new Category(1L, "Facilities");

        when(categoryRepository.existsByNameIgnoreCase(request.getName())).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);

        CategoryResponse response = categoryService.createCategory(request);

        assertEquals(1L, response.getId());
        assertEquals("Facilities", response.getName());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void createCategory_duplicate_throws409() {
        CreateCategoryRequest request = new CreateCategoryRequest("Facilities");

        when(categoryRepository.existsByNameIgnoreCase(request.getName())).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> categoryService.createCategory(request));

        assertEquals(409, exception.getStatus());
        assertEquals("CATEGORY_ALREADY_EXISTS", exception.getErrorCode());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void updateCategory_success_returnsUpdatedResponse() {
        Long categoryId = 1L;
        UpdateCategoryRequest request = new UpdateCategoryRequest("Academic");
        Category existing = new Category(categoryId, "Old Name");
        Category updated = new Category(categoryId, "Academic");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), categoryId)).thenReturn(false);
        when(categoryRepository.save(existing)).thenReturn(updated);

        CategoryResponse response = categoryService.updateCategory(categoryId, request);

        assertEquals(categoryId, response.getId());
        assertEquals("Academic", response.getName());
        verify(categoryRepository).save(existing);
    }

    @Test
    void updateCategory_notFound_throws404() {
        Long categoryId = 1L;
        UpdateCategoryRequest request = new UpdateCategoryRequest("Academic");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> categoryService.updateCategory(categoryId, request));

        assertEquals(404, exception.getStatus());
        assertEquals("CATEGORY_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void updateCategory_duplicate_throws409() {
        Long categoryId = 1L;
        UpdateCategoryRequest request = new UpdateCategoryRequest("Academic");
        Category existing = new Category(categoryId, "Old Name");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), categoryId)).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> categoryService.updateCategory(categoryId, request));

        assertEquals(409, exception.getStatus());
        assertEquals("CATEGORY_ALREADY_EXISTS", exception.getErrorCode());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void deleteCategory_success_deletesCategory() {
        Long categoryId = 1L;

        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(ticketRepository.existsByCategoryId(categoryId)).thenReturn(false);

        categoryService.deleteCategory(categoryId);

        verify(categoryRepository).deleteById(categoryId);
    }

    @Test
    void deleteCategory_notFound_throws404() {
        Long categoryId = 1L;

        when(categoryRepository.existsById(categoryId)).thenReturn(false);

        AppException exception = assertThrows(AppException.class, () -> categoryService.deleteCategory(categoryId));

        assertEquals(404, exception.getStatus());
        assertEquals("CATEGORY_NOT_FOUND", exception.getErrorCode());
        verify(ticketRepository, never()).existsByCategoryId(categoryId);
        verify(categoryRepository, never()).deleteById(categoryId);
    }

    @Test
    void deleteCategory_inUse_throws409() {
        Long categoryId = 1L;

        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(ticketRepository.existsByCategoryId(categoryId)).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> categoryService.deleteCategory(categoryId));

        assertEquals(409, exception.getStatus());
        assertEquals("CATEGORY_IN_USE", exception.getErrorCode());
        verify(categoryRepository, never()).deleteById(categoryId);
    }
}
