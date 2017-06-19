/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jenkinsci.plugins.testrail;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.util.ListBoxModel;
import hudson.tasks.*;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.testrail.JunitResults.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.jenkinsci.plugins.testrail.TestRailObjects.*;

import javax.servlet.ServletException;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.List;

public class TestRailNotifier extends Notifier {

    private int testrailProject;
    private int testrailSuite;
    private String junitResultsGlob;
    private String testrailMilestone;
    private boolean enableMilestone;
    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public TestRailNotifier(int testrailProject, int testrailSuite, String junitResultsGlob, String testrailMilestone, boolean enableMilestone) {
        this.testrailProject = testrailProject;
        this.testrailSuite = testrailSuite;
        this.junitResultsGlob = junitResultsGlob;
        this.testrailMilestone = testrailMilestone;
        this.enableMilestone = enableMilestone;
    }

    public void setTestrailProject(int project) { this.testrailProject = project;}
    public int getTestrailProject() { return this.testrailProject; }
    public void setTestrailSuite(int suite) { this.testrailSuite = suite; }
    public int getTestrailSuite() { return this.testrailSuite; }
    public void setJunitResultsGlob(String glob) { this.junitResultsGlob = glob; }
    public String getJunitResultsGlob() { return this.junitResultsGlob; }
    public String getTestrailMilestone() { return this.testrailMilestone; }
    public void setTestrailMilestone(String milestone) { this.testrailMilestone = milestone; }
    public void setEnableMilestone(boolean mstone) {this.enableMilestone = mstone; }
    public boolean getEnableMilestone() { return  this.enableMilestone; }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        TestRailClient  testrail = getDescriptor().getTestrailInstance();
        testrail.setHost(getDescriptor().getTestrailHost());
        testrail.setUser(getDescriptor().getTestrailUser());
        testrail.setPassword(getDescriptor().getTestrailPassword());

        ExistingTestCases testCases = null;
        try {
            testCases = new ExistingTestCases(testrail, this.testrailProject, this.testrailSuite);
        } catch (ElementNotFoundException e) {
            listener.getLogger().println("Cannot find project or suite on TestRail server. Please check your Jenkins job and system configurations.");
            return false;
        }

        String[] caseNames = null;
        try {
            caseNames = testCases.listTestCases();
            listener.getLogger().println("Test Cases: ");
            for (int i = 0; i < caseNames.length; i++) {
                listener.getLogger().println("  " + caseNames[i]);
            }
        } catch (ElementNotFoundException e) {
            listener.getLogger().println("Failed to list test cases");
            listener.getLogger().println("Element not found:" + e.getMessage());
        }

        listener.getLogger().println("Munging test result files.");
        Results results = new Results();

        // FilePath doesn't have a read method. We want to actually process the files on the master
        // because during processing we talk to TestRail and slaves might not be able to.
        // So we'll copy the result files to the master and munge them there:
        //
        // Create a temp directory.
        // Do a base.copyRecursiveTo() with file masks into the temp dir.
        // process the temp files.
        // it looks like the destructor deletes the temp dir when we're finished
        FilePath tempdir = new FilePath(Util.createTempDir());
        // This picks up *all* result files so if you have old results in the same directory we'll see those, too.
        FilePath ws = build.getWorkspace();
        try {
            ws.copyRecursiveTo(junitResultsGlob, "", tempdir);
        } catch (Exception e) {
            listener.getLogger().println("Error trying to copy files to Jenkins master: " + e.getMessage());
            return false;
        }
        JUnitResults actualJunitResults = null;
        try {
            actualJunitResults = new JUnitResults(tempdir, this.junitResultsGlob, listener.getLogger());
        } catch (JAXBException e) {
            listener.getLogger().println(e.getMessage());
            return false;
        }
        List<Testsuite> suites = actualJunitResults.getSuites();
        try {
            for (Testsuite suite: suites) {
                results.merge(addSuite(suite, null, testCases));
            }
        } catch (Exception e) {
            listener.getLogger().println("Failed to create missing Test Suites in TestRail.");
            listener.getLogger().println("EXCEPTION: " + e.getMessage());
        }

