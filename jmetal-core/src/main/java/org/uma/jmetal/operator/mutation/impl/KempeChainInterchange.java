package org.uma.jmetal.operator.mutation.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Random;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.solution.integermatrixsolution.IntegerMatrixSolution;
import org.uma.jmetal.solution.integermatrixsolution.impl.DefaultIntegerMatrixSolution;
import org.uma.jmetal.util.checking.Check;
import org.uma.jmetal.util.pseudorandom.BoundedRandomGenerator;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;
import org.uma.jmetal.util.pseudorandom.RandomGenerator;

/**
 * This class implements a Kempe Chain Interchange mutation. The solution type of the solution must be IntegerMatrix. 
 * @author aadatti <aadatti.cs@buk.edu.ng>
 */
   
@SuppressWarnings("serial")
public class KempeChainInterchange<T> implements MutationOperator<IntegerMatrixSolution<T>>
{
  private double mutationProbability;
  private RandomGenerator<Double> mutationRandomGenerator;
  private BoundedRandomGenerator<Integer> positionRandomGenerator;
  private int numberOfExams;
  
  int[][]conflictMatrix;

  /** Constructor */
  public KempeChainInterchange(double mutationProbability, int[][] conMat) {
    this(
        mutationProbability,
        () -> JMetalRandom.getInstance().nextDouble(),
        (a, b) -> JMetalRandom.getInstance().nextInt(a, b), conMat);
  }

  /** Constructor */
  public KempeChainInterchange(
      double mutationProbability, RandomGenerator<Double> randomGenerator, int[][] conMat) {
    this(
        mutationProbability,
        randomGenerator,
        BoundedRandomGenerator.fromDoubleToInteger(randomGenerator),conMat);
  }

  /** Constructor */
  public KempeChainInterchange(
      double mutationProbability,
      RandomGenerator<Double> mutationRandomGenerator,
      BoundedRandomGenerator<Integer> positionRandomGenerator, int[][] conMat) {
    Check.probabilityIsValid(mutationProbability);
    this.mutationProbability = mutationProbability;
    this.mutationRandomGenerator = mutationRandomGenerator;
    this.positionRandomGenerator = positionRandomGenerator;
    this.conflictMatrix = conMat;
  }

  /* Getters */
  @Override
  public double getMutationProbability() {
    return mutationProbability;
  }

  /* Setters */
  public void setMutationProbability(double mutationProbability) {
    this.mutationProbability = mutationProbability;
  }

  /* Execute() method */
  @Override
  public IntegerMatrixSolution<T> execute(IntegerMatrixSolution<T> solution) {
    Check.isNotNull(solution);
    doMutation(solution);
     
    return solution;
  }

