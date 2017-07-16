package be.nabu.eai.module.services.iface;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.managers.base.BaseArtifactGUIInstance;
import be.nabu.eai.developer.managers.base.BasePortableGUIManager;
import be.nabu.eai.developer.managers.util.RootElementWithPush;
import be.nabu.eai.developer.util.ElementClipboardHandler;
import be.nabu.eai.module.types.structure.StructureGUIManager;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.jfx.control.tree.Tree;
import be.nabu.libs.property.ValueUtils;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.services.DefinedServiceInterfaceResolverFactory;
import be.nabu.libs.services.api.DefinedServiceInterface;
import be.nabu.libs.services.vm.Pipeline;
import be.nabu.libs.services.vm.PipelineInterfaceProperty;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.ModifiableComplexType;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.structure.Structure;
import be.nabu.libs.validator.api.ValidationMessage;
import be.nabu.libs.validator.api.ValidationMessage.Severity;

public class ServiceInterfaceGUIManager extends BasePortableGUIManager<DefinedServiceInterfaceArtifact, BaseArtifactGUIInstance<DefinedServiceInterfaceArtifact>> {

	public ServiceInterfaceGUIManager() {
		super("Service Interface", DefinedServiceInterfaceArtifact.class, new ServiceInterfaceManager());
	}

	@Override
	protected List<Property<?>> getCreateProperties() {
		return null;
	}
	
	@Override
	public String getCategory() {
		return "Services";
	}

	@Override
	protected BaseArtifactGUIInstance<DefinedServiceInterfaceArtifact> newGUIInstance(Entry entry) {
		return new BaseArtifactGUIInstance<DefinedServiceInterfaceArtifact>(this, entry);
	}

	@Override
	protected void setEntry(BaseArtifactGUIInstance<DefinedServiceInterfaceArtifact> guiInstance, ResourceEntry entry) {
		guiInstance.setEntry(entry);
	}

	@Override
	protected DefinedServiceInterfaceArtifact newInstance(MainController controller, RepositoryEntry entry, Value<?>...values) throws IOException {
		return new DefinedServiceInterfaceArtifact(entry.getId(), new Pipeline(new Structure(), new Structure()));
	}

	@Override
	public void display(MainController controller, AnchorPane pane, DefinedServiceInterfaceArtifact instance) throws IOException, ParseException {
		SplitPane split = new SplitPane();
		split.setOrientation(Orientation.HORIZONTAL);
		
		// show the input & output
		StructureGUIManager structureManager = new StructureGUIManager();
		VBox input = new VBox();
		RootElementWithPush element = new RootElementWithPush(
			instance.getInputDefinition(), 
			false,
			instance.getInputDefinition().getProperties()
		);
		// block all properties for the input field
		element.getBlockedProperties().addAll(element.getSupportedProperties());
		
		Tree<Element<?>> inputTree = structureManager.display(controller, input, element, instance.getInputDefinition() instanceof ModifiableComplexType, false);
		inputTree.setClipboardHandler(new ElementClipboardHandler(inputTree));
		split.getItems().add(input);
		
		VBox output = new VBox();
		element = new RootElementWithPush(
			instance.getOutputDefinition(), 
			false,
			instance.getOutputDefinition().getProperties()
		);
		// block all properties for the output field
		element.getBlockedProperties().addAll(element.getSupportedProperties());
		
		Tree<Element<?>> outputTree = structureManager.display(controller, output, element, instance.getOutputDefinition() instanceof ModifiableComplexType, false);
		outputTree.setClipboardHandler(new ElementClipboardHandler(outputTree));
		split.getItems().add(output);
		
		
		VBox box = new VBox();
		HBox iface = new HBox();
		
		TextField ifaceName = new TextField();
		DefinedServiceInterface value = ValueUtils.getValue(PipelineInterfaceProperty.getInstance(), instance.getPipeline().getProperties());
		ifaceName.setText(value == null ? null : value.getId());
		ifaceName.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> arg0, String arg1, String arg2) {
				if (arg2 == null || arg2.isEmpty()) {
					// unset the pipeline attribute
					instance.getPipeline().setProperty(new ValueImpl<DefinedServiceInterface>(PipelineInterfaceProperty.getInstance(), null));
					// reload
					inputTree.refresh();
					outputTree.refresh();
					MainController.getInstance().setChanged();
				}
				else {
					DefinedServiceInterface iface = DefinedServiceInterfaceResolverFactory.getInstance().getResolver().resolve(arg2);
					if (iface != null) {
						// reset the pipeline attribute
						instance.getPipeline().setProperty(new ValueImpl<DefinedServiceInterface>(PipelineInterfaceProperty.getInstance(), iface));
						// reload
						inputTree.refresh();
						outputTree.refresh();
						MainController.getInstance().setChanged();
					}
					else {
						controller.notify(new ValidationMessage(Severity.ERROR, "The indicated node is not a service interface: " + arg2));
					}
				}
			}
		});
		
		iface.getChildren().addAll(new Label("Parent Interface: "), ifaceName);
		
		box.getChildren().addAll(iface, split);
		
		VBox.setVgrow(split, Priority.ALWAYS);
		
		AnchorPane.setTopAnchor(box, 0d);
		AnchorPane.setBottomAnchor(box, 0d);
		AnchorPane.setLeftAnchor(box, 0d);
		AnchorPane.setRightAnchor(box, 0d);
		
		pane.getChildren().add(box);
	}

	@Override
	protected void setInstance(BaseArtifactGUIInstance<DefinedServiceInterfaceArtifact> guiInstance, DefinedServiceInterfaceArtifact instance) {
		guiInstance.setArtifact(instance);
	}
}
