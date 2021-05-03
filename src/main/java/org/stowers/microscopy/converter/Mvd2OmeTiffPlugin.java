package org.stowers.microscopy.converter;

import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;


/**
 * Created by cjw on 9/16/16.
 */

@Plugin(type = Command.class, name = "Click for Roi",  menuPath="Plugins>Stowers>Chris>Convert Mvd2 to OME Tiff")
public class Mvd2OmeTiffPlugin implements Command, Previewable {

    @Parameter(label = "Select an Mvd2 file to convert")
    File mvd2File;

    @Parameter(label="Select a directory to save to", style="directory")
    File saveToDir;

//    @Parameter(label="Series number")
//    int series;

    @Override
    public void run() {

        String f1 = mvd2File.getAbsolutePath();
        String d1 = saveToDir.getAbsolutePath();
        Mvd2Converter m = new Mvd2Converter(f1, d1);
        m.convertAllSeries();
        m.close();
        m = null;
    }

    @Override
    public void preview() {

    }

    @Override
    public void cancel() {

    }

    public static void main(String[] args) {

        final ImageJ imagej = net.imagej.Main.launch(args);

    }
}
