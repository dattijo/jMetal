package org.uma.jmetal.operator.mutation.impl;

import java.util.ArrayList;
import java.util.Comparator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.problem.integermatrixproblem.IntegerMatrixProblem;
import org.uma.jmetal.solution.integermatrixsolution.IntegerMatrixSolution;
import org.uma.jmetal.util.checking.Check;
import org.uma.jmetal.util.pseudorandom.BoundedRandomGenerator;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;
import org.uma.jmetal.util.pseudorandom.RandomGenerator;

/**
 *
 * @author aadatti
 */
public class TimeslotMove<T> implements MutationOperator<IntegerMatrixSolution<T>> {
    class localSearchComparator implements Comparator<IntegerMatrixSolution> {

        @Override
        public int compare(IntegerMatrixSolution a, IntegerMatrixSolution b) {
            return a.getObjective(0) < b.getObjective(0) ? -1 : a.getObjective(0) == b.getObjective(0) ? 0 : 1;
        }
    }

    private int evaluations;
    private int improvementRounds;
    private int numberOfImprovements;
    Comparator comparator;

    private double mutationProbability;
    private RandomGenerator<Double> mutationRandomGenerator;
    private BoundedRandomGenerator<Integer> positionRandomGenerator;
    private IntegerMatrixProblem problem;

    int numberOfTimeslots;
    boolean timeslotsExhausted=false;

    public TimeslotMove(double mutationProbability, int timeslots, IntegerMatrixProblem problem) {
        this(mutationProbability, () -> JMetalRandom.getInstance().nextDouble(), (a, b) -> JMetalRandom.getInstance().nextInt(a, b), timeslots, problem);
    }

    /**
     * Constructor
     */
    public TimeslotMove(double mutationProbability, RandomGenerator<Double> randomGenerator, int timeslots, IntegerMatrixProblem problem) {
        this(mutationProbability, randomGenerator, BoundedRandomGenerator.fromDoubleToInteger(randomGenerator), timeslots, problem);
    }

    /**
     * Constructor
     */
    public TimeslotMove(double mutationProbability, RandomGenerator<Double> mutationRandomGenerator, BoundedRandomGenerator<Integer> positionRandomGenerator, int timeslots, IntegerMatrixProblem problem) {
        Check.probabilityIsValid(mutationProbability);
        this.mutationProbability = mutationProbability;
        this.mutationRandomGenerator = mutationRandomGenerator;
        this.positionRandomGenerator = positionRandomGenerator;
        this.numberOfTimeslots = timeslots;
        this.problem = problem;
        this.numberOfImprovements = 0;
        this.improvementRounds = 1000;
        this.comparator = new localSearchComparator();
    }

    @Override
    public double getMutationProbability() {
        return mutationProbability;
    }

    public void setMutationProbability(double mutationProbability) {
        this.mutationProbability = mutationProbability;
    }

    @Override
    public IntegerMatrixSolution<T> execute(IntegerMatrixSolution<T> solution) {
        Check.isNotNull(solution);
//        System.out.println("\n\nMUTATING NEW SOLUTION");
        int best;
        evaluations = 0;
                
        int rounds = improvementRounds;

        int i = 0;
        while (i < rounds) {
//            System.out.println("Round = "+i+" of "+rounds);
            if(timeslotsExhausted==true)break;
            IntegerMatrixSolution mutatedSolution = doMutation((IntegerMatrixSolution) solution.copy());
            
            problem.evaluate(mutatedSolution);
            evaluations++;

            best = comparator.compare(mutatedSolution, solution);
            if (best == -1) {
                solution = mutatedSolution;
                numberOfImprovements++;
            } else if (best == 0) {
                if (mutationRandomGenerator.getRandomValue() < 0.5) {
                    solution = mutatedSolution;
                }
            }
            i++;
        }

        return solution;
    }

    public IntegerMatrixSolution<T> doMutation(IntegerMatrixSolution<T> solution) {       
        
        int solutionLength = solution.getNumberOfVariables();

        if ((solutionLength != 0) && (solutionLength != 1)) {
            if (mutationRandomGenerator.getRandomValue() < mutationProbability) {
                int randExamIndex = positionRandomGenerator.getRandomValue(0, solutionLength - 1);
                int randTimeslotIndex;
                ArrayList<Integer> freeTimeslots = getFreeTimeslots(solution);
//                System.out.println("freeTimeslots="+freeTimeslots.size());
                if(freeTimeslots.size()>0){
                    randTimeslotIndex = positionRandomGenerator.getRandomValue(0, freeTimeslots.size()-1);
                }
                else{
                    timeslotsExhausted=true;
                    return solution;
                }
                                
                ArrayList exam = (ArrayList)solution.getVariable(randExamIndex);
//                System.out.println("TimeslotChangeMutation on exam: "+exam.toString());
                int oldRoom = getRoom(exam);
                int oldTimeslot = getTimeslot(exam);
                
                exam.set(oldTimeslot, 0);
                exam.set(freeTimeslots.get(randTimeslotIndex), oldRoom);
//                System.out.println("TimeslorChangeMutation Successful: "+exam.toString());
                solution.setVariable(randExamIndex, (T)exam);
            }
        }
        
        return (IntegerMatrixSolution) solution.copy();
    }
    
    ArrayList getFreeTimeslots(IntegerMatrixSolution solution){
        ArrayList usedTimeslots = new ArrayList();
        ArrayList freeTimeslots = new ArrayList();
        
        for(int i=0; i<solution.getNumberOfVariables();i++){
            int timeslot = getTimeslot((ArrayList)solution.getVariable(i));
            if(!usedTimeslots.contains(timeslot)){
                usedTimeslots.add(timeslot);
            }
        }
        
        for(int i=0;i<numberOfTimeslots;i++){
            if(!usedTimeslots.contains(i)){
                freeTimeslots.add(i);
            }
        }        
        return freeTimeslots;        
    } 
    
    public int getTimeslot(ArrayList<Integer> exam){
        for (int i = 0; i < exam.size(); i++){
            if (exam.get(i) != 0) {
                return i;
            }
        }
        return -1;
    }

    public int getRoom(ArrayList<Integer> exam) {
        for (int i = 0; i < exam.size(); i++) {
            if (exam.get(i) != 0) {
                return exam.get(i);
            }
        }
        return -1;
    }
}
