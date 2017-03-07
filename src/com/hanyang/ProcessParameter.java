package com.hanyang;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import gate.util.Out;

public class ProcessParameter {
   public JSONObject generateParameter(JSONObject swagger, String paraStr, String fullText, List<String> urlList, Long paraLocation, String action) throws JSONException{
	//1. generate the urlMap to link the url and it's location in the text
	Map<String, Integer> urlLocation = genUrlLocation(fullText, urlList);
	//2. find the nearest url for one parameter
	String url = matchURL(paraStr, fullText, urlLocation, paraLocation);
	Out.prln(paraStr + "   match   "+ url);
	//3. write parameters in the swagger
	swagger = addParameter(swagger, paraStr, url, action);
	return swagger;
   }
   
   
   public JSONObject addParameter(JSONObject swagger, String paraStr, String url, String action) throws JSONException {
	   JSONObject urlObject = swagger.getJSONObject("paths").getJSONObject(url);
	   JSONObject paraObject = new JSONObject();
	   paraObject.put("parameters", paraStr);
	   urlObject.put(action, paraObject);
	   return swagger;
   }


   public Map<String, Integer> genUrlLocation (String fullText, List<String> urlList) {
	   Map<String, Integer> urlLocation = new HashMap<String, Integer>();
	   for (String element: urlList) {
		   int location = fullText.indexOf(element);
           urlLocation.put(element, location);		   
	   }
	   return urlLocation;
	   
   }
   
   public String matchURL(String paraStr, String fullText, Map<String, Integer> urlLocation, Long paraLocation) {
		int minimumDistance = Integer.MAX_VALUE;
		String matchUrl = null;
		for (Entry<String, Integer> entry: urlLocation.entrySet()){
			// search the nearest url in the prevois context 
			if ((paraLocation - entry.getValue() < minimumDistance) && (paraLocation - entry.getValue() > 0) ) {
				minimumDistance = (int) (paraLocation - entry.getValue());
				matchUrl = entry.getKey();
			}
		}
		return matchUrl;
	}


	public boolean isParaTable(String txt) {
		if (Pattern.compile(Pattern.quote("parameter"), Pattern.CASE_INSENSITIVE).matcher(txt).find()) {
			return true;
		}
		return false;
	}
}
