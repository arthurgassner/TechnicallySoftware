
//the list of imports
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import logist.LogistSettings;
import logist.behavior.AuctionBehavior;
import logist.config.Parsers;
import logist.agent.Agent;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 * 
 */
public class Auction implements AuctionBehavior {

	private Agent agent;
	private Agent adversary;
	
	private BidRecord bidRecord;

	private StateActionTable stateActionTable;
	private long timeout_setup;
	private long timeout_plan;
	private long timeout_bid;

	private long predictedAdversaryCostRaw;
	private long predictedAdversaryCostModified;
	private long amountWonByAdversary = 0;
	private long adversaryBidDiscrepancy; 
	//private long numberOfAdversaryTasks = 0;
	
	private long amountWonByFriendly = 0;
	//private long numberOfFriendlyTasks = 0;
	
	/*
	 * TODO: tweak this value. It may also be worth making it dynamic 
	 * (e.g. in later rounds of bidding use a less profitable but more 
	 * likely to succeed percentage (i.e. closer to 0) but focus on 
	 * earning a larger profit margin in earlier rounds... no idea if it'd pay off
	*/
	private final double BID_DIFF_PERCENTAGE = 0.85; 
	private final double RANDOM_MAG = 100;
	private final long MARGINAL_COST_OFFSET = 300;
	private final double PROBABILITY_TO_USE_TOTAL_MARGINAL_COST = 0.5;
	
	private Solution currentSolutionProposition; // The solution we're proposing
	private Solution currentSolution; // The solution we're actually going with
	private static final int AMOUNT_SOLUTIONS_KEPT_BY_SLS = 1;
	private HashSet<Task> tasksToHandle;
	private HashSet<Task> adversaryTasks;
	private long friendlyTimeout;
	private long adversaryTimeout;

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

		//first iteration only requires one sls - update these values accordingly as tasks are distributed to give more time to agent with more tasks
		this.friendlyTimeout = this.timeout_bid;
		this.adversaryTimeout = 0;
		
		/*
		 * I didn't make class vars for these since currently they are only used once to initialize stateActionTable
		 * If we want to change more than just the discount factor of the stateActionTable, we may need to reinitialize
		 * and then we may want local copies of the vars from  the xml
		*/
		double gamma = agent.readProperty("discount-factor", Double.class,0.95);
		double threshold = agent.readProperty("convergence-threshold", Double.class,0.01);
		int maxNumTasks = agent.readProperty("max-tasks", Integer.class,50);
		
		this.agent = agent;
		this.adversary = agent; //create a copy of the agent to be used for estimating adversary bidding
		this.stateActionTable = new StateActionTable(topology, distribution, gamma,maxNumTasks,threshold,this.agent.vehicles(),2);
		this.currentSolution = new Solution(this.agent.vehicles());
		this.currentSolutionProposition = null;
		this.tasksToHandle = new HashSet<Task>();
		this.adversaryTasks = new HashSet<Task>();
		this.bidRecord = new BidRecord();
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		this.bidRecord.recordBids(winner, bids); 
		System.out.print("[BIDS SUBMITTED] : ");
		for (Long bid : bids) {
			System.out.print(bid + " ");
		}
		
		System.out.println();
		System.out.println();
		
		long adversaryBid = 0;
		
		//assuming only 2 agents here:
		for(int i = 0; i<2; i++){
			if(i != this.agent.id()){
				adversaryBid = bids[i];
			}
		}
		
		//keep a running average of the discrepancy and use it to modify our bidding strategy
		long numTasksAuctioned = previous.id + 1; //add 1 b/c index starts at 0
		//i dont think there is ever a case where this is run 
		//and numTasksAuctioned = 0... if that could ever be a case, check for divide by zero
		this.adversaryBidDiscrepancy = (this.predictedAdversaryCostModified - adversaryBid + (numTasksAuctioned-1)*this.adversaryBidDiscrepancy)/numTasksAuctioned; 
		
		//assign task accordingly, update saved parameters
		if (winner == this.agent.id()) {
			this.tasksToHandle.add(previous);
			this.currentSolution = this.currentSolutionProposition;
			this.amountWonByFriendly = this.bidRecord.getTotalReward(this.agent.id());
		}
		else{
			this.adversaryTasks.add(previous);
			this.amountWonByAdversary = this.bidRecord.getTotalAdversaryReward(this.agent.id());
		}
		
