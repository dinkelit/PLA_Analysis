import ij.plugin.PlugIn;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.WaitForUserDialog;
import ij.io.OpenDialog;
import ij.WindowManager;
import ij.measure.Calibration;
import java.io.File;

public class PLA_Soma extends ImagePlus implements PlugIn {

	// STRING FOR OS
	public static String os_system = "";
	public static String os_slash = "";

	public static String version = "0.85";

	public void run(String arg) {
		IJ.log("-----------------[ Start PLA_Soma V"+version+" ]-----------------");

		os_system = System.getProperty("os.name");
	    if (os_system.contains("Mac"))
	    	 os_slash = "/";
	    else os_slash = "\\";
		
		OpenDialog od = new OpenDialog("Select imagefile");
		ImagePlus imp = IJ.openImage(od.getPath());

		String path = od.getPath().substring(0,od.getPath().length()-imp.getTitle().length());
		String imageName = imp.getTitle();
		IJ.log("img: "+imageName+" path: "+path);

		// GET MEASUREMENTS (PIXLE/MICRONS)
		Calibration cal = imp.getCalibration();
		double x = cal.pixelWidth;
		double y = cal.pixelHeight;
        
        IJ.run(imp, "Select All", "");
        Integer oldWidth = (int)imp.getRoi().getFloatWidth();
		Integer oldHeight = (int)imp.getRoi().getFloatHeight();
        double distance = oldHeight/(oldHeight*x);
        
        IJ.run(imp, "Select None", "");
		IJ.log("Measurements: "+x+" x "+y+" | "+oldWidth+" x "+oldHeight+" = "+(oldHeight*x)+" => distance: "+oldHeight/(oldHeight*x));
		
        // IJ.run(imp, "Set Scale...", "distance=5.5494 known=1 pixel=1 unit=micron");
        
		IJ.setTool("brush");
		imp.show();
		new WaitForUserDialog("Selection required", "1. Hold the left mouse button down and drag the brush over the soma.\n\n2. Areas can be erased by clicking with the brush tool outside the marked area and drag it on the zone which shall be removed. \n\n3. Press the OK button to continue").show();
		IJ.run("Copy", "");

		Integer newWidth = (int)imp.getRoi().getFloatWidth();
		Integer newHeight = (int)imp.getRoi().getFloatHeight();
		
		// Add 1/3 of height and width to the borders to make sure that the cell mass is less than the background
		newHeight += 2*(int)(newHeight/3);
		newWidth  += 2*(int)(newWidth/3);
		
		Integer bitDepth = imp.getBitDepth();
		IJ.log("NewFile: "+newWidth+" x "+newHeight+" bitD: "+bitDepth);
		imp.close();
		ImagePlus imp2 = IJ.createImage("brushed_"+imageName, bitDepth+"-bit black", newWidth, newHeight, 1);
		IJ.run(imp2, "Paste", "");
		IJ.run(imp2, "Select None", "");
		imp2.show();
		String fullpath = createPaths(path);
        IJ.run(imp2, "Set Scale...", "distance="+distance+" known=1 pixel=1 unit=micron");
		IJ.saveAs(imp2, "Tiff", fullpath+os_slash+imp2.getTitle());

		//IJ.run("Close All", "");
        System.gc();
		return;

	}

   	public String createPaths(String path){
	    	String fullpath = path+os_slash+"Brushed"+os_slash;
	    	Boolean success = (new File(fullpath)).mkdirs();
	    	return fullpath;
    }
	
}

