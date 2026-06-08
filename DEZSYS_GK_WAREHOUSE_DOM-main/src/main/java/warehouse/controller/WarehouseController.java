package warehouse.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import warehouse.model.ProductData;
import warehouse.model.WarehouseData;
import warehouse.repository.WarehouseRepository;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class WarehouseController {

    @Autowired
    private WarehouseRepository repository;

    private String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    // ── WAREHOUSE ENDPOINTS ───────────────────────────────────────────────────

    /** POST /warehouse – neuen Lagerstandort anlegen */
    @PostMapping("/warehouse")
    public ResponseEntity<WarehouseData> addWarehouse(@RequestBody WarehouseData warehouse) {
        if (warehouse.getTimestamp() == null) warehouse.setTimestamp(now());
        return new ResponseEntity<>(repository.save(warehouse), HttpStatus.CREATED);
    }

    /** GET /warehouse – alle Lagerstandorte mit Produkten */
    @GetMapping("/warehouse")
    public ResponseEntity<List<WarehouseData>> getAllWarehouses() {
        return ResponseEntity.ok(repository.findAll());
    }

    /** GET /warehouse/{id} – ein Lagerstandort per warehouseID */
    @GetMapping("/warehouse/{id}")
    public ResponseEntity<WarehouseData> getWarehouse(@PathVariable String id) {
        return repository.findByWarehouseID(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** DELETE /warehouse/{id} – Lagerstandort löschen */
    @DeleteMapping("/warehouse/{id}")
    public ResponseEntity<String> deleteWarehouse(@PathVariable String id) {
        Optional<WarehouseData> wh = repository.findByWarehouseID(id);
        if (wh.isEmpty()) return ResponseEntity.notFound().build();
        repository.delete(wh.get());
        return ResponseEntity.ok("Warehouse " + id + " deleted.");
    }

    @PostMapping("/product")
    public ResponseEntity<WarehouseData> addProduct(@RequestBody ProductData product) {
        Optional<WarehouseData> opt = repository.findByWarehouseID(product.getWarehouseID());
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        WarehouseData wh = opt.get();
        if (product.getProductID() == null)
            product.setProductID("P-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        wh.getProducts().add(product);
        return new ResponseEntity<>(repository.save(wh), HttpStatus.CREATED);
    }


    /** GET /product – alle Produkte aller Lager */
    @GetMapping("/product")
    public ResponseEntity<List<ProductData>> getAllProducts() {
        List<ProductData> all = repository.findAll().stream()
                .flatMap(wh -> wh.getProducts().stream()
                        .peek(p -> p.setWarehouseID(wh.getWarehouseID())))
                .collect(Collectors.toList());
        return ResponseEntity.ok(all);
    }

    /** GET /product/{id} – ein Produkt und alle Lagerstandorte die es führen */
    @GetMapping("/product/{id}")
    public ResponseEntity<List<ProductData>> getProduct(@PathVariable String id) {
        List<ProductData> found = repository.findAll().stream()
                .flatMap(wh -> wh.getProducts().stream()
                        .filter(p -> id.equals(p.getProductID()))
                        .peek(p -> p.setWarehouseID(wh.getWarehouseID())))
                .collect(Collectors.toList());
        if (found.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(found);
    }

    /**
     * DELETE /product/{id} – Produkt aus einem Lager entfernen
     * Query-Parameter ?warehouseID=WH-001 (optional; ohne = aus allen Lagern löschen)
     */
    @DeleteMapping("/product/{id}")
    public ResponseEntity<String> deleteProduct(
            @PathVariable String id,
            @RequestParam(required = false) String warehouseID) {

        List<WarehouseData> targets = warehouseID != null
                ? repository.findByWarehouseID(warehouseID).map(List::of).orElse(List.of())
                : repository.findAll();

        int removed = 0;
        for (WarehouseData wh : targets) {
            int before = wh.getProducts().size();
            wh.getProducts().removeIf(p -> id.equals(p.getProductID()));
            if (wh.getProducts().size() < before) {
                repository.save(wh);
                removed++;
            }
        }
        if (removed == 0) return ResponseEntity.notFound().build();
        return ResponseEntity.ok("Product " + id + " removed from " + removed + " warehouse(s).");
    }
}