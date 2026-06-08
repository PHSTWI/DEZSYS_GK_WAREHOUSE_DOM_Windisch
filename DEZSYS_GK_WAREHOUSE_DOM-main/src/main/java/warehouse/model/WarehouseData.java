package warehouse.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "warehouseData")
public class WarehouseData {

    @Id
    private String id;

    private String warehouseID;
    private String warehouseName;
    private String warehouseAddress;
    private String warehouseCity;
    private String warehouseCountry;
    private String timestamp;


    private List<ProductData> products = new ArrayList<>();

    public WarehouseData() {}

    public WarehouseData(String warehouseID, String warehouseName,
                         String warehouseAddress, String warehouseCity,
                         String warehouseCountry, String timestamp) {
        this.warehouseID = warehouseID;
        this.warehouseName = warehouseName;
        this.warehouseAddress = warehouseAddress;
        this.warehouseCity = warehouseCity;
        this.warehouseCountry = warehouseCountry;
        this.timestamp = timestamp;
    }

    public String getId()                        { return id; }
    public void setId(String id)                 { this.id = id; }

    public String getWarehouseID()               { return warehouseID; }
    public void setWarehouseID(String v)         { this.warehouseID = v; }

    public String getWarehouseName()             { return warehouseName; }
    public void setWarehouseName(String v)       { this.warehouseName = v; }

    public String getWarehouseAddress()          { return warehouseAddress; }
    public void setWarehouseAddress(String v)    { this.warehouseAddress = v; }

    public String getWarehouseCity()             { return warehouseCity; }
    public void setWarehouseCity(String v)       { this.warehouseCity = v; }

    public String getWarehouseCountry()          { return warehouseCountry; }
    public void setWarehouseCountry(String v)    { this.warehouseCountry = v; }

    public String getTimestamp()                 { return timestamp; }
    public void setTimestamp(String v)           { this.timestamp = v; }

    public List<ProductData> getProducts()       { return products; }
    public void setProducts(List<ProductData> v) { this.products = v; }
}

