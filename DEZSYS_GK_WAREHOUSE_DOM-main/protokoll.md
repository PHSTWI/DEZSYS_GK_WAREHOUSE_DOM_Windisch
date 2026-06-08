# DEZSYS_GK_WAREHOUSE_DOM – Protokoll Philipp Windisch
**Middleware Engineering: Document Oriented Middleware using MongoDB**

---

## Aufgabenstellung

Ziel war die Implementierung einer dokumentenorientierten Middleware mit Spring Boot und MongoDB. Ein zentrales System soll Lagerdaten von mehreren Standorten über eine REST-Schnittstelle empfangen, in einer MongoDB-Datenbank im JSON-Format abspeichern und für verschiedene Abfragen des Betriebs (Management, Einkauf, Vertrieb) bereitstellen.

---

## Vorgehensweise

### 1. MongoDB starten

MongoDB läuft in einem Docker-Container. Gestartet und verbunden wird er wie folgt:

```bash
docker exec -it mongo bash
mongosh
show dbs
use test
```

---

### 2. Datenstruktur

Statt zwei getrennten Collections (eine für Warehouses, eine für Products) wurde eine **eingebettete Dokumentenstruktur** gewählt: Produkte sind direkt im Warehouse-Dokument als Array gespeichert.

**Vorteile dieses Ansatzes:**
- Eine einzige Abfrage liefert ein Lager inklusive aller Produkte – kein JOIN nötig
- Entspricht dem typischen NoSQL-Designprinzip: Daten so speichern, wie sie abgefragt werden

**Beispiel-Dokument in MongoDB:**
```json
{
  "warehouseID": "1",
  "warehouseName": "Zentrallager Wien",
  "warehouseAddress": "Lagerstrasse 1",
  "warehouseCity": "Wien",
  "warehouseCountry": "Austria",
  "timestamp": "2024-12-01 08:00:00",
  "products": [
    {
      "productID": "P-G01",
      "productName": "Bio Orangensaft Sonne",
      "productCategory": "Getraenke",
      "productQuantity": 542
    }
  ]
}
```

---

### 3. Modellklassen

**`WarehouseData.java`** – Haupt-Dokument, direkt in MongoDB gespeichert:

```java
@Document(collection = "warehouseData")
public class WarehouseData {
    @Id
    private String id;          // interne MongoDB-ID
    private String warehouseID;
    private String warehouseName;
    private String warehouseAddress;
    private String warehouseCity;
    private String warehouseCountry;
    private String timestamp;
    private List<ProductData> products; // eingebettete Produkte
}
```

**`ProductData.java`** – kein eigenes `@Document`, da es ins Warehouse eingebettet wird:

```java
public class ProductData {
    private String warehouseID;
    private String productID;
    private String productName;
    private String productCategory;
    private double productQuantity;
}
```

---

### 4. Repository

```java
public interface WarehouseRepository extends MongoRepository<WarehouseData, String> {
    Optional<WarehouseData> findByWarehouseID(String warehouseID);
}
```

`MongoRepository` ist ein Spring Data Interface, das automatisch alle Standard-CRUD-Operationen (`save`, `findAll`, `delete`, ...) bereitstellt – ohne dass man SQL oder MongoDB-Queries manuell schreiben muss.

Es wird keine `application.properties` benötigt, da Spring Boot eine **Auto-Configuration** mitbringt. Wenn keine explizite Konfiguration vorhanden ist, verbindet sich Spring Boot automatisch mit den folgenden Defaults:

- Host: `localhost`
- Port: `27017`
- Datenbank: `test`

Deshalb landen die Daten ohne weitere Konfiguration direkt in der `test`-Datenbank – genau dort, wo `mongosh` sie findet. `findByWarehouseID` wird von Spring Data anhand des Methodennamens automatisch in die passende MongoDB-Abfrage `{ warehouseID: "..." }` übersetzt.

---

### 5. REST-Controller

Alle Endpunkte gemäß Angabe wurden implementiert:

| Methode | Endpunkt | Beschreibung |
|--------|----------|--------------|
| `POST` | `/warehouse` | Neuen Lagerstandort anlegen |
| `GET` | `/warehouse` | Alle Lager mit Produkten abrufen |
| `GET` | `/warehouse/{id}` | Ein Lager per ID abrufen |
| `DELETE` | `/warehouse/{id}` | Lager löschen |
| `POST` | `/product` | Produkt zu einem Lager hinzufügen |
| `GET` | `/product` | Alle Produkte aller Lager abrufen |
| `GET` | `/product/{id}` | Ein Produkt und seine Lagerstandorte abrufen |
| `DELETE` | `/product/{id}` | Produkt aus einem oder allen Lagern löschen |

Da Produkte eingebettet sind, muss für produkt-bezogene Abfragen über alle Warehouse-Dokumente iteriert werden. Beispiel für `GET /product`:

