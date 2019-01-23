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


//PDF
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.PageSize;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Element;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.Font;
import com.itextpdf.text.Font.FontFamily;

/**
* A Java controller for export datalist in CSV.
*
* @author zarbano@pyxisos.com
* @since 2.1.0
*/
public class ExportDataPdfPOST extends AbstractWebScript {
   private String fileName = "CsvFile";
   private String format = "csv";
   
   //Delimiter used in CSV file
 	 private static final String COMMA_DELIMITER = ",";
 	 private static final String NEW_LINE_SEPARATOR = "\n";
   
   
   //logging
	 Log log = LogFactory.getLog(ExportDataPdfPOST.class);

	 /**
	  * This method is inherited from AbstractWebscript class and called
	  * internally.
	  */
	 @Override
	 public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException{
	 	
    	String nomeSito = "";
    	String nomeFile = "";
    	
    	
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
   	
   	writeDataInPdf(res, columns, records);
  
      log.debug("esecuzione del webscript per esportazione in PDF");
	  
	 }
	 
	
		
	  public void writeDataInPdf(WebScriptResponse res, JSONArray columns, JSONArray records) throws IOException{
		 
			Document document = new Document(PageSize.A4);
			try{
				//creazione del documento
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				PdfWriter.getInstance(document, baos);
				document.open();
				generateTableForPdf(document, columns, records);
				document.close();
				//scrittura della risposta
				res.addHeader("Content-Disposition", "attachment; filename=" + this.fileName + "." + this.format);
				res.setContentType(MimetypeMap.MIMETYPE_PDF);
				res.getOutputStream().write(baos.toByteArray());}
		
				catch (Exception e) {
					e.printStackTrace();
			}
		  
	  }
	 
	  
	  public void generateTableForPdf(Document doc, JSONArray columns, JSONArray records) throws IOException, DocumentException{
      	PdfPTable table = new PdfPTable(columns.length());
      	table.setWidthPercentage(100);
      	table.setSpacingBefore(0f);
          table.setSpacingAfter(0f);
      	generateHeaderTablePdf(table, columns);
      	generateDataTablePdf(table, records);
      	
      	/*table.setHeaderRows(1);
      	table.setSplitRows(false);
      	table.setComplete(false);

			for (int i = 0; i < 5; i++) {table.addCell("Header " + i);}

			for (int i = 0; i < 500; i++) {
				if (i%5 == 0) {
					doc.add(table);
				}
				table.addCell("Test " + i);
			}*/

			table.setComplete(true);
			doc.add(table);
	  
	  }
	  
	  public void generateHeaderTablePdf(PdfPTable table, JSONArray columns) throws IOException, DocumentException {
			try{
				for (int i = 0; i < columns.length(); i++){
					// first row
					Font f = new Font(FontFamily.HELVETICA, 8, Font.NORMAL, new BaseColor(0,0,0));
					PdfPCell cell = new PdfPCell(new Phrase((String)columns.get(i),f));
					 cell.setMinimumHeight(20);
                   cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                   cell.setHorizontalAlignment(Element.ALIGN_MIDDLE);
					 cell.setBackgroundColor(new BaseColor(221,221,221));
					table.addCell(cell);
					//log.debug("Colonna " + i + ": " + columns.get(i));
					}
				}
			
			catch (JSONException e) {
				throw new RuntimeException(e);
			}
	  }
	  
	  public void generateDataTablePdf(PdfPTable table, JSONArray records) throws IOException, DocumentException {
			try{
				for (int i = 0; i < records.length(); i++){
					JSONArray x = records.getJSONArray(i);
					for (int j = 0; j < x.length(); j++){
						Object obj = x.get(j);
						Font f = new Font(FontFamily.HELVETICA, 8, Font.NORMAL, new BaseColor(0,0,0));
						PdfPCell cell = new PdfPCell(new Phrase(obj.toString(),f));
						cell.setMinimumHeight(10);
						table.addCell(cell);
						//else if(obj instanceof Integer){
						//	table.addCell(Integer.toString(obj));
						//}
					}
				}
			}
		
			catch (JSONException e) {
				throw new RuntimeException(e);
			}
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