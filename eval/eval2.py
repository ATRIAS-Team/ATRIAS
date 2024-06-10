"""version:
pandas = 1.0.1
numpy = 1.18.1
"""

import pandas as pd
import matplotlib.pyplot as plt
import pickle
import numpy as np


"""evaluate average of the first and last 100/1000 episodes"""

rewards = pd.read_pickle(r'C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/distance50kdqn_2701_2_highState.pkl')
rewards.columns = ["reward"]
rewards = rewards["reward"].tolist()


average1 = sum(rewards[:100])/100
print(average1)

average2 = sum(rewards[49900:])/100
print(average2)


average1 = sum(rewards[:1000])/1000
print(average1)

average2 = sum(rewards[49000:])/1000
print(average2)

print("------------------------------")

# Distance
distance = pd.read_pickle(r'C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/duration50kdqn_2701_2_highState.pkl')
distance.columns = ["distance"]
distance = distance["distance"].tolist()


average1 = sum(distance[:100])/100
print(average1)

average2 = sum(distance[49900:])/100
print(average2)

average1 = sum(distance[:1000])/1000
print(average1)

average2 = sum(distance[49000:])/1000
print(average2)

print("------------------------------")


# Jobs
jobs = pd.read_pickle(r'C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/jobs50kdqn_0212_highState.pkl')
jobs.columns = ["jobs"]
jobs = jobs["jobs"].tolist()


average1 = sum(jobs[:100])/100
print(average1)

average2 = sum(jobs[49900:])/100
print(average2)


average1 = sum(jobs[:1000])/1000
print(average1)

average2 = sum(jobs[49000:])/1000
print(average2)

print("------------------------------")

# Duration
duration = pd.read_pickle(r'C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/duration50kdqn_0212_highState.pkl')
duration.columns = ["duration"]
duration = duration["duration"].tolist()
print(np.var(duration))


average1 = sum(duration[:100])/100
print(average1)

average2 = sum(duration[49900:])/100
print(average2)


average1 = sum(duration[:1000])/1000
print(average1)

average2 = sum(duration[49000:])/1000
print(average2)

print("------------------------------")

# Charge
charge = pd.read_pickle(r'C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/charges50kdqn_0212_highState.pkl')
charge.columns = ["charge"]
charge = charge["charge"].tolist()


average1 = sum(charge[:100])/100
print(average1)

average2 = sum(charge[49900:])/100
print(average2)


average1 = sum(charge[:1000])/1000
print(average1)

average2 = sum(charge[49000:])/1000
print(average2)

print("------------------------------")

# over
over = pd.read_pickle(r'C:/Users/Oemer/Desktop/David Thesis Abgabe 19.02.2021_Gutachten/jobs50kdqn_2701_2_lowState.pkl')
over.columns = ["over"]
over = over["over"].tolist()


average1 = sum(over[:100])/100
print(average1)

average2 = sum(over[49900:])/100
print(average2)


average1 = sum(over[:1000])/1000
print(average1)

average2 = sum(over[49000:])/1000
print(average2)