```java
List<ProductData> all = repository.findAll().stream()
    .flatMap(wh -> wh.getProducts().stream()
        .peek(p -> p.setWarehouseID(wh.getWarehouseID())))
    .collect(Collectors.toList());
```

`flatMap` entfaltet alle Produkt-Listen aller Lager in eine einzige flache Liste. `peek` setzt dabei die `warehouseID` auf jedem Produkt, damit der Client weiß, aus welchem Lager es stammt.

`DELETE /product/{id}` unterstützt einen optionalen Query-Parameter `?warehouseID=1` – ohne diesen wird das Produkt aus allen Lagern entfernt:

```java
List<WarehouseData> targets = warehouseID != null
    ? repository.findByWarehouseID(warehouseID).map(List::of).orElse(List.of())
    : repository.findAll();
```

---

### 6. Testdaten-Generierung

Beim Start der Applikation (`Application.java`) werden automatisch Testdaten generiert:

- **5 Lager** × **60 Produkte** (6 Kategorien × 10 Produkte) = **300 Produkte gesamt**
- Lagermengen werden mit `Random(42)` (fixer Seed) zufällig zwischen 10 und 1000 erzeugt – reproduzierbar bei jedem Start

```java
Random rnd = new Random(42);
for (String[] wh : WAREHOUSES) {
    List<ProductData> products = new ArrayList<>();
    for (String[] p : PRODUCTS) {
        double qty = 10 + rnd.nextInt(991);
        products.add(new ProductData(wh[0], p[0], p[1], p[2], qty));
    }
}
```

---

## MongoDB Shell – Pflichtbefehle

**Lagerstand eines Produktes über alle Lagerstandorte:**
```js
db.warehouseData.aggregate([
  { $unwind: "$products" },
  { $match: { "products.productID": "P-G01" } },
  { $group: {
      _id: "$products.productID",
      totalQuantity: { $sum: "$products.productQuantity" },
      warehouses: { $push: { wh: "$warehouseID", qty: "$products.productQuantity" } }
  }}
])
```

**Lagerstand eines Produktes in einem bestimmten Lager:**
```js
db.warehouseData.aggregate([
  { $match: { warehouseID: "2" } },
  { $unwind: "$products" },
  { $match: { "products.productID": "P-G01" } },
  { $project: { _id: 0, warehouseID: 1, "products.productQuantity": 1 } }
])
```

---

## 5 CRUD-Operationen (Mongo Shell)

**1. CREATE – Produkt hinzufügen**
```js
db.warehouseData.updateOne(
  { warehouseID: "1" },
  { $push: { products: {
      productID: "P-X99",
      productName: "Testprodukt",
      productCategory: "Lebensmittel",
      productQuantity: 100,
      warehouseID: "1"
  }}}
)
```

**2. READ – Alle Getränke, sortiert nach Menge (Top 5)**
```js
db.warehouseData.aggregate([
  { $unwind: "$products" },
  { $match: { "products.productCategory": "Getraenke" } },
  { $sort: { "products.productQuantity": -1 } },
  { $limit: 5 }
])
```

**3. UPDATE – Lagerbestand eines Produktes ändern**
```js
db.warehouseData.updateOne(
  { warehouseID: "1", "products.productID": "P-G01" },
  { $set: { "products.$.productQuantity": 500 } }
)
```

**4. DELETE – Produkt aus einem Lager entfernen**
```js
db.warehouseData.updateOne(
  { warehouseID: "1" },
  { $pull: { products: { productID: "P-X99" } } }
)
```

**5. AGGREGATE – Produkte mit Gesamtbestand unter 200 Stück**
```js
db.warehouseData.aggregate([
  { $unwind: "$products" },
  { $group: {
      _id: "$products.productID",
      productName: { $first: "$products.productName" },
      totalQty: { $sum: "$products.productQuantity" }
  }},
  { $match: { totalQty: { $lt: 200 } } },
  { $sort: { totalQty: 1 } }
])
```

---

## 3 Fragestellungen für das Berichtswesen

**Frage 1 – Welches Lager hat den höchsten Gesamtbestand?**
```js
db.warehouseData.aggregate([
  { $unwind: "$products" },
  { $group: {
      _id: "$warehouseID",
      warehouseName: { $first: "$warehouseName" },
      totalQty: { $sum: "$products.productQuantity" }
  }},
  { $sort: { totalQty: -1 } }
])
```
*Ergebnis: Zentrallager Wien führt mit 29.769 Einheiten.*

**Frage 2 – Wie ist der Gesamtbestand je Produktkategorie?**
```js
db.warehouseData.aggregate([
  { $unwind: "$products" },
  { $group: {
      _id: "$products.productCategory",
      totalQty: { $sum: "$products.productQuantity" },
      count: { $sum: 1 }
  }},
  { $sort: { totalQty: -1 } }
])
```
*Ergebnis: Getränke führen mit 26.990 Einheiten (50 Einträge über alle Lager).*

