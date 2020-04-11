package org.uma.jmetal.operator.mutation.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.solution.integermatrixsolution.IntegerMatrixSolution;
//import org.uma.jmetal.solution.permutationsolution.PermutationSolution;
import org.uma.jmetal.util.checking.Check;
import org.uma.jmetal.util.pseudorandom.BoundedRandomGenerator;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;
import org.uma.jmetal.util.pseudorandom.RandomGenerator;

/**
 *
 * @author PhDLab
 */
/**
 * This class implements a Kempe Chain Interchange mutation. The solution type of the solution must be IntegerMatrix.
 *
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 * @author Juan J. Durillo
 */
   
@SuppressWarnings("serial")
public class KempeChainInterchange<T> implements MutationOperator<IntegerMatrixSolution<T>>
{
  private double mutationProbability;
  private RandomGenerator<Double> mutationRandomGenerator;
  private BoundedRandomGenerator<Integer> positionRandomGenerator;
  private int numberOfExams;

  /** Constructor */
  public KempeChainInterchange(double mutationProbability) {
    this(
        mutationProbability,
        () -> JMetalRandom.getInstance().nextDouble(),
        (a, b) -> JMetalRandom.getInstance().nextInt(a, b));
  }

  /** Constructor */
  public KempeChainInterchange(
      double mutationProbability, RandomGenerator<Double> randomGenerator) {
    this(
        mutationProbability,
        randomGenerator,
        BoundedRandomGenerator.fromDoubleToInteger(randomGenerator));
  }

  /** Constructor */
  public KempeChainInterchange(
      double mutationProbability,
      RandomGenerator<Double> mutationRandomGenerator,
      BoundedRandomGenerator<Integer> positionRandomGenerator) {
    Check.probabilityIsValid(mutationProbability);
    this.mutationProbability = mutationProbability;
    this.mutationRandomGenerator = mutationRandomGenerator;
    this.positionRandomGenerator = positionRandomGenerator;
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
//        System.out.println("Solution before KInterchange:"+solution.getVariables());
        numberOfExams=solution.getNumberOfVariables();
        int timeslot1,timeslot2;
        Set<ArrayList<Integer>> ti = new HashSet<>();
        Set<ArrayList<Integer>> tj = new HashSet<>();
        
        Set<ArrayList<Integer>> K = new HashSet<>();  
         
        ArrayList<Integer> tiExams = new ArrayList<>();
        ArrayList<Integer> tjExams = new ArrayList<>();
        
        ArrayList<Integer> exam1 = new ArrayList<>((ArrayList)solution.getVariable(pos1));//ti

        for(timeslot1=0;timeslot1<exam1.size();timeslot1++)
        {
            if(exam1.get(timeslot1)!=0)break;            
        }
         
        ArrayList<Integer> exam2 = new ArrayList<>((ArrayList)solution.getVariable(pos2));//tj

        for(timeslot2=0;timeslot2<exam2.size();timeslot2++)
        {
            if(exam2.get(timeslot2)!=0)break;
        } 
        
        for(int i=0;i<numberOfExams;i++)
        {
            if(timeslot1==timeslot2)break;
            ArrayList<Integer> currExam = new ArrayList<>((ArrayList)solution.getVariable(i));           
            if(currExam.get(timeslot1)!=0)
            {
                ti.add((ArrayList)currExam.clone());
                tiExams.add(i);
                solution.setVariable(i, (T)currExam);
            }
            if(currExam.get(timeslot2)!=0)
            {
                tj.add((ArrayList)currExam.clone());
                tjExams.add(i);
                solution.setVariable(i, (T)currExam);
            }
        }        
        
        K.addAll(ti);
        K.addAll(tj);
        Set ti_complement_K = new HashSet<>(ti);
        Set tj_intersection_K = new HashSet<>(tj);
        ti_complement_K.removeAll(K);
        tj_intersection_K.retainAll(K);
        Set newTi = new HashSet<>(ti_complement_K);
        newTi.addAll(tj_intersection_K);        
        
        Set tj_complement_K = new HashSet<>(tj);
        Set ti_intersection_K = new HashSet<>(ti);
        tj_complement_K.removeAll(K);
        ti_intersection_K.retainAll(K);
        Set newTj = new HashSet<>(tj_complement_K);
        newTj.addAll(ti_intersection_K);
        
        ArrayList newTiArray = new ArrayList();
        newTiArray.addAll(newTi);
        for(int i=0;i<newTiArray.size();i++)
        {
            ArrayList<Integer> currExam = new ArrayList((ArrayList)newTiArray.get(i));
            int oldRoom = currExam.get(timeslot2);
            currExam.set(timeslot2, 0);
            currExam.set(timeslot1, oldRoom);
            solution.setVariable(tjExams.get(i), (T)currExam);
        }
        
        ArrayList newTjArray = new ArrayList();
        newTjArray.addAll(newTj);
        for(int i=0;i<newTj.size();i++)
        {
            ArrayList<Integer> currExam = new ArrayList((ArrayList)newTjArray.get(i));
            int oldRoom = currExam.get(timeslot1);
            currExam.set(timeslot1, 0);
            currExam.set(timeslot2, oldRoom);
            solution.setVariable(tiExams.get(i), (T)currExam);            
        }

//        System.out.println("Solution after KInterchange:"+solution.getVariables());
//------------------------------<kempechain>----------------------------------->              
      }
    }   
  }
}