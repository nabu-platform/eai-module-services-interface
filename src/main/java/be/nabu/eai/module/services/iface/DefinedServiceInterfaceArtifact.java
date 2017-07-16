package be.nabu.eai.module.services.iface;

import java.util.Set;

import be.nabu.libs.property.ValueUtils;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.DefinedServiceInterface;
import be.nabu.libs.services.api.ServiceInstance;
import be.nabu.libs.services.api.ServiceInterface;
import be.nabu.libs.services.vm.Pipeline;
import be.nabu.libs.services.vm.PipelineInterfaceProperty;
import be.nabu.libs.types.SimpleTypeWrapperFactory;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.base.SimpleElementImpl;
import be.nabu.libs.types.structure.Structure;

public class DefinedServiceInterfaceArtifact implements DefinedServiceInterface, DefinedService {

	Pipeline pipeline;
	private String id;
	private Structure serviceInput;
	private String implementationIdName;

	public DefinedServiceInterfaceArtifact(String id, Pipeline pipeline) {
		this.id = id;
		this.pipeline = pipeline;
	}
	
	@Override
	public ComplexType getInputDefinition() {
		return (ComplexType) pipeline.get(Pipeline.INPUT).getType();
	}

	@Override
	public ComplexType getOutputDefinition() {
		return (ComplexType) pipeline.get(Pipeline.OUTPUT).getType();
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public ServiceInterface getParent() {
		return ValueUtils.getValue(PipelineInterfaceProperty.getInstance(), pipeline.getProperties());
	}

	@Override
	public ServiceInterface getServiceInterface() {
		return new ServiceInterface() {
			@Override
			public ComplexType getInputDefinition() {
				if (serviceInput == null) {
					synchronized(DefinedServiceInterfaceArtifact.this) {
						Structure structure = new Structure();
						structure.setName("input");
						structure.setSuperType(DefinedServiceInterfaceArtifact.this.getInputDefinition());
						for (int i = 0; i < 1000; i++) {
							implementationIdName = i == 0 ? "implementationId" : "implementationId" + i;
							if (structure.get(implementationIdName) == null) {
								structure.add(new SimpleElementImpl<String>(implementationIdName, SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), structure));
								break;
							}
						}
						serviceInput = structure;
					}
				}
				return serviceInput;
			}
			@Override
			public ComplexType getOutputDefinition() {
				return DefinedServiceInterfaceArtifact.this.getOutputDefinition();
			}
			@Override
			public ServiceInterface getParent() {
				return DefinedServiceInterfaceArtifact.this;
			}
		};
	}

	@Override
	public ServiceInstance newInstance() {
		return new DefinedServiceInterfaceInstance(this);
	}

	@Override
	public Set<String> getReferences() {
		return null;
	}

	String getImplementationIdName() {
		return implementationIdName;
	}

	public Pipeline getPipeline() {
		return pipeline;
	}
	
}