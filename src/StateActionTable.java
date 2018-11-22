import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import logist.plan.Action;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;
import logist.plan.Action.Move;
import logist.simulation.Vehicle;
import logist.plan.Action.Delivery;

/**
 * This class provides you with the best action to do given a certain state
 */
public class StateActionTable {

	private List<City> cityList;
	private int numCities;
	private int numActions;
	private ArrayList<ArrayList<ArrayList<Double>>> T;
	private HashMap<Vehicle,ArrayList<ArrayList<Double>>> P = new HashMap<Vehicle,ArrayList<ArrayList<Double>>>();
	private HashMap<Vehicle,ArrayList<Double>> V = new HashMap<Vehicle,ArrayList<Double>>();
	private HashMap <Vehicle,ArrayList<Integer>> best = new HashMap<Vehicle,ArrayList<Integer>>();;
	private double gamma;// discount factor
	private double gamma_0; 
	private int maxNumTasks;
	private Topology topology;
	private TaskDistribution td;
	private List<Vehicle> vehicleList;
	private double rewardPerDistanceEstimate;
	
	//TODO: set threshold in XML such that fast convergence is priority. Doesn't need to be super optimal (maybe 1% of the expected value?)
	private double threshold; 
	public StateActionTable(Topology topology, TaskDistribution td, Double gamma, int maxNumTasks, double threshold, List<Vehicle> vehicleList, double rewardPerDistanceEstimate) {
		this.cityList = topology.cities();
		this.numCities = this.cityList.size();
		this.numActions = this.numCities + 1;
		this.gamma = gamma;
		this.gamma_0 = gamma;
		this.maxNumTasks = maxNumTasks;
		this.threshold = threshold;
		this.vehicleList = vehicleList; 
		this.rewardPerDistanceEstimate = rewardPerDistanceEstimate;
		
		computeStateTransitionProbability(td);
		for(Vehicle vehicle : vehicleList){
			
			computeProfitMatrix(topology, td, vehicle);
			computeBest(vehicle);			
		}
	}

	private void computeStateTransitionProbability(TaskDistribution td) {

		// Initialize the T as full of zeros
		ArrayList<ArrayList<ArrayList<Double>>> T = new ArrayList<ArrayList<ArrayList<Double>>>();

		for (int i = 0; i < this.numCities * (this.numCities + 1); i++) {
			ArrayList<ArrayList<Double>> zero_array = new ArrayList<ArrayList<Double>>();
			for (int j = 0; j < this.numActions; j++) {
				ArrayList<Double> zero_column = new ArrayList<Double>();
				for (int k = 0; k < this.numCities * (this.numCities + 1); k++) {
					zero_column.add(0.0);
				}
				zero_array.add(zero_column);
			}
			T.add(zero_array);
		}

		int valueToEncodeState = this.numCities + 1;

		for (int current_from = 0; current_from < this.numCities; current_from++) {
			for (int current_to = 0; current_to < this.numCities + 1; current_to++) {
				int state = current_from * valueToEncodeState + current_to;

				for (int future_from = 0; future_from < this.numCities; future_from++) {
					for (int future_to = 0; future_to < this.numCities + 1; future_to++) {
						int future_state = future_from * valueToEncodeState + future_to;

						/*
						 * Action are encoded such that : if it's a number corresponding to a city, it
						 * means you have a task and you need to go to that city. It its value isn't the
						 * value of a city, then you pick up
						 */
						for (int action = 0; action < this.numActions; action++) {
							//TODO: double check that this is the line i wanted to remove
							//if ((current_to != current_from) && (future_to != future_from)&& (current_from != future_from)) {
								if ((action == this.numActions - 1) && (current_to == future_from)) {
									if (future_to == this.numCities) {
										// Compute sum
										double sum = 0;
										for (int city_to = 0; city_to < this.numCities; city_to++) {
											sum += td.probability(this.cityList.get(future_from),
													this.cityList.get(city_to));
										}

										T.get(state).get(action).set(future_state, 1 - sum);
									} else {
										double p = td.probability(this.cityList.get(future_from),
												this.cityList.get(future_to));
										T.get(state).get(action).set(future_state, p);
									}
								} else {
									if ((action != current_from) && (action == future_from)) {
										if (future_to == this.numCities) {

											// Compute sum
											double sum = 0;
											for (int city_to = 0; city_to < this.numCities; city_to++) {
												sum += td.probability(this.cityList.get(future_from),
														this.cityList.get(city_to));
											}

											T.get(state).get(action).set(future_state, 1 - sum);

										} else {
											double p = td.probability(this.cityList.get(future_from),
													this.cityList.get(future_to));
											T.get(state).get(action).set(future_state, p);
										}
									}
								}
							//}
						}

					}
				}
			}
		}
		this.T = T;
	}

	private void computeProfitMatrix(Topology topology, TaskDistribution td, Vehicle vehicle) {
		
		double cost_per_km = (double) vehicle.costPerKm();
		// print city names
		List<City> cityList = topology.cities();

		// initialize maps for p, r and d
		ArrayList<ArrayList<Double>> nullList = new ArrayList<ArrayList<Double>>();
		P.put(vehicle, nullList);
		
		for (int current_from = 0; current_from < numCities; current_from++) {

			for (int current_to = 0; current_to < numCities + 1; current_to++) {

				int s = (current_from) * (numCities + 1) + current_to;
				P.get(vehicle).add(new ArrayList<Double>());

				for (int a = 0; a < numActions; a++) {

					P.get(vehicle).get(s).add(a, (double) 0); // initialize values as 0

					City fromCity = cityList.get(current_from);

					if (a < numCities) // no task
					{
						City toCityAction = cityList.get(a);
						P.get(vehicle).get(s).set(a, -1 * fromCity.distanceTo(toCityAction) * cost_per_km);
					} else // if there is a task
					{
						if (current_to < numCities) {
							City toCity = cityList.get(current_to);
							double reward = this.rewardPerDistanceEstimate*toCity.distanceTo(fromCity);
							double distanceCost = fromCity.distanceTo(toCity) * cost_per_km;
							double profit = reward - distanceCost;
							P.get(vehicle).get(s).set(a, profit);
						}
					}
				}
			}
		}
	}

