package com.sinapsi.desktop.controller;

import com.sinapsi.client.web.SinapsiWebServiceFacade.WebServiceCallback;
import com.sinapsi.desktop.enginesystem.DesktopDeviceInfo;
import com.sinapsi.desktop.main.Launcher;
import com.sinapsi.model.impl.Device;
import com.sinapsi.model.impl.User;
import com.sinapsi.utils.Pair;

import javafx.scene.control.Button;
import javafx.stage.Stage;


public class LayoutController {
	
	private DesktopDeviceInfo deviceInfo = new DesktopDeviceInfo();
	private Stage stage;
	
	public LayoutController(Button button, Stage stage) {
		this.stage = stage;
	}
	
	public void login(String email, String password) {
		Launcher.bgService.getWeb().requestLogin(email, deviceInfo.getDeviceName(), deviceInfo.getDeviceModel(), new WebServiceCallback<Pair<byte[],byte[]>>() {
			
			@Override
			public void success(Pair<byte[], byte[]> t, Object response) {
				Launcher.bgService.getWeb().login(email, password, deviceInfo.getDeviceName(), deviceInfo.getDeviceModel(), new WebServiceCallback<User>() {

					@Override
					public void success(User user, Object response) {
						if(user == null) {
							// TODO Error dialog
							return;
						}
						Launcher.bgService.getWeb().registerDevice(user, 
								email, 
								deviceInfo.getDeviceName(), 
								deviceInfo.getDeviceModel(), 
								deviceInfo.getDeviceType(), 
								deviceInfo.getVersion(), 
								new WebServiceCallback<Device>() {
									
									@Override
									public void success(Device device, Object response) {
										Launcher.bgService.setDevice(device);
										Launcher.bgService.initEngine();
										Launcher.bgService.getWSClient().establishConnection();
										stage.close();
									}
									
									@Override
									public void failure(Throwable error) {
										// TODO Error Dialog
									}
								});
					}

					@Override
					public void failure(Throwable error) {
						// TODO Auto-generated method stub
					}
				});
			}
			
			@Override
			public void failure(Throwable error) {
				// TODO Auto-generated method stub
				
			}
		});
	}

	public void logout() {
		
	}
}
