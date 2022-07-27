package com.infraleap.vaadin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.function.ValueProvider;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FileTree extends VerticalLayout {

    /**
     * Note that the file could be NULL (nothing selected).
     * Also keep file.isFile() or file.isDirectory() cases in mind.
     */
    public interface SelectedHandler {
        void handle(File file);
    }

    public interface FileNameFilter {
        boolean accept(String s);
    }

    private final Tree<FileWrapper> filesTree;

    private static class FileWrapper implements Comparable<FileWrapper> {
        private File wrappedFile;

        public FileWrapper(File toBeWrapped) {
            this.wrappedFile = toBeWrapped;
        }

        public boolean isDirectory() {
            return wrappedFile.isDirectory();
        }

        public String getName() {
            return this.wrappedFile.getName();
        }

        public void setName(String newName) {
            String parent = wrappedFile.getParent();
            File renameTo;
            if (parent != null) {
                renameTo = new File(parent + File.separatorChar + newName);
            } else {
                renameTo = new File(newName);
            }

            if (renameTo.exists()) {
                System.err.println("File already exists: Could not rename '" + wrappedFile.getAbsolutePath() + "' to '" + renameTo.getAbsolutePath() + "'.");
                return;
            }

            if (wrappedFile.renameTo(renameTo)) {
                // iff true, then renaming succeeded
                System.err.println("Successfully renamed '" + wrappedFile.getAbsolutePath() + "' to '" + renameTo.getAbsolutePath() + "'.");
                wrappedFile = renameTo;
            } else {
                System.err.println("Could not rename '" + wrappedFile.getAbsolutePath() + "' to '" + renameTo.getAbsolutePath() + "'.");
            }
        }

        public FileWrapper[] listFiles() {
            File[] listing = wrappedFile.listFiles();
            if (listing == null) {
                return null;
            } else {
                FileWrapper[] retVal = new FileWrapper[listing.length];
                for (int i = 0; i < listing.length; i++) {
                    retVal[i] = new FileWrapper(listing[i]);
                }
                return retVal;
            }
        }

        @Override
        public int compareTo(FileWrapper o) {
            return this.wrappedFile.compareTo(o.wrappedFile);
        }
    }

    private static class Tree<T> extends TreeGrid<T> {

        Tree(ValueProvider<T, ?> valueProvider) {
            Column<T> only = addHierarchyColumn(valueProvider);
            only.setAutoWidth(true);
        }
    }

    public FileTree(String rootDir) {
        this(rootDir, false);
    }

    public FileTree(String rootDir, boolean canRename) {
        this(rootDir, canRename, null);
    }

    public FileTree(String rootDir, boolean canRename, SelectedHandler selectedHandler) {
        final FileWrapper root = new FileWrapper(new File(rootDir));

        this.filesTree = new Tree<>(FileWrapper::getName);

        Binder<FileWrapper> binder = new Binder<>();
        Editor<FileWrapper> editor = filesTree.getEditor();
        editor.setBinder(binder);

        filesTree.setItems(Collections.singleton(root), this::getFiles);
        filesTree.setWidthFull();
        filesTree.setHeight("300px");
        if (selectedHandler == null) {
            filesTree.setSelectionMode(Grid.SelectionMode.NONE);
        }
        TextField editorTextField = new TextField();
        editorTextField.setWidthFull();
        filesTree
                .getColumns()
                .stream()
                .findFirst()
                .ifPresent(
                        fileColumn -> {
                            fileColumn.setComparator(Comparator.naturalOrder());
                            GridSortOrder<FileWrapper> sortOrder = new GridSortOrder<>(fileColumn, SortDirection.ASCENDING);
                            filesTree.sort(Collections.singletonList(sortOrder));


                            if (selectedHandler != null) {
                                filesTree
                                        .asSingleSelect()
                                        .addValueChangeListener(
                                                event -> {
                                                    File file = event.getValue().wrappedFile;
                                                    selectedHandler.handle(file);
                                                }
                                        );
                            }

                            if (canRename) {
                                fileColumn.setEditorComponent(editorTextField);
                                filesTree.addItemDoubleClickListener(e -> {
                                    editor.editItem(e.getItem());
                                    Component editorComponent = e.getColumn().getEditorComponent();
                                    if (editorComponent instanceof Focusable) {
                                        ((Focusable<?>) editorComponent).focus();
                                    }
                                    editorTextField.setValue(e.getItem().getName());
                                });
                                editorTextField.getElement().addEventListener("keydown", event -> editor.cancel()).setFilter("event.key === 'Escape' || event.key === 'Esc'");

                                editorTextField.addBlurListener(e -> editor.getItem().setName(editorTextField.getValue()));
                            }
                            this.add(filesTree);
                            setSizeFull();
                        }
                );
    }


    public void setFileNameFilter(FileNameFilter fileNameFilter){
        ((TreeDataProvider<FileWrapper>) this.filesTree.getDataProvider()).setFilter( fileNameFilter == null? null : fileWrapper -> fileNameFilter.accept(fileWrapper.getName()));
    }

    private List<FileWrapper> getFiles(FileWrapper parent) {
        if (parent.isDirectory()) {
            FileWrapper[] list = parent.listFiles();
            if (list != null) {
                return Arrays.asList(list);
            }
        }

        return Collections.emptyList();
    }
}
