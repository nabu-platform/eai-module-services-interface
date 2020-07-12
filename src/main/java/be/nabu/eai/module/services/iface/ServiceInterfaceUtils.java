package be.nabu.eai.module.services.iface;

import java.util.ArrayList;
import java.util.List;

import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.DefinedServiceInterface;
import be.nabu.libs.services.api.ServiceInterface;

public class ServiceInterfaceUtils {
	public static List<DefinedService> getImplementations(String interfaceId) {
		List<DefinedService> implementations = new ArrayList<DefinedService>();
		for (DefinedService service : EAIResourceRepository.getInstance().getArtifacts(DefinedService.class)) {
			// interfaces themselves are also services, don't count them though
			if (service instanceof DefinedServiceInterface) {
				continue;
			}
			ServiceInterface serviceInterface = service.getServiceInterface();
			while (serviceInterface != null) {
				if (serviceInterface instanceof DefinedServiceInterface) {
					if (interfaceId.equals(((DefinedServiceInterface) serviceInterface).getId())) {
						implementations.add(service);
						break;
					}
				}
				serviceInterface = serviceInterface.getParent();
			}
		}
		return implementations;
	}
}
