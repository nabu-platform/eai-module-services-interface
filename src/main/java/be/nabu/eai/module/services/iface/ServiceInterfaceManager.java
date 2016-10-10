package be.nabu.eai.module.services.iface;

import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import be.nabu.eai.module.types.structure.StructureManager;
import be.nabu.eai.repository.EAIRepositoryUtils;
import be.nabu.eai.repository.api.ArtifactManager;
import be.nabu.eai.repository.api.BrokenReferenceArtifactManager;
import be.nabu.eai.repository.api.ModifiableNodeEntry;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.libs.artifacts.ArtifactResolverFactory;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.resources.ResourceReadableContainer;
import be.nabu.libs.resources.ResourceWritableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.resources.api.WritableResource;
import be.nabu.libs.services.api.DefinedServiceInterface;
import be.nabu.libs.services.api.ModifiableServiceInterface;
import be.nabu.libs.services.api.ServiceInterface;
import be.nabu.libs.services.vm.Pipeline;
import be.nabu.libs.types.definition.xml.XMLDefinitionMarshaller;
import be.nabu.libs.types.definition.xml.XMLDefinitionUnmarshaller;
import be.nabu.libs.validator.api.Validation;
import be.nabu.libs.validator.api.ValidationMessage;
import be.nabu.libs.validator.api.ValidationMessage.Severity;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;

public class ServiceInterfaceManager implements ArtifactManager<DefinedServiceInterfaceArtifact>, BrokenReferenceArtifactManager<DefinedServiceInterfaceArtifact> {

	public Pipeline loadPipeline(ResourceEntry entry, List<Validation<?>> messages) throws IOException, ParseException {
		// we need to load the pipeline which is basically a structure
		XMLDefinitionUnmarshaller unmarshaller = StructureManager.getLocalizedUnmarshaller(entry);
		ReadableContainer<ByteBuffer> readable = new ResourceReadableContainer((ReadableResource) EAIRepositoryUtils.getResource(entry, "pipeline.xml", false));
		Pipeline pipeline = new Pipeline(null, null);
		try {
			unmarshaller.unmarshal(IOUtils.toInputStream(readable), pipeline);
		}
		finally {
			readable.close();
		}
		return pipeline;
	}
	
	@Override
	public DefinedServiceInterfaceArtifact load(ResourceEntry entry, List<Validation<?>> messages) throws IOException, ParseException {
		Pipeline pipeline = loadPipeline(entry, messages);
		return new DefinedServiceInterfaceArtifact(entry.getId(), pipeline);
	}

	@Override
	public List<Validation<?>> save(ResourceEntry entry, DefinedServiceInterfaceArtifact artifact) throws IOException {
		Pipeline pipeline = artifact instanceof DefinedServiceInterfaceArtifact 
			? ((DefinedServiceInterfaceArtifact) artifact).pipeline
			: new Pipeline(artifact.getInputDefinition(), artifact.getOutputDefinition());
		savePipeline(entry, pipeline);
		if (entry instanceof ModifiableNodeEntry) {
			((ModifiableNodeEntry) entry).updateNode(getReferences(artifact));
		}
		return new ArrayList<Validation<?>>();
	}

	public void savePipeline(ResourceEntry entry, Pipeline pipeline) throws IOException {
		WritableContainer<ByteBuffer> writable = new ResourceWritableContainer((WritableResource) EAIRepositoryUtils.getResource(entry, "pipeline.xml", true));
		try {
			XMLDefinitionMarshaller marshaller = new XMLDefinitionMarshaller();
			marshaller.setIgnoreUnknownSuperTypes(true);
			marshaller.marshal(IOUtils.toOutputStream(writable), pipeline);
		}
		finally {
			writable.close();
		}
	}
	
	@Override
	public Class<DefinedServiceInterfaceArtifact> getArtifactClass() {
		return DefinedServiceInterfaceArtifact.class;
	}
	
	@Override
	public List<String> getReferences(DefinedServiceInterfaceArtifact artifact) throws IOException {
		return getReferencesForInterface(artifact);
	}

	public static List<String> getReferencesForInterface(ServiceInterface artifact) {
		List<String> references = new ArrayList<String>();
		if (artifact.getParent() instanceof Artifact) {
			references.add(((Artifact) artifact.getParent()).getId());
		}
		references.addAll(StructureManager.getComplexReferences(artifact.getInputDefinition()));
		references.addAll(StructureManager.getComplexReferences(artifact.getOutputDefinition()));
		return references;
	}

	@Override
	public List<Validation<?>> updateReference(DefinedServiceInterfaceArtifact artifact, String from, String to) throws IOException {
		return updateReferences(artifact, from, to);
	}

	public static List<Validation<?>> updateReferences(ServiceInterface artifact, String from, String to) {
		List<Validation<?>> messages = new ArrayList<Validation<?>>();
		if (artifact.getParent() instanceof Artifact) {
			String id = ((Artifact) artifact.getParent()).getId();
			if (from.equals(id)) {
				if (!(artifact instanceof ModifiableServiceInterface)) {
					messages.add(new ValidationMessage(Severity.ERROR, "The service interface is not modifiable"));
				}
				else {
					Artifact newParent = ArtifactResolverFactory.getInstance().getResolver().resolve(to);
					if (!(newParent instanceof DefinedServiceInterface)) {
						messages.add(new ValidationMessage(Severity.ERROR, "Not a service interface: " + to));	
					}
					else {
						((ModifiableServiceInterface) artifact).setParent((DefinedServiceInterface) newParent);
					}
				}
			}
		}
		messages.addAll(StructureManager.updateReferences(artifact.getInputDefinition(), from, to));
		messages.addAll(StructureManager.updateReferences(artifact.getOutputDefinition(), from, to));
		return messages;
	}
	
	@Override
	public List<Validation<?>> updateBrokenReference(ResourceContainer<?> container, String from, String to) throws IOException {
		List<Validation<?>> messages = new ArrayList<Validation<?>>();
		Resource child = container.getChild("pipeline.xml");
		if (child != null) {
			EAIRepositoryUtils.updateBrokenReference(child, from, to, Charset.forName("UTF-8"));
		}
		return messages;
	}
}
