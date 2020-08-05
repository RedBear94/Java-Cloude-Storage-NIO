import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    public ListView<String> lv;
    public TextField txt;
    public Button send;
    private Socket socket;
    private DataInputStream is;
    private DataOutputStream os;
    private final String clientFilesPath = "./common/src/main/resources/clientFiles";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            socket = new Socket("localhost", 8189);
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        File dir = new File(clientFilesPath);
        for (String file : dir.list()) {
            lv.getItems().add(file);
        }
    }

    // ./download fileName
    // ./upload fileName
    public void sendCommand(ActionEvent actionEvent) {
        String command = txt.getText();
        String [] op = command.split(" ");
        if (op[0].equals("./download")) {
            try {
                os.writeBytes(op[0]+" ");
                os.writeBytes(op[1]);

                byte [] responseBuffer = new byte[2];
                is.read(responseBuffer);
                String response = new String(responseBuffer);

                if (response.equals("OK")) {
                    File file = new File(clientFilesPath + "/" + op[1]);
                    if (!file.exists()) {
                        file.createNewFile();
                        lv.getItems().add(op[1]);
                    } else {
                        System.out.println("exists");
                    }

                    byte [] buffer = new byte[1];

                    try(FileOutputStream fos = new FileOutputStream(file)) {
                        do {
                            is.read(buffer);
                            response = new String(buffer);
                            System.out.println(response);
                            if(!response.equals("|")){
                                fos.write(buffer, 0, 1);
                            }
                        } while(!response.equals("|"));
                    }
                    System.out.println("File uploaded!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (op[0].equals("./upload")) {
            try {
                os.writeBytes(op[0]+" ");
                os.writeBytes(op[1]);

                File file = new File(clientFilesPath + "/" + op[1]);
                if(file.exists()) {
                    FileInputStream fileIn = new FileInputStream(file);

                    byte[] buf = new byte[Short.MAX_VALUE];
                    int bytesRead;
                    while ((bytesRead = fileIn.read(buf)) != -1) {
                        //os.writeShort(bytesRead);
                        os.write(buf, 0, bytesRead);
                    }
                    //os.writeShort(-1);
                    fileIn.close();
                    System.out.println("Файл отправлен");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
