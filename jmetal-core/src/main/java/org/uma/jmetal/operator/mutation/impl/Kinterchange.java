package org.uma.jmetal.operator.mutation.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.solution.integermatrixsolution.IntegerMatrixSolution;
import org.uma.jmetal.util.checking.Check;
import org.uma.jmetal.util.pseudorandom.BoundedRandomGenerator;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;
import org.uma.jmetal.util.pseudorandom.RandomGenerator;

/**
 *
 * @author aadatti
 */
public class Kinterchange <T> implements MutationOperator<IntegerMatrixSolution<T>>
{
    private double mutationProbability;
    private RandomGenerator<Double> mutationRandomGenerator;
    private BoundedRandomGenerator<Integer> positionRandomGenerator;
    int[][]conflictMatrix;
    
    public Kinterchange(double mutationProbability, int[][] conMat) 
    {
        this(mutationProbability,() -> JMetalRandom.getInstance().nextDouble(), (a, b) -> JMetalRandom.getInstance().nextInt(a, b), conMat);
    }

    /** Constructor */
    public Kinterchange(double mutationProbability, RandomGenerator<Double> randomGenerator, int[][] conMat) 
    {
        this(mutationProbability, randomGenerator, BoundedRandomGenerator.fromDoubleToInteger(randomGenerator),conMat);
    }

  /** Constructor */
    public Kinterchange(double mutationProbability, RandomGenerator<Double> mutationRandomGenerator, BoundedRandomGenerator<Integer> positionRandomGenerator, int[][] conMat) 
    {
        Check.probabilityIsValid(mutationProbability);
        this.mutationProbability = mutationProbability;
        this.mutationRandomGenerator = mutationRandomGenerator;
        this.positionRandomGenerator = positionRandomGenerator;
        this.conflictMatrix = conMat;        
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
        doMutation(solution);    
        return solution;
    }
    
