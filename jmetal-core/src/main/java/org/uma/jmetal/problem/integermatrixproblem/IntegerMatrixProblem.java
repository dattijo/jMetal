
package org.uma.jmetal.problem.integermatrixproblem;

import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.integermatrixsolution.IntegerMatrixSolution;

/**
 *
 * @author PhDLab
 */
public interface IntegerMatrixProblem <S extends IntegerMatrixSolution<?>> extends Problem<S> {
    int getLength() ;
    
    int getExamsFromVariable(int index);
    
//    int geTotalNumberOfExams();
}