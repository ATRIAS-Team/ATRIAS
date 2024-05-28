# EES-Jadex Layer Installation 


In the following, we will describe the Installation steps for EES-Jadex.
 1. Download the repository and process the Setup and Build package. 
 2. Import the AMOD Scenario provided in the folder "amod-campus" in the following path: 
 3. Create a run config with the following specifications:
 4. Update the paths in the following classes:
 5. 


# Emergency Evacuation Simulator


## Dependencies

This program depends on the following projects:
* [BDI-ABM Integration](https://github.com/agentsoz/bdi-abm-integration)
* [Jill BDI Engine](https://github.com/agentsoz/jill)
* [Social Network Diffusion Model](https://github.com/agentsoz/diffusion-model)

## Setup

Initialise and update the git submodules as below. This only has to be done once, when you first clone this repository.

```
git submodule update --init --recursive
```

## How to build

```
mvn package
```

This will produce the EES release archive in `ees/target/ees-x.y.z-SNAPSHOT.zip`.

## How to run

To run the example scenario unzip the release archive and follow the instructions provided in the packaged README.md.

## Known Issues

* See [GitHub Issues](https://github.com/agentsoz/ees/issues).

## License

Emergency Evacuation Simulator
Copyright (C) 2014-2020 by its authors. See ees/AUTHORS file.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

For contact information, see ees/AUTHORS file.
