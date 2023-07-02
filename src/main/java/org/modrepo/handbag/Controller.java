/**
 * Copyright 2023 Richard Rodgers
 * SPDX-Licence-Identifier: Apache-2.0
 */
package org.modrepo.handbag;

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
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Callback;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import org.controlsfx.control.PropertySheet;
import org.controlsfx.control.textfield.TextFields;
import org.controlsfx.property.editor.Editors;

import com.fasterxml.jackson.jr.ob.JSON;

import org.modrepo.packr.Bag;
import org.modrepo.packr.BagBuilder;
import org.modrepo.packr.Serde;
import org.modrepo.packr.BagBuilder.EolRule;
import org.modrepo.bagmatic.Bagmatic;
import org.modrepo.bagmatic.ContextBuilder;
import org.modrepo.bagmatic.impl.profile.BagitProfile;
import org.modrepo.bagmatic.impl.profile.BagitTagConstraint;
import org.modrepo.bagmatic.model.Result;
import org.modrepo.handbag.model.MetadataAssembler;
import org.modrepo.handbag.model.WorkSpec;

public class Controller {

    @FXML private AccordionDP settings;
    @FXML private TextField bagNameField;
    @FXML private TextField destField;
    @FXML private Button destBrowse;
    @FXML private CheckBox destReuse;
    @FXML private ChoiceBox<String> pkgFormats;
    @FXML private CheckBox resMdBox;
    @FXML private CheckBox retainMdBox;
    @FXML private TextField profileField;
    @FXML private Button profileBrowse;
    @FXML private Label workflowLabel;
   // @FXML private ChoiceBox<Workflow> workflowChoiceBox;
    @FXML private TextField logField;
    @FXML private Button logBrowse;
    @FXML private CheckBox logAppend;
    @FXML private ChoiceBox<String> textEncs;
    @FXML private ChoiceBox<String> eolRules;
    @FXML private Label bagLabel;
    @FXML private Label bagSizeLabel;
    @FXML private Button sendButton;
    @FXML private Button trashButton;
    @FXML private TreeView<PathRef> payloadTreeView;
    @FXML private TreeView<PathRef> tagTreeView;
    @FXML private PropertySheet metadataPropertySheet;
    @FXML private TextField addMetaField;
    @FXML private Button addMetaButton;
    @FXML private Button repMetaButton;
    @FXML private TitledPane basicPane;
    @FXML private ResourceBundle resources;
    @FXML private HBox profBox;
    @FXML private Label consoleLogLabel;
    @FXML private Button logBackButton;

    // flags for state of metadata editing
    // - clean means no edits have been performed
    // - complete means all non-optional properties have values
    private boolean cleanMetadata = true;
    private boolean completeMetadata = false;
    private long bagSize = 0L;
    private int counter = 0;
    private final Image dirIcon = new Image(getClass().getResourceAsStream("/Folder.png"));
    private final Image refIcon = new Image(getClass().getResourceAsStream("/Anchor.png"));
    private TextField dispatchField = new TextField();
    private List<CheckBox> chkSumAlgs;
    private WorkSpec workSpec;
    private ConsoleLog consoleLog = new ConsoleLog();

