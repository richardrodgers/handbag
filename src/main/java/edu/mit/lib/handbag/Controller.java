/**
 * Copyright 2023 Richard Rodgers
 * SPDX-Licence-Identifier: Apache-2.0
 */
package edu.mit.lib.handbag;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.util.Callback;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.controlsfx.control.PropertySheet;

import com.fasterxml.jackson.jr.ob.JSON;

import org.modrepo.packr.BagBuilder;
import org.modrepo.packr.Serde;

import edu.mit.lib.handbag.model.MetadataAssembler;

public class Controller {

    @FXML private Label workflowLabel;
    @FXML private ChoiceBox<Workflow> workflowChoiceBox;
    @FXML private Label bagLabel;
    @FXML private Label bagSizeLabel;
    @FXML private Button sendButton;
    @FXML private Button trashButton;
    @FXML private TreeView<PathRef> payloadTreeView;
    @FXML private TreeView<PathRef> tagTreeView;
    @FXML private PropertySheet metadataPropertySheet;

    // flags for state of metadata editing
    // - clean means no edits have been performed
    // - gooey means at least one sticky field has been (re)assigned
    // - complete means all non-optional properties have values
    private boolean cleanMetadata = true;
    private boolean gooeyMetadata = false;
    private boolean completeMetadata = false;
    private long bagSize = 0L;
    private String bagName;
    private int counter = 0;
    private String agent = "anon";
    private Map<String, String> appProps;
    private final Image dirIcon = new Image(getClass().getResourceAsStream("/Folder.png"));
    private final Image refIcon = new Image(getClass().getResourceAsStream("/Anchor.png"));

    public void setAgent(String agent) {
        this.agent = agent;
        Event.fireEvent(workflowChoiceBox, new ActionEvent("agent", null));
    }

    public void setAppProperties(Map<String, String> props) {
        appProps = props;
    }

    public void initialize() {

        Image wfIm = new Image(getClass().getResourceAsStream("/SiteMap.png"));
        workflowLabel.setGraphic(new ImageView(wfIm));
        workflowLabel.setContentDisplay(ContentDisplay.BOTTOM);

        workflowChoiceBox.addEventHandler(ActionEvent.ACTION, event -> {
            if (workflowChoiceBox.getItems().size() != 0) {
                return;
            }
            ObservableList<Workflow> wflowList = FXCollections.observableArrayList(loadWorkflows());
            workflowChoiceBox.setItems(wflowList);
            workflowChoiceBox.getSelectionModel().selectedIndexProperty().addListener((ov, value, new_value) -> {
                int newVal = (int)new_value;
                if (newVal != -1) {
                    Workflow newSel = workflowChoiceBox.getItems().get(newVal);
                    if (newSel != null) {
                        workflowLabel.setText(newSel.getName());
                        generateBagName(newSel.getBagNameGenerator());
                        bagLabel.setText(bagName);
                        sendButton.setText(newSel.getDestinationName());
                        setMetadataList(newSel);
                    }
                }
            });
        });

        Image bagIm = new Image(getClass().getResourceAsStream("/Bag.png"));
        bagLabel.setGraphic(new ImageView(bagIm));
        bagLabel.setContentDisplay(ContentDisplay.BOTTOM);

        Image sendIm = new Image(getClass().getResourceAsStream("/Cabinet.png"));
        sendButton.setGraphic(new ImageView(sendIm));
        sendButton.setContentDisplay(ContentDisplay.BOTTOM);
        sendButton.setDisable(true);
        sendButton.setOnAction(e -> transmitBag());

        Image trashIm = new Image(getClass().getResourceAsStream("/Bin.png"));
        trashButton.setGraphic(new ImageView(trashIm));
        trashButton.setContentDisplay(ContentDisplay.BOTTOM);
        trashButton.setDisable(true);
        trashButton.setOnAction(e -> reset(false));

        initTreeView(payloadTreeView, "data");
        initTreeView(tagTreeView, "root");

        metadataPropertySheet.setModeSwitcherVisible(false);
        metadataPropertySheet.setDisable(false);
        metadataPropertySheet.addEventHandler(KeyEvent.ANY, event -> {
            checkComplete(metadataPropertySheet.getItems());
            event.consume();
        });
    }

