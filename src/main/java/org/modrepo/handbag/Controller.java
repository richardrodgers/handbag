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
import javafx.scene.web.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.Callback;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import org.controlsfx.control.CheckComboBox;
import org.controlsfx.control.PropertySheet;
import org.controlsfx.control.PropertySheet.Item;
//import org.controlsfx.control.textfield.TextFields;
import org.controlsfx.property.editor.Editors;

import org.modrepo.packr.Bag;
import org.modrepo.packr.BagBuilder;
import org.modrepo.packr.Serde;
import org.modrepo.packr.BagBuilder.EolRule;

import org.modrepo.bagmatic.Bagmatic;
import org.modrepo.bagmatic.ContextBuilder;
import org.modrepo.bagmatic.impl.profile.BagitProfile;
import org.modrepo.bagmatic.impl.profile.BagitTagConstraint;
import org.modrepo.bagmatic.model.Result;

import org.modrepo.handbag.model.JobSpec;
import org.modrepo.handbag.model.WorkSpec;
import static org.modrepo.handbag.HttpAccess.*;

public class Controller {

    @FXML private AccordionDP settings;
    @FXML private TextField bagNameField;
    @FXML private TextField destField;
    @FXML private Button destButton;
    @FXML private ChoiceBox<String> pkgFormats;
    @FXML private CheckBox resMdBox;
    @FXML private CheckBox retainMdBox;
    @FXML private TextField profileField;
    @FXML private Button profileButton;
    @FXML private Label workLabel;
    @FXML private Label jobNameLabel;
    @FXML private TextField dispatchField;
    @FXML private Button workButton;
    @FXML private ChoiceBox<String> activeJobBox;
    @FXML private CheckBox mapPayload;
    @FXML private CheckBox metaPayload;
    @FXML private TextField logField;
    @FXML private Button logBrowse;
    @FXML private CheckBox logAppend;
    @FXML private CheckComboBox<String> chksumAlgs;
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
    @FXML private Button logTopButton;
    @FXML private Button logBackButton;
    @FXML private ListView<String> fetchView;
    @FXML private WebView manualView;
    
    // flags for state of metadata editing
    // - clean means no edits have been performed
    // - complete means all non-optional properties have values
    private boolean cleanMetadata = true;
    private boolean completeMetadata = false;
    private long bagSize = 0L;
    private int counter = 1;
    private final Image dirIcon = new Image(getClass().getResourceAsStream("/Folder.png"));
    private final Image refIcon = new Image(getClass().getResourceAsStream("/Anchor.png"));
    private WorkSpec workSpec;
    private ContextBuilder cbuilder = Bagmatic.platformBuilder();
    private Map<String, BagitProfile> loadedProfiles = new HashMap<>();
    private String curProfile = null;
    private List<MetadataItem> addedItems = new ArrayList<>();
    private Logger logger = new Logger();
    private List<String> chksums = List.of("sha512", "sha256", "sha1", "md5");

