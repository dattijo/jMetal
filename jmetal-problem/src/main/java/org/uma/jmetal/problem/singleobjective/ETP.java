package org.uma.jmetal.problem.singleobjective;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.jgrapht.Graph;
import org.jgrapht.alg.color.*;
import org.jgrapht.alg.interfaces.VertexColoringAlgorithm.Coloring;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import org.uma.jmetal.problem.integermatrixproblem.impl.AbstractIntegerMatrixProblem;
import org.uma.jmetal.solution.integermatrixsolution.IntegerMatrixSolution;
import org.uma.jmetal.solution.integermatrixsolution.impl.DefaultIntegerMatrixSolution;

/**
 *
 * @author aadatti
 */
public class ETP extends AbstractIntegerMatrixProblem        
{
    NumberFormat formatter = new DecimalFormat("#0000.0000");     
    
    Map<Integer,Student> studentMap;
//    Map<Integer,Integer> afterConstraintVector;
//    Map<Integer,Integer> coincidenceConstraintVector;
//    Map<Integer,Integer> exclusionConstraintVector;
    ArrayList<Exam> examVector;
    ArrayList<Exam> exclusiveExamsVector;
    ArrayList<TimeSlot> timeslotVector;
    ArrayList<Room> roomVector;    
    ArrayList<Faculty> facultyVector;
    ArrayList<Campus> campusVector;
    ArrayList<ArrayList<Integer>> timetableSolution;
    
    int numberOfExams;
    int numberOfTimeSlots;    
    int numberOfCampuses;
    int numberOfFaculties;
    int numberOfRooms;
    int overallRoomCapacity;
    int twoInARow;
    int twoInADay;
    int periodSpread;
    int nonMixedDurations;
    int numberOfLargestExams;
    int numberOfLastPeriods;
    int frontLoadPenalty;

    private int examDuration;
    
    int [][] conflictMatrix;
    double [][] roomToRoomDistanceMatrix;  
    
    int [][] exclusionMatrix;// = new int[numberOfExams][numberOfExams];
    int [][] coincidenceMatrix;// = new int[numberOfExams][numberOfExams];
    int [][] afterMatrix;// = new int[numberOfExams][numberOfExams];
    
    Graph<Integer, DefaultEdge> exGraph;
    Coloring coloredGraph;
    
    Random rand = new Random();
    
   
    
    class Student
    {
        int sId;
        ArrayList<Exam> examList = new ArrayList<>();
        
        Student(int id)
        {
            sId = id;
        }
        
        void addExam(Exam e)
        {
            examList.add(e);
        }
    }
    
    class Exam
    {
        int examId,examDuration,studentsCount=0;
        TimeSlot timeslot;
        Room room;//=new Room(Integer.MAX_VALUE,Integer.MAX_VALUE,Integer.MAX_VALUE,Integer.MAX_VALUE);//(int cap, int rId, int fId, int dToF)
        ArrayList<Student> enrollmentList = new ArrayList<>();
        boolean exlcusive=false;
         
        Exam(int id, int duration)
        {
            examId=id;
            examDuration=duration;
        }

        void addStudent(Student student)
        {
            enrollmentList.add(student);
            studentsCount++;
        }
        
        void setTimeSlot(TimeSlot tSlot)
        {
            timeslot = tSlot;
        }
        
        void setRoom(Room rm)
        {
            room =rm;
        }
    }
    
    class Room
    {
        int capacity, roomId, distToFaculty;
        Faculty myFaculty;
        List<Exam> examList = new ArrayList<>();       
        
        Room(int cap, int rId, int fId, int dToF)
        {
            capacity = cap;
            roomId = rId;
            myFaculty = facultyVector.get(fId-1);
            distToFaculty = dToF;
        }
        
        void allocateExam(Exam exam)
        {
            examList.add(exam);
        }
        
        Faculty getFaculty()
        {
            return myFaculty;
        }               
    }
    
    class Faculty
    {
        int facId,  distToCampus;
        double longitude, latitude;
        Campus myCampus;
        
        Faculty(int fId, int cId, double lon, double lat, int dToC)
        {
            myCampus = campusVector.get(cId-1);
            facId = fId;
            longitude = lon;
            latitude = lat;
            distToCampus = dToC;
        }
        
        Campus getCampus()
        {
            return myCampus;
        }
    }
    
    class Campus
    {
        int campId;
        double longitude, latitude;
        Campus(int cId,double lon, double lat)
        {
            campId=cId;
            longitude=lon;
            latitude=lat;
        }
    }
    
