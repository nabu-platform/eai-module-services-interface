package be.nabu.eai.module.services.iface;

import javax.jws.WebParam;

public interface HookListener {
	public void fire(@WebParam(name = "hook") String hook, @WebParam(name = "input") Object input);
}
