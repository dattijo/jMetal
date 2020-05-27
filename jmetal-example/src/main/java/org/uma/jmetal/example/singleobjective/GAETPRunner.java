package org.uma.jmetal.example.singleobjective;

import java.util.ArrayList;
import java.util.List;

import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.singleobjective.geneticalgorithm.GeneticAlgorithmBuilder;

import org.uma.jmetal.example.AlgorithmRunner;

import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.NullCrossover;

import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.Kinterchange;

import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.operator.selection.impl.BinaryTournamentSelection;

import org.uma.jmetal.problem.integermatrixproblem.IntegerMatrixProblem;
import org.uma.jmetal.problem.singleobjective.ETP;

import org.uma.jmetal.solution.integermatrixsolution.IntegerMatrixSolution;

import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.comparator.RankingAndCrowdingDistanceComparator;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;

/**
 * @author aadatti
 */
public class GAETPRunner 
{          
            
    public static void main(String[] args) throws Exception 
    {
        IntegerMatrixProblem problem;
        Algorithm<IntegerMatrixSolution<Integer>> algorithm;
        CrossoverOperator<IntegerMatrixSolution<Integer>> crossover;
        MutationOperator<IntegerMatrixSolution<Integer>> mutation;
        SelectionOperator<List<IntegerMatrixSolution<Integer>>, IntegerMatrixSolution<Integer>> selection;                              
    
        problem = new ETP("C:/Users/PhDLab/Documents/NetBeansProjects/examTimetableDataReader/exam_comp_set44.exam");
    
        int[][]conflictMatrix =  problem.getConflictMatrix();
    
        crossover = new NullCrossover();    

        double mutationProbability = 0.9 ;   

        mutation = new Kinterchange(mutationProbability, conflictMatrix, problem);          

        selection = new BinaryTournamentSelection<IntegerMatrixSolution<Integer>>(
                    new RankingAndCrowdingDistanceComparator<IntegerMatrixSolution<Integer>>());            
        
    
        algorithm = new GeneticAlgorithmBuilder<>(problem, crossover, mutation)
            .setPopulationSize(100)      //.setPopulationSize(100)
            .setMaxEvaluations(1000)      //.setMaxEvaluations(250000) 
            .setSelectionOperator(selection)
            .setVariant(GeneticAlgorithmBuilder.GeneticAlgorithmVariant.GENERATIONAL)
            .build() ; 

         AlgorithmRunner algorithmRunner = new AlgorithmRunner.Executor(algorithm).execute() ;
    
        IntegerMatrixSolution<Integer> solution = algorithm.getResult() ;
        
        List<IntegerMatrixSolution<Integer>> population = new ArrayList<>(1) ;

        population.add(solution) ;
        
        long computingTime = algorithmRunner.getComputingTime() ;

        new SolutionListOutput(population)
            .setVarFileOutputContext(new DefaultFileOutputContext("VAR.tsv"))
            .setFunFileOutputContext(new DefaultFileOutputContext("FUN.tsv"))
            .print();

        JMetalLogger.logger.info("Total execution time: " + computingTime + "ms");
        JMetalLogger.logger.info("Objectives values have been written to file FUN.tsv");
        JMetalLogger.logger.info("Variables values have been written to file VAR.tsv");
    }
}