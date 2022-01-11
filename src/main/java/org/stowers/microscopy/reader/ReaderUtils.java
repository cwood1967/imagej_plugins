package org.stowers.microscopy.reader;

import net.imglib2.ops.parse.token.Int;

public class ReaderUtils {

    public static String Nd2Series(String filename) {
        Nd2ImagePlus nd2 = new Nd2ImagePlus(filename, "None");
        int n = nd2.getSeriesCount();
        String sn = Integer.toString(n);
        return sn;
    }
}
