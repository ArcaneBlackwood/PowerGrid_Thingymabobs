This is a fork of the original  CPG MoreGrid mod.  Please give the original creator some love too!
For the original creator, please feel free to copy/pull anything from my fork without permission or credit.
Also check out W00PKER's fork, they added some cool stuff too at https://github.com/W00PKER/CPG_MoreGrid !

# PowerGrid: Thingymabobs

A NeoForge 1.21.1 add-on for Create: Power Grid, adding a bunch of little things that dont quite fit into the basemod!

Items appear in both a dedicated Thingymabobs tab and the vanilla Redstone Blocks tab.

### Zoeys ideas ❤️ >:3

- [ ] Plasma globe

### Arcanes ideas 💙

- [x] Transformer component
- [x] SCR Thyristor
- [x] Non recharcable zinc-carbom battery component 
- [x] Potato batteries
	- [x] Poisonous variants
	- [x] Array (8 potatos)
	- [x] Multiblock (24 potatos per block)
- [x] Switch variants (DPDT, SPDT, TPST, DPST and LV, MV)
	- [x] LV Components
- [ ] Button component variants (SPDT, small)
- [x] Small low power Resistor, Diode, Capacitor
	- [ ] Transistors (Low power, less efficient, less gain)
- [ ] LED (Default era accurate, low efficiency, limited colors)
- [ ] Bistable relay(two seperate coils for each side)
- [ ] Small low power relays
- [ ] Thermocouple
- [ ] Bimetalic switch(Heat switch)
- [x] Linked reciever/transmitters/directional reciever
- [x] Tall connector component
- [x] Small Bulb
- [x] Shunt resistor
- [ ] Small logic level thyratron, electron tube, triode
- [ ] Multi position stackable rotaty switch (With non stackable component) (4 can stack in single block?)
	- [ ] Electrically operated variant
- [ ] Aeronautics stuff
	- [ ] Tilt switch /w variants (Off placed axis, approaching axis)
	- [ ] Gyroscope (Spun up flywheel off axis rotation detected by coil)
	- [x] Accelerometer (Spring bound magnetic mass detected by coil)
	- [ ] Altitude (Acts like a potentiometer?)
- [ ] Fancy dispalys (Compatable with modular display block)
	- [ ] Meter with configurable color, labels and unity
		- [ ] Center zero meter variant
		- [ ] Duel needle variant
		- [ ] Duel perpindicular slider variant
	- [ ] "Bar graph" display.  Voltage dependant orthogonal slider.
	- [ ] Add backlights to displays
	- [ ] Nixie tubes
- [ ] Punch card reader component(seekable)
	- Maybe string multiple cards together?
- [ ] Multistranded cable that can connect to circuit board headers (1x4 small, 4x4 medium, 8x8 large)
- [ ] Some block to route power and signal conduit cables through walls
	- [ ] Addable endpoints, sockets, switches, and all "modular display" items
	Maybe done with a "cable facade" like style.  Possibly work with copycat blocks and microblocks too?
	- [ ] Cable tray variant (Endpoints only on underside, but can interface easily with conduits)
- [ ] Fancy traffic lights
- [ ] Mechanical counter (Like a cars odometer).  Power applied for rotation, resetting.  Could have internal voltage dividers or encoders for number positions?  "Latch" option, if power removed move to next number if not settled(Tick tick sounds, and togglable ding if added bell?  Can be used without electricicty)
- [ ] Mechanically timed button.  Like a cooking timer, twist to set.
- [x] Lava lamp!
- [ ] Pressure sensor multiblock.  Senses weight of entitie or aeronautic ships
- [ ] Self extendable cable from holdable item.  Connects to portable battery.
- [ ] Retractable cable block
- [x] Electric furnace
- [ ] Particle spawner?
- [ ] Midi keyboard/precussion switch?
- [ ] Add sequenced recipes for most components, gives double output
- [ ] Large block railgun?

## Installation

Replace your powergrid with my custom build here:  
https://github.com/ArcaneBlackwood/PowerGrid-tweaks/releases/latest  

Currently this build is waiting on pull requests on the Power Grid mod this mod requires. They also need to make their way into the main official build. The PR's in question are  
patryk3211/PowerGrid#1076  For allowing components to save and load items. This is used for the wireless receiver/transmitter.  
patryk3211/PowerGrid#1051  For enabling the lava lamp to use a custom thermal behaviour. It works quite differently, and cannot use the builtin type.  

Due to this I have built my custom powergrid. If you are testing this mod, please use this custom build else it wont work. Note in my custom build I have a few modifications such as motor current is dependent on load, as well as generators volume dependent on load as well. Ill probably work these into a separate mod, or this mod with a config option.  

## Building

Place my custom build Power Grid JAR at:

```text
libs/powergrid-mc1.21.1-0.6.0.1.jar
```

Build with:

```bash
./gradlew build
```