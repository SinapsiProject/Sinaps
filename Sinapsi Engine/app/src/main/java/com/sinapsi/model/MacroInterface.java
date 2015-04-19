package com.sinapsi.model;

import com.sinapsi.engine.system.SystemFacade;
import com.sinapsi.model.impl.Device;

import java.util.List;

/**
 * Macro interface.
 * Pure model support interface for the Macro class.
 *
 */
public interface MacroInterface {
    public String getName();
    public int getId();
    public void setName(String name);

    public List<Action> getActions();
    public void addAction(Action a);

    public void setTrigger(Trigger t);
    public Trigger getTrigger();

    public void execute(DeviceInterface fs);
}
