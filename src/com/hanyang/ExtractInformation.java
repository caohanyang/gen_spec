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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
	// corpus/www.instagram.com dev.twitter.com www.twilio.com www.youtube.com
	// www.flickr.com
	private static String FOLDER_PATH = "corpus/www.flickr.com";
	private static String API_NAME = "flickr";
	// "https", "http"
	private static List<String> SCHEME = new ArrayList<String>(Arrays.asList("null"));
	// "table", 
	private static List<String> TEMPLATE = new ArrayList<String>(Arrays.asList("list"));
	// "single", 
	private static List<String> NUMBER = new ArrayList<String>(Arrays.asList("multiple"));
	// , "delete"
	private static List<String> ABBREV_DELETE = new ArrayList<String>(Arrays.asList("del"));

	public static void main(String[] args) throws GateException, JSONException, IOException {
		Gate.init();

		// 1. create corpus
		Corpus corpus = Factory.newCorpus("Test HTML Corpus");
		File folder = new File(FOLDER_PATH);
		File[] listFiles = folder.listFiles();

		// 2. generate swagger according to pattern
		for (Iterator<String> sIterator = SCHEME.iterator(); sIterator.hasNext();) {
			String scheme = sIterator.next();

			for (Iterator<String> tIterator = TEMPLATE.iterator(); tIterator.hasNext();) {
				String template = tIterator.next();

				for (Iterator<String> nIterator = NUMBER.iterator(); nIterator.hasNext();) {
					String number = nIterator.next();

					for (Iterator<String> dIterator = ABBREV_DELETE.iterator(); dIterator.hasNext();) {
						String abbrev = dIterator.next();
						// generate different swagger
						generateSwagger(corpus, listFiles, scheme, template, number, abbrev);
						System.gc();
					}
				}
			}
		}

		// 3. compare the json files and select the final one.
		selectSwagger(folder);

	}

	public static void generateSwagger(Corpus corpus, File[] listFiles, String scheme, String template, String number,
			String abbrev) throws ResourceInstantiationException, JSONException, IOException, MalformedURLException {
		// 2. initial the specification
		GenerateMain mainObject = new GenerateMain();
		JSONObject swagger = mainObject.generateStructure();

		// 3. different mode
		if (scheme == "null") {
		   searchBaseUrl(listFiles, corpus, swagger);
		} else {
			for (int i = 0; i < listFiles.length; i++) {
				// print the file name
				Out.prln("=============File name=======================");
				Out.prln(listFiles[i].getPath());
				String type = new Tika().detect(listFiles[i].getPath());
				// only detect html
				if (type.equals("text/html")) {
					executeOneFile(listFiles[i].getPath(), corpus, swagger, scheme, template, number, abbrev);
				}
			}
		}

		// 4. prune swagger
		ProcessBaseUrl processBa = new ProcessBaseUrl();
		swagger = processBa.handleBaseUrl(swagger);

		// 5. write to file
		writeSwagger(swagger, scheme, template, number, abbrev);

	}

	public static void searchBaseUrl(File[] listFiles, Corpus corpus, JSONObject swagger) throws MalformedURLException, ResourceInstantiationException, JSONException {
		// define the list of baseUrl
		List<String> baseUrlList = new ArrayList<String>();
		for (int i = 0; i < listFiles.length; i++) {
			// print the file name
			Out.prln("=============File name=======================");
			Out.prln(listFiles[i].getPath());
			String type = new Tika().detect(listFiles[i].getPath());
			// only detect html
			if (type.equals("text/html")) {
				URL u = Paths.get(listFiles[i].getPath()).toUri().toURL();
				FeatureMap params = Factory.newFeatureMap();
				params.put("sourceUrl", u);
				Document doc = (Document) Factory.createResource("gate.corpora.DocumentImpl", params);
				// 1. add doc
				corpus.add(doc);
				// 2. get all text
				DocumentContent textAll = doc.getContent();
				// 3. initial swagger
				ProcessMethod processMe = new ProcessMethod();
				processMe.generateDefault(swagger);
                
				// 4.1 search for the GET https
				String strAll = textAll.toString();
				// Fix 1: suppose the len(content between get and http) < 40 + "://"
				String regexRest = "(?si)rest.+request";
				String regexHttp = "(?si)((http)|(https)){1}://";
				Pattern pRest = Pattern.compile(regexRest);
				Matcher matcherREST = pRest.matcher(strAll);
				while (matcherREST.find()) {
					// first find the page which contains REST + request
					Pattern pHttp = Pattern.compile(regexHttp);
					Matcher matcherHttp = pHttp.matcher(strAll);
					while (matcherHttp.find()) {
						// Fix 2: suppose the URL length < 40
						String matchStrNull = strAll.substring(matcherHttp.start()).split("\n")[0].trim();
						// final API endpoint must contain API_NAME
						matchStrNull = processMe.constrainUrl(matchStrNull, API_NAME);
						if (matchStrNull != null) {
							Out.prln(matchStrNull);
							baseUrlList.add(matchStrNull);
						}
					}
				}
				
			}
		}
		
		Out.prln(baseUrlList);
		
        //find the base url
//		ProcessBaseUrl processbase = new ProcessBaseUrl();
//		String base = processbase.combineUrl(baseUrlList);
		Map<Object, Long> counts =
				baseUrlList.stream().collect(Collectors.groupingBy(e -> e, Collectors.counting()));
		Out.prln(counts);
	}

	public static void executeOneFile(String path, Corpus corpus, JSONObject swagger, String scheme, String template,
			String number, String abbrev) throws ResourceInstantiationException, JSONException, IOException {
		URL u = Paths.get(path).toUri().toURL();
		FeatureMap params = Factory.newFeatureMap();
		params.put("sourceUrl", u);
		Document doc = (Document) Factory.createResource("gate.corpora.DocumentImpl", params);
		// 1. add doc
		corpus.add(doc);

		// 2. get all text
		DocumentContent textAll = doc.getContent();
		// Out.prln(textAll);
		// 3. initial swagger
		ProcessMethod processMe = new ProcessMethod();
		processMe.generateDefault(swagger);

		// 4.1 search for the GET https
		String strAll = textAll.toString();
		// Fix 1: suppose the len(content between get and http) < 40 + "://"
		String regexAll = "(?si)((get)|(post)|(" + abbrev + ")|(put)|(patch)){1}\\s(.*?)" + scheme;
		Pattern p = Pattern.compile(regexAll);
		Matcher matcher = p.matcher(strAll);
		String actionStr = null, urlString = null;
		List<JSONObject> infoJson = new ArrayList<JSONObject>();

		while (matcher.find()) {
			JSONObject sectionJson = new JSONObject();
			int startIndex = matcher.start();
			Out.prln("start: " + matcher.start());
			Out.prln("end:   " + matcher.end());
			// Fix 2: suppose the URL length < 100
			String matchStr = strAll.substring(matcher.start(), matcher.end() + 100);
			Out.prln("========matchSTR==============");
			Out.prln(matchStr);

			// match reversed action
			Pattern action = Pattern.compile(
					"((teg)|(tsop)|(" + new StringBuilder(abbrev).reverse().toString() + ")|(tup)|(hctap))",
					Pattern.CASE_INSENSITIVE);
			// match the reversed string, from right to left
			Matcher matcherAction = action.matcher(new StringBuilder(matchStr).reverse());
			// find the first match
			if (matcherAction.find()) {
				// find the action which is nearest to http
				Out.prln("matchStart： " + matcherAction.start());
				// Out.prln("==========real Action============");
				int acOffset = matchStr.length() - matcherAction.start() - 4;
				int acLocation = startIndex + acOffset;
				// Out.prln(strAll.substring(acLocation, matcher.end()));
				Out.prln("==========REST Action============");
				actionStr = new StringBuilder(matcherAction.group(1)).reverse().toString();
				JSONObject acJson = new JSONObject();
				acJson.put(actionStr, acLocation);
				sectionJson.put("action", acJson);

				Out.prln(actionStr);
			}
			// match endpoint
			String regexHttp = scheme;
			Pattern endpoint = Pattern.compile(regexHttp, Pattern.CASE_INSENSITIVE);
			Matcher endpointMatcher = endpoint.matcher(matchStr);
			if (endpointMatcher.find()) {
				Out.prln("urlStart： " + endpointMatcher.start());
				int uLocation = startIndex + endpointMatcher.start();
				urlString = matchStr.substring(endpointMatcher.start()).split("\n")[0].trim();
				// handle url, make it short and clean
				urlString = processMe.cleanUrl(urlString);

				// Out.prln("==========real ADDRESS============");
				// Out.prln(strAll.substring(uLocation, uLocation + 100));
				Out.prln("==========URL ADDRESS============");
				Out.prln(urlString);
				JSONObject urJson = new JSONObject();
				urJson.put(urlString, uLocation);
				sectionJson.put("url", urJson);
			}

			// Write into swagger
			// After matching table, we write url/action into swagger
			// processMe.addUrl(swagger, urlString, actionStr);
			infoJson.add(sectionJson);
		}

		// 4.2 handle info json
		// solve the conflicts: one URL with two action
		Out.prln("---------INFO JSON-------");
		Out.prln(infoJson.toString());

		// 5.1 get original markups
		doc.setMarkupAware(true);

		AnnotationSet annoOrigin = doc.getAnnotations("Original markups");

		if (template == "table") {
			// 5.2 get table annotation
			AnnotationSet annoTable = annoOrigin.get("table");
			handleTemplate(swagger, number, template, doc, processMe, strAll, infoJson, annoTable);
		} else if (template == "list") {
			AnnotationSet annoList = annoOrigin.get("dl");
			// 5.3 get list annotation
			handleTemplate(swagger, number, template, doc, processMe, strAll, infoJson, annoList);
		}

	}

	private static void handleTemplate(JSONObject swagger, String templateNum, String template, Document doc,
			ProcessMethod processMe, String strAll, List<JSONObject> infoJson, AnnotationSet annoTemplate)
			throws JSONException {
		// 5.3 for each page, set findParaTable = False
		boolean findParaTemplate = false;
		// 5.3.1 Test if the page contains multiply parameter table or not
		Iterator<Annotation> testIter = annoTemplate.iterator();
		String templateNumber = templateNum;
		// int numTemplate = 0;
		while (testIter.hasNext()) {
			Annotation anno = (Annotation) testIter.next();
			String templateText = gate.Utils.stringFor(doc, anno);
			ProcessParameter processPa = new ProcessParameter();
			if (processPa.isParaTemplate(templateText, anno, strAll)) {
				// numTemplate++;
			}
		}

		// if (numTemplate > 1) {
		// // more than one parameter template in the page
		// multiTemplate = "multiply";
		// }

		// 5.3.2 handle the template context
		Iterator<Annotation> templateIter = annoTemplate.iterator();
		while (templateIter.hasNext()) {
			Annotation anno = (Annotation) templateIter.next();
			String templateText = gate.Utils.stringFor(doc, anno);
			ProcessParameter processPa = new ProcessParameter();
			if (processPa.isParaTemplate(templateText, anno, strAll)) {
				findParaTemplate = true;
				Out.prln("==========TABLE =================");
				Out.prln(templateText);
				processPa.generateParameter(swagger, templateText, strAll, infoJson, anno, doc, processMe,
						templateNumber, template);
			}
		}

		// 5.4 In case of the method doesn't have parameter
		// fix then.....
		if (!findParaTemplate) {
			// can't find table
			if (!infoJson.isEmpty()) {
				// In case of the method doesn't have parameter
				// add the noPara url
				processMe.addNoParaUrl(swagger, strAll, infoJson);
			}
		}
	}

	public static void writeSwagger(JSONObject swagger, String scheme, String template, String number, String abbrev)
			throws IOException {
		// Print pretty swagger
		String fileName = scheme + "_" + template + "_" + number + "_" + abbrev + ".json";
		writeFile(swagger.toString(), fileName);
	}

	public static void writeFile(String swagger, String fileName) throws IOException {
		FileWriter fileWriter = new FileWriter(FOLDER_PATH + "/" + fileName);
		String swString = swagger.toString();
		JsonParser parser = new JsonParser();
		JsonElement jelement = parser.parse(swString);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String prettyJson = gson.toJson(jelement);

		fileWriter.write(prettyJson);
		fileWriter.close();
	}

	public static void selectSwagger(File folder) throws IOException {
		File[] listNew = folder.listFiles();
		String finalName = "OpenAPI.json";
		File finalFile = new File(FOLDER_PATH + "/" + finalName);

		for (int i = 0; i < listNew.length; i++) {
			String type = new Tika().detect(listNew[i].getPath());
			// only detect .json
			if (type.equals("application/json")) {
				// ignore final file
				if (listNew[i].getName() != finalName) {
					// select the maximum size file
					if (listNew[i].length() > finalFile.length()) {
						writeFile(new String(Files.readAllBytes(listNew[i].toPath())), finalName);
					}
				}
			}
		}
	}

}
