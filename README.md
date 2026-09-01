This is a fork of the original  CPG MoreGrid mod.  Please give the original creator some love too!
For the original creator, please feel free to copy/pull anything from my fork without permission or credit.
Also check out W00PKER's fork, they added some cool stuff too at https://github.com/W00PKER/CPG_MoreGrid !

# MoreGrid

A NeoForge 1.21.1 add-on for Create: Power Grid 0.5.5.1, adding four compact and rotatable circuit-board parts:

- 4-terminal lumped-model transformer with an integer winding divider (up to 30 total turns)
- Latching SCR thyristor with gate trigger, holding current, forward drop and on resistance
- Discharging, non-rechargeable zinc-carbon dry-cell pack
- Poisonous/Potato Battery Array+Multiblock (Poisonous variants slowly self recharge)
- DPDT/SPDT/TPST LV/MV Switches
- Small Bulb/Diode/Capacitor/Resistor/DIP Switch
- Buzzer with current dependant frequency version
- Tall connector component
- High power, low resistance shunt resistor
- Lava lamp(Allows to sleep with monsters, and prevents phantoms longer!)
- Electric furnace. Decently fast, but requires some careful temperature management

Items appear in both a dedicated MoreGrid tab and the vanilla Redstone Blocks tab.

## Setup

Place the released Power Grid JAR at:

```text
libs/powergrid-mc1.21.1-0.5.5.1.jar
```

Then open the project in IntelliJ with Java 21 and run the `client` configuration.

Build with:

```bash
./gradlew build
```

See [README_KO.md](README_KO.md) for the complete model specifications and testing instructions.

## Planned
- [x] Potato batteries
	- [x] Poisonous variants
	- [x] Array (8 potatos)
	- [x] Multiblock (24 potatos per block)
- [x] Switch variants (DPDT, SPDT, TPST, DPST and LV, MV)
	- [x] Components
- [x] Small low power Resistor, Diode, Capacitor
- [x] Linked reciever/transmitters/directional reciever
- [x] Tall connector component
- [x] Small Bulb
- [x] Shunt resistor
- [ ] Mechanical counter (Like a cars odometer).  Power applied for rotation, resetting.  Could have internal voltage dividers or encoders for number positions?  "Latch" option, if power removed move to next number if not settled
- [ ] Mechanically timed button.  Like a cooking timer, twist to set.
- [ ] Aeronautics stuff
	- [ ] Tilt switch /w variants (Off placed axis, approaching axis)
	- [ ] Gyroscope (Spun up flywheel off axis rotation detected by coil)
	- [ ] Accelerometer (Spring bound magnetic mass detected by coil)
	- [ ] Altitude (Acts like a potentiometer?)
- [ ] Fancy dispalys (Compatable with modular display block)
	- [ ] Meter with configurable color, labels and unity
		- [ ] Center zero meter variant
		- [ ] Duel needle variant
		- [ ] Duel perpindicular slider variant
	- [ ] "Bar graph" display.  Voltage dependant orthogonal slider.
	- [ ] Add backlights to displays
- [ ] Fancy traffic lights
- [x] Lava lamp!
- [ ] Pressure sensor multiblock.  Senses weight of entitie or aeronautic ships
- [ ] Self extendable cable from holdable item.  Connects to portable battery.
- [ ] Retractable cable block
- [x] Electric furnace
- [ ] Midi keyboard/precussion switch?
- [ ] Add sequenced recipes for most components, gives double output
- [ ] Some block to route power and signal conduit cables through walls
	- [ ] Addable endpoints, sockets, switches, and all "modular display" items
	Maybe done with a "cable facade" like style.  Possibly work with copycat blocks and microblocks too?
	- [ ] Cable tray variant (Endpoints only on underside, but can interface easily with conduits)