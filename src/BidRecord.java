import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * Class used to record the bids of the agents, and output interesting results
 * regarding these bids (average, stddev, ...)
 * 
 * Each agent is represented as a unique int
 * 
 * @author heimdall
 *
 */
public class BidRecord {
	private ArrayList<ArrayList<Long>> bids;
	private ArrayList<Integer> winners;

	public BidRecord() {
		this.bids = new ArrayList<ArrayList<Long>>();
		this.winners = new ArrayList<Integer>();
	}

	/**
	 * record the bids as well as the winner
	 * @param winner
	 * @param bids
	 */
	public void recordBids(int winner, Long[] bids) {
		this.winners.add(winner);
		this.bids.add(new ArrayList<Long>(Arrays.asList(bids)));
	}

	/**
	 * 
	 * @return a list with the winner of each round. The first element is the winner
	 *         of each round
	 */
	public ArrayList<Integer> getWinners() {
		return this.winners;
	}

	/**
	 * 
	 * @param agentID the id of the agent of interest
	 * @return the sum of all the rewards that the agent of interest will get once
	 *         it has delivered all the tasks assigned to him
	 */
	public long getTotalReward(int agentID) {
		long total_reward = 0; 
		int current_round = 0;
		for (int id : this.winners) {
			if (id == agentID) {
				total_reward += Collections.min(this.bids.get(current_round));
			}
			current_round++;
		}
		return total_reward;
	}
}
