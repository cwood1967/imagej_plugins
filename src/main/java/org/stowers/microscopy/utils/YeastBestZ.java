package org.stowers.microscopy.utils;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.LutLoader;
import ij.process.ImageProcessor;
import ij.plugin.ContrastEnhancer;
import ij.process.FloatProcessor;
import ij.process.LUT;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.stowers.microscopy.reader.Nd2ImagePlus;

import java.awt.*;
import java.io.File;

import ij.gui.GenericDialog;

@Plugin(type = Command.class, name = "Yeast Best Z",  menuPath="Plugins>Stowers>Chris>Yeast Best Z")
public class YeastBestZ implements Previewable, Command {

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
        ImageStack stack = imp.getStack();
        process();
    }

    protected void process() {

        int nc = imp.getNChannels();
        int nz = imp.getNSlices(); // the number of z slices

        ImageStack stack = imp.getStack();
        ImageStack pstack = new ImageStack(imp.getWidth(), imp.getHeight(), nz);

        double[] zprofile = new double[nz];
        ImagePlus pimp = new ImagePlus();

        for (int i = 1; i <= nz; i++) {
            int index = imp.getStackIndex(transmitted_channel, i, 1);
            ImageProcessor ip = stack.getProcessor(index).duplicate();
            ip.blurGaussian(1.);
            ip.findEdges();
            double mx = ip.getStatistics().mean;
            zprofile[i - 1] = mx;

        }

        double[] d2 = deriv(deriv(zprofile));

        double dmax = -Double.MAX_VALUE;
        int argmax = 0;
        int imax = 0;
        for (int i = 0; i < d2.length; i++) {
            if (d2[i] > dmax) {
                dmax = d2[i];
                argmax = imp.getStackIndex(transmitted_channel, i + 1, 1);
                imax = i + 1;
            }
        }
        pimp.setProcessor(stack.getProcessor(argmax));
        pimp.setLut(LUT.createLutFromColor(Color.gray));
        pimp.setTitle(imp.getTitle() + "-" + "BestZ" + "-" + imax);
        ce.setUseStackHistogram(false);
        ce.setNormalize(false);
        ce.stretchHistogram(pimp, 0.35);
        pimp.show();



    }

    protected double[] deriv(double[] profile) {

        double[] d = new double[profile.length];
        for (int i = 0; i < profile.length; i++) {

            double f0;
            double f2;

            if (i == 0) {
                f0 = profile[0];
            } else
                f0 = profile[i - 1];

            if (i == profile.length - 1) {
                f2 = profile[profile.length - 1];
            } else {
                f2 = profile[i + 1];
            }

            d[i] = (f2 - f0)/2.;
        }

        return d;
    }

    @Override
    public void preview() {

    }

    @Override
    public void cancel() {

    }
}
