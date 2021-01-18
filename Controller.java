import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller {

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private Button button;

    @FXML
    private TextArea message;

    @FXML
    void initialize() {
        button.setOnAction(actionEvent -> Platform.exit());
        message.setText(SuccessfulUploadMessage.path);
    }
}