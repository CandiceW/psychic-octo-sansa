package student;

import java.awt.Color;
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
	private HashSet<Parcel> unpicked = new HashSet<Parcel>();

	private class WorkQueue {
		private Color c;
		private LinkedList<Parcel> queue = new LinkedList<Parcel>();
		private int dist;
		private Parcel current;
		
		private WorkQueue(Color ci) {
			c=ci;
		}

		private int distanceDiffInsert(Node ns, Parcel pi, Node ne) {
			int od = getDistance(ns,ne);
			int d = getDistance(ns,pi.start) + getDistance(pi.destination,ne);
			return d-od; //change of interim distance, more negative change is more desirable
		}

		private void updateDist(int po, Parcel p) {
			Node ns = po-1<0?getBoard().getTruckDepot():queue.get(po-1).destination;
			Node ne = po+1==queue.size()?getBoard().getTruckDepot():queue.get(po+1).start;
			dist = dist - getDistance(ns,ne) + getDistance(ns,p.start) 
					+ getDistance(p.start,p.destination) + getDistance(p.destination,ne);
		}

		private Parcel poll() {
			Parcel p = queue.get(0);
			Node ns = getBoard().getTruckDepot();
			Node ne = queue.size()==1?getBoard().getTruckDepot():queue.get(1).start;
			dist = dist + getDistance(ns,ne) - getDistance(ns,p.start) 
					- getDistance(p.start,p.destination) - getDistance(p.destination,ne);
			queue.remove(p);
			current = p;
			return p;
		}

		private void addOptimal(Parcel p) {
			if (queue.size()==0) {
				queue.add(p); updateDist(0,p); return;
			}
			int opt =0; int i = 1; 
			int distDiff = distanceDiffInsert(getBoard().getTruckDepot(),p,queue.get(0).start);
			while (i<queue.size()) {
				int distDiffc = distanceDiffInsert(queue.get(i-1).destination,p,queue.get(i).start);
				if (distDiffc<distDiff){
					distDiff = distDiffc;
					opt = i;
				}
				i++;
			}
			int distDiffc = distanceDiffInsert(queue.get(i-1).destination,p,getBoard().getTruckDepot());
			if (distDiffc<distDiff){
				distDiff = distDiffc;
				opt = i;
			}
			queue.add(opt, p);
			updateDist(opt,p);
		}

		/**return the difference made in interim distance when inserting parcel p at optimal
		 * position of this workqueue
		 */
		private int addDistanceChange(Parcel p) {
			if (queue.size()==0) {
				return distanceDiffInsert(getBoard().getTruckDepot(),p,getBoard().getTruckDepot());
			}
			int opt =0; int i = 1; 
			int distDiff = distanceDiffInsert(getBoard().getTruckDepot(),p,queue.get(0).start);
			while (i<queue.size()) {
				int distDiffc = distanceDiffInsert(queue.get(i-1).destination,p,queue.get(i).start);
				if (distDiffc<distDiff){
					distDiff = distDiffc;
					opt = i;
				}
				i++;
			}
			int distDiffc = distanceDiffInsert(queue.get(i-1).destination,p,getBoard().getTruckDepot());
			if (distDiffc<distDiff){
				distDiff = distDiffc;
				opt = i;
			}
			return distDiff + getDistance(p.start,p.destination);
		}

	}



	@Override
	public void run() {
		unpicked.addAll(getParcels());
		for (Node n : getNodes()) {
			n.setUserData(new HashMap<Node,Integer>());
		}
		for (Truck t : getTrucks()) t.setUserData(new WorkQueue(t.getColor()));
		//		HashSet<Parcel> del = new HashSet<Parcel>();
		//		for (Parcel p : unpicked) {
		//			for (Truck t : getTrucks()) {
		//				if (p.getColor() == t.getColor()) {
		//					((WorkQueue)t.getUserData()).addOptimal(p);
		//					del.add(p); break;
		//				}
		//			}
		//		}
		//		unpicked.removeAll(del);
		//HashSet<Parcel> nonColor = new HashSet<Parcel>();
		MinHeap<Truck> h = new MinHeap<Truck>();
		for (Truck t : getTrucks()) h.add(t,((WorkQueue)t.getUserData()).dist);
		while (unpicked.size()!=0) {
			Truck t = h.peek();
			Parcel pc = null;
			for (Parcel p : unpicked) {
				pc = p;
				if (pc.getColor()==t.getColor()) {
					((WorkQueue)h.peek().getUserData()).addOptimal(pc);
					break;
				}
			}
			if (!((WorkQueue)h.peek().getUserData()).queue.contains(pc))
				((WorkQueue)h.peek().getUserData()).addOptimal(pc);
			h.updatePriority(h.peek(),((WorkQueue)h.peek().getUserData()).dist);
			unpicked.remove(pc);
			//System.out.println(unpicked.size());
		}
		//		for (Parcel p : nonColor) {
		//			((WorkQueue)h.peek().getUserData()).addOptimal(p);
		//			h.updatePriority(h.peek(),((WorkQueue)h.peek().getUserData()).dist);
		//		}
		//		for (Parcel p : unpicked) {
		//			((WorkQueue)h.peek().getUserData()).addOptimal(p);
		//			h.updatePriority(h.peek(),((WorkQueue)h.peek().getUserData()).dist);
		//		}
		System.out.println("Done!!");
		int sum = 0;
		for (Truck t : getTrucks()) {
			sum += ((WorkQueue)t.getUserData()).queue.size();
		}
		System.out.println(sum);
		for (Truck t : getTrucks()) {
			System.out.println(t);
			System.out.println(((WorkQueue) t.getUserData()).queue.size());
			assign(t);
			pickUpParcel(t);
		}
	}
	
	private void formQueue(ArrayList<WorkQueue> wql, HashSet<Parcel> ps){
		
	}
	
	private int optimalInsertionScore(ArrayList<WorkQueue> wql,Parcel p) {
		return 0;
	}
	
	private WorkQueue optimalInsertion(ArrayList<WorkQueue> wql,Parcel p) {
		return null;
	}

	private void assign(Truck t) {
		synchronized (t.getUserData()) {
			if (((WorkQueue) t.getUserData()).queue.isEmpty()) {
				t.setTravelPath(Paths.dijkstra(t.getLocation(), getBoard()
						.getTruckDepot()));
			} else {
				Parcel p = ((WorkQueue) t.getUserData()).poll();
				System.out.println(((WorkQueue) t.getUserData()).current.start+" "+
						((WorkQueue) t.getUserData()).current.destination);
				LinkedList<Node> path = Paths.dijkstra(t.getLocation(),
						p.getLocation());
				path.remove(path.size() - 1);
				path.addAll(Paths.dijkstra(p.getLocation(), p.destination));
				for (Node n : path) {
					System.out.print(n.name + " ");
				}
				System.out.println();
				t.setTravelPath(path);
			}
		}
	}

	private void pickUpParcel(Truck t) {
		HashSet<Parcel> ps = t.getLocation().getParcels();
		for (Parcel p : ps) {
			if (((WorkQueue) t.getUserData()).current == p)
				t.pickupLoad(p);
		}
	}

	//	private void assign(Truck t) {
	//		if (unpicked.size()==0) {
	//			System.out.println();
	//			for (Node n : Paths.dijkstra(t.getLocation(), this.getBoard().getTruckDepot())) {
	//				System.out.print(n.name+" ");
	//			}
	//			t.setTravelPath(Paths.dijkstra(t.getLocation(), this.getBoard().getTruckDepot()));
	//			return;
	//		}
	//		MinHeap<Parcel> h = new MinHeap<Parcel>();
	//		for (Parcel p : unpicked) {
	//			h.add(p, -getTruckTakeParcelScore(t,p));
	//		}
	//		double score = -h.peekPriority();
	//		Parcel p = h.poll();
	//		assignment.put(p, t);
	//		LinkedList<Node> path = Paths.dijkstra(t.getLocation(), p.getLocation());
	//		path.remove(path.size()-1);
	//		path.addAll(Paths.dijkstra(p.getLocation(), p.destination));
	//		System.out.println();
	//		for (Node n : path) {
	//			System.out.print(n.name+" ");
	//		}
	//		t.setTravelPath(path);
	//		unpicked.remove(p);
	//	}

	//	/**return the score of truck t taking parcel p from p's current location to its destination */
	//	private int getParcelToDestinationScore(Truck t, Parcel p) {
	//		int m = t.getColor()==p.getColor()?this.getBoard().getOnColorMultiplier():1;
	//		int c = getTravelCost(t,p.getLocation(),p.destination);
	//		if (p.isHeld()&&t.getLoad()!=p) 
	//			c +=this.getBoard().getDropoffCost()+this.getBoard().getPickupCost();
	//		else if (!p.isHeld()) c+= this.getBoard().getPickupCost();
	//		return this.getBoard().getPayoff()*m-c-this.getBoard().getDropoffCost();
	//	}

	/**return the shortest distance between Node start and Node end*/
	@SuppressWarnings("unchecked")
	private int getDistance(Node start, Node end){
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

	//
	//	/**return the cost of Truck t traveling from Node start and Node end*/
	//	public int getTravelCost(Truck t, Node start, Node end){
	//		int distance = getDistance(start, end);    
	//		return distance/t.getSpeed()*Score.cost(t.getSpeed());
	//	}
	//
	//	/**return the score when Truck t start
	//	 * from it's current node and pick up Parcel parcel and diliver it to destination*/
	//	public int getTruckTakeParcelScore(Truck t, Parcel p){
	//		int cost=getTravelCost(t,t.getLocation(),p.getLocation());
	//		return getParcelToDestinationScore(t,p)-cost;
	//	}

	@Override
	public void truckNotification(Truck t, Notification not) {
		synchronized (t.getUserData()) {
			if (not == Notification.LOCATION_CHANGED && t.getLoad() != null
					&& t.getLoad().destination == t.getLocation()) {
				t.dropoffLoad();
				System.out.println(t);
				System.out.println(((WorkQueue) t.getUserData()).queue.size());
				//				System.out.println(((WorkQueue) t.getUserData()).current.start+" "+
				//						((WorkQueue) t.getUserData()).current.destination);
				assign(t);
				pickUpParcel(t);
			}
			if (not == Notification.PARCEL_AT_NODE) {
				pickUpParcel(t);
			}
			//		if (not==Notification.WAITING&&t.getLocation()!=this.getBoard().getTruckDepot()){
			//			System.out.println(t);
			//			synchronized (this){
			//				assign(t);
			//			}
			//		}
		}
	}

}