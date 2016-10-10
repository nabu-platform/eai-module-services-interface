package be.nabu.eai.module.services.iface;

import be.nabu.libs.services.DefinedServiceResolverFactory;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.Service;
import be.nabu.libs.services.api.ServiceException;
import be.nabu.libs.services.api.ServiceInstance;
import be.nabu.libs.types.api.ComplexContent;

public class DefinedServiceInterfaceInstance implements ServiceInstance {

	private DefinedServiceInterfaceArtifact definition;

	public DefinedServiceInterfaceInstance(DefinedServiceInterfaceArtifact definition) {
		this.definition = definition;
	}
	
	@Override
	public Service getDefinition() {
		return definition;
	}

	@Override
	public ComplexContent execute(ExecutionContext executionContext, ComplexContent input) throws ServiceException {
		if (input == null) {
			throw new ServiceException("INTERFACE-1", "Need at the very least an implementation service to call");
		}
		String implementationId = (String) input.get(definition.getImplementationIdName());
		if (implementationId == null) {
			throw new ServiceException("INTERFACE-1", "Need at the very least an implementation service to call");
		}
		DefinedService resolve = DefinedServiceResolverFactory.getInstance().getResolver().resolve(implementationId);
		if (resolve == null) {
			throw new ServiceException("INTERFACE-2", "The implementation service '" + implementationId + "' does not exist");
		}
		// execute the service and return the result
		return new ServiceRuntime(resolve, executionContext).run(input);
	}
	
}