    class TimeSlot
    {
        int id,day,pos,duration;
        ArrayList<Exam> examList = new ArrayList<>();
        TimeSlot(int i,int d, int t, int dur)
        {
            id = i;
            day = d;
            pos = t;
            duration = dur;
        }
        
        void addExam(Exam e)
        {
            examList.add(e);
        }
    }
    
    public ETP(String problemFile) throws IOException
    {
        overallRoomCapacity=0;
        
        studentMap = new HashMap<>();
        examVector = new ArrayList<>();
        exclusiveExamsVector = new ArrayList<>();
        timeslotVector = new ArrayList();
        roomVector = new ArrayList();
        facultyVector = new ArrayList();
        campusVector = new ArrayList();
        timetableSolution = new ArrayList<ArrayList<Integer>>(); 
        
//        afterConstraintVector = new HashMap<>();
//        coincidenceConstraintVector = new HashMap<>();
//        exclusionConstraintVector = new HashMap<>();
         
        conflictMatrix = readProblem(problemFile);                
        
        roomToRoomDistanceMatrix = new double[numberOfRooms][numberOfRooms];
        
//        exclusionMatrix = new int[numberOfExams][numberOfExams];
//        coincidenceMatrix = new int[numberOfExams][numberOfExams];
//        afterMatrix = new int[numberOfExams][numberOfExams];
        
        generateDistanceMatrices();
        
        exGraph = new SimpleGraph<>(DefaultEdge.class); 
        createGraph(conflictMatrix);          
        
        setNumberOfVariables(numberOfExams);
        setNumberOfObjectives(1);        
        //setNumberOfConstraints(1);
        setName("ETP");        
    }
    
    @Override
    public int[][] getConflictMatrix()
    {
        return conflictMatrix;
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
        
        readTimeSlots(token,found);
        readCampuses(token,found);
        readFaculties(token,found);
        readRooms(token,found);
        readPeriodConstraints(token,found);
        readRoomConstraints(token,found);
        readWeightings(token,found);                
        
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
                numberOfTimeSlots=(int)tok.nval;
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
                        int currentStudent = (int)tok.nval;                                               
                        if(!studentMap.containsKey(currentStudent))
                        {                            
                            studentMap.put(currentStudent, new Student(currentStudent));
                        }
                        examVector.get(tok.lineno()-2).addStudent(studentMap.get(currentStudent));
                        studentMap.get(currentStudent).examList.add(new Exam(tok.lineno()-1,examDuration));
                        break;
                }                
            }
        } 
        
        //Print Student Map
