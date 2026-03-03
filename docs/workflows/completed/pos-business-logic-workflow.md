# POS Transaction Business Logic Workflow — AkademiaPlus

**Target**: Claude Code CLI  
**Repo**: `/Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api`  
**Prerequisite**: Read `CLAUDE.md`, `AI-CODE-REF.md`, and `DESIGN.md` before starting.

---

## 1. Current State Analysis

### 1.1 What Exists

The POS module has basic CRUD scaffolding for two root entities and one child entity:

| Entity | Table | CRUD State | Notes |
|--------|-------|------------|-------|
| `StoreProductDataModel` | `store_products` | C✅ R✅ U❌ D✅ | `updateStoreProduct` defined in OpenAPI but no use case or controller method |
| `StoreTransactionDataModel` | `store_transactions` | C✅ R✅ D✅ | Creation is header-only — no line items, no inventory effect |
| `StoreSaleItemDataModel` | `store_sale_items` | ❌ None | Repository exists, no controller, no use cases, no OpenAPI endpoints |

### 1.2 What's Missing (Gaps)

1. **Transaction → SaleItems linkage**: `StoreTransactionCreationRequest` carries only `transactionType`, `totalAmount`, `paymentMethod`. No `saleItems[]` array. The `CascadeType.ALL` on the entity is ready but unused.
2. **Inventory decrement**: Creating a transaction does not touch `StoreProduct.stockQuantity`.
3. **Employee context injection**: `employeeId` is never populated from the JWT/security context.
4. **Update product endpoint**: OpenAPI defines `PUT /store-products/{id}` but no implementation exists.
5. **Computed `totalAmount`**: The client must provide `totalAmount` manually — it should be computed from `sum(saleItem.quantity * saleItem.unitPriceAtSale)`.
6. **Stock validation**: No check for sufficient inventory before accepting a sale.

### 1.3 Entity Relationship Graph

```
StoreTransaction (header)
  ├── employeeId ──FK──→ Employee (optional, ManyToOne)
  └── saleItems ──CascadeALL──→ StoreSaleItem[] (OneToMany)
                                    └── storeProductId ──FK──→ StoreProduct (ManyToOne)
                                         └── stockQuantity (decremented on SALE, incremented on REFUND)
```

---

## 2. Target Architecture

### 2.1 New Transaction Creation Flow

```
Client POST /v1/pos-system/store-transactions
  ├── Request body: { transactionType, paymentMethod, saleItems: [ { storeProductId, quantity } ] }
  │
  ├── StoreTransactionCreationUseCase.create(dto)
  │     ├── 1. Resolve employeeId from SecurityContext (optional — may be null for self-service)
  │     ├── 2. Validate all referenced storeProductIds exist within tenant
  │     ├── 3. For each sale item:
  │     │     ├── Look up StoreProduct → get current price
  │     │     ├── Validate stockQuantity >= requested quantity (for SALE type)
  │     │     ├── Compute unitPriceAtSale = product.price
  │     │     └── Compute itemTotal = quantity × unitPriceAtSale
  │     ├── 4. Compute totalAmount = sum(itemTotal) — override any client-provided value
  │     ├── 5. Build StoreTransactionDataModel with nested StoreSaleItemDataModels
  │     ├── 6. Save via CascadeType.ALL → single saveAndFlush
  │     └── 7. Decrement stockQuantity on each product (for SALE type)
  │           Increment stockQuantity on each product (for REFUND type)
  │
  └── Response: { storeTransactionId }
```

### 2.2 Transaction Types and Inventory Effect

| transactionType | Inventory Effect | Validation |
|-----------------|-----------------|------------|
| `SALE` | Decrement `stockQuantity` by item quantity | Stock must be >= quantity |
| `REFUND` | Increment `stockQuantity` by item quantity | Original transaction should exist (future) |

### 2.3 Update Product Flow

```
Client PUT /v1/pos-system/store-products/{storeProductId}
  ├── Request body: { name, description, price, stockQuantity }
  │
  ├── UpdateStoreProductUseCase.update(id, dto)
  │     ├── 1. Build composite key from TenantContextHolder + id
  │     ├── 2. Find existing product or throw EntityNotFoundException
  │     ├── 3. Map DTO fields onto existing entity (named TypeMap, skip ID)
  │     └── 4. Save and return response DTO
  │
  └── Response: { storeProductId }
```

