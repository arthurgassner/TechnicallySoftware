import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import logist.plan.Action;
import logist.plan.Action.Delivery;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology.City;

public class Solution {
	private final HashMap<Vehicle, ArrayList<Action>> vehicleAgendas;
	private final HashMap<Vehicle, ArrayList<TaskWrapper>> simpleVehicleAgendas;
	private final List<Vehicle> vehicles;
	public final double totalCost;

	public Solution(HashMap<Vehicle, ArrayList<TaskWrapper>> simpleVehicleAgendas) {
		this.simpleVehicleAgendas = simpleVehicleAgendas;
		ArrayList<Vehicle> vehicles = new ArrayList<Vehicle>();
		vehicles.addAll(simpleVehicleAgendas.keySet());
		this.vehicles = vehicles;
		this.vehicleAgendas = generateCompleteAgendas();
		this.totalCost = this.getTotalCost();
	}
	
	/**
	 * Construct a solution where all the vehicles do NOTHING.
	 * No tasks is handled here.
	 * The cost is 0 since NOTHING is done.
	 * @param vehicles
	 */
	public Solution(List<Vehicle> vehicles) {
		this.simpleVehicleAgendas = new HashMap<Vehicle, ArrayList<TaskWrapper>>();
		for (Vehicle v : vehicles) {
			this.simpleVehicleAgendas.put(v, new ArrayList<TaskWrapper>());
		}
		
		this.vehicles = vehicles;
		this.vehicleAgendas = generateCompleteAgendas();
		this.totalCost = 0;
	}

	public ArrayList<Vehicle> getVehicles() {
		return new ArrayList<Vehicle>(this.vehicles);
	}

	public HashMap<Vehicle, ArrayList<TaskWrapper>> getSimpleVehicleAgendas() {
		return this.simpleVehicleAgendas;
	}

	public HashMap<Vehicle, ArrayList<Action>> getVehicleAgendas() {
		return vehicleAgendas;
	}

	/**
	 * 
	 * @return the total cost of this solution
	 */
	private double getTotalCost() {
		double totalCostOfThisSolution = 0;
		for (Vehicle vehicle : this.vehicleAgendas.keySet()) {
			double totalDistanceOfThisVehicle = 0;

			City fromCity = vehicle.getCurrentCity();

			for (int i = 0; i < this.simpleVehicleAgendas.get(vehicle).size(); i++) {
				City toCity = this.simpleVehicleAgendas.get(vehicle).get(i).getEndCity();
				totalDistanceOfThisVehicle += fromCity.distanceTo(toCity);
				fromCity = toCity;
			}

			double totalCostOfThisVehicle = totalDistanceOfThisVehicle * vehicle.costPerKm();
			totalCostOfThisSolution += totalCostOfThisVehicle;
		}
		return totalCostOfThisSolution;
	}

	private HashMap<Vehicle, ArrayList<Action>> generateCompleteAgendas() {

		HashMap<Vehicle, ArrayList<Action>> completeVehicleAgenda = new HashMap<Vehicle, ArrayList<Action>>();

		// Generate complete vehicle task lists from simplified version
		for (Vehicle vehicle : this.vehicles) {
			ArrayList<TaskWrapper> simpleTaskList = simpleVehicleAgendas.get(vehicle);
			City origin = vehicle.getCurrentCity();
			for (TaskWrapper task : simpleTaskList) {
				if (completeVehicleAgenda.containsKey(vehicle)) {
					completeVehicleAgenda.get(vehicle).addAll(this.getAgenda(origin, task));
				} else {
					completeVehicleAgenda.put(vehicle, this.getAgenda(origin, task));
				}
				origin = task.getEndCity();
			}
		}

		return completeVehicleAgenda;
	}

	/**
	 * 
	 * @param origin
	 * @param task
	 * @return a list of Action, consisting, in order, of the actions needed to go
	 *         from origin and then perform the action related to task
	 */
	private ArrayList<Action> getAgenda(City origin, TaskWrapper task) {

		ArrayList<Action> actions = new ArrayList<Action>();
		ArrayList<City> pathCities = (ArrayList<City>) origin.pathTo(task.getEndCity());

		// first add actions for movement from vehicle's current location to target city
		for (City pathCity : pathCities) {
			actions.add(new Move(pathCity));
		}

		// then add action for specific task component
		if (task.isPickup()) {
			actions.add(new Pickup(task.getTask()));
		} else {
			actions.add(new Delivery(task.getTask()));
		}

		return actions;
	}

	/**
	 * 
	 * @param vehicle
	 * @return the tasks handled by vehicle
	 */
	public HashSet<Task> getTasks(Vehicle vehicle) {
		HashSet<Task> tasksOfThisVehicle = new HashSet<Task>();
		for (TaskWrapper taskWrapper : this.simpleVehicleAgendas.get(vehicle)) {
			tasksOfThisVehicle.add(taskWrapper.getTask());
		}
		return tasksOfThisVehicle;
	}

	@Override
	public boolean equals(Object that) {
		if (!(that instanceof Solution))
			return false;
		Solution s = (Solution) that;

		for (Vehicle v : this.simpleVehicleAgendas.keySet()) {
			if (!s.simpleVehicleAgendas.keySet().contains(v)) {
				return false;
			}
			
			if (this.simpleVehicleAgendas.get(v).size() != s.simpleVehicleAgendas.get(v).size()) {
				return false;
			}
			
			for (int i = 0;i < this.simpleVehicleAgendas.get(v).size(); i++) {
				TaskWrapper tw1 = this.simpleVehicleAgendas.get(v).get(i);
				TaskWrapper tw2 = s.simpleVehicleAgendas.get(v).get(i);
				if (!tw1.equals(tw2)) {
					return false;
				}
			}
		}
		return true;
	}
}
