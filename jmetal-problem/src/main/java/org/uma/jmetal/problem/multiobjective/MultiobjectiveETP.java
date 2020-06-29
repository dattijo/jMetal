package org.uma.jmetal.problem.multiobjective;

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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.alg.color.*;
import org.jgrapht.alg.interfaces.VertexColoringAlgorithm.Coloring;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import org.uma.jmetal.problem.integermatrixproblem.impl.AbstractIntegerMatrixProblem;
import org.uma.jmetal.solution.integermatrixsolution.IntegerMatrixSolution;
import org.uma.jmetal.solution.integermatrixsolution.impl.DefaultIntegerMatrixSolution;

import net.sourceforge.jFuzzyLogic.FIS;
import net.sourceforge.jFuzzyLogic.FunctionBlock;
import net.sourceforge.jFuzzyLogic.plot.JFuzzyChart;
import net.sourceforge.jFuzzyLogic.rule.Variable;

/**
 *
 * @author aadatti
 */
public final class MultiobjectiveETP extends AbstractIntegerMatrixProblem {

    FIS fis;
    NumberFormat formatter = new DecimalFormat("#0000.0000");

    Map<Integer, Student> studentMap;
    ArrayList<Exam> examVector;
    ArrayList<Exam> exclusiveExamsVector;
    ArrayList<TimeSlot> timeslotVector;
    ArrayList<Room> roomVector;
    ArrayList<Faculty> facultyVector;
    ArrayList<Campus> campusVector;
    ArrayList<ArrayList<Integer>> timetableSolution;
    ArrayList<Integer> largestExams;
    ArrayList<Integer> courseType;
    ArrayList<Integer> courseCredits;
    ArrayList<Double> successRatio;
    ArrayList<Integer> perceivedDifficulty;
    ArrayList<Double> computedDifficulty;

    int numberOfExams;
    int numberOfTimeSlots;
    int numberOfCampuses;
    int numberOfFaculties;
    int numberOfRooms;

    int twoInARowWeight;
    int twoInADayWeight;
    int periodSpreadWeight;
    int nonMixedDurationsWeight;
    int frontLoadWeight;
    int numberOfLargestExams;
    int numberOfLastPeriods;

    int spreadGap;

    private int examDuration;

    int[][] conflictMatrix;
    double[][] roomToRoomDistanceMatrix;

    int[][] exclusionMatrix;
    int[][] coincidenceMatrix;
    int[][] afterMatrix;

    boolean feasible;

    Graph<Integer, DefaultEdge> exGraph;
    Coloring coloredGraph;

    class Student {

        int sId;
        ArrayList<Exam> examList = new ArrayList<>();

        Student(int id) {
            sId = id;
        }

        void addExam(Exam e) {
            examList.add(e);
        }
    }

    class Exam {

        int examId, examDuration, priority, studentsCount = 0;
        private double difficulty;
        TimeSlot timeslot;
        Room room;
        ArrayList<Student> enrollmentList = new ArrayList<>();
        boolean exlcusive = false;

        Exam(int id, int duration) {
            examId = id;
            examDuration = duration;
        }

        void addStudent(Student student) {
            enrollmentList.add(student);
            studentsCount++;
        }

        void setTimeSlot(int tSlot) {
            if (tSlot != -1) {
                timeslot = timeslotVector.get(tSlot);
            } else {
                timeslot = null;
            }
        }

        void setRoom(int i) {
            if (i != -1) {
                room = roomVector.get(i);
            } else {
                room = null;
            }
        }

        void setDifficulty(double d) {
            difficulty = d;
        }

        double getDifficulty() {
            return difficulty;
        }
    }

    class Room {

        int freeSeats, capacity, roomId, distToFaculty, penalty;
        Faculty myFaculty;
        List<Exam> examList = new ArrayList<>();

        Room(int cap, int rId, int fId, int dToF, int pen) {
            capacity = cap;
            roomId = rId;
            myFaculty = facultyVector.get(fId);
            distToFaculty = dToF;
            penalty = pen;
            freeSeats = capacity;
        }

        boolean allocateExam(int i) {
            Exam exam = examVector.get(i);
            if (!examList.contains(exam)) {
                if (getFreeSeats(exam.examId) >= exam.studentsCount) {
                    examList.add(exam);
                    return true;
                }
            }
            return false;
        }

        void deAllocateExam(int i) {
            Exam exam = examVector.get(i);
            if (!examList.remove(exam)) {
                System.out.println("-->deAllocation Failed<--");
            };
//            freeSeats+=exam.studentsCount;
        }

        Faculty getFaculty() {
            return myFaculty;
        }

        int getFreeSeats(int timeslot) {
            int myFreeSeats = freeSeats;
            for (Exam e : getExams(timeslot)) {
                myFreeSeats -= e.studentsCount;
            }
            return myFreeSeats;
        }

        ArrayList<Exam> getExams(int timeslot) {
            ArrayList<Exam> result = new ArrayList();
            for (Exam e : examList) {
                if (e.timeslot.id == timeslot) {
                    result.add(e);
                }
            }
            return result;
        }
    }

    class Faculty {

        int facId, distToCampus;
        double longitude, latitude;
        Campus myCampus;

        Faculty(int fId, int cId, double lon, double lat, int dToC) {
            myCampus = campusVector.get(cId);
            facId = fId;
            longitude = lon;
            latitude = lat;
            distToCampus = dToC;
        }

        Campus getCampus() {
            return myCampus;
        }
    }

    class Campus {

        int campId;
        double longitude, latitude;

        Campus(int cId, double lon, double lat) {
            campId = cId;
            longitude = lon;
            latitude = lat;
        }
    }

    class TimeSlot {

        int id, duration, penalty;
        Date dateAndTime;
        ArrayList<Exam> examList = new ArrayList<>();

        TimeSlot(int i, Date d, int dur, int pen) {
            id = i;
            dateAndTime = d;
            duration = dur;
            penalty = pen;
        }

        void addExam(int i) {
            Exam e = examVector.get(i);
            examList.add(e);
        }

        void removeExam(int i) {
            Exam e = examVector.get(i);
            if (examList.contains(e)) {
                examList.remove(e);
            }
        }
    }

    class Conflict {

        Exam conflictingExam;
        int evictionCount;

        Conflict(Exam e, int eC) {
            conflictingExam = e;
            evictionCount = eC;
        }
    }

    class TimeslotRoomPair {

        ArrayList<Conflict> conflicts = new ArrayList();

        int timeslot1, room1, rank;

        TimeslotRoomPair(int t1, int r1) {
            timeslot1 = t1;
            room1 = r1;
            rank = 0;
        }

        void computeRank() {
            int sum = 0;
            for (Conflict con : conflicts) {
                sum += con.evictionCount;
            }
            rank = sum;
        }
    }

    class ExamComparatorByEnrollment implements Comparator<Exam> {

        @Override
        public int compare(Exam a, Exam b) {
            return a.studentsCount < b.studentsCount ? -1 : a.studentsCount == b.studentsCount ? 0 : 1;
        }
    }

    class ExamComparatorByPriority implements Comparator<Exam> {

        @Override
        public int compare(Exam a, Exam b) {
            return a.priority < b.priority ? -1 : a.priority == b.priority ? 0 : 1;
        }
    }

    class RoomComparator implements Comparator<Room> {

        @Override
        public int compare(Room a, Room b) {
            return a.capacity < b.capacity ? -1 : a.capacity == b.capacity ? 0 : 1;
        }
    }

    class TimeslotRoomPairComparator implements Comparator<TimeslotRoomPair> {

        @Override
        public int compare(TimeslotRoomPair a, TimeslotRoomPair b) {
            return a.rank < b.rank ? -1 : a.rank == b.rank ? 0 : 1;
        }
    }

    public MultiobjectiveETP(String problemFile, String fuzzySystem, String examDifficultyData) throws IOException {
        studentMap = new HashMap<>();
        examVector = new ArrayList<>();
        exclusiveExamsVector = new ArrayList<>();
        timeslotVector = new ArrayList();
        roomVector = new ArrayList();
        facultyVector = new ArrayList();
        campusVector = new ArrayList();
        timetableSolution = new ArrayList<>();
        largestExams = new ArrayList<>();
        spreadGap = 0;

        conflictMatrix = readProblem(problemFile);

        roomToRoomDistanceMatrix = new double[numberOfRooms][numberOfRooms];

        generateDistanceMatrices();

//        System.out.println("Number of Students = "+studentMap.size());
//        System.out.println("Number if Exams = "+numberOfExams);
//        System.out.println("Number if Timeslots = "+numberOfTimeSlots);
//        System.out.println("Number if Rooms = "+numberOfRooms);
//        System.out.println("Number if Campuses = "+numberOfCampuses);
//        System.out.println("Number if Faculties = "+numberOfFaculties);
        exGraph = new SimpleGraph<>(DefaultEdge.class);
        createGraph(conflictMatrix);
        generateDifficultyMatrix(fuzzySystem, examDifficultyData);
        setNumberOfVariables(numberOfExams);
        setNumberOfObjectives(2);
//        this.setNumberOfConstraints(5);
        setName("ETP");
    }

    @Override
    public int[][] getConflictMatrix() {
        return conflictMatrix;
    }

    private boolean generateDifficultyMatrix(String fuzzySystem, String examDifficultyData) throws IOException {
        courseType = new ArrayList();
        courseCredits = new ArrayList();
        successRatio = new ArrayList();
        perceivedDifficulty = new ArrayList();
        computedDifficulty = new ArrayList();

        readExamDifficultyData(examDifficultyData);

        fis = FIS.load(fuzzySystem, true);
        if (fis == null) {
            System.err.println("Can't load file: '" + fuzzySystem + "'");
            return false;
        }
        for (int i = 0; i < numberOfExams; i++) {
            FunctionBlock examDifficulty = fis.getFunctionBlock("examDifficulty");

            fis.setVariable("credits", courseCredits.get(i));
            fis.setVariable("type", courseType.get(i));
            fis.setVariable("sratio", successRatio.get(i));
            fis.setVariable("pdifficulty", perceivedDifficulty.get(i));
//            JFuzzyChart.get().chart(examDifficulty);
            fis.evaluate();

            Variable difficulty = examDifficulty.getVariable("difficulty");
//            Variable var = examDifficulty.getVariable("type");
//            JFuzzyChart.get().chart(var, true);
//            JFuzzyChart.get().chart(difficulty, difficulty.getDefuzzifier(), true);    
            double d = difficulty.getValue();
            computedDifficulty.add(d);
            examVector.get(i).setDifficulty(d);
        }
//        System.out.println("Sums\t\t\tProducts");
//        for(int i=0; i<numberOfExams-1;i++){
//            for(int j=0; j<numberOfExams;j++){
//                if(i==j)continue;
//                System.out.print(computedDifficulty.get(i)+computedDifficulty.get(j)+"\t");
//                System.out.println(computedDifficulty.get(i)*computedDifficulty.get(j));    
//            }
//        }
        return true;
    }

