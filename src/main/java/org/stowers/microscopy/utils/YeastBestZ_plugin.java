package org.stowers.microscopy.utils;

import ij.IJ;
import ij.ImagePlus;
import ij.CompositeImage;
import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.process.LUT;
import ij.plugin.ContrastEnhancer;
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

    @Parameter(label="Choose projection", choices={"MAX", "SUM", "MEAN"},
                  style="listbox")
    String projection;

    //ImageStack stack = null;
    @Override
    public void run() {
        imp.setC(transmitted_channel);
        process();
    }

    protected void process() {
        ContrastEnhancer ce = new ContrastEnhancer();
        int nc = imp.getNChannels();
        int nz = imp.getNSlices(); // the number of z slices
        int nt = imp.getNFrames(); //the number of time frames
        int h = imp.getHeight();
        int w = imp.getWidth();
        int bpp = imp.getBitDepth();
        
        YeastBestZ ybz = new YeastBestZ(imp);

        CompositeImage bestImp = (CompositeImage)ybz.process(transmitted_channel,
                      projection);
        
        // ImageProcessor ipx = bestImp.getStack().getProcessor(1);
        bestImp.setPosition(1, 1, 1);
        // bestImp.setProcessor(ipx);
        String title = imp.getTitle() + "_bestZ";
        bestImp.setTitle(title);
       
        CompositeImage cimp = (CompositeImage)imp;

        for (int c=0; c<nc; c++) {
            bestImp.setC(c + 1);
            cimp.setC(c + 1);
            bestImp.setChannelLut(cimp.getChannelLut());
            ImageProcessor cip = bestImp.getChannelProcessor();
            double cmin = cip.getStatistics().min;
            double cmax = cip.getStatistics().max;
            
            bestImp.getChannelLut().min = cmin;
            bestImp.getChannelLut().max = cmax;
            bestImp.getChannelProcessor().resetMinAndMax();
        }

        bestImp.setProperty("Info", imp.getInfoProperty());
        bestImp.setCalibration(imp.getCalibration());
        
        bestImp.show();
        for (int c=1; c<=nc; c++) {
            bestImp.setC(c);
            bestImp.resetDisplayRange();
            bestImp.updateAndRepaintWindow();
        }
        bestImp.setC(1);
        System.out.println(bestImp.getProcessor().getPixel(56, 34));
        System.out.println(bestImp.getStack().getProcessor(1).getPixel(56, 34));
    }

    @Override
    public void preview() {
    }

    @Override
    public void cancel() {

    }
}