		//update timeout values for next auction. currently uses a linear distribution of plan time to # of tasks

		System.out.print("Tasks Auctioned: ");
		System.out.println(numTasksAuctioned);
 
		System.out.print("Tasks in taskList: ");
		System.out.println(this.tasksToHandle.size());
		double friendlyTaskProportion = ((this.tasksToHandle.size()+1))/((double)numTasksAuctioned+2); //add 1 to numerator to account for next task. Add 2 to denom b/c adversary and friendly both consider next task
		System.out.print("FTP:");
		System.out.println(friendlyTaskProportion);
		this.friendlyTimeout = (long)((double)this.timeout_bid * friendlyTaskProportion);
		this.adversaryTimeout = (long)((double)this.timeout_bid * (1-friendlyTaskProportion));
		System.out.print("FTO:");
		System.out.println(this.friendlyTimeout);
		System.out.print("ATO:");
		System.out.println(this.adversaryTimeout);
	}

	@Override
	public Long askPrice(Task task) {

		System.out.println("[START] : Bid for :  " + task);

		SolutionList solutions = null;
		SolutionList adversarySolutions = null;
		if(task.id != 0){
			//first task is a special case. For all other tasks, consider adversary and our agent separately.
			// 1. For EACH AGENT, find the best solutions according to centralized (Arthur)
			TaskSet tasksWithAuctionedTask = TaskSet.copyOf(agent.getTasks());
			tasksWithAuctionedTask.add(task);
			SLS sls = new SLS(agent.vehicles(), tasksWithAuctionedTask, this.friendlyTimeout,
					Auction.AMOUNT_SOLUTIONS_KEPT_BY_SLS,this.stateActionTable,task.id);
			solutions = sls.getSolutions();

			//TODO: See if timeout needs to be halved to handle both calcs
			TaskSet adversaryTasksWithAuctionedTask = TaskSet.copyOf(agent.getTasks());
			adversaryTasksWithAuctionedTask.add(task);
			SLS slsAdversary = new SLS(adversary.vehicles(), adversaryTasksWithAuctionedTask, this.adversaryTimeout,
					Auction.AMOUNT_SOLUTIONS_KEPT_BY_SLS,this.stateActionTable,task.id);
			adversarySolutions = slsAdversary.getSolutions();
			this.predictedAdversaryCostRaw = (long) adversarySolutions.getFirstSolution().totalCost;
		}
		else{
			//for the first task, use full time just on one agent, and bid based only on that value. 
			TaskSet tasksWithAuctionedTask = TaskSet.copyOf(agent.getTasks());
			tasksWithAuctionedTask.add(task);
			SLS sls = new SLS(agent.vehicles(), tasksWithAuctionedTask, this.timeout_bid,
					Auction.AMOUNT_SOLUTIONS_KEPT_BY_SLS,this.stateActionTable,task.id);
			solutions = sls.getSolutions();
			this.predictedAdversaryCostRaw = (long) solutions.getFirstSolution().totalCost;
		}

		
		// 2. Use this.stateActionTable to discriminate the solutions of each agent,
		// selecting the best one (Simon)
		this.currentSolutionProposition = solutions.getFirstSolution();
		
		//Predict value of task to adversary
		long adversaryCost = this.predictedAdversaryCostRaw - this.amountWonByAdversary - this.adversaryBidDiscrepancy; 
		System.out.print("Adversary Break Even Bid = ");
		System.out.print(this.predictedAdversaryCostRaw);
		System.out.print("-");
		System.out.print(this.amountWonByAdversary);
		System.out.print("-");
		System.out.print(this.adversaryBidDiscrepancy);
		System.out.print("=");
		System.out.print(adversaryCost);
		System.out.println(" ");
		
		if(adversaryCost < 0){
			adversaryCost = 0;
		}
		//not sure if it is wise to directly subtract here... adding factor of 1/2 so this acts kindve like a cheaky PI controller
		this.predictedAdversaryCostModified = adversaryCost+1;
		
		
		//Randomly either bid based on minimum bid to break even OVERALL OR minimum bid to turn a profit over previous solution
		double randomDecider = Math.random();
		long bid = 0;
		double previousCost = this.currentSolution.totalCost;
		if(randomDecider<this.PROBABILITY_TO_USE_TOTAL_MARGINAL_COST){
			//Find our minimum bid to break even
			bid = (long) (this.currentSolutionProposition.totalCost - this.amountWonByFriendly) + (long)((Math.random()-.5)*this.RANDOM_MAG); 		
			System.out.print("Friendly Break Even Bid = ");
			System.out.print(this.currentSolutionProposition.totalCost);
			System.out.print("-");
			System.out.print(this.amountWonByFriendly);
			System.out.print("=");
			System.out.print(bid);
			System.out.println(" ");
			
		}
		else{
			//Find our minimum bid to turn a profit compared to the previous plan
			bid = (long) (this.currentSolutionProposition.totalCost - previousCost) + (long)((Math.random()-.5)*this.RANDOM_MAG); 		
			System.out.print("Friendly Profit Over Previous Bid = ");
			System.out.print(this.currentSolutionProposition.totalCost);
			System.out.print("-");
			System.out.print(previousCost);
			System.out.print("=");
			System.out.print(bid);
			System.out.println(" ");
			
		}

		if(bid<0){
			bid = 0;
		}
		
		if(bid<this.predictedAdversaryCostModified){
			//if our minimum bid is less than the adversary's min bid, then bid based on adversary's min bid
			bid += (long)((double)(this.predictedAdversaryCostModified-bid)*this.BID_DIFF_PERCENTAGE); 		
		}
		else{
			//bid nominal cost if adversary has a better possible bid, just in case they overbid accidentally
			//e.g. do nothing
			bid += this.MARGINAL_COST_OFFSET; //assume if the adversary has a better bid prediction, they won't be immediately below. Therefore little is risked by adding to our bid.
		}

		//TODO: do we have more time once the bids have been allocated to try and find a better solution? 
		//		If so, potentially we could use that to our advantage and accept a small loss with the thought that we'll further optimize our profit later
		// 3. Place a bid according to results 
		//bid = bid < 0 ? -bid : bid;
		System.out.println("[MY BID] : " + bid);
		return bid;
	}

	/**
	 * tasks are the tasks that THIS AGENT needs to handle
	 */
	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		
		
		System.out.println();
		System.out.println("START PLANNING [" + tasks.size() + "tasks ]");
		// TODO use the results of askPrice to output the plans (Arthur)
		// TODO MAKE THIS BETTER. YOU ALREADY COMPUTED THE SOLUTION
		ArrayList<Plan> plans = new ArrayList<Plan>();
		
		//TODO: continue SLS with part of plan timeout here!
		System.out.print("Cost before: ");
		System.out.println(this.currentSolution.totalCost);
		
		SLS sls = new SLS(agent.vehicles(), TaskSet.copyOf(this.agent.getTasks()), this.timeout_plan,
					Auction.AMOUNT_SOLUTIONS_KEPT_BY_SLS,this.stateActionTable, 50,this.currentSolution); //overloaded with current solution
		this.currentSolution = sls.getSolutions().getFirstSolution();

		System.out.print("Cost after: ");
		System.out.println(this.currentSolution.totalCost);
		
		this.currentSolution.replaceTasks(tasks);
		for (Vehicle v : vehicles) {
			plans.add(new Plan(v.getCurrentCity(), this.currentSolution.getVehicleAgendas().get(v)));
		}
		System.out.println("TOTAL COST : " + this.currentSolution.totalCost);
		System.out.println("TOTAL REWARD : " + this.bidRecord.getTotalReward(this.agent.id()));
		System.out.println("TOTAL PROFIT : " + (tasks.rewardSum() - this.currentSolution.totalCost));
		System.out.println();
		System.out.println("TOTAL REWARD OF THE OTHERS :");
		Set<Integer> winners = new LinkedHashSet<Integer>(this.bidRecord.getWinners());
		for (int winner : winners) {
			System.out.println("   " + winner + " : " + this.bidRecord.getTotalReward(winner));
		}
		System.out.println();
		return plans;
	}
}
