
//the list of imports
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import logist.LogistSettings;
import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.config.Parsers;
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
public class Auction implements AuctionBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;

	private StateActionTable stateActionTable;
	private long timeout_setup;
	private long timeout_plan;
	private long timeout_bid;
	private Solution currentSolution;
	private static final int AMOUNT_OF_SOLUTIONS_KEPT_IN_SLS = 10;

	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {

		// this code is used to get the timeouts
		LogistSettings ls = null;
		try {
			ls = Parsers.parseSettings("config" + File.separator + "settings_auction.xml");
		} catch (Exception exc) {
			System.out.println("There was a problem loading the configuration file.");
		}

		// the setup method cannot last more than timeout_setup milliseconds
		timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
		// the plan method cannot execute more than timeout_plan milliseconds
		timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
		// the bid method cannot execute more than timeout_bid milliseconds
		timeout_bid = ls.get(LogistSettings.TimeoutKey.BID);

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		// TODO : Compute the StateActionTable (Simon)
		this.stateActionTable = new StateActionTable();
		this.currentSolution = null;
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		// TODO : Make the agent handle the new task IF it won the auctions
	}

	@Override
	public Long askPrice(Task task) {

		// TODO : Only compute if at least one vehicle can handle the task that's
		// auctioned
		// if (vehicle.capacity() < task.weight)
		// return null;

		System.out.println("[START] : Bid for :  " + task);

		// 1. For EACH AGENT, find the best solutions according to centralized (Arthur)
		TaskSet tasksWithAuctionedTask = TaskSet.copyOf(agent.getTasks());
		tasksWithAuctionedTask.add(task);
		SLS sls = new SLS(agent.vehicles(), tasksWithAuctionedTask, this.timeout_bid,
				Auction.AMOUNT_OF_SOLUTIONS_KEPT_IN_SLS);
		Solution solution = sls.getSolutions().getFirstSolution();

		// 2. Use this.stateActionTable to discriminate the solutions of each agent,
		// selecting the best one (Simon)
		this.currentSolution = solution;

		// 3. Place a bid according to results (Simon)

		long bid = Long.MIN_VALUE;
		return bid;
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {

		// TODO use the results of askPrice to output the plans (Arthur)
		ArrayList<Plan> plans = new ArrayList<Plan>();
		return plans;
	}
}
