package be.nabu.eai.module.services.iface;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import be.nabu.eai.developer.api.InterfaceLister;
import be.nabu.eai.developer.util.InterfaceDescriptionImpl;

public class ServiceInterfaceLister implements InterfaceLister {

	private static Collection<InterfaceDescription> descriptions = null;
	
	@Override
	public Collection<InterfaceDescription> getInterfaces() {
		if (descriptions == null) {
			synchronized(ServiceInterfaceLister.class) {
				if (descriptions == null) {
					List<InterfaceDescription> descriptions = new ArrayList<InterfaceDescription>();
					descriptions.add(new InterfaceDescriptionImpl("Hooks", "Generic Hook Listener", "be.nabu.eai.module.services.iface.HookListener.fire"));
					ServiceInterfaceLister.descriptions = descriptions;
				}
			}
		}
		return descriptions;
	}

}