    private void initTreeView(TreeView<PathRef> view, String root) {
        TreeItem<PathRef> rootItem = new TreeItem<>(new PathRef(null, root, Paths.get(root), true), new ImageView(dirIcon));
        rootItem.setExpanded(true);
        view.setEditable(true);
        view.setCellFactory(new Callback<TreeView<PathRef>, TreeCell<PathRef>>(){
            @Override
            public TreeCell<PathRef> call(TreeView<PathRef> p) {
                return new TextFieldTreeCellImpl(root.equals("data"));
            }
        });
        view.setRoot(rootItem);
        view.setOnDragOver(event -> {
            if (event.getGestureSource() != view &&
                event.getDragboard().getFiles().size() > 0) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });
        view.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.getFiles().size() > 0) {
                for (File dragFile : db.getFiles()) {
                    if (dragFile.isDirectory()) {
                        // explode directory and add expanded relative paths to tree
                        try {
                            Files.walkFileTree(dragFile.toPath(), new SimpleFileVisitor<Path>() {
                                @Override
                                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                                    return FileVisitResult.CONTINUE;
                                }
                                @Override
                                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                    selectedRoot(view).getChildren().add(new TreeItem<>(new PathRef(null, file.getFileName().toString(), file, false)));
                                    //if (updateSize) {
                                        bagSize += Files.size(file);
                                    //}
                                    return FileVisitResult.CONTINUE;
                                }
                                @Override
                                public FileVisitResult postVisitDirectory(Path dir, IOException exp) throws IOException {
                                    return FileVisitResult.CONTINUE;
                                }
                            });
                        } catch (IOException ioe) {}
                    } else {
                        var sroot = selectedRoot(view);
                        sroot.getChildren().add(new TreeItem<>(new PathRef(null, dragFile.getName(), dragFile.toPath(), false)));
                        //if (updateSize) {
                            bagSize += dragFile.length();
                       // }
                    }
                }
                bagSizeLabel.setText(scaledSize(bagSize, 0));
                success = true;
                updateButtons();
            }
            event.setDropCompleted(success);
            event.consume();
        });
    } 

    private TreeItem<PathRef> selectedRoot(TreeView<PathRef> view) {
        // determine appropriate root for insertions
        TreeItem<PathRef> sel = view.getSelectionModel().getSelectedItem();
        if (sel != null) {
            if (sel.getValue().isFolder()) {
                return sel;
            } else {
                return sel.getParent();
            }
        }
        return view.getRoot();
    }

    private void checkComplete(List<PropertySheet.Item> mdList) {
        boolean curComplete = true;
        for (PropertySheet.Item item : mdList) {
            MetadataItem mdItem = (MetadataItem)item;
            String value = mdItem.getValue();
            if (! mdItem.isOptional() && (value == null || value.equals(""))) {
                curComplete = false;
                break;
            }
        }
        cleanMetadata = false;
        completeMetadata = curComplete;
        updateButtons();
    }

    private void transmitBag() {
        try {
            URI destUri = new URL(workflowChoiceBox.getValue().getDestinationUrl()).toURI();
            String pkgFormat = workflowChoiceBox.getValue().getPackageFormat();
            boolean localDest = destUri.getScheme().startsWith("file");
            Path destDir = Paths.get(destUri);
            var builder = localDest ? new BagBuilder(destDir.resolve(bagName)) : new BagBuilder();
            // add payload files
            for (TreeItem<PathRef> ti : payloadTreeView.getRoot().getChildren()) {
                fillPayload(ti, builder);
            }
             // add tag files
             for (TreeItem<PathRef> ti : tagTreeView.getRoot().getChildren()) {
                PathRef pr = ti.getValue();
                if (pr.getRelPath().length() > 0) {
                    builder.tag(pr.getFullPath(), pr.getSourcePath());
                } else {
                    builder.tag(pr.getFullPath(), pr.getSourcePath());
                }
            }
            // add metadata (currently only bag-info properties supported)
            for (PropertySheet.Item mdItem : metadataPropertySheet.getItems()) {
                MetadataItem item = (MetadataItem)mdItem;
                if (item.getValue() != null && item.getValue().length() > 0) {
                    builder.metadata(item.getRealName(), item.getValue());
                }
            }
            if (localDest) {
                // TODO - set notime param via config
                Serde.toPackage(builder.build(), pkgFormat, false, Optional.empty() );
            } else {
                // send to URL - TODO
            }
            reset(true);
        } catch (IOException | URISyntaxException exp) {}
    }

    private void fillPayload(TreeItem<PathRef> ti, BagBuilder builder) throws IOException, URISyntaxException {
        PathRef pr = ti.getValue();
        PathRef parent = ti.getParent().getValue();
        if (! pr.isFolder()) {
            Optional<URI> location = pr.getLocation();
            if (location.isEmpty()) {
                System.out.println("relPath: " + parent.getRelPath());
                if (parent.getRelPath() != null) {
                    builder.payload(makeRelPath(parent, pr), pr.getSourcePath());
                } else {
                    builder.payload(pr.getSourcePath());
                }
            } else {
                if (parent.getRelPath() != null) {
                    builder.payloadRef(makeRelPath(parent, pr), pr.getSourcePath(), location.get());
                } else {
                    builder.payloadRef(pr.getFileName(), pr.getSourcePath(), location.get());
                }
            }
        } else {
            for (TreeItem<PathRef> pti : ti.getChildren()) {
                System.out.println("in folder children");
                fillPayload(pti, builder);
            }
        }
    }

    private String makeRelPath(PathRef parent, PathRef ref) {
        if (parent.relPath == null) {
            return ref.fileName;
        } else if (parent.relPath.isEmpty()) {
            return parent.fileName + "/" + ref.fileName;
        } else {
            return parent.relPath + "/" + ref.fileName;
        }
    }

    // reset payload and metadata to ready state (respecting stickiness if requested)
    private void reset(boolean keepStickies) {
        payloadTreeView.getRoot().getChildren().clear();
        ObservableList<PropertySheet.Item> items = metadataPropertySheet.getItems();
        List<PropertySheet.Item> saveItems = new ArrayList<>();
        saveItems.addAll(items);
        items.clear();
        cleanMetadata = true;
        gooeyMetadata = false;
        boolean anyNeeded = false;
        int i = 0;
        for (MetadataSpec spec : workflowChoiceBox.getValue().getMetadata()) {
            if (! spec.isOptional() && spec.needsValue()) {
                anyNeeded = true;
            }
            MetadataItem item = new MetadataItem(spec);
            String oldValue = (String)saveItems.get(i).getValue();
            if (spec.isSticky() && keepStickies && oldValue != null) {
                item.setValue(oldValue);
                gooeyMetadata = true;
            }
            items.add(item);
            i++;
        }
        completeMetadata = ! anyNeeded;
        updateButtons();
        bagSize = 0L;
        bagSizeLabel.setText("[empty]");
        generateBagName(workflowChoiceBox.getValue().getBagNameGenerator());
        bagLabel.setText(bagName);
    }

    // check and update disabled state of buttons based on application state
    private void updateButtons() {
        boolean empty = payloadTreeView.getRoot().getChildren().size() == 0;
        trashButton.setDisable(empty && cleanMetadata && ! gooeyMetadata);
        sendButton.setDisable(empty || ! completeMetadata);
        workflowChoiceBox.setDisable(! (empty && cleanMetadata));
    }

    private void setMetadataList(Workflow wf) {
        ObservableList<PropertySheet.Item> items = metadataPropertySheet.getItems();
        items.clear();
        boolean anyNeeded = false;
        for (MetadataSpec spec : new MetadataAssembler().getMetadata()) {
            if (! spec.isOptional() && spec.needsValue()) {
                anyNeeded = true;
            }
            items.add(new MetadataItem(spec));
        }
        cleanMetadata = true;
        completeMetadata = ! anyNeeded;
    }

    private List<Workflow> loadWorkflows() {
        // get list of agent-specific installed workflows first, then load each one.
        // It there is dispatcher web service, use it, else look in local JSON resource files
        String dispatcherUrl = appProps.get("dispatcher");
        List<Workflow> workflows = new ArrayList<>();
        try {
            List<String> workflowIds = getWorkflowIds(dispatcherUrl);
            for (String wfFile : workflowIds) {
                Workflow wf = null;
                if (dispatcherUrl != null) {
                    try (InputStream in = new URL(dispatcherUrl).openConnection().getInputStream()) {
                        wf = JSON.std.beanFrom(Workflow.class, in);
                    }
                } else {
                    wf = JSON.std.beanFrom(Workflow.class, getClass().getResourceAsStream("/" + wfFile));
                }
                workflows.add(wf);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return workflows;
    }

    private List<String> getWorkflowIds(String dispatcherUrl) throws IOException {
        List<String> wfIdList = null;
        if (dispatcherUrl == null) {
            wfIdList = JSON.std.listOfFrom(String.class, getClass().getResourceAsStream("/" + agent + ".json"));
        } else {
            try (InputStream in = new URL(dispatcherUrl).openConnection().getInputStream()) {
                wfIdList = JSON.std.listOfFrom(String.class, in);
            }
        }
        return wfIdList;
    }

    private void generateBagName(String generator) {
        bagName = "transfer." + counter++;
    }

    private final class TextFieldTreeCellImpl extends TreeCell<PathRef> {
 
        private TextField textField;
        private final ContextMenu ctxMenu = new ContextMenu();
        private final boolean payload; 
 
        public TextFieldTreeCellImpl(boolean payload) {
            this.payload = payload;
        }
 
        @Override
        public void startEdit() {
            super.startEdit();
 
            if (textField == null) {
                createTextField();
            }
            setText(null);
            setGraphic(textField);
            textField.selectAll();
        }
 
        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(((PathRef)getItem()).getFileName());
            setGraphic(getTreeItem().getGraphic());
        }
 
        @Override
        public void updateItem(PathRef item, boolean empty) {
            super.updateItem(item, empty);
 
            if (empty) {
                setText(null);
                setGraphic(null);
            } else {
                if (isEditing()) {
                    if (textField != null) {
                        textField.setText(getString());
                    }
                    setText(null);
                    setGraphic(textField);
                } else {
                    setText(getString());
                    setGraphic(getTreeItem().getGraphic());
                    //if (!getTreeItem().isLeaf()&&getTreeItem().getParent()!= null) {
                        if (ctxMenu.getItems().isEmpty()) {
                            if (item.isFolder()) {
                                ctxMenu.getItems().add(addFolderMenuItem(this));
                            } else if (getTreeItem().getParent() != null) {
                                ctxMenu.getItems().add(removeMenuItem(this));
                                if (payload) {
                                    ctxMenu.getItems().add(fetchMenuItem(this));
                                }
                            }
                            setContextMenu(ctxMenu);
                        }
                   // }
                } 
            }
        }
 
        private void createTextField() {
            textField = new TextField(getString());
            textField.setOnKeyReleased(new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent t) {
                    if (t.getCode() == KeyCode.ENTER) {
                        commitEdit(getItem().rename(textField.getText()));
                    } else if (t.getCode() == KeyCode.ESCAPE) {
                        cancelEdit();
                    }
                }
            });
        }
 
        private String getString() {
            return getItem() == null ? "" : getItem().toString();
        }
    }

    static class PathRef {
        private String relPath;  // not including fileName
        private String fileName;
        private Path srcPath;
        private boolean folder;
        private String location;

        public PathRef(String relPath, String fileName, Path srcPath, boolean folder) {
            this.relPath = relPath;
            this.fileName = fileName;
            this.srcPath = srcPath;
            this.folder = folder;
        }

        public PathRef rename(String name) {
            return new PathRef(relPath, name, srcPath, folder);
        }

        public String getRelPath() {
            return relPath;
        }

        public String getFullPath() {
            return relPath + "/" + fileName;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public Path getSourcePath() {
            return srcPath;
        }

        public boolean isFolder() {
            return folder;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public Optional<URI> getLocation() throws URISyntaxException {
            return location != null ? Optional.of(new URI(location)) : Optional.empty();
        }

        public String toString() {
            return fileName;
        }
    }

    private MenuItem addFolderMenuItem(TreeCell<PathRef> cell) {
        MenuItem addDirItem = new MenuItem("Add Folder");
        addDirItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                String name = "new-folder";
                TreeItem<PathRef> item = cell.getTreeItem();
                var parent = item.getParent();
                String relPath = "";
                if (parent != null) {
                    var parVal = parent.getValue();
                    if (parVal.relPath != null) {
                        if (parVal.relPath.length() > 0) {
                            relPath = parVal.relPath + "/" + parVal.fileName;
                        } else {
                            relPath = parVal.fileName;
                        }
                    }
                }
                var ti = new TreeItem<PathRef>(new PathRef(relPath, name, Paths.get(name), true), new ImageView(dirIcon));
                item.getChildren().add(ti);
                ti.setExpanded(true);
            }
        });
        return addDirItem;
    }

    private MenuItem removeMenuItem(TreeCell<PathRef> cell) {
        MenuItem removeItem = new MenuItem("Remove");
        removeItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                TreeItem<PathRef> item = cell.getTreeItem();
                // subtract bytes from payload count
                try {
                    bagSize -= Files.size(item.getValue().getSourcePath());
                    bagSizeLabel.setText(scaledSize(bagSize, 0));
                } catch (Exception exp) {}
                item.getParent().getChildren().remove(item);
            }
        });
        return removeItem;
    }

    private MenuItem fetchMenuItem(TreeCell<PathRef> cell) {
        MenuItem fetchItem = new MenuItem("Fetch URI");
        fetchItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                new TextInputDialog("Fetch URI").showAndWait()
                .ifPresent(uri -> {
                    TreeItem<PathRef> item = cell.getTreeItem();
                    PathRef pr = item.getValue();
                    try {
                        if (pr.getLocation().isEmpty()) {
                            pr.setLocation(uri);
                            item.setGraphic(new ImageView(refIcon));
                            // subtract bytes from payload count - should include only in-bag files
                            bagSize -= Files.size(pr.getSourcePath());
                            bagSizeLabel.setText(scaledSize(bagSize, 0));
                        }
                    } catch (Exception exp) {}
                });
            }
        });
        return fetchItem;
    }

    private static final String[] tags = {"bytes", "KB", "MB", "GB", "TB"};
    static String scaledSize(long size, int index) {
        if (size < 1000) {
            return size + " " + tags[index];
        } else {
            return scaledSize(size / 1000, index + 1);
        }
    }
}
