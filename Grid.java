package agents;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import agents.Node.Client;

public class Grid extends Agent {

	public static int tries=0;
	private int maxTries=100;
	private String[] agentArray;
	private int numberOfAgents;
	private static int x = 5;
	private static int y = 5;
	private static String[][] locations = new String[5][5];
	static Node[][] myNodes = new Node[5][5]; // ALL GRID NODES WITH NEIGHBOURS
	int id = 0; 

	public void setup() {

		try {Thread.sleep(50);} catch (InterruptedException ie) {} // important
		// search the registry for agents
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("agent");
		template.addServices(sd);
		try {
			DFAgentDescription[] result = DFService.search(this, template);
			numberOfAgents = result.length;
			agentArray = new String[numberOfAgents];
			for (int i = 0; i < numberOfAgents; ++i) {
				agentArray[i] = result[i].getName().getLocalName();
			}
		}
		catch (FIPAException fe) {fe.printStackTrace();}
		Arrays.sort(agentArray);
		System.out.println("Found:");
		for (int i = 0; i < numberOfAgents; ++i) System.out.println(agentArray[i]);
		findStartingLocations();

		addBehaviour(new CyclicBehaviour(this) {
			public void action() {

				try {Thread.sleep(50);} catch (InterruptedException ie) {}
				printGrid();
				if(tries<maxTries) {
					for (String agentName: agentArray) { 
						// WE HAVE ONE AGENT
						ACLMessage message = new ACLMessage(ACLMessage.INFORM);
						//refer to receiver by local name
						message.addReceiver(new AID(agentName, AID.ISLOCALNAME));
						message.setContent("Pick a Direction");
						send(message);
					}
					for (int i = 0; i < numberOfAgents; i++) {
						// WE HAVE ONE AGENT
						ACLMessage msg = null;

						msg = blockingReceive();
						String senderName = msg.getSender().getLocalName();
						if (msg.getContent().equals("Client target and current node are the same staying here")) {
							System.out.println(msg.getContent());
						} else {
							System.out.println(senderName + " moving towards: " + msg.getContent());
						}
						
						int id = Integer.parseInt(senderName.
								replaceAll("[\\D]", "")); // AGENT-37 -> 37
						makeMove( msg.getContent());
					}
					tries++;
				}
				else {
					try {Thread.sleep(50);} catch (InterruptedException ie) {}
					for (String agentName: agentArray) {
						ACLMessage messageFinal = new ACLMessage(ACLMessage.INFORM);
						messageFinal.addReceiver(new AID(agentName, AID.ISLOCALNAME));
						messageFinal.setContent("End");
						send(messageFinal);
					}
					System.out.println(getLocalName()+" terminating");
					// terminate
					doDelete();
				}

			}
		});
	}