//        for(Map.Entry<Integer,Student> entry : studentMap.entrySet())            
//        {
//            System.out.print("Student " + entry.getKey()+" Exams: ");
//            for(int i =0;i<entry.getValue().examList.size();i++)
//            {
//                System.out.print(entry.getValue().examList.get(i).examId+" ");
//            }
//            System.out.println();
//        }
        
        conflictMatrix= new int[numberOfExams][numberOfExams];                
        
        //Generate Conflict Matrix
        ArrayList<Student> cleared = new ArrayList();        
        for(int currExam=0; currExam<=examVector.size()-2;currExam++)
        {         
            cleared.clear();
            int studentCount = examVector.get(currExam).enrollmentList.size();
            for(int currStudent=1; currStudent<=studentCount; currStudent++)
            {             
                Student student = examVector.get(currExam).enrollmentList.get(currStudent-1);               
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
        
            //int Matrix ConflictMatrix
//        System.out.println("\nDISPLAYING int[][] Matrix CONFLICT MARIX:\n");
//        for(int i=0;i<numberOfExams;i++)
//        {
//            for(int j=0;j<numberOfExams;j++)
//            {
//                System.out.print(conflictMatrix[i][j]+", ");
//            }
//            System.out.println();
//        }
        
        return conflictMatrix;
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
    
    void readTimeSlots(StreamTokenizer tok, boolean fnd) throws IOException
    {
        //Read TimeSlots
//        System.out.println("Number of TimeSlots = "+numberOfTimeSlots);
        fnd=false;
        int t,pCount=0;
        int day,pos,duration;
        day=pos=duration=0;
        while(!fnd) 
        {
            if ((tok.sval != null) && ((tok.sval.compareTo("Campuses") == 0)))
            {
                tok.nextToken();
                tok.nextToken();                
                numberOfCampuses=(int)tok.nval;
//                System.out.println("Finished Reading TimeSlots.");
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
                        day = (int)tok.nval;tok.nextToken();tok.nextToken();
                        pos = (int)tok.nval;tok.nextToken();tok.nextToken();
                        duration = (int)tok.nval;tok.nextToken(); 
                        addTimeSlot(++pCount,day,pos,duration);
                        break;
                }
            }
        }
        
//        System.out.println("Timeslots Vector");
//        for(int i=0; i<timeslotVector.size();i++)
//        {
//            System.out.print("Timeslot "+timeslotVector.get(i).id);
//            System.out.print(" in day "+timeslotVector.get(i).day);
//            System.out.print(" at position "+timeslotVector.get(i).pos);
//            System.out.println(" has "+timeslotVector.get(i).duration+" minutes");
//        }
    }    
    
    void readCampuses(StreamTokenizer tok, boolean fnd) throws IOException
    {
        //Read Campuses
//        System.out.println("Number of Campuses = "+numberOfCampuses);
        int t=0,cCount=0;
        fnd=false;
        double lon=0.0,lat=0.0;
        while(!fnd) 
        {
            if ((tok.sval != null) && ((tok.sval.compareTo("Faculties") == 0)))
            {
                tok.nextToken();
                tok.nextToken();              
                numberOfFaculties=(int)tok.nval;
//                System.out.println("Finished Reading Campuses.");
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
                        lon = tok.nval;tok.nextToken();tok.nextToken();
                        lat = tok.nval;tok.nextToken();
//                        System.out.println("Long: "+lon+"\nLat: "+lat);
                        addCampus(++cCount,lon,lat);
                        break;
                }
            }
        }
    }
    
    void readFaculties(StreamTokenizer tok, boolean fnd) throws IOException
    {
        //Read Faculties
//        System.out.println("Number of Faculties = "+numberOfFaculties);
        fnd=false;
        int t,camp=0,dToCamp=0,fCount=0;
        double lon=0.0,lat=0.0;
        while(!fnd) 
        {
            if ((tok.sval != null) && ((tok.sval.compareTo("Rooms") == 0)))
            {
                tok.nextToken();
                tok.nextToken();
                
                numberOfRooms=(int)tok.nval;
//                System.out.println("Finished Reading Facuties.");
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
                        camp = (int)tok.nval;tok.nextToken();tok.nextToken();
                        dToCamp = (int)tok.nval;tok.nextToken();tok.nextToken();
                        lon = tok.nval;tok.nextToken();tok.nextToken();
                        lat = tok.nval;tok.nextToken();
                        addFaculty(++fCount,camp,lon,lat,dToCamp);
                        break;
                }
            }
        }
        
//        System.out.println("Faculties Vector");
//        for(int i=0; i<facultyVector.size();i++)
//        {
//            System.out.print("Faculty "+facultyVector.get(i).facId);
//            System.out.print(" in campus "+facultyVector.get(i).campId+" is ");
//            System.out.print(" at longitude "+facultyVector.get(i).longitude);
//            System.out.print(" and latitude "+facultyVector.get(i).latitude+" is ");
//            System.out.println(facultyVector.get(i).distToCampus+"m from main entrance.");         
//        }
    }
    
    void readRooms(StreamTokenizer tok, boolean fnd) throws IOException
    {
        //Read Rooms
//        System.out.println("Number of Rooms = "+numberOfRooms);
        fnd=false;
        int t,rCount=0,cap,fac,dToFac;
        while(!fnd) 
        {
            if ((tok.sval != null) && ((tok.sval.compareTo("PeriodHardConstraints") == 0)))
            {
                tok.nextToken();
                tok.nextToken();                
//                System.out.println("Finished Reading Rooms.");
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
                        cap = (int)tok.nval;tok.nextToken();tok.nextToken();
                        fac = (int)tok.nval;tok.nextToken();tok.nextToken();
                        dToFac = (int)tok.nval;tok.nextToken(); 
                        addRoom(cap,++rCount,fac,dToFac);
                        break;
                }
            }
        }        
        
