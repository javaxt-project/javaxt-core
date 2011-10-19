package javaxt.utils;
import java.text.ParseException;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Locale;

//******************************************************************************
//**  Date Utils - By Peter Borissow
//******************************************************************************
/**
 *   Used to parse, format, and compute dates
 *
 ******************************************************************************/

public class Date {
    
    private Locale currentLocale = Locale.getDefault();
    private java.util.TimeZone timeZone;
    private java.util.Date currDate;
 
    
    public static final String INTERVAL_MILLISECONDS = "S";
    public static final String INTERVAL_SECONDS = "s";
    public static final String INTERVAL_MINUTES = "m";
    public static final String INTERVAL_HOURS = "h";
    public static final String INTERVAL_DAYS = "d";
    public static final String INTERVAL_WEEKS = "w";
    public static final String INTERVAL_MONTHS = "m";
    public static final String INTERVAL_YEARS = "y";
    

    private static String[] SupportedFormats = new String[] {

         "EEE, d MMM yyyy HH:mm:ss z",  // Mon, 7 Jun 1976 13:02:09 EST
         "EEE, dd MMM yyyy HH:mm:ss z", // Mon, 07 Jun 1976 13:02:09 EST

         "EEE MMM dd HH:mm:ss z yyyy",  // Mon Jun 07 13:02:09 EST 1976
         "EEE MMM d HH:mm:ss z yyyy",   // Mon Jun 7 13:02:09 EST 1976

         "EEE MMM dd HH:mm:ss yyyy",    // Mon Jun 07 13:02:09 1976
         "EEE MMM d HH:mm:ss yyyy",     // Mon Jun 7 13:02:09 1976

         "yyyy-MM-dd HH:mm:ss.SSSZ",    // 1976-06-07 01:02:09.000-0500
         "yyyy-MM-dd HH:mm:ss.SSS",     // 1976-06-07 01:02:09.000

         "yyyy-MM-dd HH:mm:ssZ",        // 1976-06-07 13:02:36-0500
         "yyyy-MM-dd HH:mm:ss",         // 1976-06-07 01:02:09


         "yyyy:MM:dd HH:mm:ss",         // 1976:06:07 01:02:09 (exif metadata)

         "yyyy-MM-dd-HH:mm:ss.SSS",     // 1976-06-07-01:02:09.000
         "yyyy-MM-dd-HH:mm:ss",         // 1976-06-07-01:02:09

       //"yyyy-MM-ddTHH:mm:ss.SSS",     // 1976-06-07T01:02:09.000
       //"yyyy-MM-ddTHH:mm:ss",         // 1976-06-07T01:02:09

         "dd-MMM-yyyy h:mm:ss a",       // 07-Jun-1976 1:02:09 PM
         "dd-MMM-yy h:mm:ss a",         // 07-Jun-76 1:02:09 PM
       //"d-MMM-yy h:mm:ss a",          // 7-Jun-76 1:02:09 PM


         "yyyy-MM-dd HH:mmZ",           // 1976-06-07T13:02-0500
         "yyyy-MM-dd HH:mm",            // 1976-06-07T13:02
         "yyyy-MM-dd",                  // 1976-06-07

         "dd-MMM-yy",                   // 07-Jun-76
       //"d-MMM-yy",                    // 7-Jun-76
         "dd-MMM-yyyy",                 // 07-Jun-1976

         "MMMMMM d, yyyy",              // June 7, 1976

         "M/d/yy h:mm:ss a",            // 6/7/1976 1:02:09 PM
         "M/d/yy h:mm a",               // 6/7/1976 1:02 PM

         "MM/dd/yyyy HH:mm:ss",         // 06/07/1976 13:02:09
         "MM/dd/yyyy HH:mm",            // 06/07/1976 13:02

         "M/d/yy",                      // 6/7/76
         "MM/dd/yyyy",                  // 06/07/1976
         "M/d/yyyy",                    // 6/7/1976

         "yyyyMMddHHmmssSSS",           // 19760607130200000
         "yyyyMMddHHmmss",              // 19760607130200
         "yyyyMMdd"                     // 19760607

    };

    
  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of date using current time stamp */
    
