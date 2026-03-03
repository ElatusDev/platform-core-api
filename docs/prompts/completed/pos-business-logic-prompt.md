# POS Transaction Business Logic — Claude Code Execution Prompt

**Target**: Claude Code CLI  
**Repo**: `/Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api`  
**Spec**: `docs/workflows/pending/pos-business-logic-workflow.md`  
**Prerequisite**: Read `CLAUDE.md`, `AI-CODE-REF.md`, `DESIGN.md` before starting.  
**Dependency**: All existing POS CRUD use cases must compile and pass tests.

---

## EXECUTION RULES

1. Execute phases **strictly in order** (Phase 1 → 2 → 3 → 4 → 5 → 6).
2. Do NOT skip ahead. Each phase must compile and test before the next begins.
3. After EACH phase, run the specified verification command. Fix failures before proceeding.
4. All new files MUST include the ElatusDev copyright header.
5. All `public` classes and methods MUST have Javadoc.
6. Test methods: `shouldDoX_whenGivenY()` with `@DisplayName`, Given-When-Then comments, zero `any()` matchers.
7. Read existing files BEFORE modifying — field names, import paths, and CompositeId class names vary.
8. All money calculations use `BigDecimal`, never `double` in business logic.
9. Use `applicationContext.getBean()` for all entity instantiation — never `new EntityDataModel()`.

---

## Phase 1: OpenAPI Schema Changes + DTO Regeneration

### Step 1.1: Read the current OpenAPI specs

```bash
cat pos-system/src/main/resources/openapi/store-transaction.yaml
cat pos-system/src/main/resources/openapi/pos-system-module.yaml
```

### Step 1.2: Add SaleItemRequest and SaleItemResponse schemas to `store-transaction.yaml`

Add these new schemas under `components.schemas`:

```yaml
    SaleItemRequest:
      type: object
      properties:
        storeProductId:
          type: integer
          format: int64
          description: ID of the product being purchased
        quantity:
          type: integer
          minimum: 1
          description: Number of units to purchase
      required:
        - storeProductId
        - quantity
    SaleItemResponse:
      type: object
      properties:
        storeSaleItemId:
          type: integer
          format: int64
          readOnly: true
          description: Sale item ID
        storeProductId:
          type: integer
          format: int64
          description: ID of the product sold
        quantity:
          type: integer
          description: Number of units sold
        unitPriceAtSale:
          type: number
          format: double
          description: Unit price at time of sale
        itemTotal:
          type: number
          format: double
          description: Total for this line item (quantity × unitPriceAtSale)
      required:
        - storeSaleItemId
        - storeProductId
        - quantity
        - unitPriceAtSale
        - itemTotal
```

### Step 1.3: Modify StoreTransactionCreationRequest

Replace the current `StoreTransactionCreationRequest` (which uses `allOf` referencing `BaseStoreTransaction`) with a standalone schema that includes `saleItems` but removes `totalAmount`:

```yaml
    StoreTransactionCreationRequest:
      type: object
      properties:
        transactionType:
          type: string
          maxLength: 30
          description: Type of transaction (SALE, REFUND)
        paymentMethod:
          type: string
          maxLength: 50
          description: Payment method (CASH, CREDIT_CARD, DEBIT_CARD, DIGITAL_WALLET)
        saleItems:
          type: array
          items:
            $ref: '#/components/schemas/SaleItemRequest'
          minItems: 1
          description: Line items for the transaction
      required:
        - transactionType
        - paymentMethod
        - saleItems
```

### Step 1.4: Add saleItems to GetStoreTransactionResponse

In the existing `GetStoreTransactionResponse`, add inside the second `allOf` element's properties:

```yaml
            saleItems:
              type: array
              items:
                $ref: '#/components/schemas/SaleItemResponse'
              readOnly: true
              description: Line items that comprise this transaction
```

### Step 1.5: Update pos-system-module.yaml

Add new schema references:

```yaml
    SaleItemRequest:
      $ref: './store-transaction.yaml#/components/schemas/SaleItemRequest'
    SaleItemResponse:
      $ref: './store-transaction.yaml#/components/schemas/SaleItemResponse'
```

### Step 1.6: Regenerate DTOs

```bash
mvn clean generate-sources -pl pos-system -am -DskipTests
```

Verify:
```bash
find pos-system/target/generated-sources -name "*SaleItem*DTO.java" | sort
find pos-system/target/generated-sources -name "*StoreTransactionCreation*DTO.java"
```

Confirm `StoreTransactionCreationRequestDTO` has `getSaleItems()` and no longer has `getTotalAmount()`.

### Step 1.7: Compile check (EXPECT FAILURES)

```bash
mvn clean compile -pl pos-system -am -DskipTests 2>&1 | head -50
```

Expected: `StoreTransactionCreationUseCase` and its test reference old DTO shape. Note the errors but do NOT fix yet — Phase 3 handles this.

### Step 1.8: Commit (allow broken state since next phase fixes it)

```bash
git add pos-system/src/main/resources/openapi/
git commit -m "api(pos-system): add sale items to transaction OpenAPI schema

Add SaleItemRequest/SaleItemResponse DTOs. Move totalAmount to
server-computed (removed from creation request). Add saleItems
array to StoreTransactionCreationRequest and GetStoreTransactionResponse."
```

---

## Phase 2: UpdateStoreProductUseCase

### Step 2.1: Check EntityType for STORE_PRODUCT constant

```bash
cat utilities/src/main/java/com/akademiaplus/utilities/EntityType.java
```

If `STORE_PRODUCT` and `STORE_TRANSACTION` constants don't exist, add them following the existing pattern. Also add `STORE_SALE_ITEM` for completeness.

If EntityType uses string constants:
```java
public static final String STORE_PRODUCT = "entity.store_product";
public static final String STORE_TRANSACTION = "entity.store_transaction";
public static final String STORE_SALE_ITEM = "entity.store_sale_item";
```

Compile: `mvn clean compile -pl utilities -am -DskipTests`

### Step 2.2: Read the existing StoreProductController

```bash
cat pos-system/src/main/java/com/akademiaplus/store/interfaceadapters/StoreProductController.java
```

Confirm it implements `StoreProductsApi` which includes `updateStoreProduct` from the OpenAPI.

### Step 2.3: Create UpdateStoreProductUseCase

**File**: `pos-system/src/main/java/com/akademiaplus/store/usecases/UpdateStoreProductUseCase.java`

```java
/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.store.usecases;

import com.akademiaplus.billing.store.StoreProductDataModel;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.store.interfaceadapters.StoreProductRepository;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.pos.system.dto.StoreProductCreationRequestDTO;
import openapi.akademiaplus.domain.pos.system.dto.StoreProductCreationResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles updating an existing store product by mapping new field values
 * onto the persisted entity.
 * <p>
 * Uses a named TypeMap ({@value MAP_NAME}) that skips the entity ID field
 * to prevent overwriting the composite key during mapping.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class UpdateStoreProductUseCase {

    /**
     * Named TypeMap identifier for update mapping.
     * Registered in {@link com.akademiaplus.config.PosModelMapperConfiguration}.
     */
    public static final String MAP_NAME = "storeProductUpdateMap";

    private final StoreProductRepository storeProductRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;

    /**
     * Updates the store product identified by {@code storeProductId}
     * within the current tenant context.
     *
     * @param storeProductId the entity-specific product ID
     * @param dto            the updated field values
     * @return response containing the product ID
     * @throws EntityNotFoundException if no product exists with the given composite key
     * @throws com.akademiaplus.utilities.exceptions.InvalidTenantException
     *         if no tenant context is set
     */
    @Transactional
    public StoreProductCreationResponseDTO update(Long storeProductId,
                                                   StoreProductCreationRequestDTO dto) {
        Long tenantId = tenantContextHolder.requireTenantId();

        StoreProductDataModel existing = storeProductRepository
                .findById(new StoreProductDataModel.ProductCompositeId(tenantId, storeProductId))
                .orElseThrow(() -> new EntityNotFoundException(
                        EntityType.STORE_PRODUCT, String.valueOf(storeProductId)));

        modelMapper.map(dto, existing, MAP_NAME);
        StoreProductDataModel saved = storeProductRepository.saveAndFlush(existing);
        return modelMapper.map(saved, StoreProductCreationResponseDTO.class);
    }
}
```

