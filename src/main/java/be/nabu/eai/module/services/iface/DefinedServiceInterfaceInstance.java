package be.nabu.eai.module.services.iface;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.services.DefinedServiceResolverFactory;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.ServiceUtils;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.Service;
import be.nabu.libs.services.api.ServiceException;
import be.nabu.libs.services.api.ServiceInstance;
import be.nabu.libs.services.api.ServiceInterface;
import be.nabu.libs.services.pojo.POJOUtils;
import be.nabu.libs.types.ComplexContentWrapperFactory;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.KeyValuePair;
import be.nabu.libs.types.binding.api.Window;
import be.nabu.libs.types.binding.xml.XMLBinding;
import be.nabu.libs.types.java.BeanType;
import be.nabu.libs.types.mask.MaskedContent;
import be.nabu.libs.types.utils.KeyValuePairImpl;

public class DefinedServiceInterfaceInstance implements ServiceInstance {

	private DefinedServiceInterfaceArtifact definition;
	private Map<String, String> closestMatches = new HashMap<String, String>();
	private List<String> keys;

	public DefinedServiceInterfaceInstance(DefinedServiceInterfaceArtifact definition) {
		this.definition = definition;
	}
	
	@Override
	public Service getDefinition() {
		return definition;
	}

	public static boolean isImplementation(Service service, ServiceInterface iface) {
		ServiceInterface serviceInterface = service.getServiceInterface();
		while (serviceInterface != null && !serviceInterface.equals(iface)) {
			// check on id as well
			if (serviceInterface instanceof Artifact && iface instanceof Artifact && ((Artifact) serviceInterface).getId().equals(((Artifact) iface).getId())) {
				break;
			}
			serviceInterface = serviceInterface.getParent();
		}
		return serviceInterface != null;
	}
	
	private List<String> getKeys() {
		if (keys == null) {
			synchronized(this) {
				if (keys == null) {
					// we only scan one level deep (for now)
					List<String> keys = new ArrayList<String>();
					for (Element<?> child : TypeUtils.getAllChildren(definition.getInputDefinition())) {
						if (child.getType() instanceof BeanType && Object.class.equals(((BeanType<?>) child.getType()).getBeanClass())) {
							keys.add("input/" + child.getName());
						}
					}
//					for (Element<?> child : TypeUtils.getAllChildren(definition.getOutputDefinition())) {
//						if (child.getType() instanceof BeanType && Object.class.equals(((BeanType<?>) child.getType()).getBeanClass())) {
//							keys.add("output/" + child.getName());
//						}
//					}
					this.keys = keys;
				}
			}
		}
		return keys;
	}
	
	@SuppressWarnings("unchecked")
	private String getClosestMatch(ComplexContent input) {
		StringBuilder closeMatchKey = new StringBuilder();
		List<KeyValuePair> keyPairs = new ArrayList<KeyValuePair>();
		for (String key : getKeys()) {
			// only input at this point!
			if (key.startsWith("input/")) {
				KeyValuePairImpl impl = new KeyValuePairImpl();
				impl.setKey(key);
				Object object = input.get(key.substring("input/".length()));
				if (object != null) {
					if (object instanceof Iterable) {
						object = ((Iterable<?>) object).iterator().next();
					}
					if (!(object instanceof ComplexContent)) {
						object = ComplexContentWrapperFactory.getInstance().getWrapper().wrap(object);
					}
					if (object != null) {
						String id = ((DefinedType) ((ComplexContent) object).getType()).getId();
						closeMatchKey.append(";").append(key).append("=").append(id);
						impl.setValue(id);
						keyPairs.add(impl);
					}
				}
			}
		}
		String keyMatch = closeMatchKey.toString();
		if (!closestMatches.containsKey(keyMatch)) {
			int prio = 0;
			DefinedService closest = null;
			for (DefinedService service : EAIResourceRepository.getInstance().getArtifacts(DefinedService.class)) {
				if (isImplementation(service, definition)) {
					// don't match yourself or (preferably) other defined service interfaces (might be extensions)
					if (!(service instanceof DefinedServiceInterfaceArtifact) && !service.getId().equals(definition.getId())) {
						Integer matchPercentage = ServiceUtils.getMatchPercentage(service, keyPairs);
						if (matchPercentage != null) {
							if (closest == null || matchPercentage > prio) {
								prio = matchPercentage;
								closest = service;
							}
						}
					}
				}
			}
			closestMatches.put(keyMatch, closest == null ? null : closest.getId());
		}
		return closestMatches.get(keyMatch);
	}
	
	@Override
	public ComplexContent execute(ExecutionContext executionContext, ComplexContent input) throws ServiceException {
		String implementationId = null;
		if (input == null || input.get(definition.getImplementationIdName()) == null) {
			implementationId = getClosestMatch(input);
		}
		else {
			implementationId = (String) input.get(definition.getImplementationIdName());
		}
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
		// otherwise we do a mask in case you are playing with types
		// e.g. the interface might have a java.lang.Object, while the implementation might have a specific type
		// it currently does not play nice
		else {
			input = new MaskedContent(input, resolve.getServiceInterface().getInputDefinition());
		}
		ServiceRuntime serviceRuntime = new ServiceRuntime(resolve, executionContext);
		if (useAsContext != null && useAsContext) {
			ServiceUtils.setServiceContext(serviceRuntime, implementationId);
		}
		// execute the service and return the result
		return serviceRuntime.run(input);
	}
	
}