    private boolean readExamDifficultyData(String file) throws IOException {
        InputStream in = getClass().getResourceAsStream(file);
        if (in == null) {
            in = new FileInputStream(file);
        }

        InputStreamReader isr = new InputStreamReader(in);
        BufferedReader br = new BufferedReader(isr);

        StreamTokenizer token = new StreamTokenizer(br);

        token.eolIsSignificant(true);
        boolean found = false;

        int t = 0;
        while (!found && t != StreamTokenizer.TT_EOF) {
            switch (t) {
                case StreamTokenizer.TT_NUMBER:
//                    System.out.println("1token.nval "+token.nval);
                    courseType.add((int) token.nval);
                    token.nextToken();
                    token.nextToken();
//                    System.out.println("2token.nval "+token.nval);
                    courseCredits.add((int) token.nval);
                    token.nextToken();
                    token.nextToken();
//                    System.out.println("3token.nval "+token.nval);
                    successRatio.add(token.nval);
                    token.nextToken();
                    token.nextToken();
//                    System.out.println("4token.nval "+token.nval);
                    perceivedDifficulty.add((int) token.nval);
                    break;
                case StreamTokenizer.TT_EOL:
                    token.nextToken();
                    break;
            }
            t = token.nextToken();
            token.nextToken();
        }

//        System.out.println("courseType : "+courseType.toString());
//        System.out.println("courseCredits : "+courseCredits.toString());
//        System.out.println("successRatio : "+successRatio.toString());
//        System.out.println("perceivedDifficulty : "+perceivedDifficulty.toString());  
        return true;
    }

    private int[][] readProblem(String file) throws IOException {
        InputStream in = getClass().getResourceAsStream(file);
        if (in == null) {
            in = new FileInputStream(file);
        }

        InputStreamReader isr = new InputStreamReader(in);
        BufferedReader br = new BufferedReader(isr);

        StreamTokenizer token = new StreamTokenizer(br);

        token.eolIsSignificant(true);
        boolean found = false;
//        found = false ;

        conflictMatrix = readExams(token, found);

        readTimeslots(token, found);
        readCampuses(token, found);
        readFaculties(token, found);
        readRooms(token, found);
        readTimeslotConstraints(token, found);
        readRoomConstraints(token, found);
        readWeightings(token, found);

        setExamPriorities();
        return conflictMatrix;
    }

