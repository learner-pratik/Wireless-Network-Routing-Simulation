import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

public class Node {

    private static final String ipDirectory = "inputs/", opDirectory = "outputs/";
    private int ID, duration, destID;
    private String message;
    File input, output;

    Node(int ID, int duration, int destID, String message) {
        this.ID = ID;
        this.duration = duration;
        this.destID = destID;
        this.message = message;

        setupFiles();
    }

    private void setupFiles() {
        try {
            input = new File(ipDirectory+"input_"+ID+".txt");
            output = new File(opDirectory+"output_"+ID+".txt");
            if (!input.exists()) input.createNewFile();
            if (!output.exists()) output.createNewFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendHello(BufferedWriter writer) {
        try {
            String msg = "hello "+ID;
            writer.write(msg);
            writer.write(System.lineSeparator());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startNode() {
        Object execution = new Object();
        try {
            synchronized(execution) {
                BufferedWriter writer = new BufferedWriter(new FileWriter(output));
                for (int time = 0; time < duration; time++) {
                    if (time%5 == 0) sendHello(writer);
                    System.out.println("Node"+ID+" executing for-"+time);
                    execution.wait(1000);
                }
                writer.close();
            }
        } catch(InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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