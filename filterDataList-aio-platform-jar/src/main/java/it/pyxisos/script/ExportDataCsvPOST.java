package it.pyxisos.script;

//JAVA UTILS
import java.io.IOException;

//JSON
import org.json.JSONException;
import org.json.*;
import org.json.JSONObject;

//WEBSCRIPT
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.surf.util.Content;

//CSV
import java.io.StringWriter;
import org.apache.commons.csv.*;
import org.alfresco.repo.content.MimetypeMap;

//LOGGING
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

 


/**
 * A Java controller for export datalist in CSV.
 *
 * @author zarbano@pyxisos.com
 * @since 2.1.0
 */
public class ExportDataCsvPOST extends AbstractWebScript {
     private String fileName = "CsvFile";
     private String format = "csv";
     
     //Delimiter used in CSV file
   	 private static final String COMMA_DELIMITER = ",";
   	 private static final String NEW_LINE_SEPARATOR = "\n";
     
     
     //logging
	 Log log = LogFactory.getLog(ExportDataCsvPOST.class);
 
	 /**
	  * This method is inherited from AbstractWebscript class and called
	  * internally.
	  */
	 @Override
	 public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException{
	 	
      	String nomeSito = "";
      	String nomeFile = "";
      	
      	
      	log.debug("contenuto della richiesta 1: " + req.parseContent());
      	log.debug("contenuto della richiesta 2: " + req.getContent());
      	//Controlliamo che la richiesta non sia vuota
      	if (req.getContent() == null)
          throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Corpo del POST assente.");   	
      
    	
    	JSONObject datiExcel = null;
		
		//check json object
		Object jsonO = req.parseContent();
		if (jsonO instanceof JSONObject && jsonO != null)
			datiExcel = (JSONObject)jsonO;
		else
          throw new WebScriptException(Status.STATUS_BAD_REQUEST, "JSON errato " + jsonO);
          
        
       	JSONArray columns = new JSONArray();
        JSONArray records = new JSONArray();
        
        try{
         	 	
        	columns = datiExcel.getJSONArray("columns");
        	records = datiExcel.getJSONArray("rows");
        }
        catch (JSONException e) {
         	throw new RuntimeException(e);
     	}
     	
     	writeDataInCsv(res, columns, records);
    
        log.debug("esecuzione del webscript per esportazione in CSV");
	  
	 }
	 
	
		
	  public void writeDataInCsv(WebScriptResponse res, JSONArray columns, JSONArray records) throws IOException {
		 
		  StringWriter sw = new StringWriter();
		  String[] headers = generateHeaderInCsv(columns);
		  CSVPrinter csv = new CSVPrinter(sw,  CSVFormat.DEFAULT.withHeader(headers).withRecordSeparator(this.NEW_LINE_SEPARATOR));
          generateDataInCSV(csv, records);
		  sw.flush();
		  byte[] data = sw.toString().getBytes("UTF-8");          
          res.addHeader("Content-Disposition", "attachment; filename=" + this.fileName + "." + this.format);
		  res.setContentType(MimetypeMap.MIMETYPE_TEXT_CSV);
          res.getOutputStream().write(data);
		  
	  }
	 
	  
	  public String[] generateHeaderInCsv(JSONArray columns){
	  		String[] listString = new String[columns.length()];
			try{
				for (int i = 0; i < columns.length(); i++){
					listString[i] = ((String)columns.get(i));
					//log.debug("Colonna " + i + ": " + columns.get(i));
					}
				}
			
			catch (JSONException e) {
				throw new RuntimeException(e);
			}
			return listString;
	  }
	  
	  
	  
	  public void generateDataInCSV(CSVPrinter csv, JSONArray records) throws IOException{
	  		try{	
				for (int i = 0; i < records.length(); i++){
					//singolo oggetto/riga JSON
					JSONArray x = records.getJSONArray(i);
					Object[] row = new Object[x.length()];
					for (int j = 0; j < x.length(); j++){
					    row[j] = x.get(j);
					}
					csv.printRecord(row);
				}
	  		}
	  		catch (JSONException e) {
				throw new RuntimeException(e);
			}
	 }
	
 }