	private void findStartingLocations() {

		for (int i=0; i<x; i++) {
			for (int j=0; j<y; j++) {
				locations[i][j] = "X";
			}
		}

		locations[0][0] = "0"; //R
		locations[4][0] = "0"; //Y
		locations[4][3] = "0"; //B
		locations[0][4] = "0"; //G

		// SPAWN AGENT
		// nextInt is normally exclusive of the top value, so add 1 to make it inclusive
		int x1 = ThreadLocalRandom.current().nextInt(0, 4 + 1);
		int y1 = ThreadLocalRandom.current().nextInt(0, 4 + 1);
		
		while ((x1 == 0 && y1 == 0) || (x1 == 0 && y1 == 4) 
				|| (x1 == 4 && y1 == 0) || (x1 == 4 && y1 == 3)) {
			x1 = ThreadLocalRandom.current().nextInt(0, 4 + 1);
			y1 = ThreadLocalRandom.current().nextInt(0, 4 + 1);
		}
		
		locations[x1][y1] = "A";

		// CREATE OUR GRID NODES
		for (int i=0; i<x; i++) {
			for (int j=0; j<y; j++) {
				myNodes[i][j] = new Node(i, j);
			}
		}

		// CREATE NEIGHBOURS FOR OUR NODES
		for (int i=0; i<x; i++) {
			for (int j=0; j<x; j++) {
				if (i-1>=0) {
					myNodes[i][j].addNeighbor(1, myNodes[i-1][j]);
				}
				if (i+1<=4) {
					myNodes[i][j].addNeighbor(1, myNodes[i+1][j]);
				}
				if (j-1>=0) {
					myNodes[i][j].addNeighbor(1, myNodes[i][j-1]);
				}
				if (j+1<=4) {
					myNodes[i][j].addNeighbor(1, myNodes[i][j+1]);
				}
			}
		}

		// REMOVE NEIGHBOURS THAT HAVE VALUE 1 INSTEAD OF 100 (conflict)
		myNodes[0][1].removeNeighbor(1, myNodes[0][2]);
		myNodes[1][1].removeNeighbor(1, myNodes[1][2]);
		myNodes[0][2].removeNeighbor(1, myNodes[0][1]);
		myNodes[1][2].removeNeighbor(1, myNodes[1][1]);

		myNodes[3][0].removeNeighbor(1, myNodes[3][1]);
		myNodes[4][0].removeNeighbor(1, myNodes[4][1]);
		myNodes[3][1].removeNeighbor(1, myNodes[3][0]);
		myNodes[4][1].removeNeighbor(1, myNodes[4][0]);

		myNodes[3][2].removeNeighbor(1, myNodes[3][3]);
		myNodes[4][2].removeNeighbor(1, myNodes[4][3]);
		myNodes[3][3].removeNeighbor(1, myNodes[3][2]);
		myNodes[4][3].removeNeighbor(1, myNodes[4][2]);

		// FIND DEAD ENDS (-100 value)
		myNodes[0][1].addNeighbor(100, myNodes[0][2]);
		myNodes[1][1].addNeighbor(100, myNodes[1][2]);
		myNodes[0][2].addNeighbor(100, myNodes[0][1]);
		myNodes[1][2].addNeighbor(100, myNodes[1][1]);

		myNodes[3][0].addNeighbor(100, myNodes[3][1]);
		myNodes[4][0].addNeighbor(100, myNodes[4][1]);
		myNodes[3][1].addNeighbor(100, myNodes[3][0]);
		myNodes[4][1].addNeighbor(100, myNodes[4][0]);

		myNodes[3][2].addNeighbor(100, myNodes[3][3]);
		myNodes[4][2].addNeighbor(100, myNodes[4][3]);
		myNodes[3][3].addNeighbor(100, myNodes[3][2]);
		myNodes[4][3].addNeighbor(100, myNodes[4][2]);

		// SPAWN CLIENT
		for(int i=0;i<10;i++) {
			spawnClient();
		}
	}

	private void spawnClient() {

		int r = ThreadLocalRandom.current().nextInt(0, 3 + 1);

		if(r == 0) {
			locations[0][0] = Integer.toString(Integer.parseInt(locations[0][0]) + 1); //R
			//the first parameter of addClient is a client id [type: String], 
			//the second parameter is the client's target [type: Node]
			myNodes[0][0].addClient("Client-" + id++, chooseClientTarget()); 
		} else if(r == 1) {
			locations[4][0] = Integer.toString(Integer.parseInt(locations[4][0]) + 1); //Y
			myNodes[4][0].addClient("Client-" + id++, chooseClientTarget());
		} else if(r == 2) {
			locations[4][3] = Integer.toString(Integer.parseInt(locations[4][3]) + 1); //B
			myNodes[4][3].addClient("Client-" + id++, chooseClientTarget());
		} else if(r == 3) {
			locations[0][4] = Integer.toString(Integer.parseInt(locations[0][4]) + 1); //G
			myNodes[0][4].addClient("Client-" + id++, chooseClientTarget());
		}

	}

	private Node chooseClientTarget() {

		int t = ThreadLocalRandom.current().nextInt(0, 3 + 1);
		int x = 0, y = 0;

		if (t == 0) {
			x = 0;
			y = 0;
		} else if(t == 1) {
			x = 4;
			y = 0;
		} else if(t == 2) {
			x = 4;
			y = 3;
		} else if(t == 3) {
			x = 0;
			y = 4;
		}

		Node target = myNodes[x][y];

		return target;
	}

	private void printGrid() {
		for (int i=0; i<x; i++) {
			for (int j=0; j<y; j++) {
				System.out.print("\t" + locations[i][j]);
			}
			System.out.println();
		}
	}

