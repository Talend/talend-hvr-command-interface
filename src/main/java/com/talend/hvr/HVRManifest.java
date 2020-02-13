package com.talend.hvr;

public class HVRManifest {
    private String channel;
    private String cycle_begin;
    private String cycle_end;
    private boolean initial_load;
    private IntegLoc integ_loc;
    private String[] tables;

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getCycle_begin() {
        return cycle_begin;
    }

    public void setCycle_begin(String cycle_begin) {
        this.cycle_begin = cycle_begin;
    }

    public String getCycle_end() {
        return cycle_end;
    }

    public void setCycle_end(String cycle_end) {
        this.cycle_end = cycle_end;
    }

    public boolean isInitial_load() {
        return initial_load;
    }

    public void setInitial_load(boolean initial_load) {
        this.initial_load = initial_load;
    }

    public IntegLoc getInteg_loc() {
        return integ_loc;
    }

    public void setInteg_loc(IntegLoc integ_loc) {
        this.integ_loc = integ_loc;
    }

    public String[] getTables() {
        return tables;
    }

    public void setTables(String[] tables) {
        this.tables = tables;
    }
}


class IntegLoc {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}