### Step 2.4: Register update TypeMap in PosModelMapperConfiguration

Read the current file:
```bash
cat pos-system/src/main/java/com/akademiaplus/config/PosModelMapperConfiguration.java
```

Add import for `UpdateStoreProductUseCase` and a new registration method:

```java
private void registerStoreProductUpdateMap() {
    modelMapper.createTypeMap(
            StoreProductCreationRequestDTO.class,
            StoreProductDataModel.class,
            UpdateStoreProductUseCase.MAP_NAME
    ).addMappings(mapper -> {
        mapper.skip(StoreProductDataModel::setStoreProductId);
    }).implicitMappings();
}
```

Call `registerStoreProductUpdateMap()` inside `registerTypeMaps()`.

### Step 2.5: Wire into StoreProductController

Add `UpdateStoreProductUseCase` as a constructor dependency and implement the override:

```java
private final UpdateStoreProductUseCase updateStoreProductUseCase;

@Override
public ResponseEntity<StoreProductCreationResponseDTO> updateStoreProduct(
        Long storeProductId, StoreProductCreationRequestDTO storeProductCreationRequestDTO) {
    return ResponseEntity.ok(
            updateStoreProductUseCase.update(storeProductId, storeProductCreationRequestDTO));
}
```

Update the constructor signature to include all 5 use cases.

### Step 2.6: Create unit test

**File**: `pos-system/src/test/java/com/akademiaplus/store/usecases/UpdateStoreProductUseCaseTest.java`

```java
/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.store.usecases;

import com.akademiaplus.billing.store.StoreProductDataModel;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.store.interfaceadapters.StoreProductRepository;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.pos.system.dto.StoreProductCreationRequestDTO;
import openapi.akademiaplus.domain.pos.system.dto.StoreProductCreationResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UpdateStoreProductUseCase}.
 */
@DisplayName("UpdateStoreProductUseCase")
@ExtendWith(MockitoExtension.class)
class UpdateStoreProductUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long PRODUCT_ID = 42L;
    private static final String PRODUCT_NAME = "Notebook";
    private static final Double PRODUCT_PRICE = 9.99;
    private static final Integer STOCK_QUANTITY = 100;

    @Mock private StoreProductRepository storeProductRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;

    private UpdateStoreProductUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateStoreProductUseCase(
                storeProductRepository, tenantContextHolder, modelMapper);
    }

    private StoreProductCreationRequestDTO buildDto() {
        StoreProductCreationRequestDTO dto = new StoreProductCreationRequestDTO();
        dto.setName(PRODUCT_NAME);
        dto.setPrice(PRODUCT_PRICE);
        dto.setStockQuantity(STOCK_QUANTITY);
        return dto;
    }

    @Nested
    @DisplayName("Successful Update")
    class SuccessfulUpdate {

        @Test
        @DisplayName("Should find existing product and map DTO onto it")
        void shouldFindAndMapDto_whenProductExists() {
            // Given
            StoreProductCreationRequestDTO dto = buildDto();
            StoreProductDataModel existing = new StoreProductDataModel();
            StoreProductDataModel saved = new StoreProductDataModel();
            StoreProductCreationResponseDTO expectedResponse = new StoreProductCreationResponseDTO();
            expectedResponse.setStoreProductId(PRODUCT_ID);

            StoreProductDataModel.ProductCompositeId compositeId =
                    new StoreProductDataModel.ProductCompositeId(TENANT_ID, PRODUCT_ID);

            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(storeProductRepository.findById(compositeId)).thenReturn(Optional.of(existing));
            doNothing().when(modelMapper).map(dto, existing, UpdateStoreProductUseCase.MAP_NAME);
            when(storeProductRepository.saveAndFlush(existing)).thenReturn(saved);
            when(modelMapper.map(saved, StoreProductCreationResponseDTO.class))
                    .thenReturn(expectedResponse);

            // When
            StoreProductCreationResponseDTO result = useCase.update(PRODUCT_ID, dto);

            // Then
            verify(storeProductRepository).findById(compositeId);
            verify(modelMapper).map(dto, existing, UpdateStoreProductUseCase.MAP_NAME);
            verify(storeProductRepository).saveAndFlush(existing);
            assertThat(result.getStoreProductId()).isEqualTo(PRODUCT_ID);
        }
    }

    @Nested
    @DisplayName("Product Not Found")
    class ProductNotFound {

        @Test
        @DisplayName("Should throw EntityNotFoundException when product does not exist")
        void shouldThrowEntityNotFound_whenProductDoesNotExist() {
            // Given
            StoreProductCreationRequestDTO dto = buildDto();
            StoreProductDataModel.ProductCompositeId compositeId =
                    new StoreProductDataModel.ProductCompositeId(TENANT_ID, PRODUCT_ID);

            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(storeProductRepository.findById(compositeId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.update(PRODUCT_ID, dto))
                    .isInstanceOf(EntityNotFoundException.class)
                    .satisfies(ex -> {
                        EntityNotFoundException enfe = (EntityNotFoundException) ex;
                        assertThat(enfe.getEntityType()).isEqualTo(EntityType.STORE_PRODUCT);
                        assertThat(enfe.getEntityId()).isEqualTo(String.valueOf(PRODUCT_ID));
                    });
        }
    }
}
```

