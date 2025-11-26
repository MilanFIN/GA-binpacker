package com.binpacker.app;

import com.binpacker.lib.common.BoxSpec;
import com.binpacker.lib.common.Point3f;
import com.binpacker.lib.optimizer.GAOptimizer;
import com.binpacker.lib.optimizer.Optimizer;
import com.binpacker.lib.solver.BestFit3D;
import com.binpacker.lib.solver.FirstFit2D;
import com.binpacker.lib.solver.FirstFit3D;
import com.binpacker.lib.solver.Solver;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.DrawMode;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GuiApp extends Application {

	private Group world;
	private Label statusLabel;

	// Camera controls
	private double lastMouseX;
	private double lastMouseY;
	private final Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
	private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);

	@Override
	public void start(Stage primaryStage) {
		BorderPane root = new BorderPane();

		// 3D Scene
		world = new Group();
		SubScene subScene = create3DScene(world);
		root.setCenter(subScene);

		// Controls
		HBox controls = new HBox(10);
		Button solveButton = new Button("Solve");
		statusLabel = new Label("Ready");
		controls.getChildren().addAll(solveButton, statusLabel);
		controls.setStyle("-fx-padding: 10; -fx-alignment: center-left;");
		root.setBottom(controls);

		// Event Handling
		solveButton.setOnAction(e -> runSolver());

		Scene scene = new Scene(root, 800, 600);
		primaryStage.setTitle("Bin Packer 3D");
		primaryStage.setScene(scene);
		primaryStage.show();
	}

	private SubScene create3DScene(Group world) {
		world.getChildren().clear();

		SubScene subScene = new SubScene(world, 800, 550, true, javafx.scene.SceneAntialiasing.BALANCED);

		PerspectiveCamera camera = new PerspectiveCamera(true);
		camera.setTranslateZ(100);
		camera.setRotationAxis(Rotate.Y_AXIS);
		camera.setRotate(180);
		camera.setTranslateY(0);
		camera.setTranslateX(0);
		camera.setNearClip(0.1);
		camera.setFarClip(1000.0);
		camera.setFieldOfView(45);

		// Camera Group for rotation
		Group cameraGroup = new Group();
		cameraGroup.getChildren().add(camera);
		cameraGroup.getTransforms().addAll(rotateY, rotateX);
		world.getChildren().add(cameraGroup);

		subScene.setCamera(camera);

		// Event Handling for Camera
		subScene.setOnMousePressed(event -> {
			lastMouseX = event.getSceneX();
			lastMouseY = event.getSceneY();
			subScene.requestFocus();
		});

		subScene.setOnMouseDragged(event -> {
			if (event.isPrimaryButtonDown()) {
				double dx = event.getSceneX() - lastMouseX;
				double dy = event.getSceneY() - lastMouseY;

				rotateY.setAngle(rotateY.getAngle() + dx * 0.5);
				rotateX.setAngle(rotateX.getAngle() + dy * 0.5);
			}
			lastMouseX = event.getSceneX();
			lastMouseY = event.getSceneY();
		});

		subScene.setOnScroll(event -> {
			double delta = -event.getDeltaY();
			double newZ = camera.getTranslateZ() + delta * 0.5;
			// Limit zoom? Maybe not strictly necessary for now.
			camera.setTranslateZ(newZ);
		});
		subScene.setOnKeyPressed(event -> {
			double moveAmount = 5.0; // Define how much the camera moves per key press
			switch (event.getCode()) {
				case LEFT:
					camera.setTranslateX(camera.getTranslateX() + moveAmount);
					break;
				case RIGHT:
					camera.setTranslateX(camera.getTranslateX() - moveAmount);
					break;
				default:
					break;
			}
		});

		// Request focus for the subScene so it can receive key events
		subScene.setFocusTraversable(true);
		subScene.requestFocus();

		// Add some axes for reference
		Box xAxis = new Box(100, 0.5, 0.5);
		xAxis.setMaterial(new PhongMaterial(Color.RED));
		Box yAxis = new Box(0.5, 100, 0.5);
		yAxis.setMaterial(new PhongMaterial(Color.GREEN));
		Box zAxis = new Box(0.5, 0.5, 100);
		zAxis.setMaterial(new PhongMaterial(Color.BLUE));

		world.getChildren().addAll(xAxis, yAxis, zAxis);

		return subScene;
	}

	private void runSolver() {
		statusLabel.setText("Solving...");

		world.getChildren().removeIf(node -> node instanceof Box && ((Box) node).getWidth() < 50); // Keep axes

		// Generate Data
		List<BoxSpec> boxes = generateRandomBoxes(500);
		BoxSpec bin = new BoxSpec(new Point3f(0, 0, 0), new Point3f(30, 30, 30));
		// Solve
		Solver solver = new FirstFit3D();
		Optimizer optimizer = new GAOptimizer();
		optimizer.initialize(solver, boxes, bin, 1000, 20);

		// Predefine a list of colors for visualization
		Random random = new Random();
		List<Color> boxColors = new ArrayList<>();
		// Assuming 1000 is the number of boxes generated by generateRandomBoxes(1000)
		for (int i = 0; i < boxes.size(); i++) {
			boxColors.add(Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
		}

		Task<Void> solverTask = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				// Create a dedicated group for solver output to easily clear it
				final Group solverOutputGroup = new Group();
				Platform.runLater(() -> {
					// Clear previous solver output from the main world group, keeping axes
					world.getChildren().removeIf(node -> node instanceof Box && ((Box) node).getWidth() < 50);
					world.getChildren().add(solverOutputGroup);
				});

				for (int i = 0; i < 100; i++) {
					final List<List<BoxSpec>> result = optimizer.executeNextGeneration();
					final double rawRate = optimizer.rate(result, bin) * 100;
					final String rate = String.format("%.2f", rawRate);
					final int generation = i + 1;

					Platform.runLater(() -> {
						statusLabel
								.setText("Solving... Generation " + generation + " complete, " + rate + "% full");

						solverOutputGroup.getChildren().clear(); // Clear previous generation's visualization

						// Visualize current generation
						int binOffset = -50;

						for (List<BoxSpec> binBoxes : result) {
							for (BoxSpec spec : binBoxes) {
								// Use the box's ID to get a consistent color
								Color boxColor = boxColors.get(spec.id % boxColors.size());
								PhongMaterial boxMaterial = new PhongMaterial(boxColor);
								Box box = new Box(spec.size.x, spec.size.y, spec.size.z);
								box.setMaterial(boxMaterial);

								// JavaFX Box is centered at (0,0,0), so we need to offset by half size
								box.setTranslateX(spec.position.x + spec.size.x / 2 + binOffset);
								box.setTranslateY(spec.position.y + spec.size.y / 2);
								box.setTranslateZ(spec.position.z + spec.size.z / 2);

								solverOutputGroup.getChildren().add(box);
							}

							// Draw bin outline
							Box binBox = new Box(bin.size.x, bin.size.y, bin.size.z);
							binBox.setDrawMode(DrawMode.LINE);
							binBox.setMaterial(new PhongMaterial(Color.BLACK));
							binBox.setTranslateX(bin.position.x + bin.size.x / 2 + binOffset);
							binBox.setTranslateY(bin.position.y + bin.size.y / 2);
							binBox.setTranslateZ(bin.position.z + bin.size.z / 2);
							solverOutputGroup.getChildren().add(binBox);

							binOffset += 40; // Space out bins
						}
					});

				}
				return null;
			}
		};
		new Thread(solverTask).start();

		// statusLabel.setText("Solved! Bins: " + result.size());
	}

	private List<BoxSpec> generateRandomBoxes(int count) {
		List<BoxSpec> boxes = new ArrayList<>();
		Random random = new Random();
		for (int i = 0; i < count; i++) {
			float width = random.nextInt(8) + 4;
			float height = random.nextInt(8) + 4;
			float depth = random.nextInt(8) + 4;
			BoxSpec box = new BoxSpec(new Point3f(0, 0, 0), new Point3f(width, height, depth));
			box.id = i;
			boxes.add(box);
		}
		return boxes;
	}
}