    public void doMutation(IntegerMatrixSolution<T> solution) 
    {
        //PICK TWO RANDOM EXAMS TO SWAP
        int solutionLength = solution.getNumberOfVariables();
        Map<Integer, ArrayList<Integer>> solutionMap = new HashMap<>();
        
        for(int i =0;i<solutionLength;i++)
        {
            solutionMap.put(i, new ArrayList((ArrayList)solution.getVariable(i)));
        }
//        System.out.println("solutionMap before kinterchange:"+solutionMap);
        
        
        if ((solutionLength != 0) && (solutionLength != 1)) 
        {
            if (mutationRandomGenerator.getRandomValue() < mutationProbability) 
            {
                int pos1 = positionRandomGenerator.getRandomValue(0, solutionLength - 1);
                int pos2 = positionRandomGenerator.getRandomValue(0, solutionLength - 1);

                while (pos1 == pos2) 
                {
                    if (pos1 == (solutionLength - 1))
                        pos2 = positionRandomGenerator.getRandomValue(0, solutionLength - 2);
                    else 
                        pos2 = positionRandomGenerator.getRandomValue(pos1, solutionLength - 1);
                }
            
                ArrayList exam1 = solutionMap.get(pos1);//System.out.println("exam1: "+exam1);
                ArrayList exam2 = solutionMap.get(pos2);//System.out.println("exam2: "+exam2);
        
                //COLLECT ALL EXAMS HAVING THE SAME TIMESLOT WITH EACH OF THE TWO RANDOMLY SELECTED EXAMS
                Map<Integer, ArrayList<Integer>> mapTi = new HashMap<>();
                Map<Integer, ArrayList<Integer>> mapTj = new HashMap<>();
                
                int timeslot1,timeslot2;                                
                
                for(timeslot1=0;timeslot1<exam1.size();timeslot1++)
                {
                    if((int)exam1.get(timeslot1)!=0)break;                           
                }//System.out.println("timeslot1="+timeslot1);
                
                for(timeslot2=0;timeslot2<exam2.size();timeslot2++)
                {
                    if((int)exam2.get(timeslot2)!=0)break;                           
                }//System.out.println("timeslot2="+timeslot2);
                
                while(timeslot1==timeslot2)
                {
                    Random rand = new Random();
                    pos2 = rand.nextInt(solutionMap.get(0).size());

                    exam2 = solutionMap.get(pos2);

                    for(timeslot2=0;timeslot2<exam2.size();timeslot2++)
                    {
                        if((int)exam2.get(timeslot2)!=0)break;
                    }
                }//System.out.println("re:timeslot2="+timeslot2);
                
                for(int i =0; i<solutionMap.size();i++)
                {
                    if(solutionMap.get(i).get(timeslot1)!=0)
                    {
                        mapTi.put(i, solutionMap.get(i));
                    }
                    else if(solutionMap.get(i).get(timeslot2)!=0)
                    {
                        mapTj.put(i, solutionMap.get(i));
                    }
                }//System.out.println("mapTi: "+mapTi+"\nmapTj: "+mapTj);
                
                //FROM ABOVE, COLLECT THE ONES HAVING CONFLICTS
                Map<Integer, ArrayList<Integer>> newMapTi = new HashMap<>();
                Map<Integer, ArrayList<Integer>> newMapTj = new HashMap<>();
                
                ArrayList tiKeys = new ArrayList(mapTi.keySet());//System.out.println("tiKeys:"+tiKeys);
                ArrayList tjKeys = new ArrayList(mapTj.keySet());//System.out.println("tjKeys:"+tjKeys);
                
                for(int i =0; i<tiKeys.size();i++)
                {
                    for(int j=0; j<tjKeys.size();j++)
                    {
                        int x = (int)tiKeys.get(i);
                        int y = (int)tjKeys.get(j);
                        
                        if(conflictMatrix[x][y]!=0)
                        {
                            newMapTi.put(x, mapTi.get(x));
                            newMapTj.put(y, mapTj.get(y));
                        }
                    }
                }//System.out.println("newMapTi:"+newMapTi+"\nnewMapTj:"+newMapTj);
        
                //KINTERCHANGE 
                Set K = new HashSet();
                
                K.addAll(newMapTi.keySet());
                K.addAll(newMapTj.keySet());
               //System.out.println("K:"+K);
              
                Set ti_complement_K = new HashSet<>(newMapTi.keySet());
                Set tj_intersection_K = new HashSet<>(newMapTj.keySet());
                ti_complement_K.removeAll(K);
                tj_intersection_K.retainAll(K);
                Set newTi = new HashSet<>(ti_complement_K);
                newTi.addAll(tj_intersection_K);    
                //System.out.println("newTi"+newTi);

                Set tj_complement_K = new HashSet<>(newMapTj.keySet());
                Set ti_intersection_K = new HashSet<>(newMapTi.keySet());
                tj_complement_K.removeAll(K);
                ti_intersection_K.retainAll(K);
                Set newTj = new HashSet<>(tj_complement_K);
                newTj.addAll(ti_intersection_K);
//                System.out.println("newTj"+newTj);
                
                //...AND SWAP TIMESLOTS
                ArrayList tiArray = new ArrayList(newTi);
                ArrayList tjArray = new ArrayList(newTj);
                
                for(int i=0;i<tiArray.size();i++)
                {
                    int x = (int)tiArray.get(i);
                    int oldRoom =  solutionMap.get(x).get(timeslot2);    
                    solutionMap.get(x).set(timeslot2, 0);
                    solutionMap.get(x).set(timeslot1, oldRoom);
//                    System.out.println("roomSwap"+solutionMap.get(x)); 
                }
                
                for(int i=0;i<tjArray.size();i++)
                {
                    int x = (int)tjArray.get(i);
                    int oldRoom =  solutionMap.get(x).get(timeslot1);
                    solutionMap.get(x).set(timeslot1, 0);
                    solutionMap.get(x).set(timeslot2, oldRoom);
//                    System.out.println("roomSwap"+solutionMap.get(x));
                }
                
                //REPLACE BACK INTO SOLUTION
                
                for(int i=0;i<solutionLength;i++)
                {
                    solution.setVariable(i, (T)solutionMap.get(i));
                }
//                System.out.println("solutionMap after kinterchange:"+solutionMap);
//                System.out.println("solution after kinterchage:"+solution.getVariables());
                    
                //CROSSCHECK CONFLICT 
                
            }
        }
    }    
}