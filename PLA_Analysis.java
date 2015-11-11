import ij.plugin.PlugIn;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.process.ImageProcessor;
import ij.plugin.Duplicator;

import ij.plugin.filter.Analyzer;
import ij.measure.ResultsTable;
import java.io.File;
import ij.io.Opener;
import java.util.List;
import java.util.ArrayList;
import java.io.PrintWriter;
import java.io.FileWriter;

public class PLA_Analysis extends ImagePlus implements PlugIn {

	// STRING FOR OS
	public static String os_system = "";
	public static String os_slash = "";
	
    /////////// ---------- CONFIGURATION
    public static Double areaMaxValue = 9999999.0;  	// AREA UPPER BOUND FOR MAX PLA-SIZE
    public static Boolean fixedThreshold = true;	// SET TO 0 IF YOU WANT TO DEFINE IT AUTOMATICALLY, OTHERWISE YOUR FIXED VALUE WILL BE USED
    public static Integer autoThreshold = 0;        
    /////////// --------------------------------------------

    public static String version = "0.83";
    public static String path = "";
    public static String pathChannels;
    public static String pathAnalysis;
    public static ArrayList<String> imageFiles;
    public static ArrayList<String> plaResults;
    
	// Format of singleResults:
    public static Integer headerElements;
    public static ArrayList<String> csvHeader;
    public static String singleResults;
    public static String allResults;
    public static Integer plaThreshold;
    
    public static Double map2Area;
    public static Double plaArea;
	public static Double totalIntDen;
	public static Double maxIntDen;
	public static Double minIntDen;
    public static Double sumMAP2Area;
    public static Double sumPLAArea;
	public static Double sumIntDen;

    public static Integer countedAreas;
    public static Integer numAreas;

    public void run(String arg) {

    // Initialization
    
    imageFiles = new ArrayList<String>();
    plaResults = new ArrayList<String>();
    headerElements = 6;
    csvHeader = new ArrayList<String>();
    singleResults = "";
    allResults = "";
    plaThreshold = 70;
    map2Area = 0.0;
    plaArea = 0.0;
    sumMAP2Area = 0.0;
    sumPLAArea = 0.0;
    countedAreas = 0;
    numAreas = 0;
	totalIntDen = 0.0;
	maxIntDen = 0.0;
	minIntDen = 99999999.99;
	sumIntDen = 0.0;
    
    os_system = System.getProperty("os.name");
    if (os_system.contains("Mac"))
    	 os_slash = "/";
    else os_slash = "\\";

    IJ.log("-----------------[ Start PLA_Analysis V"+version+" ]-----------------");
    IJ.log("System information: "+os_system+" separator: "+os_slash);
    path = IJ.getDirectory("Choose Directory of Image File(s)");
    IJ.log("- Directory: "+path);
    
    // Ask for automatic/fixed MAP2-threshold
	GenericDialog gd_map = new GenericDialog("MAP2-Threshold");
	gd_map.addMessage("Do you want to define a fixed MAP2-threshold for all files? \n\n\nSet the value to 0 if the threshold has to be defined automatically for each image. ");
	gd_map.addNumericField("Threshold: ", autoThreshold, 0);
	gd_map.showDialog();
    if (gd_map.wasCanceled()) return;
    autoThreshold = Math.max(0,Math.min((int)gd_map.getNextNumber(),254));
    if (autoThreshold == 0) {fixedThreshold = false;}
    else fixedThreshold = true;

     // Ask for automatic/fixed MAP2-threshold
    GenericDialog gd_pla = new GenericDialog("PLA-Threshold");
    gd_pla.addMessage("Change the Number if you want a different PLA-threshold for all files. Otherwise click OK.");
    gd_pla.addNumericField("Threshold: ", plaThreshold, 0);
    gd_pla.showDialog();
    if (gd_pla.wasCanceled()) return;
    plaThreshold = Math.max(0, Math.min((int)gd_pla.getNextNumber(),254));
    
    final File folder = new File(path);
    imageFiles = listFilesForFolder(folder);
    createPaths(path);
    
    // Create the subfolders
	String imageName;
	
	IJ.log("---------------------- Start processing images");
	String HTMLString = "";
    	for (int i=0; i<imageFiles.size(); i++){
    		plaArea = 0.0;
    		map2Area = 0.0;
			totalIntDen = 0.0;
    		imageName = imageFiles.get(i);
            if (csvHeader.size() < headerElements)
                csvHeader.add("imagename");
            singleResults += imageName+";";
            IJ.log("Image: "+imageName);
            processImage( imageName );
            measureImage( imageName );
            resetAll();

            if (i<imageFiles.size()-1)
            IJ.log("------------------------------");

            sumMAP2Area += map2Area;
            sumPLAArea += plaArea;
			sumIntDen += (totalIntDen/countedAreas);
            
            HTMLString = makeHTML(i, imageName, false, HTMLString);
            allResults = allResults + singleResults+"\n";
            singleResults="";
            }
    	makeHTML(0, "", true, HTMLString);
    	writeCSS();
        writeCSV();
	
    System.gc();
	IJ.log("-----------------[ End PLA-Plugin ]-----------------");
	return;
    }