### Step 2.7: Compile + test

```bash
mvn clean compile -pl pos-system -am -DskipTests
mvn test -pl pos-system -am -Dtest="UpdateStoreProductUseCaseTest"
```

Note: The full module test will fail because the transaction creation test is broken from Phase 1. Only run the specific test class.

### Step 2.8: Commit

```bash
git add -A
git commit -m "feat(pos-system): implement UpdateStoreProductUseCase with PUT endpoint

Add UpdateStoreProductUseCase with find-or-404, named TypeMap
mapping onto existing entity, and unit tests. Wire into
StoreProductController. Add STORE_PRODUCT/STORE_TRANSACTION
constants to EntityType."
```

---

## Phase 3: Refactor StoreTransactionCreationUseCase

This is the core phase. Read every file involved before writing any code.

### Step 3.1: Read current implementation and its test

```bash
cat pos-system/src/main/java/com/akademiaplus/store/usecases/StoreTransactionCreationUseCase.java
cat pos-system/src/test/java/com/akademiaplus/store/usecases/StoreTransactionCreationUseCaseTest.java
cat multi-tenant-data/src/main/java/com/akademiaplus/billing/store/StoreTransactionDataModel.java
cat multi-tenant-data/src/main/java/com/akademiaplus/billing/store/StoreSaleItemDataModel.java
cat multi-tenant-data/src/main/java/com/akademiaplus/billing/store/StoreProductDataModel.java
```

### Step 3.2: Read the regenerated DTO to confirm new shape

```bash
cat pos-system/target/generated-sources/openapi/src/main/java/openapi/akademiaplus/domain/pos/system/dto/StoreTransactionCreationRequestDTO.java
cat pos-system/target/generated-sources/openapi/src/main/java/openapi/akademiaplus/domain/pos/system/dto/SaleItemRequestDTO.java
```

Confirm `StoreTransactionCreationRequestDTO.getSaleItems()` returns `List<SaleItemRequestDTO>`.
Confirm `SaleItemRequestDTO` has `getStoreProductId()` and `getQuantity()`.

### Step 3.3: Rewrite StoreTransactionCreationUseCase

**Replace the entire file** with the new implementation. The complete class must include:

**New imports** (add to existing):
```java
import com.akademiaplus.billing.store.StoreSaleItemDataModel;
import com.akademiaplus.billing.store.StoreProductDataModel;
import com.akademiaplus.store.interfaceadapters.StoreProductRepository;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.pos.system.dto.SaleItemRequestDTO;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
```

