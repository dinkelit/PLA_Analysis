import ij.plugin.PlugIn;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.WaitForUserDialog;
import ij.io.OpenDialog;
import ij.WindowManager;
import java.io.File;

public class PLA_Dendrites extends ImagePlus implements PlugIn {

	// STRING FOR OS
	public static String os_system = "";
	public static String os_slash = "";

	public static String version = "0.71";

	public void run(String arg) {
		IJ.log("-----------------[ Start PLA_Dendrites V"+version+" ]-----------------");

		os_system = System.getProperty("os.name");
	    if (os_system.contains("Mac"))
	    	 os_slash = "/";
	    else os_slash = "\\";
		
		OpenDialog od = new OpenDialog("Select imagefile");
		ImagePlus imp = IJ.openImage(od.getPath());

		String path = od.getPath().substring(0,od.getPath().length()-imp.getTitle().length());
		String imageName = imp.getTitle();
		IJ.log("img: "+imageName+" path: "+path);
		
		IJ.setTool("polyline");
		imp.show();
		new WaitForUserDialog("Selection required", "1. Track the dendrite with left clicks.\n\n2. Place your last selection with right click. \n\n3. Press the OK button to continue").show();
		IJ.run("Straighten...", "title=straightened_"+imp.getTitle()+" line=20");
		imp.close();
		ImagePlus imp2 = WindowManager.getCurrentImage();
		IJ.log("img2: "+imp2.getTitle());
		
		String fullpath = createPaths(path);
		IJ.saveAs(imp2, "Tiff", fullpath+os_slash+imp2.getTitle());
		imp2.close();
		
		GenericDialog gd = new GenericDialog("Perform a PLA-analysis?");
		gd.addMessage("The straightened image will be saved in a folder 'Straightened' within this destination:\n\n"+path+"\n\nClick OK to perform a PLA-analysis on the 'Straightened' folder");
		gd.showDialog();
		if (gd.wasCanceled()) {
			IJ.run("Close All", "");
            System.gc();
			return;
		}else{
			IJ.run("Run PLA-analysis on a folder", "choose="+fullpath);
            System.gc();
            return;
		}

	}

   	public String createPaths(String path){
	    	String fullpath = path+os_slash+"Straightened"+os_slash;
	    	Boolean success = (new File(fullpath)).mkdirs();
	    	return fullpath;
    }
	
}

