package com.myapp.hull.ui;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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
import javafx.scene.Node;
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
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
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
        VBox panel = new VBox(10); 
        panel.setPadding(new Insets(15)); 
        panel.setMinWidth(250); 
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
        // Open algorithm analysis page
        Button complexityButton = createActionButton("Algorithm Analysis", "chart-line");
        complexityButton.setOnAction(e -> showComplexityAnalysis());

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
            complexityButton,
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
    
    private void savePointsToFile(List<Point2D> points, String filename) {
        try {
            File file = new File(filename);
            PrintWriter writer = new PrintWriter(new FileWriter(file));
            for (Point2D point : points) {
                writer.printf("%.2f %.2f%n", point.getX(), point.getY());
            }
            writer.close();
            updateStatus("Points saved to " + filename);
        } catch (IOException e) {
            showError("File Error", "Failed to save points to file: " + e.getMessage());
        }
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
            
            // Save points to file
            savePointsToFile(points, "currRandom.txt");
            
            drawPoints();
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
        savePointsToFile(points, "currRandom.txt");
        
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
    // The complexity analysis popup

private void showComplexityAnalysis() {
    // Create a custom dialog
    Dialog<Void> dialog = new Dialog<>();
    dialog.setTitle("Algorithm Complexity Analysis");
    dialog.setHeaderText("Performance Analysis of Convex Hull Algorithms");
    
    // Configure the dialog pane
    DialogPane dialogPane = dialog.getDialogPane();
    dialogPane.setPrefSize(650, 500); // Make it reasonably large
    dialogPane.setStyle("-fx-background-color: rgb(40, 40, 40);");
    dialogPane.getButtonTypes().add(ButtonType.CLOSE);
    
    // Style the close button
    Button closeButton = (Button) dialogPane.lookupButton(ButtonType.CLOSE);
    closeButton.setStyle("-fx-background-color: rgb(180, 20, 20); -fx-text-fill: white;");
    
    // Add hover effect
    closeButton.setOnMouseEntered(e -> 
        closeButton.setStyle("-fx-background-color: rgb(220, 20, 20); -fx-text-fill: white;")
    );
    
    closeButton.setOnMouseExited(e -> 
        closeButton.setStyle("-fx-background-color: rgb(180, 20, 20); -fx-text-fill: white;")
    );
    
    // Create a tabbed pane for different algorithms
    javafx.scene.control.TabPane tabPane = new javafx.scene.control.TabPane();
    tabPane.setTabClosingPolicy(javafx.scene.control.TabPane.TabClosingPolicy.UNAVAILABLE);
    tabPane.setStyle("-fx-background-color: rgb(40, 40, 40);");
    
    dialogPane.setPrefSize(650, 600); // Increase height a bit
    dialogPane.setMinHeight(Region.USE_PREF_SIZE);
    dialogPane.setMaxHeight(Double.MAX_VALUE);

    // Make sure the dialog is resizable
    dialog.setResizable(true);

    // And modify your tab creation to use scrollable content:
    tabPane.getTabs().addAll(
        createAlgorithmTab("Graham Scan", createScrollableContent(createGrahamContent())),
        createAlgorithmTab("Chan's Algorithm", createScrollableContent(createChanContent())),
        createAlgorithmTab("Merge Hull", createScrollableContent(createMergeHullContent())),
        createAlgorithmTab("Jarvis March", createScrollableContent(createJarvisMarchContent())),
        createAlgorithmTab("Comparison", createScrollableContent(createComparisonContent()))
    );
    
    // Set the content of the dialog
    dialogPane.setContent(tabPane);
    
    // Show the dialog
    dialog.showAndWait();
}

private javafx.scene.control.Tab createAlgorithmTab(String title, javafx.scene.Node content) {
    javafx.scene.control.Tab tab = new javafx.scene.control.Tab(title);
    tab.setContent(content);
    return tab;
}

private ScrollPane createScrollableContent(Node content) {
    ScrollPane scrollPane = new ScrollPane(content);
    scrollPane.setFitToWidth(true);
    scrollPane.setStyle("-fx-background: rgb(40, 40, 40); -fx-background-color: rgb(40, 40, 40);");
    return scrollPane;
}

private VBox createGrahamContent() {
    VBox content = new VBox(15);
    content.setPadding(new Insets(20));
    content.setStyle("-fx-background-color: rgb(40, 40, 40);");
    
    // Title
    Label title = new Label("Graham Scan Algorithm");
    title.setFont(Font.font("System", FontWeight.BOLD, 16));
    title.setTextFill(TEXT_COLOR);
    
    // Complexity section
    VBox complexity = createComplexitySection(
        "O(n log n)", 
        "O(n)", 
        "Sorting points by polar angle, then incrementally building hull"
    );
    
    // Algorithm description
    Label descTitle = new Label("Description");
    descTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
    descTitle.setTextFill(TEXT_COLOR);
    
    Label description = new Label(
        "Graham Scan sorts all points by their polar angle relative to the lowest point, " +
        "then processes them in order, maintaining a convex hull by removing interior points. " +
        "The algorithm uses a stack to track the hull vertices, adding points one by one and " +
        "removing any points that would create a non-convex angle."
    );
    description.setWrapText(true);
    description.setTextFill(TEXT_COLOR);
    
    // Steps section
    Label stepsTitle = new Label("Algorithm Steps");
    stepsTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
    stepsTitle.setTextFill(TEXT_COLOR);
    
    VBox steps = new VBox(8);
    steps.getChildren().addAll(
        createStepLabel("1. Find the point with the lowest y-coordinate (and leftmost if tied)"),
        createStepLabel("2. Sort all other points by polar angle around this point"),
        createStepLabel("3. Initialize stack with first three points"),
        createStepLabel("4. For each remaining point:"),
        createStepLabel("   a. While the last three points make a non-left (anticlockwise) turn, remove the middle point"),
        createStepLabel("   b. Add the current point to the stack"),
        createStepLabel("5. The stack now contains all points of the convex hull")
    );
    
    // Best and worst case scenarios
    Label casesTitle = new Label("Best and Worst Cases");
    casesTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
    casesTitle.setTextFill(TEXT_COLOR);
    
    Label cases = new Label(
        "Graham Scan always has O(n log n) time complexity due to the initial sorting step. " +
        "The best case is when many points lie inside the hull, but the sorting still dominates. " +
        "The worst case is when most points are on the hull, but the time complexity remains the same."
    );
    cases.setWrapText(true);
    cases.setTextFill(TEXT_COLOR);
    
    // Add visualization example
    Label visTitle = new Label("Visual Example");
    visTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
    visTitle.setTextFill(TEXT_COLOR);
    
    // Create a simple canvas showing Graham Scan steps
    Canvas exampleCanvas = createAlgorithmVisualization("graham");
    
    content.getChildren().addAll(
        title, 
        complexity, 
        new Separator(), 
        descTitle, 
        description,
        new Separator(),
        stepsTitle,
        steps,
        new Separator(),
        casesTitle,
        cases,
        new Separator(),
        visTitle,
        exampleCanvas
    );
    
    return content;
}

private VBox createChanContent() {
    VBox content = new VBox(15);
    content.setPadding(new Insets(20));
    content.setStyle("-fx-background-color: rgb(40, 40, 40);");
    
    // Title
    Label title = new Label("Chan's Algorithm");
    title.setFont(Font.font("System", FontWeight.BOLD, 16));
    title.setTextFill(TEXT_COLOR);
    
    // Complexity section
    VBox complexity = createComplexitySection(
        "O(n log h)", 
        "O(n)", 
        "h is the number of vertices in the final hull. The Algorithm is output sensitive."
    );
    
    // Algorithm description
    Label descTitle = new Label("Description");
    descTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
    descTitle.setTextFill(TEXT_COLOR);
    
    Label description = new Label(
        "Chan's Algorithm combines Graham Scan and Jarvis March to achieve optimal O(n log h) " +
        "time complexity. It partitions points into subsets, computes sub-hulls using Graham Scan, " +
        "and then uses a modified Jarvis March to merge them. This is particularly efficient when " +
        "the output size (h) is small compared to the input size (n)."
    );
    description.setWrapText(true);
    description.setTextFill(TEXT_COLOR);
    
    // Steps section
    Label stepsTitle = new Label("Algorithm Steps");
    stepsTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
    stepsTitle.setTextFill(TEXT_COLOR);
    
    VBox steps = new VBox(8);
    steps.getChildren().addAll(
        createStepLabel("1. Partition points into √n subsets of size √n each"),
        createStepLabel("2. Compute the convex hull of each subset using Graham scan (O(√n log √n) each)"),
        createStepLabel("3. Use a modified Jarvis March (gift wrapping) to compute the overall hull"),
        createStepLabel("4. For each step of Jarvis March, find the most counterclockwise point"),
        createStepLabel("5. Use binary search on each sub-hull to find the most counterclockwise point"),
        createStepLabel("6. If h is not known, use doubling with increasing guesses for h")
    );
    
    // Best and worst case scenarios
    Label casesTitle = new Label("Best and Worst Cases");
    casesTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
    casesTitle.setTextFill(TEXT_COLOR);
    
    Label cases = new Label(
        "Best case: When h is very small, Chan's algorithm approaches O(n), making it significantly " +
        "faster than Graham Scan for large inputs with small output hulls.\n\n" +
        "Worst case: When h is O(n) (most points are on the hull), the performance is O(n log n), " +
        "similar to Graham Scan. However, the algorithm has additional overhead that makes it " +
        "slightly less practical for these cases."
    );
    cases.setWrapText(true);
    cases.setTextFill(TEXT_COLOR);
        
    content.getChildren().addAll(
        title, 
        complexity, 
        new Separator(), 
        descTitle, 
        description,
        new Separator(),
        stepsTitle,
        steps,
        new Separator(),
        casesTitle,
        cases,
        new Separator()
    );
    
    return content;
}

private VBox createMergeHullContent() {
    VBox content = new VBox(15);
    content.setPadding(new Insets(20));
    content.setStyle("-fx-background-color: rgb(40, 40, 40);");
    
    // Title
    Label title = new Label("Merge Hull Algorithm");
    title.setFont(Font.font("System", FontWeight.BOLD, 16));
    title.setTextFill(TEXT_COLOR);
    
    // Complexity section
    VBox complexity = createComplexitySection(
        "O(n log n)", 
        "O(n)", 
        "Divide-and-conquer approach with merge step"
    );
    
    // Algorithm description
    Label descTitle = new Label("Description");
    descTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
    descTitle.setTextFill(TEXT_COLOR);
    
    Label description = new Label(
        "The Merge Hull algorithm uses a divide-and-conquer approach similar to merge sort. " +
        "It recursively divides the points into two subsets, computes the convex hull of each subset, " +
        "and then merges the two hulls. The merge step, which finds the upper and lower tangent lines " +
        "between the two hulls, is the key operation."
    );
    description.setWrapText(true);
    description.setTextFill(TEXT_COLOR);
    
    // Steps section
    Label stepsTitle = new Label("Algorithm Steps");
    stepsTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
    stepsTitle.setTextFill(TEXT_COLOR);
    
    VBox steps = new VBox(8);
    steps.getChildren().addAll(
        createStepLabel("1. If the number of points is small (3 or less), compute hull directly"),
        createStepLabel("2. Otherwise, divide points into left and right halves (by x-coordinate)"),
        createStepLabel("3. Recursively compute the convex hull of the left half"),
        createStepLabel("4. Recursively compute the convex hull of the right half"),
        createStepLabel("5. Merge the two hulls:"),
        createStepLabel("   a. Find the upper tangent between hulls"),
        createStepLabel("   b. Find the lower tangent between hulls"),
        createStepLabel("   c. Merge the two hulls along these tangents")
    );
    
    // Best and worst case scenarios
    Label casesTitle = new Label("Best and Worst Cases");
    casesTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
    casesTitle.setTextFill(TEXT_COLOR);
    
    Label cases = new Label(
        "Best case: When points are relatively balanced and evenly distributed, the algorithm " +
        "achieves O(n log n) time complexity.\n\n" +
        "Worst case: Even in the worst case, the algorithm maintains O(n log n) time complexity. " +
        "However, the constant factors may be higher than Graham Scan in practice. The advantage of " +
        "Merge Hull is that it's parallelizable, allowing for potential speedup on multi-core systems."
    );
    cases.setWrapText(true);
    cases.setTextFill(TEXT_COLOR);
    
    // Add visualization example
    Label visTitle = new Label("Visual Example");
    visTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
    visTitle.setTextFill(TEXT_COLOR);
    
    // Create a simple canvas showing Merge Hull steps
    Canvas exampleCanvas = createAlgorithmVisualization("merge");
    
    content.getChildren().addAll(
        title, 
        complexity, 
        new Separator(), 
        descTitle, 
        description,
        new Separator(),
        stepsTitle,
        steps,
        new Separator(),
        casesTitle,
        cases,
        new Separator(),
        visTitle,
        exampleCanvas
    );
    
    return content;
}

private VBox createJarvisMarchContent() {
    VBox content = new VBox(15);
    content.setPadding(new Insets(20));
    content.setStyle("-fx-background-color: rgb(40, 40, 40);");
    
    // Title
    Label title = new Label("Jarvis March (Gift Wrapping)");
    title.setFont(Font.font("System", FontWeight.BOLD, 16));
    title.setTextFill(TEXT_COLOR);
    
    // Complexity section
    VBox complexity = createComplexitySection(
        "O(nh)", 
        "O(n)", 
        "h is the number of vertices in the final hull"
    );
    
    // Algorithm description
    Label descTitle = new Label("Description");
    descTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
    descTitle.setTextFill(TEXT_COLOR);
    
    Label description = new Label(
        "Jarvis March (also known as Gift Wrapping) is a simple convex hull algorithm that builds " +
        "the hull by incrementally finding the next point with the smallest counterclockwise angle. " +
        "It mimics the process of wrapping a string around the set of points. While conceptually simple, " +
        "it can be inefficient for large sets of points where many points lie on the hull."
    );
    description.setWrapText(true);
    description.setTextFill(TEXT_COLOR);
    
    // Steps section
    Label stepsTitle = new Label("Algorithm Steps");
    stepsTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
    stepsTitle.setTextFill(TEXT_COLOR);
    
    VBox steps = new VBox(8);
    steps.getChildren().addAll(
        createStepLabel("1. Find the leftmost point (with smallest x-coordinate)"),
        createStepLabel("2. Set this as the starting point of the hull"),
        createStepLabel("3. Repeat until we return to the starting point:"),
        createStepLabel("   a. Find the point with the smallest counterclockwise angle"),
        createStepLabel("   b. Add this point to the hull"),
        createStepLabel("   c. Set this as the new reference point"),
        createStepLabel("4. The hull is complete when we return to the starting point")
    );
    
    // Best and worst case scenarios
    Label casesTitle = new Label("Best and Worst Cases");
    casesTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
    casesTitle.setTextFill(TEXT_COLOR);
    
    Label cases = new Label(
        "Best case: When h is small (few points on the hull), Jarvis March can be very efficient " +
        "with O(n) performance when h is constant.\n\n" +
        "Worst case: When most or all points are on the hull (h ≈ n), the algorithm degrades to " +
        "O(n²), making it much slower than other algorithms for these cases."
    );
    cases.setWrapText(true);
    cases.setTextFill(TEXT_COLOR);
    
    content.getChildren().addAll(
        title, 
        complexity, 
        new Separator(), 
        descTitle, 
        description,
        new Separator(),
        stepsTitle,
        steps,
        new Separator(),
        casesTitle,
        cases,
        new Separator()
    );
    
    return content;
}

private VBox createComparisonContent() {
    VBox content = new VBox(15);
    content.setPadding(new Insets(20));
    content.setStyle("-fx-background-color: rgb(40, 40, 40);");
    
    // Title
    Label title = new Label("Algorithm Comparison");
    title.setFont(Font.font("System", FontWeight.BOLD, 16));
    title.setTextFill(TEXT_COLOR);
    
    // Comparison table
    Label tableTitle = new Label("Performance Comparison");
    tableTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
    tableTitle.setTextFill(TEXT_COLOR);
    
    // Create grid for the table
    GridPane table = new GridPane();
    table.setHgap(10);
    table.setVgap(10);
    table.setPadding(new Insets(10));
    table.setStyle("-fx-background-color: rgb(50, 50, 50); -fx-background-radius: 5;");
    
    // Table headers
    String[] headers = {"Algorithm", "Time Complexity", "Space", "Best For", "Worst For"};
    for (int i = 0; i < headers.length; i++) {
        Label header = new Label(headers[i]);
        header.setFont(Font.font("System", FontWeight.BOLD, 12));
        header.setTextFill(PRIMARY_COLOR);
        table.add(header, i, 0);
    }
    
    // Table data
    String[][] data = {
        {"Graham Scan", "O(n log n)", "O(n)", "General purpose", "Very large datasets"},
        {"Chan's Algorithm", "O(n log h)", "O(n)", "Many interior points", "Most points on hull"},
        {"Merge Hull", "O(n log n)", "O(n)", "Parallel processing", "Simple implementations"},
        {"Jarvis March", "O(nh)", "O(n)", "Few hull points", "Many hull points"}
    };
    
    for (int row = 0; row < data.length; row++) {
        for (int col = 0; col < data[row].length; col++) {
            Label cell = new Label(data[row][col]);
            cell.setTextFill(TEXT_COLOR);
            table.add(cell, col, row + 1);
        }
    }
    
    // Complexity chart - shows time complexity vs. hull size
    Label chartTitle = new Label("Time Complexity vs Hull Size");
    chartTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
    chartTitle.setTextFill(TEXT_COLOR);
    
    Canvas complexityChart = createComplexityChart();
    
    // When to use which algorithm
    Label recommendationsTitle = new Label("Algorithm Recommendations");
    recommendationsTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
    recommendationsTitle.setTextFill(TEXT_COLOR);
    
    VBox recommendations = new VBox(10);
    recommendations.setStyle("-fx-background-color: rgb(50, 50, 50); -fx-background-radius: 5; -fx-padding: 10;");
    
    recommendations.getChildren().addAll(
        createRecommendationItem("Small Datasets (<100 points)", 
            "Any algorithm works well. Graham Scan is a safe choice."),
        createRecommendationItem("Large Datasets with Few Hull Points", 
            "Chan's Algorithm offers the best theoretical performance."),
        createRecommendationItem("Most Points on Hull", 
            "Graham Scan or Merge Hull. Avoid Jarvis March."),
        createRecommendationItem("Need for Parallelization", 
            "Merge Hull is naturally parallelizable."),
        createRecommendationItem("Educational Purposes", 
            "Jarvis March is conceptually simplest, followed by Graham Scan.")
    );
    
    content.getChildren().addAll(
        title,
        tableTitle,
        table,
        new Separator(),
        chartTitle,
        complexityChart,
        new Separator(),
        recommendationsTitle,
        recommendations
    );
    
    return content;
}

private Label createStepLabel(String step) {
    Label label = new Label(step);
    label.setTextFill(TEXT_COLOR);
    return label;
}

private VBox createComplexitySection(String time, String space, String notes) {
    VBox section = new VBox(8);
    section.setStyle("-fx-background-color: rgb(50, 50, 50); -fx-padding: 10; -fx-background-radius: 5;");
    
    HBox timeBox = new HBox(10);
    Label timeLabel = new Label("Time Complexity:");
    timeLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
    timeLabel.setTextFill(TEXT_COLOR);
    Label timeValue = new Label(time);
    timeValue.setTextFill(PRIMARY_COLOR);
    timeValue.setFont(Font.font("System", FontWeight.BOLD, 12));
    timeBox.getChildren().addAll(timeLabel, timeValue);
    
    HBox spaceBox = new HBox(10);
    Label spaceLabel = new Label("Space Complexity:");
    spaceLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
    spaceLabel.setTextFill(TEXT_COLOR);
    Label spaceValue = new Label(space);
    spaceValue.setTextFill(PRIMARY_COLOR);
    spaceValue.setFont(Font.font("System", FontWeight.BOLD, 12));
    spaceBox.getChildren().addAll(spaceLabel, spaceValue);
    
    HBox notesBox = new HBox(10);
    Label notesLabel = new Label("Notes:");
    notesLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
    notesLabel.setTextFill(TEXT_COLOR);
    Label notesValue = new Label(notes);
    notesValue.setTextFill(TEXT_COLOR);
    notesBox.getChildren().addAll(notesLabel, notesValue);
    
    section.getChildren().addAll(timeBox, spaceBox, notesBox);
    return section;
}

private HBox createRecommendationItem(String scenario, String recommendation) {
    HBox item = new HBox(10);
    
    Label scenarioLabel = new Label(scenario + ":");
    scenarioLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
    scenarioLabel.setTextFill(TEXT_COLOR);
    scenarioLabel.setMinWidth(200);
    
    Label recommendationLabel = new Label(recommendation);
    recommendationLabel.setTextFill(TEXT_COLOR);
    recommendationLabel.setWrapText(true);
    
    item.getChildren().addAll(scenarioLabel, recommendationLabel);
    return item;
}

private Canvas createComplexityChart() {
    Canvas canvas = new Canvas(550, 300);
    GraphicsContext gc = canvas.getGraphicsContext2D();
    
    // Background
    gc.setFill(Color.rgb(50, 50, 50));
    gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    
    // Draw axes
    gc.setStroke(Color.WHITE);
    gc.setLineWidth(2);
    
    // X axis
    gc.strokeLine(50, 250, 500, 250);
    
    // Y axis
    gc.strokeLine(50, 50, 50, 250);
    
    // Axis labels
    gc.setFill(Color.WHITE);
    gc.setFont(Font.font("System", 12));
    gc.fillText("Hull Size (h)", 250, 280);
    
    // Rotate text for Y axis
    gc.save();
    gc.translate(20, 150);
    gc.rotate(-90);
    gc.fillText("Time Complexity", 0, 0);
    gc.restore();
    
    // X axis marks
    for (int i = 0; i <= 10; i++) {
        double x = 50 + i * 45;
        gc.strokeLine(x, 250, x, 255);
        gc.fillText(String.valueOf(i * 10) + "%", x - 10, 270);
    }
    
    
    // Plot lines for each algorithm
    
    // Chan's Algorithm: O(n log h)
    gc.setStroke(Color.GREEN);
    gc.setLineWidth(2);
    gc.beginPath();
    gc.moveTo(50, 210);
    for (int x = 0; x <= 450; x++) {
        double h = x / 450.0;
        double y = 210 - (30 * Math.log1p(h * 10) / Math.log(2));
        gc.lineTo(50 + x, y);
    }
    gc.stroke();
    
    // Graham Scan: O(n log n) - horizontal line
    gc.setStroke(Color.RED);
    gc.setLineWidth(2);
    gc.beginPath();
    gc.moveTo(50, 210);
    for (int x = 0; x <= 450; x++) {
        double h = x / 450.0;
        double y = 200 - (30 * Math.log1p(h * 10) / Math.log(2));
        gc.lineTo(50 + x, y);
    }
    gc.stroke();
    
    // Jarvis March: O(nh)
    gc.setStroke(Color.YELLOW);
    gc.setLineWidth(2);
    gc.beginPath();
    gc.moveTo(50, 210);
    for (int x = 0; x <= 450; x++) {
        double h = x / 450.0;
        double y = 210 - (200 * h);
        gc.lineTo(50 + x, y);
    }
    gc.stroke();
    
    // Merge Hull: O(n log n) - horizontal line
    gc.setStroke(Color.CYAN);
    gc.setLineWidth(2);
    gc.beginPath();
    gc.moveTo(50, 210);
    for (int x = 0; x <= 450; x++) {
        double h = x / 450.0;
        double y = 198 - (30 * Math.log1p(h * 10) / Math.log(2));
        gc.lineTo(50 + x, y);
    }
    gc.stroke();
    
    // Add legend
    int legendY = 130;
    int legendX = 350;
    
    // Graham Scan
    gc.setFill(Color.RED);
    gc.fillRect(legendX, legendY, 20, 10);
    gc.setFill(Color.WHITE);
    
    gc.fillText("Graham Scan Θ(nlogn)", legendX + 30, legendY + 10);
    // Chan's Algorithm
    gc.setFill(Color.GREEN);
    gc.fillRect(legendX, legendY + 20, 20, 10);
    gc.setFill(Color.WHITE);
    gc.fillText("Chan's Algorithm Θ(nlogh)", legendX + 30, legendY + 30);
    
    // Merge Hull
    gc.setFill(Color.CYAN);
    gc.fillRect(legendX, legendY + 40, 20, 10);
    gc.setFill(Color.WHITE);
    gc.fillText("Merge Hull Θ(nlogn)", legendX + 30, legendY + 50);
    
    // Jarvis March
    gc.setFill(Color.YELLOW);
    gc.fillRect(legendX, legendY + 60, 20, 10);
    gc.setFill(Color.WHITE);
    gc.fillText("Jarvis March Θ(nh)", legendX + 30, legendY + 70);
    
    return canvas;
}

private Canvas createAlgorithmVisualization(String algorithm) {
    Canvas canvas = new Canvas(550, 200);
    GraphicsContext gc = canvas.getGraphicsContext2D();
    
    // Background
    gc.setFill(Color.rgb(50, 50, 50));
    gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    
    // Create points - simple example with about 20 points
    List<Point2D> points = new ArrayList<>();
    
    // Use specific example patterns for each algorithm to showcase its behavior
    switch (algorithm) {
        case "graham":
            // Create a set of points with some on the edge and some inside
            points.add(new Point2D(100, 100));
            points.add(new Point2D(150, 50));
            points.add(new Point2D(200, 70));
            points.add(new Point2D(250, 50));
            points.add(new Point2D(300, 80));
            points.add(new Point2D(350, 120));
            points.add(new Point2D(300, 150));
            points.add(new Point2D(250, 160));
            points.add(new Point2D(200, 140));
            points.add(new Point2D(150, 150));
            points.add(new Point2D(160, 100)); // interior
            points.add(new Point2D(200, 100)); // interior
            points.add(new Point2D(250, 100)); // interior
            points.add(new Point2D(270, 120)); // interior
            points.add(new Point2D(200, 120)); // interior
            break;
        case "chan":
            // Create a pattern with lots of interior points
            for (int i = 0; i < 5; i++) {
                // Create outer hull points
                points.add(new Point2D(100 + i*80, 50));
                points.add(new Point2D(100 + i*80, 150));
            }
            // Add interior points
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    points.add(new Point2D(130 + i*80, 70 + j*30));
                }
            }
            break;
        case "merge":
            // Create two distinct clusters to show merge
            // Left cluster
            points.add(new Point2D(80, 70));
            points.add(new Point2D(120, 50));
            points.add(new Point2D(150, 80));
            points.add(new Point2D(130, 120));
            points.add(new Point2D(90, 110));
            points.add(new Point2D(110, 80)); // interior
            // Right cluster
            points.add(new Point2D(300, 80));
            points.add(new Point2D(340, 60));
            points.add(new Point2D(380, 90));
            points.add(new Point2D(360, 130));
            points.add(new Point2D(320, 120));
            points.add(new Point2D(340, 100)); // interior
            break;
        case "jarvis":
            // Create a star-like pattern good for jarvis march
            double centerX = 275, centerY = 100;
            double radius1 = 90, radius2 = 40;
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4;
                points.add(new Point2D(
                    centerX + radius1 * Math.cos(angle),
                    centerY + radius1 * Math.sin(angle)
                ));
                // Add some interior points
                if (i % 2 == 0) {
                    points.add(new Point2D(
                        centerX + radius2 * Math.cos(angle + Math.PI/8),
                        centerY + radius2 * Math.sin(angle + Math.PI/8)
                    ));
                }
            }
            break;
        default:
            // Default random points
            for (int i = 0; i < 15; i++) {
                points.add(new Point2D(100 + Math.random() * 300, 50 + Math.random() * 100));
            }
    }
    
    // Draw points
    for (Point2D point : points) {
        gc.setFill(Color.WHITE);
        gc.fillOval(point.getX() - 3, point.getY() - 3, 6, 6);
    }
    
    // Draw convex hull
    List<Point2D> hull = new ArrayList<>();
    switch (algorithm) {
        case "graham":
            // Simulate Graham scan hull
            hull.add(points.get(0));
            hull.add(points.get(1));
            hull.add(points.get(3));
            hull.add(points.get(4));
            hull.add(points.get(5));
            hull.add(points.get(6));
            hull.add(points.get(7));
            hull.add(points.get(9));
            break;
        case "chan":
            // Simulate Chan's algorithm hull
            hull.add(points.get(0));
            hull.add(points.get(1));
            hull.add(points.get(2));
            hull.add(points.get(3));
            hull.add(points.get(4));
            hull.add(points.get(5));
            hull.add(points.get(6));
            hull.add(points.get(7));
            hull.add(points.get(8));
            hull.add(points.get(9));
            break;
        case "merge":
            // Simulate Merge Hull
            hull.add(points.get(0));
            hull.add(points.get(1));
            hull.add(points.get(2));
            hull.add(points.get(3));
            hull.add(points.get(4));
            hull.add(points.get(6));
            hull.add(points.get(7));
            hull.add(points.get(8));
            hull.add(points.get(9));
            hull.add(points.get(10));
            break;
        case "jarvis":
            // Simulate Jarvis march hull (just the outer points)
            for (int i = 0; i < 8; i++) {
                hull.add(points.get(i));
            }
            break;
    }
    
    // Draw hull
    // Assuming hull contains leftHull followed by rightHull
