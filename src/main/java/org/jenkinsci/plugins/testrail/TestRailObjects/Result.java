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

/**
 * Created by Drew on 3/25/2014.
 */
public class Result {
    private int caseId;
    private CaseStatus status;
    private Float elapsed;
    private String comment;

    public Result(int caseId, CaseStatus status, String comment, Float elapsed) {
        this.caseId = caseId;
        this.status = status;
        this.comment = comment;
        this.elapsed = elapsed;
    }

    public void setCaseId(int caseId) { this.caseId = caseId; }
    public void setStatus(CaseStatus status) { this.status = status; }
    public void setElapsed(float timeInSeconds) { this.elapsed = timeInSeconds; }
    public void setComment(String comment) { this.comment = comment; }

    public int getCaseId() { return this.caseId; }
    public CaseStatus getStatus() { return this.status; }
    public Float getElapsed() { return this.elapsed; }
    public String getComment() { return this.comment; }

    public String getElapsedTimeString() {
        int time = (elapsed == null || elapsed.intValue() == 0) ? 1 : elapsed.intValue();

        return time + "s";
    }
}
