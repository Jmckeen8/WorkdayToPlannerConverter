package jsonToXml;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class jsonIN {
	
	public void readJSON(Schedb schedb) {
		JSONParser jsonParser = new JSONParser();
		
		try (FileReader reader = new FileReader("prod-data.json")){
			Object obj = jsonParser.parse(reader);
			
			JSONObject reportEntry = (JSONObject) obj;
			
			JSONArray courseList = (JSONArray) reportEntry.get("Report_Entry");
			
			processJSON(courseList, schedb);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
	}
	
	public void processJSON(JSONArray courseList, Schedb schedb) {
		boolean cont = true;
		int index = 0;
		while (cont) {
			
			JSONObject currSection = (JSONObject) courseList.get(index);
			JSONArray allSectionsThisCourseTerm = new JSONArray(); //array for all sections of a given course in a single term
			allSectionsThisCourseTerm.add(currSection);
			
			String currSectionCourseSection = (String) currSection.get("Course_Section");  //Course_Section from json
			String currSectionCourseSubjNum = currSectionCourseSection.substring(0, currSectionCourseSection.indexOf("-"));
			String currSectionAcademicPeriod = (String) currSection.get("Starting_Academic_Period_Type");
			
			int findOthersAddition = 1;  //place of next section we're checking
			if(index < courseList.size() - 1) {
				boolean findOthers = true;
				
				while(findOthers) {
					JSONObject nextSection = (JSONObject) courseList.get(index+findOthersAddition);
					
					String nextSectionCourseSection = (String) nextSection.get("Course_Section");  //Course_Section from json
					String nextSectionCourseSubjNum = nextSectionCourseSection.substring(0, nextSectionCourseSection.indexOf("-"));
					String nextSectionAcademicPeriod = (String) nextSection.get("Starting_Academic_Period_Type");
					
					if(nextSectionCourseSubjNum.equals(currSectionCourseSubjNum) && nextSectionAcademicPeriod.equals(currSectionAcademicPeriod)) {
						allSectionsThisCourseTerm.add(nextSection);
						findOthersAddition++;
					} else {
						findOthers = false;
					}
				}
			}
			
			//System.out.println(allSectionsThisCourseTerm);
			
			
			//at this point allSectionsThisCourseThisTerm is populated
			
			ArrayList<dept> departments = schedb.getDepartments();
			
			String currSecDept = currSectionCourseSection.substring(0, currSectionCourseSection.indexOf(" "));
			dept dummyDept = new dept(currSecDept);
			
			int subjectIndexInDepartments = departments.indexOf(dummyDept);  //index of department of course in departments list
			dept department = departments.get(subjectIndexInDepartments);  //specific department object
			
			//get details for course object
			String courseNum = currSectionCourseSubjNum.substring(currSectionCourseSubjNum.indexOf(" ") + 1, currSectionCourseSubjNum.length());
			
			String courseTitleFull = (String) currSection.get("Course_Title");
			String courseName = courseTitleFull.substring(courseTitleFull.indexOf("-") + 2, courseTitleFull.length());
			String courseDescRaw = (String) currSection.get("Course_Description");
			String courseDesc;
			if(courseDescRaw != null) {
				courseDesc = courseDescRaw.replaceAll("\\<[^>]*>", " ");
				courseDesc = courseDesc.replace("&amp;", "&");
				courseDesc = courseDesc.replace("â€™", "'");
				courseDesc = courseDesc.replace("â€˜", "'");
				courseDesc = courseDesc.replace("&#39;", "'");
				courseDesc = courseDesc.replace("â€œ", "\"");
				courseDesc = courseDesc.replace("â€�", "\"");
				courseDesc = courseDesc.replace("Â", " ");
				courseDesc = courseDesc.replace("â€”", "—");
				courseDesc = courseDesc.replace("â€“", "–");
				courseDesc = courseDesc.replace("&#43;", "+");
				//courseDesc = courseDescRaw;
			}else {
				courseDesc = courseDescRaw;
			}
			double courseCredits =  Double.parseDouble((String)currSection.get("Credits"));
			
			course newCourse = new course(courseNum, courseName, courseDesc, courseCredits);
			if(department.getCourses().contains(newCourse)) {
				newCourse = department.getCourses().get(department.getCourses().indexOf(newCourse));
			}else {
				department.getCourses().add(newCourse);	
			}
			
			
			//section details
			
			//lists to gather like sections
			ArrayList<section> lectures = new ArrayList<section>();
			ArrayList<section> conferences = new ArrayList<section>();
			ArrayList<section> labs = new ArrayList<section>();
			
			for (int i = 0; i < allSectionsThisCourseTerm.size(); i++) { //for all course sections
				JSONObject thisSection = (JSONObject) allSectionsThisCourseTerm.get(i);
				
				//section number
				String thisCourseSectionFull = (String) thisSection.get("Course_Section");
				String thisSectionNum;
				if(thisCourseSectionFull.contains("-Quiz")) {
					thisSectionNum = thisCourseSectionFull.substring(thisCourseSectionFull.indexOf("-") + 1, thisCourseSectionFull.indexOf("-", thisCourseSectionFull.indexOf("-") + 6) - 1);
				}else if(thisCourseSectionFull.contains("-Y")) {
					thisSectionNum = thisCourseSectionFull.substring(thisCourseSectionFull.indexOf("-") + 1, thisCourseSectionFull.indexOf("-", thisCourseSectionFull.indexOf("-") + 6) - 1);
				}else if(thisCourseSectionFull.contains("GPS:")) {
					thisSectionNum = thisCourseSectionFull.substring(thisCourseSectionFull.indexOf("-") + 1, thisCourseSectionFull.indexOf(":") - 6);
				}else {
					thisSectionNum = thisCourseSectionFull.substring(thisCourseSectionFull.indexOf("-") + 1, thisCourseSectionFull.indexOf("-", thisCourseSectionFull.indexOf("-") + 1) - 1);
				}
				
				if(thisSectionNum.contains("(")) {
					thisSectionNum = thisSectionNum.substring(0, thisSectionNum.indexOf("(") - 1);
				}
				
				//seats available and total seats
				String enrolledCapacityString = (String) thisSection.get("Enrolled_Capacity");
				int enrolled = Integer.parseInt(enrolledCapacityString.substring(0, enrolledCapacityString.indexOf("/")));
				int capacity = Integer.parseInt(enrolledCapacityString.substring(enrolledCapacityString.indexOf("/") + 1, enrolledCapacityString.length()));
				int availableSeats = capacity - enrolled;
				
				//waitlist capacity and actual
				String WaitlistCapacityString = (String) thisSection.get("Waitlist_Waitlist_Capacity");
				int waitlistActual = Integer.parseInt(WaitlistCapacityString.substring(0, WaitlistCapacityString.indexOf("/")));
				int waitlistTotal = Integer.parseInt(WaitlistCapacityString.substring(WaitlistCapacityString.indexOf("/") + 1, WaitlistCapacityString.length()));
				
				//part_of_term
				String term = (String) thisSection.get("Starting_Academic_Period_Type");
				String termActual;
				if(term.equals("Fall")) {
					termActual = "A Term, B Term";
				} else if(term.equals("Spring")){
					termActual = "C Term, D Term";
				}else {
					termActual = term;
				}
				
				//Section cluster (if exists)
				section newSection;
				if(thisSection.get("Student_Course_Section_Cluster") != null) {
					String fullClusterString = (String) thisSection.get("Student_Course_Section_Cluster");
					String clusterLetter = fullClusterString.substring(fullClusterString.indexOf("(") + 1, fullClusterString.indexOf(")"));
					newSection = new section(00000, thisSectionNum, capacity, availableSeats, waitlistTotal, waitlistActual, "202201", termActual, clusterLetter);
				}else {
					newSection = new section(00000, thisSectionNum, capacity, availableSeats, waitlistTotal, waitlistActual, "202201", termActual);
				}
				
				//build new section object ^^^^
				
				//add GPS boolean
				if(currSecDept.equals("FY") && (courseNum.equals("1100") || courseNum.equals("1101"))) {
					newSection.setGPS(true);
				}else {
					newSection.setGPS(false);
				}
				
				
				//COMMON PERIOD ATTRIBUTES
				
				//type
				String workdayType = (String) thisSection.get("Instructional_Format");
				String plannerType = "";
				if(workdayType.equals("Discussion")) {
					plannerType = "Conference";
				}else if(workdayType.equals("Laboratory")) {
					plannerType = "Lab";
				}else if(workdayType.equals("Workshop")) {
					plannerType = "Physical Education";
				}else if(workdayType.equals("Experimental")) {
					plannerType = "Other";
				}else {
					plannerType = workdayType;  //Lecture and Seminar are OK with Planner
				}
				//use plannerType
				
				//professor
				String thisProfessor = "";
				if(thisSection.get("Instructors") == null) {
					thisProfessor = "Not Assigned";
				}else {
					thisProfessor = (String) thisSection.get("Instructors");
				}
				//use thisProfessor
				
				//need to build periods
				String allPeriodsString = (String) thisSection.get("Section_Details");
				if (allPeriodsString == null) {
					allPeriodsString = "Unknown|";  //for some reason at least one class has no section details??
				}
				String[] allPeriods = allPeriodsString.split(";");
				for (String period : allPeriods) {
					String[] periodDetails = period.split("\\|");  //index 0: location, index 1: days of week, index 2: time
					
					//location
					String thisLocation = periodDetails[0];
					
					boolean monday = false, tuesday = false, wednesday = false, thursday = false, friday = false;
					Date startTime = null;
					Date endTime = null;
					
					
					if(periodDetails.length > 1) {  //this section has a meeting pattern (days/times) ONLY, else values remain as above
						//days of week
						
						String days = periodDetails[1];
						if (days.contains("M")) {
							monday = true;
						}
						if(days.contains("T")) {
							tuesday = true;
						}
						if(days.contains("W")) {
							wednesday = true;
						}
						if(days.contains("R")) {
							thursday = true;
						}
						if(days.contains("F")) {
							friday = true;
						}
						
						//time
						
						String startTimeString = periodDetails[2].substring(1, periodDetails[2].indexOf("-") - 1);
						try {
							startTime = new SimpleDateFormat("hh:mm aa").parse(startTimeString);
						} catch (java.text.ParseException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						
						String endTimeString = periodDetails[2].substring(periodDetails[2].indexOf("-") + 2);
						try {
							endTime = new SimpleDateFormat("hh:mm aa").parse(endTimeString);
						} catch (java.text.ParseException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}else {  //no meeting days/times
						try {
							startTime = new SimpleDateFormat("hh:mm aa").parse("12:00 PM");
						} catch (java.text.ParseException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						try {
							endTime = new SimpleDateFormat("hh:mm aa").parse("12:00 PM");
						} catch (java.text.ParseException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
					period newPeriod = new period(plannerType, thisProfessor, monday, tuesday, wednesday, thursday, friday, startTime, endTime, thisLocation);
					newSection.getPeriods().add(newPeriod);
				}
				
				//INTERRUPT
				
				if(plannerType.equals("Lecture")) {
					lectures.add(newSection);
				}
				else if(plannerType.equals("Conference")) {
					conferences.add(newSection);
				}
				else if(plannerType.equals("Lab")) {
					labs.add(newSection);
				}else {
					newCourse.getSections().add(newSection);
				}
				
			}
			
			//SECTION COMBINER STARTS HERE
			
			if(!lectures.isEmpty() && labs.isEmpty() && conferences.isEmpty()) {
				for (section lecture : lectures) {
					newCourse.getSections().add(lecture);
				}
			}
			else if(lectures.isEmpty() && !labs.isEmpty()) {   //if the class ONLY has labs (of which there are a few)
				for (section lab : labs) {
					newCourse.getSections().add(lab);
				}
			}else {
				//use lectures as our foundation, branch off from them
				for (section lecture : lectures) {
					if(lecture.isGPS() && lecture.getNote()==null) {  //if lecture is a GPS and not part of a cluster
						newCourse.getSections().add(lecture);
					}else {
						if(!conferences.isEmpty()) {  //if there are conferences
							for (section conference : conferences) {
								if(!labs.isEmpty()) {  //there are labs along with lectures and conferences
									for (section lab : labs) {
										ArrayList<section> sections = new ArrayList<section>();
										sections.add(lecture);
										sections.add(conference);
										sections.add(lab);
										if(conflictChecker(sections)) {  //if sections are ok
											section combined = combiner(sections);
											newCourse.getSections().add(combined);
										}
									}
								}else {  //just lectures and conferences, no labs
									ArrayList<section> sections = new ArrayList<section>();
									sections.add(lecture);
									sections.add(conference);
									if(conflictChecker(sections)) {  //if sections are ok
										section combined = combiner(sections);
										newCourse.getSections().add(combined);
									}
								}
							}
						} else {  //there are lectures and labs ONLY
							for (section lab : labs) {
								ArrayList<section> sections = new ArrayList<section>();
								sections.add(lecture);
								sections.add(lab);
								if(conflictChecker(sections)) {  //if sections are ok
									section combined = combiner(sections);
									newCourse.getSections().add(combined);
								}
							}
						}
					}
					
				}
			}
			
			
			if (index >= courseList.size() - 1) {  //loop exit condition
				cont = false;
			} else {
				index = index + findOthersAddition;
			}
		}
		
	}
	
	
	//method takes a list of sections and combines them into one section for planner to display as a group
	public section combiner(ArrayList<section> sections) {
		long crn = sections.get(0).getCrn();
		
		String number = "";
		for(section section : sections) {
			number = number + section.getNumber() + "/";
		}
		number = number.substring(0, number.length() - 1);  //chop off last "/"
		
		int seats = 10000;
		for (section section: sections) {
			if(section.getSeats() < seats) {
				seats = section.getSeats();
			}
		}
		
		long availableSeats = 10000;
		for (section section: sections) {
			if(section.getAvailableseats() < availableSeats) {
				availableSeats = section.getAvailableseats();
			}
		}
		
		int maxWaitlist = 10000;
		for (section section: sections) {
			if(section.getMaxWaitlist() < maxWaitlist) {
				maxWaitlist = section.getMaxWaitlist();
			}
		}
		
		long actualWaitlist = 10000;
		for (section section: sections) {
			if(section.getActualWaitlist() < actualWaitlist) {
				actualWaitlist = section.getActualWaitlist();
			}
		}
		
		String term = sections.get(0).getTerm();
		String partOfTerm = sections.get(0).getPartOfTerm();
		
		section result = new section(crn, number, seats, availableSeats, maxWaitlist, actualWaitlist, term, partOfTerm);
		
		for (section section : sections) {
			ArrayList<period> thisSectionPeriods = section.getPeriods();
			for (period period : thisSectionPeriods) {
				result.getPeriods().add(period);
			}
		}
		
		
		return result;
	}
	
	//checks if the sections in the list are "compatible" with each other
	//sections are "compatible" if they a) adhere to cluster rules, if any exist AND b) don't time conflict with each other
	//returns TRUE if sections work well together, FALSE otherwise
	public boolean conflictChecker(ArrayList<section> sections) {
		
		//check if cluster requirements are met
		boolean goodCluster = true;
		String cluster = "";
		
		//check if course is GPS. If GPS course all combinations MUST be a part of a cluster
		boolean isGPS = sections.get(0).isGPS();
		
		if(isGPS) {  //all sections MUST be a part of the same cluster
			if(sections.get(0).getNote() != null) {   //if there is a cluster
				cluster = sections.get(0).getNote();
			}
			for(int i = 1; i < sections.size(); i++) {  //for the remaining sections
				String thisCluster;
				if(sections.get(i).getNote() != null) {
					thisCluster = sections.get(i).getNote();
				}else {
					thisCluster = "";
				}
				
				if(!(thisCluster.equals(cluster))) {  //if clusters don't match
					goodCluster = false;
				}
			}
		}else {    //it's ok to match with sections with no cluster assignment
			for (section section : sections) {
				if (section.getNote() != null) {   //if this section has a cluster
					if(cluster.isEmpty()) {
						cluster = section.getNote();
					}else {   //cluster assignment already exists, check
						if(!(cluster.equals(section.getNote()))) {  //if the clusters do not match
							goodCluster = false;
						}
					}
				}
			}
		}
		
		
		
		boolean goodTimes = true;
		
		if (sections.size() == 2) {
			period period1 = sections.get(0).getPeriods().get(0);
			period period2 = sections.get(1).getPeriods().get(0);
			goodTimes = periodConflictChecker(period1, period2);
		} else if(sections.size() == 3) {
			period period1 = sections.get(0).getPeriods().get(0);
			period period2 = sections.get(1).getPeriods().get(0);
			period period3 = sections.get(2).getPeriods().get(0);
			
			goodTimes = periodConflictChecker(period1, period2) && periodConflictChecker(period1, period3) && periodConflictChecker(period2, period3);
		}
		
		return goodCluster && goodTimes;
	}
	
	//checks if the given TWO PERIODS conflict with each other
	//returns FALSE if they don't work with each other, TRUE if the combination is ok
	public boolean periodConflictChecker(period period1, period period2) {
		boolean result = true;
		//assume true if they do overlap
		boolean timeOverlap = period2.getStarts().compareTo(period1.getEnds()) <= 0 && period2.getEnds().compareTo(period1.getStarts()) >= 0;
		
		if(timeOverlap) {
			if(period1.isMonday() && period2.isMonday()) {
				result = false;
			}
			if(period1.isTuesday() && period2.isTuesday()) {
				result = false;
			}
			if(period1.isWednesday() && period2.isWednesday()) {
				result = false;
			}
			if(period1.isThursday() && period2.isThursday()) {
				result = false;
			}
			if(period1.isFriday() && period2.isFriday()) {
				result = false;
			}
		} 
		
		return result;
	}

}