if(algorithm.equals("merge")){

int totalPoints = hull.size();
int mid = totalPoints / 2;

// Draw left hull
if (totalPoints >= 2) {
    gc.setStroke(Color.CYAN);
    gc.beginPath();
    gc.moveTo(hull.get(0).getX(), hull.get(0).getY());
    for (int i = 1; i < mid; i++) {
        gc.lineTo(hull.get(i).getX(), hull.get(i).getY());
    }
    gc.lineTo(hull.get(0).getX(), hull.get(0).getY()); // close left
    gc.stroke();
}


// Draw right hull
if (totalPoints > mid) {
    gc.setStroke(Color.CYAN);
    gc.beginPath();
    gc.moveTo(hull.get(mid).getX(), hull.get(mid).getY());
    for (int i = mid + 1; i < totalPoints; i++) {
        gc.lineTo(hull.get(i).getX(), hull.get(i).getY());
    }
    gc.lineTo(hull.get(mid).getX(), hull.get(mid).getY()); // close right
    gc.stroke();
}
}
else{
    gc.setStroke(getAlgorithmColor(algorithm));
    gc.setLineWidth(2);
    gc.beginPath();
    gc.moveTo(hull.get(0).getX(), hull.get(0).getY());
    for (int i = 1; i < hull.size(); i++) {
        if(algorithm.equals("merge") && i == 5) continue;
        gc.lineTo(hull.get(i).getX(), hull.get(i).getY());
    }
    gc.closePath();
    gc.stroke();
    
}
    
    // Add algorithm-specific visualizations
    switch (algorithm) {
        case "graham":
            // Show sorting by polar angle
            gc.setStroke(Color.LIGHTBLUE);
            gc.setLineWidth(1);
            Point2D lowestPoint = hull.get(6);
            for (int i = 1; i < 5; i++) {
                gc.strokeLine(lowestPoint.getX(), lowestPoint.getY(), 
                               points.get(i).getX(), points.get(i).getY());
            }
            
            // Show angle annotations
            gc.setFill(Color.LIGHTBLUE);
            gc.fillText("θ₁", (lowestPoint.getX() + points.get(1).getX()) / 2 - 10, 
                        (lowestPoint.getY() + points.get(1).getY()) / 2 - 5);
            gc.fillText("θ₂", (lowestPoint.getX() + points.get(2).getX()) / 2 - 10, 
                        (lowestPoint.getY() + points.get(2).getY()) / 2 - 5);
            
            // Show stack processing
            drawStack(gc, 450, 80, "Stack");
            break;
            
        case "chan":
            // Show subhulls
            drawSubHulls(gc, points);
            break;
            
        case "merge":
            // Show the merge process
            drawMergeProcess(gc, points);
            break;
            
        case "jarvis":
            // Show gift wrapping steps
            //drawJarvisSteps(gc, hull);
            break;
    }
    
    return canvas;
}