**New constants**:
```java
public static final String ERROR_EMPTY_SALE_ITEMS = "Transaction must contain at least one sale item";
public static final String ERROR_INSUFFICIENT_STOCK =
        "Insufficient stock for product '%s' (ID: %d): requested %d, available %d";
public static final String TRANSACTION_TYPE_SALE = "SALE";
public static final String TRANSACTION_TYPE_REFUND = "REFUND";
```

**New dependency** (added to constructor):
```java
private final StoreProductRepository storeProductRepository;
```

**Rewritten `create()` method**:
```java
@Transactional
public StoreTransactionCreationResponseDTO create(StoreTransactionCreationRequestDTO dto) {
    validateSaleItems(dto);

    StoreTransactionDataModel transaction = transform(dto);
    List<StoreSaleItemDataModel> saleItems = buildSaleItems(dto, transaction);
    transaction.setSaleItems(saleItems);
    transaction.setTotalAmount(computeTotalAmount(saleItems));

    StoreTransactionDataModel saved = storeTransactionRepository.saveAndFlush(transaction);
    adjustInventory(saleItems, dto.getTransactionType());

    return modelMapper.map(saved, StoreTransactionCreationResponseDTO.class);
}
```

**New private methods** (each with Javadoc):

```java
/**
 * Validates that the request contains at least one sale item.
 */
private void validateSaleItems(StoreTransactionCreationRequestDTO dto) {
    if (dto.getSaleItems() == null || dto.getSaleItems().isEmpty()) {
        throw new IllegalArgumentException(ERROR_EMPTY_SALE_ITEMS);
    }
}

/**
 * Builds sale item entities by resolving each referenced product,
 * capturing price-at-sale, and validating stock for SALE transactions.
 */
private List<StoreSaleItemDataModel> buildSaleItems(
        StoreTransactionCreationRequestDTO dto,
        StoreTransactionDataModel transaction) {

    Long tenantId = transaction.getTenantId();
    List<StoreSaleItemDataModel> items = new ArrayList<>();

    for (SaleItemRequestDTO itemDto : dto.getSaleItems()) {
        StoreProductDataModel product = storeProductRepository
                .findById(new StoreProductDataModel.ProductCompositeId(
                        tenantId, itemDto.getStoreProductId()))
                .orElseThrow(() -> new EntityNotFoundException(
                        EntityType.STORE_PRODUCT,
                        String.valueOf(itemDto.getStoreProductId())));

        if (TRANSACTION_TYPE_SALE.equals(dto.getTransactionType())) {
            validateStock(product, itemDto.getQuantity());
        }

        StoreSaleItemDataModel saleItem = applicationContext
                .getBean(StoreSaleItemDataModel.class);
        saleItem.setStoreProductId(itemDto.getStoreProductId());
        saleItem.setQuantity(itemDto.getQuantity());
        saleItem.setUnitPriceAtSale(product.getPrice());
        saleItem.setItemTotal(product.getPrice()
                .multiply(BigDecimal.valueOf(itemDto.getQuantity())));
        saleItem.setTransaction(transaction);

        items.add(saleItem);
    }
    return items;
}

/**
 * Validates that a product has sufficient stock for the requested quantity.
 */
private void validateStock(StoreProductDataModel product, Integer requestedQuantity) {
    if (product.getStockQuantity() < requestedQuantity) {
        throw new IllegalArgumentException(String.format(
                ERROR_INSUFFICIENT_STOCK,
                product.getName(),
                product.getStoreProductId(),
                requestedQuantity,
                product.getStockQuantity()));
    }
}

/**
 * Computes the transaction total from the sum of all sale item totals.
 */
private BigDecimal computeTotalAmount(List<StoreSaleItemDataModel> saleItems) {
    return saleItems.stream()
            .map(StoreSaleItemDataModel::getItemTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
}

/**
 * Adjusts inventory for each sale item based on transaction type.
 * SALE decrements stock; REFUND increments stock.
 */
private void adjustInventory(List<StoreSaleItemDataModel> saleItems,
                              String transactionType) {
    for (StoreSaleItemDataModel item : saleItems) {
        StoreProductDataModel product = storeProductRepository.findById(
                new StoreProductDataModel.ProductCompositeId(
                        item.getTenantId(), item.getStoreProductId()))
                .orElseThrow();

        int adjustment = item.getQuantity();
        if (TRANSACTION_TYPE_SALE.equals(transactionType)) {
            product.setStockQuantity(product.getStockQuantity() - adjustment);
        } else if (TRANSACTION_TYPE_REFUND.equals(transactionType)) {
            product.setStockQuantity(product.getStockQuantity() + adjustment);
        }
        storeProductRepository.saveAndFlush(product);
    }
}
```

