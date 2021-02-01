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

import ij.gui.GenericDialog;

@Plugin(type = Command.class, name = "SIMR Nd2Reader",  menuPath="Plugins>Stowers>Chris>SIMR Nd2 Reader")
public class Nd2ReaderPlugin implements Previewable, Command {

    @Parameter()
    File imagefile;

    @Parameter
    private UIService uis;

    int series = -2;

    private Nd2ImagePlus nd2 = null;

    @Override
    public void run() {
        System.out.println(imagefile.getAbsolutePath());
        nd2 = new Nd2ImagePlus(imagefile.getAbsolutePath());
        if (nd2.getSeriesCount()> 1) {
            System.out.println("N Series " + nd2.getSeriesCount());
            while (series == -2) {
                setSeries();
            }
            System.out.println("Series " + series);
            if (series == -1) {
                //open all
                for (int i = 0; i < nd2.getSeriesCount(); i++) {
                    ImagePlus _imp = nd2.getImagePlus(true, i);
                    _imp.setTitle(_imp.getTitle() + "-" + i);
                    _imp.show();
                }
            } else {
                nd2.getImagePlus(true, series).show();
            }
        }
        else {

            nd2.getImagePlus(true, 0).show();
        }
        nd2.close();
    }

    private void setSeries() {
        GenericDialog gd = new GenericDialog("Select a series");
        gd.addMessage("Enter the series to open, between 1 and " + nd2.getSeriesCount() + ", -1 for all");
        gd.addNumericField("Series:", 1,0);
        gd.showDialog();

        series = (int)gd.getNextNumber();
        if (series == -1) {
            return;
        }
        if (series > 0) {
            series = series - 1 ;
        }
        if (series >= nd2.getSeriesCount()) {
            series = -2;
            return;
        } else if (series >= 0) {
            return;
        }
        series = -2;
        IJ.showMessage("Set series greater than 0, or equal to -1");
    }
    @Override
    public void preview() {

    }

    @Override
    public void cancel() {

    }
}