//        System.out.println("Room Vector");
//        for(int i=0; i<roomVector.size();i++)
//        {
//            System.out.print("Room "+roomVector.get(i).roomId);
//            System.out.print(" in faculty "+roomVector.get(i).myFaculty.facId);
//            System.out.print(" has capacity "+roomVector.get(i).capacity);
//            System.out.println(" and is "+roomVector.get(i).distToFaculty+"m from main faculty entrance.");         
//        }
    }
    
    void readPeriodConstraints(StreamTokenizer tok, boolean fnd) throws IOException
    {
        exclusionMatrix = new int[numberOfExams][numberOfExams];
        coincidenceMatrix = new int[numberOfExams][numberOfExams];
        afterMatrix = new int[numberOfExams][numberOfExams];        
        
        //Read PeriodHardConstraints
        tok.wordChars('_', '_');
        fnd=false;int t;
        while(!fnd) 
        {
            if ((tok.sval != null) && ((tok.sval.compareTo("RoomHardConstraints") == 0)))
            {
                tok.nextToken();
                tok.nextToken();
//                numberOfRooms=(int)tok.nval;
//                System.out.println("Finished Reading PeriodHardConstraints.");
                fnd = true ;
            }                
            else
            {                                                   
                t = tok.nextToken();
                int exam1=-1,exam2=-1;
                String constraint="";
                switch(t)
                {
                    case StreamTokenizer.TT_EOL:
                        break;
                    case StreamTokenizer.TT_NUMBER:                    
                        //System.out.println("nextToken():"+tok.nval);
                        exam1 = (int)tok.nval;//System.out.println("exam1:"+exam1);
                        tok.nextToken();tok.nextToken();
                        constraint = tok.sval;//System.out.println("constraint:"+constraint);
                        tok.nextToken();tok.nextToken();
                        exam2 = (int)tok.nval;//System.out.println("exam2:"+exam2);
                        break;
//                    case StreamTokenizer.TT_WORD:
//                        //System.out.println("nextToken():"+tok.sval);
//                        break;
                }
                
                switch(constraint)
                {
                    case "EXCLUSION":
//                        exclusionConstraintVector.put(exam1, exam2);
                        exclusionMatrix[exam1][exam2]=1;
                        break;
                    case "EXAM_COINCIDENCE":
//                        coincidenceConstraintVector.put(exam1, exam2);
                        coincidenceMatrix[exam1][exam2]=1;
                            break;
                    case "AFTER":
//                        afterConstraintVector.put(exam1, exam2);
                        afterMatrix[exam1][exam2]=1;
                        break;
                }
            }
        }
        
//        System.out.println("exclusionMatrix:"+Arrays.deepToString(exclusionMatrix));
//        System.out.println("coincidenceMatrix:"+Arrays.deepToString(coincidenceMatrix));
//        System.out.println("afterMatrix:"+Arrays.deepToString(afterMatrix));
    }
    void readRoomConstraints(StreamTokenizer tok, boolean fnd) throws IOException
    {
        //Read RoomHardConstraints
        fnd=false;int t;
        while(!fnd) 
        {
            if ((tok.sval != null) && ((tok.sval.compareTo("InstitutionalWeightings") == 0)))
            {
                tok.nextToken();tok.nextToken();
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
//                        System.out.println("nextToken():"+tok.nval);
                        examVector.get((int)tok.nval).exlcusive=true;
                        exclusiveExamsVector.add(examVector.get((int)tok.nval));
                        break;
                }
            }
        }
