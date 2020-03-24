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
package org.uma.jmetal.problem.singleobjective;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.Graph;

import org.jgrapht.alg.color.*;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import org.uma.jmetal.problem.permutationproblem.impl.AbstractIntegerPermutationProblem;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;

/**
 *
 * @author PhDLab
 */
public class ETP extends AbstractIntegerPermutationProblem
{
    class Exam
    {
        int examId,examDuration,studentsCount=0;
        ArrayList<Integer> enrollmentList = new ArrayList<>();

        Exam(int id, int duration)
        {
            examId=id;
            examDuration=duration;
        }

        void addStudent(Integer student)
        {
            enrollmentList.add(student);
            studentsCount++;
        }
    }
    
    private int numberOfExams;
    private int [][] conflictMatrix;
    private int [][] proximityMatrix;
    private int [][] ttable;
    
    Map <Integer,List> studentMap;
    ArrayList<Exam> examVector;
    
    Graph<Integer, DefaultEdge> exGraph;
    GreedyColoring exGraphColored;
    
    
      
    public ETP(String problemFile) throws IOException
    {
        examVector = new ArrayList<>();
        studentMap = new HashMap<>();
        exGraph = new SimpleGraph<>(DefaultEdge.class);
        
        conflictMatrix = readProblem(problemFile);
                
        setNumberOfVariables(numberOfExams);
        setNumberOfObjectives(1);
        setName("ETP");
    }
    
    @Override
    public void evaluate(PermutationSolution<Integer> solution) 
    {
        int fitness=0;
        int P = exGraphColored.getColoring().getNumberColors();
        
        for(int i=0; i<numberOfExams-1;i++)
        {          
            for(int j=i+1; j<numberOfExams;j++)
            {
                for(int p=0;p<P-1;p++)
                {                    
                    fitness+=ttable[i][p]*ttable[j][p+1]*conflictMatrix[i][j];                                        
                }
            }
        }        
        solution.setObjective(0, fitness);
    }

    public void evaluateConstraints()
    {
        int violationCount=0;
        int P = exGraphColored.getColoring().getNumberColors();
        
        for(int i=0; i<numberOfExams-1;i++)
        {          
            for(int j=i+1; j<numberOfExams;j++)
            {         
                    if()
                    {
                        violationCount+=proximityMatrix[i][j]*conflictMatrix[i][j];
                    }
                    
            }
        }    
    }
    
    @Override
    public int getLength() 
    {
        return numberOfExams;
    }

    private int[][] readProblem(String file) throws IOException
    {
        InputStream in = getClass().getResourceAsStream(file);
        if (in == null) 
        {
            in = new FileInputStream(file);
        }
    
        InputStreamReader isr = new InputStreamReader(in);
        BufferedReader br = new BufferedReader(isr);

        StreamTokenizer token = new StreamTokenizer(br);
        
        token.eolIsSignificant(true);
        boolean found ;
        found = false ;

        conflictMatrix  = readExams(token,found);
        createGraph(conflictMatrix);
        exGraphColored = new GreedyColoring(exGraph);
        createTTable();
        
//        readPeriods(token,found);
//        readRooms(token,found);
//        readConstraints(token,found);
//        readWeightings(token,found);                
        
        return conflictMatrix;
    }
    
    int [][] readExams(StreamTokenizer tok, boolean fnd) throws IOException
    {
        tok.nextToken();
        while(!fnd) 
        {
            if ((tok.sval != null) && ((tok.sval.compareTo("Exams") == 0)))
                fnd = true ;
            else
                tok.nextToken() ;
        }

        tok.nextToken() ;
        tok.nextToken() ;

        numberOfExams =  (int)tok.nval ;
        tok.nextToken();tok.nextToken();tok.nextToken();
        addExam(tok);
             

        //Read Enrollments
        fnd=false;
        int t=0;
        while(!fnd) 
        {
            if ((tok.sval != null) && ((tok.sval.compareTo("Periods") == 0)))
            {
                tok.nextToken();
                tok.nextToken();
//                numberOfPeriods=(int)tok.nval;
                fnd = true ;
            }                
            else
            {                
                t = tok.nextToken();

                switch(t)
                {                    
                    case StreamTokenizer.TT_EOL:    
                        tok.nextToken(); 
                        addExam(tok);                        
                        break;
                    case StreamTokenizer.TT_NUMBER: 
                        
                        Integer currentStudent = (int)tok.nval;                 
                        examVector.get(tok.lineno()-2).addStudent(currentStudent);

                        if(!studentMap.containsKey(currentStudent))
                        {                            
                            List <Integer> examList = new ArrayList();
                            studentMap.put(currentStudent, examList);                            
                        }
                        studentMap.get(currentStudent).add(tok.lineno()-1);
                        break;
                }                
            }
        }       
        
        conflictMatrix= new int[numberOfExams][numberOfExams];
        //Generate Conflict Matrix
        ArrayList cleared = new ArrayList();        
        for(int currExam=0; currExam<=examVector.size()-2;currExam++)
        {                        
            int student;
            cleared.clear();
            for(int currStudent=0; currStudent<=examVector.get(currExam).enrollmentList.size()-1;currStudent++)
            {
                student = examVector.get(currExam).enrollmentList.get(currStudent);
                if(cleared.contains(student))continue;
                cleared.add(student);
                
                for(int nextExam=currExam+1;nextExam<=examVector.size()-1;nextExam++)
                {                   
                    if(examVector.get(nextExam).enrollmentList.contains(student))
                    {
                        int conflictCount = conflictMatrix[currExam][nextExam];                        
                        conflictCount++;                        
                        conflictMatrix[currExam][nextExam]=conflictCount;
                        conflictMatrix[nextExam][currExam]=conflictCount;
                    }
                }
            }
        }
        return conflictMatrix;
    }
    
