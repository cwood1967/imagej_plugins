package org.stowers.microscopy;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.stowers.microscopy.reader.SimrND2Reader;
import org.stowers.microscopy.reader.Nd2ImagePlus;
import java.io.File;

@Plugin(type = Command.class, name = "SIMR Nd2Reader",  menuPath="Plugins>Stowers>Chris>SIMR Nd2 Reader")
public class Nd2ReaderPlugin implements Previewable, Command {

    @Parameter()
    File imagefile;

    @Override
    public void run() {
        System.out.println(imagefile.getAbsolutePath());
        Nd2ImagePlus nd2 = new Nd2ImagePlus(imagefile.getAbsolutePath());
        nd2.getImagePlus(true).show();
    }

    @Override
    public void preview() {

    }

    @Override
    public void cancel() {

    }
}