**Keep `transform()` unchanged** — it still handles header-level DTO→entity mapping via the named TypeMap.

### Step 3.4: Verify PosModelMapperConfiguration skip rules

```bash
cat pos-system/src/main/java/com/akademiaplus/config/PosModelMapperConfiguration.java
```

The existing `registerStoreTransactionMap()` already skips `setSaleItems`, `setEmployee`, `setTransactionDatetime`, and `setStoreTransactionId`. This is correct — sale items are wired manually, not via ModelMapper.

### Step 3.5: Rewrite the unit test

**Replace the entire test file**. The new test must cover these `@Nested` classes:

```
@DisplayName("StoreTransactionCreationUseCase")
@ExtendWith(MockitoExtension.class)
class StoreTransactionCreationUseCaseTest {

    // --- Constants ---
    private static final Long TENANT_ID = 1L;
    private static final Long PRODUCT_ID_1 = 10L;
    private static final Long PRODUCT_ID_2 = 20L;
    private static final String PRODUCT_NAME_1 = "Notebook";
    private static final String PRODUCT_NAME_2 = "Pen";
    private static final BigDecimal PRODUCT_PRICE_1 = new BigDecimal("5.00");
    private static final BigDecimal PRODUCT_PRICE_2 = new BigDecimal("2.50");
    private static final Integer STOCK_1 = 100;
    private static final Integer STOCK_2 = 50;
    private static final Integer QUANTITY_1 = 3;
    private static final Integer QUANTITY_2 = 5;
    private static final String PAYMENT_METHOD = "CASH";

    // --- Mocks ---
    @Mock private ApplicationContext applicationContext;
    @Mock private StoreTransactionRepository storeTransactionRepository;
    @Mock private StoreProductRepository storeProductRepository;
    @Mock private ModelMapper modelMapper;

    private StoreTransactionCreationUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new StoreTransactionCreationUseCase(
                applicationContext, storeTransactionRepository,
                storeProductRepository, modelMapper);
    }

    // --- Helpers ---
    // buildDto(transactionType, saleItemDtos...)
    // buildSaleItemDto(productId, quantity)
    // buildProduct(productId, name, price, stock)

    @Nested
    @DisplayName("Validation")
    class Validation {
        // shouldThrowException_whenSaleItemsNull
        // shouldThrowException_whenSaleItemsEmpty
        // Use StoreTransactionCreationUseCase.ERROR_EMPTY_SALE_ITEMS for message assertion
    }

    @Nested
    @DisplayName("Product Resolution")
    class ProductResolution {
        // shouldThrowEntityNotFound_whenProductIdInvalid
    }

    @Nested
    @DisplayName("Stock Validation")
    class StockValidation {
        // shouldThrowException_whenInsufficientStockForSale
        // shouldSkipStockValidation_whenTransactionTypeRefund
        // Use StoreTransactionCreationUseCase.ERROR_INSUFFICIENT_STOCK for message assertion
    }

    @Nested
    @DisplayName("Price Capture and Total Computation")
    class PriceCaptureAndTotal {
        // shouldCaptureCurrentProductPrice_asUnitPriceAtSale
        // shouldComputeItemTotal_asQuantityTimesUnitPrice
        // shouldComputeTransactionTotal_asSumOfItemTotals
    }

    @Nested
    @DisplayName("Inventory Adjustment")
    class InventoryAdjustment {
        // shouldDecrementStock_whenTransactionTypeSale
        // shouldIncrementStock_whenTransactionTypeRefund
    }

    @Nested
    @DisplayName("Persistence")
    class Persistence {
        // shouldSaveTransactionWithSaleItems
        // shouldReturnMappedResponseDto
    }
}
```

