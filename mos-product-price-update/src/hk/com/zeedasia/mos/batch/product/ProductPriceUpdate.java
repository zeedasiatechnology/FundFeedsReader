package hk.com.zeedasia.mos.batch.product;

import java.util.logging.Logger;

import hk.com.zeedasia.framework.util.PropertiesUtils;

public class ProductPriceUpdate {
	private static Logger logger = Logger.getLogger("ProductPriceUpdate.class");

	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.err.println("Usage: java ProductPriceUpdate [CSV file]");
			return;
		}
		String csvFilePath = args[0];

		PropertiesUtils.loadLogProperties(BatchConstants.LOG_PROPERTIES_FILE);
		ProductPriceService service = new ProductPriceService();
		service.updateProductPriceByCSV(csvFilePath);
		logger.info("ProductPriceUpdate batch end");
	}
}