//        System.out.println("exclusiveExamsVector:");
//        for(int i=0;i<examVector.size();i++)
//        {
//            System.out.println(examVector.get(i).examId+"--> "+examVector.get(i).exlcusive);
//        }
    }
    
    void readWeightings(StreamTokenizer tok, boolean fnd) throws IOException
    {
        //Read InstitutionalWeightings
        int t = tok.nextToken();    
        while(t != StreamTokenizer.TT_EOF)
        {                                         
            switch(t)
            {
                case StreamTokenizer.TT_EOL:
                    break;                
                case StreamTokenizer.TT_WORD:
                    System.out.println("nextToken():"+tok.sval);
                    if(tok.sval.compareTo("TWOINAROW")==0)
                    {
                        tok.nextToken();tok.nextToken();
                        twoInARow=(int)tok.nval;
                    }
                    else if(tok.sval.compareTo("TWOINADAY")==0)
                    {
                        tok.nextToken();tok.nextToken();
                        twoInADay=(int)tok.nval;
                    }
                    else if(tok.sval.compareTo("PERIODSPREAD")==0)
                    {
                        tok.nextToken();tok.nextToken();
                        periodSpread=(int)tok.nval;
                    }
                    else if(tok.sval.compareTo("NONMIXEDDURATIONS")==0)
                    {
                        tok.nextToken();tok.nextToken();
                        nonMixedDurations=(int)tok.nval;
                    }
                    else if(tok.sval.compareTo("FRONTLOAD")==0)
                    {
                        tok.nextToken();tok.nextToken();
                        numberOfLargestExams=(int)tok.nval;
                        tok.nextToken();tok.nextToken();
                        numberOfLastPeriods=(int)tok.nval;
                        tok.nextToken();tok.nextToken();
                        frontLoadPenalty=(int)tok.nval;
                    }
                    break;
            }
            t= tok.nextToken();
        }
        System.out.println("twoinarow:"+twoInARow);
        System.out.println("twoinaday:"+twoInADay);
        System.out.println("periodSpread:"+periodSpread);
        System.out.println("nonMixedDurations:"+nonMixedDurations);
        System.out.println("numberOfLargestExams:"+numberOfLargestExams);
        System.out.println("numberOfLastPeriods:"+numberOfLastPeriods);
        System.out.println("frontLoadPenalty:"+frontLoadPenalty);
    }  
    
    void generateDistanceMatrices() 
    {
        for(int i=0;i<numberOfRooms;i++)
        {
            for(int j=i;j<numberOfRooms;j++)
            {
                double long1,long2,lat1,lat2;
                Room rm1 = roomVector.get(i);
                Room rm2 = roomVector.get(j);

                if(rm1.getFaculty().facId==rm2.getFaculty().facId)
                {
                    roomToRoomDistanceMatrix[i][j]=0.0;
                    roomToRoomDistanceMatrix[j][i]=0.0;
                }
                else 
                {
                    if(rm1.getFaculty().getCampus().campId==rm2.getFaculty().getCampus().campId)
                    {
                        long1 = rm1.getFaculty().longitude;
                        lat1 = rm1.getFaculty().latitude;
                        long2 = rm2.getFaculty().longitude;
                        lat2 = rm2.getFaculty().latitude;
                        
                        roomToRoomDistanceMatrix[i][j] = rm1.distToFaculty
                                +gpsDistance(long1,lat1,long2,lat2)+rm2.distToFaculty;
                        roomToRoomDistanceMatrix[j][i] = rm1.distToFaculty
                                +gpsDistance(long1,lat1,long2,lat2)+rm2.distToFaculty;
                    }
                    else
                    {
                        long1=rm1.getFaculty().getCampus().longitude;
                        lat1=rm1.getFaculty().getCampus().latitude;
                        long2=rm2.getFaculty().getCampus().longitude;
                        lat2=rm2.getFaculty().getCampus().latitude;
                        
                        //rm2Fac+Fac2Cam+Cam2Cam+Cam2Fac+Fac2rm
                        roomToRoomDistanceMatrix[i][j]=rm1.distToFaculty+rm1.getFaculty().distToCampus
                                +gpsDistance(long1,lat1,long2,lat2)+rm2.getFaculty().distToCampus
                                +rm2.distToFaculty;
                        roomToRoomDistanceMatrix[j][i]=rm1.distToFaculty+rm1.getFaculty().distToCampus
                                +gpsDistance(long1,lat1,long2,lat2)+rm2.getFaculty().distToCampus
                                +rm2.distToFaculty;
                    }                   
                }                    
            }
        }               
        //Displa roomToRoomDistanceMatrix
//        System.out.println("roomToRoomDistanceMatrix: ");
//        for(int i=0;i<numberOfRooms;i++)
//        {
//            for(int j=0;j<numberOfRooms;j++)
//            {
//                System.out.print(formatter.format(roomToRoomDistanceMatrix[i][j])+" ");
//            }
//            System.out.println();
//        }
    }
    
    double gpsDistance(double lo1, double la1,double lo2,double la2)
    {        
            double R = 6378.137; // Radius of earth in KM
            double dLat = la2 * Math.PI / 180 - la1 * Math.PI / 180;
            double dLon = lo2 * Math.PI / 180 - lo1 * Math.PI / 180;
            double a = Math.sin(dLat/2) * Math.sin(dLat/2) 
                        + Math.cos(la1 * Math.PI / 180) * Math.cos(la2 * Math.PI / 180) 
                        * Math.sin(dLon/2) * Math.sin(dLon/2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
            double d = R * c;
            return d * 1000; // meters          
    }
    
    void addExam(StreamTokenizer tok)            
    {   
        int line = tok.lineno()-1;
        examDuration = (int)tok.nval;
        if(line<=numberOfExams)examVector.add(new Exam(line,examDuration));              
    }
    
    void addTimeSlot(int id, int d, int p, int dur)            
    {   
        timeslotVector.add(new TimeSlot(id,d,p,dur));              
    }

    void addFaculty(int id, int c, double lon, double lat, int dToC)
    {
        facultyVector.add(new Faculty(id, c, lon, lat, dToC));
    }
    
    void addRoom(int c,int id,int f,int dToF)
    {
        roomVector.add(new Room(c,id,f,dToF));
        overallRoomCapacity+=c;
    }
    
    void addCampus(int id,double lon,double lat)
    {
        campusVector.add(new Campus(id,lon,lat));
    }
    
    void allocateTimeSlots()
    {
        ArrayList<TimeSlot> availableTimeSlots = new ArrayList<>();
        availableTimeSlots.addAll(timeslotVector);
        for(int t=0;t<availableTimeSlots.size();t++)
        {
            availableTimeSlots.get(t).examList.clear();
        }
//          coloredGraph  = new LargestDegreeFirstColoring(exGraph).getColoring();
//        coloredGraph  = new GreedyColoring(exGraph).getColoring();
//        coloredGraph  = new BrownBacktrackColoring(exGraph).getColoring();
//        coloredGraph  = new RandomGreedyColoring(exGraph).getColoring();
        coloredGraph  = new SaturationDegreeColoring(exGraph).getColoring();
        
        for(int i=0;i<examVector.size();i++)
        {           
            int allocatedTime = (int)coloredGraph.getColors().get(i+1);
            examVector.get(i).setTimeSlot(availableTimeSlots.get(allocatedTime));
//            System.out.println("Exam "+examVector.get(i).examId+" @ Timeslot "+examVector.get(i).timeslot.id);            
            availableTimeSlots.get(allocatedTime).addExam(examVector.get(i));
        }
        
        for(int t=0;t<availableTimeSlots.size();t++)
        {
//            System.out.println("Timeslot "+availableTimeSlots.get(t).id+" has "+availableTimeSlots.get(t).examList.size()+" exams");
        }
    }
    
    void allocateRooms(ArrayList rooms)
    {   
        class ExamComparator implements Comparator<Exam> 
        {
        @Override
            public int compare(Exam a, Exam b) 
            {
                return a.studentsCount < b.studentsCount ? -1 : a.studentsCount == b.studentsCount ? 0 : 1;
            }
        }
        
        class RoomComparator implements Comparator<Room> 
        {
        @Override
            public int compare(Room a, Room b) 
            {
                return a.capacity < b.capacity ? -1 : a.capacity == b.capacity ? 0 : 1;
            }
        }    
        
//        System.out.println("ROOM ALLOCATION FOR NEW SOLUTION:");
        ArrayList<Exam> allocatedExams = new ArrayList();
        ArrayList<Exam> unAllocatedExams = new ArrayList();
        for(int t =0; t<timeslotVector.size();t++)
        {                        
            if(timeslotVector.get(t).examList.size()<=0)continue;
            ArrayList<Room> tmpRoomVector = new ArrayList();
            
            tmpRoomVector.addAll(roomVector);
            Collections.sort(tmpRoomVector, new RoomComparator()); 
            
//            for(int r =0; r<tmpRoomVector.size();r++)
//            {
//                System.out.println("Room "+tmpRoomVector.get(r).roomId+". Capacity = "+tmpRoomVector.get(r).capacity);
//            } 
            
            TimeSlot tmpT = (TimeSlot)timeslotVector.get(t);
//            System.out.println("Now in Timeslot "+tmpT.id+" having "+tmpT.examList.size()+" exams.");
            
            ArrayList<Exam> tmpExamVector = new ArrayList();
            tmpExamVector.addAll(tmpT.examList);
//            System.out.println("tmpT.examList.size():"+tmpT.examList.size());
            Collections.sort(tmpExamVector, new ExamComparator().reversed());  
//            int myTotalEnrollment=0;
//            for(int e =0; e<tmpExamVector.size();e++)
//            {
//                myTotalEnrollment+=tmpExamVector.get(e).studentsCount;
//                System.out.println("Exam "+tmpExamVector.get(e).examId+". Enrollment = "+tmpExamVector.get(e).studentsCount);                
//            } 
//            System.out.println("overallRoomCapacity ("+overallRoomCapacity+") > = "+"("+myTotalEnrollment+") myTotalEnrollment?"+(overallRoomCapacity>=myTotalEnrollment));
            
            int e=0;
            while(e<tmpExamVector.size())
            {                        
//                System.out.println("Allocating rooms to "+tmpExamVector.size()+" exams...");
                int r=0;

//                System.out.println("Now in Exam "+tmpExamVector.get(e).examId);
                while(tmpRoomVector.size()>0&&r<tmpRoomVector.size()&&(tmpExamVector.size()>0)&&e<tmpExamVector.size())
                {
//                    System.out.println("\nSearching for room to exam "+tmpExamVector.get(0).examId);
                    Exam tmpE = tmpExamVector.get(e); 
                    Room tmpR=  tmpRoomVector.get(r);                    

                    if(tmpE.studentsCount<=tmpR.capacity)
                    {
                        tmpE.setRoom(tmpR);
                        allocatedExams.add(tmpE); 
//                        System.out.println("Exam "+tmpE.examId+" has been set to room "+tmpR.roomId);
                        examVector.get(tmpE.examId-1).setRoom(tmpR);
//                        System.out.println("Removing exam "+tmpE.examId);
                        tmpExamVector.remove(tmpE);
//                        System.out.println("tmpExamVector now has "+tmpExamVector.size()+" exams");
//                        System.out.println("Removing timeslot "+tmpR.roomId);
                        tmpRoomVector.remove(tmpR);
//                        System.out.println("tmpRoomVector now has "+tmpRoomVector.size()+" rooms");
//                        e++;
                    }
                    else
                    {      
                        r++;
                    }                                                            
                }
                if(r>=tmpRoomVector.size()&&e<tmpExamVector.size())
                {
//                    System.out.print("...skipping exam");//+tmpExamVector.get(e).examId+"\n");// for exam "+tmpExamVector.get(0).examId+" with "+tmpExamVector.get(0).studentsCount);
                    unAllocatedExams.add(tmpExamVector.get(e));                    
//                    System.out.println("Allocated = "+allocatedExams.size());                    
//                    System.out.println("Unallocated = "+unAllocatedExams.size());
                    e++;
                }
            }
        }
       
        
//        System.out.println("****");
//        for(int e =0; e<examVector.size();e++)
//        {
//            //Exam tmpE = (Exam)tmpExamVector.get(e);
//            System.out.println("Exam "+examVector.get(e).examId+" has been set to room "+examVector.get(e).room.roomId+" in timeslot "+examVector.get(e).timeslot.id);
//        }
//        System.out.println("****");

    }
    
    ArrayList<ArrayList<Integer>> generateTimeTableMatrix()
    {
        ArrayList<Integer> randRooms = new ArrayList<>();
        for(int i=0;i<numberOfRooms;i++)
        {
            randRooms.add(i);
        }
        allocateTimeSlots();
        allocateRooms(randRooms);
        
        ArrayList<Integer> tmpSlots = new ArrayList<>();
        for(int i = 0; i<numberOfTimeSlots;i++)tmpSlots.add(i,0); 
        
        for(int j = 0; j<examVector.size();j++)timetableSolution.add(j, new ArrayList<Integer>(tmpSlots));
        
        for(int i = 0; i<numberOfExams;i++)
        {            
            for(int j = 0; j<numberOfTimeSlots;j++)
            {            
                if(examVector.get(i).timeslot.id!=j+1)continue; 
                int room=-1;
                if(examVector.get(i).room!=null)
                {
                    room = examVector.get(i).room.roomId;
                }
                                
                timetableSolution.get(i).set(j, room);                   
            }
        }

        return timetableSolution;
    }   
            
    
    @Override
    public IntegerMatrixSolution<ArrayList<Integer>> createSolution() 
    {
        timetableSolution = generateTimeTableMatrix();

        DefaultIntegerMatrixSolution solution = new DefaultIntegerMatrixSolution(getListOfExamsPerVariable(), getNumberOfObjectives());
        
        for (int i = 0 ; i < getLength(); i++)
        {
            solution.setVariable(i, timetableSolution.get(i));
        }
        
        return solution;
    }         
    
    @Override
    public void evaluate(IntegerMatrixSolution<ArrayList<Integer>> solution)             
    {        
        boolean isFeasible=false;
        
        double fitness1=0.0;    //proximity cost
        double fitness2=0.0;    //movement cost
        int fitness3=0;         //room cost
        
        //proximity constraint
        outerloop:
        for(int i=0; i<numberOfExams;i++)
        {                      
            for(int j=0; j<numberOfExams;j++)
            {
                if(i==j)continue;
                
                int slot1=0,slot2=0;                                  
                
                ArrayList<Integer> x = solution.getVariable(i);
                while(x.get(slot1)==0)slot1++;            
                
                ArrayList<Integer> y = solution.getVariable(j);
                while(y.get(slot2)==0)slot2++; 
      
                if(conflictMatrix[i][j]!=0)
                {
                    if(slot1==slot2)
                    {
                        isFeasible=false;                        
//                        System.out.println("Exam "+solution.getVariable(i)+" conflicts with exam "+solution.getVariable(j)+" @ timeslot "+slot1);
                        break outerloop;
                    }
                                        
                    int prox = (int)Math.pow(2,(5 - Math.abs(slot1-slot2)));

                    fitness1+=prox*conflictMatrix[i][j];
                    isFeasible=true;  
                }
            }                                                       
        }
        
        //movement constraint
//        for (int s=1;s<=studentMap.size();s++) 
        for(Map.Entry<Integer, Student> currStudent : studentMap.entrySet())    
        {
//            System.out.println("s = "+s);
//            for(int e1=0;e1<studentMap.get(s).examList.size();e1++)
            for(int e1=0;e1<currStudent.getValue().examList.size();e1++)
            {   
//                Exam cExam = examVector.get(studentMap.get(s).examList.get(e1).examId-1);
                Exam cExam = examVector.get(currStudent.getValue().examList.get(e1).examId-1);
//                for(int e2=e1;e2<studentMap.get(s).examList.size();e2++)
                for(int e2=e1;e2<currStudent.getValue().examList.size();e2++)
                {
                    if(e1==e2)continue;
                    Exam nExam = examVector.get(currStudent.getValue().examList.get(e2).examId-1);
//                    Exam nExam = examVector.get(studentMap.get(s).examList.get(e2).examId-1);                 
                    
                    if(cExam.room==null)continue;int rm1 = cExam.room.roomId;
                    if(nExam.room==null)continue;int rm2 = nExam.room.roomId;
                    
                    fitness2+=roomToRoomDistanceMatrix[rm1-1][rm2-1];
                }
            }
        }
        
        //room under-utilization constraint
        int roomUtilization=0;
        int totalRoomCapacity=0;
//        ArrayList sol = (ArrayList)solution;
//        sol.forEach((currExam)->
//        {
//            ArrayList x = (ArrayList)currExam;
//            //ArrayList<Integer> x = currExam;
//            
//            for(int j =0;j<x.size();j++)
//            {
//                if((int)x.get(j)==0)continue;
//                System.out.println("Evaluating..."+x.get(j));
//                totalRoomCapacity+=roomVector.get((int)x.get(j-1)).capacity
//                totalRoomCapacity+=roomVector.get(((int)x.get(j-1))).capacity;
//                roomUtilization+=roomVector.get(x.get(j)-1)
//                        .examList.get(i).studentsCount;
//            }
//        });
//        for(int i =0;i<solution.getNumberOfVariables();i++)
//        {
//            ArrayList<Integer> x = solution.getVariable(i);
//            
//            for(int j =0;j<x.size();j++)
//            {
//                if(x.get(j)==0)continue;
//                System.out.println("Exam "+i+". in room "+x.get(j)+" has "+roomVector.get(x.get(j)-1).examList.size()+" exams");
//                totalRoomCapacity+=roomVector.get(x.get(j)-1).capacity;
//                
//                roomUtilization+=roomVector.get(x.get(j)-1)
//                        .examList.get(i).studentsCount;
//            }
//        } 
        fitness3 = totalRoomCapacity-roomUtilization;
               
//        solution.setObjective(0, fitness1);
//        solution.setObjective(0, fitness2);
//        solution.setObjective(0, fitness3);
        solution.setObjective(0, (isFeasible)?((fitness1+fitness2)/studentMap.size())+fitness3:Integer.MAX_VALUE);
//        solution.setObjective(0, (isFeasible)?((fitness1+fitness2)/studentMap.size())+fitness3:Double.POSITIVE_INFINITY);

//        System.out.println("Solution Evaluated: "+solution.getObjective(0));//+" Variable: "+solution.getVariables());
    } 
    
    public boolean evaluateConstraints(IntegerMatrixSolution<ArrayList<Integer>> solution)
    {                
////        int clash=0;
//        int P = solution.getNumberOfVariables();
//        
//        for(int i=0; i<numberOfExams-1;i++)
//        {          
//            for(int j=0; j<numberOfExams-1;j++)
//            {
//                int x = solution.getVariable(i);
//                int y = solution.getVariable(j);
//                for(int p=0;p<P-1;p++)
//                {                    
//                    if(*conflictMatrix[x][y])
//                    return false;
//                }
//            }
//        } 
//        
//        
//        boolean seatingViolation=false;
//        for(int i=0; i<numberOfExams-1;i++)
//        {
//            for(int p=0;p<P-1;p++)
//            {
//                if(ttable[i][p]*examEnrollment[i]<S)
//                {
//                    seatingViolation=true;
//                }
//            }
//        }
//
//        
//        
//        solution.setConstraint(0, violationCount);  
//        System.out.println("Constraint = "+violationCount);
//        return clash;
        return false;
    }
    
    @Override
    public int getLength() 
    {
        return numberOfExams;
    }   
    
    @Override
    public ArrayList<Integer> getListOfExamsPerVariable() {
        ArrayList<Integer> list = new ArrayList<>(Arrays.asList(numberOfExams)); 
        return list ;
    }
}  