    public String convertToPNG(String imageName){
        String pathHTML = path+"pla"+os_slash+"HTML"+os_slash;
        //IJ.log(pathHTML);
        Boolean success = (new File(pathHTML)).mkdirs();
    	if (!imageName.substring(imageName.length()-4).toLowerCase().equals(".png"))
    	{
    	//ImagePlus imp = IJ.openImage(path+"\\"+imageName);
    	ImagePlus imp = IJ.openImage(path+"/"+imageName);
    	imageName = imageName.substring(0, imageName.length()-4)+".png";
    	IJ.saveAs(imp, "png", pathHTML+imageName);
    	IJ.log("... converted to .png ");
    	}
    	return imageName;
    }
    
    public ArrayList<String> listFilesForFolder(final File folder) {
    	ArrayList<String> dirFiles = new ArrayList<String>();
    	ArrayList<String> fileFormats = new ArrayList<String>() {{
	    add(".jpg");
	    add(".tif");
	    add(".png");
	    add(".gif");
	    add(".bmp");
        }};
        Integer k = 0;
	    for (final File fileEntry : folder.listFiles()) {
	        if (fileEntry.isDirectory()) {
	       	    //this is for recursive search of the directory
	            //listFilesForFolder(fileEntry);
	            //IJ.log("Skipped directory: "+fileEntry.getName());
	        } else {
	            String fileName = fileEntry.getName();
	            
	            for (int i=0; i<fileFormats.size(); i++){
	            	if (fileName.contains(fileFormats.get(i))){
	            		// replace spaces
                        if (fileName.contains(" ")){
	            			File newFile = new File(path+fileName.replace(" ", "_"));
	            			fileEntry.renameTo(newFile);
	            			fileName = fileName.replace(" ", "_");
                            }
	            		
                        // convert the image into png-format
                        IJ.log("start converting file: "+fileName);
                        convertToPNG(fileName);

                        //if the image is not added for processing
                        if (!dirFiles.contains(fileName))
                                {
                                    IJ.log("_"+k.toString()+". Image-File: "+fileName);
                                    dirFiles.add(fileName);
                                }
                                k++;
                                break;
	            	}
	            }
	        }
	    }
	return dirFiles;
	}
    
    public void createPaths(String thispath){
    	pathChannels = thispath + "pla"+os_slash+"temp"+os_slash+"channels";
    	pathAnalysis = thispath + "pla"+os_slash+"analysis";
    	Boolean success = (new File(pathChannels)).mkdirs();
        Boolean success2 = (new File(pathAnalysis)).mkdirs();
    }

    public void processImage( String imageName ){
        splitChannels( imageName );
        maskMAP2(imageName);
        maskPLA();
        return;
    }

