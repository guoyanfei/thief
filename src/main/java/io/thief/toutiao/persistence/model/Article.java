package io.thief.toutiao.persistence.model;

import java.util.Date;

public class Article {

	private Long id;

	private String title;

	private String toutiaoUrl;

	private String realUrl;

	private Date day;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getToutiaoUrl() {
		return toutiaoUrl;
	}

	public void setToutiaoUrl(String toutiaoUrl) {
		this.toutiaoUrl = toutiaoUrl;
	}

	public String getRealUrl() {
		return realUrl;
	}

	public void setRealUrl(String realUrl) {
		this.realUrl = realUrl;
	}

	public Date getDay() {
		return day;
	}

	public void setDay(Date day) {
		this.day = day;
	}

}
