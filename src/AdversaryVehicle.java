import java.awt.Color;
import logist.task.TaskSet;
import logist.topology.Topology.City;

public class AdversaryVehicle {

	private int id;
	private String name;
	private int capacity;
	private City homeCity;
	private double speed;
	private int costPerKm;
	private City currentCity;
	private TaskSet currentTasks;
	private long reward;
	private Color color;
	private long distance;

	public AdversaryVehicle(int id, int capacity, City homeCity, int costPerKm) {
		this.id = id;
		this.capacity = capacity;
		this.homeCity = homeCity;
		this.currentCity = homeCity;
		this.costPerKm = costPerKm;
		this.reward = 0;
		this.color = Color.black;
		this.currentTasks = null;	
		this.distance = 0;
	}

	/** The cost/km of the vehicle */
	int costPerKm(){
		return this.costPerKm;
	}

	City getCurrentCity(){
		return this.currentCity;
	}

	TaskSet getCurrentTasks(){
		return this.currentTasks;
	}

	/** The sum of rewards for all delivered tasks */
	long getReward(){
		return this.reward;
	}

	long getDistanceUnits(){
		return this.distance;
	}

	/** The total distance (in km) traveled by the vehicle */
	double getDistance(){
		return (double) this.distance/1000;
	}	
}