    public void measureImage( String imageName ){
        plaArea = measurePLA( imageName );
        map2Area = measureMAP2( imageName );
        return;
    }

    public void splitChannels( String imageName ){
        ImagePlus imp = IJ.openImage(path+imageName);
        IJ.run(imp, "Split Channels", "");

        IJ.log("SAVING CHANNELS AT: "+pathChannels);
        IJ.selectWindow(imageName+" (green)");
        IJ.saveAs("TIF", pathChannels+os_slash+"green.tif");

        IJ.selectWindow(imageName+" (blue)");
        IJ.saveAs("TIF", pathChannels+os_slash+"blue.tif");

        IJ.selectWindow(imageName+" (red)");
        IJ.saveAs("TIF", pathChannels+os_slash+"red.tif");

        IJ.run("Close All", "");
        return;
    }

    public void maskMAP2( String imageName ){
        IJ.log("MASKING MAP 2 :" +imageName);
        ImagePlus imp = IJ.openImage(pathChannels+os_slash+"green.tif");
        imp.show();

        ImageProcessor ip = imp.getProcessor();
        Integer thisThreshold;
        if (!fixedThreshold){
            thisThreshold = ip.getAutoThreshold();}
        else{
            thisThreshold = autoThreshold;}
        if (csvHeader.size() < headerElements)
            csvHeader.add("map2-thr;pla-thr");
        singleResults += String.valueOf(thisThreshold)+";";
        singleResults += String.valueOf(plaThreshold)+";";
        IJ.log("- MAP2-Threshold (Mask): "+thisThreshold.toString());
        
        IJ.setThreshold(imp, 0, thisThreshold);
        IJ.run("Threshold", "thresholded remaining black slice"); 
        
        IJ.run(imp, "Convert to Mask", "");
        IJ.run(imp, "Dilate", "");

        ImagePlus imp2 = new Duplicator().run(imp);
        
        IJ.saveAs("png", pathAnalysis+os_slash+"DIL_MASK_"+imageName.replace(".tif","")+".png");
        imp.close();
        imp2.show();
        return;
    }

    public void maskPLA(){
        ImagePlus imp = IJ.openImage(pathChannels+os_slash+"red.tif");
        imp.show();
        
        IJ.setThreshold(imp, 0, plaThreshold);
        IJ.run("Threshold", "thresholded remaining black slice"); 
            
        IJ.run(imp, "Convert to Mask", "");
        IJ.run(imp, "Create Mask", "");
		
        imp.close();
		
        return;
    }

    public Double measurePLA(String imageName){
        //ImagePlus dilGreen = IJ.openImage(pathAnalysis+"\\DIL_MASK_"+imageName+".jpg");
        //dilGreen.show();
        //String origTitle = dilGreen.getTitle();
        IJ.selectWindow("mask");
        IJ.run("Set Measurements...", "area mean integrated gray redirect=DUP_green.tif decimal=3");
        IJ.run("Analyze Particles...", "size=0-Infinity circularity=0.00-100.00 show display clear add in_situ");

        IJ.selectWindow("Results");
		
        ResultsTable rt = ResultsTable.getResultsTable();
        Integer numResults = rt.getCounter();
        Integer numColumns = rt.getLastColumn();
		IJ.log("- Amount of columns: "+numColumns.toString());
        Double area, mean, intDen, rawIntDen, totalArea;
        area=0.0; mean=0.0; intDen =0.0; rawIntDen=0.0; totalArea=0.0;
		countedAreas = 0;
        numAreas = numResults;
        
        for (int i = 0; i<numResults; i++){
            area = rt.getValueAsDouble(0,i);
            mean = rt.getValueAsDouble(1,i);
			intDen = rt.getValueAsDouble(20,i);
			rawIntDen = rt.getValueAsDouble(25,i);

			//IJ.log("- IntDen "+intDen.toString());

            String addExcluded = "";
            if (area > areaMaxValue)
                addExcluded = " - TOO LARGE!!!";
            //IJ.log("A: "+String.valueOf(area)+addExcluded);
            //IJ.log("M: "+String.valueOf(mean));
            //IJ.log("---Area: "+String.valueOf(area)+" Mean: "+String.valueOf(mean));
            if (mean>0.0 && area <= areaMaxValue){
                totalArea += area;
                countedAreas++;
				totalIntDen += intDen;
				
				if (intDen > maxIntDen) maxIntDen = intDen;
				if (intDen < minIntDen) minIntDen = intDen;
				
            }
        }

        IJ.log("- PLA Area: "+String.valueOf(totalArea)+" (counted: "+countedAreas.toString()+"/"+numResults.toString()+") intden: "+intDen.toString()+" rawint: "+rawIntDen.toString());
        IJ.saveAs("Results", pathAnalysis+os_slash+imageName+"_Results.csv"); // XLS OR CSV		// just _Results.xls contains PLA area
        IJ.run("Close");

        IJ.selectWindow("mask");
        IJ.saveAs("tif", path+os_slash+"pla"+os_slash+"temp"+os_slash+"red_mask.tif");
        Double ret;
        ret = totalArea;
        if (csvHeader.size() < headerElements)
            csvHeader.add("pla");
        singleResults += String.format("%.6f",ret)+";";
        return ret;
    }

