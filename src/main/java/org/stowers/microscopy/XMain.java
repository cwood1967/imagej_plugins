package org.stowers.microscopy;

import net.imagej.ImageJ;

public class XMain {

    public static void main(String[] args) {

//        final ImageJ imagej = net.imagej.Main.launch(args);
        final ImageJ imagej = new ImageJ(); //= net.imagej.ImageJ.launch(args);
        imagej.launch(args);
    }
}