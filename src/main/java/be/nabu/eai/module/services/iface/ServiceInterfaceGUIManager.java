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

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import com.sun.crypto.provider.DESCipher;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
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
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.ModifiableComplexType;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.properties.CommentProperty;
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
		
		CheckBox hook = new CheckBox("Is hook");
		hook.setSelected(instance.getConfiguration().isHook());
		
		// show the input & output
		StructureGUIManager structureManager = new StructureGUIManager();
		structureManager.setActualId(instance.getId());
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
		
		// disable the tree if we have the hook property selected
		outputTree.disableProperty().bind(hook.selectedProperty());
		
		hook.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				instance.getConfiguration().setHook(newValue != null && newValue);
				MainController.getInstance().setChanged();
				// if it is a hook, we remove everything from the output
				if (instance.getConfiguration().isHook()) {
					Structure outputDefinition = (Structure) instance.getOutputDefinition();
					// unset any super type
					outputDefinition.setSuperType((Type) null);
					// remove any children
					outputDefinition.removeAll();
					outputTree.refresh();
				}
			}
		});
		
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
		
		iface.setPadding(new Insets(10));
		Label label = new Label("Parent Interface: ");
		label.setPadding(new Insets(5, 0, 0, 0));
		iface.getChildren().addAll(label, ifaceName);
		
		TextArea description = new TextArea();
		description.setMinHeight(150);
		description.setMaxHeight(150);
		description.setPrefHeight(150);
		String comment = ValueUtils.getValue(CommentProperty.getInstance(), instance.getPipeline().getProperties());
		description.setText(comment);
		description.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> arg0, String arg1, String arg2) {
				if (arg2 == null || arg2.isEmpty()) {
					// unset the pipeline attribute
					instance.getPipeline().setProperty(new ValueImpl<String>(CommentProperty.getInstance(), null));
					MainController.getInstance().setChanged();
				}
				else {
					// reset the pipeline attribute
					instance.getPipeline().setProperty(new ValueImpl<String>(CommentProperty.getInstance(), arg2));
					MainController.getInstance().setChanged();
				}
			}
		});
		
		HBox descriptionBox = new HBox();
		descriptionBox.setPadding(new Insets(10));
		Label descriptionLabel = new Label("Description: ");
		descriptionLabel.setPadding(new Insets(5, 0, 0, 0));
		descriptionBox.getChildren().addAll(descriptionLabel, description);
		
		hook.setPadding(new Insets(10));
		box.getChildren().addAll(hook, iface, descriptionBox, split);
		
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