private Color getAlgorithmColor(String algorithm) {
    switch (algorithm) {
        case "graham": return Color.RED;
        case "chan": return Color.GREEN;
        case "merge": return Color.CYAN;
        case "jarvis": return Color.YELLOW;
        default: return Color.WHITE;
    }
}

private void drawStack(GraphicsContext gc, double x, double y, String label) {
    // Simple stack visualization
    gc.setFill(Color.rgb(70, 70, 70));
    gc.fillRect(x, y, 80, 100);
    
    gc.setStroke(Color.WHITE);
    gc.strokeRect(x, y, 80, 100);
    
    gc.setFill(Color.WHITE);
    gc.fillText(label, x + 20, y - 5);
    
    // Stack items
    String[] items = {"Point 9", "Point 7", "Point 5"};
    for (int i = 0; i < items.length; i++) {
        gc.setFill(Color.rgb(100, 100, 100));
        gc.fillRect(x + 5, y + 75 - i*20, 70, 18);
        gc.setFill(Color.WHITE);
        gc.fillText(items[i], x + 10, y + 90 - i*20);
    }
}

private void drawSubHulls(GraphicsContext gc, List<Point2D> points) {
    // Visualize sub-hulls for Chan's algorithm
    
    // Left half sub-hull
    gc.setStroke(Color.rgb(100, 200, 100, 0.7));
    gc.setLineWidth(1.5);
    
    gc.beginPath();
    gc.moveTo(points.get(0).getX(), points.get(0).getY());
    gc.lineTo(points.get(1).getX(), points.get(1).getY());
    gc.lineTo(points.get(6).getX(), points.get(6).getY());
    gc.lineTo(points.get(5).getX(), points.get(5).getY());
    gc.closePath();
    gc.stroke();
    
    // Right half sub-hull
    gc.beginPath();
    gc.moveTo(points.get(2).getX(), points.get(2).getY());
    gc.lineTo(points.get(3).getX(), points.get(3).getY());
    gc.lineTo(points.get(8).getX(), points.get(8).getY());
    gc.lineTo(points.get(7).getX(), points.get(7).getY());
    gc.closePath();
    gc.stroke();
    
    // Annotation
    gc.setFill(Color.GREEN);
    gc.fillText("Subhull 1", 120, 40);
    gc.fillText("Subhull 2", 320, 40);
    
    // Binary search indicator
    gc.setStroke(Color.ORANGE);
    gc.setLineWidth(1);
    gc.strokeOval(points.get(3).getX() - 10, points.get(3).getY() - 10, 20, 20);
    gc.setFill(Color.ORANGE);
    gc.fillText("Binary search", points.get(3).getX() + 15, points.get(3).getY() - 10);
}

