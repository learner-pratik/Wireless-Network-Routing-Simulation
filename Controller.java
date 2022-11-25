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

    Controller(int duration) {
        this.duration = duration;
        
        topologyFile = new File("topology.txt");
        topology = new HashMap<Integer, ArrayList<Integer>>(10);
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

    class NodeFile {
        File file;
        int fileNo;
        long lastLine;

        NodeFile(int fileNo, File file) {
            this.fileNo = fileNo;
            this.file = file;
            lastLine = 0;
        }

        void readAndProcess() {
            try {    
                if (lastLine < file.length()) {
                    BufferedReader reader = new BufferedReader(new FileReader(file));
                    reader.skip(lastLine);
                    
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        
                        for (Integer node : topology.get(fileNo)) {
                            String ipPath = ipDirectory+"input_"+node+".txt";
                            File input = new File(ipPath);

                            if (input.exists()) {
                                BufferedWriter writer = new BufferedWriter(new FileWriter(input, true));
                                
                                writer.write(line);
                                writer.write(System.lineSeparator());
                                System.out.println("Controller written data");
                                
                                writer.close();
                            }
                        }
                    }
                    
                    lastLine = file.length();
                    reader.close();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void startController() {
        // read the topology file and create the topology graph
        buildTopology();

        NodeFile[] nodeFiles = new NodeFile[10];
        for (int f = 0; f < 10; f++) nodeFiles[f] = null;

        long currTime = System.currentTimeMillis();
        while (System.currentTimeMillis() < currTime+(1000*duration)) {

            for (int id = 0; id < 10; id++) {
                
                String outputPath = opDirectory+"output_"+id+".txt";
                if (new File(outputPath).exists()) {
                    
                    if (nodeFiles[id] == null) {
                        File opFile = new File(outputPath);
                        nodeFiles[id] = new NodeFile(id, opFile);
                    }

                    if (nodeFiles[id] != null) nodeFiles[id].readAndProcess();
                }
            }
        }
    }

    public static void main(String[] args) {
        int time = Integer.parseInt(args[0]);

        Controller controller = new Controller(time);
        controller.startController();
    }
}