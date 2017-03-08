package com.hanyang;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.util.Out;

public class ProcessParameter {
   public JSONObject generateParameter(JSONObject swagger, String paraStr, String fullText, List<String> urlList, Annotation anno, Document doc, String action) throws JSONException{
	//1. generate the urlMap to link the url and it's location in the text
	Map<String, Integer> urlLocation = genUrlLocation(fullText, urlList);
	//2. find the nearest url for one parameter
	String url = matchURL(paraStr, fullText, urlLocation, anno.getStartNode().getOffset());
	Out.prln(paraStr + "   match   "+ url);
	//3. write parameters in the swagger
	swagger = addParameter(swagger, paraStr, url, action, anno, doc);
	//4. write document
//	Out.prln(tableDocument);
	return swagger;
   }
   
   
   public JSONObject addParameter(JSONObject swagger, String paraStr, String url, String action, Annotation anno, Document doc) throws JSONException {
	   JSONObject urlObject = swagger.getJSONObject("paths").getJSONObject(url);
	   JSONObject paraAll = new JSONObject();
	   // parser the parameters
	   JSONArray paraArray = parseParameter(paraStr, anno, doc);
	   paraAll.put("parameters", paraArray);
	   urlObject.put(action, paraAll);
	   
	   return swagger;
   }

   public JSONArray parseParameter(String paraStr, Annotation anno, Document doc) throws JSONException{
	   JSONArray paraArray = new JSONArray();
	   Long startOffset = anno.getStartNode().getOffset();
	   Long endOffset = anno.getEndNode().getOffset();
	   AnnotationSet trSet = doc.getAnnotations("Original markups").get("tr", startOffset, endOffset);
	   
	   //Iterate the row element
	   Iterator trIter = trSet.iterator();
	   
	   while(trIter.hasNext()){
		   Annotation trElement = (Annotation) trIter.next();
		   String trStr = gate.Utils.stringFor(doc, trElement);
		   
		   String key = trStr.split(" ")[0];
		   String value = trStr.substring(trStr.indexOf(trStr.split(" ")[1]));
		   JSONObject keyObject = new JSONObject();
		   keyObject.put("name", key);
		   keyObject.put("description", value);
		   // fix 1: need to solve
		   keyObject.put("in", "query");
		   // fix 2: need to know type
		   keyObject.put("type", "integer");
		   
		   paraArray.put(keyObject);
		   
	   }

	   return paraArray;
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
