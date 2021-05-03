package org.stowers.microscopy.converter;

import ij.ImagePlus;
import ij.plugin.ZProjector;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import loci.plugins.BF;
import org.stowers.microscopy.FastFileSaver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by cjw on 4/26/17.
 */

@Plugin(type = Command.class, name = "Batch Z-Project",  menuPath="Plugins>Stowers>Chris>Batch Project")
public class BatchProjectPlugin implements Command, Previewable {

    @Parameter(label = "Select a directory to project", style="directory")
    File inputDir;

    String inputDirString;

    @Parameter(label="Select a directory to save to", style="directory")
    File saveToDir;

    String saveToDirString;

    @Parameter(label="Select a method", choices={"MAX","SUM","AVERAGE"})
    String method;

    List<String> goodExt;

    int zMethod = -1;
    @Override
    public void run() {

        if (method.equals("MAX")) {
            zMethod = ZProjector.MAX_METHOD;
        } else if (method.equals("SUM")) {
            zMethod = ZProjector.SUM_METHOD;
        } else if (method.equals("AVERAGE")) {
            zMethod = ZProjector.AVG_METHOD;
        } else {
            zMethod = ZProjector.SUM_METHOD;
        }

        inputDirString = inputDir.getAbsolutePath();
        if (!inputDirString.endsWith("/")) {
            inputDirString += "/";
        }

        saveToDirString = saveToDir.getAbsolutePath();
        if (!saveToDirString.endsWith("/")) {
            saveToDirString += "/";
        }

        goodExt = new ArrayList<>();
        goodExt.add("tif");
        goodExt.add("tiff");
        goodExt.add("nd2");
        goodExt.add("dv");
        goodExt.add("lsm");
        goodExt.add("cvi");

        String inputname = inputDir.getAbsolutePath();
        String savename = saveToDir.getAbsolutePath();

        String[] filenames = inputDir.list();

        for (int i = 0; i < filenames.length; i++) {
            String f = filenames[i];
            String ext = f.substring(f.lastIndexOf(".") + 1, f.length());
            if (goodExt.contains(ext)) {
                System.out.println(f);
                projectFile(f);
            }
        }

    }


    protected void projectFile(String filename) {

        String projectedFileName = newFileName(filename);
        String f = inputDirString + filename;
        ImagePlus[] imps = null;
        ImagePlus imp = null;
        try {
            imps = BF.openImagePlus(f);
            imp = imps[0];
        }
        catch(Exception e) {
            e.printStackTrace();
            return;
        }

        int nc = imp.getNChannels();
        int nt = imp.getNFrames();
        int nz = imp.getStackSize()/nc/nt;

        ZProjector zproj = new ZProjector(imp);
        zproj.setStartSlice(1);
        zproj.setStopSlice(nz);
        zproj.setMethod(zMethod);
        zproj.doHyperStackProjection(true);
        ImagePlus projected = zproj.getProjection();

        FastFileSaver saver = new FastFileSaver(projected);
        saver.saveAsTiff(projectedFileName);
        return;
    }

    protected String newFileName(String filename) {

        String res = saveToDirString + method + "_" + filename + ".tif";
        return res;
    }

    @Override
    public void preview() {

    }

    @Override
    public void cancel() {

    }
}
