package org.stowers.microscopy.converter;

import ij.IJ;
import ij.ImagePlus;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.*;
import loci.formats.meta.IMetadata;
import loci.formats.meta.MetadataRetrieve;
import loci.formats.meta.MetadataStore;
import loci.formats.out.OMETiffWriter;
import loci.formats.services.OMEXMLService;
import loci.formats.tiff.TiffSaver;
import loci.plugins.BF;
import loci.plugins.in.ImporterOptions;
import ome.xml.model.primitives.PositiveInteger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import org.stowers.microscopy.ij1plugins.FastFileSaver;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;



public class Mvd2Converter {

    IFormatReader reader = null;
    String filename;

    IMetadata metadata;

    int numSeries;
    String saveToDir;

    ArrayList<String> usedNames;
    int nameCounter = 0;


//    private static final Logger LOGGER = LoggerFactory.getLogger(TiffSaver.class);

    public Mvd2Converter(String filename, String saveToDir) {
        this.filename = filename;
        this.saveToDir = saveToDir;

        try {
            createReader();
            usedNames = new ArrayList<>();
        }
        catch (Exception e) {
            e.printStackTrace();
            reader = null;
        }
    }

    public void close() {
        try {
            reader.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void createReader() throws Exception {

        reader = new ImageReader();

        IMetadata metadata;
        try {

            ServiceFactory factory = new ServiceFactory();
            OMEXMLService xmlService = factory.getInstance(OMEXMLService.class);
            metadata = xmlService.createOMEXMLMetadata();

        }
        catch (DependencyException e) {
            throw new FormatException("Could not create OME-XML store.", e);
        }
        catch (ServiceException e) {
            throw new FormatException("Could not create OME-XML store.", e);
        }

        reader.setMetadataStore(metadata);


        try {
            reader.setId(filename);
            numSeries = reader.getSeriesCount();
        }
        catch (IOException e) {
            throw new FormatException("IO Problem", e);
        }

    }

    public void listSeries() {
        int n = reader.getSeriesCount();
        List<CoreMetadata> mlist = reader.getCoreMetadataList();

        for (int i = 0; i < n; i++) {
            reader.setSeries(i);
            System.out.println("");
            System.out.println(reader.getSeriesMetadataValue("Name"));
        }
    }

    public OMETiffWriter createWriter(int series, String seriesName, String sPixType, int nc, int nz, int nt)
            throws Exception {
        reader.setSeries(series);
        int x = reader.getSizeX();
        int y = reader.getSizeY();
        int z = reader.getSizeZ();
        int c = reader.getSizeC();
        int t = reader.getSizeT();

        String dimOrder  = reader.getDimensionOrder();
        int pixType = reader.getPixelType();

        String sType;
        if (sPixType == null) {
            sType = FormatTools.getPixelTypeString(pixType);
        }
        else {
            sType = sPixType;
        }

        OMETiffWriter writer = new OMETiffWriter();
        PositiveInteger px = new PositiveInteger(x);
        PositiveInteger py = new PositiveInteger(y);
        reader.getMetadataStore().setPixelsSizeX(px, series);
        reader.getMetadataStore().setPixelsSizeY(py, series);

        MetadataRetrieve store = (MetadataRetrieve)reader.getMetadataStore();

        IMetadata meta;
        ServiceFactory factory = new ServiceFactory();
        OMEXMLService xmlService = factory.getInstance(OMEXMLService.class);
        meta = xmlService.createOMEXMLMetadata();

        MetadataTools.populateMetadata(meta,
                0,
                seriesName,
                true,
                dimOrder,
                sType,
                x, y, nz, nc, nt, 1);

        meta.setPixelsPhysicalSizeX(store.getPixelsPhysicalSizeX(series), 0);
        meta.setPixelsPhysicalSizeY(store.getPixelsPhysicalSizeY(series), 0);
        writer.setMetadataRetrieve(meta);
//        writer.setMetadataRetrieve(store);

        writer.setBigTiff(true);
        writer.setId(seriesName);
        writer.setCompression("Uncompressed");
        writer.setWriteSequentially(true);
        return writer;
    }


    public void convertAllSeries() {

        for (int i = 0; i < numSeries; i++) {

            convertSeries(i, saveToDir, 1);
        }
    }

    public void convertAllSeries(int mod) {

        for (int i = 0; i < numSeries; i++) {

            convertSeries(i, saveToDir, mod);
        }

        IJ.log("Done!");
    }

    public void convertSeries(int series) {
        convertSeries(series, saveToDir, 1);
    }

    public void sumProjectSeries(int series, String seriesDir, int channel) {

        float[] res;
        reader.setSeries(series);
        String seriesName = (String)reader.getSeriesMetadata().get("Name");
        seriesName = "SUM-" + channel + "-" + seriesName.replace(" ", "-");
        seriesName =  String.format ("%s/%05d-%s.ome.tiff", seriesDir, series, seriesName);

        OMETiffWriter writer = null;
        try {
            writer = createWriter(series, seriesName,  "float", 1,
                    reader.getSizeZ(), reader.getSizeT());
        }
        catch (Exception e) {
            System.err.println("********* Could not create OMETiffWriter");
            e.printStackTrace();
            return;
        }


        int nc = reader.getSizeC();
        int nz = reader.getSizeZ();
        int nt = reader.getSizeT();


        int cc;
        int cz;
        int ct;

        int nslices = nc*nz*nt;

        int nslice = 0;
        int isave = 0;

        try {
            for (int it = 0; it < reader.getSizeT(); it++) {
                System.out.println("Time-" + it);
                res = new float[reader.getSizeX()*reader.getSizeY()];
                byte[] bb = new byte[4*res.length];
                for (int iz = 0; iz < reader.getSizeZ(); iz++) {
                    System.out.println("Z-" + iz);
                    for (int ic = 0; ic < reader.getSizeC(); ic ++) {
                        if (ic == channel) {
                            byte[] b = reader.openBytes(nslice);
                            int bk = 0;
                            for (int j = 0; j < res.length; j++) {
                                byte[] bz = {b[bk], b[bk + 1], 0, 0};
                                float f = ByteBuffer.wrap(bz).order(ByteOrder.LITTLE_ENDIAN).getInt();
                                res[j] += f;
                                bk += 2;
                            }
                        }
                        nslice++;
                    }

                    System.out.println(bb.length + " " + it + " " + iz);

                }

                int bx = 0;
                for (int ir = 0; ir < res.length; ir++) {
                    byte[] ba = ByteBuffer.allocate(4).putFloat(res[ir]).array();
                    bb[bx] = ba[3];
                    bb[bx + 1] = ba[2];
                    bb[bx + 2] = ba[1];
                    bb[bx + 3] = ba[0];
                    bx += 4;
                }

                writer.saveBytes(isave, bb);
                isave++;
            }

            writer.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
//        return res;
    }

    public void convertSeries(int series, String seriesDir, int mod) {


        reader.setSeries(series);

        MetadataRetrieve store = (MetadataRetrieve)reader.getMetadataStore();
        String seriesName = store.getImageName(series);
        String id = store.getImageID(series);

        String seriesId = (String)reader.getSeriesMetadata().get("id");

        if (seriesName == null) {
            String slash = File.separator;
            String[] sp = filename.split(slash);
            seriesName = sp[sp.length - 1];
        }
        seriesName = seriesName.replace(" ", "_");
        seriesName = seriesName.replace("/", "-");
        String seriesFileName =  String.format ("%s/%05d-%s.tiff", seriesDir, nameCounter, seriesName);


        if (usedNames.contains(seriesFileName)) {
            nameCounter++;
            seriesFileName =  String.format ("%s/%05d-%s.tiff", seriesDir, nameCounter, seriesName);
            usedNames.add(seriesFileName);
        } else {
            usedNames.add(seriesFileName);
        }

        IJ.log(id + " " + seriesFileName);
        ImagePlus imp = openSeries(series, false);

        FastFileSaver saver = new FastFileSaver(imp);
        saver.saveAsTiff(seriesFileName);

        System.out.println("Saved fast???? " + seriesFileName);

    }

    protected ImagePlus openSeries(int series, boolean virtual) {

        ImagePlus resImp = null;

        try {
            ImporterOptions options = new ImporterOptions();
            options.setId(filename);
            options.setVirtual(virtual);

            for (int i = 0; i < numSeries; i++) {
                options.setSeriesOn(i, false);
            }
            options.setSeriesOn(series, true);
            options.setWindowless(true);
            ImagePlus[] x = BF.openImagePlus(options);
            resImp = x[0];
        }

        catch (Exception e) {
            e.printStackTrace();
            resImp = null;
        }

        return resImp;
    }
    public void saveVirtualStack(ImagePlus imp, String name) {

        String options = "save=" + name + " export compression=Uncompressed";
        IJ.run(imp, "Bio-Formats Exporter", options);

    }

    public static void main(String[] args) {

        if (1 == 0) {

            float f = 1.44f;
            int z = Float.floatToIntBits(f);

            System.out.println((z >>> 24) + " " + (byte)(z >>> 24));
            System.out.println((z >>> 16) + " " +(byte)(z >>> 16));
            System.out.println((z >>> 8) +  " " + (byte)(z >>> 8));
            System.out.println((z >>> 0) + " " + (byte)(z >>> 0));

            byte[] ba = ByteBuffer.allocate(4).putFloat(f).array();

            return;
        }
//        String filename = "/Volumes/projects/jjl/public/TSK/05102016 TSK 3/05102016 TSK 3.mvd2";
//        String filename = "/Users/cjw/DataTemp/Justons DNA damage plate/Justons DNA damage plate.mvd2";
        String filename =
        "/Volumes/projects/jjl/public/TSK/09142016 TSK ASC fiber formation 2/09142016 TSK ASC fiber formation 2.mvd2";
        Mvd2Converter r = new Mvd2Converter(filename, "/Users/cjw/Desktop/JJ" );

        try {
            r.createReader();
            r.listSeries();
            r.sumProjectSeries(1,  "/Users/cjw/Desktop/JJ/Sum", 0);
//            r.convertSeries(1, "/Users/cjw/Desktop/JJ", 62);
//            r.convertAllSeries();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

//        ImagePlus imp = r.getVirtualStack(1);
//        r.saveVirtualStack(imp, "/Users/cjw/Desktop/testsave.ome.btf");
    }


}
