import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

public class Node {

    private static final String ipDirectory = "inputs/", opDirectory = "outputs/";
    private int ID, duration, destID;
    private String message;
    File input, output;
    private int[] neighbors;

    Node(int ID, int duration, int destID, String message) {
        this.ID = ID;
        this.duration = duration;
        this.destID = destID;
        this.message = message;

        neighbors = new int[10];
        for (int i = 0; i < 10; i++) neighbors[i] = -1;

        setupFiles();
    }

    private String getName() {
        return "Node-"+ID+": ";
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

    private void sendHello(BufferedWriter writer, int time) {
        try {
            String msg = "hello "+ID+" at time-"+time;
            
            writer.write(msg);
            writer.write(System.lineSeparator());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendRouting(BufferedWriter writer, int time) {
        try {
            String msg = "intree "+ID+" at time-"+time;
            
            writer.write(msg);
            writer.write(System.lineSeparator());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendData(BufferedWriter writer, int time) {
        try {
            String msg = "data "+ID+" "+message+" at time-"+time;
            
            writer.write(msg);
            writer.write(System.lineSeparator());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processHello(String msg) {
        String neighID = msg.split(" ")[1];
        neighbors[Integer.parseInt(neighID)] = 0;
        System.out.println(getName() + Arrays.toString(neighbors));
    }

    private void computeInTree(String msg) {
        return;
    }

    private void processNeighbors() {
        // increment counter of all neighbors
        for (int i = 0; i < 10; i++) {
            if (neighbors[i] != -1) neighbors[i]++;
            if (neighbors[i] >= 30) neighbors[i] = -1;
        }
    }

    private void processData(String msg) {
        return;
    }

    private long readInput(long lastLine) {
        try {
            if (lastLine < input.length()) {
                BufferedReader reader = new BufferedReader(new FileReader(input));
                reader.skip(lastLine);

                String line = null;
                while ((line = reader.readLine()) != null) {
                    String type = line.split(" ")[0];
                    switch(type) {
                        case "hello":
                            processHello(line);
                            break;
                        case "intree":
                            computeInTree(line);
                            break;
                        case "data":
                            processData(line);
                            break;
                    }
                }
                
                reader.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return input.length();
    }

    // static boolean exit = false;
    private void startNode() {
        // thread for reading the input file
        // Thread t = new Thread() {
        //     @Override
        //     public void run() {
                
        //         long lastLine = 0;
        //         while (!exit) {
        //             lastLine = readInput(lastLine);
        //         }
        //     }
        // };
        // t.start();

        Object execution = new Object();
        try {
            synchronized(execution) {
                long lastLine = 0;

                for (int time = 0; time < duration; time++) {
                    
                    BufferedWriter writer = new BufferedWriter(new FileWriter(output, true));
                    
                    if (time%5 == 0) sendHello(writer, time);
                    if (time%10 == 0) sendRouting(writer, time);
                    if (time%15 == 0) if (destID != -1) sendData(writer, time);
                    
                    lastLine = readInput(lastLine);

                    processNeighbors();
                    
                    writer.close();
                    
                    execution.wait(1000);
                }
                // stop input file thread
                // exit = true;
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