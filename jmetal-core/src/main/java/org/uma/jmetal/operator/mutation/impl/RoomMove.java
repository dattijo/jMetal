package org.uma.jmetal.operator.mutation.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.problem.integermatrixproblem.IntegerMatrixProblem;
import org.uma.jmetal.solution.integermatrixsolution.IntegerMatrixSolution;
import org.uma.jmetal.util.checking.Check;
import org.uma.jmetal.util.pseudorandom.BoundedRandomGenerator;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;
import org.uma.jmetal.util.pseudorandom.RandomGenerator;

/**
 * Mutation operator that moves a random exam to a random feasible room within the same timeslot
 * @author aadatti
 * @param <T> Solution type 
 */
public class RoomMove<T> implements MutationOperator<IntegerMatrixSolution<T>>{    
  
    class localSearchComparator implements Comparator<IntegerMatrixSolution> 
    {
        @Override
            public int compare(IntegerMatrixSolution a, IntegerMatrixSolution b) 
            {
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
    
    Map<Integer, ArrayList<Integer>> availableRooms;
    int[] roomCapacities;
    int[] examEnrollments;
    
    public RoomMove(double mutationProbability, int[] roomCapacities, 
            int[] examEnrollments, IntegerMatrixProblem problem) {        
        this(mutationProbability,
                () -> JMetalRandom.getInstance().nextDouble(), 
                (a, b) -> JMetalRandom.getInstance().nextInt(a, b), 
                roomCapacities, examEnrollments, problem);
    }

    /** Constructor
     * @param mutationProbability
     * @param randomGenerator
     * @param availableRooms
     * @param problem */
    public RoomMove(double mutationProbability, RandomGenerator<Double> randomGenerator, 
            int[] roomCapacities, int[] examEnrollments,IntegerMatrixProblem problem) 
    {
        this(mutationProbability, randomGenerator, 
                BoundedRandomGenerator.fromDoubleToInteger(randomGenerator), 
                roomCapacities, examEnrollments, problem);
    }

  /** Constructor
     * @param mutationProbability
     * @param mutationRandomGenerator
     * @param positionRandomGenerator
     * @param availableRooms
     * @param problem */
    public RoomMove(double mutationProbability, RandomGenerator<Double> mutationRandomGenerator, 
            BoundedRandomGenerator<Integer> positionRandomGenerator, int[] roomCapacities, 
            int[] examEnrollments, IntegerMatrixProblem problem) 
    {
        Check.probabilityIsValid(mutationProbability);
        this.mutationProbability = mutationProbability;
        this.mutationRandomGenerator = mutationRandomGenerator;
        this.positionRandomGenerator = positionRandomGenerator;             
        this.roomCapacities = roomCapacities;
        this.examEnrollments = examEnrollments;
        this.problem = problem;
        this.numberOfImprovements = 0;
        this.improvementRounds=1000;
        this.comparator = new localSearchComparator();
    }

    @Override
    public double getMutationProbability() 
    {
        return mutationProbability;
    }

    public void setMutationProbability(double mutationProbability) 
    {
        this.mutationProbability = mutationProbability;
    }
    
    @Override
    public IntegerMatrixSolution<T> execute(IntegerMatrixSolution<T> solution) 
    {
        Check.isNotNull(solution);

        int best;
        evaluations = 0;

        int rounds = improvementRounds;

        int i = 0;
        while (i < rounds) {
          IntegerMatrixSolution mutatedSolution = doMutation((IntegerMatrixSolution)solution.copy());

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
        //PICK A RANDOM EXAM AND A RANDOM ROOM        
        int solutionLength = solution.getNumberOfVariables();        
                
        if ((solutionLength != 0) && (solutionLength != 1)) {
            if (mutationRandomGenerator.getRandomValue() < mutationProbability) {
                int randExamIndex = positionRandomGenerator.getRandomValue(0, solutionLength - 1);                  
//                System.out.println("Changing Room for Exam "+randExamIndex+": "+solution.getVariable(randExamIndex)+"");
                ArrayList<Integer> availableRooms = getFreeRooms(solution,randExamIndex);
                ArrayList roomsTried =  new ArrayList();
                int randRoomIndex = positionRandomGenerator.getRandomValue(0, availableRooms.size()-1);
                int room = availableRooms.get(randRoomIndex);
//                System.out.println("Room "+room+" selected out of "+availableRooms.size()+" rooms.");
                int attempts=0;
                boolean success=true;
                while (examEnrollments[randExamIndex] > roomCapacities[randRoomIndex]){     
                    if(attempts>availableRooms.size()){                        
                        success=false;
                        break;
                    }
                    if(!roomsTried.contains(this)){
                        roomsTried.add(room);
                        attempts++;
                    }                    
                    randRoomIndex = positionRandomGenerator.getRandomValue(0, availableRooms.size()-1);         
                }                                                                                                            
                if(success){
                    ArrayList<Integer> exam = (ArrayList)solution.getVariable(randExamIndex);                                
                
                    exam.set(getTimeslot(exam), availableRooms.get(randRoomIndex));
//                    System.out.println("Room changed succesfully: "+solution.getVariable(randExamIndex));
                    //REPLACE BACK INTO SOLUTION
                    solution.setVariable(randExamIndex, (T)exam);
                }
                else{
//                    System.out.println("Can't change room");
                }                
            }
        }        
        return (IntegerMatrixSolution) solution.copy();
    }

    ArrayList<Integer> getFreeRooms(IntegerMatrixSolution solution, int examIndex){
       ArrayList<Integer> freeRooms = new ArrayList();
       
       for(int i=0; i<solution.getNumberOfVariables();i++){
           
           if(!(getTimeslot((ArrayList)solution.getVariable(examIndex))==
                   getTimeslot((ArrayList)solution.getVariable(i)))){
               int room  = getRoom((ArrayList)solution.getVariable(i));
               if(freeRooms.contains(room)){
                    continue;
               }
               freeRooms.add(room);
           }
       }
//        System.out.println("Free Rooms:"+freeRooms.toString());
       return freeRooms;
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