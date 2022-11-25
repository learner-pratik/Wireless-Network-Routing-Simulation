import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class Controller {

    private static final String ipDirectory = "inputs/", opDirectory = "outputs/";
    private int duration;
    private File topologyFile;
    private HashMap<Integer, ArrayList<Integer>> topology;
    private Channel[] channels; 

    Controller(int duration) {
        this.duration = duration;
        topologyFile = new File("topology.txt");
        topology = new HashMap<Integer, ArrayList<Integer>>(10);
        channels = new Channel[10];
    }

    private void startController() {
        // read the topology file and create the topology graph
        buildTopology();

        Object execution = new Object();
        try {
            synchronized(execution) {
                for (int time = 0; time < duration; time++) {
                    
                    for (int id = 0; id < 10; id++) {
                        String inputFile = ipDirectory+"input_"+id+".txt";
                        if (new File(inputFile).exists()) {
                            if (channels[id] == null) {
                                channels[id] = new Channel(id);
                                channels[id].process();
                            } else channels[id].process();
                        }
                    }

                    System.out.println("Controller executing-"+time);
                    execution.wait(1000);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void buildTopology() {
        try {
            BufferedReader tReader = new BufferedReader(new FileReader(topologyFile));
            
            String line = tReader.readLine();
            while (line != null) {
                String[] lineSplit = line.split(" ");
                int src = Integer.parseInt(lineSplit[0]);
                int dest = Integer.parseInt(lineSplit[1]);

                if (!topology.containsKey(src)) {
                    topology.put(src, new ArrayList<Integer>());
                }
                ArrayList<Integer> l = topology.get(src);
                l.add(dest);
                topology.put(src, l);

                line = tReader.readLine();
            }

            tReader.close();
            System.out.println(topology.toString());

        } catch (FileNotFoundException e) {
            System.out.println("File not found");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class Channel {

        String output;
        int id, lastLine;

        Channel(int id) {
            this.id = id;
            output = opDirectory+"output_"+id+".txt";
            lastLine = 0;
        }

        void process() {
            try {
                int start = 0;
                BufferedReader reader = new BufferedReader(new FileReader(output));
                String line = reader.readLine();
                while (line != null) {
                    if (start >= lastLine) {

                        for (Integer neighbor : topology.get(id)) {
                            File nFile = new File(ipDirectory+"input_"+neighbor+".txt");
                            BufferedWriter writer = new BufferedWriter(new FileWriter(nFile));
                            writer.write(line);
                            writer.write(System.lineSeparator());
                            writer.close();
                        }
                        // System.out.println("Line read from output-"+line);

                    }
                    start++;
                    line = reader.readLine();
                }
                lastLine = start;
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        int time = Integer.parseInt(args[0]);

        Controller controller = new Controller(time);
        controller.startController();
    }
}