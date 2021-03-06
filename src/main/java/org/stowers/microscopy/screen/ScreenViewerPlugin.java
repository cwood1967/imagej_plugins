package org.stowers.microscopy.screen;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import ij.IJ;
import ij.ImagePlus;
import ij.CompositeImage;

import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.LUT;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.stowers.microscopy.reader.Nd2ImagePlus;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.HashMap;
import ij.gui.GenericDialog;

@Plugin(type = Command.class, name = "SIMR Nd2Reader",  menuPath="Plugins>Stowers>Chris>SIMR Phenix Viewer")
public class ScreenViewerPlugin implements ActionListener, Previewable, Command {

    @Parameter(label="Choose plate folder", style="directory")
    File plateDir;

    protected JFrame frame;
    protected JPanel mainPanel;
    protected JPanel panel;
    protected JComboBox<Integer> colList;
    protected JComboBox<Integer> rowList;
    protected JComboBox<Integer> fieldList;
    protected JButton button;

    protected Vector<Integer> rows;
    protected Vector<Integer> cols;
    protected Vector<Integer> fields;
    protected HashMap<List<Integer>, List<File>> fileMap;

    List<File> fileList = null;

    @Override
    public void run() {

        makeFileList();
        parseRows();
        makeFrame();
    }

    public void makeFileList() {

        fileList = new ArrayList<File>();
        for (File f : plateDir.listFiles()) {
            if (f.getName().endsWith("tiff")) {
                fileList.add(f);
            }
        }
    }

    /*
    r01c01f02p01-ch1sk1fk1fl1.tiff	r02c02f02p01-ch1sk1fk1fl1.tiff
    0123456789012345
    r01c01f03p01-ch3sk1fk1fl1.tiff	r02c02f02p01-ch2sk1fk1fl1.tiff
    */
    public void parseRows() {
       rows = new Vector<Integer>();
       cols = new Vector<Integer>();
       fields = new Vector<Integer>();

       fileMap = new HashMap<>();

       for (File f : fileList) {
           String name = f.getName();
           Integer ri = Integer.parseInt(name.substring(1,3));
           if (!rows.contains(ri)) {
               rows.add(ri);
           }
           Integer ci = Integer.parseInt(name.substring(4,6));
           if (!cols.contains(ci)) {
               cols.add(ci);
           }
           Integer fei = Integer.parseInt(name.substring(7,9));
           if (!fields.contains(fei)) {
               fields.add(fei);
           }
           List<Integer> x = new ArrayList<Integer>();
           x.add(ri);
           x.add(ci);
           x.add(fei);
           if (!fileMap.containsKey(x)) {
                fileMap.put(x, new ArrayList<File>());
           }
           fileMap.get(x).add(f);
       }

        Collections.sort(rows);
        Collections.sort(cols);
        Collections.sort(fields);
    }

    private void makeFrame() {

        mainPanel = new JPanel(new BorderLayout());
        colList = new JComboBox<>(cols);
        rowList = new JComboBox<>(rows);
        fieldList = new JComboBox<>(fields);
        button = new JButton();
        button.setText("Open:");

        button.addActionListener(this);
        panel = new JPanel(new GridLayout(0, 2));

        panel.add(new JLabel("Row"));
        panel.add(rowList);
        panel.add(new JLabel("Column"));
        panel.add(colList);
        panel.add(new JLabel("Field"));
        panel.add(fieldList);
        panel.add(new JLabel("Push to open"));
        panel.add(button);

        panel.setSize(240, 50);
        mainPanel.add(panel, BorderLayout.WEST);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25,25,25,25));
        frame = new JFrame("Screen Viewer");
        frame.setContentPane(mainPanel);
        frame.setSize(800,600);
        frame.pack();
        frame.setVisible(true);

    }
    @Override
    public void preview() {

    }

    @Override
    public void cancel() {

    }

    protected void openImage(List<Integer> info, List<File> files) {

        Collections.sort(files);
        ImageStack stack = new ImageStack();
        ArrayList<String> channel_names = new ArrayList();
        for (File f : files) {
            String fname = f.getAbsolutePath();
            ImageProcessor _ip = IJ.openImage(fname).getProcessor();
            stack.addSlice(_ip);
            String ch = f.getName().substring(13,16);
            if (!channel_names.contains(ch)) {
                channel_names.add(ch);
            }

        }

        int n_channels = channel_names.size();
        int nz = stack.size()/n_channels;
        StringBuilder title = new StringBuilder();
        title.append("Row-");
        title.append(info.get(0));
        title.append(" Column-");
        title.append(info.get(1));
        title.append(" Field-");
        title.append(info.get(2));

        ImagePlus imp = IJ.createHyperStack(title.toString(), stack.getWidth(),
                stack.getHeight(), n_channels, nz, 1, stack.getBitDepth());
        imp.setStack(stack);
        CompositeImage cimp = new CompositeImage(imp);
        cimp.setChannelLut(LUT.createLutFromColor(Color.green), 1);
        cimp.setChannelLut(LUT.createLutFromColor(Color.magenta), 2);
        cimp.setChannelLut(LUT.createLutFromColor(Color.gray), 3);
        cimp.setMode(cimp.COMPOSITE);

        cimp.show();
        cimp.setC(1);
        IJ.run("Enhance Contrast", "saturated=0.35");
        cimp.setC(2);
        IJ.run("Enhance Contrast", "saturated=0.35");
        cimp.setC(3);
        IJ.run("Enhance Contrast", "saturated=0.35");
        cimp.setC(1);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        List<Integer> x = new ArrayList<>();
        x.add((int)rowList.getSelectedItem());
        x.add((int)colList.getSelectedItem());
        x.add((int)fieldList.getSelectedItem());
        openImage(x, fileMap.get(x));
    }
}
