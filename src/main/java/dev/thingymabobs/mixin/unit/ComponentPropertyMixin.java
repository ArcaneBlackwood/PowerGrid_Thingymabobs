package dev.thingymabobs.mixin.unit;

import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import dev.thingymabobs.Thingymabobs;

@Mixin(ComponentProperty.class)
public abstract class ComponentPropertyMixin<T> {
	@Shadow
	private String namespace;
	@Shadow
	private String name;

	@Overwrite
	public String translationKey() {
		if (namespace == Thingymabobs.MOD_ID) 
			return "component" + namespace + ".property." + name;
		return namespace + ".component.property." + name;
	}
}