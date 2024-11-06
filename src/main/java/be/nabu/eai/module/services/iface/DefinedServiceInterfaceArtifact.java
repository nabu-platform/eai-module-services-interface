/*
* Copyright (C) 2016 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

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
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.properties.MinOccursProperty;
import be.nabu.libs.types.structure.Structure;

public class DefinedServiceInterfaceArtifact implements DefinedServiceInterface, DefinedService {

	Pipeline pipeline;
	private String id;
	private Structure serviceInput;
	private String implementationIdName, useAsContextName;
	private DefinedServiceInterfaceConfiguration configuration;

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
				if (getConfiguration().isHook()) {
					return DefinedServiceInterfaceArtifact.this.getInputDefinition();
				}
				else if (serviceInput == null) {
					synchronized(DefinedServiceInterfaceArtifact.this) {
						if (serviceInput == null) {
							Structure structure = new Structure();
							structure.setName("input");
							structure.setSuperType(DefinedServiceInterfaceArtifact.this.getInputDefinition());
							for (int i = 0; i < 100; i++) {
								implementationIdName = i == 0 ? "implementationId" : "implementationId" + i;
								if (structure.get(implementationIdName) == null) {
									structure.add(new SimpleElementImpl<String>(implementationIdName, SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), structure, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0)));
									break;
								}
							}
							// whether or not we want to set the implementation id as the service context
							for (int i = 0; i < 100; i++) {
								useAsContextName = i == 0 ? "useAsContext" : "useAsContext" + i;
								if (structure.get(useAsContextName) == null) {
									structure.add(new SimpleElementImpl<Boolean>(useAsContextName, SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(Boolean.class), structure, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0)));
									break;
								}
							}
							serviceInput = structure;
						}
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

	String getUseAsContextName() {
		return useAsContextName;
	}

	public Pipeline getPipeline() {
		return pipeline;
	}

	public DefinedServiceInterfaceConfiguration getConfiguration() {
		if (configuration == null) {
			configuration = new DefinedServiceInterfaceConfiguration();
		}
		return configuration;
	}

	public void setConfiguration(DefinedServiceInterfaceConfiguration configuration) {
		this.configuration = configuration;
	}
	
}