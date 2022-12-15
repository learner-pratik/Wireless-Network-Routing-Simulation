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
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class Node {

    private static final String ipDirectory = "inputs/", opDirectory = "outputs/", dataDirectory = "data/";
    
    private int ID, duration, destID;
    private String message;
    
    File input, output, dataReceived; // file pointer for node files
    private int[] neighbors; // list of existing neighbors

    private HashMap<Integer, Integer> inTree; // stores latest intree for the node
    private HashMap<Integer, ArrayList<Integer>> revInTree; // intree in reversed form, used for intree computation

    private ArrayList<HashMap<Integer, Integer>> neighborIntreeList; // stores intrees of neighbors, used when transmitting data

    Node(int ID, int duration, int destID, String message) {
        this.ID = ID;
        this.duration = duration;
        this.destID = destID;
        this.message = message;

        // -1 indicates neighbors dont exist
        neighbors = new int[10];
        for (int i = 0; i < 10; i++) neighbors[i] = -1;

        inTree = new HashMap<>();
        revInTree = new HashMap<>();

        neighborIntreeList = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) neighborIntreeList.add(null);

        setupFiles();
    }

    // intializes all the node files
    private void setupFiles() {
        try {
            input = new File(ipDirectory+"input_"+ID+".txt");
            output = new File(opDirectory+"output_"+ID+".txt");
            dataReceived = new File(dataDirectory+ID+"_received"+".txt");
            
            if (!input.exists()) input.createNewFile();
            if (!output.exists()) output.createNewFile();
            if (!dataReceived.exists()) dataReceived.createNewFile();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // method to sent hello message
    private void sendHello(BufferedWriter writer) {
        try {
            String msg = "hello "+ID;
            
            writer.write(msg);
            writer.write(System.lineSeparator());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // method to send intree message
    private void sendRouting(BufferedWriter writer) {
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

    // method to compute path for the source node
    private String computePath() {
        // Algo steps
        // 1. Check node intree to find a path from dest to node and reverse it
        // 2. As per the path, check the first node (say X) and find path to reach there
        // 3. Using X intree, get path from node to X (say P). This path doesn't change.
        // 4. Send message to first node in this path P

        String dataMsg = "data "+ID+" ";

        // check intree to see if dest exists
        if (!inTree.containsKey(destID)) return null;

        // if dest exists find first node in path
        // also store this path in list
        ArrayList<Integer> path = new ArrayList<>();
        int node = destID;
        while (node != ID) {
            path.add(node);
            node = inTree.get(node);
        }

        // check if nodes intrees is obtained
        int firstNeigh = path.get(path.size()-1);
        if (neighborIntreeList.get(firstNeigh) == null) return null;
        HashMap<Integer, Integer> nodeInTree = neighborIntreeList.get(firstNeigh);

        // check if you exist in this node intree
        if (!nodeInTree.containsKey(ID)) return null;

        // add src routing path
        dataMsg += "(";
        Integer node1 = nodeInTree.get(ID);
        while (node1 != null) {
            dataMsg += node1+" ";
            node1 = nodeInTree.get(node1);
        }
        dataMsg = dataMsg.substring(0, dataMsg.length()-1);
        dataMsg += ")";

        // add path to data message
        path.remove(path.size()-1);
        while (!path.isEmpty()) {
            dataMsg += " " + path.remove(path.size()-1);
        }
        dataMsg += " begin "+message;

        return dataMsg;
    }

    // method to send data message
    private void sendData(BufferedWriter writer) {

        String dataMsg = computePath();
        if (dataMsg == null) return;

        try {
            writer.write(dataMsg);
            writer.write(System.lineSeparator());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // on receiving hello message, counter reset to 0
    private void processHello(String msg) {
        String neighID = msg.split(" ")[1];
        neighbors[Integer.parseInt(neighID)] = 0;
    }

    // method to convert received intree message to intree data structure
    private void buildInTree(HashMap<Integer, Integer> tree, String[] data) {
        for (int d = 0; d < data.length; d++) {
            
            String[] edge = data[d].split("-");

            int src = Integer.parseInt(edge[0]);
            int dest = Integer.parseInt(edge[1]);

            tree.put(src, dest);
        }
    }

    // helper method which reverses the intree
    private void buildRevInTree(HashMap<Integer, ArrayList<Integer>> revInTree, HashMap<Integer, Integer> inTree) {
        for (Map.Entry<Integer, Integer> entry : inTree.entrySet()) {
            
            int src = entry.getKey();
            int dest = entry.getValue();

            if (!revInTree.containsKey(dest)) revInTree.put(dest, new ArrayList<Integer>());

            ArrayList<Integer> l = revInTree.get(dest);
            l.add(src);

            revInTree.put(dest, l);
        }
    }

    // helper method which computes hop count of all nodes in tree
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

    // main method which computes intree for node
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
            
            revInTree = new HashMap<>();
            buildRevInTree(revInTree, inTree);

            return;
        }
        
        int neighbor = Integer.parseInt(msg1.split(" ")[0]);
        
        String[] data = msg1.substring(2).split(" ");

        buildInTree(neighborInTree, data); // convert data message into data structure

        // add this neighbor intree to our neighbor intrees list
        HashMap<Integer, Integer> neighTree = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : neighborInTree.entrySet()) {
            neighTree.put(entry.getKey(), entry.getValue());
        }
        neighborIntreeList.set(neighbor, neighTree);

        // modify neighbor in-tree
        if (neighborInTree.containsKey(ID)) neighborInTree.remove(ID);
        neighborInTree.put(neighbor, ID);

        // build reverse intree by reversing the edges
        buildRevInTree(revNeighborInTree, neighborInTree);

        int hopCnt = 1;

        HashMap<Integer, Integer> newInTree = new HashMap<>();

        // store old intree
        HashMap<Integer, Integer> oldInTree = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : inTree.entrySet()) {
            oldInTree.put(entry.getKey(), entry.getValue());
        }

        // process all neighbors
        while (true) {
            ArrayList<Integer> l1 = new ArrayList<>();
            ArrayList<Integer> l2 = new ArrayList<>();

            // get all n neighbors
            computeHops(l1, revInTree, ID, 0, hopCnt);
            computeHops(l2, revNeighborInTree, ID, 0, hopCnt);

            if (l1.isEmpty() && l2.isEmpty()) break;

            // sort the lists
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
                            c1++; c2++;
                            break;
                        }
                    }

                    if (temp1 < temp2) {
                        // remove edge from neighbor tree and add in final tree
                        if (neighborInTree.containsKey(v1)) neighborInTree.remove(v1);
                        if (l2.contains(v1)) l2.remove(Integer.valueOf(v1));

                        newInTree.put(v1, inTree.get(v1));
                        
                        c1++;

                    } else if (temp1 > temp2) {
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

        inTree = newInTree; // update current intree to the new intree

        revInTree = new HashMap<>();
        buildRevInTree(revInTree, inTree); // built reversed intree 

        // check if intree changed, if yes send routing message
        if (!oldInTree.equals(newInTree)) {
            try {
                BufferedWriter wBufferedWriter = new BufferedWriter(new FileWriter(output, true));
                sendRouting(wBufferedWriter);
                wBufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return;
    }

    // main method to update neighbors if anyone stopped working
    private void processNeighbors() {
        // increment counter of all neighbors
        for (int i = 0; i < 10; i++) {
            if (neighbors[i] != -1) neighbors[i]++;

            // if hello message not received for 30 secs, node has stopped
            if (neighbors[i] >= 30) {
                // remove neighbors from lists
                neighbors[i] = -1;
                neighborIntreeList.set(i, null);

                // update intree by removing neighbor
                Iterator<Entry<Integer, Integer>> treeItr = inTree.entrySet().iterator();
                while (treeItr.hasNext()) {
                    Map.Entry<Integer, Integer> elem  = treeItr.next();
                    if ((int)elem.getKey() == i || (int)elem.getValue() == i) {
                        treeItr.remove();
                    }
                }

                // send the updated intree
                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(output, true));
                    sendRouting(writer);
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            };
        }
    }

    // main method which process received data message
    private void processData(String msg) {
        // process received message
        int src = Integer.parseInt(msg.substring(5, 6));

        // get src routing path
        int start, end;
        start = msg.indexOf("(");
        end = msg.indexOf(")");
        String route = msg.substring(start+1, end);

        // get remaining nodes
        start = msg.indexOf(") ");
        end = msg.indexOf("begin");
        String nodes = msg.substring(start+2, end);

        // msg string
        start = msg.indexOf("begin");
        String message = msg.substring(start);

        // process the data message
        // 1. If node part of src routing, then directly send the message
        // 2. Else process again by getting intrees and routing

        int nId = Integer.parseInt(route.substring(0, 1));
        if (nId == ID) {
            if (route.length() == 1) {

                if (nodes.length() == 0) {
                    // this is the intended dest. write message in received file
                    String receivedMessage = "message from "+src+": "+message.substring(6);

                    try {
                        BufferedWriter dataWriter = new BufferedWriter(new FileWriter(dataReceived, true));
                        
                        dataWriter.write(receivedMessage);
                        dataWriter.write(System.lineSeparator());
                        dataWriter.close(); 
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    return;
                } else {
                    // get new dest
                    int currDest = Integer.parseInt(nodes.substring(0, 1));
                    nodes = nodes.substring(2);

                    // check intree to see if dest exists
                    if (!inTree.containsKey(currDest)) return;

                    // if dest exists find first node in path
                    // also store this path in list
                    ArrayList<Integer> path = new ArrayList<>();
                    int node = inTree.get(currDest);
                    while (node != ID) {
                        path.add(node);
                        node = inTree.get(node);
                    }

                    // check if nodes intrees is obtained
                    HashMap<Integer, Integer> nodeInTree = null;
                    if (node == ID) {
                        node = currDest;
                        nodeInTree = neighborIntreeList.get(currDest);
                    } else if (node != ID) {
                        nodeInTree = neighborIntreeList.get(node);
                    }
                    if (nodeInTree == null) return;

                    // check if you exist in this node intree
                    if (!nodeInTree.containsKey(ID)) return;

                    // add src routing path
                    String newRoute = "";
                    newRoute += "(";
                    int node1 = nodeInTree.get(ID);
                    newRoute += node1;
                    while (node1 != node) {
                        node1 = nodeInTree.get(node1);
                        newRoute += " "+node1;
                    }
                    newRoute += ")";

                    // add remaining nodes
                    String newNodes = "";
                    while (!path.isEmpty()) {
                        newNodes += " " + path.remove(path.size()-1);
                    }

                    // send the updated data message
                    String finalMsg = "data "+src+" "+newRoute+newNodes+" "+nodes+message;
                    try {
                        BufferedWriter writer = new BufferedWriter(new FileWriter(output, true));
                        
                        writer.write(finalMsg);
                        writer.write(System.lineSeparator());
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            } else {
                // source routing, directly send the data
                route = route.substring(2);
                String newMsg = "data "+src+" ("+route+") "+nodes+message;

                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(output, true));
                    writer.write(newMsg);
                    writer.write(System.lineSeparator());
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }

        return;
    }

    // main method to read and process the node input file
    private long readInput(long lastLine) {
        try {
            if (lastLine < input.length()) {
                BufferedReader reader = new BufferedReader(new FileReader(input));
                reader.skip(lastLine);

                String line = null;
                while ((line = reader.readLine()) != null) {
                    String type = line.split(" ")[0];
                    switch(type) {
                        // hello message
                        case "hello":
                            processHello(line);
                            break;
                        // intree routing message
                        case "intree":
                            computeInTree(line);
                            break;
                        // main data message
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

    // main method which starts the node and runs for the given duration
    private void startNode() {

        Object execution = new Object();
        try {
            synchronized(execution) {
                long lastLine = 0; // keep track of the last line read from input file

                for (int time = 0; time < duration; time++) {
                    
                    BufferedWriter writer = new BufferedWriter(new FileWriter(output, true));
                    
                    if (time%5 == 0) sendHello(writer);
                    if (time%10 == 0) sendRouting(writer);
                    if (time%15 == 0) if (destID != -1) sendData(writer);
                    
                    lastLine = readInput(lastLine);

                    processNeighbors();
                    
                    writer.close();

                    System.out.println("Node "+ID+" running");
                    
                    execution.wait(1000);
                }

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