    int[][] readExams(StreamTokenizer tok, boolean fnd) throws IOException {
        tok.nextToken();
        while (!fnd) {
            if ((tok.sval != null) && ((tok.sval.compareTo("Exams") == 0))) {
                fnd = true;
            } else {
                tok.nextToken();
            }
        }

        tok.nextToken();
        tok.nextToken();

        numberOfExams = (int) tok.nval;
        tok.nextToken();
        tok.nextToken();
        tok.nextToken();

        addExam(tok);

        //Read Enrollments
        fnd = false;
        int t = 0;
        while (!fnd) {
            if ((tok.sval != null) && ((tok.sval.compareTo("Periods") == 0))) {
                tok.nextToken();
                tok.nextToken();

                numberOfTimeSlots = (int) tok.nval;
                fnd = true;
            } else {
                t = tok.nextToken();

                switch (t) {
                    case StreamTokenizer.TT_EOL:
                        tok.nextToken();
                        addExam(tok);
                        break;
                    case StreamTokenizer.TT_NUMBER:
                        int currentStudent = (int) tok.nval;
                        if (!studentMap.containsKey(currentStudent)) {
                            studentMap.put(currentStudent, new Student(currentStudent));
                        }
                        examVector.get(tok.lineno() - 2).addStudent(studentMap.get(currentStudent));
                        studentMap.get(currentStudent).examList.add(examVector.get(tok.lineno() - 2));
                        break;
                }
            }
        }

////        Print Student Map
//        for(Map.Entry<Integer,Student> entry : studentMap.entrySet())            
//        {
//            System.out.print("Student " + entry.getKey()+" Exams: ");
//            for(int i =0;i<entry.getValue().examList.size();i++)
//            {
//                System.out.print(entry.getValue().examList.get(i).examId+" ");
//            }
//            System.out.println();
//        }
        conflictMatrix = new int[numberOfExams][numberOfExams];

        //Generate Conflict Matrix
        ArrayList<Student> cleared = new ArrayList();
        for (int currExam = 0; currExam < examVector.size() - 1; currExam++) {
            cleared.clear();
            int studentCount = examVector.get(currExam).enrollmentList.size();
            for (int currStudent = 0; currStudent < studentCount; currStudent++) {
                Student student = examVector.get(currExam).enrollmentList.get(currStudent);
                if (cleared.contains(student)) {
                    continue;
                }

                cleared.add(student);

                for (int nextExam = currExam + 1; nextExam < examVector.size(); nextExam++) {
                    if (examVector.get(nextExam).enrollmentList.contains(student)) {
                        int conflictCount = conflictMatrix[currExam][nextExam];
                        conflictCount++;
                        conflictMatrix[currExam][nextExam] = conflictCount;
                        conflictMatrix[nextExam][currExam] = conflictCount;
                    }
                }
            }
        }

//        int Matrix ConflictMatrix;
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

    void createGraph(int[][] cMat) {
        for (int v1 = 0; v1 < numberOfExams; v1++) {
            exGraph.addVertex(v1);
        }

        for (int v1 = 0; v1 < numberOfExams; v1++) {
            for (int v2 = 0; v2 < numberOfExams; v2++) {
                if (cMat[v1][v2] != 0) {
                    exGraph.addEdge(v1, v2);
                }
            }
        }
    }

    void readTimeslots(StreamTokenizer tok, boolean fnd) throws IOException {
        //Read TimeSlots
//        System.out.println("Number of TimeSlots = "+numberOfTimeSlots);
        fnd = false;
        int t, tCount = 0;
//        int day, pos, duration, penalty;
//        day = pos = duration = 0;
        int day, month, year, hour, minutes, seconds, duration, penalty;
        day = month = year = hour = minutes = seconds = duration = penalty = 0;
        while (!fnd) {
            if ((tok.sval != null) && ((tok.sval.compareTo("Campuses") == 0))) {
                tok.nextToken();
                tok.nextToken();
                numberOfCampuses = (int) tok.nval;
//                System.out.println("Finished Reading TimeSlots.");
                fnd = true;
            } else {
                t = tok.nextToken();

                switch (t) {
                    case StreamTokenizer.TT_EOL:
                        break;
                    case StreamTokenizer.TT_NUMBER:
//                        day = (int) tok.nval;tok.nextToken();tok.nextToken();                                                
//                        pos = (int) tok.nval;tok.nextToken();tok.nextToken();
//                        duration = (int) tok.nval;tok.nextToken();tok.nextToken();
//                        penalty = (int) tok.nval;tok.nextToken();

                        day = (int) tok.nval;
                        tok.nextToken();
                        tok.nextToken();
                        month = (int) tok.nval;
                        tok.nextToken();
                        tok.nextToken();
                        year = (int) tok.nval;
                        tok.nextToken();
                        tok.nextToken();
                        hour = (int) tok.nval;
                        tok.nextToken();
                        tok.nextToken();
                        minutes = (int) tok.nval;
                        tok.nextToken();
                        tok.nextToken();
                        seconds = (int) tok.nval;
                        tok.nextToken();
                        tok.nextToken();
                        duration = (int) tok.nval;
                        tok.nextToken();
                        tok.nextToken();
                        penalty = (int) tok.nval;
                        tok.nextToken();
//                        System.out.println(day+":"+month+":"+year+", "+hour+":"+minutes+":"+seconds+", "+duration+", "+penalty);
                        Date examDateAndTime = new Date(year - 1900, month - 1, day, hour, minutes, seconds);
//                        System.out.println("examDateAndTime = "+examDateAndTime.toLocaleString());
                        addTimeSlot(tCount++, examDateAndTime, duration, penalty);
                        break;
                }
            }
        }

//        System.out.println("Timeslots Vector");
//        for(int i=0; i<timeslotVector.size();i++)
//        {
//            TimeSlot timeS = timeslotVector.get(i);
//            System.out.print("Timeslot "+timeS.id);
//            System.out.print(" @ "+timeS.dateAndTime.toLocaleString());
////            System.out.print(" at time "+timeS.getTime());
//            System.out.println(" has "+timeS.duration+" minutes");
//        }
    }

    void readCampuses(StreamTokenizer tok, boolean fnd) throws IOException {
        //Read Campuses
//        System.out.println("Number of Campuses = "+numberOfCampuses);
        int t = 0, cCount = 0;
        fnd = false;
        double lon = 0.0, lat = 0.0;
        while (!fnd) {
            if ((tok.sval != null) && ((tok.sval.compareTo("Faculties") == 0))) {
                tok.nextToken();
                tok.nextToken();
                numberOfFaculties = (int) tok.nval;
//                System.out.println("Finished Reading Campuses.");
                fnd = true;
            } else {
                t = tok.nextToken();
                switch (t) {
                    case StreamTokenizer.TT_EOL:
                        break;
                    case StreamTokenizer.TT_NUMBER:
                        lon = tok.nval;
                        tok.nextToken();
                        tok.nextToken();
                        lat = tok.nval;
                        tok.nextToken();
//                        System.out.println("Long: "+lon+"\nLat: "+lat);
                        addCampus(cCount++, lon, lat);
                        break;
                }
            }
        }
//        System.out.println("Campuse Vector:");
//        for (Campus c : campusVector){
//            System.out.println("Campus "+c.campId+"@ Longitude "+c.longitude+" and Latitude "+c.latitude);
//        } 
    }

    void readFaculties(StreamTokenizer tok, boolean fnd) throws IOException {
        //Read Faculties
//        System.out.println("Number of Faculties = "+numberOfFaculties);
        fnd = false;
        int t, camp = 0, dToCamp = 0, fCount = 0;
        double lon = 0.0, lat = 0.0;
        while (!fnd) {
            if ((tok.sval != null) && ((tok.sval.compareTo("Rooms") == 0))) {
                tok.nextToken();
                tok.nextToken();
                numberOfRooms = (int) tok.nval;
//                System.out.println("Finished Reading Facuties.");
                fnd = true;
            } else {
                t = tok.nextToken();
                switch (t) {
                    case StreamTokenizer.TT_EOL:
                        break;
                    case StreamTokenizer.TT_NUMBER:
                        camp = (int) tok.nval;
                        tok.nextToken();
                        tok.nextToken();
                        dToCamp = (int) tok.nval;
                        tok.nextToken();
                        tok.nextToken();
                        lon = tok.nval;
                        tok.nextToken();
                        tok.nextToken();
                        lat = tok.nval;
                        tok.nextToken();
                        addFaculty(fCount++, camp, lon, lat, dToCamp);
                        break;
                }
            }
        }

//        System.out.println("Faculties Vector");
//        for(int i=0; i<facultyVector.size();i++)
//        {
//            System.out.print("Faculty "+facultyVector.get(i).facId);
//            System.out.print(" in campus "+facultyVector.get(i).myCampus.campId+" is ");
//            System.out.print(" at longitude "+facultyVector.get(i).longitude);
//            System.out.print(" and latitude "+facultyVector.get(i).latitude+" is ");
//            System.out.println(facultyVector.get(i).distToCampus+"m from main entrance.");         
//        }
    }

    void readRooms(StreamTokenizer tok, boolean fnd) throws IOException {
        //Read Rooms
//        System.out.println("Number of Rooms = "+numberOfRooms);
        fnd = false;
        int t, rCount = 0, cap, fac, dToFac, penalty;
        while (!fnd) {
            if ((tok.sval != null) && ((tok.sval.compareTo("PeriodHardConstraints") == 0))) {
                tok.nextToken();
                tok.nextToken();
//                System.out.println("Finished Reading Rooms.");
                fnd = true;
            } else {
                t = tok.nextToken();
                switch (t) {
                    case StreamTokenizer.TT_EOL:
                        break;
                    case StreamTokenizer.TT_NUMBER:
                        cap = (int) tok.nval;
                        tok.nextToken();
                        tok.nextToken();

                        fac = (int) tok.nval;
                        tok.nextToken();
                        tok.nextToken();

                        dToFac = (int) tok.nval;
                        tok.nextToken();
                        tok.nextToken();

                        penalty = (int) tok.nval;
                        tok.nextToken();

                        addRoom(cap, rCount++, fac, dToFac, penalty);
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

    void readTimeslotConstraints(StreamTokenizer tok, boolean fnd) throws IOException {
        exclusionMatrix = new int[numberOfExams][numberOfExams];
        coincidenceMatrix = new int[numberOfExams][numberOfExams];
        afterMatrix = new int[numberOfExams][numberOfExams];

        //Read PeriodHardConstraints
        tok.wordChars('_', '_');
        fnd = false;
        int t;
        while (!fnd) {
            if ((tok.sval != null) && ((tok.sval.compareTo("RoomHardConstraints") == 0))) {
                tok.nextToken();
                tok.nextToken();
//                numberOfRooms=(int)tok.nval;
//                System.out.println("Finished Reading PeriodHardConstraints.");
                fnd = true;
            } else {
                t = tok.nextToken();
                int exam1 = -1, exam2 = -1;
                String constraint = "";
                switch (t) {
                    case StreamTokenizer.TT_EOL:
                        break;
                    case StreamTokenizer.TT_NUMBER:
                        //System.out.println("nextToken():"+tok.nval);
                        exam1 = (int) tok.nval;//System.out.println("exam1:"+exam1);
                        tok.nextToken();
                        tok.nextToken();
                        constraint = tok.sval;//System.out.println("constraint:"+constraint);
                        tok.nextToken();
                        tok.nextToken();
                        exam2 = (int) tok.nval;//System.out.println("exam2:"+exam2);
                        break;
//                    case StreamTokenizer.TT_WORD:
//                        //System.out.println("nextToken():"+tok.sval);
//                        break;
                }

                switch (constraint) {
                    case "EXCLUSION":
//                        exclusionConstraintVector.put(exam1, exam2);
                        exclusionMatrix[exam1][exam2] = 1;
                        break;
                    case "EXAM_COINCIDENCE":
//                        coincidenceConstraintVector.put(exam1, exam2);
                        coincidenceMatrix[exam1][exam2] = 1;
                        break;
                    case "AFTER":
//                        afterConstraintVector.put(exam1, exam2);
                        afterMatrix[exam1][exam2] = 1;
                        break;
                }
            }
        }

//        System.out.println("exclusionMatrix:"+Arrays.deepToString(exclusionMatrix));
//        System.out.println("coincidenceMatrix:"+Arrays.deepToString(coincidenceMatrix));
//        System.out.println("afterMatrix:"+Arrays.deepToString(afterMatrix));
    }

    void readRoomConstraints(StreamTokenizer tok, boolean fnd) throws IOException {
        //Read RoomHardConstraints
        fnd = false;
        int t;
        while (!fnd) {
            if ((tok.sval != null) && ((tok.sval.compareTo("InstitutionalWeightings") == 0))) {
                tok.nextToken();
                tok.nextToken();
                fnd = true;
            } else {
                t = tok.nextToken();
                switch (t) {
                    case StreamTokenizer.TT_EOL:
                        break;
                    case StreamTokenizer.TT_NUMBER:
//                        System.out.println("nextToken():"+tok.nval);
                        examVector.get((int) tok.nval).exlcusive = true;
                        exclusiveExamsVector.add(examVector.get((int) tok.nval));
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

    void readWeightings(StreamTokenizer tok, boolean fnd) throws IOException {
        //Read InstitutionalWeightings
        int t = tok.nextToken();
        while (t != StreamTokenizer.TT_EOF) {
            switch (t) {
                case StreamTokenizer.TT_EOL:
                    break;
                case StreamTokenizer.TT_WORD:
//                    System.out.println("nextToken():"+tok.sval);
                    if (tok.sval.compareTo("TWOINAROW") == 0) {
                        tok.nextToken();
                        tok.nextToken();
                        twoInARowWeight = (int) tok.nval;
                    } else if (tok.sval.compareTo("TWOINADAY") == 0) {
                        tok.nextToken();
                        tok.nextToken();
                        twoInADayWeight = (int) tok.nval;
                    } else if (tok.sval.compareTo("PERIODSPREAD") == 0) {
                        tok.nextToken();
                        tok.nextToken();
                        periodSpreadWeight = (int) tok.nval;
                    } else if (tok.sval.compareTo("NONMIXEDDURATIONS") == 0) {
                        tok.nextToken();
                        tok.nextToken();
                        nonMixedDurationsWeight = (int) tok.nval;
                    } else if (tok.sval.compareTo("FRONTLOAD") == 0) {
                        tok.nextToken();
                        tok.nextToken();
                        numberOfLargestExams = (int) tok.nval;
                        tok.nextToken();
                        tok.nextToken();
                        numberOfLastPeriods = (int) tok.nval;
                        tok.nextToken();
                        tok.nextToken();
                        frontLoadWeight = (int) tok.nval;
                    }
                    break;
            }
            t = tok.nextToken();
        }
//        System.out.println("twoinarow:"+twoInARowWeight);
//        System.out.println("twoinaday:"+twoInADayWeight);
//        System.out.println("periodSpread:"+periodSpreadWeight);
//        System.out.println("nonMixedDurations:"+nonMixedDurationsWeight);
//        System.out.println("numberOfLargestExams:"+numberOfLargestExams);
//        System.out.println("numberOfLastPeriods:"+numberOfLastPeriods);
//        System.out.println("frontLoadPenalty:"+frontLoadWeight);
    }

    void setExamPriorities() {
        for (Exam e : examVector) {
            e.priority = numberOfTimeSlots;
        }

        for (int i = 0; i < numberOfExams; i++) {
            for (int j = 0; j < numberOfExams; j++) {
                if (i == j) {
                    continue;
                }
                if (afterMatrix[i][j] == 1) {
                    int priority = examVector.get(j).priority;
                    priority -= 1;
                    examVector.get(j).priority = priority;
                }
            }
        }
    }

    void generateDistanceMatrices() {
        for (int i = 0; i < numberOfRooms; i++) {
            for (int j = i; j < numberOfRooms; j++) {
                double long1, long2, lat1, lat2;
                Room rm1 = roomVector.get(i);
                Room rm2 = roomVector.get(j);

                if (rm1.getFaculty().facId == rm2.getFaculty().facId) {
                    roomToRoomDistanceMatrix[i][j] = 0.0;
                    roomToRoomDistanceMatrix[j][i] = 0.0;
                } else {
                    if (rm1.getFaculty().getCampus().campId == rm2.getFaculty().getCampus().campId) {
                        long1 = rm1.getFaculty().longitude;
                        lat1 = rm1.getFaculty().latitude;
                        long2 = rm2.getFaculty().longitude;
                        lat2 = rm2.getFaculty().latitude;

                        roomToRoomDistanceMatrix[i][j] = rm1.distToFaculty
                                + gpsDistance(long1, lat1, long2, lat2) + rm2.distToFaculty;
                        roomToRoomDistanceMatrix[j][i] = rm1.distToFaculty
                                + gpsDistance(long1, lat1, long2, lat2) + rm2.distToFaculty;
                    } else {
                        long1 = rm1.getFaculty().getCampus().longitude;
                        lat1 = rm1.getFaculty().getCampus().latitude;
                        long2 = rm2.getFaculty().getCampus().longitude;
                        lat2 = rm2.getFaculty().getCampus().latitude;

                        //rm2Fac+Fac2Cam+Cam2Cam+Cam2Fac+Fac2rm
                        roomToRoomDistanceMatrix[i][j] = rm1.distToFaculty + rm1.getFaculty().distToCampus
                                + gpsDistance(long1, lat1, long2, lat2) + rm2.getFaculty().distToCampus
                                + rm2.distToFaculty;
                        roomToRoomDistanceMatrix[j][i] = rm1.distToFaculty + rm1.getFaculty().distToCampus
                                + gpsDistance(long1, lat1, long2, lat2) + rm2.getFaculty().distToCampus
                                + rm2.distToFaculty;
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

    double gpsDistance(double lo1, double la1, double lo2, double la2) {
        double R = 6378.137; // Radius of earth in KM
        double dLat = la2 * Math.PI / 180 - la1 * Math.PI / 180;
        double dLon = lo2 * Math.PI / 180 - lo1 * Math.PI / 180;
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(la1 * Math.PI / 180) * Math.cos(la2 * Math.PI / 180)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double d = R * c;
        return d * 1000; // meters          
    }

    void addExam(StreamTokenizer tok) {
        int line = tok.lineno() - 2;
        examDuration = (int) tok.nval;
        if (line < numberOfExams) {
            examVector.add(new Exam(line, examDuration));
        }
    }

    //tCount, examDateAndTime, duration, penalty
//    void addTimeSlot(int id, int d, int p, int dur, int pen) {
    void addTimeSlot(int id, Date date, int dur, int pen) {
        timeslotVector.add(new TimeSlot(id, date, dur, pen));
    }

    void addFaculty(int id, int c, double lon, double lat, int dToC) {
        facultyVector.add(new Faculty(id, c, lon, lat, dToC));
    }

    void addRoom(int c, int id, int f, int dToF, int pen) {
        roomVector.add(new Room(c, id, f, dToF, pen));
//        overallRoomCapacity+=c;
    }

    void addCampus(int id, double lon, double lat) {
        campusVector.add(new Campus(id, lon, lat));
    }

    boolean allocateTimeSlots() {
        try {
            int numberOfColors = 0;
            //        ArrayList<TimeSlot> availableTimeSlots = new ArrayList<>();
//        availableTimeSlots.addAll(timeslotVector);
//        System.out.println("availableTimeSlots.size()="+availableTimeSlots.size());
//        System.out.println("timeslotVector.size()="+timeslotVector.size());
            for (int t = 0; t < timeslotVector.size(); t++) {
//        for (int t = 0; t < availableTimeSlots.size(); t++) {
//            availableTimeSlots.get(t).examList.clear();
                timeslotVector.get(t).examList.clear();
            }
//          coloredGraph  = new LargestDegreeFirstColoring(exGraph).getColoring();
//        coloredGraph  = new GreedyColoring(exGraph).getColoring();
//        int chromaticNumber = new BrownBacktrackColoring(exGraph).getChromaticNumber();            
//          coloredGraph  = new BrownBacktrackColoring(exGraph).getColoring();
//        coloredGraph = new RandomGreedyColoring(exGraph).getColoring();
            coloredGraph = new SaturationDegreeColoring(exGraph).getColoring();

            numberOfColors = coloredGraph.getNumberColors();
            System.out.println("Number of Timeslots = " + numberOfTimeSlots);
            System.out.println("Chromatic Number = " + numberOfColors);
            if (numberOfColors <= numberOfTimeSlots) {
                System.out.println("Solution exist");
            } else {
                System.out.println("Solution doesn't exist");
                return false;
            }
//        coloredGraph = new ColorRefinementAlgorithm(exGraph, new GreedyColoring(exGraph).getColoring()).getColoring();
//        System.out.println("Number Colors = "+coloredGraph.getNumberColors());

//        for (int i = 0; i < examVector.size(); i++) {
            for (Exam exam : examVector) {
                //int allocatedTime = ;
                int allocatedTime = (int) coloredGraph.getColors().get(exam.examId);
//            Exam exam = examVector.get(i);
//            System.out.println("allocatedTime="+allocatedTime);
//            e.setTimeSlot(availableTimeSlots.get(allocatedTime));
//            availableTimeSlots.get(allocatedTime).addExam(e);
//            exam.setTimeSlot(allocatedTime);   
                examVector.get(exam.examId).setTimeSlot(allocatedTime);
                timeslotVector.get(allocatedTime).addExam(exam.examId);
//            System.out.println("Exam "+examVector.get(i).examId+" @ Timeslot "+examVector.get(i).timeslot.id);                        
            }

//        for(int t=0;t<timeslotVector.size();t++)
//        {
//            System.out.println("Timeslot "+timeslotVector.get(t).id+" has "+timeslotVector.get(t).examList.size()+" exams");
//        }
//        for(int t=0;t<availableTimeSlots.size();t++)
//        {
//            System.out.println("Timeslot "+availableTimeSlots.get(t).id+" has "+availableTimeSlots.get(t).examList.size()+" exams");
//        }
            return true;
        } catch (Exception e) {
            System.out.println("Cannot Allocate Timeslots");
            return false;
        }
    }

    boolean allocateRooms(ArrayList rooms) {
        try {
            //        System.out.println("\n\nROOM ALLOCATION FOR NEW SOLUTION:");
            Map<Integer, ArrayList> freeTimeslotRoomMap = new HashMap();
//        ArrayList<Exam> allocatedExams = new ArrayList();
            ArrayList<Exam> unAllocatedExams = new ArrayList();
            ArrayList<Exam> mainUnAllocatedExams = new ArrayList();
            ArrayList<Room> tmpRoomVector = new ArrayList();
            int overallCapacity = 0;
            for (Room rm : roomVector) {
                overallCapacity += rm.capacity;
            }
            tmpRoomVector.addAll(roomVector);
            Collections.sort(tmpRoomVector, new RoomComparator().reversed());
//        for(int r =0; r<tmpRoomVector.size();r++){
//            System.out.println("Room "+tmpRoomVector.get(r).roomId+". Capacity = "+tmpRoomVector.get(r).capacity);
//        } 
            for (TimeSlot tmpT : timeslotVector) {
//        for (int t = 0; t < timeslotVector.size(); t++) {
//            if (timeslotVector.get(t).examList.size() <= 0) {
//                continue;
//            }            
                int enrollment = 0;
                for (Exam e : tmpT.examList) {
                    enrollment += e.studentsCount;
                }

//            System.out.println("Total Room Capacity = "+overallCapacity);
//            System.out.println("Total students assigned to timeslot "+tmpT.id+" = "+enrollment);
                tmpRoomVector.clear();
                tmpRoomVector.addAll(roomVector);

                Collections.sort(tmpRoomVector, new RoomComparator().reversed());

//            TimeSlot tmpT = timeslotVector.get(t);
//            System.out.println("\nNow in Timeslot "+tmpT.id+" having "+tmpT.examList.size()+" exams.");
                ArrayList<Exam> tmpExamVector = new ArrayList();
                tmpExamVector.addAll(tmpT.examList);
                Collections.sort(tmpExamVector, new ExamComparatorByEnrollment().reversed());
//            for(int e =0; e<tmpExamVector.size();e++)
//            {
//                System.out.println("Exam "+tmpExamVector.get(e).examId+". Enrollment = "+tmpExamVector.get(e).studentsCount);                
//            } 

                int e = 0;
                while (e < tmpExamVector.size()) {
                    //System.out.println("Allocating rooms to "+tmpExamVector.size()+" exams...");
                    int r = 0;

                    //System.out.println("Now in Exam "+tmpExamVector.get(e).examId);
                    while (tmpRoomVector.size() > 0 && r < tmpRoomVector.size() && (tmpExamVector.size() > 0) && e < tmpExamVector.size()) {
                        //System.out.println("\nSearching for room to exam "+tmpExamVector.get(0).examId);
                        Exam tmpE = tmpExamVector.get(e);
                        Room tmpR = tmpRoomVector.get(r);
                        if (tmpE.studentsCount <= tmpR.capacity) {
//                        tmpE.setRoom(tmpR);
                            //System.out.println("Exam "+tmpE.examId+" has been set to room "+tmpR.roomId);
//                        allocatedExams.add(tmpE);
                            examVector.get(tmpE.examId).setRoom(tmpR.roomId);//System.out.println("Removing exam "+tmpE.examId);
                            roomVector.get(r).allocateExam(tmpE.examId);
                            tmpExamVector.remove(tmpE);//System.out.println("tmpExamVector now has "+tmpExamVector.size()+" exams");
                            //System.out.println("Removing room "+tmpR.roomId);
                            tmpRoomVector.remove(tmpR);//System.out.println("tmpRoomVector now has "+tmpRoomVector.size()+" rooms");
                        } else {
                            r++;
                        }
                    }

//                if(tmpRoomVector.size()>0){
//                        System.out.print("For timeslot "+t+", freeRoom(s) =");
//                        int freeSpace = 0;
//                        for(int i=0;i<tmpRoomVector.size();i++)
//                        {
//                            System.out.print(tmpRoomVector.get(i).roomId+", ");
//                            int cap = tmpRoomVector.get(i).capacity;
////                            System.out.print("room size :"+cap);
//                            freeSpace+=cap;                            
//                        }
//                        System.out.println("total free Space= "+freeSpace);
//                }                                
                    if (r >= tmpRoomVector.size() && e < tmpExamVector.size()) {
//                    System.out.println("...skipping exam");//+tmpExamVector.get(e).examId+"\n");// for exam "+tmpExamVector.get(0).examId+" with "+tmpExamVector.get(0).studentsCount);
                        unAllocatedExams.add(tmpExamVector.get(e));
//                    System.out.println("Allocated = "+allocatedExams.size());                    
//                    System.out.println("Unallocated = "+unAllocatedExams.size());
                        e++;
                    }
                }

//            System.out.println("****free rooms ="+tmpRoomVector.size()); 
                ArrayList tmpFreeRooms = new ArrayList(tmpRoomVector);
                if (tmpRoomVector.size() > 0) {
                    freeTimeslotRoomMap.put(tmpT.id, tmpFreeRooms);
                }
//            System.out.println("\nfreeTimeslotRoomMap (before re-allocation): ");
//            for(Map.Entry<Integer, ArrayList> entry : freeTimeslotRoomMap.entrySet()){
//                System.out.print("Timeslot "+timeslotVector.get(entry.getKey()).id+" Free Rooms:");
//                for(int i =0;i<entry.getValue().size();i++){
//                    Room rm = (Room)entry.getValue().get(i);
//                    System.out.print(rm.roomId+", ");
//                }
//                System.out.println();
//            }                        

//            int currFreeStuds=0;
//            if(unAllocatedExams.size()>0){
//                System.out.print("unAllocatedExams= ");
//                for(int i=0;i<unAllocatedExams.size();i++)
//                {
////                    System.out.print(unAllocatedExams.get(i).examId+", ");
//                    int studs = unAllocatedExams.get(i).studentsCount;
////                    System.out.print(": no. of studs = "+studs);
//                    currFreeStuds+=studs;
//                }
//                System.out.println("total unallocated studs = "+currFreeStuds);
//            }
                mainUnAllocatedExams.addAll(unAllocatedExams);
                unAllocatedExams.clear();
            }

//        System.out.println("1ST PASS ALLOCATION");
//        for(int e =0; e<examVector.size();e++)
//        {
//            Exam tmpE = (Exam)examVector.get(e);
//            
//            if(!mainUnAllocatedExams.contains(tmpE))
//            {                            
//                System.out.println("Exam "+tmpE.examId
//                    +" has been set to room "+tmpE.room.roomId                                            
//                    +" in timeslot "+tmpE.timeslot.id);              
//            }
//        }
            //RE-ALLOCATING UNALLOCATED EXAMS - version 2
//        System.out.println("RE-ALLOCATING UNALLOCATED EXAMS");
//        System.out.println("List of 2nd pass unallocated exams:");
//        for(Exam ex:mainUnAllocatedExams){
//            System.out.print(ex.examId+" ");
//        }System.out.println();
            boolean examAllocated;
//        boolean cannotAllocateTime;
            Collections.sort(mainUnAllocatedExams, new ExamComparatorByEnrollment().reversed());
            ArrayList<Exam> pass2Unallocated = new ArrayList();
            for (Exam exam1 : mainUnAllocatedExams) {
//        for(int i=0; i<mainUnAllocatedExams.size();i++){              
//            Exam exam1 = mainUnAllocatedExams.get(0);
//            System.out.println("Attempting to allocate exam..."+exam1.examId);

//            for(Integer time : freeTimeslotRoomMap.keySet()){
//                System.out.println("Free Rooms in Timeslot "+time+": ");
//                int j=0;
//                for(ArrayList<Room> roomZ : freeTimeslotRoomMap.values()){
//                    System.out.print(roomZ.get(j++).roomId+", ");
//                }
//                System.out.println();
//            }
                ArrayList<Integer> freeTimeslots = new ArrayList(freeTimeslotRoomMap.keySet());

//            cannotAllocateTime = false;            
                examAllocated = false;
                boolean conflictFound;
                for (int newTimeslot : freeTimeslots) {
//            if(freeTimeslots.size()>0){
//                int timeslotIndex=0;
//                int timeslot = freeTimeslots.get(timeslotIndex);
//                System.out.println("\tTrying Timeslot.."+timeslot); 
                    conflictFound = false;
                    for (Exam exam2 : timeslotVector.get(newTimeslot).examList) {
//                for(int j = 0;j<examVector.size();j++){                
//                    Exam exam2 = examVector.get(j);
//                    if(timeslot!=exam2.timeslot.id)continue; 
//                    int exam1ID = exam1.examId;
//                    int exam2ID = exam2.examId;
//                    int conf = conflictMatrix[exam1ID][exam2ID];
                        if (conflictMatrix[exam1.examId][exam2.examId] != 0) {
//                        System.out.println("\tExam "+(exam1ID+1)+" conflicts with "+" Exam "+(exam2ID+1)
//                                +" @ slots "+timeslot+" & "+exam2.timeslot.id+" resp.\t");
//                        System.out.println("\tconflictMatrix["+(exam1ID+1)+"]["+(exam2ID+1)+"]= "+conf);
                            conflictFound = true;
                            break;
                        }
                    }
                    if (conflictFound) {
                        continue;
                    } else {
//                    boolean roomAllocated=false;
//                    boolean noRoom=false;
                        ArrayList<Room> freeRooms = freeTimeslotRoomMap.get(newTimeslot);
                        Collections.sort(freeRooms, new RoomComparator().reversed());
                        for (Room rm : freeRooms) {
                            Room selectedRoom = roomVector.get(rm.roomId);
                            if (exam1.studentsCount <= selectedRoom.capacity) {
//                            System.out.println("\tRoom "+rm.roomId+" is suitable.");
                                int oldTimeslot = exam1.timeslot.id;
//                            TimeSlot newTimeslot = timeslotVector.get(timeslot);
                                timeslotVector.get(oldTimeslot).removeExam(exam1.examId);
                                timeslotVector.get(newTimeslot).addExam(exam1.examId);

                                examVector.get(exam1.examId).setTimeSlot(newTimeslot);
                                examVector.get(exam1.examId).setRoom(rm.roomId);

                                roomVector.get(rm.roomId).allocateExam(exam1.examId);

//                            System.out.println("\tExam "+exam1.examId+" has been set to room "
//                                    +exam1.room.roomId+" @ Timeslot "+exam1.timeslot.id);
//                            freeTimeslotRoomMap.get(timeslot).remove(randRoomIndex);
                                freeTimeslotRoomMap.get(newTimeslot).remove(0);

//                            mainUnAllocatedExams.remove(exam1);
//                            System.out.println("\tRooms remaining: "+freeTimeslotRoomMap.get(timeslot).size());                            
                                if (freeTimeslotRoomMap.get(newTimeslot).size() <= 0) {
                                    freeTimeslotRoomMap.remove(newTimeslot);
//                                System.out.println("\tNo more rooms in timeslot "+timeslot+". Timeslot removed");
//                                System.out.println("\tTimeslots remaining "+freeTimeslotRoomMap.size());
                                }
                                examAllocated = true;
//                            roomAllocated=true;                            
                            }
                            if (examAllocated) {
                                break;
                            }
//                        else{
////                            System.out.println("\tRoom not suitable. ");
////                            System.out.println("\tExam "+exam1.examId+"'s enrollment = "+exam1.studentsCount+". But Room"+rm.roomId+"'s capacity = "+rm.capacity);
//                            roomIndex++;
////                            System.out.println("\tTrying again...");           
//                        }
                        }
                    }
                    if (examAllocated) {
                        break;
                    }
//                boolean roomAllocated=false;
//                boolean noRoom=false;
//                while(true){
//                    if(roomAllocated||cannotAllocateTime)break;          
//                    while(conflictFound||noRoom){
//                        noRoom=false;
//                        timeslotIndex++;
//                        
//                        if(timeslotIndex>=freeTimeslots.size()){
////                            System.out.println("\tTimeslots Exhausted. Cannot allocate exam"+exam1.examId
////                                    +". Moving to next exam");
//                            
//                            cannotAllocateTime = true;
//                            break;
//                        }
//                        else{
//                            timeslot = freeTimeslots.get(timeslotIndex);
////                            System.out.println("\tTrying next Timeslot = "+timeslot);
//                            conflictFound=false;
//                            for(int j = 0;j<examVector.size();j++){                
//                                Exam exam2 = examVector.get(j);
//                                if(timeslot!=exam2.timeslot.id)continue; 
//                                int exam1ID = exam1.examId;
//                                int exam2ID = exam2.examId;
//                                int conf = conflictMatrix[exam1ID][exam2ID];
//                                if(conf!=0){
////                                    System.out.println("\tExam "+(exam1ID+1)+" conflicts with "+" Exam "
////                                            +(exam2ID+1)+" @ slots "+timeslot+" & "+exam2.timeslot.id+" resp.\t");
////                                    System.out.println("\tconflictMatrix["+(exam1ID+1)+"]["+(exam2ID+1)+"]= "+conf);
//                                    conflictFound=true;
//                                    break;
//                                }                            
//                            }
//                        }
//                    }
////                    System.out.println("\tNo conflict found with all exams on timeslot "+timeslot);              
//                    ArrayList<Room> freeRooms = freeTimeslotRoomMap.get(timeslot);
//                    Collections.sort(freeRooms, new RoomComparator().reversed());
//                    roomAllocated =false;
//                    int roomIndex=0;
//                    while(!roomAllocated){                        
//                        if(roomIndex>=freeRooms.size()){
////                            System.out.println("\tRooms exhausted. Trying next timeslot...");
//                            noRoom=true;
//                            break;
//                        }    
//                        
//                        int selectedRoom = freeRooms.get(roomIndex).roomId;
//                        Room rm = roomVector.get(selectedRoom);
//                        if(exam1.studentsCount<=rm.capacity){
////                            System.out.println("\tRoom "+rm.roomId+" is suitable.");
//                            TimeSlot oldTimeslot = exam1.timeslot;
//                            TimeSlot newTimeslot = timeslotVector.get(timeslot);
//                            timeslotVector.get(oldTimeslot.id).removeExam(exam1.examId);
//                            timeslotVector.get(newTimeslot.id).addExam(exam1.examId);
//                            
//                            examVector.get(exam1.examId).setTimeSlot(newTimeslot);
//                            examVector.get(exam1.examId).setRoom(rm.roomId);
//                            
//                            roomVector.get(rm.roomId).allocateExam(exam1.examId);
//                                                        
////                            System.out.println("\tExam "+exam1.examId+" has been set to room "
////                                    +exam1.room.roomId+" @ Timeslot "+exam1.timeslot.id);
////                            freeTimeslotRoomMap.get(timeslot).remove(randRoomIndex);
//                            freeTimeslotRoomMap.get(timeslot).remove(0);
//                            
//                            mainUnAllocatedExams.remove(exam1);
////                            System.out.println("\tRooms remaining: "+freeTimeslotRoomMap.get(timeslot).size());                            
//                            if(freeTimeslotRoomMap.get(timeslot).size()<=0){
//                                freeTimeslotRoomMap.remove(timeslot);
////                                System.out.println("\tNo more rooms in timeslot "+timeslot+". Timeslot removed");
////                                System.out.println("\tTimeslots remaining "+freeTimeslotRoomMap.size());
//                            }
////                            examAllocated = true;
//                            roomAllocated=true;                            
//                        }
//                        else{
////                            System.out.println("\tRoom not suitable. ");
////                            System.out.println("\tExam "+exam1.examId+"'s enrollment = "+exam1.studentsCount+". But Room"+rm.roomId+"'s capacity = "+rm.capacity);
//                            roomIndex++;
////                            System.out.println("\tTrying again...");           
//                        }
////                        if(examAllocated){
////                            break;
////                        }
//                    }
//                    if(examAllocated){
//                        break;
//                    }
//                }                
//            }                
                }
                if (!examAllocated) {
                    pass2Unallocated.add(exam1);
                }
            }

//        System.out.println("2ND PASS ALLOCATION");
//        for(int e =0; e<examVector.size();e++)
//        {
//            Exam tmpE = (Exam)examVector.get(e);
//            
//            if(!pass2Unallocated.contains(tmpE))
//            {                            
//                System.out.println("Exam "+tmpE.examId
//                    +" has been set to room "+tmpE.room.roomId                                            
//                    +" in timeslot "+tmpE.timeslot.id);                
//            }
//        }
            //SHARED ROOM ALLOCATION
//        System.out.println("THIRD PASS ROOM ALLOCATION WITH SHARED ROOMS");
            ArrayList<Exam> pass3Unallocated = new ArrayList();
//        pass3Unallocated.addAll(thirdPassUnallocated);
//        System.out.println("List of unallocated exams:");
//        for(Exam ex:pass2Unallocated){
//            System.out.print(ex.examId+" ");
//        }System.out.println();
            for (Exam exam1 : pass2Unallocated) {
                boolean allocated = false;
//            System.out.println("Third attempt to find room for \n\tExam "+exam1.examId
//                    +" previously in Timeslot "+exam1.timeslot.id
//                    +" and Room NULL");
                for (TimeSlot timeS : timeslotVector) {

//                System.out.print("\tChecking Timeslot "+timeS.id+" for conflicts....");
                    boolean conflict = false;
                    for (Exam exam2 : timeS.examList) {
//                    System.out.print("\tconflict between exams "+exam1.examId+" and "+exam2.examId+"?...");
                        if (conflictMatrix[exam1.examId][exam2.examId] != 0) {
                            conflict = true;
//                        System.out.print("\tconflict between exams "+exam1.examId+" and "+exam2.examId+"?...");                      
//                        System.out.println(" = "+conflictMatrix[exam1.examId][exam2.examId]);
                            break;
                        }
//                    System.out.println("none found.");
                    }
                    if (!conflict) {
//                    System.out.println("None found in timeslot "+timeS.id);
                        ArrayList<Room> allRooms = new ArrayList();
                        allRooms.addAll(roomVector);
                        Collections.shuffle(allRooms);
                        for (Room room : allRooms) {
//                       if(room.getExams(timeS.id).size()>2)continue;
//                       System.out.print("\tIs room "+room.roomId+" suitable?....");
                            if (exam1.studentsCount <= room.getFreeSeats(timeS.id)) {
//                            System.out.println("Yes. Allocating");
                                int oldTimeslot = exam1.timeslot.id;
                                int newTimeslot = timeS.id;
                                timeslotVector.get(oldTimeslot).removeExam(exam1.examId);
                                timeslotVector.get(newTimeslot).addExam(exam1.examId);

                                examVector.get(exam1.examId).setTimeSlot(newTimeslot);
                                examVector.get(exam1.examId).setRoom(room.roomId);

                                roomVector.get(room.roomId).allocateExam(exam1.examId);
//                            System.out.println("\tExam "+exam1.examId
//                                    +" now in Timeslot "+exam1.timeslot.id
//                                    +" @ Room "+exam1.room.roomId);                                                    
                                allocated = true;
                                break;
                            } else {
//                            System.out.println("No. Trying next room");
                            }
                        }
                    }
                    if (allocated) {
                        break;
                    }
//                System.out.println("\tTrying next timeslot");
                }
                if (!allocated) {
                    pass3Unallocated.add(exam1);
//                System.out.println("\tCannot allocate exam "+exam1.examId+". Must be split");
                }
            }

//        System.out.println("\n****ALLOCATION SUMMARY*****");        
//        
//        for(TimeSlot tS : timeslotVector){
//            System.out.println("Timeslot "+tS.id+":");
//            for(Exam ex: tS.examList){
//                System.out.print("\tExam "+ex.examId);
//                if(ex.room==null){
//                    System.out.println(" has no room");
//                }
//                else{
//                    System.out.println(" is in room "+ex.room.roomId+" with "+(ex.room.capacity-ex.studentsCount)+" free Seats.");                    
//                }                               
//            }            
//        }
//        System.out.println("Room Vector -->");
//        for(Room r:roomVector){
//            System.out.println("Room "+r.roomId);
//            for(TimeSlot t:timeslotVector){
//                System.out.print("\tExams in Timeslot "+t.id+": ");
//                for(Exam e:r.examList){
//                    if(e.timeslot.id==t.id)System.out.print(e.examId+" ");
//                }
//                System.out.println(".");
//            }            
//        }
//        System.out.println("\nfreeTimeslotRoomMap (after reallocation) size = "+freeTimeslotRoomMap.size());
//        for(Map.Entry<Integer, ArrayList> entry : freeTimeslotRoomMap.entrySet()){
//            System.out.print("Timeslot "+timeslotVector.get(entry.getKey()).id+" Free Rooms:");
//            for(int i =0;i<entry.getValue().size();i++){
//                Room rm = (Room)entry.getValue().get(i);
//                System.out.print(rm.roomId+", ");
//            }
//            System.out.println();
//        } 
//        System.out.println("3RD PASS ALLOCATION");
            int freeSeats = 0;
            for (int e = 0; e < examVector.size(); e++) {
                Exam tmpE = (Exam) examVector.get(e);

                if (!pass3Unallocated.contains(tmpE)) {
                    int seats = tmpE.room.capacity - tmpE.studentsCount;
//                System.out.println("Exam "+tmpE.examId
//                    +" has been set to room "+tmpE.room.roomId                                            
//                    +" in timeslot "+tmpE.timeslot.id
//                    +" with "+(seats)+" free seats");
                    freeSeats += seats;
                }
            }

//        System.out.println("UNALLOCATED EXAMS:");
//        Collections.sort(pass3Unallocated, new ExamComparator().reversed());
//        int unAllocatedStudents=0;
//        for(int i=0;i<pass3Unallocated.size();i++){
////            System.out.println((i+1)+" - Exam: "+pass3Unallocated.get(i).examId+". Enrollment = "+pass3Unallocated.get(i).studentsCount);
//            unAllocatedStudents+=pass3Unallocated.get(i).studentsCount;
//        }
////        System.out.println("UNALLOCATED EXAMS = "+pass3Unallocated.size());
////        System.out.println("TOTAL UNALLOCATED STUDENTS = "+unAllocatedStudents);
////        System.out.println("TOTAL UNUSED SEATS = "+freeSeats); 
            return true;
        } catch (Exception e) {
            System.out.println("Cannot Allocate Rooms");
            return false;
        }
    }

    ArrayList<ArrayList<Integer>> generateTimeTableMatrix() {
        ArrayList<Integer> randRooms = new ArrayList<>();
        for (int i = 0; i < numberOfRooms; i++) {
            randRooms.add(i);
        }
        feasible = allocateTimeSlots();
//        allocateRooms(randRooms);

        ArrayList<Integer> tmpSlots = new ArrayList<>();
        for (int i = 0; i < numberOfTimeSlots; i++) {
            tmpSlots.add(i, -1);
        }//(0,0,0,0,0)
        //(-1,-1,-1,-1,-1)
        timetableSolution.clear();
        for (int j = 0; j < examVector.size(); j++) {
            timetableSolution.add(j, new ArrayList<>(tmpSlots));
        }//

        for (int i = 0; i < numberOfExams; i++) {
            for (int j = 0; j < numberOfTimeSlots; j++) {
                if (examVector.get(i).timeslot.id != j) {
                    continue;
                }
                int room = -1;
                if (examVector.get(i).room != null) {
                    room = examVector.get(i).room.roomId;
                }
                timetableSolution.get(i).set(j, room);
            }
        }
//        System.out.println("timetableSolution = "+timetableSolution);
        //timetableSolution = 
        return timetableSolution;
    }

    IntegerMatrixSolution iteraiveForwardSearch() {
        IntegerMatrixSolution<ArrayList<Integer>> currentSolution = new DefaultIntegerMatrixSolution(getListOfExamsPerVariable(), getNumberOfObjectives());
//        IntegerMatrixSolution<ArrayList<Integer>> bestSolution = new DefaultIntegerMatrixSolution(getListOfExamsPerVariable(), getNumberOfObjectives());

        for (int i = 0; i < numberOfExams; i++) {
            ArrayList tmp = new ArrayList(numberOfTimeSlots);
            for (int j = 0; j < numberOfTimeSlots; j++) {
                tmp.add(-1);
            }
            currentSolution.setVariable(i, tmp);
        }

//        boolean stoppingConditionReached = false;
//        boolean conflict = false;
        int selectedTimeslot;
        int selectedRoom;

        PriorityQueue<Exam> examQueue = new PriorityQueue(numberOfExams, new ExamComparatorByPriority());
        PriorityQueue<TimeslotRoomPair> timeslotRoomQueue = new PriorityQueue(new TimeslotRoomPairComparator());
        Map<Integer, PriorityQueue<TimeslotRoomPair>> conflictMap = new HashMap<>();

        for (int t = 0; t < numberOfTimeSlots; t++) {
            for (int r = 0; r < numberOfRooms; r++) {
                timeslotRoomQueue.add(new TimeslotRoomPair(t, r));
            }
        }

        for (int i = 0; i < numberOfExams; i++) {
            conflictMap.put(i, timeslotRoomQueue);
        }
//        System.out.println("Initial timeslotRoomQueue (t,r):");
//        for(TimeslotRoomPair timeRoomPair:timeslotRoomQueue){ 
//                System.out.println("("+timeRoomPair.timeslot1+", "+timeRoomPair.room1+"). Priority = "+timeRoomPair.priority);
//            }
        setExamPriorities();
        examQueue.addAll(examVector);
        for (Room r : roomVector) {
            r.examList.clear();
        }
        for (TimeSlot t : timeslotVector) {
            t.examList.clear();
        }

//        System.out.println("examQueue = "+examQueue);
        //Pick most difficult-to-assign exam
//        System.out.println("currentSolution : "+currentSolution.getVariables());
        while (examQueue.size() > 0) {
//            System.out.println("\n\t\t-->examQueue.size():"+examQueue.size()+"<------");
//            for(Exam e:examQueue){
//                System.out.println("\t\tExam "+e.examId+". Priority = "+e.priority);
//            }            
            int minPriority = examQueue.peek().priority;
            ArrayList<Exam> tmpExams = new ArrayList();
            for(Exam ex:examQueue){
                if(ex.priority<=minPriority){
                    tmpExams.add(ex);
                }
            }
            int examsCount = tmpExams.size();
            int randE;
            if(examsCount<=1){
                randE = 0;
            }else{
                randE = new Random().nextInt(examsCount-1);
            } 
            Exam exam1 = tmpExams.get(randE);
            examQueue.remove(exam1);
//            Exam exam1 = examQueue.poll();
            int studsCount = exam1.studentsCount;
            System.out.println("Exam " + exam1.examId + " polled with " + studsCount + " students. " + examQueue.size() + " exams remaining.");

//            System.out.print("\t timeslotRoomQueue ("+conflictMap.get(exam1.examId).size()+"):\n\t\t");
//            for(TimeslotRoomPair trP:conflictMap.get(exam1.examId)){
//                System.out.println("("+trP.timeslot1+","+trP.room1+")");
//            }

//            TimeslotRoomPair timeRoomPair = conflictMap.get(exam1.examId).poll();
            
            int minRank = conflictMap.get(exam1.examId).peek().rank;
            ArrayList<TimeslotRoomPair> tmpValues = new ArrayList();
            for(TimeslotRoomPair tmRmPr : conflictMap.get(exam1.examId)){
                if(tmRmPr.rank<=minRank){
                    tmpValues.add(tmRmPr);
                }
            }  
            System.out.println("tmpValues.size()= "+tmpValues.size());
            
            int size = tmpValues.size(); 
            int randV ;
            if(size<=1){
                randV = 0;
            }else{
                randV = new Random().nextInt(size-1);
            }                
            TimeslotRoomPair timeRoomPair = tmpValues.get(randV);
            conflictMap.get(exam1.examId).remove(timeRoomPair);

            selectedTimeslot = timeRoomPair.timeslot1;
            selectedRoom = timeRoomPair.room1;

            int rmCap = roomVector.get(selectedRoom).capacity;
            int frSeats = roomVector.get(selectedRoom).getFreeSeats(selectedTimeslot);
            System.out.println("\tSelectedTimeslot = " + timeslotVector.get(selectedTimeslot).id);
            System.out.println("\tselectedRoom = " + roomVector.get(selectedRoom).roomId + " has " + roomVector.get(selectedRoom).getExams(selectedTimeslot).size()
                    + " exams and " + frSeats + " free seats and capacity " + rmCap);

            //Look for Room Conflicts.             
//            System.out.println("\tSelectedRoom Cap = "+rCap);
            ArrayList<Exam> examsInSelRoomAtSelTime = roomVector.get(selectedRoom).getExams(selectedTimeslot);
            if (studsCount <= rmCap) {
                if (studsCount > frSeats) {
                    System.out.println("\tEvicting " + examsInSelRoomAtSelTime.size() + " exams in room " + selectedRoom);
                    for (Exam e : examsInSelRoomAtSelTime) {
                        currentSolution.getVariable(e.examId).set(e.timeslot.id, -1);
                        timeslotVector.get(e.timeslot.id).removeExam(e.examId);
                        roomVector.get(e.room.roomId).deAllocateExam(e.examId);
                        examVector.get(e.examId).setTimeSlot(-1);
                        examVector.get(e.examId).setRoom(-1);

                        examVector.get(e.examId).priority = Integer.MIN_VALUE;
                        examQueue.offer(examVector.get(e.examId));

                        boolean exists = false;
                        for (Conflict con : timeRoomPair.conflicts) {
                            if (con.conflictingExam.examId == e.examId) {
                                con.evictionCount++;
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            timeRoomPair.conflicts.add(new Conflict(e, 1));
                        }
                        timeRoomPair.computeRank();
                        conflictMap.get(e.examId).offer(timeRoomPair);
                    }
                    System.out.println("\tFree seats now = " + roomVector.get(selectedRoom).getFreeSeats(selectedTimeslot));
                } else {
                    //Squat   
                }
            } else {
                //Ban Room
                timeRoomPair.rank = Integer.MAX_VALUE;
                conflictMap.get(exam1.examId).offer(timeRoomPair);
                examQueue.offer(exam1);
                System.out.println("\tRoom " + selectedRoom + " @ timeslot " + selectedTimeslot + " with capacity = " + rmCap + " not suitable for " + studsCount + " students");
                continue;
            }

            ArrayList<Exam> potentialConflict = new ArrayList(timeslotVector.get(selectedTimeslot).examList);
            for (Exam exam2 : potentialConflict) {
                if (exam1.examId == exam2.examId) {
                    continue;
                }
                if (conflictMatrix[exam1.examId][exam2.examId] != 0) {
//                  System.out.println("\t\tConflict with exam "+j+" found.");

                    boolean exists = false;
                    for (Conflict con : timeRoomPair.conflicts) {
                        if (con.conflictingExam.examId == exam2.examId) {
                            con.evictionCount++;
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        timeRoomPair.conflicts.add(new Conflict(exam2, 1));
                    }
                    timeRoomPair.computeRank();
                    conflictMap.get(exam1.examId).offer(timeRoomPair);

                    System.out.println("\t\tDeallocating exam " + exam2.examId);
                    currentSolution.getVariable(exam2.examId).set(exam2.timeslot.id, -1);

                    timeslotVector.get(exam2.timeslot.id).removeExam(exam2.examId);
                    roomVector.get(exam2.room.roomId).deAllocateExam(exam2.examId);
                    examVector.get(exam2.examId).setTimeSlot(-1);
                    examVector.get(exam2.examId).setRoom(-1);
                    examVector.get(exam2.examId).priority = Integer.MIN_VALUE;
                    examQueue.offer(examVector.get(exam2.examId));
//                    conflict = true;
                }
            }

            //Allocate
            boolean flag = roomVector.get(selectedRoom).allocateExam(exam1.examId);
            if (!flag) {
                timeRoomPair.rank = Integer.MAX_VALUE;
//                timeslotRoomQueue.offer(timeRoomPair);
                conflictMap.get(exam1.examId).offer(timeRoomPair);
                exam1.priority = Integer.MIN_VALUE;
                examQueue.offer(exam1);
                System.out.println("\tRoom allocation failed");
                continue;
            }
            currentSolution.getVariable(exam1.examId).set(selectedTimeslot, selectedRoom);
            examVector.get(exam1.examId).setTimeSlot(selectedTimeslot);
            examVector.get(exam1.examId).setRoom(selectedRoom);
            timeslotVector.get(selectedTimeslot).addExam(exam1.examId);

            for (Exam e : examQueue) {
                e.priority -= 1;
            }
//            conflictMap.get(exam1.examId).offer(timeRoomPair);
            System.out.println("\tAllocated to timeslot " + exam1.timeslot.id + " room " + exam1.room.roomId);//+" value priority = "+timeRoomPair.priority);
        }
        for (int i = 0; i < currentSolution.getNumberOfVariables(); i++) {
            ArrayList exam = currentSolution.getVariable(i);
            System.out.println("Exam " + i + " is in timeslot " + getTimeslot(exam) + " and room " + getRoom(exam));
        }
        return currentSolution;
    }

    @Override
    public IntegerMatrixSolution<ArrayList<Integer>> createSolution() {
//        timetableSolution = generateTimeTableMatrix();
//
//        if (feasible) {
//            DefaultIntegerMatrixSolution solution = new DefaultIntegerMatrixSolution(getListOfExamsPerVariable(), getNumberOfObjectives());
////          System.out.println("Creating solution...");
//            for (int i = 0; i < getLength(); i++) {
//                solution.setVariable(i, timetableSolution.get(i));
//            }
        //      System.out.println("new solution:"+solution.getVariables());
        return iteraiveForwardSearch();
//        } else {
//            return null;
//        }
    }

    @Override
    public void evaluate(IntegerMatrixSolution<ArrayList<Integer>> solution) {
        double proximityFitness = evaluateProximityFitness(solution);
        double movementFitness = evaluateMovementFitness(solution);                 //movement cost
        double roomUtilizationFitness = evaluateRoomUtilizationFitness(solution);   //room cost

        double compositeFitness = proximityFitness + movementFitness + roomUtilizationFitness;
        int itc2007Fitness = evaluateITC2007Fitness(solution);

        this.evaluateConstraints(solution);
//        solution.setObjective(0, proximityFitness);
        solution.setObjective(1, movementFitness);
//        solution.setObjective(0, roomUtilizationFitness); 
//        solution.setObjective(0, compositeFitness);
        solution.setObjective(0, itc2007Fitness);
//        solution.setObjective(1, compositeFitness);

        System.out.println("Objective(0) =" + solution.getObjective(0) + " Conflicts = " + solution.getAttribute("CONFLICTS"));
//        System.out.println("Solution = "+solution);
//        System.out.println("CONFLICTS =" + solution.getAttribute("CONFLICTS")+"\tObjective(1) =" + solution.getObjective(1)+"\tObjective(0) =" + 
//                solution.getObjective(0));                
    }

    public double evaluateProximityFitness(IntegerMatrixSolution<ArrayList<Integer>> solution) {
        //proximity constraint 
        double proximity = 0.0;
        for (int i = 0; i < solution.getNumberOfVariables(); i++) {
            int slot1 = getTimeslot(solution.getVariable(i));
            if (slot1 == -1) {
                return Integer.MAX_VALUE;
            }
            for (int j = 0; j < solution.getNumberOfVariables(); j++) {
                if (i == j) {
                    continue;
                }
                int slot2 = getTimeslot(solution.getVariable(j));
                if (slot2 == -1) {
                    return Integer.MAX_VALUE;
                }
                if (conflictMatrix[i][j] != 0) {
                    double prox = Math.pow(2, (5 - Math.abs(slot1 - slot2)));
                    double diffFactor = computedDifficulty.get(i) + computedDifficulty.get(j);
                    proximity += (prox / diffFactor) * conflictMatrix[i][j];
                }
            }
        }
        return proximity / studentMap.size();
    }

    public double evaluateMovementFitness(IntegerMatrixSolution<ArrayList<Integer>> solution) {
        //movement constraint
        double movementCost = 0.0;
//        System.out.println("\n\nEVALUATING MOVEMENT COST for solution:");
//        System.out.println(solution.getVariables());
        for (Map.Entry<Integer, Student> currStudent : studentMap.entrySet()) {
//            System.out.println("Student="+currStudent.getKey());
            for (int e1 = 0; e1 < currStudent.getValue().examList.size(); e1++) {
                Exam cExam = examVector.get(currStudent.getValue().examList.get(e1).examId);

                for (int e2 = e1; e2 < currStudent.getValue().examList.size(); e2++) {
                    if (e1 == e2) {
                        continue;
                    }
                    Exam nExam = examVector.get(currStudent.getValue().examList.get(e2).examId);
//                    if(cExam.room==null)continue;int rm1 = cExam.room.roomId;
//                    if(nExam.room==null)continue;int rm2 = nExam.room.roomId;
//                    
//                    fitness2+=roomToRoomDistanceMatrix[rm1-1][rm2-1];
                    int room1 = getRoom(solution.getVariable(cExam.examId));
                    int room2 = getRoom(solution.getVariable(nExam.examId));
                    if (room1 == -1 || room2 == -1) {
                        return Double.MAX_VALUE;
                    }
                    if (room1 > -1 && room2 > -1) {
//                        System.out.println("room="+room1+"\nroom2="+room2);
                        double currCost = roomToRoomDistanceMatrix[room1][room2];
//                        System.out.println("Movement cost btw exam "+cExam.examId+" in room "+(room1+1)+" to exam "+nExam.examId
//                            +" in room "+(room2+1)+" is "+currCost);
                        movementCost += currCost;

                    }
                }
            }
        }
        return movementCost / studentMap.size();
    }

    public double evaluateRoomUtilizationFitness(IntegerMatrixSolution<ArrayList<Integer>> solution) {
        //room under-utilization and over-utilization constraint 
        int roomCap = 0, studSize = 0;
        double underUtilization = 0.0;
        double overUtilization = 0.0;

        for (int i = 0; i < solution.getNumberOfVariables(); i++) {
            ArrayList<Integer> exam = solution.getVariable(i);
            int room = getRoom(exam);
            if (room == -1) {
                return Double.MAX_VALUE;
            }
            roomCap = roomVector.get(room).capacity;
            studSize = examVector.get(i).studentsCount;
            if (studSize < roomCap) {
                underUtilization += (roomCap - studSize);
            } else if (studSize > roomCap) {
                overUtilization += (studSize - roomCap);
            }
//            else{               
//                overUtilization+=studSize;
//            }
        }
        return (underUtilization + overUtilization) / roomVector.size();
    }

    public int evaluateITC2007Fitness(IntegerMatrixSolution<ArrayList<Integer>> solution) {
        //TwoInARow,TwoInADay,PeriodSpread
        TimeSlot slot1, slot2;
        int twoInARowCount = 0;
        int twoInADayCount = 0;
        int periodSpreadCount = 0;
        for (Student currStudent : studentMap.values()) //        for(int i=0; i < studentMap.size();i++)
        {
            ArrayList<Exam> examList = currStudent.examList;
            for (int j = 0; j < examList.size(); j++) {
                for (int k = 0; k < examList.size(); k++) {
                    if (j == k) {
                        continue;
                    }
//                    System.out.print("-->"+getTimeslot(solution.getVariable(currStudent.getValue().examList.get(j).examId - 1))+" ");
                    int timeS1 = getTimeslot(solution.getVariable(examList.get(j).examId));
                    int timeS2 = getTimeslot(solution.getVariable(examList.get(k).examId));
                    if (timeS1 == -1 || timeS2 == -1) {
                        return Integer.MAX_VALUE;
                    }
                    slot1 = timeslotVector.get(timeS1);
                    slot2 = timeslotVector.get(timeS2);

//                    if (slot1.day == slot2.day) {
                    if (slot1.dateAndTime.getMonth() == slot2.dateAndTime.getMonth()
                            && slot1.dateAndTime.getDay() == slot2.dateAndTime.getDay()) {
                        if (Math.abs(slot1.id - slot2.id) == 1) {
                            twoInARowCount++;
                        } else if (Math.abs(slot1.id - slot2.id) > 1) {
                            twoInADayCount++;
                        }
                    }
                    if (Math.abs(slot1.id - slot2.id) < spreadGap) {
                        periodSpreadCount++;
                    }
                }
            }
        }
        int twoInARowPenalty = twoInARowWeight * twoInARowCount;
        int twoInADayPenalty = twoInADayWeight * twoInADayCount;
        int periodSpreadPenalty = periodSpreadWeight * periodSpreadCount;

        //NonMixedDuration, Room Penalty & Timeslot Penalty
        Map<Integer, Set> roomDurationMap = new HashMap<>();
        int nonMixedDurationPenalty = 0;
        int timeslotPenalty = 0;
        int roomPenalty = 0;
        for (int i = 0; i < timeslotVector.size(); i++) {
            for (int j = 0; j < solution.getNumberOfVariables(); j++) {
                //if(getTimeslot(solution.getVariable(j))==timeslotVector.get(i).id-1)
                if (solution.getVariable(j).get(i) != 0) {
                    int room = getRoom(solution.getVariable(j));
                    if (room == -1) {
                        nonMixedDurationPenalty = Integer.MAX_VALUE;
                        timeslotPenalty = Integer.MAX_VALUE;
                        roomPenalty = Integer.MAX_VALUE;
                        break;
                    }
//                    System.out.println("room="+room);
                    int duration = examVector.get(j).examDuration;

                    if (roomDurationMap.containsKey(room)) {
                        roomDurationMap.get(room).add(duration);
                    } else {
                        roomDurationMap.put(room, new HashSet(duration));
                    }

                    //Timeslot Penalty
                    if (timeslotVector.get(i).penalty != 0) {
                        timeslotPenalty += timeslotVector.get(i).penalty;
                    }

                    //Room Penalty
                    if (room >= 0) {
                        if (roomVector.get(room).penalty != 0) {
                            roomPenalty += roomVector.get(room).penalty;
                        }
                    }
                }
            }

            ArrayList<Set> durations = new ArrayList<>();
            durations.addAll(roomDurationMap.values());
            for (int j = 0; j < durations.size(); j++) {
                nonMixedDurationPenalty += (durations.get(j).size() - 1) * nonMixedDurationsWeight;
            }
        }

        //Frontload
        int frontLoadViolation = 0;

        for (int i = 0; i < examVector.size(); i++) {
            if (examVector.get(i).studentsCount >= numberOfLargestExams) {
                largestExams.add(examVector.get(i).examId);
            }
        }

        for (int i = 0; i < solution.getNumberOfVariables(); i++) {
            int timeslot = getTimeslot(solution.getVariable(i));
            if (timeslot == -1) {
                frontLoadViolation = Integer.MAX_VALUE;
                break;
            }
            if (largestExams.contains(i) && timeslot > (timeslotVector.size() - numberOfLastPeriods)) {
                frontLoadViolation++;
            }
        }
        int frontLoadPenalty = frontLoadViolation * frontLoadWeight;

        System.out.println("\n\ntwoInARowPenalty = " + twoInARowPenalty);
        System.out.println("twoInADayPenalty = " + twoInADayPenalty);
        System.out.println("periodSpreadPenalty = " + periodSpreadPenalty);
        System.out.println("nonMixedDurationPenalty = " + nonMixedDurationPenalty);
        System.out.println("frontLoadPenalty = " + frontLoadPenalty);
        System.out.println("timeslotPenalty = " + timeslotPenalty);
        System.out.println("roomPenalty = " + roomPenalty);
        System.out.println("\n");
        //ITC2007 Objective Function
        return twoInARowPenalty + twoInADayPenalty + periodSpreadPenalty
                + nonMixedDurationPenalty + frontLoadPenalty + timeslotPenalty + roomPenalty;
    }

    public void evaluateConstraints(IntegerMatrixSolution<ArrayList<Integer>> solution) {
        //conflicts
        int conflicts = 0;
        for (int i = 0; i < solution.getNumberOfVariables(); i++) {
            for (int j = 0; j < solution.getNumberOfVariables(); j++) {
                if (i == j) {
                    continue;
                }

                int slot1 = 0, slot2 = 0;

                ArrayList<Integer> x = solution.getVariable(i);
                slot1 = getTimeslot(x);
//                while (x.get(slot1) == 0) {
//                    slot1++;
//                }

                ArrayList<Integer> y = solution.getVariable(j);
                slot2 = getTimeslot(y);
//                while (y.get(slot2) == 0) {
//                    slot2++;
//                }
                if (slot1 == -1 || slot2 == -1) {
                    conflicts = Integer.MAX_VALUE;
                    break;
                }
                if (conflictMatrix[i][j] != 0) {
                    if (slot1 == slot2) {
                        conflicts++;
//                        System.out.println("Exam "+i+" conflicts with "+j);
                    }
                }
            }
        }//System.out.println("conflicts="+conflicts);

        //room under-utilization constraint
        int roomOccupancyViolation = 0;
        int examCapacity = 0;
        int roomCapacity = 0;

        for (int i = 0; i < solution.getNumberOfVariables(); i++) {
            ArrayList<Integer> exam = solution.getVariable(i);

            for (int j = 0; j < exam.size(); j++) {
                if (exam.get(j) == 0) {
                    continue;
                }
                int room = exam.get(j);

                if (room != -1) {
                    examCapacity = examVector.get(i).studentsCount;
                    roomCapacity = roomVector.get(room - 1).capacity;

                    //roomCapacity=examVector.get(i).room.capacity;
                    if (examCapacity > roomCapacity) {
                        roomOccupancyViolation++;
                    }
                }
            }
        }//System.out.println("roomOccupancyViolation="+roomOccupancyViolation);

        //Timeslot Utilisation
        int timeslotUtilisationViolation = 0;
        int timeslotDuration = 0;
        int examDuration = 0;
        for (int i = 0; i < solution.getNumberOfVariables(); i++) {
            ArrayList<Integer> exam = solution.getVariable(i);

            for (int j = 0; j < exam.size(); j++) {
                if (exam.get(j) == 0) {
                    continue;
                }
                timeslotDuration = timeslotVector.get(j).duration;
                examDuration = examVector.get(i).examDuration;
                if (timeslotDuration < examDuration) {
                    timeslotUtilisationViolation++;
                }
            }
        }//System.out.println("timeslotUtilisationViolation="+timeslotUtilisationViolation);

        //PeriodOrdering
        int periodOrderingViolation = 0;
        for (int i = 0; i < solution.getNumberOfVariables(); i++) {
            ArrayList<Integer> x = solution.getVariable(i);
            int k = getTimeslot(x);
            for (int j = 0; j < solution.getNumberOfVariables(); j++) {
                ArrayList<Integer> y = solution.getVariable(i);
                int l = getTimeslot(y);
//                for (k = 0; k < solution.getVariable(i).size(); k++) {
//                    if (solution.getVariable(i).get(k) != 0) {
//                        break;
//                    }
//                }
//                for (l = 0; l < solution.getVariable(i).size(); l++) {
//                    if (solution.getVariable(j)
//                            .get(l) != 0) {
//                        break;
//                    }
//                }

                if (exclusionMatrix[i][j] != 0) {
                    if (k == l) {
                        periodOrderingViolation++;
                    }
                }
                if (afterMatrix[i][j] != 0) {
                    if (k < l) {
                        periodOrderingViolation++;
                    }
                }
                if (coincidenceMatrix[i][j] != 0) {
                    if (conflictMatrix[i][j] != 0) {
                        break;
                    }
                    if (k != l) {
                        periodOrderingViolation++;
                    }
                }
            }
        }//System.out.println("periodOrderingViolation="+periodOrderingViolation);

        //RoomConstraints
        int roomConstraintViolation = 0;
        for (int i = 0; i < exclusiveExamsVector.size(); i++) {
            int exclusiveExam = exclusiveExamsVector.get(i).examId;
            int k, exclusiveRoom = -1;
            for (k = 0; k < solution.getVariable(exclusiveExam).size(); k++) {
                if (solution.getVariable(exclusiveExam).get(k) != 0) {
                    exclusiveRoom = solution.getVariable(exclusiveExam).get(k);
                    break;
                }
            }

            for (int j = 0; j < solution.getNumberOfVariables(); j++) {
                if (j == exclusiveExam) {
                    continue;
                }
                int r, room = -1;
                for (r = 0; r < solution.getVariable(j).size(); r++) {
                    if (solution.getVariable(j).get(r) != 0) {
                        room = solution.getVariable(j).get(r);
                        break;
                    }
                }
                if (exclusiveRoom == room) {
                    roomConstraintViolation++;
                }
            }
        }//System.out.println("roomConstraintViolation="+roomConstraintViolation);

        solution.setAttribute("CONFLICTS", conflicts);
        solution.setAttribute("ROOM_UTILIZATION_PENALTY", roomOccupancyViolation);
        solution.setAttribute("TIMESLOT_PENALTY", timeslotUtilisationViolation);
        solution.setAttribute("TIMESLOT_ORDERING_PENALTY", periodOrderingViolation);
        solution.setAttribute("ROOM_CONSTRAINT_PENALTY", roomConstraintViolation);
    }

    @Override
    public int getLength() {
        return numberOfExams;
    }

    @Override
    public ArrayList<Integer> getListOfExamsPerVariable() {
        ArrayList<Integer> list = new ArrayList<>(Arrays.asList(numberOfExams));
        return list;
    }

    public int getTimeslot(ArrayList<Integer> exam) {

        for (int i = 0; i < exam.size(); i++) {
            if (exam.get(i) != -1) {
                return i;
            }
        }
//        System.out.println("Timeslot not found for exam "+exam.toString());
        return -1;
    }

    public int getRoom(ArrayList<Integer> exam) {
        for (int i = 0; i < exam.size(); i++) {
            if (exam.get(i) != -1) {
                return exam.get(i);
            }
        }
//        System.out.println("Room not found for exam "+exam.toString());
        return -1;
    }

    @Override
    public int[] getRoomCapacities() {
        int[] roomCapacities = new int[roomVector.size()];
        for (int i = 0; i < roomVector.size(); i++) {
            roomCapacities[i] = roomVector.get(i).capacity;
        }
        return roomCapacities;
    }

    @Override
    public int[] getExamEnrollments() {
        int[] examEnrollments = new int[examVector.size()];
        for (int i = 0; i < examVector.size(); i++) {
            examEnrollments[i] = examVector.get(i).studentsCount;
        }
        return examEnrollments;
    }

    @Override
    public int getNumberOfTimeslots() {
        return numberOfTimeSlots;
    }

    public ArrayList getLargestExams() {
        return largestExams;
    }
}