**Important mock pattern** for multiple sale items — each `getBean` call returns a new prototype:

```java
when(applicationContext.getBean(StoreSaleItemDataModel.class))
    .thenReturn(new StoreSaleItemDataModel())
    .thenReturn(new StoreSaleItemDataModel());
```

All tests use exact matchers (zero `any()`), Given-When-Then comments, and reference constants from the use case class for message assertions.

### Step 3.6: Compile + test

```bash
mvn clean compile -pl pos-system -am -DskipTests
mvn test -pl pos-system -am
```

All POS module tests must pass. Fix any failures before proceeding.

### Step 3.7: Commit

```bash
git add -A
git commit -m "feat(pos-system): add sale items and inventory management to transaction creation

Refactor StoreTransactionCreationUseCase to:
- Accept sale items array in creation request
- Resolve products and capture price-at-sale
- Validate stock availability for SALE transactions
- Compute totalAmount server-side from item totals
- Adjust inventory (decrement for SALE, increment for REFUND)
- Save transaction with cascaded sale items via CascadeType.ALL

Rewrite unit tests covering validation, product resolution,
stock validation, price capture, inventory adjustment, and
persistence. All tests use Given-When-Then with zero any() matchers."
```

---

## Phase 4: Employee Context Injection

### Step 4.1: Read the security infrastructure

```bash
find security/src/main/java -name "JwtTokenProvider.java" | head -1
cat <found-path>

find security/src/main/java -name "JwtRequestFilter.java" | head -1
cat <found-path>

grep -rn "employeeId\|employee_id\|claims" security/src/main/java/ | head -20
```

Identify:
1. How the JWT is parsed and which claims are extracted
2. What object is stored in `SecurityContextHolder.getContext().getAuthentication().getPrincipal()`
3. Whether `employeeId` is available as a claim or must be derived from `internalAuthId`

**IMPORTANT**: Do NOT proceed until you understand the claim structure. The exact implementation depends on what you find in `JwtTokenProvider`.

### Step 4.2: Add employee extraction to StoreTransactionCreationUseCase

If `employeeId` is available in the JWT claims/principal:

```java
/**
 * Extracts the employee ID from the current security context, if available.
 * Returns {@code null} for unauthenticated or non-employee requests.
 *
 * @return the employee ID from the JWT, or {@code null}
 */
private Long extractEmployeeId() {
    // Implementation depends on JwtTokenProvider claim structure
    // Read the security module first
}
```

Call in `create()` after `transform()`:
```java
StoreTransactionDataModel transaction = transform(dto);
transaction.setEmployeeId(extractEmployeeId());
```

### Step 4.3: Add test with mocked SecurityContext

Add `@Nested "EmployeeContext"` class with:
- `shouldSetEmployeeId_whenAuthenticated` — mock SecurityContextHolder with employeeId claim
- `shouldLeaveEmployeeIdNull_whenNotAuthenticated` — empty SecurityContext

### Step 4.4: Compile + test + commit

```bash
mvn clean compile -pl pos-system -am -DskipTests
mvn test -pl pos-system -am
```

```bash
git add -A
git commit -m "feat(pos-system): inject employee context from JWT into transactions

Extract employeeId from authenticated JWT and set on transaction
header during creation. Optional — transactions without
authentication context leave employeeId null (self-service scenario)."
```

---

## Phase 5: Enhanced Transaction Read Responses

### Step 5.1: Read the current Get use cases

