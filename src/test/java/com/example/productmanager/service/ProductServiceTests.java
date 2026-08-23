package com.example.productmanager.service;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.productmanager.model.Product;
import com.example.productmanager.model.ProductCategory;
import com.example.productmanager.repository.CustomerOrderRepository;
import com.example.productmanager.repository.ProductRepository;

class ProductServiceTests {

    @Test
    void shouldReturnOnlyFeaturedProductsWithinRequestedLimit() {
        ProductRepository productRepository = mock(ProductRepository.class);
        CustomerOrderRepository customerOrderRepository = mock(CustomerOrderRepository.class);
        ProductService productService = new ProductService(productRepository, customerOrderRepository);

        Product featuredOne = Product.builder()
                .id(1L)
                .name("Sữa tươi Vinamilk")
                .category(ProductCategory.SUA)
                .price(new BigDecimal("42000"))
                .quantity(120)
                .unit("Hộp")
                .build();

        Product featuredTwo = Product.builder()
                .id(2L)
                .name("Gạo ST25")
                .category(ProductCategory.THUC_PHAM)
                .price(new BigDecimal("52000"))
                .quantity(80)
                .unit("Kg")
                .build();

        when(productRepository.findTopFeaturedProducts(5))
                .thenReturn(List.of(featuredOne, featuredTwo));

        List<Product> featuredProducts = productService.getFeaturedProducts(5);

        assertThat(featuredProducts).hasSize(2);
        verify(productRepository).findTopFeaturedProducts(5);
    }

    @Test
    void createProductShouldRejectInvalidDataBeforeSave() {
        ProductRepository productRepository = mock(ProductRepository.class);
        CustomerOrderRepository customerOrderRepository = mock(CustomerOrderRepository.class);
        ProductService productService = new ProductService(productRepository, customerOrderRepository);

        Product invalidProduct = Product.builder()
                .name("   ")
                .category(ProductCategory.THUC_PHAM)
                .price(BigDecimal.ZERO)
                .quantity(-1)
                .unit("Kg")
                .build();

        assertThrows(IllegalArgumentException.class, () -> productService.createProduct(invalidProduct));
        verify(productRepository, never()).save(invalidProduct);
    }
}
