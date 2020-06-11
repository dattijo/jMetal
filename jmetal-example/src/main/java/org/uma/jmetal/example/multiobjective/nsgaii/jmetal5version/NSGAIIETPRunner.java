package org.uma.jmetal.example.multiobjective.nsgaii.jmetal5version;

import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.jmetal5version.NSGAIIBuilder;
import org.uma.jmetal.example.AlgorithmRunner;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.PMXCrossover;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.PermutationSwapMutation;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.operator.selection.impl.BinaryTournamentSelection;
import org.uma.jmetal.problem.multiobjective.MultiobjectiveTSP;
import org.uma.jmetal.problem.permutationproblem.PermutationProblem;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;
import org.uma.jmetal.util.AbstractAlgorithmRunner;
import org.uma.jmetal.util.JMetalException;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.comparator.RankingAndCrowdingDistanceComparator;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

import java.io.IOException;
import java.util.List;
import org.uma.jmetal.operator.crossover.impl.NullCrossover;
import org.uma.jmetal.operator.mutation.impl.Kinterchange;
import org.uma.jmetal.problem.integermatrixproblem.IntegerMatrixProblem;
import org.uma.jmetal.problem.multiobjective.MultiobjectiveETP;
import org.uma.jmetal.solution.integermatrixsolution.IntegerMatrixSolution;

/**
 * Class for configuring and running the NSGA-II algorithm to solve the bi-objective ETP
 *
 * @author Ahmad A. Datti <aadatti.cs@buk.edu.ng>
 */
public class NSGAIIETPRunner extends AbstractAlgorithmRunner {
  /**
   * @param args Command line arguments.
   * @throws java.io.IOException
   * @throws SecurityException
   */
  public static void main(String[] args) throws JMetalException, IOException {
    JMetalRandom.getInstance().setSeed(100L);

    IntegerMatrixProblem problem;
    Algorithm<List<IntegerMatrixSolution<Integer>>> algorithm;
    CrossoverOperator<IntegerMatrixSolution<Integer>> crossover;
    MutationOperator<IntegerMatrixSolution<Integer>> mutation;
    SelectionOperator<List<IntegerMatrixSolution<Integer>>, IntegerMatrixSolution<Integer>> selection;

    problem = new MultiobjectiveETP("C:/Users/PhDLab/Documents/NetBeansProjects/examTimetableDataReader/exam_comp_set33.exam");

    int [][] conflictMatrix =  problem.getConflictMatrix(); 
    boolean kempeChain=true;
    
    crossover = new NullCrossover();

    double mutationProbability = 0.9;
    mutation = new Kinterchange(mutationProbability, conflictMatrix, problem, kempeChain);

    selection =
        new BinaryTournamentSelection<IntegerMatrixSolution<Integer>>(
            new RankingAndCrowdingDistanceComparator<IntegerMatrixSolution<Integer>>());

    int populationSize = 100;   //int populationSize = 100;
    algorithm =
        new NSGAIIBuilder<IntegerMatrixSolution<Integer>>(
                problem, crossover, mutation, populationSize)
            .setSelectionOperator(selection)
            .setMaxEvaluations(200) //.setMaxEvaluations(10000)
            .build();

    AlgorithmRunner algorithmRunner = new AlgorithmRunner.Executor(algorithm).execute();

    List<IntegerMatrixSolution<Integer>> population = algorithm.getResult();
    long computingTime = algorithmRunner.getComputingTime();

    new SolutionListOutput(population)
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.tsv"))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.tsv"))
        .print();

    JMetalLogger.logger.info("Total execution time: " + computingTime + "ms");
    JMetalLogger.logger.info("Random seed: " + JMetalRandom.getInstance().getSeed());
    JMetalLogger.logger.info("Objectives values have been written to file FUN.tsv");
    JMetalLogger.logger.info("Variables values have been written to file VAR.tsv");
  }
}
