package com.hanyang;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
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

	public void addNoParaUrl(JSONObject swagger, String strAll, List<JSONObject> infoJson) throws JSONException {
		// choose the most proper url/action pair
		Pair<String, String> properPair = solveConflicts(strAll, infoJson);
		// handle it badly, need to fix:
		String action = properPair.getKey();
		String url = properPair.getValue();
		addUrl(swagger, url, action);
	}

	public Pair<String, String> solveConflicts(String strAll, List<JSONObject> infoJson) throws JSONException {
       // if the are no parameter table in the page
	   // if one URL have two actions, solve the conflicts
		Pair<String, String> properPair = null;
		int miniMum = Integer.MAX_VALUE;
		  for (int i = 0; i < infoJson.size(); i ++) {
			  JSONObject acObject = infoJson.get(i).getJSONObject("action");
			  JSONObject urObject = infoJson.get(i).getJSONObject("url");
			  String actionFinal = acObject.keys().next().toString();
			  String urlFinal = urObject.keys().next().toString();
			  
			  int acLocation = acObject.getInt(actionFinal);
			  int urlLocation = urObject.getInt(urlFinal);		  
			  
			  if (Math.abs(acLocation - urlLocation) < miniMum) {
				  miniMum = Math.abs(acLocation - urlLocation);
				  properPair = Pair.of(actionFinal, urlFinal);
			  }
		  }
		  
		  return properPair;
	}

   
}
