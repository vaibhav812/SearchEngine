package engine.simple.Milestone1;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Class for breaking a big corpus into small .json files. The output path is fixed and will be dynamic in coming versions.
 * The form of json that this file parses is:
 * 		{
 * 			"documents": [
 *	 			{"body": "..."}
 * 				{"body": "..."}
 * 				.
 * 				.
 * 				.
 * 				{"body": "..."}
 * 			]
 * 		}
 * @author vaibhavjain
 * @version 1.0
 */
public class FormCorpus {
	
	static int writeIndex = 1;
	
	@SuppressWarnings("unchecked")
	public static void initCorpus(File jsonFilePath) throws Exception{
		if(jsonFilePath.exists()) {
			JSONParser parser = new JSONParser();
			JSONObject jObject =(JSONObject) parser.parse(new FileReader(jsonFilePath));
			JSONArray jArray =(JSONArray) jObject.get("documents");
			String writePath = "D:\\CSULB\\Sem 3 - Fall 2017\\Search Engine Technology\\Milestone_1_Corpus_Expanded\\";
			jArray.forEach(jsonObject -> {
				File f = new File(writePath + writeIndex + ".json");
				writeIndex++;
				FileWriter fWriter;
				try {
				fWriter = new FileWriter(f);
				JSONObject obj = (JSONObject) jsonObject;
				fWriter.write(obj.toJSONString());
				fWriter.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		}
	}
}