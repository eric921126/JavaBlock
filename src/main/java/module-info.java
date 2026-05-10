module com.eric.javablock {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.eric.javablock to javafx.fxml;
    exports com.eric.javablock;
}