private void drawMergeProcess(GraphicsContext gc, List<Point2D> points) {
    // Visualize merge process
    
    // Draw division line
    gc.setStroke(Color.WHITE);
    gc.setLineWidth(1);
    gc.setLineDashes(5, 5);
    gc.strokeLine(225, 40, 225, 160);
    gc.setLineDashes(0);
    
    // Draw tangent lines
    gc.setStroke(Color.ORANGE);
    gc.setLineWidth(2);
    
    // Upper tangent
    gc.strokeLine(points.get(1).getX(), points.get(1).getY(), 
                  points.get(7).getX(), points.get(7).getY());
    
    // Lower tangent
    gc.strokeLine(points.get(3).getX(), points.get(3).getY(), 
                  points.get(9).getX(), points.get(9).getY());
    
    // Annotations
    gc.setFill(Color.ORANGE);
    gc.fillText("Upper tangent", (points.get(2).getX() + points.get(7).getX()) / 2 - 40, 
                (points.get(2).getY() + points.get(7).getY()) / 2 );
    gc.fillText("Lower tangent", (points.get(4).getX() + points.get(10).getX()) / 2 - 40, 
                (points.get(4).getY() + points.get(10).getY()) / 2 + 25);
}

private void drawJarvisSteps(GraphicsContext gc, List<Point2D> hull) {
    // Visualize gift wrapping steps
    
    // Show the "wrapping" process
    gc.setStroke(Color.ORANGE);
    gc.setLineWidth(1);
    Point2D current = hull.get(0);
    Point2D next = hull.get(1);
    
    // Draw search angles
    for (int i = 2; i < hull.size(); i += 2) {
        drawAngle(gc, current, next, hull.get(i), Color.rgb(255, 165, 0, 0.3));
    }
    
    // Show current point and direction
    gc.setStroke(Color.RED);
    gc.setLineWidth(2);
    gc.strokeOval(current.getX() - 5, current.getY() - 5, 10, 10);
    
    // Draw arrow indicating direction
    drawArrow(gc, current, next, Color.ORANGE);
    
    // Annotation
    gc.setFill(Color.ORANGE);
    gc.fillText("Current point", current.getX() + 10, current.getY() - 10);
    gc.fillText("Next hull point", next.getX() + 10, next.getY() - 10);
    gc.fillText("(smallest CCW angle)", next.getX() + 10, next.getY() + 5);
}

