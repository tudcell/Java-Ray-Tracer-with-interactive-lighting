module demo {
    requires javafx.graphics;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.base;

    opens demo to javafx.fxml; // Open the package to javafx.fxml
    exports demo;

}