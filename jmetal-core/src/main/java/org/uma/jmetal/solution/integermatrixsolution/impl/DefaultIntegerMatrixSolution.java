/*
 * The MIT License
 *
 * Copyright 2020 PhDLab.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.uma.jmetal.solution.integermatrixsolution.impl;

import java.util.Map;
import org.uma.jmetal.solution.AbstractSolution;
import org.uma.jmetal.solution.integermatrixsolution.IntegerMatrixSolution;

/**
 *
 * @author PhDLab
 */
@SuppressWarnings("serial")
public class DefaultIntegerMatrixSolution extends AbstractSolution<Integer[]> 
    implements IntegerMatrixSolution<Integer[]> 
{
    
  /** Constructor */
  public DefaultIntegerMatrixSolution(int permutationLength, int numberOfObjectives) {
    super(permutationLength, numberOfObjectives); 

  }

  /** Copy Constructor */
  public DefaultIntegerMatrixSolution(DefaultIntegerMatrixSolution [] solution) {
    super(solution[].getLength(), solution[].getNumberOfObjectives());

  }

  @Override
  public DefaultIntegerMatrixSolution copy() {
    return new DefaultIntegerMatrixSolution(this);
  }

  @Override
  public Map<Object, Object> getAttributes() {
    return attributes;
  }

  @Override
  public int getLength() {
    return getNumberOfVariables();
  }
}
