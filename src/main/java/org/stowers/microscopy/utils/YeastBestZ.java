package org.stowers.microscopy.utils;

import ij.IJ;
import ij.ImagePlus;
import ij.CompositeImage;
import ij.ImageStack;
import ij.process.FloatProcessor;
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
   
    public ImagePlus process(int channel, String projection) {
        ImagePlus best_imp = IJ.createHyperStack("Best", width, height, nc, 1,
                                nt, 32);

        ImageStack best_stack = best_imp.getStack(); 
        
        for (int i=1; i <=nt; i++) {
            for (int c=1; c<=nc; c++) {
                 if (c == channel) {
                    int zb = calcFrameBest(channel, i);            
                    int zb_slice = imp.getStackIndex(channel, zb, i);
                    ImageProcessor zip = stack.getProcessor(zb_slice);
                    zip.setMinAndMax(zip.getStatistics().min, zip.getStatistics().max);
                    int slice_in_best = best_imp.getStackIndex(channel, 1, i);
                    best_stack.setProcessor(zip, slice_in_best);
                }    
                else {
                    ImageProcessor pip = project(c, i, projection);
                    int proj_index = best_imp.getStackIndex(c, 1, i);
                    best_stack.setProcessor(pip, proj_index);
                }
            }
        }

        best_imp.setStack(best_stack);
        return best_imp;
    }
    

    public ImageProcessor project(int c, int t, String projection) {

        float[] jpixels = new float[width*height];
        for (int z=1 ; z<=nz; z++) {
            int index = imp.getStackIndex(c, z, t);
            float[] fpix = (float[])imp.getStack().getProcessor(index).convertToFloatProcessor().getPixels();
            
            for (int j=0; j<fpix.length; j++) {
                if (projection.equals("MAX")) {
                    if (fpix[j] > jpixels[j]) {
                        jpixels[j] = fpix[j];
                    }
                }
                else {
                    jpixels[j] += fpix[j];
                }
            }
            
        }
        if (projection.equals("MEAN")) {
            for (int i=0; i < jpixels.length; i++) {
                jpixels[i] /= nz;
            }
        }
        
        FloatProcessor fip = new FloatProcessor(width, height, jpixels); 
        fip.setMinAndMax(fip.getStatistics().min, fip.getStatistics().max);
        return fip;
    }

    // remember imagej is sometimes 1 based (most of the time)
    public int calcFrameBest(int channel, int frame) {
       
        double[] zprofile = new double[nz];

        for (int i = 1; i <= nz; i++) {
            int index = imp.getStackIndex(channel, i, frame);
            FloatProcessor _ip = stack.getProcessor(index).convertToFloatProcessor();
            FloatProcessor ip = (FloatProcessor)_ip.duplicate();
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
        System.out.println("Best Z for frame " + frame + ":  " + argmax);
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
