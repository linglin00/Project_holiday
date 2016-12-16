package WebScraper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class ProcessJson {
	private static String[] findSubdir(String currentDir) {
		File fileDir = new File(currentDir);
		String[] directories = fileDir.list(new FilenameFilter() {
			@Override
			public boolean accept(File current, String name) {
				return new File(current, name).isDirectory();
			}
		});
		
		return directories;
	}
	
	private static String[] findJsonFile(String currentDir) {
		File fileDir = new File(currentDir);
		String[] jsonFiles = fileDir.list(new FilenameFilter() {
			@Override
			public boolean accept(File current, String name) {
				return new File(current, name).getName().contains(".json");
			}
		});
		return jsonFiles;
	}
	
	public static void main(String[] argv) throws Exception {
		BufferedWriter bw = new BufferedWriter(new FileWriter("elasticsearchdata.sh"));
		
		String dirName = "/home/ling/travelblog";
		List<String> level_1 = Arrays.asList("North-America"); //findSubdir(dirName);
		for( String level_1_dir : level_1 ){
			String current_level_1_dir = dirName + "/" + level_1_dir;
			String[] level_2 = findSubdir(current_level_1_dir);
			for( String level_2_dir : level_2) {
				String current_level_2_dir = current_level_1_dir + "/" + level_2_dir;
				String[] level_3 = findSubdir(current_level_2_dir);
				for( String level_3_dir: level_3 ) {
					String current_level_3_dir = current_level_2_dir + "/" + level_3_dir;
					String[] jsonFiles = findJsonFile(current_level_3_dir);
					
					for( String jsonFile : jsonFiles ) {
						String inputFile = current_level_3_dir + "/" + jsonFile;
						try {
							BufferedReader br = new BufferedReader(new FileReader(inputFile));
							String line = "";
							String jsonLine = "";
							while((line = br.readLine()) != null) {
								jsonLine += line;
							};
							br.close();
							System.out.println(inputFile);
							JSONObject obj = new JSONObject(jsonLine);
							JSONArray obJsonArray = obj.getJSONArray("data");
							for (int i = 0; i < obJsonArray.length(); i++)
							{						
								JSONObject obJson = obJsonArray.getJSONObject(i);
								String url = obJson.getString("url");
								String title = obJson.getString("title").replaceAll(" | Travel Blog", "").replaceAll("|TravelBlog", "");
								String state = level_3_dir;
								String continent = level_1_dir;
								String country = level_2_dir;
								String area = obJson.getString("area");
								int bidx = area.lastIndexOf("/");
								if(bidx >= 0 && bidx < area.length() - 1)
									area = area.substring(bidx+1, area.length());
								String jsonString = "{\"title\":\"" + title + "\",\"url\":\"" + url + "\",\"continent\":\"" + continent + "\",\"country\":\"" + country + "\",\"state\":\"" + state + "\",\"area\":\"" + area + "\"}";
								bw.write("curl -XPOST 'localhost:9200/traveldata/docs/?pretty' -d'" + jsonString + "'\n");
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		bw.close();
	}

}
