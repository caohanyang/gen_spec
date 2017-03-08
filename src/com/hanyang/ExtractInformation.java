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
import java.util.Iterator;
import java.util.List;
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
import gate.CorpusController;
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
        
        // 5. get all text
        DocumentContent textAll = doc.getContent();
//        Out.prln(textAll);
        
        // 5.1 search for the GET https
        String strAll = textAll.toString();
        // Fix 1: suppose the len(content between get and http) < 40
        Pattern p = Pattern.compile("(?si)((get)|(post)|(del)|(put)|(patch)){1}\\s(.{0,40})http");
        Matcher matcher = p.matcher(strAll);
        List<String> urlList = new ArrayList<String>();
        String actionStr=null, urlString=null;
        while (matcher.find()) {
        	// Fix 2: suppose the URL length < 100
        	String matchStr = strAll.substring(matcher.start(), matcher.end()+100);
//        	Out.prln("========tmpSTR==============");
//        	Out.prln(matchStr);
            // match action        	
        	Pattern action = Pattern.compile("((get)|(post)|(del)|(put)|(patch))", Pattern.CASE_INSENSITIVE);
        	Matcher matcherAction = action.matcher(matchStr);
        	
        	if(matcherAction.find()){
        		actionStr = matchStr.split(" ")[0];
//        		Out.prln("==========REST Action============");
//        		Out.prln(actionStr);
        	}
        	// match endpoint
        	Pattern endpoint = Pattern.compile("http", Pattern.CASE_INSENSITIVE);
        	Matcher endpointMatcher = endpoint.matcher(matchStr);
        	if(endpointMatcher.find()){
        		urlString = matchStr.substring(endpointMatcher.start()).split("\n")[0];
        		urlList.add(urlString);
//        		Out.prln("==========URL ADDRESS============");
//        		Out.prln(urlString);
        	}
        	
        	// Write into swagger
        	ProcessMethod processMe = new ProcessMethod();
        	processMe.generateDefault(swagger);
        	processMe.addUrl(swagger, urlString, actionStr);
        }
        
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
        		processPa.generateParameter(swagger, txt, strAll, urlList, anno, doc, actionStr);
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
