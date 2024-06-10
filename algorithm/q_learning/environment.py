"""Version:
pandas = 1.0.1
"""

import random
import pandas as pd


class Vehicle():
    """
    This class describes the reinforcement learning environment.
    It describes the state and the action decision of a vehicle
    and creates a queue of trip requests.
    """
    def __init__(self):
        super(Vehicle, self).__init__()
        self.action_space = ['commit', 'negotiate']
        self.n_actions = len(self.action_space)
        self.reward = 0

        # state variables
        self.energy = 20
        self.n_ctrips = 0
        self.state = (self.energy, self.n_ctrips, 0, 0)

        # trip requests
        self.original_trips = []
        self.trips = []

    	 # for evaluation
        self.total_reward = 0
        self.duration = 0
        self.over = 0 # trip into no energy
        self.job = 0 # counter for how many trip where completed, without total_over
        self.distance = 0
        self.charge = 0

    def create_trips(self):
        """
        import trips from the NYC Taxi dataset. Create trips consisting of trip distance
        fare_amount and minutes.
        """
        nyc_trips = pd.read_excel(r'C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/10_trips_range_ny.xlsx')
        subset = nyc_trips[['trip_distance', 'Minutes']]
        subset = subset.round(0)
        self.original_trips = list(tuple(x) for x in subset.to_numpy())
        return self.original_trips

    def refresh_trips(self):
        """
        Create a new list of current trip requests
        from the original trip requests created in def create_trips
        """
        random.shuffle(self.original_trips)
        self.trips = list(self.original_trips)

    def reset(self):
        """reset state of the vehicle to the() initial values"""

        self.n_ctrips = 0
        self.energy = 20
        self.state = ([20, 0, self.trips[0][0], self.trips[0][1]])

        self.total_reward = 0
        self.duration = 0
        self.over = 0 # trip into no energy
        self.job = 0 # counter for how many trip where completed, without total_over
        self.distance = 0
        self.charge = 0
        return self.state


    def commit(self):
        """Changes if agent chooses the action commit"""

        done = False
      
        print("committed")
        self.n_ctrips = self.n_ctrips + 1
        self.energy = self.energy - self.trips[0][0]
        if self.n_ctrips >= 5:
            self.reward = - (self.n_ctrips)
            self.job = 1
        else:
            self.reward = 10 * (self.trips[0][0]/ self.trips[0][1])
            self.job = 1
        if self.energy < 0:
            self.job = 0
            self.reward = -10
            self.trips = [] # delete all trips
            self.over = 1
            done = True
        if self.trips:
            self.trips.pop(0)
            if self.trips:
                self.state = ([self.energy, self.n_ctrips, self.trips[0][0], self.trips[0][1]])
            else:
                done = True
        return (self.state, self.reward, done)

    def negotiate(self):
        """Changes if agent chooses the action negotiate"""

        print("negotiated")
        self.reward = 0
        self.job = 0
        self.trips.pop(0)
        if self.trips:
            self.state = ([self.energy, self.n_ctrips, self.trips[0][0], self.trips[0][1]])
        return (self.state, self.reward) 

    def decide(self, action):
        """function gets an action and transfers it to commit or negotiate"""

        done = False
        self.reward = 0
        if self.trips:
            if action == "commit":
                self.duration = self.trips[0][1]
                self.distance = self.trips[0][0]
                self.charge = 0
                self.commit()
                
            elif action == "negotiate":
                self.negotiate()
                self.duration = 0
                self.distance = 0
                self.charge = 0

        else:
            print("Alle Aufträge abgearbeitet")
            self.reward = 0
            done = True
        print(self.reward)

        return (self.state, self.reward, done)
