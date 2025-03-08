package demo;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;


/**
 * Represents a 3D vector with x, y, and z components.
 */
class Vector3 {
    public final double x, y, z;

    public Vector3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3 subtract(Vector3 v) {
        if (v == null) {
            throw new IllegalArgumentException("Vector cannot be null.");
        }
        return new Vector3(x - v.x, y - v.y, z - v.z);
    }

    public Vector3 normalize() {
        double mag = Math.sqrt(x * x + y * y + z * z);
        if (mag == 0) {
            throw new ArithmeticException("Cannot normalize a zero vector.");
        }
        return new Vector3(x / mag, y / mag, z / mag);
    }

    public double dot(Vector3 v) {
        if (v == null) {
            throw new IllegalArgumentException("Vector cannot be null.");
        }
        return x * v.x + y * v.y + z * v.z;
    }

    public Vector3 multiply(double scalar) {
        return new Vector3(x * scalar, y * scalar, z * scalar);
    }

    public Vector3 add(Vector3 v) {
        if (v == null) {
            throw new IllegalArgumentException("Vector cannot be null.");
        }
        return new Vector3(x + v.x, y + v.y, z + v.z);
    }
}



/**
 * Represents a ray with an origin and a direction.
 */
class Ray {
    public final Vector3 origin, direction;

    public Ray(Vector3 origin, Vector3 direction) {
        if (origin == null || direction == null) {
            throw new IllegalArgumentException("Origin and direction cannot be null.");
        }
        this.origin = origin;
        this.direction = direction.normalize();
    }
}

/**
 * Represents a sphere with a center, radius, and color.
 */
class Sphere {
    public Vector3 center;
    public final double radius;
    public final Color color;
    public final boolean isLightSource; // Indicates if the sphere is a light source

    public Sphere(Vector3 center, double radius, Color color, boolean isLightSource) {
        if (center == null || radius <= 0 || color == null) {
            throw new IllegalArgumentException("Invalid sphere parameters: center, radius, or color cannot be null, and radius must be positive.");
        }
        this.center = center;
        this.radius = radius;
        this.color = color;
        this.isLightSource = isLightSource;
    }

    /**
     * Calculates the intersection of the ray with the sphere.
     * Returns the smallest positive t value (distance along the ray) or -1 if no intersection.
     */
    public double intersect(Ray ray) {
        if (ray == null) {
            throw new IllegalArgumentException("Ray cannot be null.");
        }

        Vector3 oc = ray.origin.subtract(center);
        double a = ray.direction.dot(ray.direction);
        double b = 2.0 * oc.dot(ray.direction);
        double c = oc.dot(oc) - radius * radius;
        double discriminant = b * b - 4 * a * c;

        if (discriminant < 0) return -1.0; // No intersection

        double t1 = (-b - Math.sqrt(discriminant)) / (2.0 * a);
        double t2 = (-b + Math.sqrt(discriminant)) / (2.0 * a);

        if (t1 > 0 && t2 > 0) return Math.min(t1, t2); // Return the closest intersection
        if (t1 > 0) return t1;
        if (t2 > 0) return t2;
        return -1.0; // No valid intersection
    }
}



/**
 * Main application class for the ray tracing simulation.
 */
public class RayTracingApp extends Application {
    private Canvas canvas;
    private List<Sphere> spheres;
    private Sphere selectedSphere = null;
    private List<Vector3> lightSources; // List of light sources

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("JavaFX Ray Tracing Simulation");
        canvas = new Canvas(800, 600);

        // Initialize spheres
        spheres = new ArrayList<>();
        lightSources = new ArrayList<>();

        try {
            // Add light sources
            Sphere lightSphere1 = new Sphere(new Vector3(-0.5, 0, -3), 0.6, Color.RED, true);
            Sphere lightSphere2 = new Sphere(new Vector3(0.5, 1, -3), 0.2, Color.YELLOW, true);
            spheres.add(lightSphere1);
            spheres.add(lightSphere2);
            lightSources.add(lightSphere1.center);
            lightSources.add(lightSphere2.center);

            // Add other spheres
            spheres.add(new Sphere(new Vector3(0.5, 0, -3), 0.5, Color.BLUE, false));
        } catch (IllegalArgumentException e) {
            System.err.println("Error initializing spheres: " + e.getMessage());
            return;
        }

        // Add mouse event handlers
        canvas.setOnMousePressed(this::onMousePressed);
        canvas.setOnMouseDragged(this::onMouseDragged);

        StackPane root = new StackPane(canvas);
        primaryStage.setScene(new Scene(root));
        primaryStage.show();

