package org.stowers.microscopy;

import ij.IJ;
import ij.ImagePlus;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.stowers.microscopy.reader.Nd2ImagePlus;
import java.io.File;

@Plugin(type = Command.class, name = "SIMR Nd2 Projection",
        menuPath="Plugins>Stowers>Chris>SIMR Nd2 Projection")
public class Nd2ReaderProjectionPlugin extends Nd2ReaderPlugin {

    @Parameter(label="Projection Type", choices={"None", "MAX", "SUM"})
    String projection_choice = "None";

    public Nd2ReaderProjectionPlugin() {
        super();
        super.projection_choice = this.projection_choice;
    }

    @Override
    public void run() {
        super.projection_choice = this.projection_choice;
        super.run();
    }
}
