module com.t2drx {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.apache.pdfbox;

    opens com.t2drx to javafx.fxml;
    opens com.t2drx.controller to javafx.fxml;
    opens com.t2drx.model to javafx.fxml, javafx.base;

    exports com.t2drx;
    exports com.t2drx.model;
    exports com.t2drx.engine;
    exports com.t2drx.controller;
}
