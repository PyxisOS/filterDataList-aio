package it.pyxisos.script;

/*
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

//JAVA UTILS
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


//JSON
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

//WEBSCRIPT
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.surf.util.Content;


//LOGGING
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//EXCEL
import org.alfresco.repo.content.MimetypeMap;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Font;


/**
 * Wevscript controller to export data in excel
 *
 * @author zarbano@pyxisos.com
 * @since 2.1.0
 */
public class ExportDataExcelPOST extends AbstractWebScript {
     private String fileName = "ExcelSheet";
     private String format = "xlsx";
     
     //logging
	 Log log = LogFactory.getLog(ExportDataExcelPOST.class);
 
	 /**
	  * This method is inherited from AbstractWebscript class and called
	  * internally.
	  */
	 @Override
	 public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException{
	 
      	
      	//Controlliamo che la richiesta non sia vuota
      	if (req.getContent() == null)
          throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Corpo del POST assente.");   	
      
    	
    	JSONObject datiExcel = null;
    	
    	log.debug("contenuto della richiesta 1: " + req.parseContent().toString());
      	log.debug("contenuto della richiesta 2: " + req.getContent());
      	
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

     	writeDataInExcel(res, columns, records);
     	
     	log.debug("esecuzione del webscript per esportazione in EXCEL");
	 }
	 
	 
	 
	 
	 /**
	  * 
	  * This method is used for Writing data in byte array.It is calling other
	  * methods as well which will help to generate data.
	  * 
	  * @param res
	  *            Writing content in output stream
	  * @throws IOException
	  */
		public void writeDataInExcel(WebScriptResponse res, JSONArray columns, JSONArray records) throws IOException {
		
	
			// Create Work Book and WorkSheet
			Workbook wb = new XSSFWorkbook();
			Sheet sheet = wb.createSheet("ExcelFile");
		  	sheet.createFreezePane(0, 1);
 			
 			//style intestazione
			CellStyle styleHeader = generateStyleHeader(wb);
			
			//style intestazione
			CellStyle styleData = generateStyleData(wb);
			
 			
 			//generazione dell'intestazione
		  	generateHeaderInExcel(styleHeader, sheet, columns);
		  	//generazione del contenuto
		  	generateDataInExcel(styleData, sheet, records);
		  	//generazione dello stream
		  	ByteArrayOutputStream baos = new ByteArrayOutputStream();
		  	//scrittura dello stream
		  	wb.write(baos);
		 	//scrittura dei byte nella response
		 	ResponseDataInExcel(res, baos.toByteArray());
	 }
	 
	 
	 /**
	  * 
	  * @param res
	  *            Used for controlling the response.In our case we want to give
	  *            file as response.
	  * @param data
	  *            Content which we need to write in excel file
	  * @throws IOException
	  */
	 public void ResponseDataInExcel(WebScriptResponse res, byte[] data) throws IOException {
		  String filename = this.fileName + "." + this.format;
		  // Set Header for downloading file.
		  log.debug("FileName: " + filename);
		  res.addHeader("Content-Disposition", "attachment; filename=" + filename);
		  // Set spredsheet data
		  byte[] spreadsheet = null;
		  res.setContentType(MimetypeMap.MIMETYPE_OPENXML_SPREADSHEET);
		  res.getOutputStream().write(data);
	 }
	 
	 
	 /**
	  * 
	  * @return CellStyle of cells headers columns
	  */
	 public CellStyle generateStyleHeader(Workbook wb){
			CellStyle style = wb.createCellStyle();
			Font font = wb.createFont();
			font.setBoldweight(Font.BOLDWEIGHT_BOLD);
			style.setFont(font);
			style.setBorderRight(CellStyle.BORDER_THIN);
			style.setBorderLeft(CellStyle.BORDER_THIN);
			style.setBorderTop(CellStyle.BORDER_THIN);
			style.setBorderBottom(CellStyle.BORDER_THIN);
			return style;
	 }
	 
	  /**
	  * 
	  * @return CellStyle of cell data columns
	  */
	 public CellStyle generateStyleData(Workbook wb){
			CellStyle style = wb.createCellStyle();
			style.setBorderRight(CellStyle.BORDER_THIN);
			style.setBorderLeft(CellStyle.BORDER_THIN);
			style.setBorderTop(CellStyle.BORDER_THIN);
			style.setBorderBottom(CellStyle.BORDER_THIN);
			return style;
	 }
	 
	 
	 /**
	  * 
	  * @return List of header which we need to write in excel
	  */
	 public List<String> getHeaderList(JSONArray columns) {
	  	List<String> listString = new ArrayList<>();
	  	try{
			for (int i = 0; i < columns.length(); i++){
				listString.add((String)columns.get(i));
				//log.debug("Colonna " + i + ": " + columns.get(i));}
			}
		}
		catch (JSONException e) {
         	throw new RuntimeException(e);
     	}
     	return listString;
	 }
	 
	 
		 /**
	  * This method is used for creating multiple row.So the inner list object
	  * will contain the cell details and the outer one will contain row details
	  * 
	  * @return List of values which we need to write in excel
	  */
	 public List<List<Object>> getData(JSONArray records) {
	   List<List<Object>> listString = new ArrayList<>();	

	   try{
			for (int i = 0; i < records.length(); i++){
				List<Object> sampleRowX = new ArrayList<>();
				JSONArray x = records.getJSONArray(i);
				for (int j = 0; j < x.length(); j++){
					sampleRowX.add(x.get(j));
				}
				listString.add(sampleRowX);
			}
		}
		catch (JSONException e) {
         	throw new RuntimeException(e);
     	}
	  return listString;
	 }
	 
	 
	 /**
	  * Used for generating header in excel.
	  * 
	  * @param sheet
	  *  It is an excel sheet object, where the header will be created
	  */
		public void generateHeaderInExcel(CellStyle style, Sheet sheet, JSONArray columns) {
			
			List<String> headerValues = getHeaderList(columns);
			Row hr = sheet.createRow(0);
			hr.setHeight((short) 400);
			for (int i = 0; i < headerValues.size(); i++) {
				Cell c = hr.createCell(i);
				c.setCellValue(headerValues.get(i));
				c.setCellStyle(style);
				sheet.autoSizeColumn(i);
			}
	 	}
	 
	 /**
	  * Used for generating data in excel sheet
	  * 
	  * @param sheet
	  *            sheet It is an excel sheet object, where the data will be
	  *            written
	  */
		public void generateDataInExcel(CellStyle style, Sheet sheet, JSONArray records) {
			List<List<Object>> listOfData = getData(records);
			// Give first row as 1 and column as 0
			int rowNum = 1, colNum = 0;
			 
			for (List<Object> rowValues : listOfData) {
				Row r = sheet.createRow(rowNum);
				r.setHeight((short) 400);
				colNum = 0;
				for (Object obj : rowValues) {
					Cell c = r.createCell(colNum);
					// Here you can add n number of condition for identifying the
					// type of object and based on that fetch value of it
					if (obj instanceof String) {
						 c.setCellValue(obj.toString());
					} else if (obj instanceof Integer) {
						c.setCellValue((int) obj);
					}
					//applichiamo lo stile
					c.setCellStyle(style);
					//ridimensionamento dell celle automatico
					sheet.autoSizeColumn(colNum);
					colNum++;
				}
			
			rowNum++;
	  	}
	}
 }