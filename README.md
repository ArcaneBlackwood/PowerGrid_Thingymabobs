This is a fork of the original  CPG MoreGrid mod.  Please give the original creator some love too!
For the original creator, please feel free to copy/pull anything from my fork without permission or credit.
Also check out W00PKER's fork, they added some cool stuff too at https://github.com/W00PKER/CPG_MoreGrid !

# MoreGrid

A NeoForge 1.21.1 add-on for Create: Power Grid 0.5.5.1, adding four compact and rotatable circuit-board parts:

- 4-terminal lumped-model transformer with an integer winding divider (up to 30 total turns)
- One-shot I²t cartridge fuse
- Latching SCR thyristor with gate trigger, holding current, forward drop and on resistance
- Discharging, non-rechargeable zinc-carbon dry-cell pack

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
