package io.thief.toutiao;

import io.thief.toutiao.persistence.mapper.ArticleMapper;
import io.thief.toutiao.persistence.model.Article;
import io.thief.toutiao.tools.DateUtil;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@SuppressWarnings("deprecation")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:applicationContext.xml")
public class Toutiao {

	@Autowired
	private ArticleMapper articleMapper;

	@Test
	public void testSelenium() {
		System.getProperties().setProperty("webdriver.chrome.driver", "/Users/gyf/Documents/software/application/chromedriver");
		WebDriver driver = new ChromeDriver();

		Date today = DateUtil.getDayDate(new Date());
		String day = DateUtil.getDayStr(today);
		Article article = null;
		Date maxDay = articleMapper.getMaxDay();
		while (true) {
			String site = "http://toutiao.io/prev/" + day;
			driver.get(site);
			List<WebElement> elements = driver.findElements(By.className("title"));

			for (WebElement it : elements) {
				WebElement temp = null;
				try {
					temp = it.findElement(By.tagName("a"));
				} catch (NoSuchElementException e) {
					continue;
				}
				String toutiaoUrl = getUrl(temp.getAttribute("outerHTML"));
				if (day.equals(DateUtil.getDayStr(maxDay))) {
					Article art = articleMapper.findByDayAndToutiaoUrl(DateUtil.getDayDate(day), toutiaoUrl);
					if (art != null) {
						continue;
					}
				}
				article = new Article();

				article.setTitle(getContent(temp.getAttribute("outerHTML")));
				article.setToutiaoUrl(toutiaoUrl);
				article.setDay(DateUtil.getDayDate(day));
				articleMapper.insert(article);
			}
			day = DateUtil.getDayStr(DateUtil.daysAddOrSub(DateUtil.getDayDate(day), -1));
			if (DateUtil.getDayDate(day).before(DateUtil.getDayDate(maxDay))) {
				break;
			}
		}
		driver.close();
	}

	@Test
	public void testUpdateRealUrl() throws InterruptedException {
		List<Article> articles = articleMapper.findAllRealUrlNull();
		ExecutorService pool = Executors.newFixedThreadPool(100);
		for (Article it : articles) {
			final Article a = it;
			if (StringUtils.isNotBlank(it.getRealUrl())) {
				continue;
			}
			pool.execute(new Runnable() {
				@Override
				public void run() {
					innerUpdateUrl(a);
				}
			});
		}
		Thread.sleep(600000);
		pool.shutdown();
	}

	private void innerUpdateUrl(Article it) {
		CloseableHttpClient httpclient = null;
		HttpGet httpGet = null;
		try {
			HttpContext httpContext = new BasicHttpContext();
			httpclient = new DefaultHttpClient();

			httpGet = new HttpGet(it.getToutiaoUrl());
			httpGet.setConfig(RequestConfig.custom().setSocketTimeout(1000).setConnectionRequestTimeout(1000).setConnectTimeout(1000).build());

			httpclient.execute(httpGet, httpContext);

			HttpHost currentHost = (HttpHost) httpContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
			HttpUriRequest req = (HttpUriRequest) httpContext.getAttribute(ExecutionContext.HTTP_REQUEST);
			String realUrl = (req.getURI().isAbsolute()) ? req.getURI().toString() : (currentHost.toURI() + req.getURI());
			System.out.println(realUrl);
			articleMapper.updateRealUrlById(realUrl, it.getId());
		} catch (Exception e) {
			System.out.println("error, id=" + it.getId());
		}
	}

	public static class UTF8GetMethod extends GetMethod {
		public UTF8GetMethod(String url) {
			super(url);
		}

		@Override
		public String getRequestCharSet() {
			return "UTF-8";
		}
	}

	public static String getUrl(String str) {
		String ptn = "(href=[\"\']*)([^\"\']*[\"\'])";
		Pattern p = Pattern.compile(ptn);
		Matcher m = p.matcher(str);
		while (m.find()) {
			String temp = m.group(0);
			return temp.substring(6, temp.length() - 1);
		}
		return "";
	}

	public static String getContent(String str) {
		Pattern p = Pattern.compile("<a[^>]*>(.*?)</a>");
		Matcher m = p.matcher(str);
		while (m.find()) {
			return m.group(1);
		}
		return "";
	}

}
