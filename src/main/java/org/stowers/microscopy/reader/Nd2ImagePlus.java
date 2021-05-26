package org.stowers.microscopy.reader;


import com.drew.imaging.ImageProcessingException;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import loci.formats.FormatException;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

public class Nd2ImagePlus {

    private static String strXpos = "dXPos";
    private static String strYpos = "dYPos";
    private static String strZpos = "dZPos";
    private static String strZstep = "dZStep";
    SimrND2Reader reader;
    int nz;
    int nt;
    int nc;
    int pnz; // number of z considering projection
    int w;
    int h;
    int bpp;
    int nseries;

    boolean showProgress = false;
    private byte[] buf;
    ImagePlus imp = null;
    ImageStack stack = null;

    String projection;
    boolean is_proj;

    public Nd2ImagePlus(String imagefilename, String projection) {

        this.projection = projection;
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
        nseries = reader.getSeriesCount();
        bpp = reader.getBitsPerPixel()/8;

        if (projection.equals("None")) {
            pnz = nz;
            this.is_proj = false;
        } else {
            pnz = 1;
            this.is_proj = true;
        }
    }


    public ImageStack readStack() {

        int size = nt * pnz * nc;
        stack = new ImageStack(w, h, size);

        int pindex = 0;
        buf = new byte[bpp*nc*w*h];
        int pjz;

        for (int jt = 0; jt < nt; jt++) {

            float[][] pStack = new float[nc][w*h];
            for (int jz = 0; jz < nz; jz++) {
                byte[] rawplane = readPlane(jt, jz);
                short[][] cplane = unInterLeave(rawplane);



                if (!is_proj) {
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
                } else {
                    pStack = projPlane(cplane, pStack);
                }
            }

            if (is_proj) {
                int stack_index = jt * nc;
                for (int jc = 0; jc < nc; jc++) {
                    ImageProcessor _ip = new FloatProcessor(w, h);
                    _ip.setPixels(pStack[jc]);
                    stack.setProcessor(_ip, stack_index + jc + 1);
                }
            }
        }
        return stack;
    }

    private float[][] projPlane(short[][] cplane, float[][] projStack) {

        for (int i = 0; i < cplane.length; i++) {
            short[] v = cplane[i];
            for (int j = 0; j < v.length; j++) {
                if (projection.equals("MAX")) {
                    if (cplane[i][j] > projStack[i][j]) {
                        projStack[i][j] = cplane[i][j];
                    }
                } else {
                    projStack[i][j] += cplane[i][j];
                }
            }
        }
        return projStack;
    }

    public ImagePlus getImagePlus(boolean showProgress, int series) {

        this.showProgress = showProgress;
        reader.setSeries(series);
        //CoreMetadata core = reader.getCore(series);
        File cf = new File(reader.getCurrentFile());
        System.out.println(cf.getName());
        imp = IJ.createHyperStack(cf.getName(),
                w, h, nc, pnz, nt, 16);
        imp.setStack(cf.getName(), readStack());


        Calibration cal = new Calibration();
        cal.setUnit("micron");
        cal.pixelWidth = reader.getTrueSizeX();
        cal.pixelHeight = reader.getTrueSizeY();
        cal.pixelDepth = reader.getTrueSizeZ();
        imp.setCalibration(cal);

        Hashtable<String, Object> meta = reader.getGlobalMetadata();

        String xp = meta.get(strXpos).toString();
        String yp = meta.get(strYpos).toString();
        String zp = meta.get(strZpos).toString();
        String zstep = "None";
        if (meta.contains(strZstep)) {
            zstep = (String) meta.get(strZstep).toString();
        }
        imp.setProp(strXpos, xp);
        imp.setProp(strYpos, yp);
        imp.setProp(strZpos, zp);
        imp.setProp(strZstep, zstep);
//        for (String key : meta.keySet()) {
//            Object obj = meta.get(key);
//            imp.setProp(key, obj.toString());
//        }
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

    public int getSeriesCount() {
        return reader.getSeriesCount();
    }

    public void close() {
        try {
            reader.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
