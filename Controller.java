import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class Controller {

    private int duration;
    private File topologyFile;
    private HashMap<Integer, ArrayList<Integer>> topology;
    private Channel[] channels; 

    Controller(int duration) {
        this.duration = duration;
        // topologyFile = new File("topology.txt");
        topology = new HashMap<Integer, ArrayList<Integer>>(10);
        channels = new Channel[10];
    }

    private void startController() {
        // read the topology file and create the topology graph
        buildTopology();

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                // read and write files
                for (int id = 0; id < 10; id++) {
                    File input = new File("input_"+id);
                    if (input.exists()) {
                        if (channels[id] == null) {
                            channels[id] = new Channel(id);
                        } else {
                            channels[id].process();
                        }
                    }
                }
                System.out.println("printing");
            }
            
        }, duration*1000);
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

        BufferedReader frInput;
        FileWriter fwOutput;
        int lastInput;

        Channel(int id) {
            String input = "input_"+id;
            String output = "output_"+id;
            lastInput = 0;

            try {
                frInput = new BufferedReader(new FileReader(input));
                fwOutput = new FileWriter(output);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void process() {
            int start = 0;
            String line;
            try {
                line = frInput.readLine();
                while (line != null) {
                    if (start > lastInput) {
                        fwOutput.append(line);
                        fwOutput.append(System.lineSeparator());
                    }
                    start++;
                    line = frInput.readLine();
                }
                lastInput = start;
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