/**
 * Copyright 2014 MIT Libraries
 * Licensed under: http://www.apache.org/licenses/LICENSE-2.0
 */
package edu.mit.lib.handbag;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;

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
import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;

import org.controlsfx.control.PropertySheet;

import com.fasterxml.jackson.jr.ob.JSON;

import edu.mit.lib.bagit.Filler;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import javafx.scene.control.Alert.AlertType;

public class Controller {

    @FXML private Label workflowLabel;
    @FXML private ChoiceBox<Workflow> workflowChoiceBox;
    @FXML private Label bagLabel;
    @FXML private Label bagSizeLabel;
    @FXML private Label bagCountLabel;
    @FXML private Button sendButton;
    @FXML private Button trashButton;
    @FXML private TreeView<PathRef> payloadTreeView;
    @FXML private PropertySheet metadataPropertySheet;

    // flags for state of metadata editing
    // - clean means no edits have been performed
    // - gooey means at least one sticky field has been (re)assigned
    // - complete means all non-optional properties have values
    private boolean cleanMetadata = true;
    private boolean gooeyMetadata = false;
    private boolean completeMetadata = false;
    private long bagSize = 0L;
    private long maxBagSize = 10000000000L;
    private String bagName;
    private int counter = 0;
    private String agent = "anon";
    private Map<String, String> appProps;
    private StringBuilder relPathSB;
    private StringBuilder emailBody;

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
                        maxBagSize = newSel.getMaxBagSize();
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

        TreeItem<PathRef> rootItem = new TreeItem<>(new PathRef("", Paths.get("data")));
        rootItem.setExpanded(true);
        payloadTreeView.setRoot(rootItem);
        payloadTreeView.setOnDragOver(event -> {
         if (event.getGestureSource() != payloadTreeView &&
             event.getDragboard().getFiles().size() > 0) {
             event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
         }
         event.consume();
        });
        payloadTreeView.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.getFiles().size() > 0) {
                for (File dragFile : db.getFiles()) {
                    if (dragFile.isDirectory()) {
                        // explode directory and add expanded relative paths to tree
                        relPathSB = new StringBuilder();
                        try {
                            Files.walkFileTree(dragFile.toPath(), new SimpleFileVisitor<Path>() {
                                @Override
                                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                                    relPathSB.append(dir.getFileName()).append("/");
                                    return FileVisitResult.CONTINUE;
                                }
                                @Override
                                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                    bagSize += Files.size(file);
                                    if (bagSize > maxBagSize) {
                                        alert("Bag is too big.");
                                        bagSize -= Files.size(file);
                                        return FileVisitResult.TERMINATE;
                                    } else {
                                        payloadTreeView.getRoot().getChildren().add(new TreeItem<>(new PathRef(relPathSB.toString(), file)));
                                        return FileVisitResult.CONTINUE;
                                    }
                                }
                            });
                        } catch (IOException ioe) {}
                    } else {
                        bagSize += dragFile.length();
                        if (bagSize > maxBagSize) {
                            alert("Bag is too big. Maximum bag size is " + scaledSize(maxBagSize, 0) + ".");
                            bagSize -= dragFile.length();
                            break;
                        } else {
                            payloadTreeView.getRoot().getChildren().add(new TreeItem<>(new PathRef("", dragFile.toPath())));
                        }
                    }
                }
                bagSizeLabel.setText(scaledSize(bagSize, 0));
                success = true;
                updateButtons();
            }
            event.setDropCompleted(success);
            event.consume();
        });

        metadataPropertySheet.setModeSwitcherVisible(false);
        metadataPropertySheet.setDisable(false);
        metadataPropertySheet.addEventHandler(KeyEvent.ANY, event -> {
            checkComplete(metadataPropertySheet.getItems());
            event.consume();
        });
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
            String creatorEmail = "";
            String pkgFormat = workflowChoiceBox.getValue().getPackageFormat();
            boolean localDest = destUri.getScheme().startsWith("file");
            Path destDir = Paths.get(destUri);
            Filler filler = localDest ? new Filler(destDir.resolve(bagName)) : new Filler();
            // add payload files
            for (TreeItem ti : payloadTreeView.getRoot().getChildren()) {
                PathRef pr = (PathRef)ti.getValue();
                if (pr.getRelPath().length() > 0) {
                    filler.payload(pr.getRelPath() + pr.getPath().getFileName(), pr.getPath());
                } else {
                    filler.payload(pr.getPath());
                }
            }
            // add metadata (currently only bag-info properties supported)
            for (PropertySheet.Item mdItem : metadataPropertySheet.getItems()) {
                MetadataItem item = (MetadataItem)mdItem;
                if (item.getValue() != null && item.getValue().length() > 0) {
                    if (item.getRealName().equals("Bag-Creator")) {
                        creatorEmail = item.getValue();
                    }
                    filler.metadata(item.getRealName(), item.getValue());
                }
            }
            // add bag size and manifest contents to email body
            emailBody = new StringBuilder();
            emailBody.append("Bag size: ").append(bagSizeLabel.getText()).append("\n\n");
            emailBody.append("Bag contents:\n");
            filler.getManifest().forEach((string) -> { 
                emailBody.append(string).append("\n");
            });
            
            if (localDest) {
                if ("uncompressed".equals(pkgFormat)) {
                    filler.toDirectory();
                } else {
                    filler.toPackage(pkgFormat);
                }
            } else {
                // send to URL - TODO
            }
            sendEmail(workflowChoiceBox.getValue().getDestinationEmail(), 
                    "no-reply@mit.edu", creatorEmail, bagName, emailBody.toString());
            // popup notification of successful bag transmission
            alert("Bag " + bagName + " successfully transmitted " + 
                    "to " + workflowChoiceBox.getValue().getDestinationName() + ".");
            counter++;
            reset(true);
        } catch (IOException | URISyntaxException exp) {
            alert("Bag submission error. Do you have access to the destination?");
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
        bagCountLabel.setText(counter + " bags transmitted");
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
        for (MetadataSpec spec : wf.getMetadata()) {
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
        Date timestamp = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
        String ts = sdf.format(timestamp);
        bagName = generator + "_" + ts;
//        bagName = generator + "-" + counter++;
    }

    private void sendEmail(String to, String from, String cc, String bag, String body) {
        String host = "outgoing.mit.edu";
        Properties properties = System.getProperties();
        properties.setProperty("mail.smtp.host", host);
        Session session = Session.getDefaultInstance(properties);
        String dest = workflowChoiceBox.getValue().getDestinationName();
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO,
                    new InternetAddress(to));
            message.addRecipient(Message.RecipientType.CC, new InternetAddress(
            cc));
            message.setSubject("Bag " + bag + " has been submitted to " + dest + " by " + cc);
            message.setText(body);

            Transport.send(message);
        } catch (MessagingException mex) {
            mex.printStackTrace();
        }
    }
    
    private void alert(String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Information Dialog");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    static class PathRef {
        private String relPath;
        private Path path;

        public PathRef(String relPath, Path path) {
            this.relPath = relPath;
            this.path = path;
        }

        public String getRelPath() {
            return relPath;
        }

        public Path getPath() {
            return path;
        }

        public String toString() {
            return relPath + path.toFile().getName();
        }
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
