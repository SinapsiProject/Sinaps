package com.sinapsi.engine;

import com.sinapsi.engine.execution.ExecutionInterface;
import com.sinapsi.engine.execution.RemoteExecutionDescriptor;
import com.sinapsi.engine.log.SinapsiLog;
import com.sinapsi.model.DeviceInterface;
import com.sinapsi.model.MacroComponent;
import com.sinapsi.model.MacroInterface;

import java.util.Collection;
import java.util.HashMap;

/**
 * Macro engine class. Used to initialize the whole macro execution system
 * and to keep a list of all defined macros.
 */
public class MacroEngine {

    private DeviceInterface device;
    private ActivationManager activator;
    private ComponentFactory factory;
    private SinapsiLog log;

    private HashMap<String,MacroInterface> macros = new HashMap<>();

    /**
     * Creates a new MacroEngine instance with a custom component
     * class set.
     * @param currentDevice the devices on which this engine is running
     * @param activationManager the activation manager for trigger activation
     * @param sinapsiLog the sinapsi log
     * @param componentClasses the set of component classes
     */
    @SafeVarargs
    public MacroEngine(DeviceInterface currentDevice,
                       ActivationManager activationManager,
                       SinapsiLog sinapsiLog,
                       Class<? extends MacroComponent>... componentClasses){
        device = currentDevice;
        activator = activationManager;
        log = sinapsiLog;

        factory = new ComponentFactory(device, log, componentClasses);

        sinapsiLog.log("MACROENGINE", "Engine initialized.");
    }


    /**
     * Getter of the ComponentFactory instance generated by this
     * MacroEngine
     * @return the component factory
     */
    public ComponentFactory getComponentFactory(){
        return factory;
    }

    /**
     * Adds a new macro to the list of defined macros. When added,
     * the macro's trigger is registered on the ActivationManager
     * and starts listening for system events.
     * @param m the macro
     */
    public void addMacro(MacroInterface m){
        if(m.getTrigger().getExecutionDevice().getId() == device.getId())
            if(m.isEnabled())
                m.getTrigger().register(activator);

        macros.put(m.getName(), m);
        log.log("MACROENGINE", "Added macro " + m.getId() + ":'" + m.getName()+"' to the engine");
    }

    /**
     * Adds all the macros in the collection to the list of defined
     * macros, registering every trigger on the ActivationManager.
     * @param lm the collection of macros
     */
    public void addMacros(Collection<MacroInterface> lm){
        for(MacroInterface m: lm){
            addMacro(m);
        }
        log.log("MACROENGINE", "Added " + lm.size() + " macros to the engine");
    }

    public void startEngine(){
        log.log("MACROENGINE", "Engine started.");
        activator.setEnabled(true);
        activator.activateForOnEngineStart();
    }

    private MacroInterface getMacroById(int idMacro){
        for(MacroInterface m: macros.values()){
            if(m.getId() == idMacro)
                return m;
        }
        return null;
    }

    public void continueMacro(RemoteExecutionDescriptor red) throws MissingMacroException{

        ExecutionInterface ei = activator.executionInterface.cloneInstance();
        MacroInterface m = getMacroById(red.getIdMacro());

        if(m == null) throw new MissingMacroException();

        ei.continueExecutionFromRemote(m, red.getLocalVariables(), red.getStack());
        ei.execute();
        log.log("MACROENGINE", "Continuing execution of macro with name '" + m.getName() + "'");
    }

    public void pauseEngine(){
        activator.setEnabled(false);
    }

    public void resumeEngine(){
        activator.setEnabled(true);
    }

    public HashMap<String, MacroInterface> getMacros() {
        return macros;
    }

    public void setMacroEnabled(int id, boolean enabled) throws MissingMacroException {
        MacroInterface m = getMacroById(id);
        if(m == null) throw new MissingMacroException();
        m.setEnabled(enabled);

    }

    public class MissingMacroException extends Exception {
    }
}
