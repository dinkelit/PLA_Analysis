import ij.plugin.PlugIn;
import ij.IJ;
import ij.ImagePlus;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.io.PrintWriter;
import java.io.FileWriter;

public class lif_to_tif extends ImagePlus implements PlugIn {

	// STRING FOR OS
	public static String os_system = "";
	public static String os_slash = "";

	public static String version = "0.71";
	
	public static String path = "";
	public static ArrayList<String> lifFiles = new ArrayList<String>();

	public void run(String arg) {
    	IJ.log("----------------[ Start [LIF] to [TIF] conversion V"+version+"]----------------");

	    os_system = System.getProperty("os.name");
	    if (os_system.contains("Mac"))
	    	 os_slash = "/";
	    else os_slash = "\\";
    	
    	path = IJ.getDirectory("Choose one .lif-file");
    	IJ.log("- File: "+path);
    	

	final File folder = new File(path);
	listFilesForFolder(folder);

	String imageName;
	for (int i = 0; i<lifFiles.size(); i++){
		imageName = lifFiles.get(i);
		IJ.run("Bio-Formats Importer", "  open="+path+os_slash+imageName+" color_mode=Default view=Hyperstack stack_order=XYCZT use_virtual_stack");
		IJ.run("Z Project...", "projection=[Max Intensity]");
		IJ.run("Make Composite", "");
		IJ.run("RGB Color", "");
		IJ.saveAs("Tif", path+os_slash+imageName.substring(0, imageName.length()-3)+"tif");
		IJ.run("Close");
		IJ.run("Close");
		IJ.run("Close");
		IJ.log("- ... done");
	}
	IJ.log("-----------------[ End [LIF] to [TIF] conversion ]-----------------");
    	
	}

	public void listFilesForFolder(final File folder) {
    	ArrayList<String> fileFormats = new ArrayList<String>() {{
	    add(".lif");
	}};
	Integer k = 1;
	    for (final File fileEntry : folder.listFiles()) {
	        if (fileEntry.isDirectory()) {
	       	    //this is for recursive search of the directory
	            //listFilesForFolder(fileEntry);
	            //IJ.log("Skipped directory: "+fileEntry.getName());
	        } else {
	            String fileName = fileEntry.getName();
	            for (int i=0; i<fileFormats.size(); i++){
	            	if (fileName.contains(fileFormats.get(i))){
	            		if (!lifFiles.contains(fileName))
	            		{
	            		lifFiles.add(fileName);
	            		IJ.log(k.toString()+". .lif - File: "+fileName);
	            		}
	            		k++;
	            	}
	            }
	        }
	    }
	}
 
}