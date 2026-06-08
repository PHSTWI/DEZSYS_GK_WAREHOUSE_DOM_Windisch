package warehouse.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import warehouse.model.WarehouseData;

import java.util.Optional;

public interface WarehouseRepository extends MongoRepository<WarehouseData, String> {

    Optional<WarehouseData> findByWarehouseID(String warehouseID);

}