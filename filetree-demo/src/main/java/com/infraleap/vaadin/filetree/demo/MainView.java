package com.infraleap.vaadin.filetree.demo;

import com.infraleap.vaadin.FileTree;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.io.File;
import java.io.IOException;

/**
 * The main view contains a button and a click listener.
 */
@Route("")
public class MainView extends VerticalLayout {

    static File root = new File(new File(System.getProperty("java.io.tmpdir")).getAbsolutePath() + File.separator + "ROOT");

    /*
     * This static code block only sets up some artificial file tree, for users to browse.
     * Normally, you would likely want to expose some existing directory tree.
     */
    static void initDirs(){
        try {
            boolean createdRoot = root.mkdir();
            if (createdRoot || root.exists() && root.isDirectory()){
                File subdir = new File(root.getAbsolutePath() + File.separator + "Sub-Directory");
                boolean createdSubDir = subdir.mkdir();
                if (createdSubDir || subdir.exists() && subdir.isDirectory()) {
                    File four = new File(subdir.getAbsolutePath() + File.separator + "four");
                    four.createNewFile();
                }
            }
            File one = new File(root.getAbsolutePath() + File.separator + "one");
            one.createNewFile();
            File two = new File(root.getAbsolutePath() + File.separator + "two");
            two.createNewFile();
            File three = new File(root.getAbsolutePath() + File.separator + "three");
            three.createNewFile();
        }
        catch (IOException ioException){
            ioException.printStackTrace();
        }
    }

    public MainView() {
        initDirs();
        this.setSizeFull();
        add(new FileTree(root.getAbsolutePath(), true, file -> {
            Notification.show("File '"+file.getAbsolutePath()+"' selected.", 3000, Notification.Position.BOTTOM_CENTER);
        }));
    }
}
