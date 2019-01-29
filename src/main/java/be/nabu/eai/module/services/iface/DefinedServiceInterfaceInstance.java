package be.nabu.eai.module.services.iface;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;

import be.nabu.libs.services.DefinedServiceResolverFactory;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.ServiceUtils;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.Service;
import be.nabu.libs.services.api.ServiceException;
import be.nabu.libs.services.api.ServiceInstance;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.binding.api.Window;
import be.nabu.libs.types.binding.xml.XMLBinding;

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
		// we can pass precaptured input to the implementation
		int index = implementationId.indexOf('$');
		String additionalContent = null;
		if (index > 0) {
			additionalContent = implementationId.substring(index + 1);
			implementationId = implementationId.substring(0, index);
		}
		DefinedService resolve = DefinedServiceResolverFactory.getInstance().getResolver().resolve(implementationId);
		if (resolve == null) {
			throw new ServiceException("INTERFACE-2", "The implementation service '" + implementationId + "' does not exist");
		}
		Boolean useAsContext = (Boolean) input.get(definition.getUseAsContextName());
		
		// check if we have a "closure"
		if (additionalContent != null) {
			XMLBinding binding = new XMLBinding(resolve.getServiceInterface().getInputDefinition(), Charset.forName("UTF-8"));
			try {
				ComplexContent additionalInput = binding.unmarshal(new ByteArrayInputStream(additionalContent.getBytes("UTF-8")), new Window[0]);
				// overwrite the values from the runtime (if applicable)
				for (Element<?> child : TypeUtils.getAllChildren(input.getType())) {
					// only try to set it if it exists in the target service
					if (additionalInput.getType().get(child.getName()) != null) {
						Object value = input.get(child.getName());
						if (value != null) {
							additionalInput.set(child.getName(), value);
						}
					}
				}
				input = additionalInput;
			}
			catch (Exception e) {
				throw new ServiceException("INTERFACE-3", "Invalid captured content", e);
			}
		}
		ServiceRuntime serviceRuntime = new ServiceRuntime(resolve, executionContext);
		if (useAsContext != null && useAsContext) {
			ServiceUtils.setServiceContext(serviceRuntime, implementationId);
		}
		// execute the service and return the result
		return serviceRuntime.run(input);
	}
	
}