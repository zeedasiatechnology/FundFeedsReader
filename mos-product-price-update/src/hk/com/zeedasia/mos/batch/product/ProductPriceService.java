package hk.com.zeedasia.mos.batch.product;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.mail.MessagingException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;

import hk.com.zeedasia.framework.http.RestService;
import hk.com.zeedasia.framework.mail.MailException;
import hk.com.zeedasia.framework.mail.MailSender;
import hk.com.zeedasia.framework.mail.MailTemplate;
import hk.com.zeedasia.framework.mail.MailTemplateLoader;
import hk.com.zeedasia.framework.util.CSVReader;
import hk.com.zeedasia.framework.util.JsonUtils;
import hk.com.zeedasia.framework.util.PropertiesUtils;
import hk.com.zeedasia.mos.batch.lib.dao.MstProductDao;
import hk.com.zeedasia.mos.batch.lib.dao.MstProductFundDao;
import hk.com.zeedasia.mos.batch.lib.entity.MstProduct;
import hk.com.zeedasia.mos.batch.lib.entity.MstProductFund;

public class ProductPriceService {
	private static Logger logger = Logger.getLogger("ProductPriceService.class");

	private Properties properties;
	private List<String> columnList;
	private int mappingCodeInd;
	private int priceInd;
	private int priceDateInd;

	private String urlStr;

	public ProductPriceService() throws IOException {
		this(null);
	}

	public ProductPriceService(Properties properties) throws IOException {
		logger.info("Initialising ProductPriceService");
		if (properties == null) {
			properties = PropertiesUtils.getProperties(BatchConstants.PROPERTIES_FILE);
		}

		urlStr = properties.getProperty(BatchConstants.PROP_PRODUCT_PRICE_UPDATE_API);
		this.properties = properties;
	}

	public String updateProductPrice(String jsonStr) throws IOException {
		logger.info(jsonStr);
		return RestService.httpPost(urlStr, jsonStr);
	}

	public void updateProductPriceByCSV(String csvFilePath) throws IOException {
		File csvFile = new File(csvFilePath);
		updateProductPriceByCSV(csvFile);
	}

	public boolean isPriceOutdated(MstProduct product, Date priceDate) {
		Date productPriceDate = product.getPriceDate();

		if (productPriceDate == null) {
			return false;
		}

		return productPriceDate.after(priceDate);
	}

