package WebScraper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class nomadicmatt {
	private static String urlBase = "http://www.nomadicmatt.com/travel-blog/";
	private static int totalPages = 138;
	
	private static void parseHtml(String blogUrl, String outputFile, BufferedWriter mapbw) {
		try {
			HttpClient client = HttpClientBuilder.create().build();
			HttpGet httpGet = new HttpGet(blogUrl);
	        HttpResponse response = client.execute(httpGet);
	        HttpEntity entity = response.getEntity();
	        String responseString = EntityUtils.toString(entity, "UTF-8");
	        Document doc = Jsoup.parse(responseString);
	        Elements list = doc.select("title");
	        Element titleElement = list.size() > 0 ? list.first() : null;
	        String title = list.size() > 0 ? titleElement.ownText():"Unknown";
	        mapbw.write(blogUrl + "^" + title + "^" + outputFile +"\n");
	        
	        Elements mainlist = doc.select("main[class=content]");
	        
	        BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
	        for(Element e:mainlist) {
	        	String outputString = e.text();
	        	bw.write(outputString + "\n");
	        }
	        
	        bw.close();
	        EntityUtils.consume(entity);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		List<String> urlList = new ArrayList<>();
		
		try {
			for (int i = 1; i <= totalPages; i ++ ) {
				String url = urlBase;
				if ( i > 1)
					url += "page/" + i + "/";
				//System.out.println(url);
				HttpClient client = HttpClientBuilder.create().build();
				HttpGet httpGet = new HttpGet(url);
		        HttpResponse response = client.execute(httpGet);
		        HttpEntity entity = response.getEntity();
		        String responseString = EntityUtils.toString(entity, "UTF-8");
				Document doc = Jsoup.parse(responseString);
				Elements links = doc.select("a[class=more-link]"); // a with title
				links.forEach(e -> {
					String href = e.attr("href");
					urlList.add(href);
					//System.out.println(href);
				});
				EntityUtils.consume(entity);
			}
			
			File file = new File("/home/ling/travelblog/nomadicmatt/mapping.txt");
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			int cnt = 0;
			for(String blogUrl : urlList) {
				cnt ++ ;
				String blogOutput = "/home/ling/travelblog/nomadicmatt/blog_" + cnt + ".txt";
				parseHtml(blogUrl, blogOutput, bw);
			};
			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
