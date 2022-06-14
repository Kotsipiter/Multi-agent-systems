package agents;

import agents.Node.Client;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

public class MyAgent extends Agent {

	private static Client client = null;
	static String direction;
	public void setup() {

		// register agent to directory facilitator
		DFAgentDescription dfd = new DFAgentDescription();
		// agent id
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("agent");
		sd.setName(getLocalName());
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}

		// while(1)

		addBehaviour(new CyclicBehaviour(this) {
			public void action() {

				ACLMessage msg = null;
				// waiting to receive message
				msg = blockingReceive();

				if (msg.getContent().equals("Pick a Direction")) {
					String newdirection = pickDirection();
					ACLMessage message = new ACLMessage(ACLMessage.INFORM);
					// refer to receiver by local name
					message.addReceiver(new AID("Grid", AID.ISLOCALNAME));

					setDirection(newdirection);
					message.setContent(direction);
					send(message);
				}
				else if (msg.getContent().equals("End")) {
					System.out.println(getLocalName() + " terminating");
					// take down from registry
					takeDown();
					// terminate
					doDelete();
				}
			}

			private String pickDirection() {

				int[] agentCoordinates = Grid.search("A");

				if (client != null) { // if our agent already has a client picked up, then find closest path for them
					Node agentNode = Grid.myNodes[agentCoordinates[0]][agentCoordinates[1]];
					if(agentNode == client.target) {
						setPickedUp(null);
						String decision = "Client target and current node are the same staying here";
						return decision;
					}
					Node nextMove = Node.aStar(agentNode, client.target);
					if (nextMove.parent == null) {
						setPickedUp(null);
						String decision = decidePath(agentCoordinates, nextMove);
						System.out.println("Ofloading client");
						return decision;
					} else {
						String decision = decidePath(agentCoordinates, nextMove.parent);
						System.out.println("Taking client to destination");
						return decision;
					}
				} else { // if agent doesn't have a client, then find closer path leading to the closer client
					Node agentNode = Grid.myNodes[agentCoordinates[0]][agentCoordinates[1]];
					boolean empty = false;

					int len00 = 1000, len40 = 1000, len43 = 1000, len04 = 1000;
					Node closestNode = null; // which one of the r, g, b, y is closest to the agent

					// node of clients (r, g, b or y)
					Node node00, node40, node43, node04 = null;

					// find closest node that has clients
					node00 = Grid.myNodes[0][0];
					if (!node00.clients.isEmpty()) {
						node00 = Node.aStar(agentNode, node00);
						len00 = Node.printPath(node00);
					}
					node40 = Grid.myNodes[4][0];
					if (!node40.clients.isEmpty()) {
						node40 = Node.aStar(agentNode, node40);
						len40 = Node.printPath(node40);
					}
					node43 = Grid.myNodes[4][3];
					if (!node43.clients.isEmpty()) {
						node43 = Node.aStar(agentNode, node43);
						len43 = Node.printPath(node43);
					}
					node04 = Grid.myNodes[0][4];
					if (!node04.clients.isEmpty()) {
						node04 = Node.aStar(agentNode, node04);
						len04 = Node.printPath(node04);
					}

					if (len00 == 1000 && len40 == 1000 && len43 == 1000 && len04 == 1000) {
						// if there are no clients spawned on the grid, let the agent know that
						System.out.println("There are no clients.");
						empty = true;
					} // else, save the node with the smallest path in closestNode [type Node]
					else if (len00 <= len40 && len00 <=len43 && len00 <= len04) {
						closestNode = Grid.myNodes[0][0] ;
					} else if (len40 <= len00 && len40  <=len43 && len40 <= len04) {
						closestNode = Grid.myNodes[4][0];
					} else if (len43 <= len00 && len43 <= len40 && len43 <= len04) {
						closestNode = Grid.myNodes[4][3];
					} else if (len04 <= len00 && len04 <= len43 && len04 <= len40) {
						closestNode = Grid.myNodes[0][4];
					}

					// if there aren't any clients in the grid, just return empty
					if (empty) {
						Grid.tries = 100;
						return "";
					} else {
						// call a star to find closest path to chosen closestNode
						Node nextMove = Node.aStar(agentNode, closestNode);

						// return up, down, left or right
						if (nextMove.parent == null) {
							String decision = decidePath(agentCoordinates, nextMove);
							
							return decision;
						} else {
							String decision = decidePath(agentCoordinates, nextMove.parent);
							System.out.println("Going to destinations");
							return decision;
						}
					}
				}
			}

			private String decidePath(int[] agentCoordinates, Node parent) {

				if ((agentCoordinates[0] - parent.i) == 1) {
					return "up";
				} else if ((agentCoordinates[0] - parent.i) == -1) {
					return "down";
				} else if ((agentCoordinates[1] - parent.j) == 1) {
					return "left";
				} else if ((agentCoordinates[1] - parent.j) == -1) {
					return "right";
				} else {
					return "Agent has arrived at destination";
				}
			}
		});
	}

	public static Client isPickedUp() {
		return client;
	}

	public static void setPickedUp(Client client1) {
		client = client1;
	}

	public static String getDirection() {
		return direction;
	}

	public void setDirection(String direction1) {
		direction = direction1;
	}
}