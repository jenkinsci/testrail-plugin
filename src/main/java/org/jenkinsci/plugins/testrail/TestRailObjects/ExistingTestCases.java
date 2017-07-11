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
package org.jenkinsci.plugins.testrail.TestRailObjects;

import org.jenkinsci.plugins.testrail.JunitResults.Testcase;
import org.jenkinsci.plugins.testrail.TestRailClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Drew on 3/24/2014.
 */
public class ExistingTestCases {
    private TestRailClient testRailClient;
    private int projectId;
    //private String suite;
    private int suiteId;
    private List<Case> cases;
    private List<Section> sections;

    public ExistingTestCases(TestRailClient testRailClient, int projectId, int suite)
            throws IOException, ElementNotFoundException {
        this.projectId = projectId;
        this.testRailClient = testRailClient;
        this.suiteId = suite;
        this.cases = new ArrayList<Case>(Arrays.asList(testRailClient.getCases(this.projectId, this.suiteId)));
        this.sections = new ArrayList<Section>(Arrays.asList(testRailClient.getSections(this.projectId, this.suiteId)));
    }

    public int getProjectId() {
        return this.projectId;
    }

    public int getSuiteId() {
        return this.suiteId;
    }

    public List<Case> getCases() {
        return this.cases;
    }

    public List<Section> getSections() {
        return this.sections;
    }

    private String getSectionName(int sectionId) throws ElementNotFoundException {
        Iterator<Section> iterator = sections.iterator();
        while (iterator.hasNext()) {
            Section section = iterator.next();
            if (section.getId() == sectionId) {
                return section.getName();
            }
        }
        throw new ElementNotFoundException("sectionId: " + sectionId);
    }

    public int getCaseId(String sectionName, String caseName) throws ElementNotFoundException {
        Iterator<Case> caseIterator = cases.iterator();
        while (caseIterator.hasNext()) {
            Case testcase = caseIterator.next();
            if (testcase.getTitle().equals(caseName)) {
                Iterator<Section> sectionIterator = sections.iterator();
                while (sectionIterator.hasNext()) {
                    Section section = sectionIterator.next();
                    if (section.getName().equals(sectionName) && (testcase.getSectionId() == section.getId())) {
                        return testcase.getId();
                    }
                }
            }
        }
        throw new ElementNotFoundException(sectionName + ": " + caseName);
    }

    public int getSectionId(String sectionName) throws ElementNotFoundException {
        Iterator<Section> iterator = sections.iterator();
        while (iterator.hasNext()) {
            Section section = iterator.next();
            if (section.getName().equals(sectionName)) {
                return section.getId();
            }
        }
        throw new ElementNotFoundException(sectionName);
    }

    public int addSection(String sectionName, String parentId) 
            throws IOException, ElementNotFoundException, TestRailException {
        Section addedSection = testRailClient.addSection(sectionName, projectId, suiteId, parentId);
        sections.add(addedSection);
        return addedSection.getId();
    }

    public int addCase(Testcase caseToAdd, int sectionId) throws IOException, TestRailException {
        Case addedCase = testRailClient.addCase(caseToAdd, sectionId);
        cases.add(addedCase);
        return addedCase.getId();
    }

    public String[] listTestCases() throws ElementNotFoundException {
        ArrayList<String> result = new ArrayList<String>();
        Iterator<Case> caseIterator = cases.iterator();
        while (caseIterator.hasNext()) {
            Case testcase = caseIterator.next();
            String sectionName = getSectionName(testcase.getSectionId());
            result.add(sectionName + ": " + testcase.getTitle());
        }
        return result.toArray(new String[result.size()]);
    }
}

