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
	   JSONObject actionObject = new JSONObject();
	   actionObject.put(action, new JSONObject());
	   
	   urlObject.put(url, actionObject);
	   
	   return swagger;
   }
}
