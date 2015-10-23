package com.mnk.jenkins.plugins.chef;


import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.Extension;
import hudson.Util;
import hudson.model.Hudson;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import sun.swing.StringUIClientPropertyKey;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ChefParameterDefinition extends ParameterDefinition {
    private static final long serialVersionUID = -2946187268529855645L;
    private static final Logger LOGGER = Logger.getLogger(ChefParameterDefinition.class.getName());
    private static final Pattern MATCH_ALL = Pattern.compile(".*");
    private transient int maxItemLimit = Integer.MAX_VALUE;
    private transient Pattern itemFilterPattern = null;

    private String chefServerUrl;
    private String itemFilter;
    private SortOrder sortOrder;
    private String maxItems;
    private String credentialsId;
    private String value;
    private String defaultValue;
    private String chefItem;

    @DataBoundConstructor
    public ChefParameterDefinition(String name, String description, String chefServerUrl,
                                   String chefItem, String itemFilter, String sortOrder,
                                   String defaultValue, String maxItems, String credentialsId) {
        super(name, description);
        this.chefServerUrl = Util.removeTrailingSlash(chefServerUrl);
        this.chefItem = chefItem;
        this.itemFilter = itemFilter;
        this.sortOrder = SortOrder.valueOf(sortOrder);
        this.defaultValue = StringUtils.trim(defaultValue);
        this.maxItems = maxItems;
        this.credentialsId = credentialsId;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    public List<String> getItems() {
        List<String> items = new ArrayList<String>();
        return items;
    }

    protected UsernamePasswordCredentials findCredentialsByCredentialsId() {
        List<UsernamePasswordCredentials> credentials =
                CredentialsProvider.lookupCredentials(UsernamePasswordCredentials.class, Hudson.getInstance(), ACL.SYSTEM,
                        new DomainRequirement());
        CredentialsMatcher credentialsMatcher = CredentialsMatchers.withId(this.credentialsId);
        return CredentialsMatchers.firstOrNull(credentials, credentialsMatcher);
    }

    public int getMaxItemLimit() {
        if (StringUtils.isNotBlank(this.maxItems)) {
            try {
                this.maxItemLimit = Integer.parseInt(this.maxItems);
            } catch (NumberFormatException e) {
                // NO-OP
            }
        }
        return this.maxItemLimit;
    }

    private List<String> filterItems(List<String> allItems) {
        List<String> filteredItems = new ArrayList<String>(allItems.size());
        for (String item : allItems) {
            if (getItemFilterPattern().matcher(item).matches()) {
                filteredItems.add(item);
                if (filteredItems.size() == getMaxItemLimit()) {
                    break;
                }
            }
        }
        return filteredItems;
    }

    public Pattern getItemFilterPattern() {
        if (itemFilterPattern == null) {
            if (StringUtils.isNotBlank(this.itemFilter)) {
                itemFilterPattern = Pattern.compile(this.itemFilter);
            } else {
                itemFilterPattern = MATCH_ALL;
            }
        }
        return itemFilterPattern;
    }

    public String getChefServerUrl() {
        return this.chefServerUrl;
    }

    public void setChefServerUrl(String chefServerUrl) {
        this.chefServerUrl = chefServerUrl;
    }

    public String getChefItem() {
        return this.chefItem;
    }

    public void setChefItem(String chefItem) {
        this.chefItem = chefItem;
    }

    public String getMaxItems() {
        return this.maxItems;
    }

    public void setMaxItems(String maxItems) {
        this.maxItems = maxItems;
    }

    public String getItemFilter() {
        return this.itemFilter;
    }

    public void setItemFilter(String itemFilter) {
        this.itemFilter = itemFilter;
    }

    public String getCredentialsId() {
        return this.credentialsId;
    }

    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    private ParameterValue createValue(String value) {
        return new ChefParameterValue(getName(), value);
    }

    @Override
    public ParameterValue createValue(StaplerRequest request) {
        String[] values = request.getParameterValues(getName());
        if (values == null || values.length == 0) {
            return this.getDefaultParameterValue();
        }
        String value = values[0];
        return createValue(value);
    }

    @Override
    public ParameterValue createValue(StaplerRequest request, JSONObject formData) {
        ChefParameterValue value = request.bindJSON(ChefParameterValue.class, formData);
        value.setDescription(getDescription());
        return value;
    }

    @Override
    public ParameterValue getDefaultParameterValue() {
        return super.getDefaultParameterValue();
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public enum SortOrder {
        DESC("Descending"), ASC("Ascending");

        private String displayName;

        public String getDisplayName() {
            return this.displayName;
        }

        SortOrder(String displayName) {
            this.displayName = displayName;
        }
    }

    public enum ChefItem {
        ENVIRONMENTS("Environments"),
        NODES("Nodes"),
        ROLES("Roles"),
        COOKBOOKS("Cookbooks"),
        DATABAGS("Databags"),
        CLIENTS("Clients"),
        USERS("Users"),
        GROUPS("Groups"),
        POLICIES("Policies");

        private String displayName;

        public String getDisplayName() {
            return this.displayName;
        }

        ChefItem(String displayName) {
            this.displayName = displayName;
        }
    }

    @Extension
    public static class DescriptorImpl extends ParameterDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.ChefParameterDefinition_DisplayName();
        }

        public ListBoxModel doFillCredentialsIdItems() {
            List<StandardCredentials> credentials =
                    CredentialsProvider.lookupCredentials(StandardCredentials.class,
                            Hudson.getInstance(),
                            ACL.SYSTEM,
                            new DomainRequirement());
            CredentialsMatcher credentialsMatcher = CredentialsMatchers.instanceOf(UsernamePasswordCredentials.class);
            return new StandardListBoxModel().withEmptySelection().withMatching(credentialsMatcher, credentials);
        }


        public FormValidation doCheckItemFilter(@QueryParameter String value) {
            return doCheckRegex(value);
        }

        private static FormValidation doCheckRegex(String value) {
            if (StringUtils.isNotBlank(value)) {
                try {
                    Pattern.compile(value);
                } catch (PatternSyntaxException e) {
                    FormValidation.error(Messages.ChefParameterDefinition_InvalidRegex());
                }
            }
            return FormValidation.ok();
        }

        public ChefItem[] getChefItems() {
            return ChefItem.values();
        }

        public SortOrder[] getSortOrders() {
            return SortOrder.values();
        }
    }

}