        renderScene(); // Render the initial scene
    }

    /**
     * Renders the scene by tracing rays for each pixel on the canvas.
     */
    private void renderScene() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        int width = (int) canvas.getWidth();
        int height = (int) canvas.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double u = (x - width / 2.0) / (width / 2.0);
                double v = (y - height / 2.0) / (height / 2.0);
                Ray ray = new Ray(new Vector3(0, 0, 0), new Vector3(u, v, -1));

                Color pixelColor = Color.BLACK;
                double minT = Double.MAX_VALUE;
                Sphere closestSphere = null;

                for (Sphere sphere : spheres) {
                    try {
                        double t = sphere.intersect(ray);
                        if (t > 0 && t < minT) {
                            minT = t;
                            closestSphere = sphere;
                        }
                    } catch (IllegalArgumentException e) {
                        System.err.println("Error calculating intersection: " + e.getMessage());
                        continue;
                    }
                }

                if (closestSphere != null) {
                    if (closestSphere.isLightSource) {
                        pixelColor = closestSphere.color; // Light source remains fully lit
                    } else {
                        try {
                            Vector3 hitPoint = ray.origin.add(ray.direction.multiply(minT));
                            Vector3 normal = hitPoint.subtract(closestSphere.center).normalize();

                            // Accumulate lighting from all light sources
                            double totalBrightness = 0.0;
                            for (Vector3 lightSource : lightSources) {
                                Vector3 lightDir = lightSource.subtract(hitPoint).normalize();
                                double diffuse = Math.max(0, normal.dot(lightDir));

                                // Compute squared distance between light and sphere center
                                double sphereDistance = lightSource.subtract(closestSphere.center).dot(lightSource.subtract(closestSphere.center));
                                double R_light = spheres.get(lightSources.indexOf(lightSource)).radius; // Radius of the light source
                                double R_target = closestSphere.radius; // Radius of the target sphere
                                double sumRadii = R_light + R_target; // Combined radius for intersection check

                                // Check if spheres intersect and light source is larger
                                if (sphereDistance < sumRadii * sumRadii && R_light >= R_target) {
                                    // Calculate blend factor based on intersection depth
                                    double d = Math.sqrt(sphereDistance); // Actual distance between centers
                                    double intersectionDepth = R_light - (d - R_target); // How deep the light is in the sphere
                                    double blendFactor = Math.max(0, Math.min(1, intersectionDepth / R_light)); // Normalize

                                    // Fully light the parts facing the light source
                                    if (diffuse > 0) {
                                        diffuse = (1 - blendFactor) * diffuse + blendFactor * 1.0;
                                    }
                                }

                                double lightDistance = hitPoint.subtract(lightSource).dot(hitPoint.subtract(lightSource));
                                double k = 0.4; // Attenuation factor
                                totalBrightness += diffuse / (1 + k * lightDistance);
                            }

                            double ambient = 0.25; // Base ambient light
                            double brightness = ambient + totalBrightness;
                            brightness = Math.min(1, brightness); // Clamp brightness

                            pixelColor = closestSphere.color.deriveColor(0, 1, brightness, 1);
                        } catch (ArithmeticException | IllegalArgumentException e) {
                            System.err.println("Error calculating lighting: " + e.getMessage());
                            pixelColor = Color.BLACK; // Fallback color
                        }
                    }
                }

                gc.getPixelWriter().setColor(x, y, pixelColor);
            }
        }
    }

    /**
     * Handles mouse press events to select a sphere.
     */
    private void onMousePressed(MouseEvent event) {
        try {
            Ray clickRay = new Ray(new Vector3(0, 0, 0),
                    new Vector3(
                            (event.getX() - canvas.getWidth() / 2.0) / (canvas.getWidth() / 2.0),
                            (event.getY() - canvas.getHeight() / 2.0) / (canvas.getHeight() / 2.0),
                            -1
                    ).normalize());

            selectedSphere = null;
            double minT = Double.MAX_VALUE;
            for (Sphere sphere : spheres) {
                double t = sphere.intersect(clickRay);
                if (t > 0 && t < minT) {
                    minT = t;
                    selectedSphere = sphere;
                }
            }
        } catch (IllegalArgumentException | ArithmeticException e) {
            System.err.println("Error handling mouse press: " + e.getMessage());
        }
    }

    /**
     * Handles mouse drag events to move the selected sphere.
     */
    private void onMouseDragged(MouseEvent event) {
        if (selectedSphere != null) {
            try {
                double x = (event.getX() - canvas.getWidth() / 2.0) / (canvas.getWidth() / 2.0);
                double y = (event.getY() - canvas.getHeight() / 2.0) / (canvas.getHeight() / 2.0);
                selectedSphere.center = new Vector3(x, y, -3);

                // Update light source position if the selected sphere is a light source
                if (selectedSphere.isLightSource) {
                    lightSources.set(spheres.indexOf(selectedSphere), selectedSphere.center);
                }

                renderScene();
            } catch (IllegalArgumentException e) {
                System.err.println("Error handling mouse drag: " + e.getMessage());
            }
        }
    }
}