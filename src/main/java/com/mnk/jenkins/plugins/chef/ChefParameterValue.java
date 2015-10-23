package com.mnk.jenkins.plugins.chef;


import hudson.model.StringParameterValue;
import org.kohsuke.stapler.DataBoundConstructor;

public class ChefParameterValue extends StringParameterValue {
    private static final long serialVersionUID = 7993744779892726377L;

    @DataBoundConstructor
    public ChefParameterValue(String name, String value) {
        super(name, value);
    }
}
