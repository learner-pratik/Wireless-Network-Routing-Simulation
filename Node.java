import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Node {

    private static final String ipDirectory = "inputs/", opDirectory = "outputs/";
    
    private int ID, duration, destID;
    private String message;
    
    File input, output;
    private int[] neighbors;

    private HashMap<Integer, Integer> inTree;
    private HashMap<Integer, ArrayList<Integer>> revInTree;

    Node(int ID, int duration, int destID, String message) {
        this.ID = ID;
        this.duration = duration;
        this.destID = destID;
        this.message = message;

        neighbors = new int[10];
        for (int i = 0; i < 10; i++) neighbors[i] = -1;

        inTree = new HashMap<>();
        revInTree = new HashMap<>();

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
            String msg = "hello "+ID;
            
            writer.write(msg);
            writer.write(System.lineSeparator());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendRouting(BufferedWriter writer, int time) {
        // convert intree to string
        String tree = "";
        for (Map.Entry<Integer, Integer> entry : inTree.entrySet()) {
            tree += " "+"("+entry.getKey()+"-"+entry.getValue()+")";
        }

        try {
            String msg = "intree "+ID+tree;
            
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
    }

    private void buildInTree(HashMap<Integer, Integer> neighborInTree, String[] data) {
        for (int d = 0; d < data.length; d++) {
            
            String[] edge = data[d].split("-");

            int src = Integer.parseInt(edge[0]);
            int dest = Integer.parseInt(edge[1]);

            neighborInTree.put(src, dest);
        }
    }

    private void buildRevInTree(HashMap<Integer, ArrayList<Integer>> revInTree, HashMap<Integer, Integer> inTree) {
        for (Map.Entry<Integer, Integer> entry : inTree.entrySet()) {
            
            int src = entry.getKey();
            int dest = entry.getValue();

            if (!revInTree.containsKey(dest)) revInTree.put(dest, new ArrayList<>());

            ArrayList<Integer> l = revInTree.get(dest);
            l.add(src);

            revInTree.put(dest, l);
        }
    }

    private void computeHops(ArrayList<Integer> hops, HashMap<Integer, ArrayList<Integer>> revInTree, int node, int cnt, int hopVal) {
        if (cnt == hopVal) {
            hops.add(node);
            return;
        }

        ArrayList<Integer> neighbors = revInTree.get(node);

        if (neighbors == null) return;

        for (Integer neigh : neighbors) {
            computeHops(hops, revInTree, neigh, cnt+1, hopVal);
        }
    }

    private void computeInTree(String msg) {
        // build neighbor intree
        // msg format -> intree 4 (1-4) (3-4) (5-3) (2-1)
        HashMap<Integer, Integer> neighborInTree = new HashMap<>();
        HashMap<Integer, ArrayList<Integer>> revNeighborInTree = new HashMap<>();

        String msg1 = msg.replace("(", "").replace(")", "").replace("intree ", "");

        // if neighbor intree is the node itself, then add the edge
        if (msg1.length() == 1) {
            int neigh = Integer.parseInt(msg1);
            inTree.put(neigh, ID);
            buildRevInTree(revInTree, inTree);
            return;
        }
        
        int neighbor = Integer.parseInt(msg1.split(" ")[0]);
        
        String[] data = msg1.substring(2).split(" ");

        buildInTree(neighborInTree, data);

        // modify neighbor in-tree
        if (neighborInTree.containsKey(ID)) neighborInTree.remove(ID);
        neighborInTree.put(neighbor, ID);

        // build reverse intree by reversing the edges
        buildRevInTree(revNeighborInTree, neighborInTree);

        int hopCnt = 1;

        HashMap<Integer, Integer> newInTree = new HashMap<>();

        // process all neighbors
        while (true) {
            ArrayList<Integer> l1 = new ArrayList<>();
            ArrayList<Integer> l2 = new ArrayList<>();

            // get all n neighbors
            computeHops(l1, revInTree, ID, 0, hopCnt);
            computeHops(l2, revNeighborInTree, ID, 0, hopCnt);

            if (l1.isEmpty() && l2.isEmpty()) break;

            Collections.sort(l1);
            Collections.sort(l2);

            int c1 = 0, c2 = 0;
            
            while (c1 < l1.size() && c2 < l2.size()) {

                int v1 = l1.get(c1), v2 = l2.get(c2);

                if (v1 < v2) {
                    // remove edge from neighbor tree and add in final tree
                    neighborInTree.remove(v1);
                    if (l2.contains(v1)) l2.remove(Integer.valueOf(v1));
                    
                    newInTree.put(v1, inTree.get(v1));
                    
                    c1++;

                } else if (v1 > v2) {
                    // remove edge from intree and add in final tree
                    inTree.remove(v2);
                    if (l1.contains(v2)) l1.remove(Integer.valueOf(v2));
                    
                    newInTree.put(v2, neighborInTree.get(v2));
                    
                    c2++;

                } else {
                    int temp1 = v1, temp2 = v2;

                    // if both node values same, break tie by checking parents
                    while (temp1 == temp2) {
                        if (inTree.containsKey(temp1)) temp1 = inTree.get(temp1);
                        if (neighborInTree.containsKey(temp2)) temp2 = neighborInTree.get(temp2);

                        // if reached root, just add the edge from intree
                        if (temp1 == ID && temp2 == ID) {
                            newInTree.put(v1, inTree.get(v1));
                            inTree.remove(v1);
                            break;
                        }
                    }

                    if (temp1 < temp2) {
                        // remove edge from neighbor tree and add in final tree
                        if (neighborInTree.containsKey(v1)) neighborInTree.remove(v1);
                        if (l2.contains(v1)) l2.remove(Integer.valueOf(v1));

                        newInTree.put(v1, inTree.get(v1));
                        
                        c1++;

                    } else {
                        // remove edge from intree and add in final tree
                        if (inTree.containsKey(v2)) inTree.remove(v2);
                        if (l1.contains(v2)) l1.remove(Integer.valueOf(v2));
                        
                        newInTree.put(v2, neighborInTree.get(v2));

                        c2++;
                    }
                }
            }

            // if all n hop nodes are processed of neighbor tree
            while (c1 < l1.size()) {
                int v1 = l1.get(c1);

                if (neighborInTree.containsKey(v1)) neighborInTree.remove(v1);
                if (l2.contains(v1)) l2.remove(Integer.valueOf(v1));

                newInTree.put(v1, inTree.get(v1));

                c1++;
            }

            // if all n hop nodes are processed of intree
            while (c2 < l2.size()) {
                int v2 = l2.get(c2);

                if (inTree.containsKey(v2)) inTree.remove(v2);
                if (l1.contains(v2)) l1.remove(Integer.valueOf(v2));

                newInTree.put(v2, neighborInTree.get(v2));

                c2++;
            }

            // recompute the reversed intree to compute hops
            revInTree = new HashMap<>();
            revNeighborInTree = new HashMap<>();

            buildRevInTree(revInTree, inTree);
            buildRevInTree(revNeighborInTree, neighborInTree);

            hopCnt++;
        }

        inTree = newInTree;

        revInTree = new HashMap<>();
        buildRevInTree(revInTree, neighborInTree);

        System.out.println("Intree for " + getName() + inTree.toString());

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

    static boolean exit = false;
    private void startNode() {
        // thread for reading the input file
        Thread t = new Thread() {
            @Override
            public void run() {
                
                long lastLine = 0;
                while (!exit) {
                    lastLine = readInput(lastLine);
                }
            }
        };
        t.start();

        Object execution = new Object();
        try {
            synchronized(execution) {
                // long lastLine = 0;

                for (int time = 0; time < duration; time++) {
                    
                    BufferedWriter writer = new BufferedWriter(new FileWriter(output, true));
                    
                    if (time%5 == 0) sendHello(writer, time);
                    if (time%10 == 0) sendRouting(writer, time);
                    if (time%15 == 0) if (destID != -1) sendData(writer, time);
                    
                    // lastLine = readInput(lastLine);

                    processNeighbors();
                    
                    writer.close();
                    
                    execution.wait(1000);
                }
                // stop input file thread
                exit = true;
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