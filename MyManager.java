package student;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import game.Parcel;
import game.Board;
import game.Manager;
import game.Node;
import game.Score;
import game.Truck;

public class MyManager extends Manager {
	private HashMap<Parcel, Truck> assignment = new HashMap<Parcel,Truck>();
	private HashSet<Parcel> unpicked = new HashSet<Parcel>();

	@Override
	public void run() {
		unpicked.addAll(this.getParcels());
		for (Node n : this.getNodes()) {
			n.setUserData(new HashMap<Node,Integer>());
		}
		for (Truck t : this.getTrucks()) {
			synchronized (this){
				assign(t);
			}
		}
	}

	private void assign(Truck t) {
		if (unpicked.size()==0) {
			System.out.println();
			for (Node n : Paths.dijkstra(t.getLocation(), this.getBoard().getTruckDepot())) {
				System.out.print(n.name+" ");
			}
			t.setTravelPath(Paths.dijkstra(t.getLocation(), this.getBoard().getTruckDepot()));
			return;
		}
		MinHeap<Parcel> h = new MinHeap<Parcel>();
		for (Parcel p : unpicked) {
			h.add(p, -getTruckTakeParcelScore(t,p));
		}
		double score = -h.peekPriority();
		Parcel p = h.poll();
		assignment.put(p, t);
		LinkedList<Node> path = Paths.dijkstra(t.getLocation(), p.getLocation());
		path.remove(path.size()-1);
		path.addAll(Paths.dijkstra(p.getLocation(), p.destination));
			System.out.println();
			for (Node n : path) {
				System.out.print(n.name+" ");
			}
		t.setTravelPath(path);
		unpicked.remove(p);
	}

	/**return the score of truck t taking parcel p from p's current location to its destination */
	private int getParcelToDestinationScore(Truck t, Parcel p) {
		int m = t.getColor()==p.getColor()?this.getBoard().getOnColorMultiplier():1;
		int c = getTravelCost(t,p.getLocation(),p.destination);
		if (p.isHeld()&&t.getLoad()!=p) 
			c +=this.getBoard().getDropoffCost()+this.getBoard().getPickupCost();
		else if (!p.isHeld()) c+= this.getBoard().getPickupCost();
		return this.getBoard().getPayoff()*m-c-this.getBoard().getDropoffCost();
	}

	/**return the shortest distance between Node start and Node end*/
	@SuppressWarnings("unchecked")
	public int getDistance(Node start, Node end){
		HashMap<Node,Integer> startmap=(HashMap<Node, Integer>)start.getUserData();
		HashMap<Node,Integer> endmap  =((HashMap<Node, Integer>)end.getUserData());
		if (startmap.containsKey(end)){
			return (int) startmap.get(end);
		} else if (endmap.containsKey(start)){
			return (int)endmap.get(start);
		} else {
			startmap.putAll(Paths.dijkstra(start));
			return (int) startmap.get(end);
		}
	}


	/**return the cost of Truck t traveling from Node start and Node end*/
	public int getTravelCost(Truck t, Node start, Node end){
		int distance = getDistance(start, end);    
		return distance/t.getSpeed()*Score.cost(t.getSpeed());
	}

	/**return the score when Truck t start
	 * from it's current node and pick up Parcel parcel and diliver it to destination*/
	public int getTruckTakeParcelScore(Truck t, Parcel p){
		int cost=getTravelCost(t,t.getLocation(),p.getLocation());
		return getParcelToDestinationScore(t,p)-cost;
	}

	@Override
	public void truckNotification(Truck t, Notification not) {
		if (not==Notification.LOCATION_CHANGED&&t.getLoad()!=null&&
				t.getLoad().destination==t.getLocation()) {
			assignment.remove(t.getLoad());
			t.dropoffLoad();
			synchronized (this){
				assign(t);
			}
		}
		if (not==Notification.PARCEL_AT_NODE) {
			synchronized (this) {
				HashSet<Parcel> ps = t.getLocation().getParcels();
				for (Parcel p : ps) {
					if (assignment.get(p)==t) t.pickupLoad(p);
				}
			}
		}
//		if (not==Notification.WAITING&&t.getLocation()!=this.getBoard().getTruckDepot()){
//			System.out.println(t);
//			synchronized (this){
//				assign(t);
//			}
//		}
	}

}