    public Double measureMAP2(String imageName){
        ImagePlus imp = IJ.openImage(pathChannels+os_slash+"green.tif");
        imp.show();

        ImageProcessor ip = imp.getProcessor();
        Integer thisThreshold;
        if (!fixedThreshold)
            {
            thisThreshold = ip.getAutoThreshold();
            autoThreshold = thisThreshold;
            }
        else{
            thisThreshold = autoThreshold;}
        
        IJ.setThreshold(imp, 0, thisThreshold);
        IJ.log("- MAP2-Threshold (Measure): "+thisThreshold.toString());
        IJ.run("Threshold", "thresholded remaining black slice"); 
        IJ.run(imp, "Convert to Mask", "");
        IJ.run(imp, "Create Selection", "");
        IJ.run("Set Measurements...", "area mean gray redirect=None decimal=3");
        IJ.run(imp, "Measure", "");
        IJ.run(imp, "Select None", "");
        imp.close();
        IJ.selectWindow("Results");

        ResultsTable rt = ResultsTable.getResultsTable();
        Double thisMap2Area = rt.getValueAsDouble(0,0);

        //IJ.log("- MAP2 Area: "+String.valueOf(thisMap2Area)+" micron? = "+(String.valueOf(thisMap2Area)).contains("."));
            
        IJ.saveAs("Results", pathAnalysis+os_slash+imageName+"_NeuronArea__Results.csv"); // XLS OR CSV //_NeuronArea_Results.xls contains MAP2 Area
        IJ.run("Close");

        IJ.log("-- PLA/MAP2 - RATIO: "+String.valueOf(plaArea/thisMap2Area)+"   * 100 = "+String.valueOf((plaArea/thisMap2Area)*100));
        if (csvHeader.size() < headerElements)
            csvHeader.add("map2");
        singleResults += String.format("%.6f", thisMap2Area)+";";
        if (csvHeader.size() < headerElements)
            csvHeader.add("pla/map2-ratio");
        singleResults += String.format("%.6f", plaArea/thisMap2Area)+";";
        Double ret = thisMap2Area;

        return ret;
    }

