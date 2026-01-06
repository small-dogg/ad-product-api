package com.smalldogg.adproductapi.product;

import com.smalldogg.adproductapi.product.enums.ProductSearchType;
import com.smalldogg.adproductapi.product.enums.ProductSortType;
import com.smalldogg.adproductapi.product.response.BulkAddResponse;
import com.smalldogg.adproductapi.product.response.ProductApiResponse;
import com.smalldogg.adproductapi.product.response.ProductListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
@RestController
public class ProductController {

    private final ProductService productService;


    @GetMapping
    public ProductListResponse getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) Long partnerId,
            @RequestParam(defaultValue = "ID") ProductSortType sortType,
            @RequestParam(required = false) String searchKeyword,
            @RequestParam(defaultValue = "NAME") ProductSearchType searchType
    ) {
        return productService.getProducts(page, limit, partnerId, sortType, searchKeyword, searchType);
    }

    @GetMapping("/{productId}")
    public ProductApiResponse getProduct(@PathVariable("productId") Long productId) {
        return productService.getProduct(productId);
    }

    @PostMapping(value="bulk-add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkAddResponse> bulkAdd(@RequestPart("file") MultipartFile file) throws Exception {
        BulkAddResponse result = productService.importCsv(file, true); // true=UPSERT
        return ResponseEntity.ok(new BulkAddResponse(
                result.totalLines(),
                result.insertedOrUpdatedRows(),
                result.skippedRows()
        ));
    }
}
