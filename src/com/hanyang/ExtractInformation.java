package com.hanyang;

import java.io.File;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.tika.Tika;
import org.json.JSONException;
import org.json.JSONObject;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Corpus;
import gate.Document;
import gate.DocumentContent;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.creole.ResourceInstantiationException;
import gate.util.GateException;
import gate.util.Out;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class ExtractInformation {

	/** The Corpus Pipeline application to contain ANNIE */
	private static String FOLDER_PATH = "corpus/www.instagram.com";
	
	public static void main(String[] args) throws GateException, JSONException, IOException {		
		Gate.init();
        
        // 1. create corpus
        Corpus corpus = Factory.newCorpus("Test HTML Corpus");
        File folder = new File(FOLDER_PATH);
        File[] listFiles = folder.listFiles();
        
        // 2. initial the specification
        GenerateMain mainObject = new GenerateMain();
        JSONObject swagger = mainObject.generateStructure();
        
        // 3. create doc
        for (int i = 0; i < listFiles.length; i++) {
        	String type = new Tika().detect(listFiles[i].getPath());
        	// only detect html
        	if (type.equals("text/html")) {
        	  genOneUrl(listFiles[i].getPath(), corpus, swagger);
        	}
        }
          
	}

	public static void genOneUrl(String path, Corpus corpus, JSONObject swagger) throws ResourceInstantiationException, JSONException, IOException {
		URL u = Paths.get(path).toUri().toURL();
        FeatureMap params = Factory.newFeatureMap();
        params.put("sourceUrl", u);
        Document doc = (Document) Factory.createResource("gate.corpora.DocumentImpl", params);
        // 3. add doc
        corpus.add(doc);
        
        // 4. get all text
        DocumentContent textAll = doc.getContent();
//        Out.prln(textAll);
        // 5. initial swagger
        ProcessMethod processMe = new ProcessMethod();
    	processMe.generateDefault(swagger);
    	
        // 5.1 search for the GET https
        String strAll = textAll.toString();
        // Fix 1: suppose the len(content between get and http) < 40
        Pattern p = Pattern.compile("(?si)((get)|(post)|(del)|(put)|(patch)){1}\\s(.*?)http");
        Matcher matcher = p.matcher(strAll); 
        String actionStr=null, urlString=null;
        List<JSONObject> infoJson = new ArrayList<JSONObject>();
        
        while (matcher.find()) {
        	JSONObject sectionJson = new JSONObject();
        	int startIndex = matcher.start();
            Out.prln("start: " + matcher.start());
            Out.prln("end:   " + matcher.end());
        	// Fix 2: suppose the URL length < 100
        	String matchStr = strAll.substring(matcher.start(), matcher.end()+100);
        	Out.prln("========matchSTR==============");
        	Out.prln(matchStr);
            
        	// match reversed action        	
        	Pattern action = Pattern.compile("((teg)|(tsop)|(led)|(tup)|(hctap))", Pattern.CASE_INSENSITIVE);
        	// match the reversed string, from right to left
        	Matcher matcherAction = action.matcher(new StringBuilder(matchStr).reverse());
        	// find the first match
        	if(matcherAction.find()){
        		//find the action which is nearest to http
        		Out.prln("matchStart： "+matcherAction.start());
        		Out.prln("==========real Action============");
        		int acOffset = matchStr.length() - matcherAction.start() - 4;
        		int acLocation = startIndex + acOffset;
        		Out.prln(strAll.substring(acLocation, matcher.end()));
        		Out.prln("==========REST Action============");
        		actionStr = new StringBuilder(matcherAction.group(1)).reverse().toString();
        		JSONObject acJson = new JSONObject();
        		acJson.put(actionStr, acLocation);
        		sectionJson.put("action", acJson);
        		
        		Out.prln(actionStr);
        	}
        	// match endpoint
        	Pattern endpoint = Pattern.compile("http", Pattern.CASE_INSENSITIVE);
        	Matcher endpointMatcher = endpoint.matcher(matchStr);
        	if(endpointMatcher.find()){
        		Out.prln("urlStart： "+ endpointMatcher.start());
        		int uLocation = startIndex + endpointMatcher.start();
        		urlString = matchStr.substring(endpointMatcher.start()).split("\n")[0];
        		Out.prln("==========real ADDRESS============");
        		Out.prln(strAll.substring(uLocation, uLocation + 100));
        		Out.prln("==========URL ADDRESS============");
        		Out.prln(urlString);
        		JSONObject urJson = new JSONObject();
        		urJson.put(urlString, uLocation);
        		sectionJson.put("url", urJson);
        	}
        	
        	// Write into swagger
        	processMe.addUrl(swagger, urlString, actionStr);
        	infoJson.add(sectionJson);
        }
        
        Out.prln(swagger.toString());
        
        // 6.1 get original markups
        doc.setMarkupAware(true);
       
        AnnotationSet annoOrigin = doc.getAnnotations("Original markups");
        
        // 6.2 get table annotation
        AnnotationSet annoTable = annoOrigin.get("table");          
        
        Iterator tableIter = annoTable.iterator();
        while(tableIter.hasNext()) {
        	Annotation anno = (Annotation) tableIter.next();
        	String txt = gate.Utils.stringFor(doc, anno);
        	ProcessParameter processPa = new ProcessParameter();
        	if (processPa.isParaTable(txt)) {
        		Out.prln("==========TABLE =================");
        		Out.prln(txt);
        		processPa.generateParameter(swagger, txt, strAll, infoJson, anno, doc);
        	}
        }
        
        // Print pretty swagger
        FileWriter fileWriter = new FileWriter("swagger.json");
        String swString = swagger.toString();
        JsonParser parser = new JsonParser();
        JsonElement jelement = parser.parse(swString);
        
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String prettyJson = gson.toJson(jelement);
        
		fileWriter.write(prettyJson);
		fileWriter.close();
		
	}
	

}