```bash
cat pos-system/src/main/java/com/akademiaplus/store/usecases/GetStoreTransactionByIdUseCase.java
cat pos-system/src/main/java/com/akademiaplus/store/usecases/GetAllStoreTransactionsUseCase.java
cat pos-system/src/main/java/com/akademiaplus/store/interfaceadapters/StoreTransactionRepository.java
```

### Step 5.2: Add eager fetch for saleItems

In `StoreTransactionRepository`, add custom queries to eagerly load sale items.

**Option A** — `@EntityGraph` (try first):
```java
@EntityGraph(attributePaths = {"saleItems"})
@Override
Optional<StoreTransactionDataModel> findById(
        StoreTransactionDataModel.StoreTransactionCompositeId id);
```

**Option B** — `@Query` with `JOIN FETCH` (if @EntityGraph conflicts with tenant filter):
```java
@Query("SELECT t FROM StoreTransactionDataModel t LEFT JOIN FETCH t.saleItems " +
       "WHERE t.tenantId = :tenantId AND t.storeTransactionId = :transactionId")
Optional<StoreTransactionDataModel> findByIdWithSaleItems(
        @Param("tenantId") Long tenantId,
        @Param("transactionId") Long transactionId);
```

### Step 5.3: Register SaleItem → SaleItemResponse TypeMap (if needed)

Test whether ModelMapper handles the nested mapping implicitly first. If not:

```java
private void registerSaleItemResponseMap() {
    modelMapper.createTypeMap(
            StoreSaleItemDataModel.class,
            SaleItemResponseDTO.class
    ).implicitMappings();
}
```

### Step 5.4: Compile + test + commit

```bash
mvn clean compile -pl pos-system -am -DskipTests
mvn test -pl pos-system -am
```

```bash
git add -A
git commit -m "feat(pos-system): include sale items in transaction read responses

Update GetById and GetAll transaction use cases to map sale items
into the response DTO. Add eager fetch to prevent N+1 queries."
```


---

## Phase 6: Full Verification

### Step 6.1: Run all POS module tests

```bash
mvn test -pl pos-system -am
```

Every test must pass: creation (product + transaction), update, getById, getAll, delete.

### Step 6.2: Run full project build

```bash
mvn clean install -DskipTests
mvn test
```

Fix any cross-module regressions.

### Step 6.3: Verify all controller endpoints are wired

```bash
grep -n "@Override" pos-system/src/main/java/com/akademiaplus/store/interfaceadapters/StoreProductController.java
grep -n "@Override" pos-system/src/main/java/com/akademiaplus/store/interfaceadapters/StoreTransactionController.java
```

Both controllers must implement ALL methods from their respective generated API interfaces.

### Step 6.4: Final commit summary

If any cleanup is needed after the full build, commit:

```bash
git add -A
git commit -m "chore(pos-system): final verification and cleanup after business logic rollout"
```

---

## VERIFICATION CHECKLIST

Run after all phases complete:

- [ ] `mvn clean install` passes (full build, all modules)
- [ ] `mvn test -pl pos-system -am` — all POS tests green
- [ ] `POST /store-products` — creates product with stockQuantity ✅
- [ ] `PUT /store-products/{id}` — updates product fields ✅
- [ ] `POST /store-transactions` with `saleItems[]` — creates transaction + items ✅
- [ ] Transaction creation decrements `stockQuantity` on products ✅
- [ ] Transaction with `REFUND` type increments `stockQuantity` ✅
- [ ] Transaction creation rejects insufficient stock with 400 ✅
- [ ] Transaction creation rejects empty `saleItems` with 400 ✅
- [ ] `totalAmount` is server-computed, not client-provided ✅
- [ ] `GET /store-transactions/{id}` returns `saleItems[]` in response ✅
- [ ] `GET /store-transactions` returns transactions with `saleItems[]` ✅
- [ ] `employeeId` is populated from JWT when authenticated ✅
- [ ] All tests use Given-When-Then, zero `any()` matchers ✅
- [ ] All public classes and methods have Javadoc ✅
- [ ] ElatusDev copyright header on all new files ✅