    public String makeHTML(Integer i, String imageName, Boolean writeFileNow, String HTMLString){
    	String pathHTML = path+os_slash+"pla"+os_slash+"HTML"+os_slash+i.toString()+os_slash;
    	Boolean success = (new File(pathHTML)).mkdirs();
        imageName = imageName.replace(".tif","");

        if (writeFileNow){
            // make new results file
                IJ.log("------------- SUMMARY -------------");
                Double meanPLA = sumPLAArea/imageFiles.size();
                Double meanMAP2 = sumMAP2Area/imageFiles.size();
				Double meanIntDen = sumIntDen/imageFiles.size();
                Double meanRatio = ((meanPLA/imageFiles.size()) / (meanMAP2/imageFiles.size()))*100;
                IJ.log("SUM of Areas [PLA] "+String.valueOf(sumPLAArea)+" / [MAP2] "+String.valueOf(sumMAP2Area)+" = "+String.valueOf(sumPLAArea/sumMAP2Area));
                IJ.log("MEAN Value [PLA] "+String.valueOf(meanPLA)+" / [MAP2] "+String.valueOf(meanMAP2)+" = "+String.valueOf(meanRatio));
                
            String HTMLHeader = "<html><head><title>PLA-Analysis</title><link href=\"../HTML/style.css\" rel=\"stylesheet\" type=\"text/css\"></head><body><div id=\"main_container\"><h1>Analysis of PLA</h1><div class=\"content_left\"><p>MAP2-Threshold</p><p>PLA-Threshold</p><p>Analyzed images</p><p>Mean-PLA</p><p>Mean-MAP2</p><p>Mean-Ratio</p><p>Mean-Intensity</p></div><div class=\"content_right\"><p>Auto</p><p>"+plaThreshold+"</p><p>"+imageFiles.size()+"</p><p>"+String.format("%.2f", meanPLA)+"</p><p>"+String.format("%.2f", meanMAP2)+"</p><p>"+String.format("%.2f", meanRatio)+"%</p><p>"+String.format("%.2f", meanIntDen)+"</p><p><a href=\"ALL_RESULTS.csv\" target=\"blank\"> >> ALL_RESULTS.csv</a></p></div><div class=\"clear_float\"></div>";
            String fullHTML = HTMLHeader+HTMLString;
            try {
            PrintWriter writer = new PrintWriter(pathAnalysis+os_slash+"results_summary.html", "UTF-8");
            writer.println(fullHTML);
            writer.close();
            }catch (Exception ex){	
            }

            return HTMLString;
        }

        else {

            ImagePlus green = IJ.openImage(pathChannels+os_slash+"green.tif");
            green.show();
            ImagePlus red = IJ.openImage(pathChannels+os_slash+"red.tif");
            red.show();
            ImagePlus blue = IJ.openImage(pathChannels+os_slash+"blue.tif");
            blue.show();

            IJ.run(green, "Merge Channels...", "c1=red.tif c2=green.tif c3=blue.tif create");
            IJ.selectWindow("Composite");
            IJ.run("Split Channels", "");

            IJ.selectWindow("C1-Composite");
            IJ.saveAs("png", pathHTML+"red_orig.png");
            IJ.run("Close");
            IJ.selectWindow("C2-Composite");
            IJ.saveAs("png", pathHTML+"green_orig.png");
            IJ.run("Close");
            IJ.selectWindow("C3-Composite");
            IJ.saveAs("png", pathHTML+"blue_orig.png");
            IJ.run("Close");

            ImagePlus red_mask = IJ.openImage(path+os_slash+"pla"+os_slash+"temp"+os_slash+"red_mask.tif");
            red_mask.show();
            IJ.run(red_mask, "Invert", "");
            IJ.run(red_mask, "RGB Color", "");
            ImagePlus green_dil = IJ.openImage(pathAnalysis+os_slash+"DIL_MASK_"+imageName+".png");
            green_dil.show();
            ImagePlus blue_orig = IJ.openImage(pathHTML+"blue_orig.png");
            blue_orig.show();
            
            //IJ.log("HERE MERGE CHANNELS!");
            //IJ.selectWindow("red_mask.tif");
            //IJ.run("8-bit", "");
            
            Integer red_bitDepth = red_mask.getBitDepth();
            Integer green_bitDepth = green_dil.getBitDepth();
            Integer blue_bitDepth = blue_orig.getBitDepth();
            IJ.log("Bit-depths: "+red_bitDepth+" "+green_bitDepth+" "+blue_bitDepth);
            if (( red_bitDepth != green_bitDepth ) || (red_bitDepth != green_bitDepth ) || ( green_bitDepth != blue_bitDepth )){
                IJ.run(red_mask, "RGB Color", "");
                IJ.run(green_dil, "RGB Color", "");
                IJ.run(blue_orig, "RGB Color", "");
            }
           
            IJ.run(green_dil, "Merge Channels...", "c1=red_mask.tif c2=DIL_MASK_"+imageName+".png c3=blue_orig.png create");

            IJ.selectWindow("Composite");
            IJ.saveAs("png", pathHTML+"red_green_composite.png");
            IJ.run("Close");

            ImagePlus green2 = IJ.openImage(pathChannels+os_slash+"green.tif");
            green2.show();
            IJ.run(green2, "Invert", "");
            IJ.saveAs("png", pathHTML+"green_inverted.png");
            IJ.run("Close");
            ImagePlus red2 = IJ.openImage(pathChannels+os_slash+"red.tif");
            red2.show();
            IJ.run(red2, "Invert", "");
            IJ.saveAs("png", pathHTML+"red_inverted.png");
            IJ.run("Close");
            ImagePlus blue2 = IJ.openImage(pathChannels+os_slash+"blue.tif");
            blue2.show();
            IJ.run(blue2, "Invert", "");
            IJ.saveAs("png", pathHTML+"blue_inverted.png");
            IJ.run("Close");
			
			// SAVE PLA MASK
			ImagePlus imp = IJ.openImage(pathChannels+os_slash+"red.tif");
			imp.show();
			IJ.setThreshold(imp, 0, plaThreshold);
			IJ.run("Threshold", "thresholded remaining black slice"); 
			IJ.run(imp, "Convert to Mask", "");
			IJ.run(imp, "Create Mask", "");
			IJ.saveAs("png", pathAnalysis+os_slash+"PLA_MASK_"+imageName+".png");
			imp.close();
			IJ.run("Close");

            // append HTML-data to HTML-File
            HTMLString += "<div class=\"results\"><h2>"+imageName+"</h2><div class=\"upper_content_box\"><h3>Results</h3><div class=\"content_left\"><p>MAP2 Area</p><p>MAP2-THR</p><p>PLA Area</p><p>Ratio</p><p>Counted</p><p>&empty;Intensity</p></div><div class=\"content_right\"><p>"+String.format("%.2f", map2Area)+"</p><p>"+String.valueOf(autoThreshold)+"</p><p>"+String.format("%.2f", plaArea)+"</p><p>"+String.format("%.2f", (plaArea/map2Area)*100)+"%</p><p>"+countedAreas.toString()+"/"+numAreas.toString()+"</p><p>"+String.format("%.2f", (totalIntDen/countedAreas))+"</p></div></div><div class=\"upper_content_box\"><h3>Original Image</h3><a href=\"../HTML/"+imageName+".png\" target=\"blank\"><img src=\"../HTML/"+imageName+".png\" /></a></div><div class=\"upper_content_box\"><h3>Dilated mask</h3><a href=\"DIL_MASK_"+imageName+".png\" target=\"blank\"><img src=\"DIL_MASK_"+imageName+".png\" /></a></div><div class=\"clear_float\"></div><h4>Channels</h4><div class=\"lower_content_box\">    <a href=\"../HTML/"+i.toString()+"/red_orig.png\" target=\"blank\"><img src=\"../HTML/"+i.toString()+"/red_orig.png\" /></a>    <a href=\"../HTML/"+i.toString()+"/red_inverted.png\" target=\"blank\"><img src=\"../HTML/"+i.toString()+"/red_inverted.png\" /></a></div>   <div class=\"lower_content_box\"><a href=\"PLA_MASK_"+imageName+".png\" target=\"blank\"><img src=\"PLA_MASK_"+imageName+".png\" /></a>&nbsp;<b>PLA Mask</b><br /><br />&nbsp;<b>Intensity</b><br />&nbsp;min: "+String.format("%.2f",minIntDen)+"<br />&nbsp;max: "+String.format("%.2f",maxIntDen)+"</div>     <div class=\"lower_content_box\">    <a href=\"../HTML/"+i.toString()+"/green_orig.png\" target=\"blank\"><img src=\"../HTML/"+i.toString()+"/green_orig.png\" /></a>    <a href=\"../HTML/"+i.toString()+"/green_inverted.png\" target=\"blank\"><img src=\"../HTML/"+i.toString()+"/green_inverted.png\" /></a></div></div><div class=\"clear_float\"></div>";
            //countedAreas.toString()+"/"+numAreas.toString()
            if (csvHeader.size() < headerElements)
                csvHeader.add("counted_areas;total_areas");
            singleResults += String.valueOf(countedAreas.toString())+";";
            singleResults += String.valueOf(numAreas.toString())+";";
            if (i == imageFiles.size()-1){
                HTMLString+="</div></body></html>";
                }
            }

    return HTMLString;
    }