**Frage 3 – Welche Produkte haben einen Gesamtbestand unter 1.000 Stück über alle Lager?**
```js
db.warehouseData.aggregate([
  { $unwind: "$products" },
  { $group: {
      _id: "$products.productID",
      productName: { $first: "$products.productName" },
      totalQty: { $sum: "$products.productQuantity" }
  }},
  { $match: { totalQty: { $lt: 1000 } } },
  { $sort: { totalQty: 1 } }
])
```
*Ergebnis: Nur P-T06 „Purina ONE Katze 3kg" unterschreitet mit 926 Einheiten die 1.000er-Grenze.*

---

## Fragestellungen

### 1. Vier Vorteile von NoSQL gegenüber einem relationalen DBMS

- **Flexibles Schema:** Neue Felder können jederzeit ohne aufwendige Migrationsskripte hinzugefügt werden. Das ist besonders vorteilhaft, wenn sich Datenstrukturen häufig ändern.
- **Horizontale Skalierbarkeit:** NoSQL-Datenbanken wie MongoDB unterstützen Sharding nativ – die Last lässt sich auf viele Server verteilen, ohne die Anwendungslogik zu ändern.
- **Native JSON/BSON-Speicherung:** Java-Objekte werden direkt als Dokumente gespeichert, ohne den Umweg über ein Object-Relational-Mapping. Das reduziert Komplexität und verbessert die Performance.
- **Bessere Performance bei eingebetteten Strukturen:** Eine einzige Datenbankabfrage liefert ein komplettes Lager inklusive aller Produkte – bei einem relationalen System wären dafür mehrere JOINs nötig.

### 2. Vier Nachteile von NoSQL gegenüber einem relationalen DBMS

- **Keine referentielle Integrität:** Es gibt keine Foreign Keys oder Constraints – Datenkonsistenz muss vollständig in der Anwendungslogik sichergestellt werden.
- **Kein standardisiertes Abfragemodell:** Anstelle von SQL gibt es herstellerspezifische Abfragesprachen. Wissen ist schwerer übertragbar, und komplexe Abfragen sind oft umständlicher zu formulieren.
- **Eingeschränkte Transaktionsunterstützung:** Atomare Transaktionen über mehrere Dokumente hinweg sind erst ab MongoDB 4.0 möglich und deutlich schwerfälliger als in relationalen Systemen.
- **Datenredundanz durch Embedding:** Da Daten oft mehrfach eingebettet werden (z.B. `warehouseID` in jedem Produkt), entsteht Redundanz. Änderungen müssen an mehreren Stellen manuell synchronisiert werden.

### 3. Schwierigkeiten bei der Zusammenführung der Daten

Bei der zentralen Zusammenführung von Daten mehrerer Lagerstandorte können folgende Probleme auftreten: Unterschiedliche Standorte können abweichende Feldnamen oder Datenformate verwenden (Schemainkonsistenz). Ohne eine einheitliche ID-Strategie entstehen Duplikate. Zeitstempel ohne UTC-Normalisierung machen zeitliche Auswertungen unzuverlässig. Außerdem können Netzwerkausfälle zu inkonsistenten Zwischenständen führen, wenn Daten nicht transaktional übertragen werden.

### 4. Arten von NoSQL-Datenbanken

| Typ | Beschreibung | Vertreter |
|-----|-------------|-----------|
| **Document Store** | Speichert Daten als JSON/BSON-Dokumente | MongoDB |
| **Key-Value Store** | Einfache Schlüssel-Wert-Paare, sehr schnell | Redis |
| **Wide-Column Store** | Spaltenorientiert, gut für Zeitreihendaten | Apache Cassandra |
| **Graph Database** | Optimiert für stark vernetzte Daten | Neo4j |

### 5. CAP-Theorem

Das CAP-Theorem besagt, dass ein verteiltes System nur zwei der drei folgenden Eigenschaften gleichzeitig garantieren kann:

- **CA – Consistency + Availability:** Das System ist immer konsistent und verfügbar, bietet aber keinen Schutz bei Netzwerkpartitionierung. Klassische relationale Datenbanken fallen in diese Kategorie.
- **CP – Consistency + Partition Tolerance:** Das System bleibt auch bei Netzwerkausfällen konsistent, akzeptiert dafür aber kurzzeitige Nichtverfügbarkeit. MongoDB mit `majority`-Read-Concern ist ein Beispiel.
- **AP – Availability + Partition Tolerance:** Das System ist immer erreichbar und partitionsresistent, liefert aber bei Ausfällen eventuell veraltete Daten (*Eventual Consistency*). Cassandra ist ein typischer Vertreter.