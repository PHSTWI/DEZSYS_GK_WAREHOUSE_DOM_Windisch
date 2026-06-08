package warehouse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import warehouse.model.ProductData;
import warehouse.model.WarehouseData;
import warehouse.repository.WarehouseRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@SpringBootApplication
public class Application implements CommandLineRunner {

	@Autowired
	private WarehouseRepository repository;

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	// 6 Kategorien mit je 10 Produkten = 60 Produkte pro Lager x 5 Lager = 300 gesamt
	static final String[][] PRODUCTS = {
			{"P-G01","Bio Orangensaft Sonne","Getraenke"},
			{"P-G02","Bio Apfelsaft Gold","Getraenke"},
			{"P-G03","Mineralwasser Still 1L","Getraenke"},
			{"P-G04","Eistee Zitrone 500ml","Getraenke"},
			{"P-G05","Multivitaminsaft 1L","Getraenke"},
			{"P-G06","Cola Classic 1.5L","Getraenke"},
			{"P-G07","Traubensaft Rot 1L","Getraenke"},
			{"P-G08","Ingwer Limonade","Getraenke"},
			{"P-G09","Kokosgetraenk 330ml","Getraenke"},
			{"P-G10","Granatapfelsaft 750ml","Getraenke"},

			{"P-W01","Ariel Color Pulver 3kg","Waschmittel"},
			{"P-W02","Persil Universal Gel 1.5L","Waschmittel"},
			{"P-W03","Weichspueler Lenor 1L","Waschmittel"},
			{"P-W04","Spuelmittel Fairy 500ml","Waschmittel"},
			{"P-W05","Domestos Bleiche 750ml","Waschmittel"},
			{"P-W06","Mr. Proper Citrus 1L","Waschmittel"},
			{"P-W07","Ariel Pods 42 Stueck","Waschmittel"},
			{"P-W08","Kuschelweich Weichspueler","Waschmittel"},
			{"P-W09","Glasreiniger Sidolin","Waschmittel"},
			{"P-W10","Badezimmer Reiniger Viss","Waschmittel"},

			{"P-T01","Mampfi Katzenfutter Rind","Tierfutter"},
			{"P-T02","Whiskas Lachs 400g","Tierfutter"},
			{"P-T03","Felix Gelee 12er Pack","Tierfutter"},
			{"P-T04","Pedigree Hundefutter Rind","Tierfutter"},
			{"P-T05","Royal Canin Kitten 2kg","Tierfutter"},
			{"P-T06","Purina ONE Katze 3kg","Tierfutter"},
			{"P-T07","Dreamies Katzensnacks","Tierfutter"},
			{"P-T08","Milkbone Hunde-Snacks","Tierfutter"},
			{"P-T09","Vogelkoerner Mix 1kg","Tierfutter"},
			{"P-T10","Kaninchenpellets 3kg","Tierfutter"},

			{"P-R01","Saugstauberbeutel 5er","Reinigung"},
			{"P-R02","Mikrofasertuecher 3er Pack","Reinigung"},
			{"P-R03","Toilettenpapier 16 Rollen","Reinigung"},
			{"P-R04","Muellsaecke 30L 20er Pack","Reinigung"},
			{"P-R05","Besen mit Stiel","Reinigung"},
			{"P-R06","Abflussreiniger 500ml","Reinigung"},
			{"P-R07","WC-Stein 4er Pack","Reinigung"},
			{"P-R08","Gummihandschuhe M 3er","Reinigung"},
			{"P-R09","Wischmopp Set","Reinigung"},
			{"P-R10","Staubwedel Mikrofaser","Reinigung"},

			{"P-E01","AA Batterien 10er Pack","Elektronik"},
			{"P-E02","USB-C Kabel 1m","Elektronik"},
			{"P-E03","HDMI Kabel 2m","Elektronik"},
			{"P-E04","Mehrfachsteckdose 4-fach","Elektronik"},
			{"P-E05","LED Gluehbirne E27 8W","Elektronik"},
			{"P-E06","LAN Kabel CAT6 5m","Elektronik"},
			{"P-E07","Taschenlampe LED","Elektronik"},
			{"P-E08","Verlaengerungskabel 5m","Elektronik"},
			{"P-E09","Sicherung 16A 5er Pack","Elektronik"},
			{"P-E10","Steckdose mit USB","Elektronik"},

			{"P-L01","Mehl Type 700 1kg","Lebensmittel"},
			{"P-L02","Zucker 1kg","Lebensmittel"},
			{"P-L03","Olivenoel Extra Virgin 500ml","Lebensmittel"},
			{"P-L04","Nudeln Spaghetti 500g","Lebensmittel"},
			{"P-L05","Reis Langkorn 1kg","Lebensmittel"},
			{"P-L06","Honig Wildblume 250g","Lebensmittel"},
			{"P-L07","Kaffeepulver Jacobs 500g","Lebensmittel"},
			{"P-L08","Tomatensauce Sugo 400g","Lebensmittel"},
			{"P-L09","Haferflocken 500g","Lebensmittel"},
			{"P-L10","Schwarztee 25 Beutel","Lebensmittel"},
	};

	static final String[][] WAREHOUSES = {
			{"1", "Zentrallager Wien",    "Lagerstrasse 1",   "Wien",      "Austria"},
			{"2", "Lager Graz",           "Industriegasse 5", "Graz",      "Austria"},
			{"3", "Lager Linz",           "Hafenweg 12",      "Linz",      "Austria"},
			{"4", "Lager Salzburg",       "Gewerbepark 3",    "Salzburg",  "Austria"},
			{"5", "Lager Innsbruck",      "Tirolerstrasse 8", "Innsbruck", "Austria"},
	};

	@Override
	public void run(String... args) {
		repository.deleteAll();

		Random rnd = new Random(42);

		for (String[] wh : WAREHOUSES) {
			WarehouseData warehouse = new WarehouseData(
					wh[0], wh[1], wh[2], wh[3], wh[4], "2024-12-01 08:00:00"
			);

			List<ProductData> products = new ArrayList<>();
			for (String[] p : PRODUCTS) {
				double qty = 10 + rnd.nextInt(991); // 10 – 1000
				products.add(new ProductData(wh[0], p[0], p[1], p[2], qty));
			}
			warehouse.setProducts(products);
			repository.save(warehouse);
		}

		long total = repository.findAll().stream()
				.mapToLong(w -> w.getProducts().size()).sum();

		System.out.println("Warehouses: " + repository.count());
		System.out.println("Products total: " + total);
	}
}