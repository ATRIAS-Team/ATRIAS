# Jadex-DJL
An environment of Jadex (Agent Development Platform) and MATSim (Traffic Simulation) with a Deep Reinforcement Learning scenario considering Multiple Software-Agents.


## Introduction 

This project aims to integrate Deep RL algorithms into Jadex BDI agents applying them in a Mobility on Demand environment using MATSim traffic simulation as an environment. Therefore, we first extended 

### Installation

### License

# Jadex-DJL: Integration of Deep Reinforcement Learning into the Jadex BDI Agent architecture
This is the implementation of the BDI-DRL architecture proposed in the paper: "TBA" published in "TBA. In this project, the integration of DRL into the cognitive Jadex BDI Agent architecture is considered as an hybrid approach to combine symbolic AI and Deep Neural Networks, known s Neuro-Symbolic AI. We integrate Q-Learning (QL) and Deep Q-Learning (DQN) into cognitive BDI software agents implemented in the Jadex Agent Development Framework. Furthermore, we consider Autonomous Ride-Hailing as an application scenario, using MATSim as a simulation environment and trip requests from a bike-sharing company. In our experiments, we compare the RL-BDI approach with an informed utility-based approach evaluating critical domain-specific quality measures.


## How to run

The following steps are required to start the BDI-RL architecture.




- `src/`: source folder 
  - `Agent0.java`: Area agent distributing trip requests to vehicle agents;
  - `Agent1.java`: Vehicle Agent 1 (other configured Agents are also Vehicle Agents);
  - https://github.com/M4rc3l-M/ees-Jadex
  - https://github.com/M4rc3l-M/TrikeFramework

![BDI architecture](TrikeAgent_abstract.pdf)



1. Add the `jade.jar` into 
2.  in the *run configuration*

Program arguments:
```bash
-gui Agent0:Agent0;Agent1:Agent1;Agent2:Agent2;Agent3:Agent3;
```


# Dataset information

The data samples were created by modifying the "Call A Bike" data sets from Deutsche Bahn (DB).
https://data.deutschebahn.com/dataset/data-call-a-bike.html 


# Running DRL and RL 

There are two configurations for the learning process, Q-Leearning as an RL method and Deep-Q-Learning as a DRL method. 
To configure different running scenarios, use the corresponding run.py file and add it to the Jadex platform.



To cite this work, please refer to the corresponding Paper in the Introduction.

# License

See LICENSE.md file