	private void makeMove(String content) { 

		// search on the grid where's agent A
		int[] agentCoordinates = search("A");

		// agent is going to move, so we have to change his grid position (no longer there)
		if((agentCoordinates[0] == 0 && agentCoordinates[1] == 0) || (agentCoordinates[0] == 0 && agentCoordinates[1] == 4) 
				|| (agentCoordinates[0] == 4 && agentCoordinates[1] == 0) || (agentCoordinates[0] == 4 && agentCoordinates[1] == 3)) {
			locations[agentCoordinates[0]][agentCoordinates[1]] = locations[agentCoordinates[0]][agentCoordinates[1]].replace("A", "");
		} else {
			locations[agentCoordinates[0]][agentCoordinates[1]] = "X";
		}


		switch (content) {
		case "up":
			agentCoordinates[0] = agentCoordinates[0] - 1;
			changeCoordinates(agentCoordinates);

			System.out.println("The agent went UP");
			break;

		case "down":
			agentCoordinates[0] = agentCoordinates[0] + 1;
			changeCoordinates(agentCoordinates);

			System.out.println("The agent went DOWN");
			break;

		case "left":
			agentCoordinates[1] = agentCoordinates[1] - 1;
			changeCoordinates(agentCoordinates);

			System.out.println("The agent went LEFT");
			break;

		case "right": 
			agentCoordinates[1] = agentCoordinates[1] + 1;
			changeCoordinates(agentCoordinates);

			System.out.println("The agent went RIGHT");
			break;
			
		case "Client target and current node are the same staying here":
			locations[agentCoordinates[0]][agentCoordinates[1]] = "A" + locations[agentCoordinates[0]][agentCoordinates[1]];
			break;
			
		default: // in case there aren't any clients on the grid
			if(MyAgent.isPickedUp()==null) {
				changeCoordinates(agentCoordinates);
			}
			System.out.println("The agent stayed in place.");
			break;
		}
	}

	public static int[] search(String AgentName) {

		int coordinates[] = new int[2];
		int k = 0;

		for (int i=0; i<x; i++) {
			for (int j=0; j<y; j++) {
				if (locations[i][j].contains(AgentName)) {
					coordinates[0] = i;
					coordinates[1] = j;
					k=1;
					break;
				}
			}
			if (k==1) {
				break;
			}	
		}

		return coordinates;
	}

	private void changeCoordinates(int[] coordinates) {
		if (locations[coordinates[0]][coordinates[1]].contentEquals("X")) {
			locations[coordinates[0]][coordinates[1]] = "A";
		} else { // if agent steps into r, g, b, y
			if (Integer.parseInt(locations[coordinates[0]][coordinates[1]]) > 0) {
				// reduce number of customers on block because agent picked them up
				locations[coordinates[0]][coordinates[1]] = Integer.toString((Integer.parseInt(locations[coordinates[0]][coordinates[1]]) - 1));
				locations[coordinates[0]][coordinates[1]] = "A" + locations[coordinates[0]][coordinates[1]];

				// calls a star in order to find the quickest client, and get their name and target
				if (MyAgent.isPickedUp() == null) {
					Client chosenClient = chooseClientWithClosestDestination(coordinates);
					myNodes[coordinates[0]][coordinates[1]].removeClient(chosenClient.name, chosenClient.target); 
					MyAgent.setPickedUp(chosenClient);
				}
			} else if (Integer.parseInt(locations[coordinates[0]][coordinates[1]]) == 0) {
				if (!myNodes[coordinates[0]][coordinates[1]].clients.isEmpty()) {
					MyAgent.setPickedUp(myNodes[coordinates[0]][coordinates[1]].clients.get(0));
					myNodes[coordinates[0]][coordinates[1]].removeClient(myNodes[coordinates[0]][coordinates[1]].clients.get(0).name, myNodes[coordinates[0]][coordinates[1]].clients.get(0).target);
				}
				locations[coordinates[0]][coordinates[1]] = "A" + locations[coordinates[0]][coordinates[1]];
			}
		}
	}

	private Client chooseClientWithClosestDestination(int[] coordinates) {

		Node client1;
		Client chosenClient = null; 
		int min = 10000;

		for (Node.Client client : myNodes[coordinates[0]][coordinates[1]].clients) {
			client1 = Node.aStar(myNodes[coordinates[0]][coordinates[1]], client.target);
			if (min > Node.printPath(client1)) {
				min = Node.printPath(client1);
				chosenClient = client;
			}
		}

		return chosenClient;

	}

	public String[][] getLocations() {
		return locations;
	}

	public void setLocations(String[][] locations1) {
		locations = locations1;
	}

	public static Node[][] getMyNodes() {
		return myNodes;
	}

	public static void setMyNodes(Node[][] myNodes1) {
		myNodes = myNodes1;
	}
}