	public void updateProductPriceByCSV(File csvFile) throws IOException {
		MstProductFundDao dao = null;
		String message = "";

		try {
			char delimiter = getCSVDelimiter();

			CSVReader reader = new CSVReader(csvFile);
			reader.setDelimiter(delimiter);

			if (reader.hasNext()) {
				String line = reader.nextLine();
				String header = properties.getProperty(BatchConstants.PROP_CSV_HEADER);
				checkCSVHeader(header, line);
				columnList = CSVReader.parseLine(header, delimiter);
				setFieldIndex();
			}

			message += "- Imported Fund Price file: " + csvFile.getName() + "<br />";
			logger.info("Imported Fund Price file: " + csvFile.getName());

			Properties daoProp = PropertiesUtils.getProperties(BatchConstants.DAO_PROPERTIES_FILE);
			dao = new MstProductFundDao(daoProp);
			List<ProductPriceBean> beanList = new ArrayList<ProductPriceBean>();
			List<String> lipperIdList = new ArrayList<String>();

			while (reader.hasNext()) {
				List<String> line = reader.next();
				String lipperId = line.get(mappingCodeInd);

				if (StringUtils.isBlank(lipperId)) {
					continue;
				}

				if (lipperIdList.contains(lipperId)) {
					message += "- Duplicate Lipper ID: " + lipperId + "<br />";
					logger.info("Duplicate Lipper ID: " + lipperId);
					continue;
				}

				List<MstProductFund> productFundList = dao.getMstProductFundByLipperId(lipperId);

				for (MstProductFund productFund : productFundList) {
					String productCode = productFund.getProductCode();
					String exchangeCode = productFund.getExchangeCode();

					if (StringUtils.isBlank(productCode)) {
						continue;
					}

					String priceDateStr = line.get(priceDateInd);
					String price = line.get(priceInd);

					if ((StringUtils.isBlank(price)) || (StringUtils.isBlank(priceDateStr))) {
						message += "- No price/price date. Lipper ID: " + lipperId + "<br />";
						logger.info("No price/price date. Lipper ID: " + lipperId);
						continue;
					}

					try {
						Date priceDate = DateUtils.parseDate(priceDateStr, new String[] { "yyyyMMdd" });
						priceDateStr = DateFormatUtils.format(priceDate, "yyyy-MM-dd'T'00:00:00");

						MstProductDao productDao = new MstProductDao(daoProp);
						MstProduct product = productDao.getMstProduct(exchangeCode, productCode);

						if (isPriceOutdated(product, priceDate)) {
							message += "- Skipped outdated price. Lipper ID: " + lipperId + ", Product Price Date: "
									+ product.getPriceDate() + ", Price Date: "
									+ DateFormatUtils.format(priceDate, "yyyy-MM-dd") + "<br />";
							logger.info("Skipped outdated price. Lipper ID: " + lipperId + ", Product Price Date: "
									+ product.getPriceDate() + ", Price Date: "
									+ DateFormatUtils.format(priceDate, "yyyy-MM-dd"));
							continue;
						}
					} catch (ParseException e) {
						logger.warning(e.getMessage());
						continue;
					}

					ProductPriceBean bean = new ProductPriceBean();
					bean.setMappingCode(lipperId);
					bean.setProductCode(productCode);
					bean.setExchangeCode(exchangeCode);
					bean.setPrice(price);
					bean.setPriceDate(priceDateStr);
					bean.setPriceSource(BatchConstants.DEFAULT_PRICE_SOURCE);
					beanList.add(bean);

					lipperIdList.add(lipperId);
				}
			}

			if (lipperIdList.size() > 0) {
				String jsonStr = JsonUtils.toJson(beanList);
				updateProductPrice(jsonStr);
			}

			message += "- " + lipperIdList.size() + " product price updated";
			logger.info(lipperIdList.size() + " product price updated");
			sendEmail(message);
		} finally {
			if (dao != null) {
				dao.close();
			}
		}
	}

	protected void sendEmail(String message) {
		String to = properties.getProperty(BatchConstants.PROP_MAIL_TO);
		String templateFilePath = properties.getProperty(BatchConstants.PROP_MAIL_TEMP_UPDATE_NOTICE);
		File templateFile = new File(templateFilePath);

		MailTemplate template = MailTemplateLoader.load(templateFile);
		String subject = template.getSubject();
		String content = template.getContent();
		content = content.replace("${message}", message);

		try {
			MailSender sender = MailSender.getInstance();
			sender.init();
			sender.sendHtml(to, subject, content);
			logger.info("Email sent to: " + to);
		} catch (MessagingException | MailException e) {
			logger.info("Exception while sending email: " + e.getMessage());
		}
	}

	private void setFieldIndex() throws IOException {
		String reportMapJson = properties.getProperty(BatchConstants.PROP_CSV_MAP_JSON);
		Map<String, String> csvMap = JsonUtils.toMap(reportMapJson);
		logger.info(csvMap.toString());

		String mappingCodeCol = csvMap.get(BatchConstants.FIELD_MAPPING_CODE);
		String priceCol = csvMap.get(BatchConstants.FIELD_PRICE);
		String priceDateCol = csvMap.get(BatchConstants.FIELD_PRICE_DATE);

		mappingCodeInd = columnList.indexOf(mappingCodeCol);
		priceInd = columnList.indexOf(priceCol);
		priceDateInd = columnList.indexOf(priceDateCol);

		logger.info("mappingCode Index: " + mappingCodeInd);
		logger.info("price Index: " + priceInd);
		logger.info("priceDate Index: " + priceDateInd);
	}

	private char getCSVDelimiter() throws IOException {
		String delimiterStr = properties.getProperty(BatchConstants.PROP_CSV_DELIMITER);
		if ((delimiterStr == null) || (delimiterStr.length() != 1)) {
			throw new IOException("Invalid CSV Delimiter");
		}

		return delimiterStr.charAt(0);
	}

	private void checkCSVHeader(String header, String firstLine) throws IOException {
		if (!firstLine.equals(header)) {
			throw new IOException("Invalid CSV header: " + firstLine);
		}
	}
}
