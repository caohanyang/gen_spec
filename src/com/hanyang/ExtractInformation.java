package com.hanyang;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import gate.annotation.AnnotationImpl;
import gate.creole.ANNIEConstants;
import gate.creole.SerialAnalyserController;
import gate.util.GateException;
import gate.util.Out;
import gate.util.persistence.PersistenceManager;

public class ExtractInformation {

	/** The Corpus Pipeline application to contain ANNIE */
	private CorpusController annieController;
	private static String FILE_PATH = "file:corpus/www.instagram.com/www.instagram.com_developer_endpoints_users.html";
	
	public static void main(String[] args) throws GateException, IOException {
        Gate.init();

        // 1. create corpus
        Corpus corpus = Factory.newCorpus("Test HTML Corpus");
        // 2. create doc
        URL u = new URL(FILE_PATH);
        FeatureMap params = Factory.newFeatureMap();
        params.put("sourceUrl", u);
        Document docNew = (Document) Factory.createResource("gate.corpora.DocumentImpl", params);
        // 3. add doc
        corpus.add(docNew);
        
        // 4. initial the specification
        GenerateMain mainObject = new GenerateMain();
        JSONObject swagger = mainObject.generateStructure();
        // 5. get all text
        DocumentContent textAll = docNew.getContent();
        
        // 5.1 search for the GET https
        String strAll = textAll.toString();
        Pattern p = Pattern.compile("(?si)((get)|(post)|(del)|(put)|(patch)){1}\\s(.*)http");
        Matcher matcher = p.matcher(strAll);
        
        while (matcher.find()) {
        	String group = matcher.group();
        	Out.prln(group);
        	String tmpStr = strAll.substring(matcher.start(), matcher.start()+100);
//        	Out.prln(strAll);
        	Out.prln("========tmpSTR==============");
        	Out.prln(tmpStr);
            // match action        	
        	Pattern action = Pattern.compile("((get)|(post)|(del)|(put)|(patch))", Pattern.CASE_INSENSITIVE);
        	Matcher matcherAction = action.matcher(tmpStr);
        	if(matcherAction.find()){
        		String actionStr = tmpStr.split(" ")[0];
        		Out.prln("==========REST Action============");
        		Out.prln(action);
        	}
        	// match endpoint
        	Pattern endpoint = Pattern.compile("http", Pattern.CASE_INSENSITIVE);
        	Matcher endpointMatcher = endpoint.matcher(tmpStr);
        	if(endpointMatcher.find()){
        		String urlString = tmpStr.substring(endpointMatcher.start()).split(" ")[0];
        		Out.prln("==========URL ADDRESS============");
        		Out.prln(urlString);
        	}
        }
        
        // 6.1 get original markups
        docNew.setMarkupAware(true);
       
        AnnotationSet annoOrigin = docNew.getAnnotations("Original markups");
        
        // 6.2 get table annotation
        AnnotationSet annoTable = annoOrigin.get("table");
        
        Iterator tableIter = annoTable.iterator();
        while(tableIter.hasNext()) {
        	Annotation anno = (Annotation) tableIter.next();
        	String txt = gate.Utils.stringFor(docNew, anno);
        	Out.prln("==========TABLE =================");
        	Out.prln(txt);
        }
        
        // Test swagger
        Out.prln(swagger);
        FileWriter fileWriter = new FileWriter("swagger.json");
		fileWriter.write(swagger.toString());
		fileWriter.close();
        
        
        
        
        
        
        
        
//        // 5. save all the contents including original markups to xml
//        String xmlFile = docNew.toXml();
//        FileWriter fw = new FileWriter("my.xml");
//        fw.write(xmlFile);
//        fw.close();
//        
//        // 6. get informations from xml file
//        URL ux = new URL("file:///Users/hanyangcao/Documents/workspace/TestGate/temp/my.xml");
//        FeatureMap paramsx = Factory.newFeatureMap();
//        params.put("sourceUrl", ux);
//        Document docXML = (Document) Factory.createResource("gate.corpora.DocumentImpl", paramsx);
//        Out.prln(docXML.getAnnotations());
        
        
        
//        // 5. get all table Annotations
//        // 5.1 initialise ANNIE (this may take several minutes)
//        testG annie = new testG();
//        annie.initAnnie();
//        
//        // 5.2 tell the pipeline about the corpus and run it
//        annie.setCorpus(corpus);
//        annie.execute();
//        
//        // 5.3 corpus iterator
//        Iterator iter = corpus.iterator();
//        int count = 0;
//        
//        while(iter.hasNext()) {
//        	Document doc = (Document) iter.next();
//        	AnnotationSet defaultAnnotSet = doc.getAnnotations();
//        	
//        	// 5.3.1 set table type
//            String type = "table";
//            
//            // 5.3.3 get type annotations
//            AnnotationSet tableSet = defaultAnnotSet.get(type);
//        	Out.prln(defaultAnnotSet.getAllTypes());
//     
//        }
	}
	
	  /**
	   * Initialise the ANNIE system. This creates a "corpus pipeline"
	   * application that can be used to run sets of documents through
	   * the extraction system.
	   */
	  public void initAnnie() throws GateException, IOException {
	    Out.prln("Initialising ANNIE...");

	    // load the ANNIE application from the saved state in plugins/ANNIE
	    File pluginsHome = Gate.getPluginsHome();
	    File anniePlugin = new File(pluginsHome, "ANNIE");
	    File annieGapp = new File(anniePlugin, "ANNIE_with_defaults.gapp");
	    annieController =
	      (CorpusController) PersistenceManager.loadObjectFromFile(annieGapp);
	    Out.prln("...ANNIE loaded");
	  } // initAnnie()
	  /** Tell ANNIE's controller about the corpus you want to run on */
	  public void setCorpus(Corpus corpus) {
	    annieController.setCorpus(corpus);
	  } // setCorpus

	  /** Run ANNIE */
	  public void execute() throws GateException {
	    Out.prln("Running ANNIE...");
	    annieController.execute();
	    Out.prln("...ANNIE complete");
	  } // execute()

}
