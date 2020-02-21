package com.talend.hvr;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class TalendManifest {
    private Mapping[] mappings;

    public Mapping[] getMappings() {
        return mappings;
    }

    public void setMappings(Mapping[] mappings) {
        this.mappings = mappings;
    }

    public boolean contains(String table)
    {
        for (Mapping mapping : this.mappings)
        {
            if (mapping.getHvrTable().equals(table))
                return true;
        }

        return false;
    }

    public Mapping getMapping(String table) {
        for (Mapping mapping : this.mappings)
        {
            if (mapping.getHvrTable().equals(table))
                return mapping;
        }

        return null;
    }

}

class Mapping {
    @JsonProperty("hvr_table")
    private String hvrTable;

    @JsonProperty("talend_job")
    private String talendJob;

    private List<Parameter> parameters = new ArrayList<>();

    public String getHvrTable() {
        return hvrTable;
    }

    public void setHvrTable(String hvrTable) {
        this.hvrTable = hvrTable;
    }

    public String getTalendJob() {
        return talendJob;
    }

    public void setTalendJob(String talendJob) {
        this.talendJob = talendJob;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters.addAll(parameters);
    }
}

class Parameter {
    private Hashtable<String, String> contextVariables = new Hashtable<>();

    public Hashtable<String, String> getContextVariables() {
        return contextVariables;
    }

    @JsonAnySetter
    public void setContextVariables(String key, String value) {
        this.contextVariables.put(key, value);
    }
}