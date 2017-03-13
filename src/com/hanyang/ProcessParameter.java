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
   public JSONObject generateParameter(JSONObject swagger, String paraStr, String fullText, List<JSONObject> infoJson, Annotation anno, Document doc) throws JSONException{
//	//1. generate the urlMap to link the url and it's location in the text
//	Map<String, Integer> urlLocation = genUrlLocation(fullText, infoJson);
//	//2. find the nearest url for one parameter
//	String url = matchURL(paraStr, fullText, urlLocation, anno.getStartNode().getOffset());
//	String action = matchAction(paraStr, fullText, actionLocation, anno.getStartNode().getOffset());
//	Out.prln(paraStr + "   match   "+ url + "action: " + action);
	//3. write parameters in the swagger
	swagger = addParameter(swagger, paraStr, infoJson, anno, doc, fullText);
	//4. write document
//	Out.prln(tableDocument);
	return swagger;
   }
   
   
   public JSONObject addParameter(JSONObject swagger, String paraStr, List<JSONObject> infoJson, Annotation anno, Document doc, String fullText) throws JSONException {
	   JSONObject sectionJson = matchURL(paraStr, fullText, infoJson, anno.getStartNode().getOffset());
	   String url = sectionJson.getString("url");
	   String action = sectionJson.getString("action");
	   
	   JSONObject urlObject = swagger.getJSONObject("paths").getJSONObject(url);
	   //1. find the action
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
   
   public JSONObject matchURL(String paraStr, String fullText, List<JSONObject> infoJson, Long paraLocation) throws JSONException {
		JSONObject sectionObject = new JSONObject();
	    int minimumDistance = Integer.MAX_VALUE;
		for (JSONObject it: infoJson){
			JSONObject entry = it.getJSONObject("url");
			Iterator keys = entry.keys();
			if (keys.hasNext()) {
				String key = (String) keys.next();
				// search the nearest url in the prevois context 
				if ((paraLocation - entry.getInt(key) < minimumDistance) && (paraLocation - entry.getInt(key) > 0) ) {
					minimumDistance = (int) (paraLocation - entry.getInt(key));
					sectionObject.put("action", it.getJSONObject("action").keys().next());
					sectionObject.put("url", key);
				}
			}
		}
		return sectionObject;
	}

	public boolean isParaTable(String txt) {
		if (Pattern.compile(Pattern.quote("parameter"), Pattern.CASE_INSENSITIVE).matcher(txt).find()) {
			return true;
		}
		return false;
	}
	
}