  /** Performs the operation */
  public void doMutation(IntegerMatrixSolution<T> solution) {
   
    ArrayList<ArrayList<Integer>> sol = new ArrayList<>();
    
    for(int i=0;i<solution.getNumberOfVariables();i++)
    {
        sol.add(new ArrayList((ArrayList)solution.getVariable(i)));
    }
      
    int permutationLength;
    permutationLength = solution.getNumberOfVariables();

    if ((permutationLength != 0) && (permutationLength != 1)) {
      if (mutationRandomGenerator.getRandomValue() < mutationProbability) {
        int pos1 = positionRandomGenerator.getRandomValue(0, permutationLength - 1);
        int pos2 = positionRandomGenerator.getRandomValue(0, permutationLength - 1);

        while (pos1 == pos2) {
          if (pos1 == (permutationLength - 1))
            pos2 = positionRandomGenerator.getRandomValue(0, permutationLength - 2);
          else pos2 = positionRandomGenerator.getRandomValue(pos1, permutationLength - 1);
        }
   
//-------------------------------<original>------------------------------------>        
//        
//        T temp = solution.getVariable(pos1);
//        solution.setVariable(pos1, solution.getVariable(pos2));
//        solution.setVariable(pos2, temp);
//-------------------------------<original>------------------------------------>

//------------------------------<kempechain>----------------------------------->  
//        System.out.println("Solution before kinterchange:"+solution.getVariables());
        
        int timeslot1,timeslot2;
//        Set<ArrayList<Integer>> ti = new HashSet<>();
//        Set<ArrayList<Integer>> tj = new HashSet<>();
        ArrayList arrayTi = new ArrayList();
        ArrayList arrayTj = new ArrayList();        
        Set<ArrayList<Integer>> K = new HashSet<>(); 
        
        Map<ArrayList,Integer> examIndices = new HashMap<>();
        
        ArrayList<Integer> exam1 = new ArrayList<>((ArrayList)sol.get(pos1));
          

        for(timeslot1=0;timeslot1<exam1.size();timeslot1++)
        {
            if(exam1.get(timeslot1)!=0)break;            
        }
//        System.out.println("rand exam1 "+exam1+" timeslot1= "+timeslot1);
         
        ArrayList<Integer> exam2 = new ArrayList<>((ArrayList)sol.get(pos2));

        for(timeslot2=0;timeslot2<exam2.size();timeslot2++)
        {
            if(exam2.get(timeslot2)!=0)break;
        }
//        System.out.println("rand exam2 "+exam2+" timeslot2= "+timeslot2);
//        System.out.println("solution (before kinterchange)"+solution.getVariables());
//        System.out.println("sol (before kinterchange)"+sol);
        
        for(int i=0;i<sol.size();i++)
        {
            while(timeslot1==timeslot2)
            {
                Random rand = new Random();
                pos2 = rand.nextInt(sol.get(pos2).size());
                
                exam2 = new ArrayList<>((ArrayList)sol.get(pos2));

                for(timeslot2=0;timeslot2<exam2.size();timeslot2++)
                {
                    if(exam2.get(timeslot2)!=0)break;
                }
            }
            
            ArrayList<Integer> currExam = new ArrayList<>(sol.get(i));           
            if(currExam.get(timeslot1)!=0)
            {
                arrayTi.add(new ArrayList(currExam));
                examIndices.put(currExam, i);
            }
            if(currExam.get(timeslot2)!=0)
            {
                arrayTj.add(new ArrayList(currExam));
                examIndices.put(currExam, i);
            }
        } 
          
                                  
        ArrayList tmpTi = new ArrayList(arrayTi);
        ArrayList tmpTj = new ArrayList(arrayTj);
        
//        System.out.println("\n\nNEW CANDIDATE SOLUTION"+sol);
//        System.out.print("Prime @ position ("+pos1+") = "+sol.get(pos1)+" b4 swap");
//        int oRoom = sol.get(pos1).get(timeslot1);
////        System.out.println("Moving prime candidate exam"+sol.get(pos1)+" in room "+oRoom+" from timeslot "+timeslot1+" to "+timeslot2);
//        sol.get(pos1).set(timeslot1, 0);
//        sol.get(pos1).set(timeslot2, oRoom);
////        System.out.println(" and after swap = "+sol.get(pos1));
//        
////          System.out.println("arrayTj = "+arrayTj);  
////          System.out.println(" with indices "+examIndices);
//          boolean needSecondSwap=false;
//        for(int j=0;j<arrayTj.size();j++)
//        {
//            if(conflictMatrix[pos1][examIndices.get(arrayTj.get(j))]!=0)
//            {   
////                System.out.println("conflictMatrix["+pos1+"]["+examIndices.get(arrayTj.get(j))+"]="+conflictMatrix[pos1][examIndices.get(arrayTj.get(j))]);
////                System.out.print("Conflict found between prime "+sol.get(pos1)+" @ pos = "+pos1);
////                System.out.println(" and arrayTj("+examIndices.get(arrayTj.get(j))+"): "+arrayTj.get(j));               
//                int oldRoom = sol.get(examIndices.get(arrayTj.get(j))).get(timeslot2);
////                System.out.println("Now moving "+sol.get(examIndices.get(arrayTj.get(j)))+" in room "+oldRoom+" from timeslot "+timeslot2+" to "+timeslot1);
//                if(oldRoom!=0)
//                {
//                    sol.get(examIndices.get(arrayTj.get(j))).set(timeslot2, 0);
//                    sol.get(examIndices.get(arrayTj.get(j))).set(timeslot1, oldRoom);
//                }
////                System.out.println("Moved. "+sol.get(examIndices.get(arrayTj.get(j))));
//                needSecondSwap=true;
////                System.out.println("sol.get(examIndices.get("+examIndices.get(arrayTj.get(j))+")="+sol.get(examIndices.get(arrayTj.get(j))));
//            }
//        } 
//        
////        System.out.println("Now checking arrayTi for conflict with exams moved from arrayTj due to prime...");
//        if(needSecondSwap)
//        {
////            System.out.println("INSIDE 2ND SWAP");
//            for(int i=0;i<arrayTi.size();i++)
//            {
//                if(examIndices.get(arrayTi.get(i))==pos1)continue;
//                for(int j=0;j<arrayTj.size();j++)
//                {
//                    if(conflictMatrix[examIndices.get(arrayTi.get(i))][examIndices.get(arrayTj.get(j))]!=0)
//                    {
////                        System.out.println("arrayTi = (exams with timeslot1 =("+timeslot1+"): "+arrayTi);          
////                        System.out.println("arrayTj = (exams with timeslot2 =("+timeslot2+"): "+arrayTj);
////                        System.out.println(" with indices "+examIndices);
//                        
//                        int oldRoom = sol.get(examIndices.get(arrayTi.get(i))).get(timeslot1);
////                        System.out.println("Moving "+sol.get(examIndices.get(arrayTi.get(i)))+" in room "+oldRoom+" from timeslot "+timeslot1+" to "+timeslot2);
//                        if(oldRoom!=0)
//                        {                        
//                            sol.get(examIndices.get(arrayTi.get(i))).set(timeslot1, 0);
//                            sol.get(examIndices.get(arrayTi.get(i))).set(timeslot2, oldRoom);
//                        }
//    //                    System.out.println("Moved. "+sol.get(examIndices.get(arrayTi.get(i))));
//    //                    System.out.println("sol.get(examIndices.get("+examIndices.get(arrayTi.get(i))+")="+sol.get(examIndices.get(arrayTi.get(i))));
//    //                    tmpTi.add(arrayTi.get(i));//System.out.println("adding "+arrayTi.get(i)+" to K");
//    //                    tmpTj.add(arrayTj.get(j));//System.out.println("adding "+arrayTj.get(j)+" to K");
//                    }
//                }            
//            }
//        }
        
        K.addAll(tmpTi);
        K.addAll(tmpTj);
//        System.out.println("K {"+K+"}");
        Set ti_complement_K = new HashSet<>(tmpTi);
        Set tj_intersection_K = new HashSet<>(tmpTj);
        ti_complement_K.removeAll(K);
        tj_intersection_K.retainAll(K);
        Set newTi = new HashSet<>(ti_complement_K);
        newTi.addAll(tj_intersection_K);    
//        System.out.println("newTi"+newTi);
        
        Set tj_complement_K = new HashSet<>(tmpTj);
        Set ti_intersection_K = new HashSet<>(tmpTi);
        tj_complement_K.removeAll(K);
        ti_intersection_K.retainAll(K);
        Set newTj = new HashSet<>(tj_complement_K);
        newTj.addAll(ti_intersection_K);
//        System.out.println("newTj"+newTj);
        
        ArrayList newTiArray = new ArrayList();
        newTiArray.addAll(newTi);
        for(int i=0;i<newTiArray.size();i++)
        {
            ArrayList<Integer> currExam = new ArrayList((ArrayList)newTiArray.get(i));
            int index = examIndices.get(currExam);
            if(currExam.get(timeslot2)!=0)
            { 
                int oldRoom = currExam.get(timeslot2);
                currExam.set(timeslot2, 0);
                currExam.set(timeslot1, oldRoom);
            }
            sol.set(index, currExam);
//            sol.add(currExam);
//            System.out.println("tjExams="+tjExams+"\ti="+i);
//            int index = tjExams.get(i);
//            sol.set(index, currExam);
        }
        
        ArrayList newTjArray = new ArrayList();
        newTjArray.addAll(newTj);
        for(int i=0;i<newTjArray.size();i++)
        {
            ArrayList<Integer> currExam = new ArrayList((ArrayList)newTjArray.get(i));
            int index = examIndices.get(currExam);
            if(currExam.get(timeslot1)!=0)
            {   
                int oldRoom = currExam.get(timeslot1);
                currExam.set(timeslot1, 0);
                currExam.set(timeslot2, oldRoom);
            }
            sol.set(index, currExam);
            sol.add(currExam);
//            System.out.println("tiExams="+tiExams+"\ti="+i);
            int index = tiExams.get(i);
            sol.set(index, currExam);            
        }
          System.out.println("sol.size()="+sol.size());
          System.out.println("solution.size()="+solution.getNumberOfVariables());
          
        for(int i=0;i<solution.getNumberOfVariables();i++)
        {
            solution.setVariable(i, (T)sol.get(i));
//            sol.add(new ArrayList((ArrayList)solution.getVariable(i)));
        }
        
//        System.out.println("solution (after kinterchange)"+solution.getVariables());
//        System.out.println("sol (after kinterchange)"+sol);
//          System.out.println("Now checking for conflicts");
        for(int i=0; i<permutationLength-1;i++)
        {                      
            for(int j=i+1; j<permutationLength;j++)
            {
                if(i==j)continue;
                
                int slot1=0,slot2=0;                                  
                
                ArrayList<Integer> x = (ArrayList)solution.getVariable(i);
                while(x.get(slot1)==0)slot1++;            
                
                ArrayList<Integer> y = (ArrayList)solution.getVariable(j);
                while(y.get(slot2)==0)slot2++; 
      
                
                if(conflictMatrix[i][j]!=0)
                {                    
                    if(slot1==slot2)
                    {                      
                        System.out.println("In Solution: "+solution.getVariables()+",");                        
                        System.out.println("Exam ("+i+")"+x+" @ timeslot "+slot1+" conflicts with exam ("+j+")"+y+" @ timeslot "+slot2);
                    }
                }
            }                                                       
        }

//   System.out.println("Solution after KInterchange:"+solution.getVariables());
//------------------------------<kempechain>----------------------------------->              
      }
    }   
  }
}