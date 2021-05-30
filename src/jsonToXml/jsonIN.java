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
			
			for (int i = 0; i < allSectionsThisCourseTerm.size(); i++) { //for all course sections
				JSONObject thisSection = (JSONObject) allSectionsThisCourseTerm.get(i);
				
				//section number
				String thisCourseSectionFull = (String) thisSection.get("Course_Section");
				String thisSectionNum = thisCourseSectionFull.substring(thisCourseSectionFull.indexOf("-") + 1, thisCourseSectionFull.indexOf("-", thisCourseSectionFull.indexOf("-") + 1) - 1);
				
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
				} else {
					termActual = term;
				}
				
				//build new section object
				section newSection = new section(00000, thisSectionNum, capacity, availableSeats, waitlistTotal, waitlistActual, "202201", termActual);
				
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
				
				newCourse.getSections().add(newSection);
				
			}
			
			
			if (index >= courseList.size() - 1) {  //loop exit condition
				cont = false;
			} else {
				index = index + findOthersAddition;
			}
		}
		
	}

}
