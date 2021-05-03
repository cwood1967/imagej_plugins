package org.stowers.microscopy;

/**
 * Created by cjw on 6/22/16.
 */

import java.nio.file.Paths;
import java.util.ArrayList;

import ij.*;
import ij.gui.MessageDialog;
import ij.io.FileInfo;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.awt.Frame;

@Plugin(type = Command.class, name = "Click for Roi",  menuPath="Plugins>Stowers>Chris>Save As Tiff")
public class TiffSaverPlugin implements Command, Previewable {

    @Parameter
    ImagePlus imp;

    private String filename;

    FileInfo fi = null;

    String name;

    @Override
    public void run() {

        String[] exts = new String[] {".tif", ".tiff"};

        String version = System.getProperty("java.version");
        int dot1 = version.indexOf(".");

        double v = Double.parseDouble(version.substring(0, dot1 + 2));
        System.out.println(dot1 + " " + v + " " + version);

        if (v < 1.7) {
            Frame f = new Frame();
            MessageDialog m = new MessageDialog(f, "Update Java", "Update your Java to 1,7 or higher");
            return;
        }

        ij.io.SaveDialog sd = null;
        try {
            sd = new ij.io.SaveDialog("Save file", imp.getTitle(), ".tif");


            if (sd == null) {
                return;
            }

            String dirname = sd.getDirectory();
            String fname = sd.getFileName();

            if (fname == null) {
                return;
            }

            boolean hasext = false;
            for (int i = 0; i < exts.length; i++) {
                if (fname.toLowerCase().endsWith(exts[i])) {
                    hasext = true;
                    break;
                }
            }

            if (!hasext) {
                fname = fname + ".tif";
            }
            filename = Paths.get(dirname, fname).toString();

            FastFileSaver f = new FastFileSaver(imp);
            f.saveAsTiff(filename);
        }
        catch (Exception e) {
            System.out.println("Couldn't save file");
            e.printStackTrace();
            return;
        }
    }

    @Override
    public void preview() {

    }

    @Override
    public void cancel() {

    }



}
