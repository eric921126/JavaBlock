module com.eric.javablock {
    requires javafx.controls;
    requires javafx.fxml;
    requires jakarta.json;
    requires java.desktop;

    opens com.eric.javablock to javafx.fxml;
    exports com.eric.javablock;
}