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
 *
 * @author aadatti
 */
public class RoomSwap<T> implements MutationOperator<IntegerMatrixSolution<T>> {
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
    boolean cannotMutate=false;
    
    public RoomSwap(double mutationProbability, int[] roomCapacities, 
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
    public RoomSwap(double mutationProbability, RandomGenerator<Double> randomGenerator, 
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
    public RoomSwap(double mutationProbability, RandomGenerator<Double> mutationRandomGenerator, 
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
          if(cannotMutate)break;
          System.out.println("i="+i);
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
                int randExamIndex1 = positionRandomGenerator.getRandomValue(0, solutionLength - 1);
                int randExamIndex2 = positionRandomGenerator.getRandomValue(0, solutionLength - 1);
                
                while (randExamIndex1 == randExamIndex2) {
                    if (randExamIndex1 == (solutionLength - 1)) {
                        randExamIndex2 = positionRandomGenerator.getRandomValue(0, solutionLength - 2);
                    } else {
                        randExamIndex2 = positionRandomGenerator.getRandomValue(randExamIndex1, solutionLength - 1);
                    }
                }
                int room1 = getRoom((ArrayList)solution.getVariable(randExamIndex1));
                int room2 = getRoom((ArrayList)solution.getVariable(randExamIndex2));
                int timeslot1 = getTimeslot((ArrayList)solution.getVariable(randExamIndex1));
                int timeslot2 = getTimeslot((ArrayList)solution.getVariable(randExamIndex2));
                boolean feasible=true;                
                if(examEnrollments[randExamIndex1] 
                        <= roomCapacities[room2-1]
                        &&examEnrollments[randExamIndex2] 
                        <= roomCapacities[room1-1]){
                    
                    for(int i=0;i<solution.getNumberOfVariables();i++){
                        int tmpTimeslot = getTimeslot((ArrayList)solution.getVariable(i));
                        int tmpRoom = getRoom((ArrayList)solution.getVariable(i));
                        if(tmpTimeslot==timeslot1&&tmpRoom==room2){
                            feasible=false;
                            break;
                        }
                        if(tmpTimeslot==timeslot2&&tmpRoom==room1){
                            feasible=false;
                            break;
                        }
                    }                    
                }
                else{
                    feasible=false;
                }
                if(feasible){
                    System.out.println("Selected Exams:");
                    System.out.println("\tExam 1 = "+randExamIndex1+". Timeslot = "+timeslot1+". Room = "+room1);
                    System.out.println("\tExam 2 = "+randExamIndex2+". Timeslot = "+timeslot2+". Room = "+room2);
                    ArrayList<Integer> exam1 = (ArrayList)solution.getVariable(randExamIndex1);
                    ArrayList<Integer> exam2 = (ArrayList)solution.getVariable(randExamIndex2);  
                    exam1.set(timeslot1, room2);
                    exam2.set(timeslot2, room1);
                    solution.setVariable(randExamIndex1, (T)exam1);
                    solution.setVariable(randExamIndex2, (T)exam2); 
                    System.out.println("New exam1 = "+solution.getVariable(randExamIndex1));
                    System.out.println("New exam2 = "+solution.getVariable(randExamIndex2));
                    cannotMutate=false;
                }   
                else{
                    cannotMutate=true;
                }
            }
        }        
        return (IntegerMatrixSolution) solution.copy();
    }

//    ArrayList<Integer> getFreeRooms(IntegerMatrixSolution solution, int examIndex){
//       ArrayList<Integer> freeRooms = new ArrayList();
//       
//       for(int i=0; i<solution.getNumberOfVariables();i++){
//           
//           if(!(getTimeslot((ArrayList)solution.getVariable(examIndex))==
//                   getTimeslot((ArrayList)solution.getVariable(i)))){
//               int room  = getRoom((ArrayList)solution.getVariable(i));
//               if(freeRooms.contains(room)){
//                    continue;
//               }
//               freeRooms.add(room);
//           }
//       }
////        System.out.println("Free Rooms:"+freeRooms.toString());
//       return freeRooms;
//    }    
    
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
