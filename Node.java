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

    private static final String ipDirectory = "inputs/", opDirectory = "outputs/", dataDirectory = "data/", logDirectory = "logs/";
    
    private int ID, duration, destID, logCounter;
    private String message;
    
    File input, output, dataReceived, log;
    private int[] neighbors;

    private HashMap<Integer, Integer> inTree;
    private HashMap<Integer, ArrayList<Integer>> revInTree;

    private HashMap<Integer, Integer>[] neighborIntreeList;

    Node(int ID, int duration, int destID, String message) {
        this.ID = ID;
        this.duration = duration;
        this.destID = destID;
        this.message = message;
        logCounter = 0;

        neighbors = new int[10];
        for (int i = 0; i < 10; i++) neighbors[i] = -1;

        inTree = new HashMap<>();
        revInTree = new HashMap<>();

        neighborIntreeList = new HashMap[10];
        for (int i = 0; i < 10; i++) neighborIntreeList[i] = null;

        setupFiles();
    }

    // private String getName() {
    //     return "Node-"+ID+": ";
    // }

    private void setupFiles() {
        try {
            input = new File(ipDirectory+"input_"+ID+".txt");
            output = new File(opDirectory+"output_"+ID+".txt");
            dataReceived = new File(dataDirectory+ID+"_received"+".txt");
            log = new File(logDirectory+"log_"+ID+".txt");
            
            if (!input.exists()) input.createNewFile();
            if (!output.exists()) output.createNewFile();
            if (!dataReceived.exists()) dataReceived.createNewFile();
            if (!log.exists()) log.createNewFile();
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

    // Algo steps
    // 1. Check node intree to find a path from dest to node and reverse it
    // 2. As per the path, check the first node (say X) and find path to reach there
    // 3. Using X intree, get path from node to X (say P). This path doesn't change.
    // 4. Send message to first node in this path P
    private String computePath() {
        // writeToLog((logCounter++)+". Computing path for main src node");
        String dataMsg = "data "+ID+" ";

        // check intree to see if dest exists
        // writeToLog((logCounter++)+". Intree check");
        if (!inTree.containsKey(destID)) return null;
        // writeToLog((logCounter++)+". Intree contains destination: "+inTree.toString());

        // if dest exists find first node in path
        // also store this path in list
        ArrayList<Integer> path = new ArrayList<>();
        int node = destID;
        while (node != ID) {
            path.add(node);
            node = inTree.get(node);
        }
        // writeToLog((logCounter++)+". Path: "+path.toString());

        // check if nodes intrees is obtained
        int firstNeigh = path.get(path.size()-1);
        // writeToLog((logCounter++)+". Neighbor Intree check: ");
        if (neighborIntreeList[firstNeigh] == null) return null;
        HashMap<Integer, Integer> nodeInTree = neighborIntreeList[firstNeigh];
        // writeToLog((logCounter++)+". Neighbor Intree: "+nodeInTree.toString());

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
        // writeToLog((logCounter++)+". Src routing path: "+dataMsg);

        // add path to data message
        path.remove(path.size()-1);
        while (!path.isEmpty()) {
            dataMsg += " " + path.remove(path.size()-1);
        }
        dataMsg += " begin "+message;
        // writeToLog((logCounter++)+". Final data msg: "+dataMsg);

        return dataMsg;
    }

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

    // private void writeToLog(String data) {
    //     try {
    //         BufferedWriter logWriter = new BufferedWriter(new FileWriter(log, true));
    //         logWriter.write(data);
    //         logWriter.write(System.lineSeparator());
    //         logWriter.close();
    //     } catch (IOException e) {
    //         e.printStackTrace();
    //     }

    // }

    private void computeInTree(String msg) {
        // build neighbor intree
        // msg format -> intree 4 (1-4) (3-4) (5-3) (2-1)
        HashMap<Integer, Integer> neighborInTree = new HashMap<>();
        HashMap<Integer, ArrayList<Integer>> revNeighborInTree = new HashMap<>();

        // writeToLog((logCounter++)+". Msg received: "+msg);
        String msg1 = msg.replace("(", "").replace(")", "").replace("intree ", "");

        // if neighbor intree is the node itself, then add the edge
        if (msg1.length() == 1) {
            // writeToLog((logCounter++)+"."+" if for no intree called");
            int neigh = Integer.parseInt(msg1);
            inTree.put(neigh, ID);
            revInTree = new HashMap<>();
            buildRevInTree(revInTree, inTree);
            // writeToLog((logCounter++)+". Intree for this->"+inTree.toString());
            return;
        }
        
        int neighbor = Integer.parseInt(msg1.split(" ")[0]);
        
        String[] data = msg1.substring(2).split(" ");

        buildInTree(neighborInTree, data);
        // writeToLog((logCounter++)+". Neighbor intree before modification->"+neighborInTree.toString());

        // add this neighbor intree to our neighbor intrees list
        HashMap<Integer, Integer> neighTree = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : neighborInTree.entrySet()) {
            neighTree.put(entry.getKey(), entry.getValue());
        }
        neighborIntreeList[neighbor] = neighTree;

        // modify neighbor in-tree
        if (neighborInTree.containsKey(ID)) neighborInTree.remove(ID);
        neighborInTree.put(neighbor, ID);

        // writeToLog((logCounter++)+". Neighbor intree after modification->"+neighborInTree.toString());

        // build reverse intree by reversing the edges
        buildRevInTree(revNeighborInTree, neighborInTree);
        // writeToLog((logCounter++)+". Reversed neighbor intree->"+revNeighborInTree.toString());

        int hopCnt = 1;

        HashMap<Integer, Integer> newInTree = new HashMap<>();

        // writeToLog((logCounter++)+". Current Intree->"+inTree.toString());
        // writeToLog((logCounter++)+". Current reversed intree->"+revInTree.toString());

        // process all neighbors
        // writeToLog((logCounter++)+". Start while");
        while (true) {
            // writeToLog((logCounter++)+". For hop count: "+hopCnt);
            ArrayList<Integer> l1 = new ArrayList<>();
            ArrayList<Integer> l2 = new ArrayList<>();

            // get all n neighbors
            computeHops(l1, revInTree, ID, 0, hopCnt);
            computeHops(l2, revNeighborInTree, ID, 0, hopCnt);

            // writeToLog((logCounter++)+". Lists: l1->"+l1.toString()+", l2->"+l2.toString());

            if (l1.isEmpty() && l2.isEmpty()) break;

            Collections.sort(l1);
            Collections.sort(l2);

            // writeToLog((logCounter++)+". Lists sorted: l1->"+l1.toString()+", l2->"+l2.toString());

            int c1 = 0, c2 = 0;
            
            while (c1 < l1.size() && c2 < l2.size()) {

                int v1 = l1.get(c1), v2 = l2.get(c2);
                // writeToLog((logCounter++)+". Values: v1="+v1+", v2="+v2);

                if (v1 < v2) {
                    // remove edge from neighbor tree and add in final tree
                    // writeToLog((logCounter++)+". First if");
                    neighborInTree.remove(v1);
                    if (l2.contains(v1)) l2.remove(Integer.valueOf(v1));
                    // writeToLog((logCounter++)+". Modified neighbor tree->"+neighborInTree.toString());
                    // writeToLog((logCounter++)+". Modified l2->"+l2.toString());
                    
                    newInTree.put(v1, inTree.get(v1));
                    // writeToLog((logCounter++)+". Current new intree->"+newInTree.toString());
                    
                    c1++;

                } else if (v1 > v2) {
                    // remove edge from intree and add in final tree
                    // writeToLog((logCounter++)+". Second if");
                    inTree.remove(v2);
                    if (l1.contains(v2)) l1.remove(Integer.valueOf(v2));
                    // writeToLog((logCounter++)+". Modified in tree->"+inTree.toString());
                    // writeToLog((logCounter++)+". Modified l1->"+l2.toString());
                    
                    newInTree.put(v2, neighborInTree.get(v2));
                    // writeToLog((logCounter++)+". Current new intree->"+newInTree.toString());
                    
                    c2++;

                } else {
                    int temp1 = v1, temp2 = v2;
                    // writeToLog((logCounter++)+". Else");
                    // writeToLog((logCounter++)+". Temp values at start: temp1-"+temp1+", temp2-"+temp2);
                    // if both node values same, break tie by checking parents
                    while (temp1 == temp2) {
                        if (inTree.containsKey(temp1)) temp1 = inTree.get(temp1);
                        if (neighborInTree.containsKey(temp2)) temp2 = neighborInTree.get(temp2);
                        // writeToLog((logCounter++)+". Temp values: temp1-"+temp1+", temp2-"+temp2);

                        // if reached root, just add the edge from intree
                        if (temp1 == ID && temp2 == ID) {
                            // writeToLog((logCounter++)+". Reached root");
                            newInTree.put(v1, inTree.get(v1));
                            // writeToLog((logCounter++)+". Modified in tree->"+inTree.toString());
                            // writeToLog((logCounter++)+". Current new intree->"+newInTree.toString());
                            c1++; c2++;
                            break;
                        }
                    }

                    if (temp1 < temp2) {
                        // remove edge from neighbor tree and add in final tree
                        // writeToLog((logCounter++)+". temp1<temp2");
                        if (neighborInTree.containsKey(v1)) neighborInTree.remove(v1);
                        if (l2.contains(v1)) l2.remove(Integer.valueOf(v1));
                        // writeToLog((logCounter++)+". Modified neighbor tree->"+neighborInTree.toString());
                        // writeToLog((logCounter++)+". Modified l2->"+l2.toString());

                        newInTree.put(v1, inTree.get(v1));
                        // writeToLog((logCounter++)+". Current new intree->"+newInTree.toString());
                        
                        c1++;

                    } else if (temp1 > temp2) {
                        // remove edge from intree and add in final tree
                        // writeToLog((logCounter++)+". temp1>temp2");
                        if (inTree.containsKey(v2)) inTree.remove(v2);
                        if (l1.contains(v2)) l1.remove(Integer.valueOf(v2));
                        // writeToLog((logCounter++)+". Modified in tree->"+inTree.toString());
                        // writeToLog((logCounter++)+". Modified l1->"+l2.toString());
                        
                        newInTree.put(v2, neighborInTree.get(v2));
                        // writeToLog((logCounter++)+". Current new intree->"+newInTree.toString());

                        c2++;
                    }
                }

            }

            // if all n hop nodes are processed of neighbor tree
            while (c1 < l1.size()) {
                // writeToLog((logCounter++)+". All hops processed of neighbor tree");
                int v1 = l1.get(c1);
                // writeToLog((logCounter++)+". Values: v1="+v1);

                if (neighborInTree.containsKey(v1)) neighborInTree.remove(v1);
                if (l2.contains(v1)) l2.remove(Integer.valueOf(v1));
                // writeToLog((logCounter++)+". Modified neighbor tree->"+neighborInTree.toString());
                // writeToLog((logCounter++)+". Modified l2->"+l2.toString());

                newInTree.put(v1, inTree.get(v1));
                // writeToLog((logCounter++)+". Current new intree->"+newInTree.toString());

                c1++;
            }

            // if all n hop nodes are processed of intree
            while (c2 < l2.size()) {
                // writeToLog((logCounter++)+". All hops processed of neighbor tree");
                int v2 = l2.get(c2);
                // writeToLog((logCounter++)+". Values: v2="+v2);

                if (inTree.containsKey(v2)) inTree.remove(v2);
                if (l1.contains(v2)) l1.remove(Integer.valueOf(v2));
                // writeToLog((logCounter++)+". Modified in tree->"+inTree.toString());
                // writeToLog((logCounter++)+". Modified l1->"+l2.toString());

                newInTree.put(v2, neighborInTree.get(v2));
                // writeToLog((logCounter++)+". Current new intree->"+newInTree.toString());

                c2++;
            }

            // recompute the reversed intree to compute hops
            revInTree = new HashMap<>();
            revNeighborInTree = new HashMap<>();
            // writeToLog((logCounter++)+". Recomputed reversed intrees");

            buildRevInTree(revInTree, inTree);
            buildRevInTree(revNeighborInTree, neighborInTree);
            // writeToLog((logCounter++)+". Reversed intree->"+revInTree.toString());
            // writeToLog((logCounter++)+". Reversed neighbor intree->"+revNeighborInTree.toString());

            hopCnt++;
        }
        // writeToLog((logCounter++)+". End while");

        inTree = newInTree;

        revInTree = new HashMap<>();
        buildRevInTree(revInTree, inTree);

        // writeToLog((logCounter++)+". Final InTree->"+inTree.toString());
        // writeToLog((logCounter++)+". Final Reversed Intree->"+revInTree.toString());
        // writeToLog("");

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
        // process received message
        // writeToLog((logCounter++)+". Received msg: "+msg);
        int src = Integer.parseInt(msg.substring(5, 6));
        // get src routing path
        int start, end;
        start = msg.indexOf("(");
        end = msg.indexOf(")");
        String route = msg.substring(start+1, end);
        // writeToLog((logCounter++)+". Routing part: "+route);
        // get remaining nodes
        start = msg.indexOf(") ");
        end = msg.indexOf("begin");
        String nodes = msg.substring(start+2, end);
        // writeToLog((logCounter++)+". Nodes part: "+nodes);
        // msg string
        start = msg.indexOf("begin");
        String message = msg.substring(start);
        // writeToLog((logCounter++)+". Message part: "+message);
        

        // write data message to file
        try {
            BufferedWriter dataWriter = new BufferedWriter(new FileWriter(dataReceived, true));
            String receivedMsg = "message from " + src + ": " + msg;
            dataWriter.write(receivedMsg);
            dataWriter.write(System.lineSeparator());
            dataWriter.close(); 
        } catch (IOException e) {
            e.printStackTrace();
        }

        // process the data message
        // 1. If node part of src routing, then directly send the message
        // 2. Else process again by getting intrees and routing
        // writeToLog((logCounter++)+". Intermediate node computation");
        int nId = Integer.parseInt(route.substring(0, 1));
        if (nId == ID) {
            if (route.length() == 1) {
                if (nodes.length() == 0) {
                    // this is the intended dest
                    // writeToLog((logCounter++)+". Reached destination");
                    return;
                } else {
                    destID = Integer.parseInt(nodes.substring(0, 1));
                    nodes = nodes.substring(2);

                    // check intree to see if dest exists
                    // writeToLog((logCounter++)+". Intree check");
                    if (!inTree.containsKey(destID)) return;
                    // writeToLog((logCounter++)+". Intree contains destination: "+inTree.toString());

                    // if dest exists find first node in path
                    // also store this path in list
                    ArrayList<Integer> path = new ArrayList<>();
                    int node = inTree.get(destID);
                    while (node != ID) {
                        path.add(node);
                        node = inTree.get(node);
                    }
                    // writeToLog((logCounter++)+". Path: "+path.toString());
                    // writeToLog((logCounter++)+". Node: "+node);
                    // check if nodes intrees is obtained
                    HashMap<Integer, Integer> nodeInTree = null;
                    if (node == ID) {
                        // writeToLog((logCounter++)+". First if for destID: "+destID);
                        node = destID;
                        nodeInTree = neighborIntreeList[destID];
                        // writeToLog((logCounter++)+". Neighbor intree list: "+neighborIntreeList.toString());
                    } else if (node != ID) {
                        // writeToLog((logCounter++)+". second if for node: "+node);
                        nodeInTree = neighborIntreeList[node];
                    }
                    if (nodeInTree == null) return;

                    // check if you exist in this node intree
                    // writeToLog((logCounter++)+". Neighbor Intree check: ");
                    if (!nodeInTree.containsKey(ID)) return;
                    // writeToLog((logCounter++)+". Neighbor Intree: "+nodeInTree.toString());

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
                    // writeToLog((logCounter++)+". Src routing path: "+newRoute);

                    // add remaining nodes
                    String newNodes = "";
                    while (!path.isEmpty()) {
                        newNodes += " " + path.remove(path.size()-1);
                    }

                    String finalMsg = "data "+src+" "+newRoute+newNodes+" "+nodes+message;
                    // writeToLog((logCounter++)+". Final data msg: "+finalMsg);
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
                // writeToLog((logCounter++)+". Src route, direct send data: ");
                route = route.substring(2);
                String newMsg = "data "+src+" ("+route+") "+nodes+message;
                // writeToLog((logCounter++)+". New modified msg: "+newMsg);
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
                    
                    if (time%5 == 0) sendHello(writer);
                    if (time%10 == 0) sendRouting(writer);
                    if (time%15 == 0) if (destID != -1) sendData(writer);
                    
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

        // node.inTree.put(0, 4);
        // node.inTree.put(1, 2);
        // node.inTree.put(2, 3);
        // node.inTree.put(4, 3);

        // // HashMap<Integer, Integer> neighMap = new HashMap<>();
        // // neighMap.put(null, null)
        // node.processData("data 2 (3) 4 begin message_from_2_to_4");

    }

}