---

## 3. OpenAPI Schema Changes

### 3.1 New DTO: SaleItemRequest (nested in transaction creation)

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
```

### 3.2 Modified DTO: StoreTransactionCreationRequest

```yaml
StoreTransactionCreationRequest:
  allOf:
    - type: object
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

**Key change**: `totalAmount` is REMOVED from the request (server-computed). `saleItems[]` is ADDED.

### 3.3 Enhanced DTO: GetStoreTransactionResponse

Add `saleItems` to the read response so clients can see line-item detail:

```yaml
SaleItemResponse:
  type: object
  properties:
    storeSaleItemId:
      type: integer
      format: int64
      readOnly: true
    storeProductId:
      type: integer
      format: int64
    quantity:
      type: integer
    unitPriceAtSale:
      type: number
      format: double
    itemTotal:
      type: number
      format: double
  required:
    - storeSaleItemId
    - storeProductId
    - quantity
    - unitPriceAtSale
    - itemTotal
```

Add to `GetStoreTransactionResponse`:

```yaml
saleItems:
  type: array
  items:
    $ref: '#/components/schemas/SaleItemResponse'
  readOnly: true
```

---

## 4. Execution Phases

### Phase Dependency Graph

```
Phase 1: OpenAPI schema changes + DTO regeneration
    ↓
Phase 2: UpdateStoreProductUseCase (independent, unblocks PUT endpoint)
    ↓
Phase 3: Refactor StoreTransactionCreationUseCase (core business logic)
    ↓
Phase 4: Employee context injection via SecurityContext
    ↓
Phase 5: Update GetAll/GetById transaction use cases (include saleItems in response)
    ↓
Phase 6: Full test suite + integration verification
```

---

## 5. Phase-by-Phase Implementation

### Phase 1: OpenAPI Schema Changes

#### Step 1.1: Modify `store-transaction.yaml`

Add `SaleItemRequest` and `SaleItemResponse` schemas. Modify `StoreTransactionCreationRequest` to remove `totalAmount` from required/properties and add `saleItems` array. Add `saleItems` to `GetStoreTransactionResponse`.

#### Step 1.2: Register new schemas in `pos-system-module.yaml`

Add `$ref` entries for `SaleItemRequest` and `SaleItemResponse`.

#### Step 1.3: Regenerate DTOs

```bash
mvn clean generate-sources -pl pos-system -am -DskipTests
```

Verify generated DTOs:
```bash
find pos-system/target/generated-sources -name "*SaleItem*DTO.java"
find pos-system/target/generated-sources -name "*StoreTransactionCreation*DTO.java"
```

Confirm `StoreTransactionCreationRequestDTO` now has `getSaleItems()` returning a list, and no longer has `getTotalAmount()`.

#### Step 1.4: Compile check

```bash
mvn clean compile -pl pos-system -am -DskipTests
```

