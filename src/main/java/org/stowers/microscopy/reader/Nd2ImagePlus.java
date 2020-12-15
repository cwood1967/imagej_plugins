package org.stowers.microscopy.reader;


import com.drew.imaging.ImageProcessingException;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import io.scif.img.IO;
import loci.formats.FormatException;
import org.jetbrains.annotations.NotNull;

import java.awt.image.ImageProducer;
import java.io.IOException;
import java.util.Hashtable;

public class Nd2ImagePlus {

    SimrND2Reader reader;
    int nz;
    int nt;
    int nc;
    int w;
    int h;
    int bpp;

    boolean showProgress = false;
    private byte[] buf;
    ImagePlus imp = null;
    ImageStack stack = null;
    public Nd2ImagePlus(String imagefilename) {

        reader = new SimrND2Reader();
        try {
            reader.initFile(imagefilename);
        }
        catch (FormatException e) {
            e.printStackTrace();
            return;
        }
        catch (IOException e) {
            e.printStackTrace();
            return;
        }
        nz = reader.getSizeZ();
        nt = reader.getSizeT();
        nc = reader.getSizeC();
        w = reader.getSizeX();
        h = reader.getSizeY();

        bpp = reader.getBitsPerPixel()/8;
    }


    public ImageStack readStack() {

        int size = nt * nz * nc;
        stack = new ImageStack(w, h, size);

        int pindex = 0;
        buf = new byte[bpp*nc*w*h];
        for (int jt = 0; jt < nt; jt++) {
            for (int jz = 0; jz < nz; jz++) {
                byte[] rawplane = readPlane(jt, jz);
                //short[] ivplane = bytesToShort(rawplane);
                short[][] cplane = unInterLeave(rawplane);

                int stack_index = jt * nz * nc + jz * nc;
                for (int jc = 0; jc < nc; jc++) {
                    ImageProcessor _ip = new ShortProcessor(w, h);
                    _ip.setPixels(cplane[jc]);
                    stack.setProcessor(_ip, stack_index + jc + 1);
                    //System.out.print(jc + " " + (stack_index + jc + 1));
                    //System.out.println(" " + jt + " " + jz + " " + stack_index);
                    if (showProgress) {
                        IJ.showProgress(pindex, size);
                        pindex++;
                    }
                }
            }
        }
        return stack;
    }

    public ImagePlus getImagePlus(boolean showProgress) {

        this.showProgress = showProgress;
        imp = IJ.createHyperStack(reader.getCurrentFile(),
                w, h, nc, nz, nt, 16);
        imp.setStack(reader.getCurrentFile(), readStack());


        Calibration cal = new Calibration();
        cal.setUnit("micron");
        cal.pixelWidth = reader.getTrueSizeX();
        cal.pixelHeight = reader.getTrueSizeY();
        cal.pixelDepth = reader.getTrueSizeZ();
        imp.setCalibration(cal);

        try {
            reader.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return imp;

    }

    private byte[] readPlane(int t, int z) {
        // use nc* because thats what bio-formats wants
        int no = nc*(t*nz + z);
        byte[] plane;
        try {
            plane = reader.openBytes(no, buf, 0, 0, w, h);
        }
        catch (FormatException e) {
            e.printStackTrace();
            return null;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return plane;
    }

    protected short bytesToShort(byte b1, byte b2) {

        int res1 = (b1 & 0x00ff);
        int res2 = ((b2 & 0x00ff) << 8);
        short res = (short)(res2 | res1);

        return res;
    }

    protected short[][] unInterLeave(@NotNull byte[] bytes) {
        short[][] res = new short[nc][w*h];

        int inc = bpp*nc;
        int channel;
        int index;
        int ka;
        int res1;
        int res2;
        short pixel;

        for (int kp = 0; kp < bytes.length; kp += inc) {
            for (int kc = 0; kc < inc; kc += bpp) {
                channel = kc/bpp;
                index = kp/inc;
                ka = kp + kc;
                //pixel = bytesToShort(bytes[ka], bytes[ka + 1]);
                res1 = (bytes[ka] & 0x00ff);
                res2 = ((bytes[ka + 1] & 0x00ff) << 8);
                pixel = (short)(res2 | res1);
                res[channel][index] = pixel;
            }
        }

        return res;
    }
}
