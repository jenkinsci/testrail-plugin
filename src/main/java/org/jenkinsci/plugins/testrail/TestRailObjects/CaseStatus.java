package org.jenkinsci.plugins.testrail.TestRailObjects;

/**
 * Created by alex.bernier on 6/19/17.
 */
public enum CaseStatus {
    PASSED(1),
    BLOCKED(2),
    UNTESTED(3),
    RETEST(4),
    FAILED(5);

    private final int id;
    CaseStatus(int id) { this.id = id; }
    public int getValue() { return id; }
}