    public void writeCSS(){
        String pathCSS = path+os_slash+"pla"+os_slash+"HTML"+os_slash;
        String cssString = "@font-face {font-family: 'Dosis';font-style: normal;font-weight: 400;src: local('Dosis Regular'), local('Dosis-Regular'), url(http://themes.googleusercontent.com/static/fonts/dosis/v2/xIAtSaglM8LZOYdGmG1JqQ.woff) format('woff');} body, div, h1, h2, h3, h4, h5, h6, p, ul, ol, li, dl, dt, dd, img, form, fieldset, input, textarea, blockquote {margin: 0; padding: 0; border: 0;} body {font: 15px 'Dosis', sans-serif; text-transform: uppercase; } #main_container {position:relative;margin:0px auto;width: 820px;} .results{position:relative;background-color: white;margin-top:40px;margin-left:20px;padding-left:10px;width: 800px;border:0px;border-top: 1px solid grey;} .upper_content_box{position:relative;float:left;background-color:white;margin-right:10px;margin-top:0px;height:260px;width:250px;} .results .upper_content_box img{width:248px;height:248px;border: 1px solid grey;} .lower_content_box{position:relative;background-color:white;float:left;margin-right:10px;margin-bottom:10px;height:122px;width:250px;} .results h4{color:red;} .results .lower_content_box img{position:relative;float:left;margin-left:2px;width:120px;height:120px;border: 1px solid grey;} .clear_float{clear:both;} .content_left{float:left;background-color:white;padding-top:20px;} .content_right{float:left;margin-left:10px;background-color:white;padding-top:20px;} h2{color:grey;} .content_left p{margin-bottom:10px;text-align:right;font-size:20px;} .content_right p{margin-bottom:10px;text-align:left;font-size:20px;} .results .upper_content_box img:hover {border-color:blue;} .results .lower_content_box img:hover {border-color:blue;} a:link { text-decoration:none; } a:visited { text-decoration:none;  } a:hover { text-decoration:none; } a:active { text-decoration:none;  } a:focus { text-decoration:none;  }";
        
        try {
            PrintWriter writer = new PrintWriter(pathCSS+"style.css", "UTF-8");
            writer.println(cssString);
            writer.close();
        }catch (Exception ex){	
        }
    }
   
    public void writeCSV(){
        String pathCSV = path+os_slash+"pla"+os_slash+"analysis"+os_slash;
        try {
            PrintWriter writer = new PrintWriter(pathCSV+"ALL_RESULTS.csv", "UTF-8");
            String headerString = "";
            
            for (int k = 0; k<csvHeader.size(); k++){
                headerString = headerString + csvHeader.get(k)+";";
                }
            writer.println(headerString);
            writer.println(allResults);

            writer.close();
        }catch (Exception ex){	
        }
    }

    public void resetAll(){
    	IJ.run("Close All", "");
    	IJ.selectWindow("ROI Manager");
    	IJ.run("Close");
        return;
    }
}