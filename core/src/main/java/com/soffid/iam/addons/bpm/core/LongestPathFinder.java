package com.soffid.iam.addons.bpm.core;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.soffid.iam.addons.bpm.common.Node;
import com.soffid.iam.addons.bpm.common.NodeType;
import com.soffid.iam.addons.bpm.common.Transition;

public class LongestPathFinder {

	public static Node find(Node current, Set<Node> next, Collection<Node> nodesList) {
		Set<Node> visited = new HashSet<Node>();
		LinkedList<Trip> trips = new LinkedList<Trip>();
		for ( Node n: next)
		{
			Trip t = new Trip ();
			t.node = n;
			t.steps = 1;
			t.source = n;
			t.visited = new HashSet<Node>();
			t.visited.add(n);
			trips.add(t);
		}
		Trip trip = trips.getFirst();
		while (trips.size() > 1)
		{
			trips.removeFirst();
			for (Transition n: trip.node.getOutTransitions())
			{
				Node nextNode = n.getTarget();
				if ( nodesList.contains(nextNode)) // a node not painted yet
				{
					for ( Iterator<Trip> it = trips.iterator(); it.hasNext(); )
					{
						Trip trip2 = it.next();
						if (trip2.visited.contains(nextNode)) // Found a shortest path
						{
							it.remove();
						}
					}
					Trip trip2 = new Trip();
					trip2.node = nextNode;
					trip2.source = trip.source;
					trip2.steps = trip.steps + 1;
					trip2.visited = new HashSet<Node> (trip.visited);
					trip2.visited.add(nextNode);
					// Add order by trip steps
					boolean added = false;
					int i = 0;
					for (Trip trip3: trips)
					{
						if (trip3.steps > trip2.steps)
							break;
						i++;
					}
					trips.add(i, trip2);
				}
			}
			if (! trips.isEmpty())
				trip = trips.getFirst();
		}
		return trip.source;
	}

}

class Trip {
	Node node;
	int steps;
	Node source;
	Set<Node> visited;
}
