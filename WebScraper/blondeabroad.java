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

public class blondeabroad {
	private static String urlBase = "http://theblondeabroad.com/blog/";
	private static int totalPages = 45;
	
	private static void parseHtml(String blogUrl, String outputFile, BufferedWriter mapbw) {
		try {
			HttpClient client = HttpClientBuilder.create().build();
			HttpGet httpGet = new HttpGet(blogUrl);
	        HttpResponse response = client.execute(httpGet);
	        HttpEntity entity = response.getEntity();
	        String responseString = EntityUtils.toString(entity, "UTF-8");
	        Document doc = Jsoup.parse(responseString);
	        Elements list = doc.select("title");
	        Elements tags = doc.select("meta[property=\"article:tag\"]");
	        Element titleElement = list.size() > 0 ? list.first() : null;
	        String title = list.size() > 0 ? titleElement.ownText():"Unknown";
	        List<String> tagStr = new ArrayList<>();
	        for(Element tag : tags) {
	        	tagStr.add(tag.attr("content"));
	        }
        	mapbw.write("{url:\"" + blogUrl + "\",title: \"" + title + "\",file:\"" + outputFile + "\",continent: \"" + "" + "\","
	        		+ "country:\"" + "" + "\", state:\"" + "" + "\",area:\"" + "" + "\"" + ",\"tags:\"" + String.join(",", tagStr) + "\"},\n");
        	
	        BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
	        Elements plist = doc.select("div[class=post-content]");
	       
	        for(Element e:plist) {
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
		String blogBaseDir = "/tmp/travelblog/blondeabroad/";
		String metaBaseDir = "/tmp/travelblog-meta/blondeabroad/";
		File dir1 = new File(blogBaseDir);
        if (!dir1.mkdir()) {
            System.err.println("Error in create dir: " + dir1 );
            System.exit(1);
        }
		File dir2 = new File(metaBaseDir);
		if (!dir2.mkdir()) {
        	System.err.println("Error in create dir: " + dir2 );
        	System.exit(1);
    	}
		
		try {
			for (int i = 1; i <= totalPages; i ++ ) {
				String url = urlBase;
				if ( i > 1)
					url += "page/" + i + "/";
				HttpClient client = HttpClientBuilder.create().build();
				HttpGet httpGet = new HttpGet(url);
		        HttpResponse response = client.execute(httpGet);
		        HttpEntity entity = response.getEntity();
		        String responseString = EntityUtils.toString(entity, "UTF-8");
				Document doc = Jsoup.parse(responseString);
				Elements links = doc.select("a[class=fusion-read-more]"); // a with title
				links.forEach(e -> {
					String href = e.attr("href");
					urlList.add(href);
				});
				EntityUtils.consume(entity);
			}
			
			File file = new File(blogBaseDir + "blondeabroad-meta.json");
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			int cnt = 0;
			for(String blogUrl : urlList) {
				cnt ++ ;
				String blogOutput = blogBaseDir + "blog_" + cnt + ".txt";
				parseHtml(blogUrl, blogOutput, bw);
			};
			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}