        listener.getLogger().println("Uploading results to TestRail.");
        String runComment = "Automated results from Jenkins: " + build.getUrl().toString();
        String milestoneId = testrailMilestone;

        int runId = -1;
        TestRailResponse response;
        try {
            runId = testrail.addRun(testCases.getProjectId(), testCases.getSuiteId(), milestoneId, runComment);
            response = testrail.addResultsForCases(runId, results);
        } catch (TestRailException e) {
            listener.getLogger().println("Error pushing results to TestRail");
            listener.getLogger().println(e.getMessage());
            return false;
        }

        boolean buildResult = (200 == response.getStatus());
        if (buildResult) {
            listener.getLogger().println("Successfully uploaded test results.");
        } else {
            listener.getLogger().println("Failed to add results to TestRail.");
            listener.getLogger().println("status: " + response.getStatus());
            listener.getLogger().println("body :\n" + response.getBody());
        }
        try {
            testrail.closeRun(runId);
        } catch (Exception e) {
            listener.getLogger().println("Failed to close test run in TestRail.");
            listener.getLogger().println("EXCEPTION: " + e.getMessage());
        }

        return buildResult;
    }

    public Results addSuite(Testsuite suite, String parentId, ExistingTestCases existingCases) throws IOException, TestRailException {
        //figure out TR sectionID
        int sectionId;
        try {
            sectionId = existingCases.getSectionId(suite.getName());
        } catch (ElementNotFoundException e1) {
            try {
                sectionId = existingCases.addSection(suite.getName(), parentId);
            } catch (ElementNotFoundException e) {
                //listener.getLogger().println("Unable to add test section " + suite.getName());
                //listener.getLogger().println(e.getMessage());
                return null;
            }
        }

        //if we have any subsections - process them
        Results results = new Results();

        if (suite.hasSuites()) {
            for (Testsuite subsuite : suite.getSuites()) {
                results.merge(addSuite(subsuite, String.valueOf(sectionId), existingCases));
            }
        }

        if (suite.hasCases()) {
            for (Testcase testcase : suite.getCases()) {
                int caseId;
                try {
                    caseId = existingCases.getCaseId(suite.getName(), testcase.getName());
                } catch (ElementNotFoundException e) {
                    caseId = existingCases.addCase(testcase, sectionId);
                }
                CaseStatus caseStatus;
                Float caseTime = testcase.getTime();
                String caseComment = null;
                Failure caseFailure = testcase.getFailure();
                if (caseFailure != null) {
                    caseStatus = CaseStatus.FAILED;
                    caseComment = (caseFailure.getMessage() == null) ? caseFailure.getText() : caseFailure.getMessage() + "\n" + caseFailure.getText();
                } else if (testcase.getSkipped() != null) {
                    caseStatus = CaseStatus.UNTESTED;
                } else {
                    caseStatus = CaseStatus.PASSED;
                }

                if (caseStatus != CaseStatus.UNTESTED){
                    results.addResult(new Result(caseId, caseStatus, caseComment, caseTime));
                }
            }
        }

        return results;
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE; //null;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        private String testrailHost = "";
        private String testrailUser = "";
        private String testrailPassword = "";
        private TestRailClient testrail = new TestRailClient("", "", "");

        /**
         * In order to load the persisted global configuration, you have to
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        public FormValidation doCheckTestrailProject(@QueryParameter int value)
                throws IOException, ServletException {
            testrail.setHost(getTestrailHost());
            testrail.setUser(getTestrailUser());
            testrail.setPassword(getTestrailPassword());
            if (getTestrailHost().isEmpty() || getTestrailUser().isEmpty() || getTestrailPassword().isEmpty() || !testrail.serverReachable() || !testrail.authenticationWorks()) {
                return FormValidation.warning("Please fix your TestRail configuration in Manage Jenkins -> Configure System.");
            }
            return FormValidation.ok();
        }

        public ListBoxModel doFillTestrailProjectItems() {
            testrail.setHost(getTestrailHost());
            testrail.setUser(getTestrailUser());
            testrail.setPassword(getTestrailPassword());

            ListBoxModel items = new ListBoxModel();
            try {
                for (Project prj : testrail.getProjects()) {
                    items.add(prj.getName(), prj.getStringId());
                }
            } catch (ElementNotFoundException e) {
            } catch (IOException e) {
            }

            return items;
        }

        public ListBoxModel doFillTestrailSuiteItems(@QueryParameter int testrailProject) {
            testrail.setHost(getTestrailHost());
            testrail.setUser(getTestrailUser());
            testrail.setPassword(getTestrailPassword());

            ListBoxModel items = new ListBoxModel();
            try {
                for (Suite suite : testrail.getSuites(testrailProject)) {
                    items.add(suite.getName(), suite.getStringId());
                }
            } catch (ElementNotFoundException e) {
            } catch (IOException e) {
            }

            return items;
        }

        public FormValidation doCheckTestrailSuite(@QueryParameter String value)
                throws IOException, ServletException {
            testrail.setHost(getTestrailHost());
            testrail.setUser(getTestrailUser());
            testrail.setPassword(getTestrailPassword());

            if (getTestrailHost().isEmpty() || getTestrailUser().isEmpty() || getTestrailPassword().isEmpty() || !testrail.serverReachable() || !testrail.authenticationWorks()) {
                return FormValidation.warning("Please fix your TestRail configuration in Manage Jenkins -> Configure System.");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckJunitResultsGlob(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.warning("Please select test result path.");
            // TODO: Should we check to see if the files exist? Probably not.
            return FormValidation.ok();
        }

        public FormValidation doCheckTestrailHost(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0) {
                return FormValidation.warning("Please add your TestRail host URI.");
            }
            // TODO: There is probably a better way to do URL validation.
            if (!value.startsWith("http://") && !value.startsWith("https://")) {
                return FormValidation.error("Host must be a valid URL.");
            }
            testrail.setHost(value);
            testrail.setUser("");
            testrail.setPassword("");
            if (!testrail.serverReachable()) {
                return FormValidation.error("Host is not reachable.");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckTestrailUser(@QueryParameter String value,
                                                  @QueryParameter String testrailHost,
                                                  @QueryParameter String testrailPassword)
                throws IOException, ServletException {
            if (value.length() == 0) {
                return FormValidation.warning("Please add your user's email address.");
            }
            if (testrailPassword.length() > 0) {
                testrail.setHost(testrailHost);
                testrail.setUser(value);
                testrail.setPassword(testrailPassword);
                if (testrail.serverReachable() && !testrail.authenticationWorks()){
                    return FormValidation.error("Invalid user/password combination.");
                }
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckTestrailPassword(@QueryParameter String value,
                                                      @QueryParameter String testrailHost,
                                                      @QueryParameter String testrailUser)
                throws IOException, ServletException {
            if (value.length() == 0) {
                return FormValidation.warning("Please add your password.");
            }
            if (testrailUser.length() > 0) {
                testrail.setHost(testrailHost);
                testrail.setUser(testrailUser);
                testrail.setPassword(value);
                if (testrail.serverReachable() && !testrail.authenticationWorks()){
                    return FormValidation.error("Invalid user/password combination.");
                }
            }
            return FormValidation.ok();
        }

        public ListBoxModel doFillTestrailMilestoneItems(@QueryParameter int testrailProject) {
            ListBoxModel items = new ListBoxModel();
            items.add("None", "");
            try {
                for (Milestone mstone : testrail.getMilestones(testrailProject)) {
                    items.add(mstone.getName(), mstone.getId());
                }
            } catch (ElementNotFoundException e) {
            } catch (IOException e) {
            }
            return items;
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "TestRail Plugin";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            testrailHost = formData.getString("testrailHost");
            testrailUser = formData.getString("testrailUser");
            testrailPassword = formData.getString("testrailPassword");


            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setTestrailHost)
            save();
            return super.configure(req,formData);
        }

        public void setTestrailHost(String host) { this.testrailHost = host; }
        public String getTestrailHost() { return testrailHost; }
        public void setTestrailUser(String user) { this.testrailUser = user; }
        public String getTestrailUser() { return testrailUser; }
        public void setTestrailPassword(String password) { this.testrailPassword = password; }
        public String getTestrailPassword() { return testrailPassword; }
        public void setTestrailInstance(TestRailClient trc) { testrail = trc; }
        public TestRailClient getTestrailInstance() { return testrail; }
    }
}
