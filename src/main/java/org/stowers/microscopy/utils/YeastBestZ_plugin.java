package org.stowers.microscopy.utils;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.ContrastEnhancer;
import ij.process.LUT;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.awt.*;

@Plugin(type = Command.class, name = "Yeast Best Z",
        menuPath="Plugins>Stowers>Chris>Yeast Best Z")
public class YeastBestZ_plugin implements Previewable, Command {

    @Parameter(label="Transmitted Light Channel")
    int transmitted_channel = 3;

    @Parameter
    ImagePlus imp;

    ContrastEnhancer ce = new ContrastEnhancer();
    //ImageStack stack = null;
    @Override
    public void run() {
        System.out.println(transmitted_channel);
        imp.setC(transmitted_channel);
        process();
    }

    protected void process() {

        int nc = imp.getNChannels();
        int nz = imp.getNSlices(); // the number of z slices
        int nt = imp.getNFrames(); //the number of time frames
        int h = imp.getHeight();
        int w = imp.getWidth();
        int bpp = imp.getBitDepth();
        
        YeastBestZ ybz = new YeastBestZ(imp);

        ImageStack bestStack = ybz.process(transmitted_channel);
       
        String title = imp.getTitle() + "_bestZ";
        ImagePlus pimp = IJ.createHyperStack(title, w, h, nc, nz, nt, bpp); 

        pimp.setStack(bestStack);
        pimp.setLut(LUT.createLutFromColor(Color.gray));
        // pimp.setTitle(imp.getTitle() + "-" + "BestZ" + "-" + imax);
        ce.setUseStackHistogram(true);
        ce.setNormalize(false);
        ce.stretchHistogram(pimp, 0.35);
        pimp.show();
    }

    @Override
    public void preview() {
    }

    @Override
    public void cancel() {

    }
}