    public Date(){
        currDate = new java.util.Date();
    }

  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of date using supplied java.util.Date */
    
    public Date(java.util.Date date){
        currDate = date;
    }
    
    
  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of date using supplied java.util.Calendar */
    
    public Date(Calendar calendar){
        currDate = calendar.getTime();
    }
    
    
  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of date using a timestamp (in milliseconds) 
   *  since 1/1/1970
   */
    
    public Date(long milliseconds){
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(milliseconds);
        currDate = cal.getTime();
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /**  Creates a new instance of date using a String representation of a date.
   */
    public Date(String date) throws ParseException {

        try{

          //Special Case: Java fails to parse the "T" in strings like
          //"1976-06-07T01:02:09.000" and "1976-06-07T13:02-0500"
            if (date.length()>="1976-06-07T13:02".length()){
                if (date.substring(10, 11).equalsIgnoreCase("T")){
                    date = date.replace("T", " ");
                }
            }


          //Loop through all known date formats and try to convert the string to a date
            for (int i=0; i<SupportedFormats.length; i++){

              //Special Case: Java fails to parse the "Z" in "1976-06-07 00:00:00Z"
                if (date.endsWith("Z") && SupportedFormats[i].endsWith("Z")){
                    date = date.substring(0, date.length()-1) + "UTC";
                }

                try{
                    currDate = parseDate(date, SupportedFormats[i]);
                    return;
                }
                catch(ParseException e){
                }
            }
        
        }
        catch(Exception e){
        }
        
      //If we're still here, throw an exception
        throw new ParseException("Failed to parse date: " + date, 0);
        
    }

    
  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of date using a date string. The format string is
   *  used to create a SimpleDateFormat to parse the input date string.
   */
    
    public Date(String date, String format) throws ParseException {
        currDate = parseDate(date, format);
    }
    
    
    
  //**************************************************************************
  //** setDate
  //**************************************************************************
  /** Used to update the current date using a date string. The format parameter
   *  is used to create a SimpleDateFormat to parse the input date string.
   */    
    public void setDate(String date, String format) throws ParseException {
        currDate = parseDate(date, format);
    }
    
    
  //**************************************************************************
  //** setDate
  //**************************************************************************
  /**  Used to update the current date using a predefined java.util.Date */
    
    public void setDate(java.util.Date date){
        currDate = date;
    }
    
    
  //**************************************************************************
  //** setLocale
  //**************************************************************************
  /**  Used to update the current local */
    
    public void setLocale(Locale locale){
        this.currentLocale = locale;
    }
    
    
    
  //**************************************************************************
  //** getLocale
  //**************************************************************************
  /**  Used to retrieve the current local */
    
    public Locale getLocale(){
        return currentLocale;
    }
    
    
  //**************************************************************************
  //** ParseDate
  //**************************************************************************
  /**  Attempts to convert a String to a Date via the user-supplied Format */
    
    private java.util.Date parseDate(String date, String format) throws ParseException {

        if (date!=null){
            date = date.trim();
            if (date.length()==0) date = null; 
        }
        if (date==null) throw new ParseException("Date is null.", 0);
        
        SimpleDateFormat formatter =
                new SimpleDateFormat(format, currentLocale);
        if (this.timeZone!=null) formatter.setTimeZone(timeZone);
        java.util.Date d = formatter.parse(date);
        this.timeZone = formatter.getTimeZone();
        return d;
    }


  //**************************************************************************
  //** setTimeZone
  //**************************************************************************
  /** Used to set the current time zone. The time zone is used when comparing
   *  and formatting dates.
   *  @param timeZone Name of the time zone (e.g. "UTC", "EDT", etc.)
   *  @param preserveTimeStamp Flag used to indicate whether to preserve the
   *  timestamp when changing time zones. Normally, when updating the timezone, 
   *  the timestamp is updated to the new timezone. For example, if the current 
   *  time is 4PM EST and you wish to switch to UTC, the timestamp would be
   *  updated to 8PM. The preserveTimeStamp flag allows users to preserve the
   *  the timestamp so that the timestamp remains fixed at 4PM.
   */
    public void setTimeZone(String timeZone, boolean preserveTimeStamp){
        if (timeZone==null) return;
        java.util.TimeZone timezone = java.util.TimeZone.getTimeZone(timeZone);

        if (preserveTimeStamp){
            
            String z1 = FormatDate(currDate, "z");

            SimpleDateFormat dateFormat = new SimpleDateFormat("z", currentLocale);
            dateFormat.setTimeZone(timezone);
            String z2 = dateFormat.format(currDate);

            String d = FormatDate(currDate, "yyyy-MM-dd HH:mm:ss.SSS z").replace(z1, z2);
            try{
                currDate = parseDate(d, "yyyy-MM-dd HH:mm:ss.SSS z");
            }
            catch(ParseException e){}
        }
        else{
            this.timeZone = timezone;
        }
    }


  //**************************************************************************
  //** setTimeZone
  //**************************************************************************
  /** Used to set the current time zone. The time zone is used when comparing
   *  and formatting dates.
   *  @param timeZone Name of the time zone (e.g. "UTC", "EST", etc.)
   */
    public void setTimeZone(String timeZone){
        setTimeZone(timeZone, false);
    }
    
    public void setTimeZone(java.util.TimeZone timeZone){
        if (timeZone==null) return;
        this.timeZone = timeZone;
    }

    public java.util.TimeZone getTimeZone(){
        return this.timeZone;
    }
    
  //**************************************************************************
  //** toString
  //**************************************************************************
  /** Returns the current date as a String */
    
    public String toString(){      
        return toString("EEE MMM dd HH:mm:ss z yyyy");
    }

    
  //**************************************************************************
  //** toString
  //**************************************************************************
  /** Used to format the current date into a string.
   *  @param format Pattern used to format the date (e.g.
   *  "EEE MMM dd HH:mm:ss z yyyy"). Please refer to the SimpleDateFormat class
   *  for more information.
   */
    public String toString(String format){
        SimpleDateFormat currFormatter = 
            new SimpleDateFormat(format, currentLocale);
        if (timeZone!=null) currFormatter.setTimeZone(timeZone);
        return currFormatter.format(currDate);
    }


  //**************************************************************************
  //** equals
  //**************************************************************************
  /** Used to compare dates and determine whether they are equal.
   *  @param obj Accepts a java.util.Date, a javaxt.utils.Date, or a String.
   */
    public boolean equals(Object obj){
        if (obj instanceof javaxt.utils.Date){
            return ((javaxt.utils.Date) obj).getDate().equals(currDate);
        }
        else if (obj instanceof java.util.Date){
            return ((java.util.Date) obj).equals(currDate);
        }
        else if (obj instanceof String){
            try{
                return new javaxt.utils.Date((String) obj).equals(currDate);
            }
            catch(ParseException e){}
        }
        return false;
    }
    

    
  //**************************************************************************
  //** FormatDate
  //**************************************************************************
    
    private String FormatDate(java.util.Date date, String OutputFormat){
        SimpleDateFormat formatter = 
                new SimpleDateFormat(OutputFormat, currentLocale);
        if (timeZone != null) formatter.setTimeZone(timeZone);
        return formatter.format(date);
    }
   
    
  //**************************************************************************
  //** compareTo
  //**************************************************************************
  /** Used to compare dates. Returns the number of intervals between two dates
   *  @param units Units of measure (e.g. hours, minutes, seconds, weeks,
   *  months, years, etc.)
   */    
    public long compareTo(javaxt.utils.Date date, String units){
        return DateDiff(currDate, date.getDate(), units);
    }

    
  //**************************************************************************
  //** compareTo
  //**************************************************************************
  /** Used to compare dates. Returns the number of intervals between two dates
   *  @param units Units of measure (e.g. hours, minutes, seconds, weeks, 
   *  months, years, etc.)
   */
    public long compareTo(java.util.Date date, String units){
        return DateDiff(currDate, date, units);
    }
    
    
  //**************************************************************************
  //** DateDiff
  //**************************************************************************
  /**  Implements compareTo public members */
    
    private long DateDiff(java.util.Date date1, java.util.Date date2, String interval){
                
        double div = 1;
        if (interval.equals("S") || interval.toLowerCase().startsWith("sec")){
            div = 1000L;
        }        
        if (interval.equals("m") || interval.toLowerCase().startsWith("min")){
            div = 60L * 1000L;
        }
        if (interval.equals("H") || interval.toLowerCase().startsWith("h")){
            div = 60L * 60L * 1000L;
        }
        if (interval.equals("d") || interval.toLowerCase().startsWith("d")){
            div = 24L * 60L * 60L * 1000L;
        }
        if (interval.equals("w") || interval.toLowerCase().startsWith("w")){
            div = 7L * 24L * 60L * 60L * 1000L;
        }       
        if (interval.equals("M") || interval.toLowerCase().startsWith("mon")){
            div = 30L * 24L * 60L * 60L * 1000L;
        }     
        if (interval.equals("y") || interval.toLowerCase().startsWith("y")){
            div = 365L * 24L * 60L * 60L * 1000L;
        }    
        
        long d1 = date1.getTime();
        long d2 = date2.getTime();
        

        int i2 = (int)Math.abs((d1 - d2) / div);         
        if (date2.after(date1)){
            i2 = -i2;
        }
        
        return i2;
    }
    
    
    public boolean isBefore(javaxt.utils.Date Date){
        return currDate.before(Date.getDate());
    }
    
    public boolean isAfter(javaxt.utils.Date Date){
        return currDate.after(Date.getDate());
    }
    
    
  //**************************************************************************
  //** Add
  //**************************************************************************
  /**  Used to add to (or subtract from) the current date. 
   *   Returns a date to which a specified time interval has been added.
   *  @param units Units of measure (e.g. hours, minutes, seconds, weeks,
   *  months, years, etc.)
   */
    
    public java.util.Date add(int amount, String units){

        Calendar cal = Calendar.getInstance();
        cal.setTime(currDate);
        
        int div = 0;
        if (units.equals("S") || units.toLowerCase().startsWith("sec")){
            div = cal.MILLISECOND;
        }        
        else if(units.equals("m") || units.toLowerCase().startsWith("min")){
            div = cal.MINUTE;
        }
        else if (units.equals("H") || units.toLowerCase().startsWith("h")){
            div = cal.HOUR_OF_DAY;
        }
        else if (units.equals("d") || units.toLowerCase().startsWith("d")){
            div = cal.DAY_OF_YEAR;
        }
        else if (units.equals("w") || units.toLowerCase().startsWith("w")){
            div = cal.WEEK_OF_YEAR;
        }   
        else if (units.equals("M") || units.toLowerCase().startsWith("mon")){
            div = cal.MONTH;
        }           
        else if (units.equals("y") || units.toLowerCase().startsWith("y")){
            div = cal.YEAR;
        }   
        cal.add(div, amount);
        currDate = cal.getTime();        
        return currDate;
    }


  //**************************************************************************
  //** getDate
  //**************************************************************************
  /**  Returns the java.utils.Date representation of this object */
    
    public java.util.Date getDate(){
        return currDate;
    }
    
    
  //**************************************************************************
  //** getTime
  //**************************************************************************
  /** Returns the number of milliseconds since January 1, 1970, 00:00:00 GMT
   *  represented by this Date object.
   */
    public long getTime(){
        return getCalendar().getTimeInMillis();
    }

    
  //**************************************************************************
  //** getCalendar
  //**************************************************************************
  /**  Returns the java.utils.Calender representation of this object */
    
    public Calendar getCalendar(){
        Calendar cal = Calendar.getInstance();
        cal.setTime(currDate);
        if (this.timeZone!=null) cal.setTimeZone(timeZone);
        return cal;
    }
    
    
  //**************************************************************************
  //** getWeekdayName
  //**************************************************************************
  /**  Returns the name of the day of the week. Example: "Monday" */
    
    public String getWeekdayName(){
        return FormatDate(currDate, "EEEEEE");
    }
    
    
  //**************************************************************************
  //** getWeekdayName
  //**************************************************************************
  /**  Returns the name of the month. Example: "January" */
    
    public String getMonthName(){
        return FormatDate(currDate, "MMMMMM");
    }
    
    
  //**************************************************************************
  //** getDayOfWeek
  //**************************************************************************
  /**  Returns the day of the week. Example: Monday = 1 */
    
    public int getDayOfWeek(){
        return Integer.valueOf(FormatDate(currDate, "F")).intValue();
    }
    
    
  //**************************************************************************
  //** getWeekInMonth
  //**************************************************************************
  /**  Returns the week number in a given month. Example: 11/14/2006 = 3 */
    
    public int getWeekInMonth(){
        return Integer.valueOf(FormatDate(currDate, "W")).intValue();
    }
    

    
  //**************************************************************************
  //** getDayInYear
  //**************************************************************************
  /**  Returns the day of the year. Example: 11/14/2006 = 318 */
    
    public int getDayInYear(){
        return Integer.valueOf(FormatDate(currDate, "D")).intValue();
    }

    
  //**************************************************************************
  //** getWeekInYear
  //**************************************************************************
  /**  Returns the week number within a given year. Example: 11/14/2006 = 46 */
    
    public int getWeekInYear(){
        return Integer.valueOf(FormatDate(currDate, "w")).intValue();
    }
    
    
  //**************************************************************************
  //** getYear
  //**************************************************************************
  /**  Returns the current year. Example: 11/14/2006 = 2006 */
    
    public int getYear(){
        return Integer.valueOf(FormatDate(currDate, "yyyy")).intValue();
    }
    
    
  //**************************************************************************
  //** getMonth
  //**************************************************************************
  /**  Returns the current month. Example: 11/14/2006 = 11 */
    
    public int getMonth(){
        return Integer.valueOf(FormatDate(currDate, "MM")).intValue();
    }    
    
  //**************************************************************************
  //** getDay
  //**************************************************************************
  /**  Returns the current day of the month. Example: 11/14/2006 = 14 */
    
    public int getDay(){
        return Integer.valueOf(FormatDate(currDate, "dd")).intValue();
    }
    
    
  //**************************************************************************
  //** getHour
  //**************************************************************************
  /**  Returns the current hour of the day. Example: 12:00 AM = 0, 1:00 PM = 13 
   */
    
    public int getHour(){
        return Integer.valueOf(FormatDate(currDate, "HH")).intValue();
    }
    
    
  //**************************************************************************
  //** getMinute
  //**************************************************************************
  /**  Returns the current minute of the hour. Example: 12:01 = 1  */
    
    public int getMinute(){
        return Integer.valueOf(FormatDate(currDate, "m")).intValue();
    }
    
    
  //**************************************************************************
  //** getSecond
  //**************************************************************************
  /**  Returns the current second of the minute. Example: 12:00:01 = 1  */
    
    public int getSecond(){
        return Integer.valueOf(FormatDate(currDate, "s")).intValue();
    }
    
    
  //**************************************************************************
  //** getMilliSecond
  //**************************************************************************
  /**  Returns the current millisecond of the second. Example: 12:00:00:01 = 1*/
    
    public int getMilliSecond(){
        return Integer.valueOf(FormatDate(currDate, "S")).intValue();
    }
    
    

  //**************************************************************************
  //** hasTimeStamp
  //**************************************************************************
  /** Used to determine whether a date has a timestamp.
   */
    public boolean hasTimeStamp(){
        java.util.Calendar cal = getCalendar();
        int hour = cal.get(java.util.Calendar.HOUR);
        int min = cal.get(java.util.Calendar.MINUTE);
        int sec = cal.get(java.util.Calendar.SECOND);
        int ms = cal.get(java.util.Calendar.MILLISECOND);
        if (hour>0 || min>0 || sec>0 || ms>0) return true;
        return false;
    }
}
