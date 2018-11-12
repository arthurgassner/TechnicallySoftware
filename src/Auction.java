

//the list of imports
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.agent.Agent;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 * 
 */
@SuppressWarnings("unused")
public class Auction implements AuctionBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private StateActionTable stateActionTable; // change class name

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		
		this.stateActionTable = new StateActionTable();
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		//TODO : Make the agent handle the new task IF it won the auctions
	}
	
	@Override
	public Long askPrice(Task task) {

		// TODO : Only compute if at least one vehicle can handle the task that's auctioned
	//	if (vehicle.capacity() < task.weight)
	//		return null;
		
		//1. Use centralized (modified to output 10 best) for EACH VEHICLE (Simon)
		
		//2. Use this.stateActionTable to discriminate the solutions of each vehicle (Arthur)
		
		//3. Place a bid according to results  (Simon)
		
		long bid = Long.MIN_VALUE;
		return bid;
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		
		// TODO use the results of askPrice to output the plans (Arthur)

		return plans;
	}
}
