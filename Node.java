import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Node {

    private int ID, duration, destID;
    private String message;

    FileReader fReader;
    FileWriter fWriter;

    Node(int ID, int duration, int destID, String message) {
        this.ID = ID;
        this.duration = duration;
        this.destID = destID;
        this.message = message;

        setupFiles();
    }

    private void setupFiles() {
        try {
            File input = new File("input_"+ID+".txt");
            File output = new File("output_"+ID+".txt");
            input.createNewFile();
            output.createNewFile();

            fReader = new FileReader(input);
            fWriter = new FileWriter(output, true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startNode() {
        for (int t = 0; t < duration; t++) {
            if (t%10 == 0) {
                try {
                    String msg = "data"+t;
                    fWriter.append(msg);
                    fWriter.append(System.lineSeparator());
                    System.out.println("Node"+ID+"written message-"+msg);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        int id = Integer.parseInt(args[0]);
        int time = Integer.parseInt(args[1]);
        int dest = Integer.parseInt(args[2]);
        String msg = (dest != -1) ? args[3] : null;
        
        Node node = new Node(id, time, dest, msg);
        node.startNode();
    }

}