**Expected**: Compilation failures in `StoreTransactionCreationUseCase` and its test (they reference `getTotalAmount()` and don't handle `saleItems`). These are addressed in Phase 3.

#### Step 1.5: Commit

```
api(pos-system): add sale items to transaction OpenAPI schema

Add SaleItemRequest/SaleItemResponse DTOs. Move totalAmount to
server-computed (removed from creation request). Add saleItems array
to StoreTransactionCreationRequest and GetStoreTransactionResponse.
```

---

### Phase 2: UpdateStoreProductUseCase

#### Step 2.1: Create UpdateStoreProductUseCase

**File**: `pos-system/src/main/java/com/akademiaplus/store/usecases/UpdateStoreProductUseCase.java`

Pattern: Find-or-throw → map DTO onto existing entity → save → return response.

Dependencies: `StoreProductRepository`, `TenantContextHolder`, `ModelMapper`.

**Key difference from creation**: Maps onto the EXISTING entity (not a prototype bean). The ID remains intact because the TypeMap skips it.

#### Step 2.2: Register update TypeMap in PosModelMapperConfiguration

Add `registerStoreProductUpdateMap()` method. Skip rules identical to the creation map (skip `setStoreProductId`). Use `UpdateStoreProductUseCase.MAP_NAME` as the TypeMap name.

#### Step 2.3: Wire into StoreProductController

The controller implements `StoreProductsApi` which has `updateStoreProduct` from the OpenAPI. Add `UpdateStoreProductUseCase` as a constructor dependency and implement the override.

#### Step 2.4: Add `STORE_PRODUCT` to EntityType

Check `utilities/src/main/java/com/akademiaplus/utilities/EntityType.java` — if `STORE_PRODUCT` constant doesn't exist, add it following the existing pattern. Also add `STORE_TRANSACTION` and `STORE_SALE_ITEM`.

#### Step 2.5: Create unit test

**File**: `pos-system/src/test/java/com/akademiaplus/store/usecases/UpdateStoreProductUseCaseTest.java`

Test classes: `@Nested SuccessfulUpdate` (finds entity, maps DTO, saves, returns response) and `@Nested ProductNotFound` (throws `EntityNotFoundException`).

#### Step 2.6: Compile + test + commit

```bash
mvn clean compile -pl pos-system -am -DskipTests
mvn test -pl pos-system -am -Dtest="UpdateStoreProductUseCaseTest"
```

```
feat(pos-system): implement UpdateStoreProductUseCase with PUT endpoint

Add UpdateStoreProductUseCase with find-or-404, named TypeMap
mapping onto existing entity, and unit tests. Wire into
StoreProductController. Add STORE_PRODUCT/STORE_TRANSACTION
constants to EntityType.
```

---

### Phase 3: Refactor StoreTransactionCreationUseCase (Core Business Logic)

This is the central phase. The existing use case is a simple pass-through mapper. It needs to become a proper transactional workflow.

#### Step 3.1: Add new constants to the use case class

```java
public static final String ERROR_EMPTY_SALE_ITEMS = "Transaction must contain at least one sale item";
public static final String ERROR_PRODUCT_NOT_FOUND = "Store product not found with ID: %d";
public static final String ERROR_INSUFFICIENT_STOCK =
        "Insufficient stock for product '%s' (ID: %d): requested %d, available %d";
public static final String TRANSACTION_TYPE_SALE = "SALE";
public static final String TRANSACTION_TYPE_REFUND = "REFUND";
```

#### Step 3.2: Add new dependencies

Add `StoreProductRepository` to constructor injection. `ApplicationContext`, `StoreTransactionRepository`, `ModelMapper` already exist.

#### Step 3.3: Rewrite `create()` method

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

#### Step 3.4: Implement helper methods

Five new private methods, each with Javadoc:

1. **`validateSaleItems(dto)`** — throws `IllegalArgumentException(ERROR_EMPTY_SALE_ITEMS)` if `dto.getSaleItems()` is null or empty.

2. **`buildSaleItems(dto, transaction)`** — iterates `dto.getSaleItems()`, for each:
   - Looks up `StoreProductDataModel` via `storeProductRepository.findById(compositeKey)` or throws `EntityNotFoundException`
   - If `TRANSACTION_TYPE_SALE`, calls `validateStock(product, quantity)`
   - Creates `StoreSaleItemDataModel` via `applicationContext.getBean(StoreSaleItemDataModel.class)`
   - Sets `storeProductId`, `quantity`, `unitPriceAtSale = product.getPrice()`, `itemTotal = price × quantity`, `transaction` reference
   - Returns the assembled list

3. **`validateStock(product, requestedQuantity)`** — throws `IllegalArgumentException(String.format(ERROR_INSUFFICIENT_STOCK, ...))` if `product.getStockQuantity() < requestedQuantity`.

4. **`computeTotalAmount(saleItems)`** — `saleItems.stream().map(StoreSaleItemDataModel::getItemTotal).reduce(BigDecimal.ZERO, BigDecimal::add)`

5. **`adjustInventory(saleItems, transactionType)`** — for each sale item, fetches the product (or uses cached reference), decrements stock for SALE or increments for REFUND, calls `storeProductRepository.saveAndFlush(product)`.

#### Step 3.5: Keep existing `transform()` intact

The `transform()` method still handles DTO→header mapping via the named TypeMap. It remains public for independent testing.

#### Step 3.6: Update ModelMapper skip rules

Verify that the existing `PosModelMapperConfiguration.registerStoreTransactionMap()` already skips `setSaleItems`, `setEmployee`, `setTransactionDatetime`, `setStoreTransactionId`. No changes needed if those skip rules are present.

#### Step 3.7: Rewrite the unit test

Restructure with `@Nested` classes:

- **Validation**: `shouldThrowException_whenSaleItemsNull`, `shouldThrowException_whenSaleItemsEmpty`
- **ProductResolution**: `shouldThrowEntityNotFound_whenProductIdInvalid`
- **StockValidation**: `shouldThrowException_whenInsufficientStockForSale`, `shouldSkipStockValidation_whenTransactionTypeRefund`
- **PriceCaptureAndTotal**: `shouldCaptureCurrentProductPrice_asUnitPriceAtSale`, `shouldComputeItemTotal_asQuantityTimesUnitPrice`, `shouldComputeTransactionTotal_asSumOfItemTotals`
- **InventoryAdjustment**: `shouldDecrementStock_whenTransactionTypeSale`, `shouldIncrementStock_whenTransactionTypeRefund`
- **Persistence**: `shouldSaveTransactionWithCascadedSaleItems`, `shouldReturnMappedResponseDto`

Mock `applicationContext.getBean(StoreSaleItemDataModel.class)` with sequential returns for multiple items.

#### Step 3.8: Compile + test + commit

```bash
mvn clean compile -pl pos-system -am -DskipTests
mvn test -pl pos-system -am
```

```
feat(pos-system): add sale items and inventory management to transaction creation

Refactor StoreTransactionCreationUseCase to:
- Accept sale items array in creation request
- Resolve products and capture price-at-sale
- Validate stock availability for SALE transactions
- Compute totalAmount server-side from item totals
- Adjust inventory (decrement for SALE, increment for REFUND)
- Save transaction with cascaded sale items

Rewrite unit tests with full business logic coverage.
```

---

### Phase 4: Employee Context Injection

#### Step 4.1: Determine the security context access pattern

Read the existing security infrastructure:
```bash
find security/src/main/java -name "JwtTokenProvider.java" -o -name "JwtRequestFilter.java" | sort
grep -rn "SecurityContextHolder\|Authentication\|Principal" security/src/main/java/
```

Identify how to extract `employeeId` from the authenticated JWT. The exact implementation depends on how `JwtTokenProvider` structures the claims.

#### Step 4.2: Add optional employee resolution to StoreTransactionCreationUseCase

After building the transaction header, inject the employee ID:

```java
private Long extractEmployeeId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated()) {
        // Extract employeeId from JWT claims — exact field depends on JwtTokenProvider
        Object employeeIdClaim = /* extract from auth */;
        if (employeeIdClaim != null) {
            return Long.valueOf(employeeIdClaim.toString());
        }
    }
    return null;
}
```

Call in `create()` after `transform()`: `transaction.setEmployeeId(extractEmployeeId());`

**IMPORTANT**: Do NOT proceed until you understand the JWT claim structure from reading `JwtTokenProvider`.

#### Step 4.3: Test with mock SecurityContext

Add `@Nested "EmployeeContext"` with tests for authenticated (employeeId set) and unauthenticated (employeeId null) scenarios.

#### Step 4.4: Compile + test + commit

```bash
mvn clean compile -pl pos-system -am -DskipTests
mvn test -pl pos-system -am
```

```
feat(pos-system): inject employee context from JWT into transactions
```

---

### Phase 5: Enhanced Transaction Read Responses

#### Step 5.1: Read the current Get use cases

```bash
cat pos-system/src/main/java/com/akademiaplus/store/usecases/GetStoreTransactionByIdUseCase.java
cat pos-system/src/main/java/com/akademiaplus/store/usecases/GetAllStoreTransactionsUseCase.java
cat pos-system/src/main/java/com/akademiaplus/store/interfaceadapters/StoreTransactionRepository.java
```

#### Step 5.2: Add eager fetch for saleItems

In `StoreTransactionRepository`, add custom queries to eagerly load sale items:

**Option A** — `@EntityGraph`:
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

Start with Option A and verify. If it conflicts with the tenant filter, use Option B.

#### Step 5.3: Register SaleItem → SaleItemResponse TypeMap (if needed)

Test whether ModelMapper handles the nested mapping implicitly. If it does, no additional config needed. If not, add a TypeMap in `PosModelMapperConfiguration`:

```java
private void registerSaleItemResponseMap() {
    modelMapper.createTypeMap(
            StoreSaleItemDataModel.class,
            SaleItemResponseDTO.class
    ).implicitMappings();
}
```

#### Step 5.4: Compile + test + commit

```bash
mvn clean compile -pl pos-system -am -DskipTests
mvn test -pl pos-system -am
```

```
feat(pos-system): include sale items in transaction read responses

Update GetById and GetAll transaction use cases to map sale items
into the response DTO. Add eager fetch to prevent N+1 queries.
```

---

### Phase 6: Full Test Suite Verification

#### Step 6.1: Run all POS module tests

```bash
mvn test -pl pos-system -am
```

#### Step 6.2: Run full project build

```bash
mvn clean install -DskipTests
mvn test
```

#### Step 6.3: Verify controller integration

Check that both controllers compile and all `@Override` methods from the generated API interfaces are implemented.

---

## 6. File Inventory

| # | File | Action | Phase |
|---|------|--------|-------|
| 1 | `pos-system/src/main/resources/openapi/store-transaction.yaml` | Modify | 1 |
| 2 | `pos-system/src/main/resources/openapi/pos-system-module.yaml` | Modify | 1 |
| 3 | `pos-system/.../usecases/UpdateStoreProductUseCase.java` | Create | 2 |
| 4 | `pos-system/.../usecases/UpdateStoreProductUseCaseTest.java` | Create | 2 |
| 5 | `pos-system/.../config/PosModelMapperConfiguration.java` | Modify | 2, 3 |
| 6 | `pos-system/.../interfaceadapters/StoreProductController.java` | Modify | 2 |
| 7 | `pos-system/.../usecases/StoreTransactionCreationUseCase.java` | Rewrite | 3 |
| 8 | `pos-system/.../usecases/StoreTransactionCreationUseCaseTest.java` | Rewrite | 3 |
| 9 | `utilities/.../EntityType.java` | Modify (add STORE_PRODUCT, STORE_TRANSACTION) | 2 |
| 10 | `pos-system/.../usecases/GetStoreTransactionByIdUseCase.java` | Modify | 5 |
| 11 | `pos-system/.../usecases/GetAllStoreTransactionsUseCase.java` | Modify | 5 |
| 12 | `pos-system/.../interfaceadapters/StoreTransactionRepository.java` | Modify (add JOIN FETCH) | 5 |

---

## 7. Critical Reminders

1. **`totalAmount` is server-computed** — never trust client-provided values for financial fields.
2. **Inventory adjustment and transaction save MUST be in the same `@Transactional` boundary** — partial failures leave inconsistent state.
3. **`CascadeType.ALL`** on `saleItems` means a single `saveAndFlush` on the transaction persists all items. Do NOT save items individually.
4. **Stock validation is SALE-only** — REFUND transactions always succeed (they add stock back).
5. **`BigDecimal` for all money** — never use `double` or `float` in business logic. The entity already uses `BigDecimal`.
6. **Prototype-scoped beans** — use `applicationContext.getBean(StoreSaleItemDataModel.class)` for each sale item, not `new StoreSaleItemDataModel()`.
7. **The `transform()` method remains public** — it's tested independently and reused by `buildTransaction()`.
8. **Employee injection is optional** — transactions can exist without an employee (self-service kiosk scenario).
9. **Named TypeMap** — the existing `storeTransactionMap` handles header fields. Sale items are wired manually after mapping.
10. **Read the security module** before Phase 4 — the JWT claim structure determines the employee extraction logic.
