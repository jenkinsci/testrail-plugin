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
package org.jenkinsci.plugins.testrail.JunitResults;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;

/**
 * Created by Drew on 3/24/2014.
 */
public class Testcase {
    private String name;
    private Failure failure;
    private Skipped skipped;
    private Float time;
    private String refs;

    @XmlAttribute
    public void setName(String name) { this.name = name.trim(); }
    @XmlElements({
            @XmlElement(name = "failure"),
            @XmlElement(name = "error")
    })
    public void setFailure(Failure failure) { this.failure = failure; }
    @XmlElement(name = "skipped")
    public void setSkipped(Skipped skipped) { this.skipped = skipped; }
    @XmlAttribute(name = "time")
    public void setTime(Float time) { this.time = time; }
    @XmlAttribute(name = "refs")
    public void setRefs(String refs) { this.refs = refs; }

    public String getName() { return this.name; }
    public Failure getFailure() { return this.failure; }
    public Skipped getSkipped() { return this.skipped; }
    public Float getTime() { return this.time; }
    public String getRefs() { return this.refs; }
}
