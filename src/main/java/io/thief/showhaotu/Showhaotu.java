package io.thief.showhaotu;

import io.thief.showhaotu.persistence.mapper.PhotoMapper;
import io.thief.showhaotu.persistence.model.Photo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.alibaba.fastjson.JSONObject;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:applicationContext.xml")
public class Showhaotu {

	@Autowired
	private PhotoMapper photoMapper;

	ExecutorService httpPool = Executors.newFixedThreadPool(100);
	ExecutorService downloadPool = Executors.newFixedThreadPool(5);

	@Test
	public void getUrl() throws InterruptedException {
		int i = 1;
		while (true) {
			final int j = i;
			httpPool.execute(new Runnable() {
				@Override
				public void run() {
					httpPost("http://showhaotu.com/json", j);
				}
			});
			Thread.sleep(1000);
			i++;
			System.out.println("循环次数：" + j);
		}
	}

	private void httpPost(String url, int page) {
		try {
			CloseableHttpClient client = HttpClientBuilder.create().build();
			HttpPost post = new HttpPost(url);
			post.setHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			post.setHeader("Accept-Encoding", "gzip, deflate");
			post.setHeader("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.6,en;q=0.4,zh-TW;q=0.2");
			post.setHeader("Connection", "keep-alive");
			post.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
			// post.setHeader("Content-Length", "97");
			post.setHeader("Cookie", "__cfduid=d6cafc5310f302cfa22ec3e179a37c20e1460980249; PHPSESSID=m3kmiml15q72u1r88uh3m4gar5; _gat=1; _ga=GA1.2.189059411.1460980252");
			post.setHeader("Host", "showhaotu.com");
			post.setHeader("Origin", "http://showhaotu.com");
			post.setHeader("Referer", "");
			post.setHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.75 Safari/537.36");
			post.setHeader("X-Requested-With", "XMLHttpRequest");
			// post.removeHeaders("Content-Length");
			post.setConfig(RequestConfig.custom().setConnectTimeout(20000).setConnectionRequestTimeout(20000).setSocketTimeout(20000).build());

			List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			nvps.add(new BasicNameValuePair("list", "images"));
			nvps.add(new BasicNameValuePair("sort", "date_desc"));
			nvps.add(new BasicNameValuePair("action", "list"));
			nvps.add(new BasicNameValuePair("auth_token", "1aa78554788042231dda0e29e02291cf50b80dc9"));
			nvps.add(new BasicNameValuePair("page", "" + page));

			post.setEntity(new UrlEncodedFormEntity(nvps));

			CloseableHttpResponse response = client.execute(post);
			String body = EntityUtils.toString(response.getEntity(), "utf-8");
			JSONObject json = JSONObject.parseObject(body);
			String html = (String) json.get("html");
			// 获取图片标签
			List<String> imgUrl = getImageUrl(html);
			// 获取图片src地址
			final List<String> imgSrc = getImageSrc(imgUrl);
			System.out.println(imgSrc);

			for (String it : imgSrc) {
				try {
					photoMapper.insert(new Photo(it));
				} catch (Exception e) {
					System.out.println("--插失败--it=" + it);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 获取img标签正则
	private static final String IMGURL_REG = "<img.*src=(.*?)[^>]*?>";
	// 获取src路径的正则
	private static final String IMGSRC_REG = "http:\"?(.*?)(\"|>|\\s+)";

	/***
	 * 获取ImageUrl地址
	 * 
	 * @param HTML
	 * @return
	 */
	private List<String> getImageUrl(String HTML) {
		Matcher matcher = Pattern.compile(IMGURL_REG).matcher(HTML);
		List<String> listImgUrl = new ArrayList<String>();
		while (matcher.find()) {
			listImgUrl.add(matcher.group());
		}
		return listImgUrl;
	}

	/***
	 * 获取ImageSrc地址
	 * 
	 * @param listImageUrl
	 * @return
	 */
	private List<String> getImageSrc(List<String> listImageUrl) {
		List<String> listImgSrc = new ArrayList<String>();
		for (String image : listImageUrl) {
			Matcher matcher = Pattern.compile(IMGSRC_REG).matcher(image);
			while (matcher.find()) {
				listImgSrc.add(matcher.group().substring(0, matcher.group().length() - 1));
			}
		}
		return listImgSrc;
	}

	@Test
	public void download() throws InterruptedException {
		List<Photo> photos = photoMapper.findAllUnDownload();
		for (final Photo it : photos) {
			downloadPool.execute(new Runnable() {
				@Override
				public void run() {
					try {
						Download(it.getUrl());
						photoMapper.consume(it.getId());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		}
		Thread.sleep(600000);
	}

	/***
	 * 下载图片
	 * 
	 * @param listImgSrc
	 * @throws Exception 
	 */
	private void Download(String url) throws Exception {
		try {
			String imageName = url.substring(url.lastIndexOf("/") + 1, url.length());
			URL uri = new URL(url);
			InputStream in = uri.openStream();
			FileOutputStream fo = new FileOutputStream(new File("/Users/gyf/Downloads/thief/" + imageName));
			byte[] buf = new byte[1024];
			int length = 0;
			System.out.println("开始下载:" + url);
			while ((length = in.read(buf, 0, buf.length)) != -1) {
				fo.write(buf, 0, length);
			}
			in.close();
			fo.close();
			System.out.println(imageName + "下载完成");
		} catch (Exception e) {
			System.out.println("下载失败");
			throw e;
		}
	}

}
