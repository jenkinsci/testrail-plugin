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
 * Created by Drew on 3/24/2014.
 */
public class Run {
    private int suiteId;
    private int id;
    private String description;
    private String milestoneId;

    public void setSuiteId(int suiteId) { this.suiteId = suiteId; }
    public void setId(int id) { this.id = id; }
    public void setDescription(String description) { this.description = description; }
    public void setMilestoneId(String mid) { this.milestoneId = mid; }

    public int getSuiteId() { return this.suiteId; }
    public int getId() { return this.id; }
    public String getDescription() { return this.description; }
    public String getMilestoneId() {return this.milestoneId; }
}
