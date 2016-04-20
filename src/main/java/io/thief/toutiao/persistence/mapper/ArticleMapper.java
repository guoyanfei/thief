package io.thief.toutiao.persistence.mapper;

import io.thief.toutiao.persistence.model.Article;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Param;

public interface ArticleMapper {

	public void insert(Article article);

	public Article find(Long id);

	public List<Article> findAll();
	
	public List<Article> findAllRealUrlNull();

	public void updateRealUrlById(@Param("realUrl") String realUrl, @Param("id") Long id);

	public Article findByDayAndToutiaoUrl(@Param("day") Date day, @Param("toutiaoUrl") String toutiaoUrl);
	
	public Date getMaxDay();
	
}
