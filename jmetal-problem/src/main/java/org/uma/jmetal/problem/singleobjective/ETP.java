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
    
    private int numberOfExams,numberOfPeriods,numberOfRooms;
    private int [][] matrix;
    private int [][] ttable;
    Graph<Integer, DefaultEdge> exGraph = new SimpleGraph<>(DefaultEdge.class);
    
    Map <Integer,List> studentMap = new HashMap<>();
    ArrayList<Exam> examVector;
    GreedyColoring exGraphColored;
    
    public ETP(String conflictFile) throws IOException
    {
        this.examVector = new ArrayList<>();
        matrix = readProblem(conflictFile);
        
        setNumberOfVariables(numberOfExams);
        setNumberOfObjectives(1);
        setName("ETP");
    }
    
    @Override
    public void evaluate(PermutationSolution<Integer> solution) 
    {
        int fitness=0;
        
        for(int i=0; i<=numberOfExams-1;i++)
        {
            for(int j=i+1; j<=numberOfExams;j++)
            {
                for(int p=0;p<=exGraphColored.getColoring().getNumberColors();p++)
                {
                    fitness+=ttable[i][p]*ttable[j][p]*matrix[i][j];
                }
            }
        }
        
        solution.setObjective(0, fitness);
    }

    @Override
    public int getLength() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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

        matrix  = readExams(token,found);
        readPeriods(token,found);
        readRooms(token,found);
        readConstraints(token,found);
        readWeightings(token,found);
        createGraph(matrix);
        exGraphColored = new GreedyColoring(exGraph);
        createTTable();
        
        System.out.println("Reading Successful.");
        
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
        System.out.println("Number of Exams: "+numberOfExams);
        tok.nextToken();tok.nextToken();tok.nextToken();

        System.out.println("Reading Token: "+tok.nval);
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
                numberOfPeriods=(int)tok.nval;
                System.out.println("Finished Reading Enrollments.");
                System.out.println("No. of Periods to be read = "+numberOfPeriods);
                fnd = true ;
            }                
            else
            {                
                t = tok.nextToken();

                switch(t)
                {                    
                    case StreamTokenizer.TT_EOL:    
                        tok.nextToken(); 
                        //System.out.println("Now reading: "+(int)tok.nval);
                        addExam(tok);                        
                        break;
                    case StreamTokenizer.TT_NUMBER: 
                        
                        Integer currentStudent = (int)tok.nval;                 
                        examVector.get(tok.lineno()-2).addStudent(currentStudent);
                        //System.out.println("Student "+(int)tok.nval+" added successfully.");
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

        //Print Student Map
//        int studentCount=0;
//        for(Map.Entry<Integer,List> entry : studentMap.entrySet())            
//        {
//            //System.out.println("Student "+ (++studentCount) + "{ " + entry.getKey() + "}: Exams = " + entry.getValue());
//        }

        //Initialize Conflict Matrix
        matrix= new int[numberOfExams][numberOfExams];
        //ArrayList <ArrayList<Integer>> conflictMatrix = new ArrayList<>(numberOfExams);
        for(int i=0;i<=numberOfExams-1;i++)
        {            
            //conflictMatrix.add(new ArrayList(numberOfExams));
            for(int j=0;j<=numberOfExams-1;j++)
            {
                matrix[i][j]=0;
                //conflictMatrix.get(i).add(0);
            }
        }
        //Generate Conflict Matrix
        ArrayList cleared = new ArrayList();
        System.out.println("Total Exams: "+examVector.size());
        examVector.forEach((e)->System.out.println(e.examId));
        
        for(int currExam=0; currExam<=examVector.size()-2;currExam++)
        {                        
            //System.out.println("Current Exam: " + examVector.get(currExam).examId+ " with "+ examVector.get(currExam).enrollmentList.size()+" students");
            int student;
            cleared.clear();
            for(int currStudent=0; currStudent<=examVector.get(currExam).enrollmentList.size()-1;currStudent++)
            {
                student = examVector.get(currExam).enrollmentList.get(currStudent);
                if(cleared.contains(student))continue;
                cleared.add(student);
                //System.out.println("Current Student: " + student);
                for(int nextExam=currExam+1;nextExam<=examVector.size()-1;nextExam++)
                {                   
                    //System.out.println("Next Exam: " + examVector.get(nextExam).examId);
                    if(examVector.get(nextExam).enrollmentList.contains(student))
                    {
                        //System.out.println("Student "+student +" found in both exams "+ currExam +" and "+ nextExam);                        
                        //int tmpEnrollment =  conflictMatrix.get(currExam).get(nextExam);
                        int conflictCount = matrix[currExam][nextExam];
                        
                        //tmpEnrollment++;  
                        conflictCount++;
                        
                        //conflictMatrix.get(currExam).set(nextExam, tmpEnrollment);                        
                        //conflictMatrix.get(nextExam).set(currExam, tmpEnrollment);
                        
                        matrix[currExam][nextExam]=conflictCount;
                        matrix[nextExam][currExam]=conflictCount;
                    }
                }
            }
        }
        return matrix;
    }
    
    void addExam(StreamTokenizer tok)            
    {
        
        int line = tok.lineno()-1;
        if(line<=numberOfExams)
        {
            //System.out.println("Token is @ line: "+tok.lineno());
            examVector.add(new Exam(line-1,(int)tok.nval));        
            //System.out.println("Exam "+ (line-1) + " added. Duration = " + examVector.get(line-1).examDuration);  
            //examVector.forEach((e)->System.out.println(e.examId));
        }                
    }
    
    void readPeriods(StreamTokenizer tok, boolean fnd) throws IOException
    {
    //Read Periods
        fnd=false;
        int t;
        while(!fnd) 
        {
            if ((tok.sval != null) && ((tok.sval.compareTo("Rooms") == 0)))
            {
                tok.nextToken();
                tok.nextToken();
                numberOfRooms=(int)tok.nval;
                System.out.println("Finished Reading Periods.");
                System.out.println("Number of Periods = "+numberOfRooms);
                fnd = true ;
            }                
            else
            {                                                   
                t = tok.nextToken();
                switch(t)
                {
                    case StreamTokenizer.TT_EOL:
                        break;
                    case StreamTokenizer.TT_NUMBER:                    
                        System.out.println("nextToken():"+tok.nval);
                        break;
                }
            }
        }
    }
    
    void readRooms(StreamTokenizer tok, boolean fnd) throws IOException
    {
    //Read Rooms
        fnd=false;int t;
        while(!fnd) 
        {
            if ((tok.sval != null) && ((tok.sval.compareTo("PeriodHardConstraints") == 0)))
            {
                tok.nextToken();
                System.out.println("Finished Reading Rooms.");
                fnd = true ;
            }                
            else
            {                                                   
                t = tok.nextToken();
                switch(t)
                {
                    case StreamTokenizer.TT_EOL:
                        break;
                    case StreamTokenizer.TT_NUMBER:                    
                        System.out.println("nextToken():"+tok.nval);
                        break;
                }
            }
        }
    }
    
    void readConstraints(StreamTokenizer tok, boolean fnd) throws IOException
    {
        //Read PeriodHardConstraints
        fnd=false;int t;
        while(!fnd) 
        {
            if ((tok.sval != null) && ((tok.sval.compareTo("RoomHardConstraints") == 0)))
            {
                tok.nextToken();
                tok.nextToken();
                numberOfRooms=(int)tok.nval;
                System.out.println("Finished Reading PeriodHardConstraints.");
                fnd = true ;
            }                
            else
            {                                                   
                t = tok.nextToken();
                switch(t)
                {
                    case StreamTokenizer.TT_EOL:
                        break;
                    case StreamTokenizer.TT_NUMBER:                    
                        System.out.println("nextToken():"+tok.nval);
                        break;
                    case StreamTokenizer.TT_WORD:
                        System.out.println("nextToken():"+tok.sval);
                        break;
                }
            }
        }

        //Read RoomHardConstraints
        fnd=false;
        while(!fnd) 
        {
            if ((tok.sval != null) && ((tok.sval.compareTo("InstitutionalWeightings") == 0)))
            {
                tok.nextToken();
                tok.nextToken();
                numberOfRooms=(int)tok.nval;
                System.out.println("Finished Reading RoomHardConstraints.");
                fnd = true ;
            }                
            else
            {                                                   
                t = tok.nextToken();
                switch(t)
                {
                    case StreamTokenizer.TT_EOL:
                        break;
                    case StreamTokenizer.TT_NUMBER:                    
                        System.out.println("nextToken():"+tok.nval);
                        break;
                    case StreamTokenizer.TT_WORD:
                        System.out.println("nextToken():"+tok.sval);
                        break;
                }
            }
        }
    }
    
    void readWeightings(StreamTokenizer tok, boolean fnd) throws IOException
    {
    //Read InstitutionalWeightings
        int t = tok.nextToken();    //WATCHOUT
        while(t != StreamTokenizer.TT_EOF)
            {                               
                switch(t)
                {
                    case StreamTokenizer.TT_EOL:
                        break;
                    case StreamTokenizer.TT_NUMBER:                    
                        System.out.println("nextToken():"+tok.nval);
                        break;
                    case StreamTokenizer.TT_WORD:
                        System.out.println("nextToken():"+tok.sval);
                        break;
                }
                t= tok.nextToken();
            }
    }
    
    void createGraph(int[][] cMat)
    {
        for(int v1=1;v1<=numberOfExams;v1++)exGraph.addVertex(v1);
 
        System.out.println("Vertices in graph: "+exGraph.vertexSet());
        for(int v1=1;v1<=numberOfExams;v1++)
        {
            for(int v2=1;v2<=numberOfExams;v2++)
            {
                if(cMat[v1-1][v2-1]!=0)exGraph.addEdge(v1, v2);
            }
        }
        
        System.out.println("Adjacency List: ");
        for(int v1=1;v1<=numberOfExams;v1++)System.out.println(exGraph.edgesOf(v1));
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
        
//        System.out.println("ttable: ");
//        System.out.print("E/P ");
//        for(int p=0;p<P;p++) System.out.print(p+ " ");
//        System.out.println();
//        for(int i=0;i<E;i++)
//        {
//            System.out.print((i+1)+":  ");
//            for(int p=0;p<P;p++)
//            {                
//                System.out.print(ttable[i][p]+" ");                
//            }
//            System.out.println();
//        }   
    }
    
}