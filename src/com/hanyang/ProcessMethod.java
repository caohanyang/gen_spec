package com.hanyang;

import org.json.JSONException;
import org.json.JSONObject;

import gate.util.Out;

public class ProcessMethod {
   public JSONObject generateDefault (JSONObject swagger) throws JSONException{
	
	   // Add path in the top level
	   if (swagger.isNull("paths") == true) {
		   swagger.put("paths", new JSONObject());
	   }
	   return swagger;  
   }
   
   public JSONObject addUrl (JSONObject swagger, String url, String action) throws JSONException {
	   JSONObject urlObject =  swagger.getJSONObject("paths");
	   Out.prln("---------match--action-------------");
	   Out.prln(action);
	   JSONObject actionObject = new JSONObject();
	   actionObject.put(action, new JSONObject());
	   
	   if (urlObject.isNull(url)) {
           // if url object is null, add directly for the first time		   
		   urlObject.put(url, actionObject);
	   } else {
		   // otherwise add the new action to the url
		   JSONObject urlInterObject = urlObject.getJSONObject(url);
		   urlInterObject.put(action, new JSONObject());
	   }
	   
	   return swagger;
   }
   
}