    public void initialize() {

        // Basic settings
        resMdBox.setIndeterminate(false);

        pkgFormats.getItems().addAll("zip", "tar", "tgz", "none");
        pkgFormats.setValue("zip");

         bagNameField.setOnAction(act -> {
            var name = bagNameField.getText();
            bagLabel.setText(generateBagName(name.isEmpty() ? "Bag" : name));
        });

        destButton.setOnAction(a -> {
            if (destField.getText() == null ||
                destField.getText().length() == 0) {
                if (chooseLocalDir(destField)) {
                    destButton.setText("Clear");
                }
            } else if (destButton.getText().equals("Clear")) {
                destField.clear();
                destButton.setText("Browse");
            }
        });

        destField.setOnAction(act -> {
            if (destButton.getText().equals("Browse")) {
                destButton.setText("Clear");
            }
        });

        resMdBox.setSelected(true);
        retainMdBox.setSelected(true);

        retainMdBox.selectedProperty().addListener(
            (ov, oldVal, newVal) -> {
                addedItems.clear();
        });

        //profileField = TextFields.createClearableTextField();
        profileButton.setOnAction(a -> {
            if (profileField.getText() == null ||
                profileField.getText().length() == 0) {
                if (chooseLocalFile(profileField, true)) {
                    profileButton.setText("Load");
                }
            } else if (profileField.getText().length() > 0 &&
                       profileButton.getText().equals("Load")) {
                loadProfile();
                profileButton.setText("Clear");
            } else if (profileButton.getText().equals("Clear")) {
                profileField.clear();
                profileButton.setText("Browse");
            }
        });

        profileField.setOnAction(act -> {
            if (profileButton.getText().equals("Browse")) {
                profileButton.setText("Load");
            }
        });

        // Feature settings
        workButton.setOnAction(a -> {
            if (dispatchField.getText() == null ||
                dispatchField.getText().length() == 0) {
                if (chooseLocalFile(dispatchField, true)) {
                    workButton.setText("Load");
                }
            } else if (dispatchField.getText().length() > 0 &&
                       workButton.getText().equals("Load")) {
                loadWork();
                workButton.setText("Clear");
            } else if (workButton.getText().equals("Clear")) {
                dispatchField.clear();
                workButton.setText("Browse");
            }
        });

        dispatchField.setOnAction(act -> {
            if (workButton.getText().equals("Browse")) {
                workButton.setText("Load");
            }
        });
    
        activeJobBox.getSelectionModel().selectedIndexProperty().addListener(
            (ov, oldVal, newVal) -> {
                var newJob = activeJobBox.getItems().get(newVal.intValue());
                if (oldVal.intValue() < 0) {
                    // job box uninitialized - just set first profile
                    curProfile = newJob;
                } else {
                    curProfile = newJob;
                }
                populateMetadataEditor();
                jobNameLabel.setText(newJob);
        });

        logBrowse.setOnAction(e -> chooseLocalDir(logField));
        logAppend.setSelected(true);

        // Advanced settings
        ObservableList<String> algs = FXCollections.observableArrayList();
        algs.addAll(chksums);
        chksumAlgs.getItems().addAll(algs);
        chksumAlgs.getCheckModel().check(0); // default to sha512

        textEncs.getItems().addAll("UTF-8", "UTF-16");
        textEncs.setValue("UTF-8");

        var rules = Arrays.asList(BagBuilder.EolRule.values()).stream()
                    .map(r -> r.toString()).toList();
        eolRules.getItems().addAll(rules);
        eolRules.setValue("SYSTEM");

        // Information settings
        settings.setExpandedPane(basicPane);

        Image wfIm = new Image(getClass().getResourceAsStream("/SiteMap.png"), 90, 90, false, false);
        workLabel.setGraphic(new ImageView(wfIm));
        workLabel.setContentDisplay(ContentDisplay.BOTTOM);

        //destField.setOnAction(e -> reset(false));

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
        var bagText = bagNameField.getText();
        bagLabel.setText(bagText.isEmpty() ? "Bag" : generateBagName(bagText));

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
            } else if (mdItem.isPreset()) {
                var editor = Editors.createTextEditor(mdItem);
                editor.getEditor().setDisable(true);
                return editor;
            } else {
                return Editors.createTextEditor(mdItem);
            }
        });
        // add in reserved MD if checked
        if (resMdBox.isSelected()) {
            var reserved = getReserved();
            metadataPropertySheet.getItems().addAll(reserved);
            curProfile = "reserved";
        } else {
            curProfile = "platform";
        }

       // repMetaButton.setOnAction(e -> repeatMetaElement());
        addMetaButton.setOnAction(e -> {
            var name = addMetaField.getText();
            var item = new MetadataItem(name, new BagitTagConstraint(false, List.of(), true, name));
            if (retainMdBox.isSelected()) {
                addedItems.add(item);
            }
            metadataPropertySheet.getItems().add(item);
            addMetaField.clear();
    });
        
        // log display
        consoleLogLabel.setText(logger.currentEntry());
        logTopButton.setOnAction(e -> consoleLogLabel.setText(logger.currentEntry()));
        logBackButton.setOnAction(e -> consoleLogLabel.setText(logger.previousEntry(consoleLogLabel.getText())));

        try {
            var manPath = Paths.get(getClass().getResource("/manual_en.html").toURI());
            var manMD = new String(Files.readAllBytes(manPath));
            var webEngine = manualView.getEngine();
            webEngine.loadContent(manMD, "text/html");
        } catch (Exception e) {}
    }

    private void showLog(String entry) {
        logger.log(entry);
        consoleLogLabel.setText(logger.currentEntry());
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
                bagSizeLabel.setText(Bag.scaledSize(bagSize, 0));
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
        try {
            var destAddr = destField.getText();
            // refine test
            boolean isLocalDest = ! destAddr.startsWith("http");
            String bagName = bagLabel.getText();
            Path dest = Paths.get(destAddr).resolve(bagName);
            if (! isLocalDest) {
                var tempBag = File.createTempFile(bagName, null);
                tempBag.deleteOnExit();
                dest = tempBag.toPath();
            }
            BagBuilder builder = new BagBuilder(dest,
                          Charset.forName(textEncs.getValue()),
                          EolRule.valueOf(eolRules.getValue()),
                          false,
                          chksumAlgs.getCheckModel().getCheckedIndices().stream()
                          .map(idx -> csAlgoCode(chksums.get(idx)))
                          .toList().toArray(new String[0]));
            // add payload files, tag files
            for (TreeItem<PathRef> ti : payloadTreeView.getRoot().getChildren()) {
                fillPayload(ti, builder);
            }
            for (TreeItem<PathRef> ti : tagTreeView.getRoot().getChildren()) {
                fillTags(ti, builder);
            }
            // add metadata (currently only bag-info properties supported)
            for (PropertySheet.Item mdItem : metadataPropertySheet.getItems()) {
                MetadataItem item = (MetadataItem)mdItem;
                if (item.getValue() != null && item.getValue().length() > 0) {
                    builder.metadata(item.getRealName(), item.getValue());
                }
            }
            var pkgFmt = pkgFormats.getValue();
            if (isLocalDest) {
                // TODO - set notime param via config
                switch (pkgFmt) {
                    case "none" -> builder.build(); // just finish in place
                    default -> Serde.toPackage(builder.build(), pkgFmt, false, Optional.empty());
                }
            } else {
                // send to URL - packaged bag only
                var destUri = new URL(destAddr).toURI();
                var pkgPath = Serde.toPackage(builder.build(), pkgFmt, false, Optional.empty());
                postPackage(destUri, pkgPath, pkgFmt);
            }
            showLog("Bag '" + bagName + "' transmitted");
            logger.logTransfer(Path.of(logField.getText()), logAppend.isSelected(),
                               bagName, bagSize, destField.getText());
            reset(true);
        } catch (IOException | URISyntaxException exp) {
            // test only
            showLog("Exception thrown: " + exp.getMessage());
            exp.printStackTrace();
        }
    }

    private void fillPayload(TreeItem<PathRef> ti, BagBuilder builder) throws IOException, URISyntaxException {
        PathRef pr = ti.getValue();
        PathRef parent = ti.getParent().getValue();
        if (pr.isFolder()) {
            for (TreeItem<PathRef> child : ti.getChildren()) {
                fillPayload(child, builder);
            }
        } else {
            Optional<URI> location = pr.getLocation();
            if (location.isEmpty()) {
                //System.out.println("relPath: " + parent.getRelPath());
                if (mapPayload.isSelected()) {
                    builder.property("source-map.txt", pr.getSourcePath().toString(), makeRelPath(parent, pr));
                }
                if (metaPayload.isSelected()) {
                    builder.property("source-meta.txt", sourceMetadata(pr.getSourcePath()), makeRelPath(parent, pr));
                }
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
        }
    }

    private void fillTags(TreeItem<PathRef> ti, BagBuilder builder) throws IOException {
        PathRef pr = ti.getValue();
        PathRef parent = ti.getParent().getValue();
        if (pr.isFolder()) {
            for (TreeItem<PathRef> child : ti.getChildren()) {
                fillTags(child, builder);
            }
        } else {
            builder.tag(makeRelPath(parent, pr), pr.getSourcePath());
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

    private String sourceMetadata(Path source) {
        try {
            var basicAttrs = Files.getFileAttributeView(source, BasicFileAttributeView.class).readAttributes();
            FileTime created = basicAttrs.creationTime();
            FileTime modified = basicAttrs.lastModifiedTime();
            var attrs = String.format("Created>%s|Modified>%s", shortTime(created), shortTime(modified));
            return attrs;
        } catch (Exception e) {
            showLog("Can't read source file attributes");
        }
        return "no data";
    }

    private static LocalDateTime shortTime(FileTime ftime) {
        var local = ftime.toInstant().truncatedTo(ChronoUnit.SECONDS);
        return LocalDateTime.ofInstant(local, ZoneOffset.UTC);
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

    // reset payload, metadata etc to ready state after a transmission or trashing
    private void reset(boolean transmitted) {
        payloadTreeView.getRoot().getChildren().clear();
        tagTreeView.getRoot().getChildren().clear();
        fetchView.getItems().clear();
        populateMetadataEditor();
        //ObservableList<PropertySheet.Item> items = metadataPropertySheet.getItems();
        //List<PropertySheet.Item> saveItems = new ArrayList<>();
        //saveItems.addAll(items);
        //items.clear();
        cleanMetadata = true;
        completeMetadata = ! metadataPropertySheet.getItems().stream()
                            .anyMatch(i -> ((MetadataItem)i).getSpec().isRequired());
        //metadataPropertySheet.getItems().addAll(getReserved());
        updateButtons();
        bagSize = 0L;
        bagSizeLabel.setText("[empty]");
        //generateBagName(workflowChoiceBox.getValue().getBagNameGenerator());
        if (transmitted) {
            // retire bag name if not template
            counter++;
            if (! bagNameField.getText().contains("$")) {
                bagNameField.clear();
            }
        }
        var bagText = bagNameField.getText();
        bagLabel.setText(bagText.isEmpty() ? "Bag" : generateBagName(bagText));
    }

    // load and filter by autogen status the bagit reserved elements
    private List<MetadataItem> getReserved() {
        var result = cbuilder.merge("reserved", "classpath:/bagit-reserved-profile.json");
        if (result.success()) {
            var reserved = result.getObject().bagInfo;
            curProfile = "reserved";
            loadedProfiles.put(curProfile, result.getObject());
            return reserved.entrySet().stream()
                                      .filter(e -> ! "autogenerated".equals(e.getValue().getDescription()))
                                      .map(MetadataItem::new).toList();
        } else {
            result.toConsole();
        }
        return List.of();
    }

    private void populateMetadataEditor() {
        var curProf = loadedProfiles.get(curProfile);
        var mdList = curProf.bagInfo.entrySet().stream()
                            .filter(e -> ! "autogenerated".equals(e.getValue().getDescription()))
                            .map(MetadataItem::new).toList();
        metadataPropertySheet.getItems().clear();
        metadataPropertySheet.getItems().addAll(mdList);
        metadataPropertySheet.getItems().addAll(addedItems);
    }

    private void loadProfile() {
        //var result = getProfile(profileField.getText());
        var result = cbuilder.merge("first", profileField.getText());
        if (result.success()) {
            var cntList = result.getObject().bagInfo
                                .entrySet().stream()
                                .map(MetadataItem::new).toList();
            metadataPropertySheet.getItems().addAll(cntList);
        } else {
            showLog(result.getErrors().get(0));
        }
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
        // Could be in local filesystem or URL or bundled in resources?
        var result = getWork(dispatchAddr);
        if (result.success()) {
            var workSpec = result.getObject();
            // eagerly load job profiles from spec
            for (JobSpec jspec: workSpec.jobs) {
               loadJob(jspec);
            }
            // populate active jobs choicebox
            activeJobBox.getItems().addAll(
                        workSpec.jobs.stream()
                        .map(j -> j.name)
                        .toList().toArray(new String[0]));
        } else {
            showLog(result.getErrors().get(0));
        }
        return workSpec;
    }

    private void loadJob(JobSpec spec) {
        //var result = getProfile(spec.profileAddr);
        var result = cbuilder.merge(spec.name, profileField.getText());
        if (result.success()) {
            loadedProfiles.put(spec.name, result.getObject());
        } else {
            showLog(result.getErrors().get(0));
        }
    }

    private boolean chooseLocalDir(TextField field) {
        var chooser = new DirectoryChooser();
        File dir = chooser.showDialog(null);
        if (dir != null) {
            try {
                field.setText(dir.getCanonicalPath());
            } catch (Exception e) {
                showLog("Unable to parse directory");
                dir = null;
            }
        }
        return dir != null;
    }

     private boolean chooseLocalFile(TextField field, boolean filterJson) {
        var chooser = new FileChooser();
        if (filterJson) {
            chooser.getExtensionFilters().add(
                new ExtensionFilter("JSON documents", "*.json"));
        }
        File file = chooser.showOpenDialog(null);
        if (file != null) {
            try {
                field.setText(file.getCanonicalPath());
            } catch (Exception e) {
                showLog("Unable to parse file name");
                file = null;
            }
        }
        return file != null;
    }

    private String generateBagName(String template) {
        var today = LocalDateTime.now();
        var curJob = activeJobBox.getSelectionModel().getSelectedItem();
        if (curJob == null) curJob = "job";
        return template.replace("$c", String.valueOf(counter)
                       .replace("$y", String.valueOf(today.getYear()))
                       .replace("$m", String.valueOf(today.getMonthValue()))
                       .replace("$d", String.valueOf(today.getDayOfMonth()))
                       .replace("$j", curJob)
        );
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
            return (relPath != null) ? relPath + "/" + fileName : fileName;
        }

        public String getPayloadPath() {
            return "data" + "/" + getFullPath();
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
                    bagSizeLabel.setText(Bag.scaledSize(bagSize, 0));
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
                var fetchDialog = new TextInputDialog("Fetch URI");
                fetchDialog.setTitle("Fetch Link");
                fetchDialog.showAndWait()
                .ifPresent(uri -> {
                    TreeItem<PathRef> item = cell.getTreeItem();
                    PathRef pr = item.getValue();
                    try {
                        if (pr.getLocation().isEmpty()) {
                            pr.setLocation(uri);
                            item.setGraphic(new ImageView(refIcon));
                            // subtract bytes from payload count - should include only in-bag files
                            var fetchSize = Files.size(pr.getSourcePath());
                            bagSize -= fetchSize;
                            bagSizeLabel.setText(Bag.scaledSize(bagSize, 0));
                            // add line to fetch list
                            var fetchStr = String.format("%s %d %s", uri, fetchSize, pr.getPayloadPath());
                            fetchView.getItems().add(fetchStr);
                        }
                    } catch (Exception exp) {}
                });
            }
        });
        return fetchItem;
    }
}
