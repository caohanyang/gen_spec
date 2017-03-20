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
   public JSONObject generateParameter(JSONObject swagger, String paraStr, String fullText, List<JSONObject> infoJson, Annotation anno, Document doc, ProcessMethod processMe, String page_pattern) throws JSONException{
	   
	   JSONObject sectionJson = matchURL(paraStr, fullText, infoJson, anno.getStartNode().getOffset(), page_pattern);
	   String url = sectionJson.getString("url");
	   String action = sectionJson.getString("action");
	   
	   //1. we add url/action into swagger now.
	   // because here we have known that each table have match one url
	   // some urls would not be used.
	   processMe.addUrl(swagger, url, action);
	   
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
   
   public JSONObject matchURL(String paraStr, String fullText, List<JSONObject> infoJson, Long paraLocation, String page_pattern) throws JSONException {
		JSONObject sectionObject = new JSONObject();
	    int minimumDistance = Integer.MAX_VALUE;
	    Out.prln("----------para location-------");
	    Out.prln(paraLocation);
		for (JSONObject it: infoJson){
			JSONObject entry = it.getJSONObject("url");
			Iterator keys = entry.keys();
			if (keys.hasNext()) {
				String key = (String) keys.next();
				//Rule 1: url that not far from "example..." ???
				
				//Rule 2: search the nearest url 
				if (page_pattern.matches("single")) {
					// if it's a single page, search from all the text
					if (Math.abs(paraLocation - entry.getInt(key)) < minimumDistance ) {
						minimumDistance = (int) Math.abs((paraLocation - entry.getInt(key)));
						sectionObject.put("action", it.getJSONObject("action").keys().next());
						sectionObject.put("url", key);
					}
				} else {
					// multiple table mode: search only in the previous context 
					if ((paraLocation - entry.getInt(key) < minimumDistance) && (paraLocation - entry.getInt(key) > 0) ) {
						minimumDistance = (int) (paraLocation - entry.getInt(key));
						sectionObject.put("action", it.getJSONObject("action").keys().next());
						sectionObject.put("url", key);
					}
				}
				
				
			}
		}
		return sectionObject;
	}

	public boolean isParaTable(String txt, Annotation anno, String strAll) {
		// not only check "parameter" in the table text
		// but also need to check text just before the table
		// maybe in the table maybe doesn't contain str "parameter"
		
		// find previous text
		int tableLocation = anno.getStartNode().getOffset().intValue();
		String appendTableText = strAll.substring(tableLocation - 20,  anno.getEndNode().getOffset().intValue());
		if (Pattern.compile("parameter", Pattern.CASE_INSENSITIVE).matcher(appendTableText).find()) {
			return true;
		}
		return false;
	}
	
}
