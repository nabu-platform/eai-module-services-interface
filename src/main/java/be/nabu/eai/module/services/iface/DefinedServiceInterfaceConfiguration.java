package be.nabu.eai.module.services.iface;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "interface")
public class DefinedServiceInterfaceConfiguration {
	private boolean hook;

	public boolean isHook() {
		return hook;
	}
	public void setHook(boolean hook) {
		this.hook = hook;
	}
}
