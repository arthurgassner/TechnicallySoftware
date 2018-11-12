We use centralized to get an idea of the marginal cost of adding an additional task to our set of tasks.

We use reactive to get an idea of how likely this "new" path is, considering that we might encounter interesting tasks on the way.

We bid accordingly.

So :

1. Process lookup table (reactive) [do shit to it so it works well] [we might have to change reactive so that it recomputes the lookup table for a certain subset of solutions]

2. For each added tasks, compute the expected marginal cost of the task (through centralized) for yourself AND for each adversary. 

3. 

PHASE 1 :

1. Preprocess lookup table (reactive). It never changes throughout the run of the programm.

2. For each added tasks, find the 10 (or more or less) best solutions (according to their profit) through centralized. Do the same thing for each adversary.

3. Use the lookup table to choose one solution (out of the 10). To do that, use a "goodness factor" (sum of how good each city you're going through is / total cost of your solution). Do the same thing for each adversary.

4. We bid according to the marginal cost of OUR solution and the marginal cost of the solution we computed for each vehicle. (For instance


IMPROVEMENTS :

a0. Make it easier to compare and test (JUnit)

a1. Use the static lookup table for each neighbor (to discriminate between neighbour WHILE centralized is running). This is combining 2 and 3, and we might then get rid of 3.
 
a2. Dynamic lookup table : change the discount factor every time the amount of tasks left changes (and therefore reprocess it for every bid)

b. Look into the look up table and see if there's a better way to account for the fact that we're guaranteed to go to certain cities 


c. Record the differences between what we expected each adversary to bid and what they actually bid, and based on that adjust our estimation of what their bid is gonna be. Based on THAT, adjust our bid.

d. Add randomness to our own bidding

e. 
