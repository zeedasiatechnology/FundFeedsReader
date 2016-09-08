package hk.com.zeedasia.mos.batch.product;

public class ProductPriceBean {
	private String mappingCode; // lipper_id
	private String productCode;
	private String price;
	private String priceDate; // Format (YYYY-MM-ddTHH:mm:ss)
	private String priceSource; // R: Thomson Reuters/M: Manual
	private String exchangeCode; // FUNDS

	public ProductPriceBean() {
	}

	public ProductPriceBean(String mappingCode, String productCode, String price, String priceDate, String priceSource,
			String exchangeCode) {
		this.mappingCode = mappingCode;
		this.productCode = productCode;
		this.price = price;
		this.priceDate = priceDate;
		this.priceSource = priceSource;
		this.exchangeCode = exchangeCode;
	}

	public String getProductCode() {
		return productCode;
	}

	public void setProductCode(String productCode) {
		this.productCode = productCode;
	}

	public String getMappingCode() {
		return mappingCode;
	}

	public void setMappingCode(String mappingCode) {
		this.mappingCode = mappingCode;
	}

	public String getPriceDate() {
		return priceDate;
	}

	public void setPriceDate(String priceDate) {
		this.priceDate = priceDate;
	}

	public String getPrice() {
		return price;
	}

	public void setPrice(String price) {
		this.price = price;
	}

	public String getPriceSource() {
		return priceSource;
	}

	public void setPriceSource(String priceSource) {
		this.priceSource = priceSource;
	}

	public String getExchangeCode() {
		return exchangeCode;
	}

	public void setExchangeCode(String exchangeCode) {
		this.exchangeCode = exchangeCode;
	}

	@Override
	public String toString() {
		return "ProductPriceDto [mappingCode=" + mappingCode + ", productCode=" + productCode + ", price=" + price
				+ ", priceDate=" + priceDate + ", priceSource=" + priceSource + ", exchangeCode=" + exchangeCode + "]";
	}
}