    public void initialize() {

        // Basic settings
        resMdBox.setIndeterminate(false);

        pkgFormats.getItems().addAll("zip", "tar", "tgz");
        pkgFormats.setValue("zip");

        destBrowse.setOnAction(e -> chooseLocalDir(destField));
        destReuse.setSelected(true);

        resMdBox.setSelected(true);
        retainMdBox.setSelected(true);

        profileField = TextFields.createClearableTextField();
        profBox.getChildren().add(1, profileField);
        profileBrowse.setOnAction(e -> chooseLocalFile(profileField));

        // Feature settings
        logBrowse.setOnAction(e -> chooseLocalDir(logField));
        logAppend.setSelected(true);

        // Advanced settings
        // FIXME
        chkSumAlgs = List.of(new CheckBox("sha512"), new CheckBox("sha256"),
                             new CheckBox("sha1"), new CheckBox("md5"));
        chkSumAlgs.get(0).setSelected(true);

        textEncs.getItems().addAll("UTF-8", "UTF-16");
        textEncs.setValue("UTF-8");

        var rules = Arrays.asList(BagBuilder.EolRule.values()).stream()
                    .map(r -> r.toString()).toList();
        eolRules.getItems().addAll(rules);
        eolRules.setValue("SYSTEM");

        // Information settings
        /* 
        Label bagitVersion = new Label("BagIt Specification Version: " + Bag.bagItVersion());
        Label libVersion = new Label("Bag Handler Software Version: " + Bag.libVersion());
        */

        settings.setExpandedPane(basicPane);

        Image wfIm = new Image(getClass().getResourceAsStream("/SiteMap.png"), 90, 90, false, false);
        workflowLabel.setGraphic(new ImageView(wfIm));
        workflowLabel.setContentDisplay(ContentDisplay.BOTTOM);

        destField.setOnAction(e -> reset(false));

        var bagText = bagNameField.getText();
        bagLabel.setText(bagText.isEmpty() ? "Bag" : bagText);
        /* 
        workflowChoiceBox.addEventHandler(Act)ionEvent.ACTION, event -> {
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
        */

        Image bagIm = new Image(getClass().getResourceAsStream("/Bag.png"), 90, 90, false, false);
        bagLabel.setGraphic(new ImageView(bagIm));
        bagLabel.setContentDisplay(ContentDisplay.BOTTOM);

        Image sendIm = new Image(getClass().getResourceAsStream("/Cabinet.png"), 90, 90, false, false);
        sendButton.setGraphic(new ImageView(sendIm));
        sendButton.setContentDisplay(ContentDisplay.BOTTOM);
        sendButton.setDisable(destField.getLength() == 0);
        sendButton.setOnAction(e -> transmitBag());

        Image trashIm = new Image(getClass().getResourceAsStream("/Bin.png"), 90, 90, false, false);
        trashButton.setGraphic(new ImageView(trashIm));
        trashButton.setTooltip(new Tooltip(resources.getString("trashTip")));
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
        metadataPropertySheet.setPropertyEditorFactory((PropertySheet.Item item) -> {
            var mdItem = (MetadataItem)item;
            if (mdItem.getPermitted().size() > 1) {
                return Editors.createChoiceEditor(mdItem, mdItem.getPermitted());
            } else {
                return Editors.createTextEditor(mdItem);
            }
        });
        // test only
        metadataPropertySheet.getItems().addAll(getReserved());

       // repMetaButton.setOnAction(e -> repeatMetaElement());
        addMetaButton.setOnAction(e -> addMetaElement(addMetaField.getText()));
        
        // log display
        consoleLogLabel.setText(consoleLog.currentEntry());
        consoleLogLabel.addEventHandler(ActionEvent.ANY, event -> {
            consoleLogLabel.setText(consoleLog.currentEntry());
            event.consume();
        });
        logBackButton.setOnAction(e -> consoleLogLabel.setText(consoleLog.previousEntry(consoleLogLabel.getText())));
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
            if (mdItem.isRequired() && (value == null || value.equals(""))) {
                curComplete = false;
                break;
            }
        }
        cleanMetadata = false;
        completeMetadata = curComplete;
        updateButtons();
    }

    private void transmitBag() {
        System.err.println("In transmit");
        try {
            URI destUri = new URL(destField.getText()).toURI();
            boolean isLocalDest = destUri.getScheme().startsWith("file");
            BagBuilder builder = null;
            if (isLocalDest) {
                Path destDir = Paths.get(destUri);
                builder = new BagBuilder(destDir.resolve(bagNameField.getText()),
                          Charset.forName(textEncs.getValue()),
                          EolRule.valueOf(eolRules.getValue()),
                          false,
                          /* 
                          chkSumAlgs.stream()
                          .filter(cb -> cb.isSelected())
                          .map(cb -> csAlgoCode(cb.getText()))
                          .toList().toArray(new String[0])
                          */
                          List.of("SHA-512").toArray(new String[0])
                          );
            } else {
                // TODO - set staging dir
                builder = new BagBuilder();
                // test only
                consoleLog.log("Non-Local destination not implemented");
                System.err.println("Log: " + consoleLog.currentEntry());
                sendButton.fireEvent(new ActionEvent());
            }
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
            if (isLocalDest) {
                // TODO - set notime param via config
                Serde.toPackage(builder.build(), pkgFormats.getValue(), false, Optional.empty() );
            } else {
                // send to URL - TODO
            }
            reset(true);
        } catch (IOException | URISyntaxException exp) {
                // test only
                consoleLog.log("Exception thrown: " + exp.getMessage());
                System.err.println("Log: " + consoleLog.currentEntry());
               // sendButton.fireEvent(new ActionEvent());
        }
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

    private void addMetaElement(String name) {
        var sheet = metadataPropertySheet;
        sheet.getItems().add(new MetadataItem(name, new BagitTagConstraint(false, List.of(), true, name)));
    }

    private void repeatMetaElement() {
        var items = metadataPropertySheet.getItems();
        for (PropertySheet.Item item : items) {
            if (item.isEditable()) {
                // and has focus
                items.add(0, new MetadataItem(item.getName(), new BagitTagConstraint(false, List.of(), true, item.getName())));
                break;
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

    private String csAlgoCode(String csAlgol) {
        String csa = csAlgol.toUpperCase();
        if (csa.startsWith("SHA") && csa.indexOf("-") == -1) {
            return "SHA-" + csa.substring(3);
        } else {
            return csa;
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
        boolean anyNeeded = false;
        int i = 0;
        /* 
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
        */
         // test only
        metadataPropertySheet.getItems().addAll(getReserved());
        completeMetadata = ! anyNeeded;
        updateButtons();
        bagSize = 0L;
        bagSizeLabel.setText("[empty]");
        //generateBagName(workflowChoiceBox.getValue().getBagNameGenerator());
        bagLabel.setText(bagNameField.getText());
    }

    // load and filter by autogen status the bagit reserved elements
    private List<MetadataItem> getReserved() {
        var ctxBuilder = Bagmatic.emptyBuilder();
        try {
            var result = ctxBuilder.addProfile(Controller.class.getResourceAsStream("/bagit-reserved-profile.json"));
            if (result.success()) {
                var reserved = result.getObject().build().getTagConstraints();
                return reserved.entrySet().stream()
                        .filter(e -> ! "autogenerated".equals(e.getValue().getDescription()))
                        .map(MetadataItem::new).toList();
            } else {
                result.toConsole();
            }
        } catch (Exception e) {
            // put error in console log
            System.err.println("Ex: " + e.getMessage());
            e.printStackTrace();
        }
        return List.of();
    }

    // check and update disabled state of buttons based on application state
    private void updateButtons() {
        boolean empty = payloadTreeView.getRoot().getChildren().size() == 0;
        trashButton.setDisable(empty && cleanMetadata);
        sendButton.setDisable(empty || ! completeMetadata);
        //workflowChoiceBox.setDisable(! (empty && cleanMetadata));
    }

    /* 
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
    */

    private WorkSpec loadWork() {
        String dispatchAddr = dispatchField.getText();
        List<Workflow> workflows = new ArrayList<>();
        // Could be in local filesystem or URL or bundled in resources?
        try {
            if (dispatchAddr.startsWith("http")) {
                // dereference URL
            } else {
                workSpec = JSON.std.beanFrom(WorkSpec.class, new FileInputStream(dispatchAddr));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return workSpec;
    }

    private List<String> getWorkflowIds(String dispatcherUrl) throws IOException {
        List<String> wfIdList = null;
        if (dispatcherUrl == null) {
            wfIdList = JSON.std.listOfFrom(String.class, getClass().getResourceAsStream("/" + "work" + ".json"));
        } else {
            try (InputStream in = new URL(dispatcherUrl).openConnection().getInputStream()) {
                wfIdList = JSON.std.listOfFrom(String.class, in);
            }
        }
        return wfIdList;
    }

    private void chooseLocalDir(TextField field) {
        var chooser = new DirectoryChooser();
        File dir = chooser.showDialog(null);
        try {
           field.setText(dir.getCanonicalPath());
        } catch (Exception e) {}
    }

     private void chooseLocalFile(TextField field) {
        var chooser = new FileChooser();
        File log = chooser.showOpenDialog(null);
        try {
            field.setText(log.getCanonicalPath());
        } catch (Exception e) {}
    }

    /* 
    private void generateBagName(String generator) {
        bagName = "transfer." + counter++;
    }
    */

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