    void addExam(StreamTokenizer tok)            
    {   
        int line = tok.lineno()-1;
        if(line<=numberOfExams)examVector.add(new Exam(line-1,(int)tok.nval));              
    }
    
    void createGraph(int[][] cMat)
    {
        for(int v1=1;v1<=numberOfExams;v1++)
        {
            exGraph.addVertex(v1);
        }
 
        for(int v1=1;v1<=numberOfExams;v1++)
        {
            for(int v2=1;v2<=numberOfExams;v2++)
            {
                if(cMat[v1-1][v2-1]!=0)exGraph.addEdge(v1, v2);
            }
        }        
    }
    
    void createTTable()
    {
        int E = numberOfExams;
        int P = exGraphColored.getColoring().getNumberColors();
        ttable = new int[E][P];
        for(int i=0;i<E;i++)
        {
            for(int p=0;p<P;p++)
            {
                if ((int)exGraphColored.getColoring().getColors().get(i+1)==p)
                {
                    ttable[i][p]=1;
                }
            }
        }        
    }
    
//    void readPeriods(StreamTokenizer tok, boolean fnd) throws IOException
//    {
//    //Read Periods
//        fnd=false;
//        int t;
//        while(!fnd) 
//        {
//            if ((tok.sval != null) && ((tok.sval.compareTo("Rooms") == 0)))
//            {
//                tok.nextToken();
//                tok.nextToken();
////                numberOfRooms=(int)tok.nval;
////                System.out.println("Finished Reading Periods.");
////                System.out.println("Number of Periods = "+numberOfRooms);
//                fnd = true ;
//            }                
//            else
//            {                                                   
//                t = tok.nextToken();
//                switch(t)
//                {
//                    case StreamTokenizer.TT_EOL:
//                        break;
//                    case StreamTokenizer.TT_NUMBER:                    
////                        System.out.println("nextToken():"+tok.nval);
//                        break;
//                }
//            }
//        }
//    }
    
//    void readRooms(StreamTokenizer tok, boolean fnd) throws IOException
//    {
//    //Read Rooms
//        fnd=false;int t;
//        while(!fnd) 
//        {
//            if ((tok.sval != null) && ((tok.sval.compareTo("PeriodHardConstraints") == 0)))
//            {
//                tok.nextToken();
////                System.out.println("Finished Reading Rooms.");
//                fnd = true ;
//            }                
//            else
//            {                                                   
//                t = tok.nextToken();
//                switch(t)
//                {
//                    case StreamTokenizer.TT_EOL:
//                        break;
//                    case StreamTokenizer.TT_NUMBER:                    
////                        System.out.println("nextToken():"+tok.nval);
//                        break;
//                }
//            }
//        }
//    }
    
//    void readConstraints(StreamTokenizer tok, boolean fnd) throws IOException
//    {
//        //Read PeriodHardConstraints
//        fnd=false;int t;
//        while(!fnd) 
//        {
//            if ((tok.sval != null) && ((tok.sval.compareTo("RoomHardConstraints") == 0)))
//            {
//                tok.nextToken();
//                tok.nextToken();
////                numberOfRooms=(int)tok.nval;
////                System.out.println("Finished Reading PeriodHardConstraints.");
//                fnd = true ;
//            }                
//            else
//            {                                                   
//                t = tok.nextToken();
//                switch(t)
//                {
//                    case StreamTokenizer.TT_EOL:
//                        break;
//                    case StreamTokenizer.TT_NUMBER:                    
////                        System.out.println("nextToken():"+tok.nval);
//                        break;
//                    case StreamTokenizer.TT_WORD:
////                        System.out.println("nextToken():"+tok.sval);
//                        break;
//                }
//            }
//        }
//
//        //Read RoomHardConstraints
//        fnd=false;
//        while(!fnd) 
//        {
//            if ((tok.sval != null) && ((tok.sval.compareTo("InstitutionalWeightings") == 0)))
//            {
//                tok.nextToken();
//                tok.nextToken();
//                numberOfRooms=(int)tok.nval;
////                System.out.println("Finished Reading RoomHardConstraints.");
//                fnd = true ;
//            }                
//            else
//            {                                                   
//                t = tok.nextToken();
//                switch(t)
//                {
//                    case StreamTokenizer.TT_EOL:
//                        break;
//                    case StreamTokenizer.TT_NUMBER:                    
////                        System.out.println("nextToken():"+tok.nval);
//                        break;
//                    case StreamTokenizer.TT_WORD:
////                        System.out.println("nextToken():"+tok.sval);
//                        break;
//                }
//            }
//        }
//    }
    
//    void readWeightings(StreamTokenizer tok, boolean fnd) throws IOException
//    {
//    //Read InstitutionalWeightings
//        int t = tok.nextToken();    //WATCHOUT
//        while(t != StreamTokenizer.TT_EOF)
//            {                               
//                switch(t)
//                {
//                    case StreamTokenizer.TT_EOL:
//                        break;
//                    case StreamTokenizer.TT_NUMBER:                    
////                        System.out.println("nextToken():"+tok.nval);
//                        break;
//                    case StreamTokenizer.TT_WORD:
////                        System.out.println("nextToken():"+tok.sval);
//                        break;
//                }
//                t= tok.nextToken();
//            }
//    }
    
    
    
}
