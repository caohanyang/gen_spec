package com.hanyang;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import gate.util.Out;

public class ProcessBaseUrl {
   public JSONObject handleBaseUrl(JSONObject swagger) throws JSONException, MalformedURLException{
	   //1. first remove the unrelated url
	   List<String> urlList = pruneUrl(swagger);
	   //2. find the common url
	   String commonUrl = combineUrl(urlList);
	   //3. adjust specification
	   swagger = adjustSpec(swagger, commonUrl);
	   
	   return swagger;
   }

   public String combineUrl(List<String> urlList) {
	    //First find the common part for most of the string
	    // Step 1 most frequence
	    Map<String, Integer> allFrequency = new HashMap<String, Integer>();
	    for (int i = 0; i < urlList.size(); i++) {
	    	// for a string , find the most frequence match common string
	    	Map<String, Integer> tmpFrequency = new HashMap<String, Integer>();
	    	String tmpUrl = urlList.get(i);
	    	for (int j = 0; j < urlList.size(); j++) {
	    		String compareUrl = urlList.get(j);
	    		if (tmpUrl.equals(compareUrl)) continue;
	    		String tmpCommon = greatestCommonPrefix(tmpUrl, compareUrl);
	    		if (tmpFrequency.containsKey(tmpCommon)) {
	    			tmpFrequency.put(tmpCommon, tmpFrequency.get(tmpCommon) + 1);
	    		} else {
	    			tmpFrequency.put(tmpCommon, 1);
	    		}
	    	}
//	    	Out.prln(tmpFrequency);
	    	// get the common string frequency, put the most frequency into a map
	    	int maxNum = 0;
	    	String maxCommon = null;
	    	for (Map.Entry<String, Integer> it: tmpFrequency.entrySet()) {
	    		if (maxNum < it.getValue()) {
	    	        maxNum = it.getValue();
	    	        maxCommon = it.getKey();
	    		}
	    	}
	    	
	    	// put them in the whole map
	    	allFrequency.put(maxCommon, maxNum);
	    }
	    
	    // Step 2 most length
	    String basePath = "http";
	    for (Map.Entry<String, Integer> entry: allFrequency.entrySet()) {
	    	if (entry.getKey().length() > basePath.length()) {
	    		basePath = entry.getKey().toString();
	    	}
	    }
	    basePath = basePath.endsWith("/")? basePath.substring(0, basePath.length() - 1) : basePath.trim();
	    
	    return basePath;
	    
   }

   public String greatestCommonPrefix(String a, String b) {
	    int minLength = Math.min(a.length(), b.length());
	    for (int i = 0; i < minLength; i++) {
	        if (a.charAt(i) != b.charAt(i)) {
	            return a.substring(0, i);
	        }
	    }
	    return a.substring(0, minLength);
	}
   
   public List<String> pruneUrl(JSONObject swagger) throws JSONException {
	   List<String> urlList = new ArrayList<String>();
	   JSONObject pathObject = swagger.getJSONObject("paths");
	   Iterator pathIter = pathObject.keys();
	   while (pathIter.hasNext()) {
		   urlList.add(pathIter.next().toString());
	   }
	   
	   Out.prln(urlList);
	   for (int i = 0; i < urlList.size(); i++) {
		   // remove ?access_tocken.....
		   String str = urlList.get(i).trim();
		   if (str.contains("?")){
			   str = str.substring(0, str.indexOf("?"));
			   if (str.endsWith("/")){
				   str = str.substring(0, str.length()-1);
			   }
			   urlList.set(i, str);
		   }
		   Out.prln(str);
	   }
	   
	   Out.prln(urlList);
	   
	   return urlList;
       	
   }
   
   public JSONObject adjustSpec(JSONObject swagger, String commonUrl) throws MalformedURLException, JSONException {
       URL url = new URL(commonUrl);
       //1. add host, basePath, scheme
	   String host = url.getHost();
	   String basePath = url.getPath();
	   String scheme = url.getProtocol();
       JSONArray  scheArr  = new JSONArray();
       swagger.put("host", host);
       swagger.put("basePath", basePath);
       scheArr.put(scheme);
       swagger.put("schemes", scheArr);
       
       //2. short internal urls
       JSONObject pathObject = swagger.getJSONObject("paths");
       Map<String, Pair<String, JSONObject>> modiMap = new HashMap<String, Pair<String, JSONObject>>();
//       List<Map<String, JSONObject>> modiList = new ArrayList<Map<String, JSONObject>>();
       // construct new object
       for (int i = 0; i < pathObject.names().length(); i++) {
    	   
    	   String keyUrl = (String) pathObject.names().get(i);
    	   if (keyUrl.contains("?")){
    		   keyUrl = keyUrl.substring(0, keyUrl.indexOf("?")).trim();
			   if (keyUrl.endsWith("/")){
				   keyUrl = keyUrl.substring(0, keyUrl.length()-1);
			   }
		   }
    	   
    	   // if url contains commonUrl
    	   if (keyUrl.contains(commonUrl)) {
    		   String retainStr = keyUrl.substring(commonUrl.length());
    		   String originStr = (String) pathObject.names().get(i);
    		   JSONObject originValue = (JSONObject) pathObject.get(originStr);
    		   
    		   Pair<String, JSONObject> replacedJson = Pair.of(retainStr, originValue);
//    		   replacedJson.put(retainStr, originValue);
    		   
    		   modiMap.put(originStr, replacedJson);
//    		   modiList.add(modiMap);
    	   }
       }
       
       // remove and add new object
       for (Map.Entry<String, Pair<String, JSONObject>> entry: modiMap.entrySet()){
    	   pathObject.remove(entry.getKey());
           pathObject.put(entry.getValue().getLeft(), entry.getValue().getRight());
       }
       
       Out.prln(pathObject);
	   return swagger;
   }
   
   
}