private void drawAngle(GraphicsContext gc, Point2D center, Point2D p1, Point2D p2, Color fillColor) {
    // Draw angle between two points from center
    gc.setFill(fillColor);
    
    double startAngle = Math.atan2(p1.getY() - center.getY(), p1.getX() - center.getX());
    double endAngle = Math.atan2(p2.getY() - center.getY(), p2.getX() - center.getX());
    
    // Ensure we draw the smaller angle
    if (Math.abs(endAngle - startAngle) > Math.PI) {
        if (endAngle > startAngle) {
            endAngle -= 2 * Math.PI;
        } else {
            startAngle -= 2 * Math.PI;
        }
    }
    
    double radius = 30;
    double startDegrees = Math.toDegrees(startAngle);
    double arcExtent = Math.toDegrees(endAngle - startAngle);
    
    gc.fillArc(center.getX() - radius, center.getY() - radius, 
               radius * 2, radius * 2, 
               -startDegrees, -arcExtent, ArcType.ROUND);
}

private void drawArrow(GraphicsContext gc, Point2D start, Point2D end, Color color) {
    gc.setStroke(color);
    gc.setFill(color);
    
    double dx = end.getX() - start.getX();
    double dy = end.getY() - start.getY();
    double length = Math.sqrt(dx * dx + dy * dy);
    
    // Normalize direction
    double dirX = dx / length;
    double dirY = dy / length;
    
    // Line with some padding
    double padding = 6;
    gc.strokeLine(start.getX() + dirX * padding, start.getY() + dirY * padding,
                  end.getX() - dirX * padding, end.getY() - dirY * padding);
    
    // Arrowhead
    double arrowSize = 8;
    double arrowAngle = Math.PI / 6; // 30 degrees
    
    double backX = end.getX() - dirX * padding;
    double backY = end.getY() - dirY * padding;
    
    double perpX = -dirY;
    double perpY = dirX;
    
    double arrowX1 = backX - arrowSize * (dirX * Math.cos(arrowAngle) + perpX * Math.sin(arrowAngle));
    double arrowY1 = backY - arrowSize * (dirY * Math.cos(arrowAngle) + perpY * Math.sin(arrowAngle));
    
    double arrowX2 = backX - arrowSize * (dirX * Math.cos(arrowAngle) - perpX * Math.sin(arrowAngle));
    double arrowY2 = backY - arrowSize * (dirY * Math.cos(arrowAngle) - perpY * Math.sin(arrowAngle));
    
    gc.beginPath();
    gc.moveTo(end.getX() - dirX * padding, end.getY() - dirY * padding);
    gc.lineTo(arrowX1, arrowY1);
    gc.lineTo(arrowX2, arrowY2);
    gc.closePath();
    gc.fill();
}
    
    public static void main(String[] args) {
        launch(args);
    }
}