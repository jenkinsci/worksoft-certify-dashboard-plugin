package org.jenkinsci.plugins.WorksoftCertifyDashboardPlugin;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class PublishResults extends Builder{

    private final String databaseName;
    private final String databaseServer;
    private final String path;
    private final String folder;
    private int passed = 0;
    private int failed = 0;
    private int aborted = 0;
    private int noResult = 0;
    private int total=0;
    private final String username;
    private final String password;
    private final boolean sqlauth;
    
    public String getDatabaseName() {
        return databaseName;
    }

    public String getDatabaseServer() {
        return databaseServer;
    }

    public String getPath() {
        return path;
    }

    public String getFolder() {
        return folder;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
    @DataBoundConstructor
    public PublishResults(String databaseServer, String databaseName, String fileName, String path, String folder,boolean sqlauth,String username,String password) {
        this.databaseServer = databaseServer;
        this.databaseName = databaseName;
        this.path = path;
        this.folder = folder;
        this.username=username;
        this.password=password;
        this.sqlauth=sqlauth;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
         DecimalFormat df = new DecimalFormat(".##");
         String urlw = "jdbc:sqlserver://" + this.databaseServer + ";databaseName=" + this.databaseName + ";integratedSecurity=true";
         String urls ="jdbc:sqlserver://" + this.databaseServer + ";databaseName=" + this.databaseName ;
         String sql = "SELECT * from [dbo].[CMP_ResultInfos] ORDER BY [StartTime] DESC";
        try {
            File f = new File(this.path);
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            Connection conn;
            if(sqlauth){
            conn = DriverManager.getConnection(urls,username,password);
            }
            else{
            conn =DriverManager.getConnection(urlw);
            }
            Statement stm = conn.createStatement();
            Statement stm1 = conn.createStatement();
            FileWriter out;
                 ResultSet rs = stm.executeQuery(sql);
                 ResultSet rs1 = stm1.executeQuery(sql);
                 while (rs1.next()) {
                     String s = rs1.getString("Status");
                     String ff = rs1.getString("LogFolderName");
                     if (ff.equals(this.folder)) {
                         
                         switch (s) {
                             case "passed":
                                 ++passed;
                                 break;
                             case "failed":
                                 ++failed;
                                 break;
                             case "aborted":
                                 ++aborted;
                                 break;
                             default:
                                 ++noResult;
                                 break;
                         }
                 }
                 }   
                 total = passed + aborted + failed + noResult;
                 double passp = (double) passed / (double) total * 100.0;
                 double failp = (double) failed / (double) total * 100.0;
                 double abortp = (double) aborted / (double) total * 100.0;
                 double noResultp = (double) noResult / (double) total * 100.0;
                 String chart = "<script type=\"text/javascript\" src=\"https://www.gstatic.com/charts/loader.js\"></script> <script type=\"text/javascript\">google.charts.load(\"current\", {packages:[\"corechart\"]});google.charts.setOnLoadCallback(drawChart); function drawChart() {var data = google.visualization.arrayToDataTable([ ['Status', 'TestCase Count'],   ['Passed'," + passed + "]," + " ['Failed', " + failed + "]," + "['Aborted'," + aborted + "]," + "['NoResult'," + noResult + "]," + "     ]);" + "  var options = {" + "is3D: true,\n" + "slices:{0: { color: 'green' }, 1: { color: 'red' },2: { color: 'orange'},  3: { color: 'grey'},}" + " };" + "var chart = new google.visualization.PieChart(document.getElementById('piechart_3d'));" + "chart.draw(data, options);" + " }\n" + "</script>";
                 out = new FileWriter(f);
                 out.write("<!DOCTYPE html>");
                 out.write("<html>");
                 out.write("<head>");
                 out.write("<style>");
                 out.write("table, th, td {");
                 out.write("margin-left: auto;\nmargin-right: auto;font-family:verdana;");
                 out.write("border: 1px solid black;");
                 out.write("border-collapse: collapse; }");
                 out.write("th,td{ padding:5px;text-align:left;}");
                 out.write("table tr:nth-child(even){background-color: #eee; }table tr:nth-child(odd) {  background-color:#fff;}table th {background-color: black;color: white;}");
                 out.write("div.table{");
                 out.write("width:50%;");
                 out.write("height:300px;");
                 out.write("float:left;}");
                 out.write("div.chart{");
                 out.write("width:50%;");
                 out.write("height:300px;");
                 out.write("float:right}");
                 out.write("</style>");
                 out.write(chart);
                 out.write("</head>");
                 out.write("<body>");
                 out.write("<h1 style=\"font-size:50px;height:100px;text-align:center;background-color:cornflowerblue;color:white;font-family:verdana;\">Worksoft Certify Execution Dashboard</h1>");
                 out.write("<h2 style=\"font-family:verdana;text-align:center\">Summary</h2>");
                 out.write("<div class=\"table\">");
                 out.write("<table style=\"width:50%\">");
                 out.write("<tr>");
                 out.write("<th>Result Status</th>");
                 out.write("<th>TestCases Total</th>");
                 out.write("<th>Proportion</th>");
                 out.write("</tr>");
                 out.write("<tr>");
                 out.write("<td>Passed</td>");
                 out.write("<td>" + Integer.toString(passed) + "</td>");
                 out.write("<td>" + df.format(passp) + "%</td>");
                 out.write("</tr>");
                 out.write("<tr>");
                 out.write("<td>Failed</td>");
                 out.write("<td>" + Integer.toString(failed) + "</td>");
                 out.write("<td>" + df.format(failp) + "%</td>");
                 out.write("</tr>");
                 out.write("<tr>");
                 out.write("<td>Aborted</td>");
                 out.write("<td>" + Integer.toString(aborted) + "</td>");
                 out.write("<td>" + df.format(abortp) + "%</td>");
                 out.write("</tr>");
                 out.write("<tr>");
                 out.write("<td>No Result</td>");
                 out.write("<td>" + Integer.toString(noResult) + "</td>");
                 out.write("<td>" + df.format(noResultp) + "%</td>");
                 out.write("</tr>");
                 out.write("<tr>");
                 out.write("<td>Total TestCases</td>");
                 out.write("<td>" + Integer.toString(total) + "</td>");
                 out.write("<td>100%</td>");
                 out.write("</tr>");
                 out.write("</table>");
                 out.write("</div>");
                 out.write("<div class=\"chart\" id=\"piechart_3d\"></div>");
                 out.write("<table style=\"width:100%\">");
                 out.write("<h2 style=\"font-family:verdana;text-align:center\">Worksoft Execution Results</h2>");
                 out.write("<tr>");
                 out.write("<th>Process</th>");
                 out.write("<th>Status</th>");
                 out.write("<th>StartTime</th>");
                 out.write("<th>EndTime</th>");
                 out.write("<th>TestStepCount</th>");
                 out.write("<th>TestStepPassedCount</th>");
                 out.write("<th>TestStepFailedCount</th>");
                 out.write("<th>TestStepSkippedCount</th>");
                 out.write("<th>TestStepAbortedCount</th>");
                 out.write("<th>Layout</th>");
                 out.write("<th>Recordset</th>");
                 out.write("<th>ModifiedBy</th>");
                 out.write("</tr>");
                 while (rs.next()) {
                     String name = rs.getString("StartingProcessName");
                     String StartTime = rs.getString("StartTime");
                     String EndTime = rs.getString("EndTime");
                     String Status = rs.getString("Status");
                     String Recordset = rs.getString("RecordSetIdentifier");
                     String Layout = rs.getString("LayoutName");
                     String modifiedBy = rs.getString("ModifiedBy");
                     int stepCount = rs.getInt("TestStepCount");
                     int passedStepCount = rs.getInt("TestStepPassedCount");
                     int skippedStepCount = rs.getInt("TestStepSkippedCount");
                     int abortedStepCount = rs.getInt("TestStepAbortCount");
                     int failedStepCount = rs.getInt("TestStepFailedCount");
                     String fol = rs.getString("LogFolderName");
                     if (!fol.equals(this.folder)) {
                         continue;
                     }
                     out.write("<tr>");
                     out.write("<td>" + name + "</td>");
                     if (Status.equals("passed")) {
                         out.write("<td><font color=\"#297A02\">" + Status + "</font></td>");
                     } else if (Status.equals("failed")) {
                         out.write("<td><font color=\"#FF0000\">" + Status + "</font></td>");
                     } else if (Status.equals("aborted")) {
                         out.write("<td><font color=\"#FF8000\">" + Status + "</font></td>");
                     } else {
                         out.write("<td>" + Status + "</td>");
                     }
                     out.write("<td>" + StartTime + "</td>");
                     out.write("<td>" + EndTime + "</td>");
                     out.write("<td>" + Integer.toString(stepCount) + "</td>");
                     out.write("<td>" + Integer.toString(passedStepCount) + "</td>");
                     out.write("<td>" + Integer.toString(failedStepCount) + "</td>");
                     out.write("<td>" + Integer.toString(skippedStepCount) + "</td>");
                     out.write("<td>" + Integer.toString(abortedStepCount) + "</td>");
                     out.write("<td>" + Layout + "</td>");
                     out.write("<td>" + Recordset + "</td>");
                     out.write("<td>" + modifiedBy + "</td>");
                     out.write("</tr>");
                 }    out.write("</table>");
                 out.write("</body>");
                 out.write("</html>");
                 out.close();
         } catch (IOException | SQLException ex) {
            listener.getLogger().println("Publishing Certify Results Failed due to::\n" + ex.getMessage());
            build.setResult(Result.FAILURE);
        }
        catch(ClassNotFoundException ex){
            listener.getLogger().println("Connection with SQL server Failed due to::\n" + ex.getMessage());
             build.setResult(Result.FAILURE);
        }
        return true;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Worksoft Certify Dashboard Plugin";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {

            save();
            return super.configure(req, formData);
        }

    }
}
