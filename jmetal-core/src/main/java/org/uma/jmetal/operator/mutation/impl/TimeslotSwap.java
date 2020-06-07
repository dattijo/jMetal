package org.uma.jmetal.operator.mutation.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.solution.integermatrixsolution.IntegerMatrixSolution;
import org.uma.jmetal.solution.integermatrixsolution.impl.DefaultIntegerMatrixSolution;
import org.uma.jmetal.util.checking.Check;
import org.uma.jmetal.util.pseudorandom.BoundedRandomGenerator;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;
import org.uma.jmetal.util.pseudorandom.RandomGenerator;

/**
 *
 * @author aadatti
 */
public class TimeslotSwap<T> implements MutationOperator<IntegerMatrixSolution<T>> {   
   
    Comparator comparator;

    private double mutationProbability;
    private RandomGenerator<Double> mutationRandomGenerator;
    private BoundedRandomGenerator<Integer> positionRandomGenerator;

    int numberOfTimeslots;
    boolean swap;
    boolean shuffle;
    ArrayList<Integer> freeTimeslots;
    ArrayList<Integer> usedTimeslots;

    public TimeslotSwap(double mutationProbability, int timeslots, boolean swap, boolean shuffle) {
        this(mutationProbability, () -> JMetalRandom.getInstance().nextDouble(), 
                (a, b) -> JMetalRandom.getInstance().nextInt(a, b), timeslots, swap, shuffle);
    }

    /**
     * Constructor
     */
    public TimeslotSwap(double mutationProbability, RandomGenerator<Double> randomGenerator, int timeslots, boolean swap, boolean shuffle) {
        this(mutationProbability, randomGenerator, BoundedRandomGenerator.fromDoubleToInteger(randomGenerator), timeslots, swap, shuffle);
    }

    /**
     * Constructor
     */
    public TimeslotSwap(double mutationProbability, RandomGenerator<Double> mutationRandomGenerator,
            BoundedRandomGenerator<Integer> positionRandomGenerator, int timeslots, boolean swap, boolean shuffle) {
        Check.probabilityIsValid(mutationProbability);
        this.mutationProbability = mutationProbability;
        this.mutationRandomGenerator = mutationRandomGenerator;
        this.positionRandomGenerator = positionRandomGenerator;
        this.numberOfTimeslots = timeslots; 
        this.swap = swap;
        this.shuffle = shuffle;
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
        return doMutation((IntegerMatrixSolution) solution);
    }

    public IntegerMatrixSolution<T> doMutation(IntegerMatrixSolution<T> solution) {       
        
        int solutionLength = solution.getNumberOfVariables();

        if ((solutionLength != 0) && (solutionLength != 1)) {
            
            if (mutationRandomGenerator.getRandomValue() < mutationProbability) {   
//                System.out.println("\n\nNEW SOLUTION");
                getFreeTimeslots(solution); 
                ArrayList<Integer> allTimeslots = new ArrayList();
                for(int i=0;i<numberOfTimeslots;i++){
                    allTimeslots.add(i);
                }
                ArrayList<ArrayList<Integer>> mySol = new ArrayList(solutionLength);
                for(int i=0;i<solutionLength;i++)mySol.add(new ArrayList());
                IntegerMatrixSolution tmpSolution = (IntegerMatrixSolution) solution.copy();                
                ArrayList exam;
                if(shuffle){                    
//                    System.out.println("usedTimeslots = "+usedTimeslots.toString());
//                    System.out.println("Solution before shuffle="+solution.getVariables());
//                    Collections.shuffle(usedTimeslots);  
                    Collections.shuffle(allTimeslots);
                    for(int slot=0; slot<allTimeslots.size();slot++){    
                        for(int i=0;i<solutionLength;i++){  
                            exam = new ArrayList();
                            exam.addAll((ArrayList)solution.getVariable(i));
                            if(getTimeslot(exam)==slot){
//                                System.out.println("\nOriginal Exam "+i+":"+solution.getVariable(i));
                                int room = getRoom(exam);                        
                                exam.set(slot, 0);                                
//                                exam.set(usedTimeslots.get(slot), room);
                                exam.set(allTimeslots.get(slot), room);
                                mySol.set(i,exam);                                
//                                System.out.println("Shuffled Exam "+i+":"+mySol.get(i).toString());
                            }                                                    
                        }                        
                    }
//                    System.out.println("\nRESULTS:");                    
                    for(int i=0;i<solutionLength;i++){ 
                        tmpSolution.setVariable(i, mySol.get(i));
//                        System.out.println("\nsolution  "+i+": "+solution.getVariable(i));
//                        System.out.println("   mySol  "+i+": "+mySol.get(i).toString());
                    }  
//                    System.out.println("Solution after shuffle: "+tmpSolution.getVariables());
                }
                else{
                    int rand1 = positionRandomGenerator.getRandomValue(0, usedTimeslots.size() - 1);
                    int randTimeslotIndex1 = usedTimeslots.get(rand1);

                    int rand2,  randTimeslotIndex2;

                    if(swap){
                        rand2 = positionRandomGenerator.getRandomValue(0, usedTimeslots.size() - 1); 
                        randTimeslotIndex2 = usedTimeslots.get(rand2);
                    }
                    else{
                        rand2 = positionRandomGenerator.getRandomValue(0, freeTimeslots.size() - 1); 
                        randTimeslotIndex2 = freeTimeslots.get(rand2);
                    }
                    int count=0;
//                    System.out.println("\n\nTimeslot 1 = "+randTimeslotIndex1);
//                    System.out.println("Timeslot 2 = "+randTimeslotIndex2);
                    while (randTimeslotIndex1 == randTimeslotIndex2) {
                        if (randTimeslotIndex1 == (solutionLength - 1)) {
                            randTimeslotIndex2 = positionRandomGenerator.getRandomValue(0, usedTimeslots.size() - 2);
                        } else {
                            randTimeslotIndex2 = positionRandomGenerator.getRandomValue(0, usedTimeslots.size() - 1);
                        }
//                        System.out.println("count="+(++count));
                    }                                      

                    for(int i=0;i<solution.getNumberOfVariables();i++){
                        exam = (ArrayList)solution.getVariable(i);
                        if(getTimeslot(exam)==randTimeslotIndex1){
//                            System.out.println("Moving exam "+i+" from timelsot "+randTimeslotIndex1+" to "+randTimeslotIndex2);
                            int room = getRoom(exam);
                            exam.set(randTimeslotIndex1, 0);
                            exam.set(randTimeslotIndex2, room);
                            tmpSolution.setVariable(i, exam);
                        }
                        else if(getTimeslot(exam)==randTimeslotIndex2){
//                            System.out.println("Moving exam "+i+" from timelsot "+randTimeslotIndex2+" to "+randTimeslotIndex1);
                            int room = getRoom(exam);
                            exam.set(randTimeslotIndex2, 0);
                            exam.set(randTimeslotIndex1, room);
                            tmpSolution.setVariable(i, exam);
                        }                                        
                    }
                }                
                solution = tmpSolution;
            }
        }        
        return solution;
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
    
    void getFreeTimeslots(IntegerMatrixSolution solution){
        usedTimeslots = new ArrayList();
        freeTimeslots = new ArrayList();
        
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
    } 
}
