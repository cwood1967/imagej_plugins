package org.stowers.microscopy.utils;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

public class YeastBestZ {
   
    int nc;
    int nz;
    int nt;
    int width;
    int height;

    ImagePlus imp = null;
    ImageStack stack = null;

    public YeastBestZ(ImagePlus imp) {
        this.imp = imp;

        nc = imp.getNChannels();
        nz = imp.getNSlices(); // the number of z slices
        nt = imp.getNFrames(); //the number of time frames
        width = imp.getWidth();
        height = imp.getHeight();
        stack = imp.getImageStack();

    }
   
    public ImageStack process(int channel) {
        ImageStack best_stack = new ImageStack(width, height, nt);
       
        for (int i=1; i <=nt; i++) {
            int zb = calcFrameBest(channel, i);            
            int zb_slice = imp.getStackIndex(channel, zb, i);
            ImageProcessor zimp = stack.getProcessor(zb_slice);
            best_stack.setProcessor(zimp, i);
            
        }    
        return best_stack;
    }
    // remember imagej is sometimes 1 based (most of the time)
    public int calcFrameBest(int channel, int frame) {
       
        double[] zprofile = new double[nz];

        for (int i = 1; i <= nz; i++) {
            int index = imp.getStackIndex(channel, i, frame);
            ImageProcessor ip = stack.getProcessor(index).duplicate();
            ip.blurGaussian(1.);
            ip.findEdges();
            double mx = ip.getStatistics().mean;
            zprofile[i - 1] = mx;

        }

        double[] d2 = deriv(deriv(zprofile));

        double dmax = -Double.MAX_VALUE;
        int argmax = 0;
        // int imax = 0;
        for (int i = 0; i < d2.length; i++) {
            if (d2[i] > dmax) {
                dmax = d2[i];
                argmax = i + 1; //imp.getStackIndex(channel, i + 1, frame);
                // imax = i + 1;
            }
        }
        System.out.println("Best Z for frame i:  " + argmax);
        return argmax;
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
}
