import java.util.ArrayList;
import java.util.Collection;

import logist.task.TaskSet;

/**
 * An ordered list of solutions.
 * 
 * The amount of solutions that can be held 
 * by a SolutionList is limited by the SolutionList's  maxAmountOfSolutions
 * 
 * Each element of the list is unique.
 * 
 * The list is ordered so
 * that the solution with the lowest goodness is the first element, and so on.
 * 
 * @author heimdall
 *
 */
public class SolutionList {
	private ArrayList<Solution> solutions;
	private final int maxAmountOfSolutions;

	/**
	 * Construct a solutionlist holding no solution at all.
	 * 
	 * @param amountOfSolutions
	 */
	public SolutionList(int maxAmountOfSolutions) {
		this.maxAmountOfSolutions = maxAmountOfSolutions;
		this.solutions = new ArrayList<Solution>();
	}

	/**
	 * Return a list of solutions.
	 * The first element has the lowest total cost, and so on.
	 * The size of the list is <= this.amountOfSolutions
	 * @return
	 */
	public ArrayList<Solution> getAll() {
		return this.solutions;
	}
	
	/**
	 * @return null if this SolutionList doesn't have any solution, 
	 * otherwise the solution with the lowest cost it has
	 */
	public Solution getFirstSolution() {
		return this.solutions.size() == 0 ? null : this.solutions.get(0);
	}
	
	
	/**
	 * @return null if this SolutionList doesn't have any solution, 
	 * otherwise the solution with the highest cost it has
	 */
	public Solution getLastSolution() {
		return this.solutions.size() == 0 ? null : this.solutions.get(this.solutions.size() - 1);
	}

	/**
	 * Add the Solution s to this SolutionList
	 * If s can be added, 
	 * it is added to this SolutionList's solutions just after (from left to right)
	 * the last solution (from left to right) whose cost is <= s's totalCost
	 * For instance, the insertion of a Solution whose cost is 8 :
	 * [2, 3, 8, 8, 8, 9, 11] => [2, 3, 8, 8, 8, *8*, 9, 11]
	 * If s cannot be added (lack of space given s.totalCost), s is not added
	 * @param s Solution to be added
	 * @return true is s has been added successfully, false otherwise 
	 */
	public boolean add(Solution s) {
		boolean solutionAdded = false;
		
		/*
		 *  Add s where it belongs (if it can be added)
		 */
		
		if (!this.solutions.contains(s)) {
			// From right to left
			for (int i = this.solutions.size() - 1; i >= -1; i--) {
				if  (i == -1) {
					this.solutions.add(0, s);
					solutionAdded = true;
					break;
				}
				else if (this.solutions.get(i).goodness <= s.goodness) {
					this.solutions.add(i + 1, s);
					solutionAdded = true;
					break;
				}
			}
		}

		
		// Trim this.solutions
		if (this.solutions.size() > this.maxAmountOfSolutions) {
			this.solutions = new ArrayList<Solution>(this.solutions.subList(0, this.maxAmountOfSolutions));
		}
		return solutionAdded;
	}
	
	/**
	 * Try to add all the solutions in c to this SolutionList
	 * @param c
	 */
	public void addAll(Collection<? extends Solution> c) {
		for (Solution s : c) {
			this.add(s);
		}
	}
}
