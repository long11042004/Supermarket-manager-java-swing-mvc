package com.example.productmanager.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.context.i18n.LocaleContextHolder;

import com.example.productmanager.entity.CustomerOrder;
import com.example.productmanager.entity.CustomerOrderItem;
import com.example.productmanager.entity.OrderStatus;
import com.example.productmanager.entity.Product;
import com.example.productmanager.entity.ProductCategory;
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
                .nameVi("Sữa tươi Vinamilk")
                .nameEn("Vinamilk fresh milk")
                .category(ProductCategory.SUA)
                .price(new BigDecimal("42000"))
                .quantity(120)
                .unitVi("Hộp")
                .unitEn("Box")
                .build();

        Product featuredTwo = Product.builder()
                .id(2L)
                .nameVi("Gạo ST25")
                .nameEn("ST25 rice")
                .category(ProductCategory.THUC_PHAM)
                .price(new BigDecimal("52000"))
                .quantity(80)
                .unitVi("Kg")
                .unitEn("kg")
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
                .nameVi("   ")
                .category(ProductCategory.THUC_PHAM)
                .price(BigDecimal.ZERO)
                .quantity(-1)
                .unitVi("Kg")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> productService.createProduct(invalidProduct));
        assertThat(ex).isNotNull();
        verify(productRepository, never()).save(invalidProduct);
    }

    @Test
    void createProductShouldRejectUnexpectedUnitAndPastExpiryDate() {
        ProductRepository productRepository = mock(ProductRepository.class);
        CustomerOrderRepository customerOrderRepository = mock(CustomerOrderRepository.class);
        ProductService productService = new ProductService(productRepository, customerOrderRepository);

        Product invalidProduct = Product.builder()
                .nameVi("Sữa tươi")
                .category(ProductCategory.SUA)
                .price(new BigDecimal("49000"))
                .quantity(12)
                .unitVi("Kg@#$")
                .expiryDate(java.time.LocalDate.now().minusDays(1))
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> productService.createProduct(invalidProduct));
        assertThat(ex).isNotNull();
        verify(productRepository, never()).save(invalidProduct);
    }

    @Test
    void createProductShouldAcceptLocalizedNameAndUnitFields() {
        ProductRepository productRepository = mock(ProductRepository.class);
        CustomerOrderRepository customerOrderRepository = mock(CustomerOrderRepository.class);
        ProductService productService = new ProductService(productRepository, customerOrderRepository);
        Locale originalLocale = LocaleContextHolder.getLocale();
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        try {
            Product localizedProduct = Product.builder()
                    .nameVi("Sữa tươi Vinamilk")
                    .nameEn("Vinamilk fresh milk")
                    .category(ProductCategory.SUA)
                    .price(new BigDecimal("42000"))
                    .quantity(120)
                    .unitVi("Hộp")
                    .unitEn("Box")
                    .build();

            when(productRepository.save(localizedProduct)).thenReturn(localizedProduct);

            Product saved = productService.createProduct(localizedProduct);

            assertThat(saved).isSameAs(localizedProduct);
            assertThat(saved.getNameVi()).isEqualTo("Sữa tươi Vinamilk");
            assertThat(saved.getDisplayName()).isEqualTo("Vinamilk fresh milk");
            verify(productRepository).save(localizedProduct);
        } finally {
            LocaleContextHolder.setLocale(originalLocale);
        }
    }

    @Test
    void deleteProductShouldCancelRelatedOrdersAndRemoveItemReferences() {
        ProductRepository productRepository = mock(ProductRepository.class);
        CustomerOrderRepository customerOrderRepository = mock(CustomerOrderRepository.class);
        ProductService productService = new ProductService(productRepository, customerOrderRepository);

        Product product = Product.builder()
                .id(10L)
                .nameVi("Sữa tươi")
                .category(ProductCategory.SUA)
                .price(new BigDecimal("42000"))
                .quantity(10)
                .unitVi("Hộp")
                .build();

        CustomerOrderItem item = CustomerOrderItem.builder()
                .product(product)
                .quantity(2)
                .unitPrice(new BigDecimal("42000"))
                .lineTotal(new BigDecimal("84000"))
                .build();

        CustomerOrder order = CustomerOrder.builder()
                .id(100L)
                .status(OrderStatus.PENDING)
                .items(new ArrayList<>(List.of(item)))
                .totalAmount(new BigDecimal("84000"))
                .build();
        item.setOrder(order);

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(customerOrderRepository.findOrdersByProductId(10L)).thenReturn(List.of(order));

        productService.deleteProduct(10L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getItems()).isEmpty();
        verify(customerOrderRepository).save(order);
        verify(productRepository).delete(product);
    }

    @Test
    void updateProductShouldSyncOrderItemPriceAndTotal() {
        ProductRepository productRepository = mock(ProductRepository.class);
        CustomerOrderRepository customerOrderRepository = mock(CustomerOrderRepository.class);
        ProductService productService = new ProductService(productRepository, customerOrderRepository);

        Product existing = Product.builder()
                .id(11L)
                .nameVi("Sữa tươi")
                .nameEn("Milk")
                .category(ProductCategory.SUA)
                .price(new BigDecimal("42000"))
                .quantity(10)
                .unitVi("Hộp")
                .unitEn("Box")
                .build();

        Product updated = Product.builder()
                .id(11L)
                .nameVi("Sữa tươi Vinamilk")
                .nameEn("Vinamilk Milk")
                .category(ProductCategory.SUA)
                .price(new BigDecimal("50000"))
                .quantity(10)
                .unitVi("Hộp")
                .unitEn("Box")
                .build();

        CustomerOrderItem item = CustomerOrderItem.builder()
                .product(existing)
                .quantity(3)
                .unitPrice(new BigDecimal("42000"))
                .lineTotal(new BigDecimal("126000"))
                .build();

        CustomerOrder order = CustomerOrder.builder()
                .id(200L)
                .status(OrderStatus.PENDING)
                .items(new ArrayList<>(List.of(item)))
                .totalAmount(new BigDecimal("126000"))
                .build();
        item.setOrder(order);

        when(productRepository.findById(11L)).thenReturn(Optional.of(existing));
        when(customerOrderRepository.findItemsByProductId(11L)).thenReturn(List.of(item));
        when(productRepository.save(existing)).thenReturn(existing);

        Product result = productService.updateProduct(11L, updated);

        assertThat(result.getPrice()).isEqualByComparingTo("50000");
        assertThat(item.getUnitPrice()).isEqualByComparingTo("50000");
        assertThat(item.getLineTotal()).isEqualByComparingTo("150000");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("150000");
        verify(productRepository).save(existing);
        verify(customerOrderRepository).save(order);
    }
}
