package io.thief.showhaotu.persistence.model;

public class Photo {

	private Long id;

	private String url;
	
	private Boolean isDownload;

	public Boolean getIsDownload() {
		return isDownload;
	}

	public void setIsDownload(Boolean isDownload) {
		this.isDownload = isDownload;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Photo(String url) {
		this.url = url;
		this.isDownload = false;
	}

	public Photo() {
	}
}