	private void computeBest(Vehicle vehicle) {
		// Create and initialize statesConverged
		ArrayList<Boolean> statesConverged = new ArrayList<Boolean>();
		for (int i = 0; i < this.numCities * (this.numCities + 1); i++) {
			statesConverged.add(false);
		}

		boolean converged = false;

		// Create and initialize Q
		ArrayList<ArrayList<Double>> Q = new ArrayList<ArrayList<Double>>();

		for (int i = 0; i < this.numCities * (this.numCities + 1); i++) {
			ArrayList<Double> zero_column = new ArrayList<Double>();
			for (int k = 0; k < this.numCities + 1; k++) {
				zero_column.add(0.0);
			}
			Q.add(zero_column);
		}

		// initialize V, which will eventually store the best
		// values...
		this.V.put(vehicle, new ArrayList<Double>());
		for (int i = 0; i < this.numCities * (this.numCities + 1); i++) {
			V.get(vehicle).add(0.0);
		}

		// Create and initialize VTemp
		ArrayList<Double> VTemp = new ArrayList<Double>();
		for (int i = 0; i < this.numCities * (this.numCities + 1); i++) {
			VTemp.add(0.0);
		}
		// Create and initialize best
		ArrayList<Integer> best = new ArrayList<Integer>();
		for (int i = 0; i < this.numCities * (this.numCities + 1); i++) {
			best.add(0);
		}

		int valueToEncodeState = this.numCities + 1;

		// Start actual algorithm
		while (!converged) {
			for (int current_from = 0; current_from < this.numCities; current_from++) {
				for (int current_to = 0; current_to < this.numCities + 1; current_to++) {

					int state = current_from * valueToEncodeState + current_to;

					for (int action = 0; action < this.numActions; action++) {

						double discounted_future = 0;
						for (int i = 0; i < this.numCities * (this.numCities + 1); i++) {
							discounted_future = discounted_future + this.T.get(state).get(action).get(i) * V.get(vehicle).get(i);
						}
						Q.get(state).set(action, this.P.get(vehicle).get(state).get(action) + this.gamma * discounted_future);// update
																													// value
					}
					VTemp.set(state, Collections.max(Q.get(state)));
					best.set(state, Q.get(state).indexOf(VTemp.get(state)));
					
					while (Collections.max(Q.get(state)) == 0) {
						double max = Collections.max(Q.get(state));
						double min = Collections.min(Q.get(state));
						int index = Q.get(state).indexOf(max);
						Q.get(state).set(index, min); // set no-transition value
														// to min value so it is
														// not selected

						VTemp.set(state, Collections.max(Q.get(state)));
						best.set(state, Q.get(state).indexOf(VTemp.get(state)));
					}
					if (VTemp.get(state) - V.get(vehicle).get(state) < this.threshold) {
						statesConverged.set(state, true);
					}
					V.get(vehicle).set(state, VTemp.get(state));
				}
			}
			if (statesConverged.indexOf(false) == -1) {
				converged = true;
			}
		}
		this.best.put(vehicle,best);
	}

	/*
	 * gets future value of the final task in a plan. 
	 */
	public double getFutureValueOfLastTask(Vehicle vehicle, City endCity) {
		int valueToEncodeState = this.numCities + 1;

		// find the numerical values associated with each city
		int current_from = this.cityList.indexOf(endCity);
		int current_to = this.numCities;
		// Compute the state
		int state = current_from * valueToEncodeState + current_to;
		//find the predicted future value of the state
		double value = V.get(vehicle).get(state); 
		return value;
	}
	
	public Action getBestAction(City fromCity, Task availableTask, Vehicle vehicle) {
		Action action = null;

		int valueToEncodeState = this.numCities + 1;

		// find the numerical values associated with each city
		int current_from = this.cityList.indexOf(fromCity);
		int current_to;

		if (availableTask != null) {// If there was a task in the from city
			current_to = this.cityList.indexOf(availableTask.deliveryCity);
		} else {
			current_to = this.numCities;
		}

		// Compute the state
		int state = current_from * valueToEncodeState + current_to;

		if (this.best.get(vehicle).get(state) < this.numCities) {// Don't take the package
													// and go elsewhere
			City toCity = this.cityList.get(this.best.get(vehicle).get(state));

			List<City> futureCities = fromCity.pathTo(toCity);
			action = new Move(futureCities.get(0));
		} else {
			action = new Delivery(availableTask);
		}

		return action;
	}

	public void printBestTable() {
		//written for debugging purposes
		for (int current_from = 0; current_from < this.numCities; current_from++) {

			for (int current_to = 0; current_to < this.numCities + 1; current_to++) {
				int state = current_from * (this.numCities + 1) + current_to;
				System.out.print(this.best.get(state) + " ");
			}
			System.out.println(" ");
		}
	}

	private void updateGamma(int tasksCompleted){
		if(tasksCompleted<maxNumTasks){
			this.gamma = this.gamma_0*((double)(maxNumTasks-tasksCompleted)/((double)maxNumTasks));
		}
	}
	
	public void updateTables(int tasksCompleted){
		this.updateGamma(tasksCompleted);
		for(Vehicle vehicle : vehicleList){
			computeProfitMatrix(this.topology, this.td, vehicle);
			computeBest(vehicle);	
		}
	}
}
