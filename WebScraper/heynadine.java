package WebScraper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
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

public class heynadine {

	private static HashMap<String, Integer> urlMap = new HashMap<>();
	
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
	        Elements tags = doc.select("article[id]");
	        List<String> tagStr = new ArrayList<>();
	        if (tags.size() > 0) {
	        	String tagLine = tags.first().attr("class");
	        	String[] tagarr = tagLine.split(" ");
	        	for(int i = 0; i < tagarr.length; i++) {
	        		int idx = tagarr[i].indexOf("tag");
	        		if (idx == 0)
	        			tagStr.add(tagarr[i].substring(4, tagarr[i].length()));
	        	}
	        }
	        mapbw.write(blogUrl + "^" + title + "^" + outputFile + "^" + String.join(",", tagStr)+"\n");
	        
	        BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
	        Elements plist = doc.select("div[class=post-entry]");
	       
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
		//urlMap.put("http://www.heynadine.com/category/travel/oceania/", 4);
		//urlMap.put("http://www.heynadine.com/category/travel/africa/", 1);
		//urlMap.put("http://www.heynadine.com/category/travel/asia-travel/", 4);
		//urlMap.put("http://www.heynadine.com/category/travel/asia-travel/southeast-asia-travel/", 3);
		//urlMap.put("http://www.heynadine.com/category/travel/europe-travel-2/western-europe/", 1);
		//urlMap.put("http://www.heynadine.com/category/travel/europe-travel-2/eastern-europe/", 2);
		//urlMap.put("http://www.heynadine.com/category/travel/europe-travel-2/mediterranean/", 1);
		//urlMap.put("http://www.heynadine.com/category/travel/europe-travel-2/scandinavia/", 1);
		//urlMap.put("http://www.heynadine.com/category/travel/europe-travel-2/united-kingdom/", 1);
		//urlMap.put("http://www.heynadine.com/category/travel/americas/north-america/", 3);
		//urlMap.put("http://www.heynadine.com/category/travel/americas/central-america/", 1);
		//urlMap.put("http://www.heynadine.com/category/travel/americas/south-america-travel/", 2);
		urlMap.put("http://www.heynadine.com/category/travel/caribbean/", 1);

		List<String> urlList = new ArrayList<>();

		try {
			for (String urlBase: urlMap.keySet()) {
				int totalPages = (int)urlMap.get(urlBase);
			
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
					Elements links = doc.select("div[class=post-header] > h2"); // a with title
					links.forEach(e -> {
						String href = e.child(0).attr("href");
						urlList.add(href);
						//System.out.println(href);
					});
					EntityUtils.consume(entity);
				}
			}
			
			File file = new File("/home/ling/travelblog/heynadine/mapping.txt");
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			int cnt = 0;
			for(String blogUrl : urlList) {
				cnt ++ ;
				String blogOutput = "/home/ling/travelblog/heynadine/blog_" + cnt + ".txt";
				parseHtml(blogUrl, blogOutput, bw);
			};
			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}