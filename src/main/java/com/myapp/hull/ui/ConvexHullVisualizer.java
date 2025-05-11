package com.myapp.hull.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.myapp.hull.Point2D;
import com.myapp.hull.PointReader;
import com.myapp.hull.PointWriter;
import com.myapp.hull.algorithms.ChansAlgorithm;
import com.myapp.hull.algorithms.ConvexHullAlgorithm;
import com.myapp.hull.algorithms.GrahamScan;
import com.myapp.hull.algorithms.JarvisMarch;
import com.myapp.hull.algorithms.MergeHullAlgorithm;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ConvexHullVisualizer extends Application {
    // Canvas dimensions and scaling factors
    private static final double CANVAS_WIDTH = 800;
    private static final double CANVAS_HEIGHT = 600;
    private static final double PADDING = 40;
    
    // UI Colors
    private static final Color BACKGROUND_COLOR = Color.rgb(20, 20, 20);
    private static final Color PRIMARY_COLOR = Color.rgb(220, 20, 20);
    private static final Color SECONDARY_COLOR = Color.rgb(180, 20, 20);
    private static final Color TEXT_COLOR = Color.rgb(240, 240, 240);
    private static final Color CANVAS_BG_COLOR = Color.WHITE;
    private static final Color POINT_COLOR = Color.rgb(40, 40, 40);
    private static final Color HULL_COLOR = Color.rgb(220, 20, 20);
    
    private Canvas canvas;
    private GraphicsContext gc;
    private List<Point2D> points = new ArrayList<>();
    private List<Point2D> hull = new ArrayList<>();
    private ConvexHullAlgorithm currentAlgorithm = new GrahamScan();
    private Stage primaryStage;
    private boolean animateHull = false;
    private Timeline animation;
    private double animationSpeed = 100; // ms per frame
    private double pointRadius = 4;
    private Label statusLabel;
    private List<List<Point2D>> partialHulls = new ArrayList<>();
    private boolean isChanAlgorithm = false;
    private int currentChanStep = 0;
    private static final int CHAN_STEP_GRAHAM = 0;
    private static final int CHAN_STEP_JARVIS = 1;
    private static final int CHAN_STEP_FINAL = 2;
    private static final int MERGE_STEP_SPLIT = 0;
    private static final int MERGE_STEP_SUBHULLS = 1;
    private static final int MERGE_STEP_MERGE = 2;
    private int currentMergeStep = 0;
    private List<List<Point2D>> mergeSteps = new ArrayList<>();
    
    private static final String LOGO_PATH = "/images/logo.jpg";
    
    // Scale from canvas coordinates to point coordinates
    private double unscaleX(double x) {
        double minX = points.stream().mapToDouble(Point2D::getX).min().orElse(0);
        double maxX = points.stream().mapToDouble(Point2D::getX).max().orElse(1);
        double range = maxX - minX;
        if (range == 0) range = 1;
        return minX + (x - PADDING) * range / (CANVAS_WIDTH - 2 * PADDING);
    }
    
    private double unscaleY(double y) {
        double minY = points.stream().mapToDouble(Point2D::getY).min().orElse(0);
        double maxY = points.stream().mapToDouble(Point2D::getY).max().orElse(1);
        double range = maxY - minY;
        if (range == 0) range = 1;
        return minY + (CANVAS_HEIGHT - y - PADDING) * range / (CANVAS_HEIGHT - 2 * PADDING);
    }
    
    // Scale from point coordinates to canvas coordinates
    private double scaleX(double x) {
        double minX = points.stream().mapToDouble(Point2D::getX).min().orElse(0);
        double maxX = points.stream().mapToDouble(Point2D::getX).max().orElse(1);
        double range = maxX - minX;
        if (range == 0) range = 1;
        return PADDING + (x - minX) * (CANVAS_WIDTH - 2 * PADDING) / range;
    }
    
    private double scaleY(double y) {
        double minY = points.stream().mapToDouble(Point2D::getY).min().orElse(0);
        double maxY = points.stream().mapToDouble(Point2D::getY).max().orElse(1);
        double range = maxY - minY;
        if (range == 0) range = 1;
        return CANVAS_HEIGHT - PADDING - (y - minY) * (CANVAS_HEIGHT - 2 * PADDING) / range;
    }
    
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        
        // Set application icon
        try {
            java.net.URL logoUrl = getClass().getResource(LOGO_PATH);
            if (logoUrl != null) {
                Image icon = new Image(logoUrl.toExternalForm());
                primaryStage.getIcons().add(icon);
            } else {
                // If resource loading fails, try loading as a file
                File logoFile = new File("src/main/resources" + LOGO_PATH);
                if (logoFile.exists()) {
                    Image icon = new Image(logoFile.toURI().toString());
                    primaryStage.getIcons().add(icon);
                }
            }
        } catch (Exception e) {
            System.err.println("Could not load application icon: " + e.getMessage());
        }
        
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: rgb(20, 20, 20);");
        
        // Create header with logo
        HBox header = createHeader();
        root.setTop(header);
        
        // Create control panel
        VBox controlPanel = createControlPanel();
        root.setLeft(controlPanel);
        
        // Create canvas area with shadow effect
        VBox canvasContainer = new VBox();
        canvasContainer.setPadding(new Insets(15));
        canvasContainer.setAlignment(Pos.CENTER);
        canvasContainer.setStyle("-fx-background-color: rgb(30, 30, 30);");
        
        canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        gc = canvas.getGraphicsContext2D();
        
        // Add drop shadow to canvas
        DropShadow dropShadow = new DropShadow();
        dropShadow.setColor(Color.rgb(0, 0, 0, 0.5));
        dropShadow.setRadius(8);
        canvas.setEffect(dropShadow);
        
        canvasContainer.getChildren().add(canvas);
        root.setCenter(canvasContainer);
        
        // Create bottom section with status bar and footer
        VBox bottomSection = new VBox();
        bottomSection.setStyle("-fx-background-color: rgb(30, 30, 30);");
        
        // Add status bar
        HBox statusBar = createStatusBar();
        bottomSection.getChildren().add(statusBar);
        
        // Add footer
        HBox footer = createFooter();
        bottomSection.getChildren().add(footer);
        
        root.setBottom(bottomSection);
        
        // Add mouse click handler
        canvas.setOnMouseClicked(e -> {
            double x = e.getX();
            double y = e.getY();
            Point2D newPoint = new Point2D(
                unscaleX(x),
                unscaleY(y)
            );
            points.add(newPoint);
            updateStatus("Added point at (" + String.format("%.2f", newPoint.getX()) + 
                         ", " + String.format("%.2f", newPoint.getY()) + ")");
            drawPoints();
        });
        
        // Initial canvas setup
        clearCanvas();
        
        Scene scene = new Scene(root, 1200, 800);
        
        // Apply CSS styles directly to the scene - no external CSS file
        applyStyles(scene);
        
        primaryStage.setTitle("Premium Convex Hull Visualizer");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false); // Make window size fixed
        primaryStage.show();
    }
    
    // Method to apply all styles directly instead of using external CSS
    private void applyStyles(Scene scene) {
        // Apply styles to Alert dialogs
        scene.getRoot().setStyle("-fx-font-family: 'System';");
    }
    
    private HBox createHeader() {
        HBox header = new HBox(10);
        header.setPadding(new Insets(10, 15, 10, 15));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: rgb(30, 30, 30);");
        
        // Add logo
        try {
            // First try loading as a resource
            java.net.URL logoUrl = getClass().getResource(LOGO_PATH);
            Image logoImage;
            
            if (logoUrl != null) {
                logoImage = new Image(logoUrl.toExternalForm());
            } else {
                // If resource loading fails, try loading as a file
                File logoFile = new File("src/main/resources" + LOGO_PATH);
                if (!logoFile.exists()) {
                    throw new IllegalStateException("Logo file not found: " + logoFile.getAbsolutePath());
                }
                logoImage = new Image(logoFile.toURI().toString());
            }
            
            ImageView logoView = new ImageView(logoImage);
            logoView.setFitHeight(40);
            logoView.setPreserveRatio(true);
            header.getChildren().add(logoView);
        } catch (Exception e) {
            System.err.println("Could not load logo: " + e.getMessage());
            e.printStackTrace();
            
            // Add a fallback text logo if image loading fails
            Label textLogo = new Label("CH");
            textLogo.setFont(Font.font("System", FontWeight.BOLD, 20));
            textLogo.setTextFill(PRIMARY_COLOR);
            header.getChildren().add(textLogo);
        }
        
        Label title = new Label("PREMIUM CONVEX HULL VISUALIZER");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));
        title.setTextFill(TEXT_COLOR);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button helpButton = createStyledButton("Help", "question-circle");
        helpButton.setOnAction(e -> showHelp());
        
        header.getChildren().addAll(title, spacer, helpButton);
        
        return header;
    }
    
    private VBox createControlPanel() {
        VBox panel = new VBox(10); // Reduced spacing
        panel.setPadding(new Insets(15)); // Reduced padding
        panel.setMinWidth(250); // Reduced width
        panel.setMaxWidth(250);
        panel.setStyle("-fx-background-color: rgb(30, 30, 30);");
        
        Label panelTitle = new Label("CONTROLS");
        panelTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        panelTitle.setTextFill(TEXT_COLOR);
        
        // Add instructions section
        VBox instructionsBox = new VBox(10);
        instructionsBox.setPadding(new Insets(10));
        instructionsBox.setStyle("-fx-background-color: rgb(40, 40, 40); -fx-background-radius: 5;");
        
        Label instructionsTitle = new Label("Instructions");
        instructionsTitle.setFont(Font.font("System", FontWeight.BOLD, 12));
        instructionsTitle.setTextFill(TEXT_COLOR);
        
        Label clickInstruction = new Label("• Click on the canvas to add points");
        clickInstruction.setFont(Font.font("System", 10));
        clickInstruction.setTextFill(TEXT_COLOR);
        
        Label dragInstruction = new Label("• Drag points to move them");
        dragInstruction.setFont(Font.font("System", 10));
        dragInstruction.setTextFill(TEXT_COLOR);
        
        Label algorithmInstruction = new Label("• Select an algorithm and click 'Compute Hull'");
        algorithmInstruction.setFont(Font.font("System", 10));
        algorithmInstruction.setTextFill(TEXT_COLOR);
        
        instructionsBox.getChildren().addAll(instructionsTitle, clickInstruction, dragInstruction, algorithmInstruction);
        
        // Algorithm selection with description
        Label algoLabel = createStyledLabel("Algorithm:");
        ComboBox<String> algorithmCombo = new ComboBox<>();
        algorithmCombo.getItems().addAll(
            "Graham Scan",
            "Chan's Algorithm",
            "Merge Hull",
            "Jarvis March"
        );
        algorithmCombo.setValue("Graham Scan");
        algorithmCombo.setMaxWidth(Double.MAX_VALUE);
        algorithmCombo.setStyle("-fx-background-color: rgb(240, 240, 240);");
        
        // Add algorithm description label
        Label algoDescription = new Label();
        algoDescription.setWrapText(true);
        algoDescription.setMaxWidth(200);
        algoDescription.setFont(Font.font("System", 10));
        algoDescription.setTextFill(TEXT_COLOR);
        algoDescription.setStyle("-fx-padding: 5 0 10 0;");
        
        // Update description based on selected algorithm
        algorithmCombo.setOnAction(e -> {
            String selected = algorithmCombo.getValue();
            switch (selected) {
                case "Graham Scan":
                    currentAlgorithm = new GrahamScan();
                    algoDescription.setText("Graham Scan: Efficient O(n log n) algorithm that sorts points by angle and builds the hull incrementally. Best for general use cases.");
                    break;
                case "Chan's Algorithm":
                    currentAlgorithm = new ChansAlgorithm();
                    algoDescription.setText("Chan's Algorithm: Optimal O(n log h) algorithm that combines Graham Scan with divide-and-conquer. Best for large point sets with small hulls.");
                    break;
                case "Merge Hull":
                    currentAlgorithm = new MergeHullAlgorithm();
                    algoDescription.setText("Merge Hull: Divide-and-conquer approach that splits points into subsets and merges their hulls. Good for parallel processing.");
                    break;
                case "Jarvis March":
                    currentAlgorithm = new JarvisMarch();
                    algoDescription.setText("Jarvis March: Simple O(nh) algorithm that wraps around the points like a gift. Good for small point sets with few hull points.");
                    break;
            }
            updateStatus("Algorithm changed to " + selected);
            if (!hull.isEmpty()) {
                computeAndDisplayHull();
            }
        });
        
        // Set initial description
        algoDescription.setText("Graham Scan: Efficient O(n log n) algorithm that sorts points by angle and builds the hull incrementally. Best for general use cases.");
        
        // Animation controls
        Label animLabel = createStyledLabel("Animation Speed:");
        Slider speedSlider = createStyledSlider(50, 1100, animationSpeed);
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            animationSpeed = newVal.doubleValue();
            if (animation != null && animation.getStatus() == Animation.Status.RUNNING) {
                animation.stop();
                animateHullConstruction();
            }
        });
        
        // Point size controls
        Label sizeLabel = createStyledLabel("Point Size:");
        Slider sizeSlider = createStyledSlider(2, 8, pointRadius);
        sizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            pointRadius = newVal.doubleValue();
            drawPoints();
            if (!hull.isEmpty()) {
                drawHull();
            }
        });
        
        // Animation toggle
        CheckBox animateCheck = new CheckBox("Animate Hull Construction");
        animateCheck.setTextFill(TEXT_COLOR);
        animateCheck.setOnAction(e -> {
            animateHull = animateCheck.isSelected();
            updateStatus("Animation " + (animateHull ? "enabled" : "disabled"));
        });
        
        // Action buttons
        VBox buttonContainer = new VBox(10);
        buttonContainer.setAlignment(Pos.CENTER);
        
        Button generateButton = createActionButton("Generate Random Points", "plus-circle");
        generateButton.setOnAction(e -> generateRandomPoints());
        
        Button loadButton = createActionButton("Load Points", "folder-open");
        loadButton.setOnAction(e -> loadPointsFromFile(primaryStage));
        
        Button saveButton = createActionButton("Save Hull", "save");
        saveButton.setOnAction(e -> saveHullToFile(primaryStage));
        
        Button clearButton = createActionButton("Clear All", "trash");
        clearButton.setOnAction(e -> {
            points.clear();
            hull.clear();
            clearCanvas();
            updateStatus("Canvas cleared");
        });
        
        Button computeButton = createActionButton("Compute Hull", "play");
        computeButton.setOnAction(e -> computeAndDisplayHull());
        
        buttonContainer.getChildren().addAll(
            generateButton, loadButton, saveButton, clearButton, computeButton
        );
        
        // Add all controls to panel
        panel.getChildren().addAll(
            panelTitle,
            createSeparator(),
            instructionsBox,
            createSeparator(),
            algoLabel, algorithmCombo,
            algoDescription,
            createSeparator(),
            animLabel, speedSlider,
            createSeparator(),
            sizeLabel, sizeSlider,
            createSeparator(),
            animateCheck,
            createSeparator(),
            buttonContainer
        );
        
        // Add spacer to push controls to top
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        panel.getChildren().add(spacer);
        
        // Add info section at bottom
        Label infoLabel = new Label("Click on the canvas to add points");
        infoLabel.setTextFill(Color.rgb(180, 180, 180));
        infoLabel.setFont(Font.font("System", 10));
        panel.getChildren().add(infoLabel);
        
        return panel;
    }
    
    private HBox createStatusBar() {
        HBox statusBar = new HBox();
        statusBar.setPadding(new Insets(4, 8, 4, 8)); // Reduced padding
        statusBar.setStyle("-fx-background-color: rgb(40, 40, 40);");
        
        statusLabel = new Label("Ready");
        statusLabel.setTextFill(Color.rgb(200, 200, 200));
        statusLabel.setFont(Font.font("System", 11)); // Reduced font size
        
        statusBar.getChildren().add(statusLabel);
        
        return statusBar;
    }
    
    private void updateStatus(String message) {
        statusLabel.setText(message);
    }
    
    private Label createStyledLabel(String text) {
        Label label = new Label(text);
        label.setTextFill(TEXT_COLOR);
        label.setFont(Font.font("System", 10));
        return label;
    }
    
    private Slider createStyledSlider(double min, double max, double value) {
        Slider slider = new Slider(min, max, value);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit((max - min) / 5);
        slider.setMinorTickCount(4);
        slider.setMaxWidth(Double.MAX_VALUE);
        slider.setStyle("-fx-control-inner-background: rgb(60, 60, 60);");
        return slider;
    }
    
    private Separator createSeparator() {
        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: rgb(60, 60, 60);");
        return separator;
    }
    
    private Button createStyledButton(String text, String iconName) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: rgb(40, 40, 40); -fx-text-fill: white;");
        return button;
    }
    
    private Button createActionButton(String text, String iconName) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefHeight(36);
        button.setStyle(
            "-fx-background-color: rgb(180, 20, 20); " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-cursor: hand;"
        );
        
        // Add hover effect
        button.setOnMouseEntered(e -> 
            button.setStyle(
                "-fx-background-color: rgb(220, 20, 20); " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-cursor: hand;"
            )
        );
        
        button.setOnMouseExited(e -> 
            button.setStyle(
                "-fx-background-color: rgb(180, 20, 20); " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-cursor: hand;"
            )
        );
        
        return button;
    }
    
    private void generateRandomPoints() {
        // Create a custom dialog for number input
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Generate Random Points");
        dialog.setHeaderText("Enter the number of points to generate");
        dialog.getDialogPane().setStyle("-fx-background-color: rgb(40, 40, 40);");

        // Set the button types
        ButtonType generateButtonType = new ButtonType("Generate", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(generateButtonType, ButtonType.CANCEL);

        // Create the number input field
        TextField numberInput = new TextField();
        numberInput.setPromptText("Enter number of points");
        numberInput.setStyle("-fx-background-color: rgb(60, 60, 60); -fx-text-fill: white;");
        
        // Add validation
        numberInput.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                numberInput.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });

        // Create the content
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.setStyle("-fx-background-color: rgb(40, 40, 40);");
        
        Label label = new Label("Number of points:");
        label.setTextFill(Color.WHITE);
        content.getChildren().addAll(label, numberInput);
        
        dialog.getDialogPane().setContent(content);

        // Style the dialog buttons
        dialog.getDialogPane().lookupButton(generateButtonType).setStyle(
            "-fx-background-color: rgb(180, 20, 20); -fx-text-fill: white;"
        );
        dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setStyle(
            "-fx-background-color: rgb(60, 60, 60); -fx-text-fill: white;"
        );

        // Convert the result to a number when the generate button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == generateButtonType) {
                try {
                    return Integer.parseInt(numberInput.getText());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });

        // Show the dialog and process the result
        Optional<Integer> result = dialog.showAndWait();
        result.ifPresent(numPoints -> {
            if (numPoints <= 0) {
                showError("Invalid Input", "Please enter a positive number of points.");
                return;
            }
            
            if (numPoints > 1000) {
                showError("Too Many Points", "Please enter a number less than or equal to 1000.");
                return;
            }
            
            points.clear();
            hull.clear();
            
            for (int i = 0; i < numPoints; i++) {
                double x = Math.random() * 100;
                double y = Math.random() * 100;
                points.add(new Point2D(x, y));
            }
            
            drawPoints();
            updateStatus("Generated " + numPoints + " random points");
        });
    }
    
    private void loadPointsFromFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Points File");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );
        
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try {
                points = PointReader.readPoints(file.getPath());
                hull.clear();
                drawPoints();
                updateStatus("Loaded " + points.size() + " points from " + file.getName());
            } catch (Exception e) {
                showError("Error loading file", e.getMessage());
            }
        }
    }
    
    private List<Point2D> sortHullPointsForRendering(List<Point2D> hullPoints) {
        if (hullPoints.size() <= 2) return hullPoints;
        
        // Find centroid of hull points
        double centerX = 0, centerY = 0;
        for (Point2D p : hullPoints) {
            centerX += p.getX();
            centerY += p.getY();
        }
        centerX /= hullPoints.size();
        centerY /= hullPoints.size();
        
        // Create a copy of hull points for sorting
        List<Point2D> sortedHull = new ArrayList<>(hullPoints);
        
        // Sort by polar angle with respect to centroid
        final double finalCenterX = centerX;
        final double finalCenterY = centerY;
        
        sortedHull.sort((p1, p2) -> {
            double angle1 = Math.atan2(p1.getY() - finalCenterY, p1.getX() - finalCenterX);
            double angle2 = Math.atan2(p2.getY() - finalCenterY, p2.getX() - finalCenterX);
            return Double.compare(angle1, angle2);
        });
        
        return sortedHull;
    }
    
    private void computeAndDisplayHull() {
        if (points.isEmpty()) {
            showError("Error", "No points to compute hull from");
            return;
        }
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Check if we're using Merge Hull Algorithm
            if (currentAlgorithm instanceof MergeHullAlgorithm) {
                MergeHullAlgorithm mergeAlgo = (MergeHullAlgorithm) currentAlgorithm;
                hull = mergeAlgo.computeHull(points);
                mergeSteps = mergeAlgo.getComputationSteps();
                currentMergeStep = 0;
            } else if (currentAlgorithm instanceof ChansAlgorithm) {
                currentChanStep = CHAN_STEP_GRAHAM;
                partialHulls.clear();
                
                // Get the partial hulls from Chan's Algorithm
                ChansAlgorithm chan = (ChansAlgorithm) currentAlgorithm;
                partialHulls = chan.getPartialHulls(points);
                hull = chan.computeHull(points);
            } else {
                hull = currentAlgorithm.computeHull(points);
            }
            
            // Sort hull points for proper visualization
            hull = sortHullPointsForRendering(hull);
            
            long endTime = System.currentTimeMillis();
            
            if (animateHull) {
                if (currentAlgorithm instanceof MergeHullAlgorithm) {
                    animateMergeHull();
                } else if (currentAlgorithm instanceof ChansAlgorithm) {
                    animateChanAlgorithm();
                } else {
                    animateHullConstruction();
                }
                updateStatus("Computing hull with " + hull.size() + " vertices in " + 
                             (endTime - startTime) + "ms (animating)");
            } else {
                drawPoints();
                drawHull();
                updateStatus("Computed hull with " + hull.size() + " vertices in " + 
                             (endTime - startTime) + "ms");
            }
        } catch (Exception e) {
            showError("Error", "Failed to compute hull: " + e.getMessage());
        }
    }
    
    private void saveHullToFile(Stage stage) {
        if (hull.isEmpty()) {
            showError("Error", "No hull to save");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Hull Points");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );
        
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                PointWriter.writePoints(hull, file.getPath());
                updateStatus("Hull points saved to " + file.getName());
            } catch (Exception e) {
                showError("Error", "Failed to save hull points: " + e.getMessage());
            }
        }
    }
    
    private void animateHullConstruction() {
        if (animation != null) {
            animation.stop();
        }
        
        drawPoints(); // Draw base layer
        
        if (hull.isEmpty()) return;
        
        // Create animation timeline
        animation = new Timeline();
        final int[] currentEdge = {0};
        
        KeyFrame frame = new KeyFrame(Duration.millis(animationSpeed), e -> {
            if (currentEdge[0] < hull.size()) {
                // Draw all edges up to current
                gc.setStroke(HULL_COLOR);
                gc.setLineWidth(2.5);
                
                gc.beginPath();
                Point2D first = hull.get(0);
                gc.moveTo(scaleX(first.getX()), scaleY(first.getY()));
                
                for (int i = 1; i <= currentEdge[0]; i++) {
                    Point2D point = hull.get(i % hull.size());
                    gc.lineTo(scaleX(point.getX()), scaleY(point.getY()));
                }
                
                gc.stroke();
                
                // Highlight current vertex
                Point2D currentPoint = hull.get(currentEdge[0] % hull.size());
                gc.setFill(HULL_COLOR);
                double x = scaleX(currentPoint.getX());
                double y = scaleY(currentPoint.getY());
                gc.fillOval(x - pointRadius - 2, y - pointRadius - 2, 
                           (pointRadius + 2) * 2, (pointRadius + 2) * 2);
                
                currentEdge[0]++;
            } else {
                animation.stop();
                // Draw the complete hull
                drawHull();
            }
        });
        
        animation.getKeyFrames().add(frame);
        animation.setCycleCount(hull.size() + 1);
        animation.play();
    }
    
    private void animateChanAlgorithm() {
        if (animation != null) {
            animation.stop();
        }

        drawPoints(); // Draw base layer

        if (hull.isEmpty()) return;

        // Two-phase animation: 1) Show sub-hulls, 2) Animate Jarvis step
        animation = new Timeline();
        final int[] phase = {0}; // 0 = show sub-hulls, 1 = animate Jarvis
        final int[] currentEdge = {0};
        final int subHullFrames = 30; // Show sub-hulls for this many frames

        KeyFrame frame = new KeyFrame(Duration.millis(animationSpeed), e -> {
            if (phase[0] == 0) {
                // Phase 1: Show sub-hulls
                drawPoints();
                drawPartialHulls();
                currentEdge[0]++;
                if (currentEdge[0] >= subHullFrames) {
                    phase[0] = 1;
                    currentEdge[0] = 0;
                }
            } else if (phase[0] == 1) {
                // Phase 2: Animate binary Jarvis step (building the global hull)
                drawPoints();
                drawPartialHulls();
                drawJarvisStepFromSubHulls(currentEdge[0]);
                currentEdge[0]++;
                if (currentEdge[0] > hull.size()) {
                    phase[0] = 2;
                    currentEdge[0] = 0;
                }
            } else {
                // Final phase: Draw the complete hull
                drawPoints();
                drawPartialHulls();
                drawHull();
                animation.stop();
            }
        });

        animation.getKeyFrames().add(frame);
        animation.setCycleCount(Animation.INDEFINITE);
        animation.play();
    }

    // Draws the global hull as it is being constructed from the sub-hulls (binary Jarvis step)
    private void drawJarvisStepFromSubHulls(int step) {
        if (hull.isEmpty() || partialHulls.isEmpty()) return;

        // Draw the global hull up to the current step
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(3.5);
        gc.beginPath();
        Point2D first = hull.get(0);
        gc.moveTo(scaleX(first.getX()), scaleY(first.getY()));
        for (int i = 1; i <= step && i < hull.size(); i++) {
            Point2D point = hull.get(i % hull.size());
            gc.lineTo(scaleX(point.getX()), scaleY(point.getY()));
        }
        gc.stroke();

        // Optionally, highlight the current vertex
        if (step < hull.size()) {
            Point2D currentPoint = hull.get(step % hull.size());
            gc.setFill(Color.BLACK);
            double x = scaleX(currentPoint.getX());
            double y = scaleY(currentPoint.getY());
            gc.fillOval(x - pointRadius - 3, y - pointRadius - 3, (pointRadius + 3) * 2, (pointRadius + 3) * 2);
        }
    }
    
    private void drawPartialHulls() {
        // Draw each partial hull with a different color
        Color[] colors = {
            Color.rgb(220, 20, 20, 0.3),   // Red
            Color.rgb(20, 220, 20, 0.3),   // Green
            Color.rgb(20, 20, 220, 0.3),   // Blue
            Color.rgb(220, 220, 20, 0.3),  // Yellow
            Color.rgb(220, 20, 220, 0.3),  // Magenta
            Color.rgb(20, 220, 220, 0.3)   // Cyan
        };
        
        Color[] edgeColors = {
            Color.rgb(220, 20, 20),   // Red
            Color.rgb(20, 220, 20),   // Green
            Color.rgb(20, 20, 220),   // Blue
            Color.rgb(220, 220, 20),  // Yellow
            Color.rgb(220, 20, 220),  // Magenta
            Color.rgb(20, 220, 220)   // Cyan
        };
        
        for (int i = 0; i < partialHulls.size(); i++) {
            List<Point2D> partialHull = partialHulls.get(i);
            Color hullColor = colors[i % colors.length];
            Color edgeColor = edgeColors[i % edgeColors.length];
            
            if (partialHull.size() > 1) {
                // Draw filled area
                gc.setFill(hullColor);
                gc.beginPath();
                Point2D first = partialHull.get(0);
                gc.moveTo(scaleX(first.getX()), scaleY(first.getY()));
                
                for (int j = 1; j < partialHull.size(); j++) {
                    Point2D point = partialHull.get(j);
                    gc.lineTo(scaleX(point.getX()), scaleY(point.getY()));
                }
                
                // Close the partial hull
                gc.lineTo(scaleX(first.getX()), scaleY(first.getY()));
                gc.closePath();
                gc.fill();
                
                // Draw edges
                gc.setStroke(edgeColor);
                gc.setLineWidth(2);
                gc.stroke();
            }
            
            // Highlight vertices
            gc.setFill(edgeColor);
            for (Point2D point : partialHull) {
                double x = scaleX(point.getX());
                double y = scaleY(point.getY());
                gc.fillOval(x - pointRadius - 1, y - pointRadius - 1, 
                           (pointRadius + 1) * 2, (pointRadius + 1) * 2);
            }
        }
    }
    
    private void animateMergeHull() {
        if (animation != null) {
            animation.stop();
        }
        
        drawPoints(); // Draw base layer
        
        if (mergeSteps.isEmpty()) return;
        
        // Create animation timeline
        animation = new Timeline();
        final int[] currentStep = {0};
        
        KeyFrame frame = new KeyFrame(Duration.millis(animationSpeed), e -> {
            if (currentStep[0] < mergeSteps.size()) {
                drawMergeStep(currentStep[0]);
                currentStep[0]++;
            } else {
                animation.stop();
                drawHull();
            }
        });
        
        animation.getKeyFrames().add(frame);
        animation.setCycleCount(mergeSteps.size() + 1);
        animation.play();
    }
    
    private void drawMergeStep(int step) {
        clearCanvas();
        drawPoints();
        
        List<Point2D> currentStep = mergeSteps.get(step);
        List<Point2D> left = new ArrayList<>();
        List<Point2D> right = new ArrayList<>();
        List<Point2D> merged = new ArrayList<>();
        
        boolean inRight = false;
        boolean inMerged = false;
        
        for (Point2D p : currentStep) {
            if (Double.isNaN(p.getX())) {
                if (!inRight) {
                    inRight = true;
                } else {
                    inMerged = true;
                }
                continue;
            }
            
            if (!inRight) {
                left.add(p);
            } else if (!inMerged) {
                right.add(p);
            } else {
                merged.add(p);
            }
        }
        
        // Draw left subset and hull
        drawSubHull(left, Color.rgb(20, 20, 220), "Left");
        
        // Draw right subset and hull
        drawSubHull(right, Color.rgb(20, 220, 20), "Right");
        
        // Draw merged hull if present
        if (!merged.isEmpty()) {
            drawSubHull(merged, Color.rgb(220, 20, 20), "Merged");
        }
    }
    
    private void drawSubHull(List<Point2D> points, Color color, String label) {
        if (points.isEmpty()) return;
        
        // Draw points
        gc.setFill(color.deriveColor(0, 1, 1, 0.5));
        for (Point2D p : points) {
            double x = scaleX(p.getX());
            double y = scaleY(p.getY());
            gc.fillOval(x - pointRadius, y - pointRadius, 
                       pointRadius * 2, pointRadius * 2);
        }
        
        // Draw hull
        if (points.size() > 1) {
            gc.setStroke(color);
            gc.setLineWidth(2);
            gc.beginPath();
            Point2D first = points.get(0);
            gc.moveTo(scaleX(first.getX()), scaleY(first.getY()));
            
            for (int i = 1; i < points.size(); i++) {
                Point2D p = points.get(i);
                gc.lineTo(scaleX(p.getX()), scaleY(p.getY()));
            }
            
            gc.closePath();
            gc.stroke();
        }
        
        // Draw label
        if (!label.isEmpty()) {
            Point2D first = points.get(0);
            gc.setFill(color);
            gc.setFont(Font.font("System", FontWeight.BOLD, 10));
            gc.fillText(label, scaleX(first.getX()) + 5, scaleY(first.getY()) - 5);
        }
    }
    
    private void clearCanvas() {
        // Draw background
        gc.setFill(CANVAS_BG_COLOR);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        
        // Draw border
        gc.setStroke(Color.rgb(60, 60, 60));
        gc.setLineWidth(1);
        gc.strokeRect(0, 0, canvas.getWidth(), canvas.getHeight());
        
        // Draw grid
        gc.setStroke(Color.rgb(230, 230, 230));
        gc.setLineWidth(0.5);
        
        double gridSize = 50;
        for (double x = gridSize; x < canvas.getWidth(); x += gridSize) {
            gc.beginPath();
            gc.moveTo(x, 0);
            gc.lineTo(x, canvas.getHeight());
            gc.stroke();
        }
        
        for (double y = gridSize; y < canvas.getHeight(); y += gridSize) {
            gc.beginPath();
            gc.moveTo(0, y);
            gc.lineTo(canvas.getWidth(), y);
            gc.stroke();
        }
    }
    
    private void drawPoints() {
        clearCanvas();
        
        // Draw points
        gc.setFill(POINT_COLOR);
        
        for (Point2D point : points) {
            double x = scaleX(point.getX());
            double y = scaleY(point.getY());
            gc.fillOval(x - pointRadius, y - pointRadius, 
                       pointRadius * 2, pointRadius * 2);
        }
        
        // If we have a hull, draw it
        if (!hull.isEmpty() && !animateHull) {
            drawHull();
        }
    }
    
    private void drawHull() {
        if (hull.isEmpty()) return;
        
        // Draw hull edges
        gc.setStroke(HULL_COLOR);
        gc.setLineWidth(2.5);
        
        // Draw hull as a closed polyline
        gc.beginPath();
        Point2D first = hull.get(0);
        gc.moveTo(scaleX(first.getX()), scaleY(first.getY()));
        
        for (int i = 1; i < hull.size(); i++) {
            Point2D point = hull.get(i);
            gc.lineTo(scaleX(point.getX()), scaleY(point.getY()));
        }
        
        // Close the polygon
        gc.lineTo(scaleX(first.getX()), scaleY(first.getY()));
        gc.stroke();
        
        // Draw hull area with higher transparency
        gc.setFill(Color.rgb(220, 20, 20, 0.2)); // Increased transparency
        gc.beginPath();
        gc.moveTo(scaleX(first.getX()), scaleY(first.getY()));
        
        for (int i = 1; i < hull.size(); i++) {
            Point2D point = hull.get(i);
            gc.lineTo(scaleX(point.getX()), scaleY(point.getY()));
        }
        
        gc.closePath();
        gc.fill();
        
        // Highlight hull vertices
        gc.setFill(HULL_COLOR);
        for (Point2D point : hull) {
            double x = scaleX(point.getX());
            double y = scaleY(point.getY());
            gc.fillOval(x - pointRadius - 2, y - pointRadius - 2, 
                       (pointRadius + 2) * 2, (pointRadius + 2) * 2);
        }
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        // Style the dialog
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: rgb(40, 40, 40);");
        
        // Style all labels in the dialog
        for (javafx.scene.Node node : dialogPane.getChildren()) {
            if (node instanceof Label) {
                ((Label) node).setTextFill(Color.WHITE);
            }
        }
        
        // Style the buttons
        for (javafx.scene.Node node : dialogPane.getChildren()) {
            if (node instanceof javafx.scene.control.ButtonBar) {
                for (javafx.scene.Node child : ((javafx.scene.control.ButtonBar) node).getButtons()) {
                    if (child instanceof Button) {
                        Button button = (Button) child;
                        button.setStyle("-fx-background-color: rgb(180, 20, 20); -fx-text-fill: white;");
                        
                        // Add hover effect
                        button.setOnMouseEntered(e -> 
                            button.setStyle("-fx-background-color: rgb(220, 20, 20); -fx-text-fill: white;")
                        );
                        
                        button.setOnMouseExited(e -> 
                            button.setStyle("-fx-background-color: rgb(180, 20, 20); -fx-text-fill: white;")
                        );
                    }
                }
            }
        }
        
        alert.showAndWait();
    }
    
    private void showHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Help");
        alert.setHeaderText("Convex Hull Visualizer Help");
        
        String helpText = 
            "This application allows you to visualize convex hull algorithms.\n\n" +
            "- Click on the canvas to add points\n" +
            "- Use 'Generate Random Points' to create a random set\n" +
            "- Select an algorithm from the dropdown menu\n" +
            "- Click 'Compute Hull' to calculate and display the convex hull\n" +
            "- Enable 'Animate Hull Construction' to see the algorithm in action\n" +
            "- Adjust animation speed and point size with the sliders\n" +
            "- Save your hull points or load existing point sets\n\n" +
            "A convex hull is the smallest convex polygon that encloses all points in a set.";
        
        alert.setContentText(helpText);
        
        // Style the dialog
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: rgb(40, 40, 40);");
        
        // Style all labels in the dialog
        for (javafx.scene.Node node : dialogPane.getChildren()) {
            if (node instanceof Label) {
                ((Label) node).setTextFill(Color.WHITE);
            }
        }
        
        // Style the buttons
        for (javafx.scene.Node node : dialogPane.getChildren()) {
            if (node instanceof javafx.scene.control.ButtonBar) {
                for (javafx.scene.Node child : ((javafx.scene.control.ButtonBar) node).getButtons()) {
                    if (child instanceof Button) {
                        Button button = (Button) child;
                        button.setStyle("-fx-background-color: rgb(180, 20, 20); -fx-text-fill: white;");
                        
                        // Add hover effect
                        button.setOnMouseEntered(e -> 
                            button.setStyle("-fx-background-color: rgb(220, 20, 20); -fx-text-fill: white;")
                        );
                        
                        button.setOnMouseExited(e -> 
                            button.setStyle("-fx-background-color: rgb(180, 20, 20); -fx-text-fill: white;")
                        );
                    }
                }
            }
        }
        
        alert.showAndWait();
    }
    
    private HBox createFooter() {
        HBox footer = new HBox();
        footer.setPadding(new Insets(8, 15, 8, 15)); // Reduced padding
        footer.setAlignment(Pos.CENTER);
        footer.setStyle("-fx-background-color: rgb(30, 30, 30);");
        
        Label footerText = new Label("Elite Computation by Abdelrahman Elnagar, Alaa Ashraf and Sarah Elfeel");
        footerText.setFont(Font.font("System", FontWeight.BOLD, 12)); // Reduced font size
        footerText.setTextFill(TEXT_COLOR);
        
        footer.getChildren().add(footerText);
        return footer;
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}