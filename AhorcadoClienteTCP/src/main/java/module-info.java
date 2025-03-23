module org.example.ahorcadoborradofx {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.ahorcadoborradofx to javafx.fxml;
    exports org.example.ahorcadoborradofx;
}